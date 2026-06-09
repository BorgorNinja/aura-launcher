package dev.aura.launcher.ui

import dev.aura.launcher.widget.SafeAppWidgetHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.aura.launcher.ui.drawer.DrawerScreen
import dev.aura.launcher.ui.home.AuraEvent
import dev.aura.launcher.ui.home.AuraUiState
import dev.aura.launcher.ui.home.HomeTab
import dev.aura.launcher.ui.navigation.NavigationTab
import dev.aura.launcher.ui.settings.SettingsScreen
import dev.aura.launcher.ui.widgets.WidgetDashboardScreen
import dev.aura.launcher.widget.LocalWidgetHost

private val TAB_ORDER = listOf(
    NavigationTab.HOME,
    NavigationTab.WIDGETS,
    NavigationTab.APPS,
    NavigationTab.SETTINGS
)

private val NAV_ITEMS = listOf(
    Triple(NavigationTab.HOME,     Icons.Default.Home,     "Home"),
    Triple(NavigationTab.WIDGETS,  Icons.Default.Widgets,  "Widgets"),
    Triple(NavigationTab.APPS,     Icons.Default.Apps,     "Drawer"),
    Triple(NavigationTab.SETTINGS, Icons.Default.Settings, "Settings")
)

@Composable
fun MainScreen(
    state:       AuraUiState,
    onEvent:     (AuraEvent) -> Unit,
    widgetHost:  SafeAppWidgetHost?,
    onAddWidget: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = TAB_ORDER.indexOf(state.selectedTab).coerceAtLeast(0)
    ) { TAB_ORDER.size }

    LaunchedEffect(state.selectedTab) {
        val page = TAB_ORDER.indexOf(state.selectedTab).coerceAtLeast(0)
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onEvent(AuraEvent.SelectTab(TAB_ORDER[page]))
        }
    }

    // Dock nav bar visible only on HOME — hidden on Drawer, Widgets, and Settings
    val showNav = state.selectedTab == NavigationTab.HOME

    CompositionLocalProvider(LocalWidgetHost provides widgetHost) {
        Box(modifier = Modifier.fillMaxSize()) {

            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (TAB_ORDER[page]) {
                    NavigationTab.HOME     -> HomeTab(state = state, onEvent = onEvent)
                    NavigationTab.WIDGETS  -> WidgetDashboardScreen(state = state, onEvent = onEvent, onAddWidget = onAddWidget)
                    NavigationTab.APPS     -> DrawerScreen(state = state, onEvent = onEvent)
                    NavigationTab.SETTINGS -> SettingsScreen(state = state, onEvent = onEvent)
                }
            }

            AnimatedVisibility(
                visible  = showNav,
                enter    = slideInVertically { it } + fadeIn(),
                exit     = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 8.dp,
                    modifier       = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    NAV_ITEMS.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = state.selectedTab == tab,
                            onClick  = { onEvent(AuraEvent.SelectTab(tab)) },
                            icon     = { Icon(icon, contentDescription = null) },
                            label    = { Text(label) },
                            colors   = NavigationBarItemDefaults.colors(
                                selectedIconColor   = MaterialTheme.colorScheme.primary,
                                selectedTextColor   = MaterialTheme.colorScheme.primary,
                                indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
