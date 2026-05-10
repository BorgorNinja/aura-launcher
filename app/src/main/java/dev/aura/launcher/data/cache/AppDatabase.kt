package dev.aura.launcher.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.aura.launcher.data.model.AppInfo

@Database(
    entities  = [AppInfo::class],
    version   = 1,
    exportSchema = false          // Schema written to app/schemas/ for git tracking.
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_index.db"
                )
                    .fallbackToDestructiveMigration()   // Index is rebuildable — no user data lost.
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
