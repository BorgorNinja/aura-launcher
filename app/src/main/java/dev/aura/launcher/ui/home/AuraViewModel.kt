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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── State ───────────────────────────────────────────────────────────────────

data class AuraUiState(
    val selectedTab:   NavigationTab = NavigationTab.HOME,
    val apps:          List<AppInfo> = emptyList(),
    val dockApps:      List<AppInfo> = emptyList(),
    val searchQuery:   String        = "",
    val searchResults: List<AppInfo> = emptyList(),
    val isSearching:   Boolean       = false,
    val settings:      AuraSettings  = AuraSettings(),
    val widgetIds:     List<Int>     = emptyList()
)

// ─── Side effects ─────────────────────────────────────────────────────────────

sealed interface SideEffect {
    data object PickWallpaper                         : SideEffect
    data class  Uninstall(val packageName: String)    : SideEffect
    data object PickWidget                            : SideEffect
}

// ─── Events ──────────────────────────────────────────────────────────────────

sealed interface AuraEvent {
    data class SelectTab(val tab: NavigationTab)       : AuraEvent
    data class Search(val query: String)               : AuraEvent
    data class Launch(val packageName: String)         : AuraEvent
    data class Uninstall(val packageName: String)      : AuraEvent
    data class ShowAppInfo(val packageName: String)    : AuraEvent
    data class SetGridColumns(val columns: Int)        : AuraEvent
    data class SetDarkTheme(val mode: String)          : AuraEvent
    data class SetNotifDots(val enabled: Boolean)      : AuraEvent
    data class AddWidget(val id: Int)                  : AuraEvent
    data class RemoveWidget(val id: Int)               : AuraEvent
    data object ClearSearch                            : AuraEvent
    data object PickWallpaper                          : AuraEvent
    data object PickWidget                             : AuraEvent
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@OptIn(FlowPreview::class)
class AuraViewModel(app: Application) : AndroidViewModel(app) {

    private val appRepo      = AppRepository(app)
    private val settingsRepo = SettingsRepository(app)

    private val _state       = MutableStateFlow(AuraUiState())
    val state: StateFlow<AuraUiState> = _state

    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects: SharedFlow<SideEffect> = _sideEffects.asSharedFlow()

    private val _query = MutableStateFlow("")

    init {
        appRepo.allApps.onEach { list ->
            _state.update { it.copy(
                apps     = list,
                dockApps = list.sortedByDescending { a -> a.launchCount }.take(4)
            )}
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
            is AuraEvent.SelectTab    -> _state.update { it.copy(selectedTab = event.tab) }
            is AuraEvent.Search       -> {
                _query.value = event.query
                _state.update { it.copy(searchQuery = event.query, isSearching = event.query.isNotBlank()) }
            }
            AuraEvent.ClearSearch     -> {
                _query.value = ""
                _state.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
            }
            is AuraEvent.Launch       -> viewModelScope.launch { appRepo.launch(getApplication(), event.packageName) }
            is AuraEvent.Uninstall    -> viewModelScope.launch { _sideEffects.emit(SideEffect.Uninstall(event.packageName)) }
            is AuraEvent.ShowAppInfo  -> viewModelScope.launch { appRepo.openAppInfo(getApplication(), event.packageName) }
            AuraEvent.PickWallpaper   -> viewModelScope.launch { _sideEffects.emit(SideEffect.PickWallpaper) }
            AuraEvent.PickWidget      -> viewModelScope.launch { _sideEffects.emit(SideEffect.PickWidget) }
            is AuraEvent.AddWidget    -> viewModelScope.launch { settingsRepo.addWidgetId(event.id) }
            is AuraEvent.RemoveWidget -> viewModelScope.launch { settingsRepo.removeWidgetId(event.id) }
            is AuraEvent.SetGridColumns -> viewModelScope.launch { settingsRepo.setGridColumns(event.columns) }
            is AuraEvent.SetDarkTheme   -> viewModelScope.launch { settingsRepo.setDarkThemeMode(event.mode) }
            is AuraEvent.SetNotifDots   -> viewModelScope.launch { settingsRepo.setNotificationDots(event.enabled) }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AuraViewModel(app) as T
    }
}
