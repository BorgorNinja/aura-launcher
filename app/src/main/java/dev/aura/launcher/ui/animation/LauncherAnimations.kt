package dev.aura.launcher.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Spring configurations and transition curves adapted from Lawnchair / AOSP Launcher3.
 */
object LauncherAnimations {

    // Icon press & release physics — reproduces Lawnchair PhysicsAnimator SpringForce.STIFFNESS_MEDIUM & DAMPING_RATIO_MEDIUM_BOUNCY
    val IconPressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Drawer slide & settle physics — smooth glide with subtle deceleration
    val DrawerSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Android 14/15/16 Launcher3 Emphasized Interpolators
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // Standard 300ms emphasized transition for slide in/out
    val EmphasizedEnterTween = tween<Float>(
        durationMillis = 320,
        easing = EmphasizedDecelerateEasing
    )

    val EmphasizedExitTween = tween<Float>(
        durationMillis = 250,
        easing = EmphasizedAccelerateEasing
    )
}
