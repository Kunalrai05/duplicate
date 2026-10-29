package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DeliveryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryEntryDao {
    @Query("SELECT * FROM delivery_entries ORDER BY timestamp DESC")
    fun getAllDeliveries(): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries WHERE customerId = :customerId ORDER BY date DESC, timestamp DESC")
    fun getDeliveriesForCustomer(customerId: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries WHERE customerId = :customerId AND date = :date LIMIT 1")
    suspend fun getCustomerDeliveryForDate(customerId: String, date: String): DeliveryEntry?

    @Query("SELECT * FROM delivery_entries WHERE customerId = :customerId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getCustomerDeliveriesInRange(customerId: String, startDate: String, endDate: String): List<DeliveryEntry>

    @Query("SELECT * FROM delivery_entries WHERE customerId = :customerId AND date LIKE :yearMonthPrefix || '%' ORDER BY date ASC")
    fun getCustomerDeliveriesForMonth(customerId: String, yearMonthPrefix: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM delivery_entries WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingSyncDeliveries(): List<DeliveryEntry>

    @Query("SELECT COUNT(*) FROM delivery_entries WHERE syncStatus = 'PENDING'")
    fun getPendingDeliveriesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(entry: DeliveryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(entries: List<DeliveryEntry>)

    @Update
    suspend fun updateDelivery(entry: DeliveryEntry)

    @Query("DELETE FROM delivery_entries WHERE id = :id")
    suspend fun deleteDeliveryById(id: String)

    @Query("UPDATE delivery_entries SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markDeliveriesSynced(ids: List<String>)

    @Query("SELECT SUM(cansGiven) FROM delivery_entries WHERE date = :date")
    fun getTotalCansGivenForDate(date: String): Flow<Int?>

    @Query("SELECT SUM(cansReceived) FROM delivery_entries WHERE date = :date")
    fun getTotalCansReceivedForDate(date: String): Flow<Int?>

    @Query("DELETE FROM delivery_entries")
    suspend fun deleteAllDeliveries()
}
