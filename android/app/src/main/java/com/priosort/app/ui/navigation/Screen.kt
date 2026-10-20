package com.priosort.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object SmartInbox : Screen("inbox", "Smart Inbox")
    data object Analytics : Screen("analytics", "Analytics")
    data object Settings : Screen("settings", "Settings")
    data object Detail : Screen("detail/{notificationId}", "Details") {
        fun createRoute(id: Long) = "detail/$id"
    }
}
