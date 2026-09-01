package com.routecj.admin.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.routecj.admin.core.security.PermissionManager
import com.routecj.admin.core.security.PermissionWrapper
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Constants.NavigationRoutes
import com.routecj.admin.presentation.dashboard.DashboardScreen
import com.routecj.admin.presentation.dashboard.DispatchDashboardScreen
import com.routecj.admin.presentation.dashboard.GodownDashboardScreen
import com.routecj.admin.presentation.dispatch.DispatchScreen
import com.routecj.admin.presentation.dispatch.QRScannerScreen
import com.routecj.admin.presentation.dispatch.VerifiedParcelDetailsScreen
import com.routecj.admin.presentation.drivers.DriversScreen
import com.routecj.admin.presentation.godowns.AddEditGodownScreen
import com.routecj.admin.presentation.godowns.AddParcelScreen
import com.routecj.admin.presentation.godowns.GodownDetailsScreen
import com.routecj.admin.presentation.godowns.GodownsScreen
import com.routecj.admin.presentation.godowns.IncomingParcelsScreen
import com.routecj.admin.presentation.godowns.ParcelDetailsScreen
import com.routecj.admin.presentation.godowns.QRDisplayScreen
import com.routecj.admin.presentation.login.ForgotPasswordScreen
import com.routecj.admin.presentation.login.LoginScreen
import com.routecj.admin.presentation.notifications.NotificationsScreen
import com.routecj.admin.presentation.orders.AddEditOrderScreen
import com.routecj.admin.presentation.orders.OrderDetailsScreen
import com.routecj.admin.presentation.orders.OrdersScreen
import com.routecj.admin.presentation.profile.ProfileScreen
import com.routecj.admin.presentation.reports.ReportsScreen
import com.routecj.admin.presentation.settings.SettingsScreen
import com.routecj.admin.presentation.splash.SplashScreen
import com.routecj.admin.presentation.tracking.TrackingScreen
import com.routecj.admin.presentation.vehicles.*

