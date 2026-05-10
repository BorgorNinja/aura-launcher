package dev.aura.launcher.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.aura.launcher.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM app_index ORDER BY label ASC")
    fun observeAll(): Flow<List<AppInfo>>

    /**
     * Ranked search — exact prefix match scored highest, then substring.
     * Runs entirely in SQLite with no FTS5 extension required.
     */
    @Query("""
        SELECT * FROM app_index
        WHERE label LIKE :prefix || '%'
           OR label LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN label LIKE :prefix || '%' THEN 0 ELSE 1 END,
            launchCount DESC,
            label ASC
        LIMIT 30
    """)
    suspend fun search(query: String, prefix: String = query): List<AppInfo>

    @Query("SELECT * FROM app_index WHERE category = :cat ORDER BY launchCount DESC")
    fun byCategory(cat: Int): Flow<List<AppInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppInfo>)

    @Query("DELETE FROM app_index WHERE packageName NOT IN (:active)")
    suspend fun pruneUninstalled(active: List<String>)

    @Query("""
        UPDATE app_index
        SET launchCount = launchCount + 1, lastLaunchMs = :nowMs
        WHERE packageName = :pkg
    """)
    suspend fun recordLaunch(pkg: String, nowMs: Long = System.currentTimeMillis())
}
