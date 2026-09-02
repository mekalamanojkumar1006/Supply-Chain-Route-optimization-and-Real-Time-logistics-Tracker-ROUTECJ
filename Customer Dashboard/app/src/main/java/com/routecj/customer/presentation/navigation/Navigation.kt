package com.routecj.customer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.routecj.customer.presentation.auth.AuthState
import com.routecj.customer.presentation.auth.AuthViewModel
import com.routecj.customer.presentation.auth.forgotpassword.ForgotPasswordScreen
import com.routecj.customer.presentation.auth.login.LoginScreen
import com.routecj.customer.presentation.auth.register.RegisterScreen
import com.routecj.customer.presentation.home.HomeScreen
import com.routecj.customer.presentation.splash.SplashScreen
import com.routecj.customer.presentation.profile.ProfileScreen
import com.routecj.customer.presentation.profile.EditProfileScreen
import com.routecj.customer.domain.model.Customer

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object PickupLocation : Screen("pickup_location")
    object Destination : Screen("destination")
    object PackageDetails : Screen("package_details")
    object Schedule : Screen("schedule")
    object BookingSummary : Screen("booking_summary")
    object BookingSuccess : Screen("booking_success/{orderId}") {
        fun createRoute(orderId: String) = "booking_success/$orderId"
    }
    object MyOrders : Screen("my_orders")
    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
    object Tracking : Screen("tracking/{orderId}") {
        fun createRoute(orderId: String) = "tracking/$orderId"
    }
    object Payment : Screen("payment/{orderId}") {
        fun createRoute(orderId: String) = "payment/$orderId"
    }
    object Notifications : Screen("notifications")
}

// Simple local state holder for passing customer object to EditProfile without complex nav args serialization for Phase 3
private var selectedCustomerForEdit: Customer? = null

@Composable
fun RouteCJNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Listen to global auth state changes to handle navigation
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (navController.currentDestination?.route == Screen.Login.route ||
                    navController.currentDestination?.route == Screen.Register.route ||
                    navController.currentDestination?.route == Screen.Splash.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated, is AuthState.Unauthorized -> {
                if (navController.currentDestination?.route != Screen.Login.route &&
                    navController.currentDestination?.route != Screen.Splash.route &&
                    navController.currentDestination?.route != Screen.Register.route &&
                    navController.currentDestination?.route != Screen.ForgotPassword.route) {
                    
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -300 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 300 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToCreateDelivery = {
                    navController.navigate("booking_flow")
                },
                onNavigateToMyOrders = {
                    navController.navigate(Screen.MyOrders.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToEditProfile = { customer ->
                    selectedCustomerForEdit = customer
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EditProfile.route) {
            selectedCustomerForEdit?.let { customer ->
                EditProfileScreen(
                    customer = customer,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        navigation(
            startDestination = Screen.PickupLocation.route,
            route = "booking_flow"
        ) {
            composable(Screen.PickupLocation.route) { backStackEntry ->
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val bookingViewModel: com.routecj.customer.presentation.booking.BookingViewModel = hiltViewModel(parentEntry)
                
                com.routecj.customer.presentation.booking.PickupLocationScreen(
                    viewModel = bookingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = { navController.navigate(Screen.Destination.route) }
                )
            }

            composable(Screen.Destination.route) { backStackEntry ->
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val bookingViewModel: com.routecj.customer.presentation.booking.BookingViewModel = hiltViewModel(parentEntry)
                
                com.routecj.customer.presentation.booking.DestinationScreen(
                    viewModel = bookingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = { navController.navigate(Screen.PackageDetails.route) }
                )
            }

            composable(Screen.PackageDetails.route) { backStackEntry ->
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val bookingViewModel: com.routecj.customer.presentation.booking.BookingViewModel = hiltViewModel(parentEntry)
                
                com.routecj.customer.presentation.booking.PackageDetailsScreen(
                    viewModel = bookingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = { navController.navigate(Screen.Schedule.route) }
                )
            }

            composable(Screen.Schedule.route) { backStackEntry ->
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val bookingViewModel: com.routecj.customer.presentation.booking.BookingViewModel = hiltViewModel(parentEntry)
                
                com.routecj.customer.presentation.booking.ScheduleScreen(
                    viewModel = bookingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = { navController.navigate(Screen.BookingSummary.route) }
                )
            }
            
            composable(Screen.BookingSummary.route) { backStackEntry ->
                val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val bookingViewModel: com.routecj.customer.presentation.booking.BookingViewModel = hiltViewModel(parentEntry)
                
                com.routecj.customer.presentation.booking.BookingSummaryScreen(
                    viewModel = bookingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onBookingSuccess = { orderId ->
                        navController.navigate(Screen.BookingSuccess.createRoute(orderId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.BookingSuccess.route,
                arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                com.routecj.customer.presentation.booking.BookingSuccessScreen(
                    orderId = orderId,
                    onNavigateHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToOrderDetails = {
                        navController.navigate(Screen.OrderDetails.createRoute(orderId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
        }

        composable(Screen.MyOrders.route) {
            com.routecj.customer.presentation.orders.MyOrdersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                },
                onNavigateToCreateOrder = {
                    navController.navigate("booking_flow")
                }
            )
        }

        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            com.routecj.customer.presentation.orders.OrderDetailsScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() },
                onTrackDelivery = { trackingOrderId ->
                    navController.navigate(Screen.Tracking.createRoute(trackingOrderId))
                },
                onPayNow = { payOrderId ->
                    navController.navigate(Screen.Payment.createRoute(payOrderId))
                }
            )
        }

        composable(
            route = Screen.Tracking.route,
            arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            com.routecj.customer.presentation.tracking.TrackingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
        ) {
            com.routecj.customer.presentation.payment.PaymentScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentSuccess = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Notifications.route) {
            com.routecj.customer.presentation.notifications.NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }
    }
}
