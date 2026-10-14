package com.evertide.riptide.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "cached_image")
data class CachedImageEntity(@PrimaryKey val cacheKey: String, val cachedAt: Long)

@Dao
interface CachedImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entry: CachedImageEntity)

    @Query("SELECT cacheKey FROM cached_image WHERE cachedAt < :cutoff")
    suspend fun expiredKeys(cutoff: Long): List<String>

    @Query("DELETE FROM cached_image WHERE cachedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM cached_image") suspend fun deleteAll()
}

@Database(entities = [CachedImageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedImageDao(): CachedImageDao
}
