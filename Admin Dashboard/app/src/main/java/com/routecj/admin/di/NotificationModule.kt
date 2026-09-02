package com.routecj.admin.di

import com.routecj.admin.data.repository.FirestoreNotificationRepository
import com.routecj.admin.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        firestoreNotificationRepository: FirestoreNotificationRepository
    ): NotificationRepository
}
