plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace   = "dev.aura.launcher.baselineprofile"
    compileSdk  = 35

    defaultConfig {
        // Baseline profile generation requires API 28+ (Android P).
        // The app itself supports API 26+, but the profiler tooling is API-28 only.
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Points the plugin at the module whose baseline profile we are generating.
    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core macrobenchmark harness — provides BaselineProfileRule
    implementation(libs.benchmark.macro.junit4)
    // UiAutomator — drives the UI during profile collection
    implementation(libs.uiautomator)
    // Test runner — provides AndroidJUnitRunner
    implementation(libs.test.runner)
    // AndroidJUnit4 runner annotation
    implementation(libs.test.ext.junit)
}
