# Aura Launcher

A high-performance, privacy-first Android launcher built with Jetpack Compose and Material 3 Expressive. Designed to be compiled entirely from the command line — no Android Studio required.

[![Build](https://github.com/BorgorNinja/aura-launcher/actions/workflows/build.yml/badge.svg)](https://github.com/BorgorNinja/aura-launcher/actions/workflows/build.yml)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material-3%20Expressive-purple)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## Screenshots

> Home screen · App drawer · Settings · Widget dashboard

*(Add screenshots here after first device install)*

---

## Features

- **Pure Compose UI** — zero XML layout inflation, no ViewBinding
- **Material 3 Dynamic Color** — Monet engine via local system API, no network call
- **Offline-first** — no `INTERNET` permission, ever
- **LRU Icon Cache** — icons decoded once per process and held in RAM, no scroll stutter
- **Local SQLite search** — instant ranked app search via Room, no external index
- **Alphabetical drawer** — section headers and right-side letter index with animated scroll
- **Heuristic categories** — derived from `ApplicationInfo.category` (API 26), no metadata API
- **Custom gesture system** — swipe up to open drawer with spring animation, swipe down to close
- **Widget hosting** — `AppWidgetHost` infrastructure for adding home screen widgets
- **Settings** — grid columns, theme (light/dark/system), notification dots, wallpaper picker
- **Dock** — 4 most-launched apps auto-populated, pinned above bottom nav
- **R8 full mode** — aggressive dead-code elimination, target APK under 3 MB

---

## Architecture

```
app/src/main/java/dev/aura/launcher/
├── AuraApplication.kt           # App entry — warms icon cache on boot
├── MainActivity.kt              # Single Activity; handles wallpaper + uninstall intents
│
├── data/
│   ├── cache/
│   │   ├── AppDao.kt            # Room DAO — search, upsert, launch tracking
│   │   ├── AppDatabase.kt       # Room DB singleton
│   │   └── AppIndexCache.kt     # Background PackageManager → Room warm-up
│   ├── model/
│   │   ├── AppInfo.kt           # Room @Entity — package, label, category, launch count
│   │   └── AuraSettings.kt      # Settings data class (grid, theme, dots)
│   └── repository/
│       ├── AppRepository.kt     # Launch, uninstall, app info, icon loading
│       └── SettingsRepository.kt # DataStore-backed settings persistence
│
├── gesture/
│   └── GestureHandler.kt        # GestureDetector wrapper — swipe/double-tap/long-press
│
├── ui/
│   ├── MainScreen.kt            # Root composable — bottom nav + AnimatedContent tab host
│   ├── drawer/
│   │   └── DrawerScreen.kt      # App grid + alphabetical index + search
│   ├── home/
│   │   ├── AuraViewModel.kt     # Unified state + SharedFlow side effects
│   │   └── HomeTab.kt           # Clock, search pill, dock, swipe gesture
│   ├── navigation/
│   │   └── NavigationTab.kt     # HOME | WIDGETS | APPS | SETTINGS enum
│   ├── settings/
│   │   └── SettingsScreen.kt    # Material You settings with DataStore persistence
│   ├── theme/
│   │   └── AuraTheme.kt         # Dynamic color via Monet, falls back to M3 defaults
│   ├── util/
│   │   └── AppIconLoader.kt     # Cache-backed icon composable via produceState
│   └── widgets/
│       └── WidgetDashboardScreen.kt # Widget host tab
│
├── util/
│   └── IconCache.kt             # LruCache<String, ImageBitmap> — 150 icons, process-scoped
│
└── widget/
    ├── AuraWidgetReceiver.kt    # BroadcastReceiver for APPWIDGET_HOST_RESTORED
    └── WidgetHost.kt            # AppWidgetHost lifecycle wrapper for Compose
```

---

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + StateFlow + SharedFlow |
| Local DB | Room 2.6.1 (SQLite) |
| Settings | DataStore Preferences 1.1.1 |
| Build | Gradle 8.9 Kotlin DSL + KSP |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 |
| Obfuscation | R8 full mode |

---

## Building from CLI (Termux / Debian / Ubuntu)

### 1. Bootstrap the SDK

```bash
chmod +x setup_sdk.sh && ./setup_sdk.sh
source ~/.bashrc
```

This downloads `cmdline-tools`, `platform-tools`, `build-tools;35.0.0`, and `platforms;android-35`, and generates the Gradle wrapper.

### 2. Configure signing

```bash
cp keystore.properties.template keystore.properties
make generate-keystore   # or fill keystore.properties manually
cp local.properties.template local.properties
# edit local.properties → set sdk.dir to your ANDROID_SDK_ROOT
```

### 3. Build

```bash
make build          # assembleRelease
make install        # push + pm install to connected device
make build-debug    # assembleDebug
make clean          # wipe build outputs
```

Or directly:

```bash
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

### Low-RAM environments (≤ 4 GB)

Reduce JVM heap in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx768m -Xms128m
```

Add swap if R8 OOMs:

```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
```

---

## Granting Widget Permission

`BIND_APPWIDGET` is a system-level permission. On first widget use, Android will prompt the user. Alternatively grant via adb:

```bash
adb shell appwidget grantbind --package dev.aura.launcher --user 0
```

---

## Makefile Targets

| Target | Description |
|---|---|
| `make build` | Release APK via `assembleRelease` |
| `make build-debug` | Debug APK |
| `make install` | Build + push + `pm install` |
| `make install-debug` | Debug install |
| `make clean` | Delete build outputs |
| `make lint` | Run lint checks |
| `make sign` | Verify release APK signature |
| `make log` | `adb logcat` filtered to Aura |
| `make size` | APK size breakdown via `apkanalyzer` |
| `make generate-keystore` | Interactive keystore generator |

---

## Privacy

- No `INTERNET` permission — the APK cannot make any network request
- No analytics, no crash reporting, no ads
- All app metadata derived locally via `PackageManager`
- Settings stored in DataStore on-device only
- Icon cache lives in process memory only — cleared on reboot

---

## Roadmap

- [ ] Icon pack support (query installed packs via `PackageManager`)
- [ ] Notification dot rendering via `NotificationListenerService`
- [ ] Persistent widget layout (save/restore `appWidgetId` list)
- [ ] Folder support on home screen
- [ ] Custom dock app pinning in settings
- [ ] Swipe-down notification shade shortcut

---

## License

MIT — see [LICENSE](LICENSE)
