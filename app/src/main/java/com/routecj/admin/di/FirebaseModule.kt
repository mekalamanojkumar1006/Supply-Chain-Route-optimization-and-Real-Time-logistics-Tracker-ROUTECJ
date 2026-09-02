package com.routecj.admin.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.data.repository.AuthRepositoryImpl
import com.routecj.admin.domain.repository.AuthRepository
import com.routecj.admin.domain.usecase.LoginUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for providing Firebase and Authentication related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage {
        // Explicitly specifying the bucket from google-services.json to prevent "Object not found" errors
        // caused by incorrect default bucket initialization in multi-project environments.
        return com.google.firebase.storage.FirebaseStorage.getInstance("gs://supplychaintracking-21492.firebasestorage.app")
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        sessionManager: com.routecj.admin.core.security.SessionManager
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firestore, sessionManager)

    @Provides
    @Singleton
    fun provideAccountProvisioningRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        firestore: FirebaseFirestore
    ): com.routecj.admin.domain.repository.AccountProvisioningRepository =
        com.routecj.admin.data.repository.AccountProvisioningRepositoryImpl(context, firestore)

    @Provides
    @Singleton
    fun provideLoginUseCase(
        authRepository: AuthRepository
    ): LoginUseCase = LoginUseCase(authRepository)
}
