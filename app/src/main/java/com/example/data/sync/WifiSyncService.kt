package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.PaymentRecord
import com.example.data.model.ScanAttachment
import com.example.data.model.SyncConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(
        val message: String,
        val syncedDeliveries: Int,
        val syncedPayments: Int,
        val syncedScans: Int,
        val pulledCustomers: Int,
        val totalPaymentAmount: Double = 0.0,
        val serverTimestamp: Long = System.currentTimeMillis()
    ) : SyncResult()
    data class Partial(val message: String, val error: String) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}

data class PushResultDetails(
    val success: Boolean = true,
    val message: String = "",
    val processedDeliveries: Int = 0,
    val processedPayments: Int = 0,
    val totalPaymentAmount: Double = 0.0,
    val serverTimestamp: Long = 0L
) {
    val details: PushResultDetails
        get() = this
}

class WifiSyncService(private val context: Context? = null) {

    // Non-pooling client avoids reusing stale/dead TCP connections closed by Python servers
    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun isWifiConnected(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true // Default true if context not provided
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun Request.Builder.addSyncHeaders(config: SyncConfig): Request.Builder {
        val pin = config.securityPin.trim()
        if (pin.isNotEmpty()) {
            header("X-Sync-Key", pin)
            header("X-Sync-Password", pin)
        }
        header("Accept", "application/json")
        header("User-Agent", "RaiChilledWater-Android/1.0")
        return this
    }

    private fun appendKeyParam(url: String, key: String): String {
        val pin = key.trim()
        if (pin.isEmpty()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}key=$pin"
    }

    suspend fun testConnection(config: SyncConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val baseUrl = config.baseUrl
        val healthUrl = appendKeyParam("$baseUrl/api/health", config.securityPin)
        val pingUrl = appendKeyParam("$baseUrl/api/sync/ping", config.securityPin)

        var lastError = ""
        for (targetUrl in listOf(healthUrl, pingUrl, baseUrl)) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .addSyncHeaders(config)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val pinStatus = if (config.securityPin.isNotBlank()) " (PIN Verified)" else ""
                        return@withContext Pair(true, "PC Server Connected ($baseUrl)$pinStatus")
                    } else if (response.code == 401 || response.code == 403) {
                        return@withContext Pair(false, "Security PIN Rejected (HTTP ${response.code}). Check PIN in Settings.")
                    } else {
                        lastError = "Server responded with HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Timeout"
            }
        }

        val hint = if (lastError.contains("unexpected end of stream", ignoreCase = true)) {
            "PC सर्वर ने कनेक्शन तुरंत बंद कर दिया। कृपया PC ऐप पर '▶️ Start Server' बटन दोबारा दबाएं।"
        } else {
            "सुनिश्चित करें कि PC पर सर्वर चालू है और दोनों एक ही नेटवर्क/हॉटस्पॉट पर हैं।"
        }

