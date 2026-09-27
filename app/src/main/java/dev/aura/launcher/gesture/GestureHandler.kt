package dev.aura.launcher.gesture

import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * GestureHandler
 *
 * Wraps GestureDetector to map common home-screen swipe gestures
 * to local Intents — no external gesture library required.
 *
 * Supported gestures:
 *   Swipe Up    → Open app drawer  (ACTION_SHOW_DRAWER)
 *   Swipe Down  → Expand notification shade via StatusBarManager
 *   Double Tap  → Launch default clock app
 *   Long Press  → Open launcher settings activity
 */
class GestureHandler(
    private val context: Context,
    private val onSwipeUp:    () -> Unit,
    private val onSwipeDown:  () -> Unit,
    private val onDoubleTap:  () -> Unit,
    private val onLongPress:  () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    companion object {
        // Defined in dp so the swipe feels the same across screen densities —
        // the previous raw-px constants (100) meant the gesture was easier to
        // trigger on low-density screens and harder on high-density ones.
        private const val SWIPE_THRESHOLD_DP     = 50f
        private const val SWIPE_VELOCITY_MIN_DP  = 50f
    }

    private val density         = context.resources.displayMetrics.density
    private val swipeThreshold  = SWIPE_THRESHOLD_DP * density
    private val swipeVelocityMin = SWIPE_VELOCITY_MIN_DP * density

    val detector = GestureDetector(context, this)

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent,
        velocityX: Float, velocityY: Float
    ): Boolean {
        val dX = e2.x - (e1?.x ?: 0f)
        val dY = e2.y - (e1?.y ?: 0f)

        return when {
            abs(dY) > abs(dX) && abs(dY) > swipeThreshold && abs(velocityY) > swipeVelocityMin -> {
                if (dY < 0) onSwipeUp() else onSwipeDown()
                true
            }
            else -> false
        }
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        onDoubleTap()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        onLongPress()
    }
}
