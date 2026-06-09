package dev.aura.launcher.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Palette model ─────────────────────────────────────────────────────────────

data class AuraPaletteEntry(
    val key:         String,
    val label:       String,
    val swatchColor: Color,
    val light:       ColorScheme,
    val dark:        ColorScheme,
)

private fun palette(
    key: String, label: String, swatch: Color,
    lp: Color, ls: Color, lt: Color,
    dp: Color, ds: Color, dt: Color,
) = AuraPaletteEntry(
    key         = key,
    label       = label,
    swatchColor = swatch,
    light       = lightColorScheme(primary = lp, secondary = ls, tertiary = lt),
    dark        = darkColorScheme (primary = dp, secondary = ds, tertiary = dt),
)

// ── Preset palettes ───────────────────────────────────────────────────────────

val AURA_PALETTES: List<AuraPaletteEntry> = listOf(
    // Dynamic — resolved at runtime via Material You / dynamic color
    AuraPaletteEntry(
        key         = "dynamic",
        label       = "Dynamic",
        swatchColor = Color(0xFF6750A4),
        light       = lightColorScheme(),
        dark        = darkColorScheme(),
    ),
    // Ocean — deep navy & teal
    palette(
        "ocean", "Ocean", Color(0xFF1565C0),
        lp = Color(0xFF1565C0), ls = Color(0xFF006874), lt = Color(0xFF1D6586),
        dp = Color(0xFF9ECAFF), ds = Color(0xFF4FD8E8), dt = Color(0xFF84CFEE),
    ),
    // Aurora — violet & emerald
    palette(
        "aurora", "Aurora", Color(0xFF6750A4),
        lp = Color(0xFF6750A4), ls = Color(0xFF2B9053), lt = Color(0xFF7D4091),
        dp = Color(0xFFCFBCFF), ds = Color(0xFF76D998), dt = Color(0xFFD99BEF),
    ),
    // Sunset — deep orange & amber
    palette(
        "sunset", "Sunset", Color(0xFFBF360C),
        lp = Color(0xFFBF360C), ls = Color(0xFFE65100), lt = Color(0xFF8D1900),
        dp = Color(0xFFFFB59D), ds = Color(0xFFFFB74D), dt = Color(0xFFFF8A65),
    ),
    // Forest — dark green & earth
    palette(
        "forest", "Forest", Color(0xFF1B5E20),
        lp = Color(0xFF1B5E20), ls = Color(0xFF33691E), lt = Color(0xFF004D40),
        dp = Color(0xFFA5D6A7), ds = Color(0xFFC5E1A5), dt = Color(0xFF80CBC4),
    ),
    // Rose Gold — pink & warm gold
    palette(
        "rose", "Rose Gold", Color(0xFFC2185B),
        lp = Color(0xFFC2185B), ls = Color(0xFFAD1457), lt = Color(0xFF880E4F),
        dp = Color(0xFFF48FB1), ds = Color(0xFFF06292), dt = Color(0xFFEC407A),
    ),
    // Midnight — deep indigo & violet
    palette(
        "midnight", "Midnight", Color(0xFF283593),
        lp = Color(0xFF283593), ls = Color(0xFF1A237E), lt = Color(0xFF311B92),
        dp = Color(0xFF7986CB), ds = Color(0xFF5C6BC0), dt = Color(0xFF7E57C2),
    ),
    // Sakura — cherry blossom & soft lavender
    palette(
        "sakura", "Sakura", Color(0xFFE91E63),
        lp = Color(0xFFE91E63), ls = Color(0xFFAB47BC), lt = Color(0xFFFF80AB),
        dp = Color(0xFFF48FB1), ds = Color(0xFFCE93D8), dt = Color(0xFFF8BBD9),
    ),
    // Amber — rich amber & burnt sienna
    palette(
        "amber", "Amber", Color(0xFFE65100),
        lp = Color(0xFFE65100), ls = Color(0xFFBF360C), lt = Color(0xFFFF6F00),
        dp = Color(0xFFFFCC80), ds = Color(0xFFFFB74D), dt = Color(0xFFFFA726),
    ),
)

fun paletteForKey(key: String): AuraPaletteEntry =
    AURA_PALETTES.find { it.key == key } ?: AURA_PALETTES[0]

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun AuraTheme(
    darkTheme:  Boolean = false,
    colorTheme: String  = "dynamic",
    content:    @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val colorScheme = when {
        colorTheme == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> {
            val p = paletteForKey(colorTheme)
            if (darkTheme) p.dark else p.light
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = androidx.compose.material3.Typography(),
        content     = content
    )
}
