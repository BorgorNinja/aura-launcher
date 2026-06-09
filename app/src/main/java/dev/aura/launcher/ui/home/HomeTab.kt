package dev.aura.launcher.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aura.launcher.data.model.AppInfo
import dev.aura.launcher.service.LockScreenService
import dev.aura.launcher.ui.navigation.NavigationTab
import dev.aura.launcher.ui.util.rememberAppIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures

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
            context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        "clock"         -> runCatching {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        "assistant"     -> runCatching {
            context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
                detectTapGestures(onDoubleTap = { executeGestureAction(context, settings.doubleTapAction) })
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

            val pm     = LocalContext.current.packageManager
            val groups = remember(state.dockSlots) {
                state.dockSlots.chunked(4).let { c ->
                    // Ensure exactly 3 groups
                    List(3) { i -> if (i < c.size) c[i] else List(4) { null } }
                }
            }

            DockBelt(
                groups                = groups,
                activeGroupIdx        = settings.activeDockGroup,
                pendingAdd            = state.pendingDockAdd,
                pm                    = pm,
                onLaunch              = { onEvent(AuraEvent.Launch(it)) },
                onRemoveFromDock      = { onEvent(AuraEvent.RemoveFromDock(it)) },
                onSlotTap             = { onEvent(AuraEvent.PlaceDockApp(it)) },
                onLongPressBackground = { onEvent(AuraEvent.PickWallpaper) },
                onRotateDockGroup     = { onEvent(AuraEvent.RotateDockGroup(it)) }
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
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("Search apps & web", style = MaterialTheme.typography.bodyLarge,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Clock ────────────────────────────────────────────────────────────────────

@Composable
private fun ClockBlock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000L); now = Date() } }
    val timeFmt = remember { SimpleDateFormat("HH:mm",       Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(timeFmt.format(now), style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Light, fontSize = 80.sp, color = Color.White))
        Text(dateFmt.format(now), style = MaterialTheme.typography.titleMedium.copy(
            color = Color.White.copy(alpha = 0.8f)))
    }
}

// ─── DockBelt ─────────────────────────────────────────────────────────────────
//
//  Visual layout (at rest):
//
//  [L0]                         [R0]   ← mini strip icons (26dp, stacked)
//  [L1]                         [R1]
//  [L2]                         [R2]
//  [L3] [── main dock ──────] [R3]   ← slot-3 of each strip aligns with main dock
//
//  Horizontal swipe rotates the belt: icons animate between strip ↔ main positions.

private data class BeltIconState(
    val xPx: Int,
    val yPx: Int,
    val sizeDp: Float,   // in dp
    val alpha: Float
)

/** Compute where icon [slotIdx] should be, given its current virtual belt position [virtPos].
 *  virtPos = -1 → left strip, 0 → main dock (center), +1 → right strip.
 *  Values outside [-1,+1] fade out as the icon exits the visible area. */
