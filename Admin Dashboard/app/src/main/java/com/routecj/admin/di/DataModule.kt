package com.routecj.admin.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.data.repository.FirestoreOrderRepository
import com.routecj.admin.data.repository.ProfileRepositoryImpl
import com.routecj.admin.data.repository.DashboardRepositoryImpl
import com.routecj.admin.domain.repository.OrderRepository
import com.routecj.admin.domain.repository.ProfileRepository
import com.routecj.admin.domain.repository.DashboardRepository
import com.routecj.admin.domain.repository.DriverRepository
import com.routecj.admin.data.repository.FirestoreDriverRepository
import com.routecj.admin.domain.repository.VehicleRepository
import com.routecj.admin.data.repository.FirestoreVehicleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for data layer dependencies.
 * Provides repository and API service instances.
 *
 * Responsibilities:
 * - Provide API service implementations
 * - Provide repository implementations
 * - Bind interfaces to implementations
 * - Manage singleton scope for data sources
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Provides OrderRepository implementation.
     * Binds the interface to concrete implementation.
     */
    @Provides
    @Singleton
    fun provideOrderRepository(
        firestore: FirebaseFirestore
    ): OrderRepository {
        return FirestoreOrderRepository(firestore)
    }

    /**
     * Provides ProfileRepository implementation.
     * Binds the interface to concrete implementation.
     * Used for fetching the current admin's profile from Firestore.
     */
    @Provides
    @Singleton
    fun provideProfileRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): ProfileRepository {
        return ProfileRepositoryImpl(firebaseAuth, firestore)
    }

    /**
     * Provides DashboardRepository implementation.
     */
    @Provides
    @Singleton
    fun provideDashboardRepository(
        firestore: FirebaseFirestore
    ): DashboardRepository {
        return DashboardRepositoryImpl(firestore)
    }

    /**
     * Provides DriverRepository implementation.
     */
    @Provides
    @Singleton
    fun provideDriverRepository(
        firestore: FirebaseFirestore
    ): DriverRepository {
        return FirestoreDriverRepository(firestore)
    }

    /**
     * Provides VehicleRepository implementation.
     */
    @Provides
    @Singleton
    fun provideVehicleRepository(
        firestore: FirebaseFirestore,
        storage: com.google.firebase.storage.FirebaseStorage
    ): VehicleRepository {
        return FirestoreVehicleRepository(firestore, storage)
    }
}

