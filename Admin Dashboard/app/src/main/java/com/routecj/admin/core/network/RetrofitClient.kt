package com.routecj.admin.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.routecj.admin.core.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Provides Retrofit instance for making API calls.
 * Implements the Singleton pattern to ensure a single instance throughout the application.
 * Includes interceptors for logging and error handling.
 */
object RetrofitClient {

    private var instance: Retrofit? = null

    /**
     * Returns or creates a Retrofit instance.
     * This is thread-safe and lazy-loaded.
     */
    fun getInstance(): Retrofit {
        return instance ?: synchronized(this) {
            instance ?: createRetrofit().also { instance = it }
        }
    }

    /**
     * Creates a new Retrofit instance with configured OkHttp client.
     */
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(createGson()))
            .build()
    }

    /**
     * Creates and configures OkHttpClient with logging and timeout interceptors.
     */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addLoggingInterceptor()
            .build()
    }

    /**
     * Configures and adds HTTP logging interceptor for debugging.
     */
    private fun OkHttpClient.Builder.addLoggingInterceptor(): OkHttpClient.Builder {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        addInterceptor(loggingInterceptor)
        return this
    }

    /**
     * Creates Gson instance for JSON serialization/deserialization.
     */
    private fun createGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
}

