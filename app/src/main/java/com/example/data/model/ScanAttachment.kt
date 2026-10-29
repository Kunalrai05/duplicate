package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "scan_attachments")
data class ScanAttachment(
    @PrimaryKey
    val id: String,
    val localFilePath: String = "",
    val localImagePath: String = "",
    val fileName: String = "",
    val captureDate: String = "",
    val date: String = "",
    val route: String = "Route 1",
    val routeName: String = "Route 1",
    val customerNote: String = "",
    val fileSize: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING",
    val status: String = "QUEUED",
    val syncError: String? = null
) : Serializable
