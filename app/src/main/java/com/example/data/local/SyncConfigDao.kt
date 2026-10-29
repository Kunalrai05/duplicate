package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConfigDao {
    @Query("SELECT * FROM sync_config WHERE id = 1 LIMIT 1")
    fun getSyncConfig(): Flow<SyncConfig?>

    @Query("SELECT * FROM sync_config WHERE id = 1 LIMIT 1")
    suspend fun getSyncConfigDirect(): SyncConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: SyncConfig)
}
