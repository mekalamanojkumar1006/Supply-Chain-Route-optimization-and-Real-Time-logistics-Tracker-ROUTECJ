package com.routecj.admin.di

import com.routecj.admin.data.repository.FirestoreTrackingRepository
import com.routecj.admin.domain.repository.TrackingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(
        firestoreTrackingRepository: FirestoreTrackingRepository
    ): TrackingRepository
}
