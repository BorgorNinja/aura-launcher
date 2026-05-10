import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ─── Signing ─────────────────────────────────────────────────────────────────
// Place keystore.properties alongside this file (never commit it).
// Generate keystore: keytool -genkey -v -keystore aura.jks -keyalg RSA -keysize 2048 -validity 10000
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace   = "dev.aura.launcher"
    compileSdk  = 35

    defaultConfig {
        applicationId         = "dev.aura.launcher"
        minSdk                = 26          // Android 8 — covers 98%+ active devices
        targetSdk             = 35
        versionCode           = 1
        versionName           = "1.0.0"

        // Room schema export — written to app/schemas/ for version tracking via git.
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"]    = "true"
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile     = file(keystoreProps.getProperty("storeFile",     "aura.jks"))
            storePassword = keystoreProps.getProperty("storePassword", "")
            keyAlias      = keystoreProps.getProperty("keyAlias",      "aura")
            keyPassword   = keystoreProps.getProperty("keyPassword",   "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Ensures smooth 120Hz animations on cold start after CLI install.
        }

        debug {
            applicationIdSuffix = ".debug"
            isDebuggable        = true
        }
    }

    // ─── Compile Options ─────────────────────────────────────────────────────
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    // ─── Compose ─────────────────────────────────────────────────────────────
    buildFeatures {
        compose    = true
        buildConfig = true
        // Explicitly disable unused features to trim build time & APK size.
        viewBinding = false
        dataBinding = false
        aidl        = false
        renderScript = false
        resValues    = false
        shaders      = false
    }

    // ─── APK Packaging ───────────────────────────────────────────────────────
    packaging {
        resources {
            // Strip metadata files that bloat APK without runtime benefit.
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/*.txt",
                "/kotlin/**.kotlin_builtins",
                "/okhttp3/**",
                "/META-INF/versions/9/previous-compilation-data.bin"
            )
        }
    }

    // ─── Split / ABI ─────────────────────────────────────────────────────────
    // Universal APK by default; enable splits for Play Store upload.
    splits {
        abi {
            isEnable         = false
            isUniversalApk  = true
        }
    }

    // ─── Lint ────────────────────────────────────────────────────────────────
    lint {
        abortOnError = false
        quiet        = true
    }
}

// ─── Dependencies ─────────────────────────────────────────────────────────────
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.ext)
    implementation(libs.activity.compose)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
}
