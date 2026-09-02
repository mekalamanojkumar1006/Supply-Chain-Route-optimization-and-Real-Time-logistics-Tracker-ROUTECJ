package com.routecj.driver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.routecj.driver.MainActivity
import com.routecj.driver.R
import java.util.Date

/**
 * Firebase Cloud Messaging Service for RouteCJ Driver App.
 * Handles incoming push notifications, creates Firestore records, and triggers system alerts.
 */
class RouteCJDriverMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "routecj_driver_alerts_channel"
        const val CHANNEL_NAME = "RouteCJ Driver Alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (token.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("drivers").document(currentUid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "notificationToken" to token,
                        "lastActive" to Date()
                    )
                )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "RouteCJ Driver Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "You have a new dispatch update."

        val tripId = remoteMessage.data["tripId"]
            ?: remoteMessage.data["dispatchId"]
            ?: remoteMessage.data["orderId"]

        val type = remoteMessage.data["type"]
            ?: remoteMessage.data["nav_type"]
            ?: "TRIP_ASSIGNED"

        // Persist notification to Firestore if driver is authenticated
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            val notifMap = mapOf(
                "recipientId" to currentUid,
                "recipientRole" to "DRIVER",
                "title" to title,
                "message" to body,
                "type" to type.uppercase(),
                "tripId" to (tripId ?: ""),
                "isRead" to false,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            FirebaseFirestore.getInstance().collection("notifications").add(notifMap)
        }

        showNotification(title, body, tripId, type)
    }

    private fun showNotification(title: String, message: String, tripId: String?, type: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for trips, dispatches, and pickups"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!tripId.isNullOrBlank()) {
                putExtra("nav_trip_id", tripId)
            }
            if (!type.isNullOrBlank()) {
                putExtra("nav_type", type)
            }
        }

        val notificationId = (remoteMessageIdHash(tripId ?: "") + System.currentTimeMillis() % 1000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun remoteMessageIdHash(tripId: String): Int {
        return (tripId.hashCode() and 0x7FFFFFFF) % 10000
    }
}
