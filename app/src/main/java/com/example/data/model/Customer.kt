package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    val id: String,
    val nameHindi: String,
    val nameEnglish: String = "",
    val phone: String = "",
    val address: String = "",
    val locality: String = "Station Road",
    val ratePerCan: Double = 30.0,
    val openingCanBalance: Int = 0,
    val currentCanBalance: Int = 0,
    val openingFinancialBalance: Double = 0.0,
    val currentFinancialBalance: Double = 0.0,
    val isActive: Boolean = true,
    val lastDeliveryDate: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable
