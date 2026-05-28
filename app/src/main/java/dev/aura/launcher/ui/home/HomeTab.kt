package dev.aura.launcher.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
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
        "clock"         -> runCatching {
            context.startActivity(
                Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        "assistant"     -> runCatching {
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
    val context         = LocalContext.current
    val settings        = state.settings
    var dragY by remember { mutableFloatStateOf(0f) }
    val inPlacementMode = state.pendingDockAdd != null

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

            if (!inPlacementMode) {
                SearchPill(
                    onClick  = { onEvent(AuraEvent.SelectTab(NavigationTab.APPS)) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            if (inPlacementMode) {
                PlacementBanner(appName = state.pendingDockAdd?.label ?: "")
                Spacer(Modifier.height(12.dp))
            } else {
                ClockBlock()
            }

            Spacer(Modifier.weight(1f))

            // Hoist PackageManager here — one LocalContext.current lookup for
            // all dock slots instead of one per OccupiedDockSlot composable.
            val pm = LocalContext.current.packageManager
            DockRow(
                slots                 = state.dockSlots,
                pendingAdd            = state.pendingDockAdd,
                pm                    = pm,
                onLaunch              = { onEvent(AuraEvent.Launch(it)) },
                onRemoveFromDock      = { onEvent(AuraEvent.RemoveFromDock(it)) },
                onSlotTap             = { onEvent(AuraEvent.PlaceDockApp(it)) },
                onLongPressBackground = { onEvent(AuraEvent.PickWallpaper) }
            )
        }
    }
}

// ─── Placement mode banner ────────────────────────────────────────────────────

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
                text  = "Adding \u201c$appName\u201d to Dock",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = "Tap a glowing slot to place it, or press Back to cancel.",
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
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text  = "Search apps & web",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Clock ────────────────────────────────────────────────────────────────────

/**
 * Live clock that updates every second.
 *
 * The `time` and `date` formatters are created once and reused — SimpleDateFormat
 * construction is non-trivial (locale data lookup). The `LaunchedEffect(Unit)`
 * coroutine runs for the lifetime of this composable; `delay(1_000)` yields the
 * thread rather than spinning. Only `ClockBlock` recomposes each tick — the rest
 * of HomeTab is unaffected because the mutableStateOf is scoped here.
 */
@Composable
private fun ClockBlock() {
    var now by remember { mutableStateOf(Date()) }

    // Tick every second. LaunchedEffect(Unit) means this runs once and lives
    // as long as ClockBlock is in the composition.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            now = Date()
        }
    }

    // Formatters are heavy; create once and reuse on each tick.
    val timeFmt = remember { SimpleDateFormat("HH:mm",       Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = timeFmt.format(now),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Light, fontSize = 80.sp, color = Color.White
            )
        )
        Text(
            text  = dateFmt.format(now),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White.copy(alpha = 0.8f)
            )
        )
    }
}

// ─── Dock row ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockRow(
    slots:                List<AppInfo?>,
    pendingAdd:           AppInfo?,
    pm:                   PackageManager,
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
                        pm               = pm,
                        dimmed           = pendingAdd != null,
                        onLaunch         = if (pendingAdd == null) { { onLaunch(app.packageName) } } else null,
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

// ─── Occupied dock slot ───────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OccupiedDockSlot(
    app:              AppInfo,
    pm:               PackageManager,
    dimmed:           Boolean,
    onLaunch:         (() -> Unit)?,
    onRemoveFromDock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val icon     = rememberAppIcon(app.packageName, pm)
    val slotAlpha = if (dimmed) 0.35f else 1f

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick     = { onLaunch?.invoke() },
                    onLongClick = { showMenu = true }
                )
                .padding(4.dp)
                .alpha(slotAlpha)
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
                text      = app.label,
                style     = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded          = showMenu,
            onDismissRequest  = { showMenu = false }
        ) {
            DropdownMenuItem(
                text    = { Text("Open") },
                onClick = { showMenu = false; onLaunch?.invoke() }
            )
            DropdownMenuItem(
                text    = { Text("Remove from Dock") },
                onClick = { showMenu = false; onRemoveFromDock() }
            )
        }
    }
}

// ─── Vacant dock slot ─────────────────────────────────────────────────────────

/**
 * The infinite pulse animation is only created when [highlighted] is true
 * (i.e. the user is in dock-placement mode).  When [highlighted] is false the
 * composable renders a static border with no running animation — no frame
 * callbacks, no recompositions, no GPU work — so the home screen is completely
 * idle when the user is just looking at it normally.
 */
@Composable
private fun VacantDockSlot(highlighted: Boolean, onTap: (() -> Unit)?) {
    // Conditionally start the infinite transition only when it is actually
    // needed.  Compose allows conditional composable calls; the slot table
    // entry is recycled when highlighted toggles, which is fine here.
    val borderAlpha: Float
    val borderWidth = if (highlighted) 2.dp else 1.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    if (highlighted) {
        val infiniteTransition = rememberInfiniteTransition(label = "dock_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue  = 0.4f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        borderAlpha = pulseAlpha

        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(borderWidth, primaryColor.copy(alpha = borderAlpha), CircleShape)
                .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Place here",
                tint               = primaryColor.copy(alpha = borderAlpha),
                modifier           = Modifier.size(22.dp)
            )
        }
    } else {
        // Static slot — no animation running, no frame callbacks scheduled.
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(borderWidth, outlineColor.copy(alpha = 0.3f), CircleShape)
        )
    }
}
