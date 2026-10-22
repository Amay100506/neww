package com.priosort.app

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.priosort.app.data.preferences.AppSettings
import com.priosort.app.data.preferences.ThemeMode
import com.priosort.app.ui.analytics.AnalyticsScreen
import com.priosort.app.ui.analytics.AnalyticsViewModel
import com.priosort.app.ui.detail.NotificationDetailScreen
import com.priosort.app.ui.detail.NotificationDetailViewModel
import com.priosort.app.ui.inbox.SmartInboxScreen
import com.priosort.app.ui.inbox.SmartInboxViewModel
import com.priosort.app.ui.navigation.Screen
import com.priosort.app.ui.settings.PrivacyPolicyScreen
import com.priosort.app.ui.settings.SettingsScreen
import com.priosort.app.ui.settings.SettingsViewModel
import com.priosort.app.ui.sound.SoundStudioScreen
import com.priosort.app.ui.sound.SoundStudioViewModel
import com.priosort.app.ui.light.LightStudioScreen
import com.priosort.app.ui.light.LightStudioViewModel
import com.priosort.app.ui.simulator.RuleSimulatorScreen
import com.priosort.app.ui.simulator.RuleSimulatorViewModel
import com.priosort.app.ui.theme.PrioSortTheme
import kotlinx.coroutines.flow.map

class MainActivity : FragmentActivity() {
    private var appUnlocked by mutableStateOf(false)
    private var biometricLockEnabled = false
    private val inboxViewModel: SmartInboxViewModel by viewModels()
    private val detailViewModel: NotificationDetailViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val analyticsViewModel: AnalyticsViewModel by viewModels()
    private val soundStudioViewModel: SoundStudioViewModel by viewModels()
    private val lightStudioViewModel: LightStudioViewModel by viewModels()
    private val ruleSimulatorViewModel: RuleSimulatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by (application as PrioSortApplication).appSettingsRepository.settings
                .map<AppSettings, AppSettings?> { it }
                .collectAsState(initial = null)
            val appSettings = settings
            if (appSettings == null) {
                PrioSortTheme { AppLoadingScreen() }
            } else {
                SideEffect {
                    biometricLockEnabled = appSettings.biometricLockEnabled
                    if (biometricLockEnabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
                val darkTheme = when (appSettings.themeMode) {
                    ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                val systemDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * appSettings.fontScale)) {
                    PrioSortTheme(darkTheme = darkTheme) {
                        if (appSettings.biometricLockEnabled && !appUnlocked) {
                            AppLockScreen(onUnlock = ::requestAppUnlock)
                        } else {
                            MainAppContent(inboxViewModel, detailViewModel, settingsViewModel, analyticsViewModel, soundStudioViewModel, lightStudioViewModel, ruleSimulatorViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        if (biometricLockEnabled) appUnlocked = false
        super.onStop()
    }

    private fun requestAppUnlock() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                appUnlocked = true
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PrioSort")
            .setSubtitle("Authenticate to access your saved notifications")
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(promptInfo)
    }
}

@Composable
private fun AppLoadingScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Loading PrioSort…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AppLockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PrioSort is locked", style = MaterialTheme.typography.headlineSmall)
        Text("Authenticate to open your saved notification inbox.", modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onUnlock) { Text("Unlock with biometrics") }
    }
}

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun MainAppContent(
    inboxViewModel: SmartInboxViewModel,
    detailViewModel: NotificationDetailViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    soundStudioViewModel: SoundStudioViewModel,
    lightStudioViewModel: LightStudioViewModel,
    ruleSimulatorViewModel: RuleSimulatorViewModel
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val tabs = listOf(
        TabItem(Screen.SmartInbox.route, "Inbox", Icons.Default.Inbox),
        TabItem(Screen.SoundStudio.route, "Sounds", Icons.Default.GraphicEq),
        TabItem(Screen.LightStudio.route, "Lights", Icons.Default.LightMode),
        TabItem(Screen.Analytics.route, "Analytics", Icons.Default.Analytics),
        TabItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )
    val showNavigation = destination?.route in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showNavigation) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.SmartInbox.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.SmartInbox.route) {
                SmartInboxScreen(
                    viewModel = inboxViewModel,
                    onNotificationClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onPrivacyClick = { navController.navigate(Screen.Privacy.route) }
                )
            }
            composable(Screen.SoundStudio.route) { SoundStudioScreen(soundStudioViewModel) }
            composable(Screen.LightStudio.route) { LightStudioScreen(lightStudioViewModel) }
            composable(Screen.RuleSimulator.route) { RuleSimulatorScreen(ruleSimulatorViewModel) }
            composable(Screen.Analytics.route) { AnalyticsScreen(analyticsViewModel) }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onPrivacyClick = { navController.navigate(Screen.Privacy.route) },
                    onSoundStudioClick = { navController.navigate(Screen.SoundStudio.route) },
                    onLightStudioClick = { navController.navigate(Screen.LightStudio.route) },
                    onRuleSimulatorClick = { navController.navigate(Screen.RuleSimulator.route) },
                    onAnalyticsClick = { navController.navigate(Screen.Analytics.route) }
                )
            }
            composable(Screen.Privacy.route) { PrivacyPolicyScreen(onBackClick = { navController.popBackStack() }) }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("notificationId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("notificationId") ?: 0L
                NotificationDetailScreen(id, detailViewModel, onBackClick = { navController.popBackStack() })
            }
        }
    }
}
