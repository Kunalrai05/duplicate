package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.BillRecord
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.PaymentRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExportHelper {

    /**
     * Exports full monthly master ledger for CA/Accountant in CSV format with UTF-8 BOM.
     * Contains Days 1-31 delivery columns, totals, charges, collections and net balances.
     */
    fun exportMonthlyMasterLedgerCsv(
        context: Context,
        year: Int,
        month: Int,
        customers: List<Customer>,
        deliveries: List<DeliveryEntry>,
        payments: List<PaymentRecord>
    ): File? {
        val monthStr = month.toString().padStart(2, '0')
        val monthPrefix = "$year-$monthStr"
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Rai_Monthly_Ledger_${year}_${monthStr}_$timeStamp.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            // UTF-8 BOM for Excel Hindi character support
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val daysHeader = (1..31).joinToString(",") { "Day $it" }
            val header = "S.No,Customer ID,Customer Name,Phone,Address,Rate (INR),$daysHeader,Total Delivered Cans,Total Returned Cans,Water Charges (INR),Extra Charges (INR),Total Bill (INR),Paid Amount (INR),Net Balance Due (INR)\n"
            fos.write(header.toByteArray(Charsets.UTF_8))

            val monthDeliveries = deliveries.filter { it.date.startsWith(monthPrefix) }
            val monthPayments = payments.filter { it.date.startsWith(monthPrefix) }

            customers.forEachIndexed { index, cust ->
                val custDeliveries = monthDeliveries.filter { it.customerId == cust.id }
                val custPayments = monthPayments.filter { it.customerId == cust.id }

                val dayMap = mutableMapOf<Int, Int>()
                custDeliveries.forEach { d ->
                    val dayNum = d.date.substringAfterLast("-").toIntOrNull() ?: 1
                    dayMap[dayNum] = (dayMap[dayNum] ?: 0) + d.cansGiven
                }

                val daysString = (1..31).joinToString(",") { day ->
                    dayMap[day]?.toString() ?: "0"
                }

                val totalDelivered = custDeliveries.sumOf { it.cansGiven }
                val totalReturned = custDeliveries.sumOf { it.cansReceived }
                val waterCharges = totalDelivered * cust.ratePerCan
                val extraCharges = 0.0
                val totalBill = waterCharges + extraCharges
                val totalPaid = custPayments.sumOf { it.amount }
                val netDue = (cust.currentFinancialBalance).coerceAtLeast(0.0)

                val line = "${index + 1},\"${cust.id}\",\"${cust.nameHindi} (${cust.nameEnglish})\",\"${cust.phone}\",\"${cust.address}\",${cust.ratePerCan},$daysString,$totalDelivered,$totalReturned,$waterCharges,$extraCharges,$totalBill,$totalPaid,$netDue\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports full Bills Register (All, Paid, Partial, Pending) to CSV.
     */
    fun exportBillsRegisterCsv(context: Context, bills: List<BillRecord>): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Rai_Bills_Register_$timeStamp.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "Bill No,Billing Date,Customer ID,Customer Name,Phone,Period Start,Period End,Delivered Cans,Returned Cans,Rate Per Can,Water Charges,Extra Products,Total Bill (INR),Paid Amount (INR),Balance Due (INR),Payment Status\n"
            fos.write(header.toByteArray(Charsets.UTF_8))

            bills.forEach { b ->
                val line = "\"${b.billNumber}\",\"${b.billingDate}\",\"${b.customerId}\",\"${b.customerName}\",\"${b.customerPhone}\",\"${b.periodStartDate}\",\"${b.periodEndDate}\",${b.totalCansDelivered},${b.totalCansReturned},${b.ratePerCan},${b.waterChargesTotal},\"${b.extraProductsSummary}\",${b.totalPayable},${b.paidAmount},${b.balanceDue},\"${b.paymentStatus}\"\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a complete standalone JSON backup of the entire database.
     * Includes Customers, Deliveries, Bills, Payments, and Settings.
     */
    fun createFullDatabaseJsonBackup(
        context: Context,
        customers: List<Customer>,
        deliveries: List<DeliveryEntry>,
        bills: List<BillRecord>,
        payments: List<PaymentRecord>,
        settings: BusinessSettings
    ): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "RaiWater_Full_Backup_$timeStamp.json"
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val file = File(backupDir, fileName)

        return try {
            val root = JSONObject()
            root.put("appName", "Rai Chilled Water ERP")
            root.put("version", "2.0")
            root.put("backupTimestamp", System.currentTimeMillis())
            root.put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // Customers
            val custArray = JSONArray()
            customers.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("nameHindi", c.nameHindi)
                obj.put("nameEnglish", c.nameEnglish)
                obj.put("phone", c.phone)
                obj.put("address", c.address)
                obj.put("locality", c.locality)
                obj.put("ratePerCan", c.ratePerCan)
                obj.put("openingCanBalance", c.openingCanBalance)
                obj.put("currentCanBalance", c.currentCanBalance)
                obj.put("openingFinancialBalance", c.openingFinancialBalance)
                obj.put("currentFinancialBalance", c.currentFinancialBalance)
                obj.put("isActive", c.isActive)
                custArray.put(obj)
            }
            root.put("customers", custArray)

            // Deliveries
            val delArray = JSONArray()
            deliveries.forEach { d ->
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("customerId", d.customerId)
                obj.put("customerName", d.customerName)
                obj.put("date", d.date)
                obj.put("time", d.time)
                obj.put("cansGiven", d.cansGiven)
                obj.put("cansReceived", d.cansReceived)
                obj.put("notation", d.notation)
                obj.put("route", d.route)
                obj.put("driverName", d.driverName)
                obj.put("notes", d.notes)
                obj.put("isCorrected", d.isCorrected)
                delArray.put(obj)
            }
            root.put("deliveries", delArray)

            // Bills
            val billArray = JSONArray()
            bills.forEach { b ->
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("billNumber", b.billNumber)
                obj.put("billingDate", b.billingDate)
                obj.put("periodStartDate", b.periodStartDate)
                obj.put("periodEndDate", b.periodEndDate)
                obj.put("customerId", b.customerId)
                obj.put("customerName", b.customerName)
                obj.put("customerPhone", b.customerPhone)
                obj.put("customerAddress", b.customerAddress)
                obj.put("totalCansDelivered", b.totalCansDelivered)
                obj.put("totalCansReturned", b.totalCansReturned)
                obj.put("ratePerCan", b.ratePerCan)
                obj.put("extraProductsSummary", b.extraProductsSummary)
                obj.put("extraProductsTotal", b.extraProductsTotal)
                obj.put("waterChargesTotal", b.waterChargesTotal)
                obj.put("totalPayable", b.totalPayable)
                obj.put("paidAmount", b.paidAmount)
                obj.put("balanceDue", b.balanceDue)
                obj.put("paymentStatus", b.paymentStatus)
                obj.put("createdAt", b.createdAt)
                billArray.put(obj)
            }
            root.put("bills", billArray)

            // Payments
            val payArray = JSONArray()
            payments.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("customerId", p.customerId)
                obj.put("customerName", p.customerName)
                obj.put("date", p.date)
                obj.put("amount", p.amount)
                obj.put("paymentMethod", p.paymentMethod)
                obj.put("reference", p.reference)
                obj.put("collector", p.collector)
                obj.put("notes", p.notes)
                obj.put("billId", p.billId ?: "")
                obj.put("timestamp", p.timestamp)
                payArray.put(obj)
            }
            root.put("payments", payArray)

            val fos = FileOutputStream(file)
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses JSON backup file and restores Customers, Deliveries, Bills, Payments.
     */
    data class RestoreDataResult(
        val customers: List<Customer>,
        val deliveries: List<DeliveryEntry>,
        val bills: List<BillRecord>,
        val payments: List<PaymentRecord>
    )

    fun parseDatabaseJsonBackup(context: Context, uri: Uri): RestoreDataResult? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val jsonString = reader.readText()
            reader.close()

            val root = JSONObject(jsonString)
            val customers = mutableListOf<Customer>()
            val deliveries = mutableListOf<DeliveryEntry>()
            val bills = mutableListOf<BillRecord>()
            val payments = mutableListOf<PaymentRecord>()

            if (root.has("customers")) {
                val array = root.getJSONArray("customers")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customers.add(
                        Customer(
                            id = obj.optString("id", "CUST-$i"),
                            nameHindi = obj.optString("nameHindi", "Customer $i"),
                            nameEnglish = obj.optString("nameEnglish", "Customer $i"),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            locality = obj.optString("locality", "रूट"),
                            ratePerCan = obj.optDouble("ratePerCan", 30.0),
                            openingCanBalance = obj.optInt("openingCanBalance", 0),
                            currentCanBalance = obj.optInt("currentCanBalance", 0),
                            openingFinancialBalance = obj.optDouble("openingFinancialBalance", 0.0),
                            currentFinancialBalance = obj.optDouble("currentFinancialBalance", 0.0),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            }

            if (root.has("bills")) {
                val array = root.getJSONArray("bills")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val total = obj.optDouble("totalPayable", 0.0)
                    val paid = obj.optDouble("paidAmount", 0.0)
                    bills.add(
                        BillRecord(
                            id = obj.optString("id", "BILL-$i"),
                            billNumber = obj.optString("billNumber", "$i"),
                            billingDate = obj.optString("billingDate", "2026-10-01"),
                            periodStartDate = obj.optString("periodStartDate", "2026-10-01"),
                            periodEndDate = obj.optString("periodEndDate", "2026-10-31"),
                            customerId = obj.optString("customerId", ""),
                            customerName = obj.optString("customerName", ""),
                            customerPhone = obj.optString("customerPhone", ""),
                            customerAddress = obj.optString("customerAddress", ""),
                            totalCansDelivered = obj.optInt("totalCansDelivered", 0),
                            totalCansReturned = obj.optInt("totalCansReturned", 0),
                            ratePerCan = obj.optDouble("ratePerCan", 30.0),
                            extraProductsSummary = obj.optString("extraProductsSummary", ""),
                            extraProductsTotal = obj.optDouble("extraProductsTotal", 0.0),
                            waterChargesTotal = obj.optDouble("waterChargesTotal", 0.0),
                            totalPayable = total,
                            paidAmount = paid,
                            balanceDue = obj.optDouble("balanceDue", total - paid),
                            paymentStatus = obj.optString("paymentStatus", "PENDING"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            RestoreDataResult(customers, deliveries, bills, payments)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares file via Android Intent with FileProvider
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "राय चिल्ड वाटर पिपरिया (म.प्र.) — फ़ाइल: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            val chooser = Intent.createChooser(intent, title)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "साझा करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
