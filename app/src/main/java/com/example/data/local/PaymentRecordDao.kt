package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC, timestamp DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE date = :date")
    fun getPaymentsForDate(date: String): Flow<List<PaymentRecord>>

    @Query("SELECT SUM(amount) FROM payments WHERE date = :date")
    fun getTotalCollectedForDate(date: String): Flow<Double?>

    @Query("SELECT * FROM payments WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingSyncPayments(): List<PaymentRecord>

    @Query("SELECT COUNT(*) FROM payments WHERE syncStatus = 'PENDING'")
    fun getPendingPaymentsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord)

    @Query("UPDATE payments SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markPaymentsSynced(ids: List<String>)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}
