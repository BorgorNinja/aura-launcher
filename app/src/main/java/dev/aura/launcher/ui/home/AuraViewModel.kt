package dev.aura.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.aura.launcher.data.model.AppInfo
import dev.aura.launcher.data.model.AuraSettings
import dev.aura.launcher.data.repository.AppRepository
import dev.aura.launcher.data.repository.SettingsRepository
import dev.aura.launcher.ui.navigation.NavigationTab
import dev.aura.launcher.util.IconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── State ───────────────────────────────────────────────────────────────────

data class AuraUiState(
    val selectedTab:    NavigationTab    = NavigationTab.HOME,
    val apps:           List<AppInfo>    = emptyList(),
    val dockSlots:      List<AppInfo?>   = listOf(null, null, null, null),
    val pendingDockAdd: AppInfo?         = null,    // app waiting to be placed in a dock slot
    val searchQuery:    String           = "",
    val searchResults:  List<AppInfo>    = emptyList(),
    val isSearching:    Boolean          = false,
    val settings:       AuraSettings     = AuraSettings(),
    val widgetIds:      List<Int>        = emptyList()
)

// ─── Side effects ─────────────────────────────────────────────────────────────

sealed interface SideEffect {
    data object PickWallpaper : SideEffect
    data object PickWidget    : SideEffect
}

// ─── Events ──────────────────────────────────────────────────────────────────

sealed interface AuraEvent {
    data class SelectTab(val tab: NavigationTab)        : AuraEvent
    data class Search(val query: String)                : AuraEvent
    data class Launch(val packageName: String)          : AuraEvent
    data class Uninstall(val packageName: String)       : AuraEvent
    data class ShowAppInfo(val packageName: String)     : AuraEvent
    data class SetGridColumns(val columns: Int)         : AuraEvent
    data class SetDarkTheme(val mode: String)           : AuraEvent
    data class SetNotifDots(val enabled: Boolean)       : AuraEvent
    data class AddWidget(val id: Int)                   : AuraEvent
    data class RemoveWidget(val id: Int)                : AuraEvent
    data class SetSwipeDownAction(val action: String)   : AuraEvent
    data class SetDoubleTapAction(val action: String)   : AuraEvent
    // Dock management
    data class StartDockAdd(val app: AppInfo)           : AuraEvent  // from drawer long-press
    data class PlaceDockApp(val slotIndex: Int)         : AuraEvent  // tap vacant slot
    data class RemoveFromDock(val slotIndex: Int)       : AuraEvent
    data object CancelDockAdd                           : AuraEvent
    data object ClearSearch                             : AuraEvent
    data object PickWallpaper                           : AuraEvent
    data object PickWidget                              : AuraEvent
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class)
class AuraViewModel(app: Application) : AndroidViewModel(app) {

    private val appRepo      = AppRepository(app)
    private val settingsRepo = SettingsRepository(app)
    private val pm           = app.packageManager

    private val _state       = MutableStateFlow(AuraUiState())
    val state: StateFlow<AuraUiState> = _state

    private val _sideEffects = MutableSharedFlow<SideEffect>(extraBufferCapacity = 1)
    val sideEffects: SharedFlow<SideEffect> = _sideEffects.asSharedFlow()

    private val _query = MutableStateFlow("")

