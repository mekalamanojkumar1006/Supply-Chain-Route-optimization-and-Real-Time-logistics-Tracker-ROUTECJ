package com.routecj.admin.di

import com.routecj.admin.data.repository.FirestoreDispatchRepository
import com.routecj.admin.domain.repository.DispatchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DispatchModule {

    @Binds
    @Singleton
    abstract fun bindDispatchRepository(
        firestoreDispatchRepository: FirestoreDispatchRepository
    ): DispatchRepository
}
