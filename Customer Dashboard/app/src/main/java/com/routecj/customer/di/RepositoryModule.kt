package com.routecj.customer.di

import com.routecj.customer.data.repository.CustomerRepositoryImpl
import com.routecj.customer.data.repository.FirebaseAuthRepositoryImpl
import com.routecj.customer.data.repository.OrderRepositoryImpl
import com.routecj.customer.data.repository.TokenRepositoryImpl
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import com.routecj.customer.domain.repository.OrderRepository
import com.routecj.customer.domain.repository.TokenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        impl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        impl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindTokenRepository(
        impl: TokenRepositoryImpl
    ): TokenRepository

    @Binds
    @Singleton
    abstract fun bindOtpRepository(
        impl: com.routecj.customer.data.repository.OtpRepositoryImpl
    ): com.routecj.customer.domain.repository.OtpRepository

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(
        impl: com.routecj.customer.data.repository.TrackingRepositoryImpl
    ): com.routecj.customer.domain.repository.TrackingRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: com.routecj.customer.data.repository.PaymentRepositoryImpl
    ): com.routecj.customer.domain.repository.PaymentRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(
        impl: com.routecj.customer.data.repository.InvoiceRepositoryImpl
    ): com.routecj.customer.domain.repository.InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: com.routecj.customer.data.repository.NotificationRepositoryImpl
    ): com.routecj.customer.domain.repository.NotificationRepository

    @Binds
    @Singleton
    abstract fun bindGodownRepository(
        impl: com.routecj.customer.data.repository.GodownRepositoryImpl
    ): com.routecj.customer.domain.repository.GodownRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(
        impl: com.routecj.customer.data.repository.RouteRepositoryImpl
    ): com.routecj.customer.domain.repository.RouteRepository

    @Binds
    @Singleton
    abstract fun bindGeocodingRepository(
        impl: com.routecj.customer.data.repository.NominatimGeocodingRepositoryImpl
    ): com.routecj.customer.domain.repository.GeocodingRepository
}