        Pair(false, "Cannot reach PC at $baseUrl ($lastError). $hint")
    }

    suspend fun pushDeliveriesAndPayments(
        config: SyncConfig,
        deliveries: List<DeliveryEntry>,
        payments: List<PaymentRecord>
    ): PushResultDetails = withContext(Dispatchers.IO) {
        if (deliveries.isEmpty() && payments.isEmpty()) {
            return@withContext PushResultDetails(
                success = true,
                message = "कोई लंबित रिकॉर्ड नहीं (No pending deliveries or payments)",
                processedDeliveries = 0,
                processedPayments = 0,
                totalPaymentAmount = 0.0
            )
        }

        val primaryUrl = appendKeyParam("${config.baseUrl}${config.pushDeliveriesPath}", config.securityPin)
        val fallbackUrl = appendKeyParam("${config.baseUrl}/api/sync/push", config.securityPin)

        val totalAmount = payments.sumOf { it.amount }

        try {
            val root = JSONObject()
            root.put("deviceId", android.os.Build.MODEL ?: "Android-Mobile")
            root.put("clientApp", "Rai Chilled Water Mobile v1.0")
            root.put("syncKey", config.securityPin.trim())
            root.put("timestamp", System.currentTimeMillis())

            val deliveriesArray = JSONArray()
            deliveries.forEach { entry ->
                val obj = JSONObject()
                obj.put("id", entry.id)
                obj.put("customerId", entry.customerId)
                obj.put("customerName", entry.customerName)
                obj.put("date", entry.date)
                obj.put("time", entry.time)
                obj.put("cansGiven", entry.cansGiven)
                obj.put("cansReceived", entry.cansReceived)
                obj.put("notation", entry.notation)
                obj.put("driverName", entry.driverName)
                obj.put("route", entry.route)
                obj.put("notes", entry.notes)
                obj.put("timestamp", entry.timestamp)
                obj.put("isCorrected", entry.isCorrected)
                obj.put("correctionReason", entry.correctionReason ?: "")
                deliveriesArray.put(obj)
            }
            root.put("deliveries", deliveriesArray)

            val paymentsArray = JSONArray()
            payments.forEach { pay ->
                val obj = JSONObject()
                obj.put("id", pay.id)
                obj.put("customerId", pay.customerId)
                obj.put("customerName", pay.customerName)
                obj.put("date", pay.date)
                obj.put("amount", pay.amount)
                obj.put("paymentMethod", pay.paymentMethod)
                obj.put("reference", pay.reference)
                obj.put("collector", pay.collector)
                obj.put("notes", pay.notes)
                obj.put("timestamp", pay.timestamp)
                paymentsArray.put(obj)
            }
            root.put("payments", paymentsArray)

            // Explicit UTF-8 byte array prevents Content-Length / stream mismatch with Hindi text
            val jsonBytes = root.toString().toByteArray(Charsets.UTF_8)
            val mediaType = "application/json; charset=utf-8".toMediaType()

            var response: Response? = null
            var lastPushError = ""

            // Attempt URLs: primary first, fallback next
            for (targetUrl in listOf(primaryUrl, fallbackUrl)) {
                // Retry once if unexpected end of stream occurs on a closed socket
                for (attempt in 1..2) {
                    try {
                        val body = jsonBytes.toRequestBody(mediaType)
                        val request = Request.Builder()
                            .url(targetUrl)
                            .addSyncHeaders(config)
                            .post(body)
                            .build()

                        val resp = client.newCall(request).execute()
                        if (resp.code == 404 && targetUrl == primaryUrl) {
                            resp.close()
                            break // Move to fallbackUrl
                        }
                        response = resp
                        break // Success or handled response
                    } catch (e: IOException) {
                        lastPushError = e.localizedMessage ?: "Network error"
                        Log.w("WifiSyncService", "Push attempt $attempt to $targetUrl failed: $lastPushError")
                        // Wait brief pause before attempt 2
                        if (attempt == 1) {
                            kotlinx.coroutines.delay(400)
                        }
                    }
                }
                if (response != null && response.code != 404) {
                    break
                }
            }

            if (response == null) {
                val diagnosticMsg = if (lastPushError.contains("unexpected end of stream", ignoreCase = true)) {
                    "PC सर्वर ने डेटा पढ़ते समय कनेक्शन बंद कर दिया (Unexpected end of stream)।\nसुझाव: PC पर Rai Chilled Water ERP में '▶️ Start Server' चालू है या नहीं, जाँचें।"
                } else {
                    lastPushError
                }
                return@withContext PushResultDetails(
                    success = false,
                    message = "PC से संपर्क नहीं हो पाया: $diagnosticMsg"
                )
            }

            response.use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string().orEmpty()
                    Log.d("WifiSyncService", "Push successful: $respStr")
                    val json = try { JSONObject(respStr) } catch (e: Exception) { JSONObject() }

                    val processedDeliveries = json.optInt("processedDeliveries", json.optInt("syncedDeliveries", deliveries.size))
                    val processedPayments = json.optInt("processedPayments", json.optInt("syncedPayments", payments.size))
                    val respTotalAmount = json.optDouble("totalPaymentAmount", totalAmount)
                    val serverTimestamp = json.optLong("serverTimestamp", System.currentTimeMillis())
                    val msg = json.optString("message", "डेटा सफलतापूर्वक सिंक हो गया (Data Synced Successfully!)")

                    PushResultDetails(
                        success = true,
                        message = msg,
                        processedDeliveries = processedDeliveries,
                        processedPayments = processedPayments,
                        totalPaymentAmount = respTotalAmount,
                        serverTimestamp = serverTimestamp
                    )
                } else if (resp.code == 401 || resp.code == 403) {
                    PushResultDetails(
                        success = false,
                        message = "सुरक्षा पिन गलत है (PIN Rejected, HTTP ${resp.code})। कृपया सेटिंग्स में सही 6-अंकीय PIN दर्ज करें।"
                    )
                } else {
                    PushResultDetails(
                        success = false,
                        message = "PC सर्वर त्रुटि (HTTP ${resp.code}): ${resp.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("WifiSyncService", "Push error", e)
            PushResultDetails(
                success = false,
                message = "Push failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun uploadScanPhoto(
        config: SyncConfig,
        scan: ScanAttachment
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val file = File(scan.localFilePath)
        if (!file.exists()) {
            return@withContext Pair(false, "Local scan file missing: ${scan.fileName}")
        }

        val url = appendKeyParam("${config.baseUrl}${config.uploadScanPath}", config.securityPin)
        try {
            val fileBody = file.asRequestBody("image/jpeg".toMediaType())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", scan.id)
                .addFormDataPart("route", scan.route)
                .addFormDataPart("captureDate", scan.captureDate)
                .addFormDataPart("customerNote", scan.customerNote)
                .addFormDataPart("targetFolder", config.targetPcScanFolder)
                .addFormDataPart("syncKey", config.securityPin.trim())
                .addFormDataPart("file", scan.fileName, fileBody)
                .build()

            val request = Request.Builder()
                .url(url)
                .addSyncHeaders(config)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "Uploaded scan to PC ERP ${config.targetPcScanFolder}${scan.fileName}")
                } else {
                    Pair(false, "Scan upload failed with HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("WifiSyncService", "Scan upload error", e)
            Pair(false, "Scan upload error: ${e.localizedMessage}")
        }
    }

    suspend fun pullCustomersFromPc(
        config: SyncConfig
    ): Pair<List<Customer>?, String> = withContext(Dispatchers.IO) {
        val primaryUrl = appendKeyParam("${config.baseUrl}${config.pullCustomersPath}", config.securityPin)
        val fallbackUrl = appendKeyParam("${config.baseUrl}/api/sync/pull", config.securityPin)

        var lastPullError = ""

        for (targetUrl in listOf(primaryUrl, fallbackUrl)) {
            for (attempt in 1..2) {
                try {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .addSyncHeaders(config)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.code == 404 && targetUrl == primaryUrl) {
                        response.close()
                        break
                    }

                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            if (resp.code == 401 || resp.code == 403) {
                                return@withContext Pair(null, "सुरक्षा पिन अमान्य है (Security PIN Rejected, HTTP ${resp.code})")
                            }
                            return@withContext Pair(null, "PC returned HTTP ${resp.code}: ${resp.message}")
                        }
                        val bodyStr = resp.body?.string().orEmpty()
                        val json = JSONObject(bodyStr)
                        val customersArray = json.optJSONArray("customers") ?: JSONArray()
                        val resultList = mutableListOf<Customer>()

                        for (i in 0 until customersArray.length()) {
                            val obj = customersArray.getJSONObject(i)
                            val customer = Customer(
                                id = obj.getString("id"),
                                nameHindi = obj.optString("nameHindi", obj.optString("name", "Unknown")),
                                nameEnglish = obj.optString("nameEnglish", ""),
                                phone = obj.optString("phone", ""),
                                address = obj.optString("address", ""),
                                locality = obj.optString("locality", obj.optString("route", "General")),
                                ratePerCan = obj.optDouble("ratePerCan", 30.0),
                                openingCanBalance = obj.optInt("openingCanBalance", 0),
                                currentCanBalance = obj.optInt("currentCanBalance", 0),
                                openingFinancialBalance = obj.optDouble("openingFinancialBalance", 0.0),
                                currentFinancialBalance = obj.optDouble("currentFinancialBalance", 0.0),
                                isActive = obj.optBoolean("isActive", true),
                                lastUpdated = System.currentTimeMillis()
                            )
                            resultList.add(customer)
                        }

                        return@withContext Pair(resultList, "सफलतापूर्वक ${resultList.size} ग्राहक PC से प्राप्त हुए!")
                    }
                } catch (e: IOException) {
                    lastPullError = e.localizedMessage ?: "Network error"
                    Log.w("WifiSyncService", "Pull attempt $attempt from $targetUrl failed: $lastPullError")
                    if (attempt == 1) {
                        kotlinx.coroutines.delay(400)
                    }
                }
            }
        }

        Pair(null, "PC से ग्राहक डाउनलोड नहीं हो पाए: $lastPullError")
    }
}
