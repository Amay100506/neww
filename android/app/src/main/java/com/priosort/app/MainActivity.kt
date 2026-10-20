package com.priosort.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.priosort.app.ui.analytics.AnalyticsScreen
import com.priosort.app.ui.analytics.AnalyticsViewModel
import com.priosort.app.ui.detail.NotificationDetailScreen
import com.priosort.app.ui.detail.NotificationDetailViewModel
import com.priosort.app.ui.inbox.SmartInboxScreen
import com.priosort.app.ui.inbox.SmartInboxViewModel
import com.priosort.app.ui.navigation.Screen
import com.priosort.app.ui.settings.SettingsScreen
import com.priosort.app.ui.settings.SettingsViewModel
import com.priosort.app.ui.theme.PrioSortTheme

class MainActivity : ComponentActivity() {

    private val inboxViewModel: SmartInboxViewModel by viewModels()
    private val detailViewModel: NotificationDetailViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val analyticsViewModel: AnalyticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrioSortTheme {
                MainAppContent(
                    inboxViewModel = inboxViewModel,
                    detailViewModel = detailViewModel,
                    settingsViewModel = settingsViewModel,
                    analyticsViewModel = analyticsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    inboxViewModel: SmartInboxViewModel,
    detailViewModel: NotificationDetailViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.SmartInbox.route,
        Screen.Analytics.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.SmartInbox.route,
                        onClick = {
                            navController.navigate(Screen.SmartInbox.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Smart Inbox") },
                        label = { Text("Smart Inbox") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0F172A),
                            indicatorColor = Color(0xFFE2E8F0)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Analytics.route,
                        onClick = {
                            navController.navigate(Screen.Analytics.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                        label = { Text("Analytics") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0F172A),
                            indicatorColor = Color(0xFFE2E8F0)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0F172A),
                            indicatorColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.SmartInbox.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.SmartInbox.route) {
                SmartInboxScreen(
                    viewModel = inboxViewModel,
                    onNotificationClick = { notification ->
                        navController.navigate(Screen.Detail.createRoute(notification.id))
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("notificationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val notifId = backStackEntry.arguments?.getLong("notificationId") ?: 0L
                NotificationDetailScreen(
                    notificationId = notifId,
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
