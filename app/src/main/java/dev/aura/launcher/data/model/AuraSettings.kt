package dev.aura.launcher.data.model

data class AuraSettings(
    val gridColumns:      Int     = 4,
    val darkThemeMode:    String  = "system",        // "system" | "light" | "dark"
    val notificationDots: Boolean = true,
    val iconPackPackage:  String  = "",
    val swipeDownAction:  String  = "notifications",
    val doubleTapAction:  String  = "none",
    val colorTheme:       String  = "dynamic",       // see AuraPaletteEntry.key
    val activeDockGroup:  Int     = 0                // 0..2, which 4-slot group is shown at center
)
