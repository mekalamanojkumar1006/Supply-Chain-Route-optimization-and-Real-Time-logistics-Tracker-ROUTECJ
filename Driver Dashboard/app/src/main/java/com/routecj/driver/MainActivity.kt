package com.routecj.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.routecj.driver.data.repository.*
import com.routecj.driver.domain.repository.NotificationRepository
import com.routecj.driver.domain.usecase.*
import com.routecj.driver.presentation.auth.AuthViewModel
import com.routecj.driver.presentation.home.DriverHomeViewModel
import com.routecj.driver.presentation.navigation.DriverAppNavHost
import com.routecj.driver.presentation.pickup.PickupViewModel
import com.routecj.driver.presentation.trip.TripViewModel
import com.routecj.driver.ui.theme.RouteCJDriverTheme
import com.routecj.driver.core.util.ConnectivityMonitor
import com.routecj.driver.presentation.components.OfflineLocalBanner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import android.content.Intent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authRepository by lazy { FirebaseAuthRepository() }
    private val driverRepository by lazy { FirestoreDriverRepository() }
    private val orderRepository by lazy { FirestoreOrderRepository() }
    private val dispatchRepository by lazy { FirestoreDispatchRepository() }
    private val vehicleRepository by lazy { FirestoreVehicleRepository() }

    private val authViewModel by lazy {
        val loginUseCase = LoginDriverUseCase(authRepository)
        val getCurrentDriverUseCase = GetCurrentDriverUseCase(authRepository)
        val sendResetUseCase = SendPasswordResetUseCase(authRepository)
        val logoutUseCase = LogoutDriverUseCase(authRepository)

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(
                        loginDriverUseCase = loginUseCase,
                        getCurrentDriverUseCase = getCurrentDriverUseCase,
                        sendPasswordResetUseCase = sendResetUseCase,
                        logoutDriverUseCase = logoutUseCase
                    ) as T
                }
            }
        )[AuthViewModel::class.java]
    }

    private val driverHomeViewModel by lazy {
        val getDriverHomeDataUseCase = GetDriverHomeDataUseCase(
            driverRepository = driverRepository,
            orderRepository = orderRepository,
            dispatchRepository = dispatchRepository,
            vehicleRepository = vehicleRepository
        )

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DriverHomeViewModel(
                        getDriverHomeDataUseCase = getDriverHomeDataUseCase,
                        authRepository = authRepository
                    ) as T
                }
            }
        )[DriverHomeViewModel::class.java]
    }

    private val tripViewModel by lazy {
        val getTripDetailsUseCase = GetTripDetailsUseCase(
            dispatchRepository = dispatchRepository,
            orderRepository = orderRepository,
            vehicleRepository = vehicleRepository
        )
        val startTripUseCase = StartTripUseCase(
            dispatchRepository = dispatchRepository,
            orderRepository = orderRepository
        )
        val completeTripUseCase = com.routecj.driver.domain.usecase.CompleteTripUseCase(
            dispatchRepository = dispatchRepository,
            orderRepository = orderRepository
        )

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TripViewModel(
                        getTripDetailsUseCase = getTripDetailsUseCase,
                        startTripUseCase = startTripUseCase,
                        completeTripUseCase = completeTripUseCase
                    ) as T
                }
            }
        )[TripViewModel::class.java]
    }

    private val pickupViewModel by lazy {
        val getBookedSlotsUseCase = GetBookedSlotsUseCase(orderRepository)
        val getPickupDetailsUseCase = GetPickupDetailsUseCase(orderRepository)
        val markDriverArrivedUseCase = MarkDriverArrivedUseCase(orderRepository)
        val verifyPickupOtpUseCase = VerifyPickupOtpUseCase(orderRepository)

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PickupViewModel(
                        getBookedSlotsUseCase = getBookedSlotsUseCase,
                        getPickupDetailsUseCase = getPickupDetailsUseCase,
                        markDriverArrivedUseCase = markDriverArrivedUseCase,
                        verifyPickupOtpUseCase = verifyPickupOtpUseCase
                    ) as T
                }
            }
        )[PickupViewModel::class.java]
    }

    private val parcelViewModel by lazy {
        val getPickupDetailsUseCase = GetPickupDetailsUseCase(orderRepository)
        val submitParcelDetailsUseCase = SubmitParcelDetailsUseCase(orderRepository)

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return com.routecj.driver.presentation.parcel.ParcelViewModel(
                        getPickupDetailsUseCase = getPickupDetailsUseCase,
                        submitParcelDetailsUseCase = submitParcelDetailsUseCase
                    ) as T
                }
            }
        )[com.routecj.driver.presentation.parcel.ParcelViewModel::class.java]
    }

    private val tripHistoryViewModel by lazy {
        val getDriverTripHistoryUseCase = GetDriverTripHistoryUseCase(
            dispatchRepository = dispatchRepository,
            orderRepository = orderRepository,
            vehicleRepository = vehicleRepository
        )

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return com.routecj.driver.presentation.triphistory.TripHistoryViewModel(
                        getDriverTripHistoryUseCase = getDriverTripHistoryUseCase
                    ) as T
                }
            }
        )[com.routecj.driver.presentation.triphistory.TripHistoryViewModel::class.java]
    }

    private val notificationRepository: NotificationRepository by lazy {
        FirestoreNotificationRepository()
    }

    private val notificationViewModel by lazy {
        val getDriverNotificationsUseCase = GetDriverNotificationsUseCase(notificationRepository)

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return com.routecj.driver.presentation.notification.NotificationViewModel(
                        getDriverNotificationsUseCase = getDriverNotificationsUseCase,
                        notificationRepository = notificationRepository
                    ) as T
                }
            }
        )[com.routecj.driver.presentation.notification.NotificationViewModel::class.java]
    }

    private val profileViewModel by lazy {
        val getDriverProfileUseCase = GetDriverProfileUseCase(
            driverRepository = driverRepository,
            vehicleRepository = vehicleRepository
        )

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return com.routecj.driver.presentation.profile.ProfileViewModel(
                        getDriverProfileUseCase = getDriverProfileUseCase,
                        driverRepository = driverRepository,
                        authRepository = authRepository
                    ) as T
                }
            }
        )[com.routecj.driver.presentation.profile.ProfileViewModel::class.java]
    }

    private val storeRepository by lazy { LocalStoreRepository() }

    private val driverMapViewModel by lazy {
        val getTripDetailsUseCase = GetTripDetailsUseCase(
            dispatchRepository = dispatchRepository,
            orderRepository = orderRepository,
            vehicleRepository = vehicleRepository
        )
        val getStoreLocationsUseCase = GetStoreLocationsUseCase(storeRepository)
        val getSelectedStoreUseCase = GetSelectedStoreUseCase(storeRepository)

        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return com.routecj.driver.presentation.map.DriverMapViewModel(
                        getTripDetailsUseCase = getTripDetailsUseCase,
                        getStoreLocationsUseCase = getStoreLocationsUseCase,
                        getSelectedStoreUseCase = getSelectedStoreUseCase
                    ) as T
                }
            }
        )[com.routecj.driver.presentation.map.DriverMapViewModel::class.java]
    }

    private var initialNotificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        // Register / sync FCM token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (!currentUid.isNullOrBlank() && !token.isNullOrBlank()) {
                    lifecycleScope.launch {
                        notificationRepository.updateFcmToken(currentUid, token)
                    }
                }
            }
        }

        val connectivityMonitor = ConnectivityMonitor(this)

        setContent {
            val isOnline by connectivityMonitor.isConnected.collectAsState(initial = true)
            
            RouteCJDriverTheme {
                Column {
                    OfflineLocalBanner(isOffline = !isOnline)
                    Box(modifier = Modifier.weight(1f)) {
                        DriverAppNavHost(
                            authViewModel = authViewModel,
                            driverHomeViewModel = driverHomeViewModel,
                            tripViewModel = tripViewModel,
                            driverMapViewModel = driverMapViewModel,
                            pickupViewModel = pickupViewModel,
                            parcelViewModel = parcelViewModel,
                            tripHistoryViewModel = tripHistoryViewModel,
                            notificationViewModel = notificationViewModel,
                            profileViewModel = profileViewModel,
                            initialNotificationRoute = initialNotificationRoute
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val tripId = intent?.getStringExtra("nav_trip_id")
        val type = intent?.getStringExtra("nav_type")

        if (!tripId.isNullOrBlank()) {
            initialNotificationRoute = when (type) {
                "TRIP_CANCELLED" -> com.routecj.driver.presentation.navigation.DriverDestinations.TRIP_HISTORY
                "SYSTEM" -> com.routecj.driver.presentation.navigation.DriverDestinations.NOTIFICATIONS
                else -> com.routecj.driver.presentation.navigation.DriverDestinations.tripDetailsRoute(tripId)
            }
        }
    }
}