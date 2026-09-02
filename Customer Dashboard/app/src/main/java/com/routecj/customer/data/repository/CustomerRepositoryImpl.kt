package com.routecj.customer.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.routecj.customer.core.error.DataError
import com.routecj.customer.core.error.toDataError
import com.routecj.customer.domain.model.Customer
import com.routecj.customer.domain.repository.CustomerRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CustomerRepository {

    private val collection = firestore.collection("customers")

    override suspend fun getCustomer(customerId: String): Result<Customer> {
        return try {
            if (auth.currentUser == null || auth.currentUser?.uid != customerId) {
                return Result.failure(DataError.PermissionDenied())
            }

            val snapshot = collection.document(customerId).get().await()
            if (snapshot.exists()) {
                val customer = Customer(
                    id = snapshot.id,
                    email = snapshot.getString("email") ?: "",
                    name = snapshot.getString("name"),
                    phoneNumber = snapshot.getString("phone"), // Using 'phone' as per expected structure
                    role = snapshot.getString("role") ?: "customer",
                    isActiveAccount = snapshot.get("isActiveAccount") as? Boolean ?: true,
                    profileImageUrl = snapshot.getString("profileImageUrl"),
                    createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                Result.success(customer)
            } else {
                Result.failure(DataError.NotFound())
            }
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }

    override suspend fun createCustomer(customer: Customer): Result<Unit> {
        return try {
            val data = mapOf(
                "uid" to customer.id,
                "email" to customer.email,
                "name" to customer.name,
                "phone" to customer.phoneNumber,
                "role" to "customer",
                "isActiveAccount" to true,
                "profileImageUrl" to customer.profileImageUrl,
                "createdAt" to customer.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            collection.document(customer.id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val wrappedFailure = if (e is FirebaseFirestoreException) e else e.toDataError()
            Result.failure(wrappedFailure)
        }
    }

    override suspend fun updateCustomer(customer: Customer): Result<Unit> {
        return try {
            if (auth.currentUser == null || auth.currentUser?.uid != customer.id) {
                return Result.failure(DataError.PermissionDenied())
            }

            val data = mutableMapOf<String, Any>()
            // Do not update email, id, role, or createdAt here
            customer.name?.let { data["name"] = it }
            customer.phoneNumber?.let { data["phone"] = it }
            customer.profileImageUrl?.let { data["profileImageUrl"] = it }
            data["updatedAt"] = System.currentTimeMillis()
            
            collection.document(customer.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }
}
