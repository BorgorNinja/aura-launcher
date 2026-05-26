package dev.aura.launcher.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aura.launcher.service.LockScreenService
import dev.aura.launcher.ui.home.AuraEvent
import dev.aura.launcher.ui.home.AuraUiState

private data class GestureOption(val value: String, val label: String, val icon: ImageVector)

private val SWIPE_DOWN_OPTIONS = listOf(
    GestureOption("notifications", "Notification shade",  Icons.Default.Notifications),
    GestureOption("lock",          "Lock screen",         Icons.Default.Lock),
    GestureOption("camera",        "Open camera",         Icons.Default.CameraAlt),
    GestureOption("assistant",     "Voice assistant",     Icons.Default.Mic),
    GestureOption("none",          "Do nothing",          Icons.Default.Block)
)

private val DOUBLE_TAP_OPTIONS = listOf(
    GestureOption("lock",          "Lock screen",         Icons.Default.Lock),
    GestureOption("clock",         "Open clock / alarms", Icons.Default.Notifications),
    GestureOption("camera",        "Open camera",         Icons.Default.CameraAlt),
    GestureOption("assistant",     "Voice assistant",     Icons.Default.Mic),
    GestureOption("none",          "Do nothing",          Icons.Default.Block)
)

@Composable
fun GestureSettingsScreen(
    state:   AuraUiState,
    onEvent: (AuraEvent) -> Unit,
    onBack:  () -> Unit
) {
    val context     = LocalContext.current
    val lockEnabled = LockScreenService.isEnabled(context)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Gesture Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Accessibility banner — only shown when lock service isn't enabled
            if (!lockEnabled) {
                Card(
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null,
                                tint     = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Lock Screen Gesture",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onTertiaryContainer))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "To use the Lock Screen gesture, enable Aura Launcher in Accessibility Settings.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f))
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { LockScreenService.openSettings(context) }) {
                            Text("Open Accessibility Settings")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Swipe Down section
            GestureSection(
                icon     = Icons.Default.ExpandMore,
                title    = "Swipe Down on Home",
                subtitle = "What happens when you swipe down on the home screen"
            ) {
                SWIPE_DOWN_OPTIONS.forEach { option ->
                    GestureOptionRow(
                        option   = option,
                        selected = state.settings.swipeDownAction == option.value,
                        onClick  = { onEvent(AuraEvent.SetSwipeDownAction(option.value)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Double Tap section
            GestureSection(
                icon     = Icons.Default.TouchApp,
                title    = "Double Tap Home Screen",
                subtitle = "What happens when you double-tap an empty area"
            ) {
                DOUBLE_TAP_OPTIONS.forEach { option ->
                    GestureOptionRow(
                        option   = option,
                        selected = state.settings.doubleTapAction == option.value,
                        onClick  = { onEvent(AuraEvent.SetDoubleTapAction(option.value)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Always-on info card
            Card(
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.KeyboardArrowDown, null,
                            tint     = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Navigation Gestures",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSecondaryContainer))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Swipe Up → opens the app drawer\nSwipe Left / Right → switches between sections",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureSection(
    icon:    ImageVector,
    title:   String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) { content() }
    }
}

@Composable
private fun GestureOptionRow(
    option:   GestureOption,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Icon(option.icon, null,
            tint     = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(option.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color      = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurface))
    }
}
