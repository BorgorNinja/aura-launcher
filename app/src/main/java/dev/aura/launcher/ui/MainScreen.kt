package dev.aura.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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

@Composable
fun MainScreen(state: AuraUiState, onEvent: (AuraEvent) -> Unit, onAddWidget: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedContent(
            targetState  = state.selectedTab,
            transitionSpec = {
                when {
                    // Swipe up from home → drawer slides up
                    initialState == NavigationTab.HOME && targetState == NavigationTab.APPS ->
                        slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it / 4 } + fadeOut()
                    // Drawer closes → slides back down
                    initialState == NavigationTab.APPS && targetState == NavigationTab.HOME ->
                        slideInVertically { -it / 4 } + fadeIn() togetherWith
                        slideOutVertically { it } + fadeOut()
                    else ->
                        fadeIn() togetherWith fadeOut()
                }
            },
            label = "tab_transition"
        ) { tab ->
            when (tab) {
                NavigationTab.HOME     -> HomeTab(state = state, onEvent = onEvent)
                NavigationTab.WIDGETS  -> WidgetDashboardScreen(state = state, onEvent = onEvent, onAddWidget = onAddWidget)
                NavigationTab.APPS     -> DrawerScreen(state = state, onEvent = onEvent)
                NavigationTab.SETTINGS -> SettingsScreen(state = state, onEvent = onEvent)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 8.dp,
                modifier       = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            ) {
                listOf(
                    Triple(NavigationTab.HOME,     Icons.Default.Home,     "Home"),
                    Triple(NavigationTab.WIDGETS,  Icons.Default.Widgets,  "Widgets"),
                    Triple(NavigationTab.APPS,     Icons.Default.Apps,     "Drawer"),
                    Triple(NavigationTab.SETTINGS, Icons.Default.Settings, "Settings")
                ).forEach { (tab, icon, label) ->
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
