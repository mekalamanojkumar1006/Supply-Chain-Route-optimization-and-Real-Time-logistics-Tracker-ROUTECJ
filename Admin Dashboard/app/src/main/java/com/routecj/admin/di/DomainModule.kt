package com.routecj.admin.di

import com.routecj.admin.domain.repository.*
import com.routecj.admin.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideGetOrdersUseCase(repo: OrderRepository) = GetOrdersUseCase(repo)

    @Provides
    @Singleton
    fun provideGetOrderByIdUseCase(repo: OrderRepository) = GetOrderByIdUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateOrderUseCase(repo: OrderRepository, n: CreateNotificationUseCase) = CreateOrderUseCase(repo, n)

    @Provides
    @Singleton
    fun provideUpdateOrderUseCase(repo: OrderRepository) = UpdateOrderUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteOrderUseCase(repo: OrderRepository) = DeleteOrderUseCase(repo)

    @Provides
    @Singleton
    fun provideUpdateOrderStatusUseCase(repo: OrderRepository) = UpdateOrderStatusUseCase(repo)

    @Provides
    @Singleton
    fun provideGetDashboardMetricsUseCase(repo: DashboardRepository) = GetDashboardMetricsUseCase(repo)

    @Provides
    @Singleton
    fun provideGetDriversUseCase(repo: DriverRepository) = GetDriversUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateDriverUseCase(repo: DriverRepository) = CreateDriverUseCase(repo)

    @Provides
    @Singleton
    fun provideUpdateDriverUseCase(repo: DriverRepository) = UpdateDriverUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteDriverUseCase(repo: DriverRepository) = DeleteDriverUseCase(repo)

    @Provides
    @Singleton
    fun provideGetVehiclesUseCase(repo: VehicleRepository) = GetVehiclesUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateVehicleUseCase(repo: VehicleRepository) = CreateVehicleUseCase(repo)

    @Provides
    @Singleton
    fun provideUpdateVehicleUseCase(repo: VehicleRepository) = UpdateVehicleUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteVehicleUseCase(repo: VehicleRepository) = DeleteVehicleUseCase(repo)

    @Provides
    @Singleton
    fun provideGetDispatchesUseCase(repo: DispatchRepository) = GetDispatchesUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateDispatchUseCase(repo: DispatchRepository) = CreateDispatchUseCase(repo)

    @Provides
    @Singleton
    fun provideUpdateDispatchStatusUseCase(repo: DispatchRepository, n: CreateNotificationUseCase) = UpdateDispatchStatusUseCase(repo, n)

    @Provides
    @Singleton
    fun provideAssignDispatchUseCase(repo: DispatchRepository, n: CreateNotificationUseCase) = AssignDispatchUseCase(repo, n)

    @Provides
    @Singleton
    fun provideGetGodownsUseCase(repo: GodownRepository) = GetGodownsUseCase(repo)

    @Provides
    @Singleton
    fun provideGetGodownByIdUseCase(repo: GodownRepository) = GetGodownByIdUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateGodownUseCase(repo: GodownRepository) = CreateGodownUseCase(repo)

    @Provides
    @Singleton
    fun provideUpdateGodownUseCase(repo: GodownRepository, n: CreateNotificationUseCase) = UpdateGodownUseCase(repo, n)

    @Provides
    @Singleton
    fun provideDeleteGodownUseCase(repo: GodownRepository) = DeleteGodownUseCase(repo)

    @Provides
    @Singleton
    fun provideGetNotificationsUseCase(repo: NotificationRepository) = GetNotificationsUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateNotificationUseCase(repo: NotificationRepository) = CreateNotificationUseCase(repo)

    @Provides
    @Singleton
    fun provideMarkNotificationReadUseCase(repo: NotificationRepository) = MarkNotificationReadUseCase(repo)

    @Provides
    @Singleton
    fun provideMarkAllNotificationsReadUseCase(repo: NotificationRepository) = MarkAllNotificationsReadUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteNotificationUseCase(repo: NotificationRepository) = DeleteNotificationUseCase(repo)

    @Provides
    @Singleton
    fun provideGetActiveTripsUseCase(repo: TrackingRepository) = GetActiveTripsUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateDriverAccountUseCase(repo: AccountProvisioningRepository) = CreateDriverAccountUseCase(repo)

    @Provides
    @Singleton
    fun provideCreateAdminAccountUseCase(repo: AccountProvisioningRepository) = CreateAdminAccountUseCase(repo)

    @Provides
    @Singleton
    fun provideGetAdminUsersUseCase(repo: AccountProvisioningRepository) = GetAdminUsersUseCase(repo)

    @Provides
    @Singleton
    fun provideUpdateUserStatusUseCase(repo: AccountProvisioningRepository) = UpdateUserStatusUseCase(repo)

    @Provides
    @Singleton
    fun provideCheckDuplicateAccountUseCase(repo: AccountProvisioningRepository) = CheckDuplicateAccountUseCase(repo)
}
