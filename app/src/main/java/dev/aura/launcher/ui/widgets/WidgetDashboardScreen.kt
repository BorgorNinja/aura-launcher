package dev.aura.launcher.ui.widgets

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.aura.launcher.ui.home.AuraEvent
import dev.aura.launcher.ui.home.AuraUiState
import dev.aura.launcher.widget.LocalWidgetHost
import dev.aura.launcher.widget.SafeAppWidgetHostView

@Composable
fun WidgetDashboardScreen(
    state:       AuraUiState,
    onEvent:     (AuraEvent) -> Unit,
    onAddWidget: () -> Unit
) {
    val host    = LocalWidgetHost.current
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 100.dp)
            ) {
                Text(
                    "Widgets",
                    style    = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                if (state.widgetIds.isEmpty() || host == null) {
                    WidgetEmptyState(onAdd = onAddWidget)
                } else {
                    state.widgetIds.forEach { widgetId ->
                        val info = runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull()
                        if (info != null) {
                            HostedWidget(
                                widgetId  = widgetId,
                                minHeight = info.minHeight.coerceIn(80, 400),
                                onRemove  = {
                                    runCatching { host.deleteAppWidgetId(widgetId) }
                                    onEvent(AuraEvent.RemoveWidget(widgetId))
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick        = onAddWidget,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier       = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add widget")
            }
        }
    }
}

// ─── Hosted widget card ───────────────────────────────────────────────────────

@Composable
private fun HostedWidget(
    widgetId:  Int,
    minHeight: Int,
    onRemove:  () -> Unit
) {
    val host = LocalWidgetHost.current ?: return
    var createFailed by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (createFailed) {
        WidgetErrorCard(onRemove = onRemove)
        return
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            AndroidView<android.view.View>(
                factory = { ctx ->
                    try {
                        val info = AppWidgetManager.getInstance(ctx).getAppWidgetInfo(widgetId)
                        // Use SafeAppWidgetHostView — its onCreateView override returns this
                        // subclass which wraps updateAppWidget() in try-catch, preventing
                        // RemoteViews inflation crashes (custom views, large bitmaps, etc.)
                        // from propagating up to kill the launcher Activity.
                        (host.createView(ctx, widgetId, info) as SafeAppWidgetHostView).also {
                            it.onError = { createFailed = true }
                        }
                    } catch (e: Exception) {
                        createFailed = true
                        android.view.View(ctx)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(minHeight.dp)
                    .padding(8.dp)
            )
        }

        IconButton(
            onClick  = { showRemoveDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = 4.dp)
                .size(32.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove widget",
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title            = { Text("Remove widget?") },
            text             = { Text("This widget will be removed from your dashboard.") },
            confirmButton    = {
                TextButton(onClick = { showRemoveDialog = false; onRemove() }) { Text("Remove") }
            },
            dismissButton    = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Error card ───────────────────────────────────────────────────────────────

@Composable
private fun WidgetErrorCard(onRemove: () -> Unit) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ErrorOutline, null,
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Widget unavailable",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Text("This widget could not be loaded.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun WidgetEmptyState(onAdd: () -> Unit) {
    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.fillMaxWidth().padding(40.dp)
        ) {
            Icon(Icons.Default.Widgets, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("No widgets yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Tap + to add widgets to your dashboard.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant))
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAdd) { Text("Add widget") }
        }
    }
}
