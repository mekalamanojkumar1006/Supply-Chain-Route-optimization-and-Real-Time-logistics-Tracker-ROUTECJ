package com.routecj.customer.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.routecj.customer.domain.model.CustomerNotification
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.NotificationRepository
import com.routecj.customer.domain.repository.TokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RouteCJFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenRepository: TokenRepository
    @Inject lateinit var notificationRepository: NotificationRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val CHANNEL_ID = "routecj_customer_channel"
        private const val CHANNEL_NAME = "RouteCJ Deliveries"

        // Keys explicitly supported in FCM data payload
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_TYPE = "type"
        private const val KEY_ORDER_ID = "orderId"

        // OTP key — EXPLICITLY IGNORED AND NEVER READ
        // private const val KEY_OTP = "otp"  // DO NOT USE
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            scope.launch {
                tokenRepository.saveToken(userId, token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val customerId = authRepository.getCurrentUserId() ?: return

        // Extract data payload — FCM data messages only
        val data = message.data
        val title = data[KEY_TITLE]
            ?: message.notification?.title
            ?: "RouteCJ Update"
        val body = data[KEY_MESSAGE]
            ?: message.notification?.body
            ?: ""
        val type = data[KEY_TYPE] ?: "GENERAL"
        val orderId = data[KEY_ORDER_ID]  // may be null for general notifications

        // OTP SECURITY: We NEVER read, log, or forward any "otp" key from FCM payload.
        // The OTP is only accessible inside the authenticated Customer App via Firestore.

        // 1. Save to Customer Inbox (Firestore) for persistent notification history
        scope.launch {
            val notification = CustomerNotification(
                notificationId = "${type}_${orderId ?: "general"}_${System.currentTimeMillis() / 10000}",
                customerId = customerId,
                orderId = orderId,
                title = title,
                message = body,
                type = type,
                createdAt = System.currentTimeMillis(),
                read = false
            )
            notificationRepository.saveNotification(notification)
        }

        // 2. Show system notification (foreground + background)
        showSystemNotification(title, body, orderId)
    }

    private fun showSystemNotification(title: String, message: String, orderId: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "RouteCJ delivery status updates"
            }
            manager.createNotificationChannel(channel)
        }

        // Tap intent — deep link to MainActivity; MainActivity reads orderId from intent
        val tapIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            orderId?.let { putExtra("deeplink_orderId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, orderId.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