    init {
        // Apps list
        appRepo.allApps.onEach { list ->
            _state.update { it.copy(apps = list) }
            // Pre-warm the icon cache so drawer scrolling is smooth.
            // Runs entirely on Dispatchers.IO — never blocks the main thread.
            viewModelScope.launch(Dispatchers.IO) {
                IconCache.preload(list.map { it.packageName }, pm)
            }
        }.launchIn(viewModelScope)

        // Dock slots — combine app list with saved dock packages so slot
        // objects stay in sync when apps are installed/uninstalled
        combine(appRepo.allApps, settingsRepo.dockSlots) { apps, pkgSlots ->
            val map = apps.associateBy { it.packageName }
            pkgSlots.map { pkg -> if (pkg == null) null else map[pkg] }
        }.onEach { slots ->
            _state.update { it.copy(dockSlots = slots) }
        }.launchIn(viewModelScope)

        settingsRepo.settings.onEach { s ->
            _state.update { it.copy(settings = s) }
        }.launchIn(viewModelScope)

        settingsRepo.widgetIds.onEach { ids ->
            _state.update { it.copy(widgetIds = ids) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _query.debounce(120)
                .flatMapLatest { q -> flow { emit(if (q.isBlank()) emptyList() else appRepo.search(q)) } }
                .collect { results -> _state.update { it.copy(searchResults = results) } }
        }
    }

    fun onEvent(event: AuraEvent) {
        when (event) {
            is AuraEvent.SelectTab          -> {
                // When navigating to Home, clear any active search so the
                // search bar is empty and the app grid is fully restored.
                if (event.tab == NavigationTab.HOME) {
                    _query.value = ""
                    _state.update {
                        it.copy(
                            selectedTab   = event.tab,
                            searchQuery   = "",
                            isSearching   = false,
                            searchResults = emptyList()
                        )
                    }
                } else {
                    _state.update { it.copy(selectedTab = event.tab) }
                }
            }
            is AuraEvent.Search             -> {
                _query.value = event.query
                _state.update { it.copy(searchQuery = event.query, isSearching = event.query.isNotBlank()) }
            }
            AuraEvent.ClearSearch           -> {
                _query.value = ""
                _state.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
            }
            is AuraEvent.Launch             -> viewModelScope.launch { appRepo.launch(getApplication(), event.packageName) }
            is AuraEvent.Uninstall          -> viewModelScope.launch { appRepo.uninstall(getApplication(), event.packageName) }
            is AuraEvent.ShowAppInfo        -> viewModelScope.launch { appRepo.openAppInfo(getApplication(), event.packageName) }
            AuraEvent.PickWallpaper         -> viewModelScope.launch { _sideEffects.emit(SideEffect.PickWallpaper) }
            AuraEvent.PickWidget            -> viewModelScope.launch { _sideEffects.emit(SideEffect.PickWidget) }
            is AuraEvent.AddWidget          -> viewModelScope.launch { settingsRepo.addWidgetId(event.id) }
            is AuraEvent.RemoveWidget       -> viewModelScope.launch { settingsRepo.removeWidgetId(event.id) }
            is AuraEvent.SetGridColumns     -> viewModelScope.launch { settingsRepo.setGridColumns(event.columns) }
            is AuraEvent.SetDarkTheme       -> viewModelScope.launch { settingsRepo.setDarkThemeMode(event.mode) }
            is AuraEvent.SetNotifDots       -> viewModelScope.launch { settingsRepo.setNotificationDots(event.enabled) }
            is AuraEvent.SetSwipeDownAction -> viewModelScope.launch { settingsRepo.setSwipeDownAction(event.action) }
            is AuraEvent.SetDoubleTapAction -> viewModelScope.launch { settingsRepo.setDoubleTapAction(event.action) }
            // Dock
            is AuraEvent.StartDockAdd       -> _state.update {
                it.copy(pendingDockAdd = event.app, selectedTab = NavigationTab.HOME)
            }
            is AuraEvent.PlaceDockApp       -> {
                val pending = _state.value.pendingDockAdd ?: return
                _state.update { it.copy(pendingDockAdd = null) }
                viewModelScope.launch { settingsRepo.setDockSlot(event.slotIndex, pending.packageName) }
            }
            is AuraEvent.RemoveFromDock     -> viewModelScope.launch { settingsRepo.setDockSlot(event.slotIndex, null) }
            AuraEvent.CancelDockAdd         -> _state.update { it.copy(pendingDockAdd = null) }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AuraViewModel(app) as T
    }
}
