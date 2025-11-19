package net.wshmkr.launcher.model

data class WidgetInfo(
    val widgetId: Int,
    val providerName: String,
    val minWidth: Int,
    val minHeight: Int,
    val label: String
)
