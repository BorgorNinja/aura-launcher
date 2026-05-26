package dev.aura.launcher.data.model

data class AuraSettings(
    val gridColumns:      Int     = 4,
    val darkThemeMode:    String  = "system",        // "system" | "light" | "dark"
    val notificationDots: Boolean = true,
    val iconPackPackage:  String  = "",
    val swipeDownAction:  String  = "notifications", // "notifications"|"camera"|"assistant"|"lock"|"none"
    val doubleTapAction:  String  = "none"           // "clock"|"camera"|"assistant"|"lock"|"none"
)
