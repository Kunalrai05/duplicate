package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY locality ASC, nameHindi ASC")
    fun getAllActiveCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY locality ASC, nameHindi ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: String): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: String): Customer?

    @Query("SELECT DISTINCT locality FROM customers WHERE locality != '' ORDER BY locality ASC")
    fun getDistinctLocalities(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET currentCanBalance = :newBalance WHERE id = :customerId")
    suspend fun updateCanBalance(customerId: String, newBalance: Int)

    @Query("UPDATE customers SET currentFinancialBalance = :newBalance WHERE id = :customerId")
    suspend fun updateFinancialBalance(customerId: String, newBalance: Double)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
}
