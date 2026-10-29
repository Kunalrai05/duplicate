package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BillRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BillRecordDao {
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<BillRecord>>

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getBillsForCustomer(customerId: String): Flow<List<BillRecord>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): BillRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillRecord)

    @Update
    suspend fun updateBill(bill: BillRecord)

    @Delete
    suspend fun deleteBill(bill: BillRecord)

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getBillCount(): Int

    @Query("SELECT billNumber FROM bills ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBillNumber(): String?

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}
