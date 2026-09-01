package com.routecj.admin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for RouteCJ Admin.
 * Entry point for Hilt dependency injection and app-level initialization.
 *
 * @HiltAndroidApp - Enables Hilt dependency injection for the entire application.
 * This annotation generates the code needed to integrate Hilt into the app.
 *
 * Responsibilities:
 * - Initialize Timber logging
 * - Set up Hilt DI container
 * - Configure app-wide settings
 * - Initialize crash reporting, analytics, etc. (if needed)
 */
@HiltAndroidApp
class RouteCJAdminApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging library
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize OpenStreetMap (osmdroid) configuration with compliant User-Agent
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = "RouteCJAdmin/${BuildConfig.VERSION_NAME} (Android; Package: com.routecj.admin)"
            load(applicationContext, applicationContext.getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        }

        // Temporary AUTH_DEBUG startup diagnostics
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val user = auth.currentUser
        val options = com.google.firebase.FirebaseApp.getInstance().options
        
        Timber.tag("AUTH_DEBUG").d("--- FIREBASE STARTUP ---")
        Timber.tag("AUTH_DEBUG").d("Project ID (options): ${options.projectId}")
        Timber.tag("AUTH_DEBUG").d("Application ID (options): ${options.applicationId}")
        Timber.tag("AUTH_DEBUG").d("currentUser at startup = ${user != null}")
        if (user != null) {
            Timber.tag("AUTH_DEBUG").d("UID: ${user.uid}")
            Timber.tag("AUTH_DEBUG").d("Email: ${user.email}")
        }
        Timber.tag("AUTH_DEBUG").d("------------------------")
    }
}

