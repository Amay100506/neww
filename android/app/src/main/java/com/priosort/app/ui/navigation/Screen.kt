package com.priosort.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object SmartInbox : Screen("inbox", "Smart Inbox")
    data object Analytics : Screen("analytics", "Analytics")
    data object SoundStudio : Screen("sound_studio", "Sound Studio")
    data object LightStudio : Screen("light_studio", "Light Studio")
    data object RuleSimulator : Screen("rule_simulator", "Rule Simulator")
    data object Settings : Screen("settings", "Settings")
    data object Privacy : Screen("privacy", "Privacy policy")
    data object Detail : Screen("detail/{notificationId}", "Details") {
        fun createRoute(id: Long) = "detail/$id"
    }
}
