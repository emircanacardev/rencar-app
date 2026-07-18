package com.flowbytestudio.rencar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flowbytestudio.rencar.data.auth.AuthSession
import com.flowbytestudio.rencar.data.auth.SessionManager
import com.flowbytestudio.rencar.data.auth.SessionState
import com.flowbytestudio.rencar.data.settings.OnboardingPreferences
import com.flowbytestudio.rencar.data.settings.ThemeController
import com.flowbytestudio.rencar.navigation.ActiveRentalRoute
import com.flowbytestudio.rencar.navigation.AppNavGraph
import com.flowbytestudio.rencar.navigation.HandoverRoute
import com.flowbytestudio.rencar.navigation.LicenseUploadRoute
import com.flowbytestudio.rencar.navigation.LoginRoute
import com.flowbytestudio.rencar.navigation.OnboardingRoute
import com.flowbytestudio.rencar.navigation.RegisterRoute
import com.flowbytestudio.rencar.navigation.RencarNavBar
import com.flowbytestudio.rencar.navigation.ReservationRoute
import com.flowbytestudio.rencar.navigation.SettingsRoute
import com.flowbytestudio.rencar.navigation.TripSummaryRoute
import com.flowbytestudio.rencar.ui.screens.login.LoginScreen
import com.flowbytestudio.rencar.ui.screens.onboarding.OnboardingScreen
import com.flowbytestudio.rencar.ui.screens.register.RegisterScreen
import com.flowbytestudio.rencar.ui.theme.RencarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeController.init(applicationContext)
        OnboardingPreferences.init(applicationContext)
        SessionManager.init(applicationContext)
        // Kayıtlı oturum doğrulanana kadar splash ekranda kalır; sonuç LOGGED_IN
        // ise login hiç görünmeden ana ekrana geçilir.
        splashScreen.setKeepOnScreenCondition {
            AuthSession.sessionState.value == SessionState.UNKNOWN
        }
        setContent {
            RencarTheme {
                RencarApp()
            }
        }
    }
}

@Composable
private fun RencarApp() {
    val sessionState by AuthSession.sessionState.collectAsState()

    // Splash setKeepOnScreenCondition ile ekranda; karar gelene kadar bir şey çizilmez.
    if (sessionState == SessionState.UNKNOWN) return

    if (sessionState == SessionState.LOGGED_OUT) {
        val hasSeenOnboarding by OnboardingPreferences.hasSeenOnboarding.collectAsState()
        val showOnboarding = BuildConfig.ALWAYS_SHOW_ONBOARDING || !hasSeenOnboarding

        val authNavController = rememberNavController()
        val proceedToLogin: () -> Unit = {
            OnboardingPreferences.markOnboardingSeen()
            authNavController.navigate(LoginRoute)
        }

        NavHost(
            navController = authNavController,
            startDestination = if (showOnboarding) OnboardingRoute else LoginRoute,
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onStartClick = proceedToLogin,
                    onLoginClick = proceedToLogin,
                )
            }

            composable<LoginRoute> {
                LoginScreen(
                    onLoggedIn = { /* AuthSession handles state */ },
                    onNavigateToRegister = { authNavController.navigate(RegisterRoute) },
                )
            }

            composable<RegisterRoute> {
                RegisterScreen(
                    onRegistered = { /* AuthSession handles state */ },
                    onNavigateToLogin = { authNavController.popBackStack() },
                )
            }
        }

        return
    }

    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val showBottomBar = currentDestination?.hasRoute(ReservationRoute::class) != true &&
        currentDestination?.hasRoute(SettingsRoute::class) != true &&
        currentDestination?.hasRoute(LicenseUploadRoute::class) != true &&
        currentDestination?.hasRoute(HandoverRoute::class) != true &&
        currentDestination?.hasRoute(ActiveRentalRoute::class) != true &&
        currentDestination?.hasRoute(TripSummaryRoute::class) != true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                RencarNavBar(navController = navController)
            }
        },
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
