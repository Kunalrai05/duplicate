package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "bills")
data class BillRecord(
    @PrimaryKey
    val id: String,
    val billNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val billingDate: String = "",
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val totalCansDelivered: Int = 0,
    val totalCansReturned: Int = 0,
    val ratePerCan: Double = 30.0,
    val extraProductsSummary: String = "",
    val extraProductsTotal: Double = 0.0,
    val waterChargesTotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalPayable: Double = 0.0,
    val paidAmount: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentStatus: String = "PENDING",
    val monthYear: String = "",
    val dateIssued: String = "",
    val previousDue: Double = 0.0,
    val currentMonthCharges: Double = 0.0,
    val paymentsReceived: Double = 0.0,
    val netPayable: Double = 0.0,
    val cansBalanceSnapshot: Int = 0,
    val status: String = "PENDING",
    val pdfPath: String? = null,
    val pdfFilePath: String? = null,
    val notes: String = "",
    val isImported: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
