package com.example.data.sync

import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.PaymentRecord

data class PushPayload(
    val deviceId: String,
    val clientApp: String = "Rai Chilled Water Mobile v1.0",
    val syncKey: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deliveries: List<DeliveryEntry>,
    val payments: List<PaymentRecord>
)

data class PushResponse(
    val status: String,
    val message: String = "",
    val processedDeliveries: Int = 0,
    val processedPayments: Int = 0,
    val totalPaymentAmount: Double = 0.0,
    val serverTimestamp: Long = 0L,
    val syncedDeliveries: Int = 0,
    val syncedPayments: Int = 0
)

data class PullResponse(
    val status: String,
    val customers: List<Customer> = emptyList(),
    val serverTime: Long = 0L,
    val message: String = ""
)

data class ScanUploadResponse(
    val status: String,
    val fileName: String,
    val savedPath: String,
    val message: String = ""
)
