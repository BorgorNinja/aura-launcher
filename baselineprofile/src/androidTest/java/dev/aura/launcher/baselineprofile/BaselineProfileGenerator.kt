package dev.aura.launcher.baselineprofile

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the ART baseline profile for Aura Launcher.
 *
 * ─── How to run ───────────────────────────────────────────────────────────────
 * Connect a physical device or rooted emulator running Android 9+ (API 28+)
 * with USB debugging enabled, then:
 *
 *   ./gradlew :baselineprofile:generateBaselineProfile
 *
 * The plugin writes the result to:
 *   app/src/main/generated/baselineProfiles/baseline-prof.txt
 *
 * Commit that file — it replaces (and supersedes) the hand-written seed at
 *   app/src/main/baseline-prof.txt
 *
 * ─── What it does ─────────────────────────────────────────────────────────────
 * The rule cold-starts the launcher, waits for the home screen, swipes open
 * the app drawer, scrolls through it, and dismisses it.  This walk covers:
 *   • Application.onCreate + MainActivity.onCreate
 *   • AuraViewModel init + StateFlow collection
 *   • AppIndexCache.warmAsync → Room DAO → Flow emission
 *   • DrawerScreen composable tree + LazyGrid compositor
 *   • IconCache.getSync fast path and preload suspend function
 *   • rememberAppIcon hot path (per-item in the drawer)
 *
 * ─── Device requirements ──────────────────────────────────────────────────────
 * • API 28+ (Android Pie)
 * • Rooted emulator OR physical device where the app can be force-compiled:
 *     adb shell cmd package compile -f -m speed-profile dev.aura.launcher
 * • The plugin handles compilation mode automatically via BaselineProfileRule.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName  = "dev.aura.launcher",
        startupModes = setOf(StartupMode.COLD)
    ) {
        // ── 1. Launch and wait for home screen to fully settle ────────────────
        startActivityAndWait()
        device.waitForIdle(2_000)

        // ── 2. Swipe up to open the app drawer ───────────────────────────────
        // Aura opens the drawer on upward fling from the bottom 25% of screen.
        val h = device.displayHeight
        val w = device.displayWidth
        device.swipe(
            /* startX  = */ w / 2,
            /* startY  = */ (h * 0.75).toInt(),
            /* endX    = */ w / 2,
            /* endY    = */ (h * 0.20).toInt(),
            /* steps   = */ 20           // slower swipe → gesture detector fires reliably
        )
        device.waitForIdle(1_500)

        // ── 3. Scroll the app list to warm the icon pipeline and LazyGrid ─────
        val scrollable = device.findObject(By.scrollable(true))
        if (scrollable != null) {
            // Scroll down ~80% of the list length to load icons past the fold
            scrollable.scroll(Direction.DOWN, 0.8f)
            device.waitForIdle(800)
            // Scroll back up — hits the fast-path getSync() on already-cached icons
            scrollable.scroll(Direction.UP, 0.8f)
            device.waitForIdle(500)
        }

        // ── 4. Dismiss the drawer and return to home ──────────────────────────
        device.pressBack()
        device.waitForIdle(500)
    }
}
