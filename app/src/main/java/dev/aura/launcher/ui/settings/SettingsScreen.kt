package dev.aura.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aura.launcher.ui.home.AuraEvent
import dev.aura.launcher.ui.home.AuraUiState

@Composable
fun SettingsScreen(
    state:   AuraUiState,
    onEvent: (AuraEvent) -> Unit
) {
    var showGestures by remember { mutableStateOf(false) }

    BackHandler(enabled = showGestures) { showGestures = false }

    AnimatedContent(
        targetState = showGestures,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
            } else {
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { gestures ->
        if (gestures) {
            GestureSettingsScreen(
                state   = state,
                onEvent = onEvent,
                onBack  = { showGestures = false }
            )
        } else {
            SettingsContent(
                state             = state,
                onEvent           = onEvent,
                onOpenGestures    = { showGestures = true }
            )
        }
    }
}

@Composable
private fun SettingsContent(
    state:          AuraUiState,
    onEvent:        (AuraEvent) -> Unit,
    onOpenGestures: () -> Unit
) {
    val settings = state.settings

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── Header ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Palette,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Launcher Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // ── Hero card ────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            "Personalize your space",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tailor your home screen experience\nwith advanced Material You styling.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── APPEARANCE ───────────────────────────────────────────
            SectionHeader("APPEARANCE")

            SettingsGroup {
                SettingsRow(
                    icon     = Icons.Default.DarkMode,
                    title    = "Theme",
                    subtitle = when (settings.darkThemeMode) {
                        "light" -> "Light"
                        "dark"  -> "Dark"
                        else    -> "System default"
                    },
                    onClick  = {
                        val next = when (settings.darkThemeMode) {
                            "system" -> "light"
                            "light"  -> "dark"
                            else     -> "system"
                        }
                        onEvent(AuraEvent.SetDarkTheme(next))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsRow(
                    icon     = Icons.Default.Palette,
                    title    = "Icon Pack",
                    subtitle = if (settings.iconPackPackage.isEmpty()) "Material You (Dynamic)" else settings.iconPackPackage,
                    onClick  = { /* TODO: icon pack picker */ }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── LAYOUT & FEEL ────────────────────────────────────────
            SectionHeader("LAYOUT & FEEL")

            SettingsGroup {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.GridView, null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Home Screen Grid", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${settings.gridColumns} columns",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                    Slider(
                        value         = settings.gridColumns.toFloat(),
                        onValueChange = { onEvent(AuraEvent.SetGridColumns(it.toInt())) },
                        valueRange    = 3f..6f,
                        steps         = 2,
                        modifier      = Modifier.padding(top = 8.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                // Gestures row — fully functional now
                SettingsRow(
                    icon     = Icons.Default.TouchApp,
                    title    = "Gestures",
                    subtitle = buildString {
                        val sd = when (settings.swipeDownAction) {
                            "notifications" -> "Swipe ↓ Notifications"
                            "camera"        -> "Swipe ↓ Camera"
                            "assistant"     -> "Swipe ↓ Assistant"
                            else            -> "Swipe ↓ Off"
                        }
                        val dt = when (settings.doubleTapAction) {
                            "clock"     -> "· 2× Clock"
                            "camera"    -> "· 2× Camera"
                            "assistant" -> "· 2× Assistant"
                            else        -> ""
                        }
                        append(sd)
                        if (dt.isNotEmpty()) append("  $dt")
                    },
                    onClick  = onOpenGestures
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                SettingsRowToggle(
                    icon     = Icons.Default.Notifications,
                    title    = "Notification Dots",
                    subtitle = "Show indicators on app icons",
                    checked  = settings.notificationDots,
                    onToggle = { onEvent(AuraEvent.SetNotifDots(it)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── SYSTEM ───────────────────────────────────────────────
            SectionHeader("SYSTEM")

            SettingsGroup {
                SettingsRow(
                    icon     = Icons.Default.Info,
                    title    = "About",
                    subtitle = "Aura Launcher 1.0.0",
                    onClick  = {}
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onEvent(AuraEvent.PickWallpaper) }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Change Wallpaper",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to open the system wallpaper picker.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall.copy(
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) { content() }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Text("›", style = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun SettingsRowToggle(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
