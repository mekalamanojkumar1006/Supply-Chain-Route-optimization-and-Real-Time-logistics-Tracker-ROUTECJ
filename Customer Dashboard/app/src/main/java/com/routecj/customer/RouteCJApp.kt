package com.routecj.customer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RouteCJApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Timber for debug logging
        Timber.plant(Timber.DebugTree())
    }
}
