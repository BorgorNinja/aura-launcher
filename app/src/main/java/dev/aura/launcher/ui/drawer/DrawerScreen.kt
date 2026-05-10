package dev.aura.launcher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aura.launcher.data.model.AppInfo
import dev.aura.launcher.ui.home.AuraEvent
import dev.aura.launcher.ui.home.AuraUiState
import dev.aura.launcher.ui.util.rememberAppIcon
import kotlinx.coroutines.launch

@Composable
fun DrawerScreen(
    state:         AuraUiState,
    onEvent:       (AuraEvent) -> Unit,
    bottomPadding: androidx.compose.foundation.layout.WindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
) {
    val displayList = if (state.isSearching) state.searchResults else state.apps
    val columns     = state.settings.gridColumns

    // Group for alphabetical index (only when not searching)
    val grouped: Map<Char, List<AppInfo>> = remember(displayList, state.isSearching) {
        if (state.isSearching) emptyMap()
        else displayList.groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
    }

    val letters       = remember(grouped) { grouped.keys.toList() }
    val gridState     = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Map letter → first grid item index (header + items)
    val letterPositions = remember(grouped, columns) {
        val positions = mutableMapOf<Char, Int>()
        var pos = 0
        grouped.forEach { (letter, apps) ->
            positions[letter] = pos
            pos += 1 + apps.size  // 1 header + N apps
        }
        positions
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Search bar ──────────────────────────────────────────
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = { onEvent(AuraEvent.Search(it)) },
                placeholder   = { Text("Search apps…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onEvent(AuraEvent.ClearSearch) }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(50),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedBorderColor    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Grid + Alpha index ───────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns        = GridCells.Fixed(columns),
                    state          = gridState,
                    contentPadding = PaddingValues(
                        start  = 8.dp,
                        end    = 28.dp,  // leave room for alpha index
                        top    = 4.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isSearching) {
                        items(displayList, key = { it.packageName }) { app ->
                            AppGridItem(
                                app         = app,
                                onClick     = { onEvent(AuraEvent.Launch(app.packageName)) },
                                onUninstall = { onEvent(AuraEvent.Uninstall(app.packageName)) },
                                onAppInfo   = { onEvent(AuraEvent.ShowAppInfo(app.packageName)) }
                            )
                        }
                    } else {
                        grouped.forEach { (letter, apps) ->
                            // Section header — spans full row
                            item(
                                key  = "header_$letter",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Text(
                                    text     = letter.toString(),
                                    style    = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(apps, key = { it.packageName }) { app ->
                                AppGridItem(
                                    app         = app,
                                    onClick     = { onEvent(AuraEvent.Launch(app.packageName)) },
                                    onUninstall = { onEvent(AuraEvent.Uninstall(app.packageName)) },
                                    onAppInfo   = { onEvent(AuraEvent.ShowAppInfo(app.packageName)) }
                                )
                            }
                        }
                    }
                }

                // ── Alphabetical scroll index (right side) ────────────
                if (!state.isSearching && letters.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(24.dp)
                            .padding(vertical = 8.dp)
                            .pointerInput(letters) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.y / size.height).coerceIn(0f, 1f)
                                    val idx      = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
                                    val pos      = letterPositions[letters[idx]] ?: return@detectTapGestures
                                    coroutineScope.launch { gridState.animateScrollToItem(pos) }
                                }
                            },
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        letters.forEach { letter ->
                            Text(
                                text  = letter.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── App Grid Item ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app:        AppInfo,
    onClick:    () -> Unit,
    onUninstall: () -> Unit,
    onAppInfo:   () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val icon: ImageBitmap? = rememberAppIcon(app.packageName)

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
                .padding(8.dp)
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
                style     = MaterialTheme.typography.labelSmall,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded         = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(text = { Text("Open")     }, onClick = { menuExpanded = false; onClick() })
            DropdownMenuItem(text = { Text("App info") }, onClick = { menuExpanded = false; onAppInfo() })
            DropdownMenuItem(text = { Text("Uninstall")}, onClick = { menuExpanded = false; onUninstall() })
        }
    }
}
