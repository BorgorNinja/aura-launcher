package dev.aura.launcher

import android.app.WallpaperManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
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
import dev.aura.launcher.ui.home.AuraViewModel
import android.net.Uri
import dev.aura.launcher.ui.home.SideEffect
import dev.aura.launcher.ui.theme.AuraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val wallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        // Init ViewModel BEFORE setContent so the sideEffects collector below
        // never races against Compose's first composition.
        val vm = ViewModelProvider(
            this, AuraViewModel.Factory(application)
        )[AuraViewModel::class.java]

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
                    onAddWidget = { }
                )
            }
        }

        // Safe to collect now — vm is already initialized above.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.sideEffects.collect { effect ->
                    when (effect) {
                        SideEffect.PickWallpaper -> launchWallpaperPicker()
                        is SideEffect.Uninstall -> startActivity(
                            Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:" + effect.packageName) }
                        )
                        is SideEffect.Uninstall -> startActivity(
                            Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:" + effect.packageName) }
                        )
                    }
                }
            }
        }
    }

    private fun launchWallpaperPicker() {
        val live = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        val intent = if (packageManager.resolveActivity(live, 0) != null) live
                     else Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose wallpaper")
        wallpaperLauncher.launch(intent)
    }

    private fun isSystemDark(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    @Deprecated("Suppress system back on launcher")
    override fun onBackPressed() { }
}
