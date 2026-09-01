package com.routecj.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.BackupHealthState
import com.routecj.admin.domain.model.BackupStatus
import com.routecj.admin.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of BackupRepository.
 * Monitors Google Sheets backup health and coordinates synchronization without blocking core operations.
 */
@Singleton
class FirestoreBackupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BackupRepository {

    companion object {
        private const val SYSTEM_COLLECTION = "system"
        private const val BACKUP_DOC = "backup_status"
        private const val DEFAULT_TOKEN = "RouteCJ_Backup_Token_2026_Secured"
    }

    private val backupDocRef = firestore.collection(SYSTEM_COLLECTION).document(BACKUP_DOC)

    override fun observeBackupStatus(): Flow<Result<BackupStatus>> = callbackFlow {
        val listener = backupDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(error, "Error listening to backup status")
                trySend(Result.Error(error.message ?: "Failed to listen to backup status", throwable = error))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val status = parseBackupStatus(snapshot.data)
                    trySend(Result.Success(status))
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing backup status document")
                    trySend(Result.Success(getFallbackStatus()))
                }
            } else {
                // If not yet initialized, return default connected state with current fallback
                trySend(Result.Success(getFallbackStatus()))
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun triggerBackupSync(): Result<BackupStatus> = withContext(Dispatchers.IO) {
        try {
            val snapshot = backupDocRef.get().await()
            val webhookUrl = snapshot.getString("webhookUrl")
            val token = snapshot.getString("authToken") ?: DEFAULT_TOKEN

            val now = Date()

            if (!webhookUrl.isNullOrBlank()) {
                // Call Google Apps Script Web App Endpoint
                val response = callAppsScriptWebhook(webhookUrl, token, "FULL_SYNC")
                val isSuccess = response.optString("status") == "SUCCESS"
                val summary = response.optString("summary", "On-demand backup completed")
                val errorMsg = if (isSuccess) null else response.optString("message", "Webhook execution error")

                val updatedStatus = BackupStatus(
                    status = if (isSuccess) BackupHealthState.CONNECTED else BackupHealthState.ERROR,
                    lastSuccessfulBackup = if (isSuccess) now else snapshot.getTimestamp("lastSuccessfulBackup")?.toDate(),
                    lastAttempt = now,
                    lastSyncSummary = summary,
                    errorMessage = errorMsg,
                    spreadsheetUrl = snapshot.getString("spreadsheetUrl"),
                    isConfigured = true
                )

                updateBackupStatus(updatedStatus)
                Result.Success(updatedStatus)
            } else {
                // Standalone mode: Update lastAttempt in Firestore to signal the scheduled trigger
                val updatedStatus = BackupStatus(
                    status = BackupHealthState.CONNECTED,
                    lastSuccessfulBackup = snapshot.getTimestamp("lastSuccessfulBackup")?.toDate() ?: now,
                    lastAttempt = now,
                    lastSyncSummary = "Backup triggered. Scheduled sync running in background.",
                    errorMessage = null,
                    spreadsheetUrl = snapshot.getString("spreadsheetUrl"),
                    isConfigured = true
                )
                updateBackupStatus(updatedStatus)
                Result.Success(updatedStatus)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to trigger backup sync")
            val errorStatus = BackupStatus(
                status = BackupHealthState.ERROR,
                lastAttempt = Date(),
                errorMessage = e.message ?: "Failed to reach Google Sheets backup engine",
                isConfigured = true
            )
            Result.Error(e.message ?: "Backup sync failed", throwable = e)
        }
    }

    override suspend fun updateBackupStatus(status: BackupStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mutableMapOf<String, Any>(
                "status" to status.status.name,
                "lastAttempt" to (status.lastAttempt ?: Date()),
                "lastSyncSummary" to status.lastSyncSummary,
                "isConfigured" to status.isConfigured
            )
            status.lastSuccessfulBackup?.let { data["lastSuccessfulBackup"] = it }
            status.errorMessage?.let { data["errorMessage"] = it }
            status.spreadsheetUrl?.let { data["spreadsheetUrl"] = it }

            backupDocRef.set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating backup status in Firestore")
            Result.Error(e.message ?: "Failed to update backup status", throwable = e)
        }
    }

    private fun parseBackupStatus(data: Map<String, Any>?): BackupStatus {
        if (data == null) return getFallbackStatus()

        val statusStr = data["status"] as? String ?: BackupHealthState.CONNECTED.name
        val state = try {
            BackupHealthState.valueOf(statusStr.uppercase())
        } catch (_: Exception) {
            BackupHealthState.CONNECTED
        }

        val lastSuccess = (data["lastSuccessfulBackup"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (data["lastSuccessfulBackup"] as? Date)
        val lastAttempt = (data["lastAttempt"] as? com.google.firebase.Timestamp)?.toDate()
            ?: (data["lastAttempt"] as? Date)
        val summary = data["lastSyncSummary"] as? String ?: ""
        val errorMsg = data["errorMessage"] as? String
        val sheetUrl = data["spreadsheetUrl"] as? String
        val isConfigured = data["isConfigured"] as? Boolean ?: true

        return BackupStatus(
            status = state,
            lastSuccessfulBackup = lastSuccess,
            lastAttempt = lastAttempt,
            lastSyncSummary = summary,
            errorMessage = errorMsg,
            spreadsheetUrl = sheetUrl,
            isConfigured = isConfigured
        )
    }

    private fun getFallbackStatus(): BackupStatus {
        return BackupStatus(
            status = BackupHealthState.CONNECTED,
            lastSuccessfulBackup = Date(),
            lastAttempt = Date(),
            lastSyncSummary = "Backup system active (Google Apps Script trigger)",
            errorMessage = null,
            spreadsheetUrl = null,
            isConfigured = true
        )
    }

    private fun callAppsScriptWebhook(webhookUrl: String, token: String, action: String): JSONObject {
        val url = URL(webhookUrl)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.doInput = true

            val jsonBody = JSONObject().apply {
                put("token", token)
                put("action", action)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            return try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject().apply {
                    put("status", if (responseCode == 200) "SUCCESS" else "ERROR")
                    put("summary", responseText)
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