private fun computeBeltIconState(
    slotIdx: Int,
    virtPos: Float,
    totalWidthPx: Int,
    density: Density
): BeltIconState {
    fun Float.dpToPx() = with(density) { this@dpToPx.dp.toPx() }

    val fullSzPx  = 52f.dpToPx()
    val miniSzPx  = 26f.dpToPx()
    val spacingPx = 30f.dpToPx()   // y-increment per mini slot (26dp icon + 4dp gap)
    val stripWPx  = 38f.dpToPx()   // total width of each side strip
    val padPx     = 6f.dpToPx()    // icon inset from strip outer edge
    val mainYPx   = (3 * 30f).dpToPx()   // = 90dp — main dock icon y in belt box

    val slotW = (totalWidthPx - 2 * stripWPx) / 4f   // width allocated per main-dock icon

    // Anchor positions
    val cX = stripWPx + slotIdx * slotW + (slotW - fullSzPx) / 2f   // center → main dock
    val cY = mainYPx
    val lX = padPx                                                    // left strip
    val lY = slotIdx * spacingPx
    val rX = totalWidthPx - padPx - miniSzPx                         // right strip
    val rY = slotIdx * spacingPx

    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    val v = virtPos.coerceIn(-1.6f, 1.6f)

    return when {
        v < -1f -> BeltIconState(
            lX.roundToInt(), lY.roundToInt(), 26f,
            alpha = (1f - (-v - 1f) / 0.6f).coerceIn(0f, 1f)
        )
        v < 0f -> {
            val t = -v  // 0 = center, 1 = left
            BeltIconState(
                lerp(cX, lX, t).roundToInt(), lerp(cY, lY, t).roundToInt(),
                lerp(52f, 26f, t), alpha = 1f
            )
        }
        v <= 1f -> {
            val t = v  // 0 = center, 1 = right
            BeltIconState(
                lerp(cX, rX, t).roundToInt(), lerp(cY, rY, t).roundToInt(),
                lerp(52f, 26f, t), alpha = 1f
            )
        }
        else -> BeltIconState(
            rX.roundToInt(), rY.roundToInt(), 26f,
            alpha = (1f - (v - 1f) / 0.6f).coerceIn(0f, 1f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockBelt(
    groups:               List<List<AppInfo?>>,   // exactly 3 groups × 4 slots
    activeGroupIdx:       Int,
    pendingAdd:           AppInfo?,
    pm:                   PackageManager,
    onLaunch:             (String) -> Unit,
    onRemoveFromDock:     (absIdx: Int) -> Unit,
    onSlotTap:            (relIdx: Int) -> Unit,
    onLongPressBackground: () -> Unit,
    onRotateDockGroup:    (delta: Int) -> Unit,
) {
    val density    = LocalDensity.current
    val haptic     = LocalHapticFeedback.current
    val scope      = rememberCoroutineScope()
    val beltOffset = remember { Animatable(0f) }

    // Group indices in belt order: left / center / right
    val leftIdx  = (activeGroupIdx + 2) % 3
    val rightIdx = (activeGroupIdx + 1) % 3

    // Virtual belt position for each group (updated by beltOffset)
    // leftGroup  at virtPos = -1 + beltOffset
    // mainGroup  at virtPos =  0 + beltOffset
    // rightGroup at virtPos = +1 + beltOffset

    // Simple drag velocity estimate
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var dragTotal     by remember { mutableFloatStateOf(0f) }

    fun rotateLeft() {   // swipe left → right group slides to center
        scope.launch {
            beltOffset.animateTo(-1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium))
            onRotateDockGroup(+1)
            beltOffset.snapTo(0f)
        }
    }

    fun rotateRight() {  // swipe right → left group slides to center
        scope.launch {
            beltOffset.animateTo(+1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium))
            onRotateDockGroup(-1)
            beltOffset.snapTo(0f)
        }
    }

    fun snapBack() {
        scope.launch { beltOffset.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)) }
    }

    // Belt content height: 3 mini rows above main dock + main dock icons + label
    val beltContentHeight = 162.dp   // (3×30dp) + 52dp + 4dp + 16dp + 16dp padding

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        dragTotal = 0f
                    },
                    onDragEnd = {
                        val elapsed = (System.currentTimeMillis() - dragStartTime).coerceAtLeast(1L)
                        val velocity = dragTotal / elapsed * 1000f  // px/s
                        when {
                            beltOffset.value < -0.28f || velocity < -900f -> rotateLeft()
                            beltOffset.value > 0.28f  || velocity > 900f  -> rotateRight()
                            else -> snapBack()
                        }
                    },
                    onHorizontalDrag = { _, delta ->
                        dragTotal += delta
                        scope.launch {
                            beltOffset.snapTo(
                                (beltOffset.value + delta / constraints.maxWidth.toFloat())
                                    .coerceIn(-1.25f, 1.25f)
                            )
                        }
                    }
                )
            }
    ) {
        val totalWidthPx = constraints.maxWidth

        Surface(
            shape          = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color          = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 4.dp,
            modifier       = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick     = {},
                    onLongClick = if (pendingAdd == null) onLongPressBackground else null
                )
        ) {
            Column {
                // ── Belt icon canvas ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(beltContentHeight)
                ) {
                    // Render all 3 groups; each icon is placed at its computed position
                    listOf(
                        Triple(groups[leftIdx],       -1f + beltOffset.value,  leftIdx),
                        Triple(groups[activeGroupIdx],  0f + beltOffset.value,  activeGroupIdx),
                        Triple(groups[rightIdx],       +1f + beltOffset.value,  rightIdx),
                    ).forEach { (slots, virtPos, groupIdx) ->
                        slots.forEachIndexed { slotIdx, app ->
                            val bState = computeBeltIconState(slotIdx, virtPos, totalWidthPx, density)
                            if (bState.alpha > 0.02f) {
                                BeltIcon(
                                    app            = app,
                                    pm             = pm,
                                    bState         = bState,
                                    isActiveGroup  = abs(virtPos) < 0.45f,
                                    pendingAdd     = pendingAdd,
                                    absIdx         = groupIdx * 4 + slotIdx,
                                    relIdx         = slotIdx,
                                    onLaunch       = { app?.let { onLaunch(it.packageName) } },
                                    onSlotTap      = { onSlotTap(slotIdx) },
                                    onRemove       = { onRemoveFromDock(groupIdx * 4 + slotIdx) },
                                    onLongClick    = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    onTapMiniLeft  = ::rotateRight,
                                    onTapMiniRight = ::rotateLeft
                                )
                            }
                        }
                    }

                    // ── Dot indicators ───────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    ) {
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == activeGroupIdx) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == activeGroupIdx)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }

                // ── Nav bar clearance ─────────────────────────────────────────
                Spacer(modifier = Modifier.navigationBarsPadding().height(76.dp))
            }
        }
    }
}

