package com.idp.smartgait.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.idp.smartgait.data.UserPreferences
import com.idp.smartgait.data.UserProfile
import com.idp.smartgait.data.dataStore
import com.idp.smartgait.ui.screens.DashboardScreen
import com.idp.smartgait.ui.screens.ForgotPasswordScreen
import com.idp.smartgait.ui.screens.GaitScreen
import com.idp.smartgait.ui.screens.HistoryScreen
import com.idp.smartgait.ui.screens.LiveScreen
import com.idp.smartgait.ui.screens.LoginScreen
import com.idp.smartgait.ui.screens.OnboardingScreen
import com.idp.smartgait.ui.screens.ProfileScreen
import com.idp.smartgait.ui.screens.ProfileSetupScreen
import com.idp.smartgait.ui.screens.SettingsScreen
import com.idp.smartgait.ui.screens.SignUpScreen
import com.idp.smartgait.ui.screens.SplashScreen
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyDark
import com.idp.smartgait.ui.theme.NavyMedium
import com.idp.smartgait.ui.theme.TextMuted
import com.idp.smartgait.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/* ================================================================
 *  ROOT NAV GRAPH
 * ================================================================ */
@Composable
fun AppNavigation(vm: MainViewModel) {
    val context = LocalContext.current
    val preferences = remember { UserPreferences(context.dataStore) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(preferences) { route ->
                navController.navigate(route) { popUpTo("splash") { inclusive = true } }
            }
        }

        composable("onboarding") {
            val scope = kotlinx.coroutines.MainScope()
            OnboardingScreen {
                scope.launch { preferences.completeOnboarding() }
                navController.navigate("login") { popUpTo("onboarding") { inclusive = true } }
            }
        }

        composable("login") {
            LoginScreen(
                preferences = preferences,
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onSignUp = { navController.navigate("signup") },
                onForgotPassword = { navController.navigate("forgot_password") }
            )
        }

        composable("signup") {
            SignUpScreen(
                preferences = preferences,
                onSignUpSuccess = { navController.navigate("profile_setup") { popUpTo("signup") { inclusive = true } } },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable("profile_setup") {
            ProfileSetupScreen(
                preferences = preferences,
                onComplete = { navController.navigate("main") { popUpTo("profile_setup") { inclusive = true } } }
            )
        }

        composable("main") {
            MainApp(vm, preferences) {
                // Logout callback
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}

/* ================================================================
 *  MAIN APP (bottom navigation)
 * ================================================================ */
private data class BottomTab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    BottomTab("Dashboard", Icons.Rounded.Dashboard),
    BottomTab("Live",      Icons.Rounded.Analytics),
    BottomTab("Gait",      Icons.Rounded.DirectionsWalk),
    BottomTab("History",   Icons.Rounded.History),
    BottomTab("Profile",   Icons.Rounded.Person)
)

@Composable
private fun MainApp(
    vm: MainViewModel,
    preferences: UserPreferences,
    onLogout: () -> Unit
) {
    val state by vm.state.collectAsState()
    val selectedScenario by vm.selectedScenario.collectAsState()
    val selectedSession by vm.selectedSession.collectAsState()
    val userProfile by preferences.userProfile.collectAsState(initial = UserProfile())
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show messages as snackbars
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NavyDark,
        bottomBar = {
            NavigationBar(containerColor = NavyMedium) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            // Clear session detail when leaving History
                            if (index != 3) vm.selectSession(null)
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            selectedTextColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = .12f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (selectedTab) {
            0 -> DashboardScreen(state, userProfile, mod)
            1 -> LiveScreen(state, vm, selectedScenario, mod)
            2 -> GaitScreen(state, mod)
            3 -> HistoryScreen(state, vm, selectedSession, mod)
            4 -> SettingsScreen(state, vm, preferences, onLogout, mod)
        }
    }
}
