// Root build.gradle.kts — Aura Launcher
// No IDE required. Pure Gradle Kotlin DSL.

plugins {
    alias(libs.plugins.android.application)         apply false
    alias(libs.plugins.kotlin.android)              apply false
    alias(libs.plugins.kotlin.compose)              apply false
    // ── ART / Baseline Profiles ───────────────────────────────────────────
    alias(libs.plugins.android.test)                apply false
    alias(libs.plugins.androidx.baselineprofile)    apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
