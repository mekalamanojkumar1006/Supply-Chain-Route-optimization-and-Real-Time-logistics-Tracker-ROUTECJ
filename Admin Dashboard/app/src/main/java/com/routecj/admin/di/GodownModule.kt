package com.routecj.admin.di

import com.routecj.admin.data.repository.FirestoreGodownRepository
import com.routecj.admin.domain.repository.GodownRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GodownModule {

    @Binds
    @Singleton
    abstract fun bindGodownRepository(
        firestoreGodownRepository: FirestoreGodownRepository
    ): GodownRepository
}
