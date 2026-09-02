package com.routecj.driver.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.routecj.driver.presentation.auth.AuthUiState
import com.routecj.driver.presentation.auth.AuthViewModel
import com.routecj.driver.presentation.auth.LoginScreen
import com.routecj.driver.presentation.home.DriverHomeScreen
import com.routecj.driver.presentation.home.DriverHomeViewModel
import com.routecj.driver.presentation.pickup.BookedSlotsScreen
import com.routecj.driver.presentation.pickup.PickupDetailsScreen
import com.routecj.driver.presentation.pickup.PickupViewModel
import com.routecj.driver.presentation.trip.TripDetailsScreen
import com.routecj.driver.presentation.trip.TripViewModel

object DriverDestinations {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val TRIP_DETAILS = "trip_details/{tripId}"
    const val TRIP_MAP = "trip_map/{tripId}"
    const val TRIP_HISTORY = "trip_history"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val VEHICLE_DETAILS = "vehicle_details"
    const val BOOKED_SLOTS = "booked_slots"
    const val PICKUP_DETAILS = "pickup_details/{orderId}"
    const val PARCEL_DETAILS = "parcel_details/{orderId}"

    fun tripDetailsRoute(tripId: String) = "trip_details/$tripId"
    fun tripMapRoute(tripId: String) = "trip_map/$tripId"
    fun pickupDetailsRoute(orderId: String) = "pickup_details/$orderId"
    fun parcelDetailsRoute(orderId: String) = "parcel_details/$orderId"
}

@Composable
fun DriverAppNavHost(
    authViewModel: AuthViewModel,
    driverHomeViewModel: DriverHomeViewModel,
    tripViewModel: TripViewModel,
    driverMapViewModel: com.routecj.driver.presentation.map.DriverMapViewModel,
    pickupViewModel: PickupViewModel,
    parcelViewModel: com.routecj.driver.presentation.parcel.ParcelViewModel,
    tripHistoryViewModel: com.routecj.driver.presentation.triphistory.TripHistoryViewModel,
    notificationViewModel: com.routecj.driver.presentation.notification.NotificationViewModel,
    profileViewModel: com.routecj.driver.presentation.profile.ProfileViewModel,
    initialNotificationRoute: String? = null
) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = DriverDestinations.SPLASH
    ) {
        composable(DriverDestinations.SPLASH) {
            com.routecj.driver.presentation.splash.SplashScreen(
                onAnimationFinished = {
                    val targetDestination = if (uiState is AuthUiState.Authenticated && !initialNotificationRoute.isNullOrBlank()) {
                        initialNotificationRoute
                    } else {
                        DriverDestinations.LOGIN
                    }
                    navController.navigate(targetDestination) {
                        popUpTo(DriverDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(DriverDestinations.LOGIN) {
            val context = androidx.compose.ui.platform.LocalContext.current
            when (val state = uiState) {
                is AuthUiState.Authenticated -> {
                    DriverHomeScreen(
                        driver = state.driver,
                        driverHomeViewModel = driverHomeViewModel,
                        notificationViewModel = notificationViewModel,
                        onNavigateToTrip = { tripId ->
                            navController.navigate(DriverDestinations.tripDetailsRoute(tripId))
                        },
                        onNavigateToBookedSlots = {
                            navController.navigate(DriverDestinations.BOOKED_SLOTS)
                        },
                        onNavigateToTripHistory = {
                            navController.navigate(DriverDestinations.TRIP_HISTORY)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(DriverDestinations.NOTIFICATIONS)
                        },
                        onNavigateToProfile = {
                            navController.navigate(DriverDestinations.PROFILE)
                        },
                        onNavigateToVehicleDetails = {
                            navController.navigate(DriverDestinations.VEHICLE_DETAILS)
                        },
                        onLogout = {
                            com.routecj.driver.service.DriverLocationService.stop(context)
                            authViewModel.logout()
                        }
                    )
                }
                else -> {
                    LoginScreen(authViewModel = authViewModel)
                }
            }
        }

        composable(
            route = DriverDestinations.TRIP_DETAILS,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                TripDetailsScreen(
                    tripId = tripId,
                    driverId = driver.id,
                    tripViewModel = tripViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenMap = { id ->
                        navController.navigate(DriverDestinations.tripMapRoute(id))
                    }
                )
            } else LoginScreen(authViewModel = authViewModel)
        }

        composable(
            route = DriverDestinations.TRIP_MAP,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                com.routecj.driver.presentation.map.DriverMapScreen(
                    tripId = tripId,
                    driverId = driver.id,
                    driverMapViewModel = driverMapViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(DriverDestinations.TRIP_HISTORY) {
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                com.routecj.driver.presentation.triphistory.TripHistoryScreen(
                    driverId = driver.id,
                    tripHistoryViewModel = tripHistoryViewModel,
                    onNavigateToTripDetails = { tripId ->
                        navController.navigate(DriverDestinations.tripDetailsRoute(tripId))
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(DriverDestinations.NOTIFICATIONS) {
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                com.routecj.driver.presentation.notification.NotificationCenterScreen(
                    driverId = driver.id,
                    notificationViewModel = notificationViewModel,
                    onNavigateToTrip = { tripId ->
                        navController.navigate(DriverDestinations.tripDetailsRoute(tripId))
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(DriverDestinations.PROFILE) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                com.routecj.driver.presentation.profile.DriverProfileScreen(
                    driverId = driver.id,
                    profileViewModel = profileViewModel,
                    onNavigateToVehicleDetails = {
                        navController.navigate(DriverDestinations.VEHICLE_DETAILS)
                    },
                    onNavigateToTripHistory = {
                        navController.navigate(DriverDestinations.TRIP_HISTORY)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(DriverDestinations.NOTIFICATIONS)
                    },
                    onLogout = {
                        com.routecj.driver.service.DriverLocationService.stop(context)
                        authViewModel.logout()
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(DriverDestinations.VEHICLE_DETAILS) {
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                com.routecj.driver.presentation.profile.VehicleDetailsScreen(
                    driverId = driver.id,
                    profileViewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(DriverDestinations.BOOKED_SLOTS) {
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                BookedSlotsScreen(
                    driverId = driver.id,
                    pickupViewModel = pickupViewModel,
                    onNavigateToPickupDetails = { orderId ->
                        navController.navigate(DriverDestinations.pickupDetailsRoute(orderId))
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(
            route = DriverDestinations.PICKUP_DETAILS,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                PickupDetailsScreen(
                    orderId = orderId,
                    driverId = driver.id,
                    pickupViewModel = pickupViewModel,
                    onNavigateToParcelDetails = { id ->
                        navController.navigate(DriverDestinations.parcelDetailsRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }

        composable(
            route = DriverDestinations.PARCEL_DETAILS,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val driver = (uiState as? AuthUiState.Authenticated)?.driver

            if (driver != null) {
                com.routecj.driver.presentation.parcel.ParcelDetailsScreen(
                    orderId = orderId,
                    driverId = driver.id,
                    parcelViewModel = parcelViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToVerifyOtp = {
                        navController.popBackStack(DriverDestinations.pickupDetailsRoute(orderId), inclusive = false)
                    },
                    onNavigateToHome = {
                        navController.navigate(DriverDestinations.LOGIN) {
                            popUpTo(DriverDestinations.LOGIN) { inclusive = false }
                        }
                    }
                )
            } else {
                LoginScreen(authViewModel = authViewModel)
            }
        }
    }
}

