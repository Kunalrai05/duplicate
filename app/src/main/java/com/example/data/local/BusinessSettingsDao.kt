package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BusinessSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessSettingsDao {

    @Query("SELECT * FROM business_settings WHERE id = 'primary_settings' LIMIT 1")
    fun getSettingsFlow(): Flow<BusinessSettings?>

    @Query("SELECT * FROM business_settings WHERE id = 'primary_settings' LIMIT 1")
    suspend fun getSettingsOnce(): BusinessSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: BusinessSettings)

    @Update
    suspend fun update(settings: BusinessSettings)
}