// ─── Individual belt icon ─────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BeltIcon(
    app:            AppInfo?,
    pm:             PackageManager,
    bState:         BeltIconState,
    isActiveGroup:  Boolean,    // true when virtPos ≈ 0 (in main dock position)
    pendingAdd:     AppInfo?,
    absIdx:         Int,
    relIdx:         Int,
    onLaunch:       () -> Unit,
    onSlotTap:      () -> Unit,
    onRemove:       () -> Unit,
    onLongClick:    () -> Unit,
    onTapMiniLeft:  () -> Unit,
    onTapMiniRight: () -> Unit,
) {
    val sizeDp      = bState.sizeDp.dp
    val isMini      = bState.sizeDp < 42f
    val showLabel   = bState.sizeDp > 45f
    val icon        = if (app != null) rememberAppIcon(app.packageName, pm) else null
    var showMenu by remember { mutableStateOf(false) }

    val clickModifier = when {
        // Tapping a mini strip icon rotates the belt toward that side
        isMini && bState.xPx < 100 -> Modifier.clickable { onTapMiniLeft() }
        isMini                      -> Modifier.clickable { onTapMiniRight() }
        // Placement mode: tap vacant main-dock slot
        pendingAdd != null && isActiveGroup && app == null ->
            Modifier.clickable { onSlotTap() }
        // Normal tap/long-press on occupied main-dock slot
        pendingAdd == null && app != null && isActiveGroup ->
            Modifier.combinedClickable(
                onClick     = onLaunch,
                onLongClick = { onLongClick(); showMenu = true }
            )
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(bState.xPx, bState.yPx) }
            .size(sizeDp)
            .alpha(bState.alpha)
            .then(clickModifier)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(sizeDp)
            ) {
                when {
                    icon != null ->
                        Image(icon, app?.label ?: "", modifier = Modifier.size(sizeDp))

                    app == null && pendingAdd != null && isActiveGroup -> {
                        // Glowing vacant slot in placement mode
                        Box(
                            modifier = Modifier
                                .size(sizeDp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Add, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Center).size(sizeDp * 0.45f)
                            )
                        }
                    }

                    app == null && isActiveGroup && !isMini -> {
                        // Vacant slot at rest (main dock only)
                        Box(
                            modifier = Modifier
                                .size(sizeDp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
                        )
                    }

                    app == null && isMini -> {
                        // Empty mini strip slot: subtle dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        )
                    }
                }
            }

            if (showLabel && app != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text      = app.label,
                    style     = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.width(sizeDp)
                )
            }
        }

        if (app != null && showMenu) {
            DropdownMenu(
                expanded         = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(text = { Text("Open") },
                    onClick = { showMenu = false; onLaunch() })
                DropdownMenuItem(text = { Text("Remove from Dock") },
                    onClick = { showMenu = false; onRemove() })
            }
        }
    }
}
