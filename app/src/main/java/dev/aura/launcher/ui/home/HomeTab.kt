package dev.aura.launcher.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aura.launcher.data.model.AppInfo
import dev.aura.launcher.service.LockScreenService
import dev.aura.launcher.ui.navigation.NavigationTab
import dev.aura.launcher.ui.util.rememberAppIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Gesture action executor ──────────────────────────────────────────────────

@SuppressLint("WrongConstant")
fun expandNotificationPanel(context: Context) {
    runCatching {
        val service = context.getSystemService("statusbar")
        service?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(service)
    }
}

fun executeGestureAction(context: Context, action: String) {
    when (action) {
        "notifications" -> expandNotificationPanel(context)
        "lock"          -> LockScreenService.lock()
        "camera"        -> runCatching {
            context.startActivity(
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        "clock"     -> runCatching {
            context.startActivity(
                Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        "assistant" -> runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VOICE_COMMAND)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

// ─── HomeTab ──────────────────────────────────────────────────────────────────

@Composable
fun HomeTab(state: AuraUiState, onEvent: (AuraEvent) -> Unit) {
    val context  = LocalContext.current
    val settings = state.settings
    var dragY by remember { mutableFloatStateOf(0f) }
    val inPlacementMode = state.pendingDockAdd != null

    // Back press during placement mode cancels the dock-add flow
    BackHandler(enabled = inPlacementMode) { onEvent(AuraEvent.CancelDockAdd) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput("swipe") {
                detectVerticalDragGestures(
                    onDragStart    = { dragY = 0f },
                    onDragEnd      = {
                        when {
                            dragY < -80f -> onEvent(AuraEvent.SelectTab(NavigationTab.APPS))
                            dragY > 80f  -> executeGestureAction(context, settings.swipeDownAction)
                        }
                        dragY = 0f
                    },
                    onVerticalDrag = { _, delta -> dragY += delta }
                )
            }
            .pointerInput("doubletap") {
                detectTapGestures(
                    onDoubleTap = { executeGestureAction(context, settings.doubleTapAction) }
                )
            }
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // Search pill — tapping opens the drawer
            if (!inPlacementMode) {
                SearchPill(
                    onClick  = { onEvent(AuraEvent.SelectTab(NavigationTab.APPS)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Placement mode banner
            if (inPlacementMode) {
                PlacementBanner(appName = state.pendingDockAdd?.label ?: "")
                Spacer(Modifier.height(12.dp))
            } else {
                ClockBlock()
            }

            Spacer(Modifier.weight(1f))

            DockRow(
                slots              = state.dockSlots,
                pendingAdd         = state.pendingDockAdd,
                onLaunch           = { onEvent(AuraEvent.Launch(it)) },
                onRemoveFromDock   = { onEvent(AuraEvent.RemoveFromDock(it)) },
                onSlotTap          = { onEvent(AuraEvent.PlaceDockApp(it)) },
                onLongPressBackground = { onEvent(AuraEvent.PickWallpaper) }
            )
        }
    }
}

// ─── Placement banner ─────────────────────────────────────────────────────────

@Composable
private fun PlacementBanner(appName: String) {
    Surface(
        shape          = RoundedCornerShape(20.dp),
        color          = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        modifier       = Modifier.padding(horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Adding "$appName" to Dock",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a glowing slot below to place it,\nor press Back to cancel.",
                style     = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Search pill ──────────────────────────────────────────────────────────────

@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick        = onClick,
        shape          = RoundedCornerShape(50),
        color          = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 2.dp,
        modifier       = modifier.fillMaxWidth().height(52.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier              = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Add, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.padding(start = 8.dp))
            Text("Search apps & web",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Clock ───────────────────────────────────────────────────────────────────

@Composable
private fun ClockBlock() {
    val now  = remember { Date() }
    val time = remember { SimpleDateFormat("HH:mm",       Locale.getDefault()).format(now) }
    val date = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Light, fontSize = 80.sp, color = Color.White))
        Text(date, style = MaterialTheme.typography.titleMedium.copy(
            color = Color.White.copy(alpha = 0.8f)))
    }
}

// ─── Dock row ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockRow(
    slots:                List<AppInfo?>,
    pendingAdd:           AppInfo?,
    onLaunch:             (String) -> Unit,
    onRemoveFromDock:     (Int) -> Unit,
    onSlotTap:            (Int) -> Unit,
    onLongPressBackground: () -> Unit
) {
    Surface(
        shape          = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color          = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        tonalElevation = 4.dp,
        modifier       = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = {},
                onLongClick = if (pendingAdd == null) onLongPressBackground else null
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 72.dp)
        ) {
            slots.forEachIndexed { index, app ->
                if (app != null) {
                    OccupiedDockSlot(
                        app              = app,
                        dimmed           = pendingAdd != null,
                        onClick          = if (pendingAdd == null) { { onLaunch(app.packageName) } } else null,
                        onRemoveFromDock = { onRemoveFromDock(index) }
                    )
                } else {
                    VacantDockSlot(
                        highlighted = pendingAdd != null,
                        onTap       = if (pendingAdd != null) { { onSlotTap(index) } } else null
                    )
                }
            }
        }
    }
}

// ─── Occupied slot ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OccupiedDockSlot(
    app:              AppInfo,
    dimmed:           Boolean,
    onClick:          (() -> Unit)?,
    onRemoveFromDock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val icon = rememberAppIcon(app.packageName)

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick     = { onClick?.invoke() },
                    onLongClick = { showMenu = true }
                )
                .padding(4.dp)
                .graphicsLayer { alpha = if (dimmed) 0.35f else 1f }
        ) {
            if (icon != null) {
                Image(
                    bitmap             = icon,
                    contentDescription = app.label,
                    modifier           = Modifier.size(52.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                app.label,
                style    = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text    = { Text("Open") },
                onClick = { showMenu = false; onClick?.invoke() }
            )
            DropdownMenuItem(
                text    = { Text("Remove from Dock") },
                onClick = { showMenu = false; onRemoveFromDock() }
            )
        }
    }
}

// ─── Vacant slot ──────────────────────────────────────────────────────────────

@Composable
private fun VacantDockSlot(highlighted: Boolean, onTap: (() -> Unit)?) {
    // animateFloat on InfiniteTransition MUST use InfiniteRepeatableSpec.
    // When not in placement mode we skip the animation entirely and use a fixed alpha.
    val pulseSpec = infiniteRepeatable<Float>(
        animation  = tween(600, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse
    )
    val animatedPulse by rememberInfiniteTransition(label = "dock_pulse").animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = pulseSpec,
        label         = "pulse_alpha"
    )
    val pulse = if (highlighted) animatedPulse else 0.3f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .then(
                if (highlighted)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = pulse), CircleShape)
                else
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            )
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
    ) {
        if (highlighted) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Place here",
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
