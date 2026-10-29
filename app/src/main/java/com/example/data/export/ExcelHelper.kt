package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelHelper {

    fun exportCustomersToCsv(context: Context, customers: List<Customer>): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Rai_Customers_$timeStamp.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            // UTF-8 BOM for Excel Hindi character compatibility
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "Customer ID,Hindi Name,English Name,Phone,Address,Locality,Rate Per Can,Can Balance,Due Amount (INR),Status\n"
            fos.write(header.toByteArray(Charsets.UTF_8))

            customers.forEach { c ->
                val line = "\"${c.id}\",\"${c.nameHindi}\",\"${c.nameEnglish}\",\"${c.phone}\",\"${c.address}\",\"${c.locality}\",${c.ratePerCan},${c.currentCanBalance},${c.currentFinancialBalance},${if (c.isActive) "Active" else "Inactive"}\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportDeliveriesToCsv(context: Context, deliveries: List<DeliveryEntry>): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Rai_Deliveries_$timeStamp.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "Transaction ID,Date,Time,Customer ID,Customer Name,Cans Given,Cans Received,Notation,Route,Driver,Notes,Sync Status\n"
            fos.write(header.toByteArray(Charsets.UTF_8))

            deliveries.forEach { d ->
                val line = "\"${d.id}\",\"${d.date}\",\"${d.time}\",\"${d.customerId}\",\"${d.customerName}\",${d.cansGiven},${d.cansReceived},\"${d.notation}\",\"${d.route}\",\"${d.driverName}\",\"${d.notes}\",\"${d.syncStatus}\"\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportedFile(context: Context, file: File, subject: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "राय चिल्ड वाटर पिपरिया — डेटा निर्यात फ़ाइल संलग्न है।")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            val chooser = Intent.createChooser(intent, "Share Export File")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "साझा करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseCustomersFromUri(context: Context, uri: Uri): List<Customer> {
        val list = mutableListOf<Customer>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var isFirstLine = true

            reader.forEachLine { rawLine ->
                val line = rawLine.trim().removePrefix("\uFEFF")
                if (line.isNotBlank()) {
                    if (isFirstLine && (line.contains("Customer ID", ignoreCase = true) || line.contains("Hindi Name", ignoreCase = true) || line.contains("Name", ignoreCase = true))) {
                        isFirstLine = false
                    } else {
                        isFirstLine = false
                        // Split CSV line respecting quotes
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                            .map { it.trim().removeSurrounding("\"") }

                        if (tokens.isNotEmpty() && tokens[0].isNotBlank()) {
                            val id = if (tokens.size > 0 && tokens[0].startsWith("CUST-")) tokens[0] else "CUST-${System.currentTimeMillis() % 10000 + list.size}"
                            val nameHindi = if (tokens.size > 1 && tokens[1].isNotBlank()) tokens[1] else (if (tokens.isNotEmpty()) tokens[0] else "New Customer")
                            val nameEnglish = if (tokens.size > 2) tokens[2] else nameHindi
                            val phone = if (tokens.size > 3) tokens[3] else ""
                            val address = if (tokens.size > 4) tokens[4] else ""
                            val locality = if (tokens.size > 5 && tokens[5].isNotBlank()) tokens[5] else "सामान्य रूट"
                            val rate = if (tokens.size > 6) tokens[6].toDoubleOrNull() ?: 30.0 else 30.0
                            val canBal = if (tokens.size > 7) tokens[7].toIntOrNull() ?: 0 else 0
                            val due = if (tokens.size > 8) tokens[8].toDoubleOrNull() ?: 0.0 else 0.0

                            list.add(
                                Customer(
                                    id = id,
                                    nameHindi = nameHindi,
                                    nameEnglish = nameEnglish,
                                    phone = phone,
                                    address = address,
                                    locality = locality,
                                    ratePerCan = rate,
                                    openingCanBalance = canBal,
                                    currentCanBalance = canBal,
                                    openingFinancialBalance = due,
                                    currentFinancialBalance = due
                                )
                            )
                        }
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
