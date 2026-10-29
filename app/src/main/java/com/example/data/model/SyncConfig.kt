package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "sync_config")
data class SyncConfig(
    @PrimaryKey
    val id: Int = 1,
    val pcHost: String = "192.168.1.105",
    val pcPort: Int = 5000,
    val pcServerIp: String = "192.168.1.105",
    val pcServerPort: Int = 5000,
    val securityPin: String = "",
    val pullCustomersPath: String = "/api/sync/pull_customers",
    val pushDeliveriesPath: String = "/api/sync/push_deliveries",
    val uploadScanPath: String = "/api/sync/upload_scan",
    val targetPcScanFolder: String = "./scans/",
    val autoSyncOnWifi: Boolean = true,
    val wifiSsid: String = "",
    val lastSyncTime: Long = 0L,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "No sync yet",
    val lastSyncSuccess: Boolean = false,
    val remoteSyncUrl: String = ""
) : Serializable {
    val baseUrl: String
        get() {
            var raw = (if (pcHost.isNotBlank()) pcHost else pcServerIp).trim()
            val isHttps = raw.startsWith("https", ignoreCase = true)
            raw = raw.replace(Regex("""^https?[:;pP/]+""", RegexOption.IGNORE_CASE), "")
                .trimStart('/', ':', ' ')
                .removeSuffix("/")

            val scheme = if (isHttps) "https" else "http"
            val port = if (pcPort > 0) pcPort else if (pcServerPort > 0) pcServerPort else 5000

            return when {
                raw.contains(":") -> "$scheme://$raw"
                raw.contains(".trycloudflare.com") || raw.contains(".ngrok-free.app") -> "$scheme://$raw"
                else -> "$scheme://$raw:$port"
            }
        }
}
