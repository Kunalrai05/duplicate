package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "deliveries")
data class DeliveryEntry(
    @PrimaryKey
    val id: String,
    val customerId: String,
    val customerName: String = "",
    val date: String, // YYYY-MM-DD
    val time: String = "",
    val cansGiven: Int = 1,
    val cansReceived: Int = 0,
    val notation: String = "1",
    val rateApplied: Double = 30.0,
    val driverName: String = "Driver 1",
    val driverId: String = "Driver 1",
    val route: String = "General",
    val notes: String = "",
    val isCorrected: Boolean = false,
    val correctionReason: String? = null,
    val syncStatus: String = "PENDING",
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
