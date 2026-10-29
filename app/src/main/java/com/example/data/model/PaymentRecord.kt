package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "payments")
data class PaymentRecord(
    @PrimaryKey
    val id: String,
    val customerId: String,
    val customerName: String = "",
    val date: String,
    val amount: Double,
    val paymentMethod: String = "CASH",
    val reference: String = "",
    val utrReference: String = "",
    val collector: String = "Admin/Driver",
    val notes: String = "",
    val billId: String? = null,
    val isSynced: Boolean = false,
    val syncStatus: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
