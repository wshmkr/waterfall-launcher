package net.wshmkr.launcher.ui

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object WidgetList : Screen("widgets")
    data object WeatherLocation : Screen("weather_location")
}
