package com.routecj.admin.di

import android.app.Application
import androidx.room.Room
import com.routecj.admin.core.network.RetrofitClient
import com.routecj.admin.core.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt dependency injection module.
 * Provides application-level singleton dependencies.
 *
 * Uses @Module to declare the class as a Hilt module.
 * Uses @InstallIn to specify the component scope (SingletonComponent for app-level dependencies).
 * Uses @Singleton to ensure single instance throughout the app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the Retrofit instance for API calls.
     * This is a singleton - the same instance is used throughout the application.
     */
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return RetrofitClient.getInstance()
    }

    /**
     * Provides application context as a dependency.
     * Useful for operations that require Context (database, shared preferences, etc.)
     */
    @Provides
    @Singleton
    fun provideAppContext(app: Application): Application {
        return app
    }
}