/**
 * Application Navigation Graph.
 * Defines all the routes and transitions between screens.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    startDestination: String = NavigationRoutes.SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen - Entry point (No permission needed)
        composable(route = NavigationRoutes.SPLASH) {
            SplashScreen(navController = navController)
        }

        // Login Screen - Authentication (No permission needed)
        composable(route = NavigationRoutes.LOGIN) {
            LoginScreen(navController = navController)
        }

        // Forgot Password Screen
        composable(route = NavigationRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController = navController)
        }

        // Main App Screens
        composable(route = NavigationRoutes.DASHBOARD) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DASHBOARD, navController) {
                DashboardScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.ORDERS) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                OrdersScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.ADD_ORDER + "?editOrderId={editOrderId}") { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                val editOrderId = backStackEntry.arguments?.getString("editOrderId")
                AddEditOrderScreen(navController = navController, editOrderId = editOrderId)
            }
        }

        composable(
            route = NavigationRoutes.ORDER_DETAILS,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailsScreen(navController = navController, orderId = orderId)
            }
        }

        composable(route = NavigationRoutes.DRIVERS) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DRIVERS, navController) {
                DriversScreen(navController = navController)
            }
        }

        composable(
            route = NavigationRoutes.DRIVER_LOCATION,
            arguments = listOf(navArgument("driverId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.TRACKING, navController) {
                val driverId = backStackEntry.arguments?.getString("driverId") ?: ""
                com.routecj.admin.presentation.tracking.DriverLocationScreen(
                    navController = navController,
                    driverId = driverId
                )
            }
        }

        composable(route = NavigationRoutes.VEHICLES) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.VEHICLES, navController) {
                VehiclesScreen(navController = navController)
            }
        }

        composable(
            route = NavigationRoutes.VEHICLE_DETAILS,
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.VEHICLES, navController) {
                VehicleDetailsScreen(
                    navController = navController,
                    vehicleId = backStackEntry.arguments?.getString("vehicleId")
                )
            }
        }

        composable(
            route = NavigationRoutes.VEHICLE_LOGS,
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.VEHICLES, navController) {
                VehicleLogsScreen(
                    navController = navController,
                    vehicleId = backStackEntry.arguments?.getString("vehicleId")
                )
            }
        }

        composable(
            route = NavigationRoutes.ADD_VEHICLE + "?editVehicleId={editVehicleId}",
            arguments = listOf(navArgument("editVehicleId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.VEHICLES, navController) {
                val editVehicleId = backStackEntry.arguments?.getString("editVehicleId")
                AddVehicleScreen(navController = navController, editVehicleId = editVehicleId)
            }
        }

        composable(route = NavigationRoutes.GODOWNS) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.GODOWNS, navController) {
                GodownsScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.GODOWN_DASHBOARD) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DASHBOARD, navController) {
                GodownDashboardScreen(navController = navController)
            }
        }

        composable(
            route = NavigationRoutes.INCOMING_PARCELS + "?status={status}",
            arguments = listOf(navArgument("status") { type = NavType.StringType; nullable = true; defaultValue = "PENDING_GODOWN_REVIEW" })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                val status = backStackEntry.arguments?.getString("status") ?: "PENDING_GODOWN_REVIEW"
                IncomingParcelsScreen(navController = navController, initialStatus = status)
            }
        }

        composable(route = NavigationRoutes.ADD_PARCEL) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                AddParcelScreen(navController = navController)
            }
        }

        composable(
            route = NavigationRoutes.PARCEL_DETAILS,
            arguments = listOf(navArgument("parcelId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                val parcelId = backStackEntry.arguments?.getString("parcelId") ?: ""
                ParcelDetailsScreen(navController = navController, parcelId = parcelId)
            }
        }

        composable(
            route = NavigationRoutes.QR_DISPLAY,
            arguments = listOf(navArgument("parcelId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.ORDERS, navController) {
                val parcelId = backStackEntry.arguments?.getString("parcelId") ?: ""
                QRDisplayScreen(navController = navController, parcelId = parcelId)
            }
        }

        composable(
            route = NavigationRoutes.ADD_GODOWN + "?editGodownId={editGodownId}",
            arguments = listOf(navArgument("editGodownId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.GODOWNS, navController) {
                val editGodownId = backStackEntry.arguments?.getString("editGodownId")
                AddEditGodownScreen(navController = navController, editGodownId = editGodownId)
            }
        }

        composable(
            route = NavigationRoutes.GODOWN_DETAILS,
            arguments = listOf(navArgument("godownId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.GODOWNS, navController) {
                val godownId = backStackEntry.arguments?.getString("godownId") ?: ""
                GodownDetailsScreen(navController = navController, godownId = godownId)
            }
        }

        composable(route = NavigationRoutes.DISPATCH) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DISPATCH, navController) {
                DispatchScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.DISPATCH_DASHBOARD) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DASHBOARD, navController) {
                DispatchDashboardScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.QR_SCANNER) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DISPATCH, navController) {
                QRScannerScreen(navController = navController)
            }
        }

        composable(
            route = NavigationRoutes.VERIFIED_PARCEL_DETAILS,
            arguments = listOf(navArgument("parcelId") { type = NavType.StringType })
        ) { backStackEntry ->
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.DISPATCH, navController) {
                val parcelId = backStackEntry.arguments?.getString("parcelId") ?: ""
                VerifiedParcelDetailsScreen(navController = navController, parcelId = parcelId)
            }
        }

        composable(route = NavigationRoutes.TRACKING) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.TRACKING, navController) {
                TrackingScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.REPORTS) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.REPORTS, navController) {
                ReportsScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.PROFILE) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.PROFILE, navController) {
                ProfileScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.USER_MANAGEMENT) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.USER_MANAGEMENT, navController) {
                com.routecj.admin.presentation.usermanagement.UserManagementScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.NOTIFICATIONS) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.NOTIFICATIONS, navController) {
                NotificationsScreen(navController = navController)
            }
        }

        composable(route = NavigationRoutes.SETTINGS) {
            PermissionWrapper(sessionManager, PermissionManager.AppFeature.SETTINGS, navController) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

