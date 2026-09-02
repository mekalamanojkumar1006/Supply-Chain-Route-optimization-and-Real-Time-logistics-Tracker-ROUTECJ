package com.routecj.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.customer.core.error.toDataError
import com.routecj.customer.domain.repository.OtpRepository
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.inject.Inject

class OtpRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OtpRepository {

    override suspend fun generatePickupOtp(orderId: String): Result<String> {
        return try {
            val orderRef = firestore.collection("orders").document(orderId)
            val otpRef = orderRef.collection("secure").document("otp")

            val now = System.currentTimeMillis()
            val expiryTime = now + (5 * 60 * 1000) // 5 minutes

            // Generate secure 6-digit OTP
            val random = SecureRandom()
            val otp = String.format("%06d", random.nextInt(1000000))

            firestore.runTransaction { transaction ->
                val orderSnapshot = transaction.get(orderRef)
                
                // Only generate if we don't have an active OTP
                val existingStatus = orderSnapshot.getString("pickupOtpStatus")
                val existingExpires = orderSnapshot.getLong("pickupOtpExpiresAt")
                
                if (existingStatus == "ACTIVE" && existingExpires != null && existingExpires > now) {
                    // There's already an active unexpired OTP. 
                    // Transaction aborts basically, but we can't easily return the plaintext from here 
                    // unless we read the subcollection too.
                    // Let's read the existing OTP to return it.
                    val existingOtpDoc = transaction.get(otpRef)
                    val existingOtpValue = existingOtpDoc.getString("value")
                    if (existingOtpValue != null) {
                        return@runTransaction existingOtpValue
                    }
                }

                // Write plain OTP to secure subcollection
                transaction.set(otpRef, mapOf(
                    "value" to otp,
                    "createdAt" to now,
                    "expiresAt" to expiryTime
                ))

                // Update main order document with public OTP metadata
                transaction.update(orderRef, mapOf(
                    "pickupOtpStatus" to "ACTIVE",
                    "pickupOtpCreatedAt" to now,
                    "pickupOtpExpiresAt" to expiryTime
                ))
                
                otp // return new otp
            }.await().let {
                Result.success(it.toString())
            }
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }

    override suspend fun getSecureOtp(orderId: String): Result<String> {
        return try {
            val snapshot = firestore.collection("orders").document(orderId)
                .collection("secure").document("otp")
                .get().await()

            val otpValue = snapshot.getString("value")
            if (otpValue != null) {
                Result.success(otpValue)
            } else {
                Result.failure(Exception("OTP not found").toDataError())
            }
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }
}
