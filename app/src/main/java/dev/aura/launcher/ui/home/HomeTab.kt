package dev.aura.launcher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aura.launcher.data.model.AppInfo
import dev.aura.launcher.ui.navigation.NavigationTab
import dev.aura.launcher.ui.util.rememberAppIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeTab(state: AuraUiState, onEvent: (AuraEvent) -> Unit) {
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart    = { dragY = 0f },
                    onDragEnd      = {
                        if (dragY < -80f) onEvent(AuraEvent.SelectTab(NavigationTab.APPS))
                        dragY = 0f
                    },
                    onVerticalDrag = { _, delta -> dragY += delta }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            SearchPill(onClick = { onEvent(AuraEvent.SelectTab(NavigationTab.APPS)) },
                       modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.weight(1f))
            ClockBlock()
            Spacer(Modifier.weight(1f))
            DockRow(
                apps        = state.dockApps,
                onLaunch    = { onEvent(AuraEvent.Launch(it)) },
                onLongPress = { onEvent(AuraEvent.PickWallpaper) }
            )
        }
    }
}

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
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            Text("Search apps & web",
                style    = MaterialTheme.typography.bodyLarge,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp))
        }
    }
}

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockRow(apps: List<AppInfo>, onLaunch: (String) -> Unit, onLongPress: () -> Unit) {
    Surface(
        shape          = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color          = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        tonalElevation = 4.dp,
        modifier       = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()   // clears system nav bar
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 72.dp)   // clear the floating bottom nav bar
        ) {
            apps.forEach { app -> DockIcon(app = app, onClick = { onLaunch(app.packageName) }) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockIcon(app: AppInfo, onClick: () -> Unit) {
    val icon = rememberAppIcon(app.packageName)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick).padding(4.dp)
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(52.dp))
        } else {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Spacer(Modifier.height(4.dp))
        Text(app.label, style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
