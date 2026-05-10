# ─── Aura Launcher — R8 / ProGuard Rules ────────────────────────────────────
# R8 full mode is enabled via gradle.properties.
# These rules prevent over-stripping of classes that are accessed reflectively.

# ─── Kotlin ──────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.coroutines.** { *; }

# ─── Compose Runtime ─────────────────────────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ─── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

# ─── DataStore ───────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ─── App-Specific ────────────────────────────────────────────────────────────
# Preserve launcher intent entry points.
-keep class dev.aura.launcher.MainActivity { *; }
-keep class * extends android.app.Activity
-keep class * extends android.content.BroadcastReceiver

# Strip all logging in release.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ─── Serialization Safety ────────────────────────────────────────────────────
-keepclassmembers class dev.aura.launcher.data.model.** { *; }

# Remove source file name and line number from stack traces (obfuscation).
-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable
