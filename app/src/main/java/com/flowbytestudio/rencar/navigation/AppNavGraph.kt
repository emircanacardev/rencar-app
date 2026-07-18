package com.flowbytestudio.rencar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.flowbytestudio.rencar.data.auth.AuthSession
import com.flowbytestudio.rencar.ui.screens.activerental.ActiveRentalScreen
import com.flowbytestudio.rencar.ui.screens.handover.HandoverScreen
import com.flowbytestudio.rencar.ui.screens.history.HistoryScreen
import com.flowbytestudio.rencar.ui.screens.license.LicenseUploadScreen
import com.flowbytestudio.rencar.ui.screens.map.MapScreen
import com.flowbytestudio.rencar.ui.screens.profile.ProfileScreen
import com.flowbytestudio.rencar.ui.screens.referral.ReferralScreen
import com.flowbytestudio.rencar.ui.screens.reservation.ReservationScreen
import com.flowbytestudio.rencar.ui.screens.settings.SettingsScreen
import com.flowbytestudio.rencar.ui.screens.tripsummary.TripSummaryScreen
import com.flowbytestudio.rencar.ui.screens.wallet.WalletScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Yeni kayıt sonrası kullanıcı doğrudan ehliyet yükleme ekranıyla karşılanır.
    val startDestination = remember { if (AuthSession.justRegistered) LicenseUploadRoute else MapRoute }
    LaunchedEffect(Unit) { AuthSession.consumeJustRegistered() }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<MapRoute> {
            MapScreen(
                onNavigateToReservation = { vehicleId ->
                    navController.navigate(ReservationRoute(vehicleId))
                },
                onNavigateToHandover = { rentalId ->
                    navController.navigate(HandoverRoute(rentalId))
                },
                onNavigateToActiveRental = { rentalId ->
                    navController.navigate(ActiveRentalRoute(rentalId))
                },
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(
                onNavigateToPayment = { rentalId ->
                    navController.navigate(TripSummaryRoute(rentalId))
                },
            )
        }
        composable<WalletRoute> { WalletScreen() }
        composable<ProfileRoute> {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToLicenseUpload = { navController.navigate(LicenseUploadRoute) },
                onNavigateToReferral = { navController.navigate(ReferralRoute) },
                onNavigateToPaymentMethods = {
                    // Bottom nav'ın kendi geçiş deseniyle aynı: düz navigate() kullanılırsa
                    // Cüzdan sekmesinin bottom nav tarafından saklanan backstack durumuyla
                    // çakışıp geri dönüşte takılı kalmasına yol açıyordu.
                    navController.navigate(WalletRoute) {
                        popUpTo(MapRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<ReferralRoute> {
            ReferralScreen(onBack = { navController.popBackStack() })
        }
        composable<LicenseUploadRoute> { backStackEntry ->
            val isStartDestination = backStackEntry.destination.route ==
                navController.graph.findStartDestination().route
            LicenseUploadScreen(
                canGoBack = !isStartDestination,
                onFinished = {
                    if (!navController.popBackStack()) navController.navigate(MapRoute)
                },
                onSkip = {
                    if (!navController.popBackStack()) navController.navigate(MapRoute)
                },
            )
        }
        composable<ReservationRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReservationRoute>()
            ReservationScreen(
                vehicleId = route.vehicleId,
                onBack = { navController.popBackStack() },
                // Kiralama PREPARING açıldıysa (dk/sa planı) foto akışına geçilir.
                onNavigateToHandover = { rentalId ->
                    navController.navigate(HandoverRoute(rentalId)) {
                        popUpTo(MapRoute)
                    }
                },
                // DAILY plan doğrudan ACTIVE başlar.
                onNavigateToActiveRental = { rentalId ->
                    navController.navigate(ActiveRentalRoute(rentalId)) {
                        popUpTo(MapRoute)
                    }
                },
            )
        }
        composable<HandoverRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HandoverRoute>()
            HandoverScreen(
                rentalId = route.rentalId,
                onBack = { navController.popBackStack() },
                onCancelled = {
                    navController.popBackStack(MapRoute, inclusive = false)
                },
                onRentalStarted = { rentalId ->
                    navController.navigate(ActiveRentalRoute(rentalId)) {
                        popUpTo(MapRoute)
                    }
                },
            )
        }
        composable<ActiveRentalRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ActiveRentalRoute>()
            ActiveRentalScreen(
                rentalId = route.rentalId,
                onEnded = {
                    navController.navigate(TripSummaryRoute(route.rentalId)) {
                        popUpTo(MapRoute)
                    }
                },
            )
        }
        composable<TripSummaryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TripSummaryRoute>()
            TripSummaryScreen(
                rentalId = route.rentalId,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
