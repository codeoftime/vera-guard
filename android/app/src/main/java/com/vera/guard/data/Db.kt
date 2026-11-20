package com.vera.guard.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long, val type: String, val severity: String?, val score: Int?, val details: String, val trackUrl: String?,
)

@Entity(tableName = "nearby_cache")
data class NearbyCacheEntity(@PrimaryKey val key: String, val json: String, val fetchedAt: Long)

@Dao
interface EventDao {
    @Insert suspend fun insert(e: EventLogEntity): Long
    @Query("SELECT * FROM event_log ORDER BY ts DESC LIMIT 100") suspend fun recent(): List<EventLogEntity>
    @Query("DELETE FROM event_log") suspend fun clear()
}

@Dao
interface NearbyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(e: NearbyCacheEntity)
    @Query("SELECT * FROM nearby_cache WHERE `key`='last'") suspend fun last(): NearbyCacheEntity?
    @Query("DELETE FROM nearby_cache") suspend fun clear()
}

@Database(entities = [EventLogEntity::class, NearbyCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun nearby(): NearbyDao
}
