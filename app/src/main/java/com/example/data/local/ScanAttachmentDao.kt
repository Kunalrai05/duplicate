package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScanAttachment
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanAttachmentDao {
    @Query("SELECT * FROM scan_attachments ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanAttachment>>

    @Query("SELECT * FROM scan_attachments WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingScans(): List<ScanAttachment>

    @Query("SELECT COUNT(*) FROM scan_attachments WHERE syncStatus = 'PENDING'")
    fun getPendingScansCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanAttachment)

    @Update
    suspend fun updateScan(scan: ScanAttachment)

    @Query("UPDATE scan_attachments SET syncStatus = 'SYNCED', syncError = NULL WHERE id = :id")
    suspend fun markScanSynced(id: String)

    @Query("UPDATE scan_attachments SET syncStatus = 'FAILED', syncError = :error WHERE id = :id")
    suspend fun markScanFailed(id: String, error: String)

    @Query("DELETE FROM scan_attachments WHERE id = :id")
    suspend fun deleteScanById(id: String)

    @Query("DELETE FROM scan_attachments")
    suspend fun deleteAllScans()
}
