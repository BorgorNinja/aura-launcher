package dev.aura.launcher

import android.Manifest
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.aura.launcher.ui.MainScreen
import dev.aura.launcher.ui.home.AuraEvent
import dev.aura.launcher.ui.home.AuraViewModel
import dev.aura.launcher.ui.home.SideEffect
import dev.aura.launcher.ui.navigation.NavigationTab
import dev.aura.launcher.ui.theme.AuraTheme
import dev.aura.launcher.widget.WIDGET_HOST_ID
import dev.aura.launcher.widget.widgetPickerIntent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vm: AuraViewModel
    private lateinit var widgetHost: AppWidgetHost

    // ── Gallery → wallpaper ───────────────────────────────────────────────────
    private val wallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> applyWallpaper(uri) }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) openGallery() }

    // ── Widget picker ─────────────────────────────────────────────────────────
    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (id == -1) return@registerForActivityResult

        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (info?.configure != null) {
            widgetConfigLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
            )
        } else {
            vm.onEvent(AuraEvent.AddWidget(id))
        }
    }

    private val widgetConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (id != -1) vm.onEvent(AuraEvent.AddWidget(id))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        vm         = ViewModelProvider(this, AuraViewModel.Factory(application))[AuraViewModel::class.java]
        widgetHost = AppWidgetHost(this, WIDGET_HOST_ID)

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val dark = when (state.settings.darkThemeMode) {
                "light" -> false
                "dark"  -> true
                else    -> isSystemDark()
            }
            AuraTheme(darkTheme = dark) {
                MainScreen(
                    state       = state,
                    onEvent     = vm::onEvent,
                    widgetHost  = widgetHost,
                    onAddWidget = { launchWidgetPicker() }
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.sideEffects.collect { effect ->
                    when (effect) {
                        SideEffect.PickWallpaper -> requestWallpaperFromGallery()
                        SideEffect.PickWidget    -> launchWidgetPicker()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            vm.onEvent(AuraEvent.SelectTab(NavigationTab.HOME))
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { widgetHost.stopListening() }
    }

    // ── Wallpaper ─────────────────────────────────────────────────────────────
    private fun requestWallpaperFromGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) openGallery()
        else permissionLauncher.launch(permission)
    }

    private fun openGallery() {
        wallpaperLauncher.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        )
    }

    private fun applyWallpaper(uri: Uri) {
        runCatching {
            WallpaperManager.getInstance(this).setStream(contentResolver.openInputStream(uri))
        }
    }

    private fun launchWidgetPicker() {
        runCatching { widgetPickerLauncher.launch(widgetPickerIntent(widgetHost)) }
    }

    private fun isSystemDark(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    @Deprecated("Suppress system back on launcher")
    override fun onBackPressed() { }
}
