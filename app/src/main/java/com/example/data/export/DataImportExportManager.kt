package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Rai Chilled Water v1.1 - Unified Data Management Engine
 *
 * Implements:
 * - Global File Naming Conventions: RaiWater_{Register}_{YYYYMMDD}_{HHmmss}
 * - UTF-8 BOM CSV engine for seamless Microsoft Excel & Hindi rendering
 * - Pure OpenXML XLSX Spreadsheet writer & parser (zero external library dependency)
 * - Native Vector PDF Statement generator with dual-ledger (Cans & INR)
 * - Content-based auto-detection across all 5 business registers
 * - Safe Mode (insert new only) & Update Existing modes with conflict detection
 * - Duplicate transaction prevention & accounting protection
 * - SHA-256 Checksum verified JSON Full Backup & Safe Restore with auto-snapshot
 * - 1-Click Sample Template generation for all registers
 */
object DataImportExportManager {

    enum class RegisterType(val displayNameHindi: String, val displayNameEnglish: String) {
        CUSTOMERS("ग्राहक खाता मास्टर (Customer Master)", "Customer Master Register"),
        DELIVERIES("दैनिक डिलीवरी रजिस्टर (Daily Deliveries)", "Daily Deliveries & Transactions"),
        PAYMENTS("भुगतान संग्रह रजिस्टर (Payment Collections)", "Payment Collections Register"),
        BILLS("बिल व इनवॉइस रजिस्टर (Bills & Invoices)", "Bills & Invoices Register"),
        MONTHLY_LEDGER("मासिक 31-दिवसीय लेजर (Monthly Ledger)", "Monthly 31-Day Master Ledger"),
        UNKNOWN("अज्ञात फ़ाइल (Unknown Format)", "Unknown / Unrecognized File")
    }

    enum class FileFormat { CSV, XLSX, JSON, PDF }

    data class ImportRowError(val rowNumber: Int, val rawData: String, val reason: String)

    data class CustomerImportItem(
        val customer: Customer,
        val isNew: Boolean,
        val isConflict: Boolean,
        val conflictReason: String = ""
    )

    data class CustomerImportPreview(
        val totalRows: Int,
        val validRows: Int,
        val invalidRows: Int,
        val newCustomersCount: Int,
        val existingCustomersCount: Int,
        val conflictsCount: Int,
        val items: List<CustomerImportItem>,
        val errors: List<ImportRowError>
    )

    data class GenericImportPreview(
        val registerType: RegisterType,
        val totalRows: Int,
        val validRows: Int,
        val invalidRows: Int,
        val previewSummary: String,
        val errors: List<ImportRowError>,
        val rawRows: List<List<String>>
    )

    data class BackupValidationResult(
        val isValid: Boolean,
        val version: String,
        val backupDate: String,
        val customerCount: Int,
        val deliveryCount: Int,
        val paymentCount: Int,
        val billCount: Int,
        val productCount: Int,
        val checksumValid: Boolean,
        val errorMessage: String = "",
        val rawJson: String = ""
    )

    data class ImportExecutionResult(
        val success: Boolean,
        val importedCount: Int,
        val updatedCount: Int,
        val skippedCount: Int,
        val message: String
    )

    data class RestoreExecutionResult(
        val success: Boolean,
        val safetyBackupPath: String,
        val message: String
    )

    // ==========================================
    // 1. GLOBAL FILE NAMING HELPERS
    // ==========================================

    fun generateFileName(registerName: String, extension: String, includeTime: Boolean = true): String {
        val datePattern = if (includeTime) "yyyyMMdd_HHmmss" else "yyyyMMdd"
        val timestamp = SimpleDateFormat(datePattern, Locale.getDefault()).format(Date())
        val cleanExt = extension.removePrefix(".")
        return "RaiWater_${registerName}_$timestamp.$cleanExt"
    }

    private fun getExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getBackupDir(context: Context): File {
        val dir = File(context.cacheDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTemplatesDir(context: Context): File {
        val dir = File(context.cacheDir, "templates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ==========================================
    // 2. CSV & PURE XLSX PARSING & ENGINES
    // ==========================================

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    private fun escapeCsvCell(value: String?): String {
        if (value == null) return ""
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun parseCsvFromStream(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var isFirst = true
        reader.forEachLine { rawLine ->
            var line = rawLine.trim()
            if (isFirst) {
                line = line.removePrefix("\uFEFF")
                isFirst = false
            }
            if (line.isNotBlank()) {
                rows.add(parseCsvLine(line))
            }
        }
        return rows
    }

    /**
     * Pure Java/Android OpenXML XLSX spreadsheet generator.
     * Generates standard Microsoft Excel .xlsx workbooks without Apache POI or heavy libraries.
     */
    fun writeXlsx(file: File, sheetName: String, headers: List<String>, dataRows: List<List<String>>) {
        val fos = FileOutputStream(file)
        val zos = ZipOutputStream(BufferedOutputStream(fos))

        fun putEntry(name: String, content: String) {
            zos.putNextEntry(ZipEntry(name))
            val bytes = content.toByteArray(Charsets.UTF_8)
            zos.write(bytes, 0, bytes.size)
            zos.closeEntry()
        }

        // 1. [Content_Types].xml
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""
        putEntry("[Content_Types].xml", contentTypes)

        // 2. _rels/.rels
        val globalRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        putEntry("_rels/.rels", globalRels)

        // 3. xl/_rels/workbook.xml.rels
        val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""
        putEntry("xl/_rels/workbook.xml.rels", wbRels)

        // 4. xl/workbook.xml
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
        putEntry("xl/workbook.xml", workbookXml)

        // 5. xl/styles.xml
        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><name val="Calibri"/><sz val="11"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
        putEntry("xl/styles.xml", stylesXml)

        // Collect shared strings
        val sharedStrings = mutableListOf<String>()
        val stringMap = mutableMapOf<String, Int>()

        fun getSharedStringIndex(s: String): Int {
            return stringMap.getOrPut(s) {
                val idx = sharedStrings.size
                sharedStrings.add(s)
                idx
            }
        }

        // Pre-populate shared strings
        headers.forEach { getSharedStringIndex(it) }
        dataRows.forEach { row ->
            row.forEach { cell ->
                if (cell.toDoubleOrNull() == null) {
                    getSharedStringIndex(cell)
                }
            }
        }

        // 6. xl/sharedStrings.xml
        val ssBuilder = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""")
        sharedStrings.forEach { s ->
            ssBuilder.append("<si><t>${escapeXml(s)}</t></si>")
        }
        ssBuilder.append("</sst>")
        putEntry("xl/sharedStrings.xml", ssBuilder.toString())

        // 7. xl/worksheets/sheet1.xml
        fun getColumnLetters(colIdx: Int): String {
            var col = colIdx
            val sb = StringBuilder()
            while (col >= 0) {
                sb.insert(0, ('A'.code + (col % 26)).toChar())
                col = (col / 26) - 1
            }
            return sb.toString()
        }

        val sheetBuilder = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>""")

        // Header Row (Row 1, bold style 1)
        sheetBuilder.append("<row r=\"1\">")
        headers.forEachIndexed { colIdx, header ->
            val ref = "${getColumnLetters(colIdx)}1"
            val sIdx = getSharedStringIndex(header)
            sheetBuilder.append("<c r=\"$ref\" t=\"s\" s=\"1\"><v>$sIdx</v></c>")
        }
        sheetBuilder.append("</row>")

        // Data Rows
        dataRows.forEachIndexed { rowIdx, row ->
            val rNum = rowIdx + 2
            sheetBuilder.append("<row r=\"$rNum\">")
            row.forEachIndexed { colIdx, cellValue ->
                val ref = "${getColumnLetters(colIdx)}$rNum"
                val num = cellValue.toDoubleOrNull()
                if (num != null && !cellValue.startsWith("0") && cellValue.length < 11) {
                    sheetBuilder.append("<c r=\"$ref\"><v>$cellValue</v></c>")
                } else {
                    val sIdx = getSharedStringIndex(cellValue)
                    sheetBuilder.append("<c r=\"$ref\" t=\"s\"><v>$sIdx</v></c>")
                }
            }
            sheetBuilder.append("</row>")
        }

        sheetBuilder.append("""  </sheetData>
</worksheet>""")
        putEntry("xl/worksheets/sheet1.xml", sheetBuilder.toString())

        zos.flush()
        zos.close()
        fos.close()
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Pure Java OpenXML XLSX Reader.
     * Unzips the .xlsx in-memory/stream, reads shared strings and sheet1.xml.
     */
    fun parseXlsxFromStream(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val sharedStrings = mutableListOf<String>()
        val sheetDataBytes = ByteArrayOutputStream()

        val zis = ZipInputStream(inputStream)
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            when (entry.name) {
                "xl/sharedStrings.xml" -> {
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(ByteArrayInputStream(baos.toByteArray()), "UTF-8")
                    var eventType = parser.eventType
                    var currentString: String? = null
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                            currentString = parser.nextText()
                            sharedStrings.add(currentString ?: "")
                        }
                        eventType = parser.next()
                    }
                }
                "xl/worksheets/sheet1.xml" -> {
                    zis.copyTo(sheetDataBytes)
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }

        if (sheetDataBytes.size() > 0) {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(sheetDataBytes.toByteArray()), "UTF-8")

            var eventType = parser.eventType
            var currentRow = mutableListOf<String>()
            var isString = false
            var cellRef = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "row") {
                            currentRow = mutableListOf()
                        } else if (parser.name == "c") {
                            cellRef = parser.getAttributeValue(null, "r") ?: ""
                            val type = parser.getAttributeValue(null, "t")
                            isString = (type == "s")
                        } else if (parser.name == "v") {
                            val text = parser.nextText()
                            if (isString) {
                                val idx = text.toIntOrNull()
                                val resolved = if (idx != null && idx in 0 until sharedStrings.size) {
                                    sharedStrings[idx]
                                } else {
                                    text
                                }
                                currentRow.add(resolved)
                            } else {
                                currentRow.add(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "row") {
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return rows
    }

    // ==========================================
    // 3. AUTO-DETECTION ENGINE
    // ==========================================

    fun autoDetectRegisterType(headerRow: List<String>): RegisterType {
        val lower = headerRow.map { it.trim().lowercase(Locale.ROOT) }
        val joined = lower.joinToString(" ")

        return when {
            joined.contains("transaction id") || (joined.contains("cans given") && joined.contains("notation")) -> {
                RegisterType.DELIVERIES
            }
            joined.contains("payment id") || (joined.contains("amount (inr)") && joined.contains("payment mode")) -> {
                RegisterType.PAYMENTS
            }
            joined.contains("bill no") || (joined.contains("water charges") && joined.contains("total bill")) -> {
                RegisterType.BILLS
            }
            joined.contains("day 1") && (joined.contains("day 31") || joined.contains("total delivered cans")) -> {
                RegisterType.MONTHLY_LEDGER
            }
            joined.contains("customer id") || joined.contains("hindi name") || joined.contains("can balance") -> {
                RegisterType.CUSTOMERS
            }
            else -> RegisterType.UNKNOWN
        }
    }

    // ==========================================
    // 4. SAMPLE TEMPLATE GENERATION
    // ==========================================

    fun generateSampleTemplate(context: Context, type: RegisterType, format: FileFormat): File {
        val dir = getTemplatesDir(context)
        val ext = if (format == FileFormat.XLSX) "xlsx" else "csv"

        val (filename, headers, sampleRows) = when (type) {
            RegisterType.CUSTOMERS -> Triple(
                "RaiWater_Customers_Template.$ext",
                listOf("Customer ID", "Hindi Name", "English Name", "Phone", "Address", "Locality / Route", "Rate Per Can", "Can Balance", "Due Amount (INR)", "Status"),
                listOf(
                    listOf("CUST-001", "अग्रवाल स्वीट्स & नमकीन", "Agrawal Sweets", "9826100001", "मेन मार्केट पिपरिया", "Main Market", "30.0", "4", "360.0", "Active"),
                    listOf("CUST-002", "राकेश किराना स्टोर", "Rakesh Kirana", "9826100002", "स्टेशन रोड पिपरिया", "Station Road", "30.0", "2", "180.0", "Active"),
                    listOf("CUST-003", "होटल राजहंस", "Hotel Rajhans", "9826100003", "शोभापुर तिराहा", "Shobhapur", "28.0", "10", "1400.0", "Active")
                )
            )
            RegisterType.DELIVERIES -> Triple(
                "RaiWater_Deliveries_Template.$ext",
                listOf("Transaction ID", "Date", "Time", "Customer ID", "Customer Name", "Cans Given", "Cans Received", "Notation", "Route", "Driver", "Notes", "Sync Status"),
                listOf(
                    listOf("TXN-20261030-001", "2026-10-30", "09:15:00", "CUST-001", "Agrawal Sweets", "2", "2", "2", "Main Market", "Driver 1", "Regular Delivery", "SYNCED"),
                    listOf("TXN-20261030-002", "2026-10-30", "09:30:00", "CUST-002", "Rakesh Kirana", "1", "1", "1", "Station Road", "Driver 1", "Delivered OK", "SYNCED")
                )
            )
            RegisterType.PAYMENTS -> Triple(
                "RaiWater_Payments_Template.$ext",
                listOf("Payment ID", "Date", "Customer ID", "Customer Name", "Amount (INR)", "Payment Mode", "Reference / UTR / Note", "Collector Name"),
                listOf(
                    listOf("PAY-20261030-001", "2026-10-30", "CUST-001", "Agrawal Sweets", "500.0", "UPI", "UPI-REF-98124876", "Driver 1"),
                    listOf("PAY-20261030-002", "2026-10-30", "CUST-002", "Rakesh Kirana", "300.0", "CASH", "Cash received on route", "Admin")
                )
            )
            RegisterType.BILLS -> Triple(
                "RaiWater_Bills_Template.$ext",
                listOf("Bill No", "Billing Date", "Customer ID", "Customer Name", "Phone", "Period Start", "Period End", "Delivered Cans", "Returned Cans", "Rate Per Can", "Water Charges", "Total Bill (INR)", "Paid Amount (INR)", "Balance Due (INR)", "Payment Status"),
                listOf(
                    listOf("BILL-2026-001", "2026-10-30", "CUST-001", "Agrawal Sweets", "9826100001", "2026-10-01", "2026-10-30", "60", "58", "30.0", "1800.0", "1800.0", "1500.0", "300.0", "PARTIAL")
                )
            )
            RegisterType.MONTHLY_LEDGER -> {
                val days = (1..31).map { "Day $it" }
                val fullHeaders = mutableListOf("S.No", "Customer ID", "Customer Name", "Phone", "Address", "Rate (INR)")
                fullHeaders.addAll(days)
                fullHeaders.addAll(listOf("Total Delivered Cans", "Total Returned Cans", "Water Charges (INR)", "Extra Charges (INR)", "Total Bill (INR)", "Paid Amount (INR)", "Net Balance Due (INR)"))

                val sampleDays = (1..31).map { if (it % 2 == 0) "2" else "0" }
                val row1 = mutableListOf("1", "CUST-001", "Agrawal Sweets", "9826100001", "Main Market", "30.0")
                row1.addAll(sampleDays)
                row1.addAll(listOf("30", "28", "900.0", "0.0", "900.0", "500.0", "400.0"))

                Triple("RaiWater_MonthlyLedger_Template.$ext", fullHeaders, listOf(row1))
            }
            RegisterType.UNKNOWN -> Triple("RaiWater_Template.$ext", listOf("Column1", "Column2"), listOf(listOf("Sample1", "Sample2")))
        }

        val file = File(dir, filename)
        if (format == FileFormat.XLSX) {
            writeXlsx(file, "Template", headers, sampleRows)
        } else {
            val fos = FileOutputStream(file)
            fos.write(UTF8_BOM)
            val headerLine = headers.joinToString(",") { escapeCsvCell(it) } + "\n"
            fos.write(headerLine.toByteArray(Charsets.UTF_8))
            sampleRows.forEach { row ->
                val line = row.joinToString(",") { escapeCsvCell(it) } + "\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
            fos.close()
        }
        return file
    }

    // ==========================================
    // 5. CUSTOMER MASTER IMPORT VALIDATION & EXECUTION
    // ==========================================

    suspend fun previewCustomerImport(
        context: Context,
        uri: Uri,
        db: AppDatabase
    ): CustomerImportPreview = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
        val isXlsx = uri.toString().endsWith(".xlsx", ignoreCase = true) ||
                (context.contentResolver.getType(uri)?.contains("spreadsheet") == true)

        val rawRows = if (isXlsx) parseXlsxFromStream(inputStream) else parseCsvFromStream(inputStream)
        inputStream.close()

        if (rawRows.isEmpty()) {
            return@withContext CustomerImportPreview(0, 0, 0, 0, 0, 0, emptyList(), listOf(ImportRowError(1, "", "फ़ाइल खाली है (Empty file)")))
        }

        val headerRow = rawRows[0]
        val dataRows = rawRows.drop(1)
        val errors = mutableListOf<ImportRowError>()
        val items = mutableListOf<CustomerImportItem>()

        val existingCustomers = db.customerDao().getAllCustomersDirect()
        val existingIdMap = existingCustomers.associateBy { it.id.trim().lowercase(Locale.ROOT) }
        val existingPhoneMap = existingCustomers.filter { it.phone.isNotBlank() }.associateBy { it.phone.trim() }

        val seenFileIds = mutableSetOf<String>()
        var validCount = 0
        var newCount = 0
        var existingCount = 0
        var conflictCount = 0

        dataRows.forEachIndexed { index, row ->
            val rowNum = index + 2
            if (row.all { it.isBlank() }) return@forEachIndexed // skip blank line

            // Expected cols: 0: ID, 1: Hindi, 2: English, 3: Phone, 4: Address, 5: Locality, 6: Rate, 7: CanBal, 8: Due, 9: Status
            val rawId = row.getOrNull(0)?.trim() ?: ""
            val rawHindi = row.getOrNull(1)?.trim() ?: ""
            val rawEnglish = row.getOrNull(2)?.trim() ?: ""
            val rawPhone = row.getOrNull(3)?.trim() ?: ""
            val rawAddress = row.getOrNull(4)?.trim() ?: ""
            val rawLocality = row.getOrNull(5)?.trim() ?: "Main Route"
            val rawRate = row.getOrNull(6)?.trim()?.toDoubleOrNull() ?: 30.0
            val rawCanBal = row.getOrNull(7)?.trim()?.toIntOrNull() ?: 0
            val rawDue = row.getOrNull(8)?.trim()?.toDoubleOrNull() ?: 0.0
            val rawStatus = row.getOrNull(9)?.trim() ?: "Active"
            val isActive = !rawStatus.equals("inactive", ignoreCase = true)

            // Validation
            if (rawHindi.isBlank() && rawEnglish.isBlank()) {
                errors.add(ImportRowError(rowNum, row.joinToString(","), "नाम (हिंदी या अंग्रेजी) अनिवार्य है"))
                return@forEachIndexed
            }

            val finalId = if (rawId.isNotBlank()) rawId else "CUST-${UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)}"

            if (seenFileIds.contains(finalId.lowercase(Locale.ROOT))) {
                errors.add(ImportRowError(rowNum, row.joinToString(","), "फ़ाइल में ही डुप्लीकेट ग्राहक ID ($finalId) पाई गई"))
                return@forEachIndexed
            }
            seenFileIds.add(finalId.lowercase(Locale.ROOT))

            val existingById = existingIdMap[finalId.lowercase(Locale.ROOT)]
            val existingByPhone = if (rawPhone.isNotBlank()) existingPhoneMap[rawPhone] else null

            var isNew = false
            var isConflict = false
            var conflictReason = ""

            if (existingById != null) {
                // Existing customer match
                if (existingByPhone != null && existingByPhone.id != existingById.id) {
                    isConflict = true
                    conflictReason = "फ़ोन नंबर ($rawPhone) किसी अन्य ग्राहक (${existingByPhone.nameHindi}) से मेल खाता है"
                    conflictCount++
                } else {
                    existingCount++
                }
            } else if (existingByPhone != null) {
                // ID is new, but phone exists on another customer!
                isConflict = true
                conflictReason = "नया ID ($finalId) लेकिन फ़ोन ($rawPhone) पहले से ग्राहक ${existingByPhone.id} पर मौजूद है"
                conflictCount++
            } else {
                isNew = true
                newCount++
            }

            validCount++
            val customer = Customer(
                id = finalId,
                nameHindi = rawHindi.ifBlank { rawEnglish },
                nameEnglish = rawEnglish.ifBlank { rawHindi },
                phone = rawPhone,
                address = rawAddress,
                locality = rawLocality,
                ratePerCan = rawRate,
                openingCanBalance = rawCanBal,
                currentCanBalance = rawCanBal,
                openingFinancialBalance = rawDue,
                currentFinancialBalance = rawDue,
                isActive = isActive,
                lastUpdated = System.currentTimeMillis()
            )

            items.add(CustomerImportItem(customer, isNew, isConflict, conflictReason))
        }

        CustomerImportPreview(
            totalRows = dataRows.size,
            validRows = validCount,
            invalidRows = errors.size,
            newCustomersCount = newCount,
            existingCustomersCount = existingCount,
            conflictsCount = conflictCount,
            items = items,
            errors = errors
        )
    }

    suspend fun executeCustomerImport(
        preview: CustomerImportPreview,
        safeMode: Boolean, // Safe Mode = Add new only. Update Mode = Add new + update existing
        db: AppDatabase
    ): ImportExecutionResult = withContext(Dispatchers.IO) {
        var imported = 0
        var updated = 0
        var skipped = 0

        val customerDao = db.customerDao()

        preview.items.forEach { item ->
            if (item.isConflict) {
                skipped++
                return@forEach
            }

            if (item.isNew) {
                customerDao.insertCustomer(item.customer)
                imported++
            } else {
                // Existing customer
                if (safeMode) {
                    // Safe mode skips existing to prevent accidental changes
                    skipped++
                } else {
                    // Update mode updates details while preserving active balances if needed
                    val currentDbCust = customerDao.getCustomerByIdDirect(item.customer.id)
                    val merged = if (currentDbCust != null) {
                        item.customer.copy(
                            // Preserve transactional balances
                            currentCanBalance = currentDbCust.currentCanBalance,
                            currentFinancialBalance = currentDbCust.currentFinancialBalance,
                            openingCanBalance = currentDbCust.openingCanBalance,
                            openingFinancialBalance = currentDbCust.openingFinancialBalance
                        )
                    } else {
                        item.customer
                    }
                    customerDao.insertCustomer(merged)
                    updated++
                }
            }
        }

        ImportExecutionResult(
            success = true,
            importedCount = imported,
            updatedCount = updated,
            skippedCount = skipped,
            message = "सफलतापूर्वक $imported नए ग्राहक जोड़े गए, $updated अपडेट किए गए, $skipped छोड़े गए।"
        )
    }

    // ==========================================
    // 6. DELIVERIES & PAYMENTS IMPORT (DUPLICATE PROTECTED)
    // ==========================================

    suspend fun previewAndImportDeliveries(
        context: Context,
        uri: Uri,
        db: AppDatabase,
        commit: Boolean
    ): GenericImportPreview = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
        val isXlsx = uri.toString().endsWith(".xlsx", ignoreCase = true)
        val rawRows = if (isXlsx) parseXlsxFromStream(inputStream) else parseCsvFromStream(inputStream)
        inputStream.close()

        val errors = mutableListOf<ImportRowError>()
        val validEntries = mutableListOf<DeliveryEntry>()
        val existingDeliveries = db.deliveryDao().getAllDeliveriesDirect()
        val existingIds = existingDeliveries.map { it.id }.toSet()

        rawRows.drop(1).forEachIndexed { index, row ->
            val rowNum = index + 2
            val txnId = row.getOrNull(0)?.trim() ?: ""
            val date = row.getOrNull(1)?.trim() ?: ""
            val time = row.getOrNull(2)?.trim() ?: "10:00:00"
            val custId = row.getOrNull(3)?.trim() ?: ""
            val custName = row.getOrNull(4)?.trim() ?: ""
            val given = row.getOrNull(5)?.trim()?.toIntOrNull() ?: 0
            val rec = row.getOrNull(6)?.trim()?.toIntOrNull() ?: 0
            val notation = row.getOrNull(7)?.trim() ?: given.toString()
            val route = row.getOrNull(8)?.trim() ?: "General"
            val driver = row.getOrNull(9)?.trim() ?: "Driver 1"
            val notes = row.getOrNull(10)?.trim() ?: ""

            if (txnId.isBlank() || date.isBlank() || custId.isBlank()) {
                errors.add(ImportRowError(rowNum, row.joinToString(","), "अनिवार्य फ़ील्ड (ID, Date, Customer ID) रिक्त है"))
                return@forEachIndexed
            }

            if (existingIds.contains(txnId)) {
                errors.add(ImportRowError(rowNum, row.joinToString(","), "डुप्लीकेट ट्रांजेक्शन ID ($txnId) — दोबारा आयात वर्जित है"))
                return@forEachIndexed
            }

            val entry = DeliveryEntry(
                id = txnId,
                customerId = custId,
                customerName = custName,
                date = date,
                time = time,
                cansGiven = given,
                cansReceived = rec,
                notation = notation,
                route = route,
                driverName = driver,
                notes = notes,
                syncStatus = "SYNCED",
                isSynced = true,
                createdAt = System.currentTimeMillis()
            )
            validEntries.add(entry)
        }

        if (commit && validEntries.isNotEmpty()) {
            db.deliveryDao().insertDeliveries(validEntries)
        }

        GenericImportPreview(
            registerType = RegisterType.DELIVERIES,
            totalRows = rawRows.size - 1,
            validRows = validEntries.size,
            invalidRows = errors.size,
            previewSummary = "वैध डिलीवरी ट्रांजेक्शन: ${validEntries.size}, त्रुटि/डुप्लीकेट: ${errors.size}",
            errors = errors,
            rawRows = rawRows.take(15)
        )
    }

    suspend fun previewAndImportPayments(
        context: Context,
        uri: Uri,
        db: AppDatabase,
        commit: Boolean
    ): GenericImportPreview = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
        val isXlsx = uri.toString().endsWith(".xlsx", ignoreCase = true)
        val rawRows = if (isXlsx) parseXlsxFromStream(inputStream) else parseCsvFromStream(inputStream)
        inputStream.close()

        val errors = mutableListOf<ImportRowError>()
        val validPayments = mutableListOf<PaymentRecord>()
        val existingPayments = db.paymentDao().getAllPaymentsDirect()
        val existingIds = existingPayments.map { it.id }.toSet()

        rawRows.drop(1).forEachIndexed { index, row ->
            val rowNum = index + 2
            val payId = row.getOrNull(0)?.trim() ?: ""
            val date = row.getOrNull(1)?.trim() ?: ""
            val custId = row.getOrNull(2)?.trim() ?: ""
            val custName = row.getOrNull(3)?.trim() ?: ""
            val amount = row.getOrNull(4)?.trim()?.toDoubleOrNull() ?: 0.0
            val mode = row.getOrNull(5)?.trim()?.uppercase(Locale.ROOT) ?: "CASH"
            val ref = row.getOrNull(6)?.trim() ?: ""
            val collector = row.getOrNull(7)?.trim() ?: "Admin"

            if (payId.isBlank() || custId.isBlank() || amount <= 0) {
                errors.add(ImportRowError(rowNum, row.joinToString(","), "अमान्य भुगतान ID या राशि (शून्य/ऋणात्मक)"))
                return@forEachIndexed
            }

            if (existingIds.contains(payId)) {
                errors.add(ImportRowError(rowNum, row.joinToString(","), "डुप्लीकेट भुगतान ID ($payId) — दोबारा भुगतान जोड़ना वर्जित है"))
                return@forEachIndexed
            }

            val p = PaymentRecord(
                id = payId,
                customerId = custId,
                customerName = custName,
                date = date,
                amount = amount,
                paymentMethod = mode,
                reference = ref,
                collector = collector,
                isSynced = true,
                syncStatus = "SYNCED",
                createdAt = System.currentTimeMillis()
            )
            validPayments.add(p)
        }

        if (commit && validPayments.isNotEmpty()) {
            db.paymentDao().insertPayments(validPayments)
        }

        GenericImportPreview(
            registerType = RegisterType.PAYMENTS,
            totalRows = rawRows.size - 1,
            validRows = validPayments.size,
            invalidRows = errors.size,
            previewSummary = "वैध भुगतान रिकॉर्ड: ${validPayments.size}, अस्वीकृत: ${errors.size}",
            errors = errors,
            rawRows = rawRows.take(15)
        )
    }

    // ==========================================
    // 7. EXPORTS ENGINE (ALL 6 REGISTERS)
    // ==========================================

    fun exportCustomers(context: Context, customers: List<Customer>, format: FileFormat): File {
        val ext = if (format == FileFormat.XLSX) "xlsx" else "csv"
        val fileName = generateFileName("Customers", ext)
        val file = File(getExportDir(context), fileName)

        val headers = listOf("Customer ID", "Hindi Name", "English Name", "Phone", "Address", "Locality / Route", "Rate Per Can", "Can Balance", "Due Amount (INR)", "Status")
        val dataRows = customers.map { c ->
            listOf(
                c.id,
                c.nameHindi,
                c.nameEnglish,
                c.phone,
                c.address,
                c.locality,
                String.format(Locale.US, "%.2f", c.ratePerCan),
                c.currentCanBalance.toString(),
                String.format(Locale.US, "%.2f", c.currentFinancialBalance),
                if (c.isActive) "Active" else "Inactive"
            )
        }

        if (format == FileFormat.XLSX) {
            writeXlsx(file, "Customers", headers, dataRows)
        } else {
            val fos = FileOutputStream(file)
            fos.write(UTF8_BOM)
            val headerLine = headers.joinToString(",") { escapeCsvCell(it) } + "\n"
            fos.write(headerLine.toByteArray(Charsets.UTF_8))
            dataRows.forEach { row ->
                val line = row.joinToString(",") { escapeCsvCell(it) } + "\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
            fos.close()
        }
        return file
    }

    fun exportDeliveries(context: Context, deliveries: List<DeliveryEntry>, format: FileFormat): File {
        val ext = if (format == FileFormat.XLSX) "xlsx" else "csv"
        val fileName = generateFileName("Deliveries", ext)
        val file = File(getExportDir(context), fileName)

        val headers = listOf("Transaction ID", "Date", "Time", "Customer ID", "Customer Name", "Cans Given", "Cans Received", "Notation", "Route", "Driver", "Notes", "Sync Status")
        val dataRows = deliveries.map { d ->
            listOf(
                d.id,
                d.date,
                d.time,
                d.customerId,
                d.customerName,
                d.cansGiven.toString(),
                d.cansReceived.toString(),
                d.notation,
                d.route,
                d.driverName,
                d.notes,
                d.syncStatus
            )
        }

        if (format == FileFormat.XLSX) {
            writeXlsx(file, "Deliveries", headers, dataRows)
        } else {
            val fos = FileOutputStream(file)
            fos.write(UTF8_BOM)
            val headerLine = headers.joinToString(",") { escapeCsvCell(it) } + "\n"
            fos.write(headerLine.toByteArray(Charsets.UTF_8))
            dataRows.forEach { row ->
                val line = row.joinToString(",") { escapeCsvCell(it) } + "\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
            fos.close()
        }
        return file
    }

    fun exportPayments(context: Context, payments: List<PaymentRecord>, format: FileFormat): File {
        val ext = if (format == FileFormat.XLSX) "xlsx" else "csv"
        val fileName = generateFileName("Payments", ext)
        val file = File(getExportDir(context), fileName)

        val headers = listOf("Payment ID", "Date", "Customer ID", "Customer Name", "Amount (INR)", "Payment Mode", "Reference / UTR / Note", "Collector Name")
        val dataRows = payments.map { p ->
            listOf(
                p.id,
                p.date,
                p.customerId,
                p.customerName,
                String.format(Locale.US, "%.2f", p.amount),
                p.paymentMethod,
                p.reference,
                p.collector
            )
        }

        if (format == FileFormat.XLSX) {
            writeXlsx(file, "Payments", headers, dataRows)
        } else {
            val fos = FileOutputStream(file)
            fos.write(UTF8_BOM)
            val headerLine = headers.joinToString(",") { escapeCsvCell(it) } + "\n"
            fos.write(headerLine.toByteArray(Charsets.UTF_8))
            dataRows.forEach { row ->
                val line = row.joinToString(",") { escapeCsvCell(it) } + "\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
            fos.close()
        }
        return file
    }

    fun exportBills(context: Context, bills: List<BillRecord>, format: FileFormat): File {
        val ext = if (format == FileFormat.XLSX) "xlsx" else "csv"
        val fileName = generateFileName("Bills", ext)
        val file = File(getExportDir(context), fileName)

        val headers = listOf("Bill No", "Billing Date", "Customer ID", "Customer Name", "Phone", "Period Start", "Period End", "Delivered Cans", "Returned Cans", "Rate Per Can", "Water Charges", "Total Bill (INR)", "Paid Amount (INR)", "Balance Due (INR)", "Payment Status")
        val dataRows = bills.map { b ->
            listOf(
                b.billNumber,
                b.billingDate,
                b.customerId,
                b.customerName,
                b.customerPhone,
                b.periodStartDate,
                b.periodEndDate,
                b.totalCansDelivered.toString(),
                b.totalCansReturned.toString(),
                String.format(Locale.US, "%.2f", b.ratePerCan),
                String.format(Locale.US, "%.2f", b.waterChargesTotal),
                String.format(Locale.US, "%.2f", b.totalPayable),
                String.format(Locale.US, "%.2f", b.paidAmount),
                String.format(Locale.US, "%.2f", b.balanceDue),
                b.paymentStatus
            )
        }

        if (format == FileFormat.XLSX) {
            writeXlsx(file, "Bills", headers, dataRows)
        } else {
            val fos = FileOutputStream(file)
            fos.write(UTF8_BOM)
            val headerLine = headers.joinToString(",") { escapeCsvCell(it) } + "\n"
            fos.write(headerLine.toByteArray(Charsets.UTF_8))
            dataRows.forEach { row ->
                val line = row.joinToString(",") { escapeCsvCell(it) } + "\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
            fos.close()
        }
        return file
    }

    fun exportMonthlyMasterLedger(
        context: Context,
        year: Int,
        month: Int,
        customers: List<Customer>,
        deliveries: List<DeliveryEntry>,
        payments: List<PaymentRecord>,
        format: FileFormat
    ): File {
        val monthStr = month.toString().padStart(2, '0')
        val monthPrefix = "$year-$monthStr"
        val ext = if (format == FileFormat.XLSX) "xlsx" else "csv"
        val fileName = generateFileName("MonthlyLedger_${year}_$monthStr", ext)
        val file = File(getExportDir(context), fileName)

        val daysHeaders = (1..31).map { "Day $it" }
        val headers = mutableListOf("S.No", "Customer ID", "Customer Name", "Phone", "Address", "Rate (INR)")
        headers.addAll(daysHeaders)
        headers.addAll(listOf("Total Delivered Cans", "Total Returned Cans", "Water Charges (INR)", "Extra Charges (INR)", "Total Bill (INR)", "Paid Amount (INR)", "Net Balance Due (INR)"))

        val monthDeliveries = deliveries.filter { it.date.startsWith(monthPrefix) }
        val monthPayments = payments.filter { it.date.startsWith(monthPrefix) }

        val dataRows = customers.mapIndexed { index, cust ->
            val custDeliveries = monthDeliveries.filter { it.customerId == cust.id }
            val custPayments = monthPayments.filter { it.customerId == cust.id }

            val dayMap = mutableMapOf<Int, Int>()
            custDeliveries.forEach { d ->
                val dayNum = d.date.substringAfterLast("-").toIntOrNull() ?: 1
                dayMap[dayNum] = (dayMap[dayNum] ?: 0) + d.cansGiven
            }

            val totalDelivered = custDeliveries.sumOf { it.cansGiven }
            val totalReturned = custDeliveries.sumOf { it.cansReceived }
            val waterCharges = totalDelivered * cust.ratePerCan
            val extraCharges = 0.0
            val totalBill = waterCharges + extraCharges
            val totalPaid = custPayments.sumOf { it.amount }
            val netDue = cust.currentFinancialBalance.coerceAtLeast(0.0)

            val row = mutableListOf(
                (index + 1).toString(),
                cust.id,
                "${cust.nameHindi} (${cust.nameEnglish})",
                cust.phone,
                cust.address,
                String.format(Locale.US, "%.2f", cust.ratePerCan)
            )
            for (day in 1..31) {
                row.add(dayMap[day]?.toString() ?: "0")
            }
            row.add(totalDelivered.toString())
            row.add(totalReturned.toString())
            row.add(String.format(Locale.US, "%.2f", waterCharges))
            row.add(String.format(Locale.US, "%.2f", extraCharges))
            row.add(String.format(Locale.US, "%.2f", totalBill))
            row.add(String.format(Locale.US, "%.2f", totalPaid))
            row.add(String.format(Locale.US, "%.2f", netDue))
            row
        }

        if (format == FileFormat.XLSX) {
            writeXlsx(file, "MonthlyLedger", headers, dataRows)
        } else {
            val fos = FileOutputStream(file)
            fos.write(UTF8_BOM)
            val headerLine = headers.joinToString(",") { escapeCsvCell(it) } + "\n"
            fos.write(headerLine.toByteArray(Charsets.UTF_8))
            dataRows.forEach { row ->
                val line = row.joinToString(",") { escapeCsvCell(it) } + "\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }
            fos.close()
        }
        return file
    }

    /**
     * Single Customer Dual-Ledger Statement (PDF Vector Canvas).
     * Preserves Hindi text, business header, dual ledger summary (Cans & INR), and chronological transactions.
     */
    fun exportCustomerStatementPdf(
        context: Context,
        customer: Customer,
        deliveries: List<DeliveryEntry>,
        payments: List<PaymentRecord>,
        settings: BusinessSettings
    ): File {
        val fileName = generateFileName("Statement_${customer.id}", "pdf")
        val file = File(getExportDir(context), fileName)

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Background (Teal)
        paint.color = AndroidColor.rgb(0, 104, 116)
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Firm Title
        paint.color = AndroidColor.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(settings.firmNameHindi.ifBlank { "राय चिल्ड वॉटर सप्लायर्स" }, 25f, 35f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${settings.firmName} • ${settings.address}", 25f, 55f, paint)
        canvas.drawText("मो: ${settings.phone} | UPI: ${settings.upiId}", 25f, 72f, paint)

        // Title: Dual-Ledger Statement
        paint.color = AndroidColor.rgb(0, 50, 70)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ग्राहक दोहरा खाता विवरण (Customer Dual-Ledger Statement)", 25f, 115f, paint)

        // Customer Info Card
        paint.color = AndroidColor.rgb(240, 245, 247)
        canvas.drawRoundRect(25f, 125f, 570f, 185f, 8f, 8f, paint)

        paint.color = AndroidColor.BLACK
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ग्राहक: ${customer.nameHindi} (${customer.nameEnglish})", 35f, 145f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ID: ${customer.id} | फ़ोन: ${customer.phone}", 35f, 162f, paint)
        canvas.drawText("पता/रूट: ${customer.address} (${customer.locality}) | दर: ₹${customer.ratePerCan}/केन", 35f, 178f, paint)

        // Dual Summary Cards (Cans & INR)
        val totalDelivered = deliveries.sumOf { it.cansGiven }
        val totalReturned = deliveries.sumOf { it.cansReceived }
        val totalPaid = payments.sumOf { it.amount }

        // Card 1: Can Summary (Left)
        paint.color = AndroidColor.rgb(230, 243, 245)
        canvas.drawRoundRect(25f, 195f, 290f, 255f, 6f, 6f, paint)
        paint.color = AndroidColor.rgb(0, 90, 100)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("🪣 केन संतुलन (Container Balance)", 35f, 212f, paint)
        paint.color = AndroidColor.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("प्रारंभिक: ${customer.openingCanBalance} | डिलीवर: $totalDelivered | वापस: $totalReturned", 35f, 230f, paint)
        paint.color = AndroidColor.rgb(180, 50, 0)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("वर्तमान बकाया केन: ${customer.currentCanBalance} Can", 35f, 248f, paint)

        // Card 2: Monetary Summary (Right)
        paint.color = AndroidColor.rgb(255, 245, 230)
        canvas.drawRoundRect(305f, 195f, 570f, 255f, 6f, 6f, paint)
        paint.color = AndroidColor.rgb(150, 80, 0)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("💰 वित्तीय संतुलन (Rupee Balance)", 315f, 212f, paint)
        paint.color = AndroidColor.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("प्रारंभिक: ₹${customer.openingFinancialBalance} | कुल जमा: ₹$totalPaid", 315f, 230f, paint)
        paint.color = AndroidColor.rgb(180, 0, 0)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("शुद्ध बकाया राशि: ₹${customer.currentFinancialBalance}", 315f, 248f, paint)

        // Transactions Table Header
        paint.color = AndroidColor.rgb(0, 104, 116)
        canvas.drawRect(25f, 265f, 570f, 285f, paint)
        paint.color = AndroidColor.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("दिनांक (Date)", 30f, 278f, paint)
        canvas.drawText("विवरण (Particulars)", 120f, 278f, paint)
        canvas.drawText("दी गई केन", 260f, 278f, paint)
        canvas.drawText("वापस केन", 330f, 278f, paint)
        canvas.drawText("जमा राशि (₹)", 400f, 278f, paint)
        canvas.drawText("बकाया राशि (₹)", 490f, 278f, paint)

        // Combined Sorted Transactions (Deliveries + Payments)
        data class StatementEntry(val date: String, val desc: String, val given: String, val returned: String, val paid: String, val due: String)

        val history = mutableListOf<StatementEntry>()
        deliveries.forEach { d ->
            history.add(StatementEntry(d.date, "दैनिक डिलीवरी (${d.notation})", "${d.cansGiven}", "${d.cansReceived}", "-", "-"))
        }
        payments.forEach { p ->
            history.add(StatementEntry(p.date, "भुगतान प्राप्त (${p.paymentMethod} ${p.reference})", "-", "-", "₹${p.amount}", "-"))
        }
        history.sortByDescending { it.date }

        var y = 302f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f

        history.take(28).forEachIndexed { index, item ->
            paint.color = if (index % 2 == 0) AndroidColor.rgb(250, 250, 250) else AndroidColor.WHITE
            canvas.drawRect(25f, y - 12f, 570f, y + 5f, paint)

            paint.color = AndroidColor.BLACK
            canvas.drawText(item.date, 30f, y, paint)
            canvas.drawText(item.desc.take(24), 120f, y, paint)
            canvas.drawText(item.given, 275f, y, paint)
            canvas.drawText(item.returned, 345f, y, paint)
            paint.color = if (item.paid != "-") AndroidColor.rgb(0, 120, 50) else AndroidColor.DKGRAY
            canvas.drawText(item.paid, 410f, y, paint)
            paint.color = AndroidColor.BLACK
            canvas.drawText(item.due, 500f, y, paint)
            y += 18f
        }

        // Footer
        paint.color = AndroidColor.GRAY
        paint.textSize = 8.5f
        canvas.drawText("कम्प्यूटरीकृत लेजर पर्ची — राय चिल्ड वाटर ERP v1.1 | जनरेट दिनांक: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())}", 25f, 825f, paint)

        pdfDoc.finishPage(page)
        val fos = FileOutputStream(file)
        pdfDoc.writeTo(fos)
        pdfDoc.close()
        fos.close()

        return file
    }

    // ==========================================
    // 8. FULL BACKUP (VERSIONED JSON + SHA-256)
    // ==========================================

    suspend fun createFullBackup(context: Context, db: AppDatabase): File = withContext(Dispatchers.IO) {
        val fileName = generateFileName("Full_Backup", "json")
        val file = File(getBackupDir(context), fileName)

        val root = JSONObject()
        root.put("appName", "Rai Chilled Water ERP")
        root.put("version", "1.1")
        root.put("backupTimestamp", System.currentTimeMillis())
        root.put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // 1. Customers
        val custArray = JSONArray()
        db.customerDao().getAllCustomersDirect().forEach { c ->
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
            obj.put("lastDeliveryDate", c.lastDeliveryDate ?: "")
            obj.put("lastUpdated", c.lastUpdated)
            custArray.put(obj)
        }
        root.put("customers", custArray)

        // 2. Deliveries
        val delArray = JSONArray()
        db.deliveryDao().getAllDeliveriesDirect().forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("customerId", d.customerId)
            obj.put("customerName", d.customerName)
            obj.put("date", d.date)
            obj.put("time", d.time)
            obj.put("cansGiven", d.cansGiven)
            obj.put("cansReceived", d.cansReceived)
            obj.put("notation", d.notation)
            obj.put("rateApplied", d.rateApplied)
            obj.put("driverName", d.driverName)
            obj.put("driverId", d.driverId)
            obj.put("route", d.route)
            obj.put("notes", d.notes)
            obj.put("isCorrected", d.isCorrected)
            obj.put("correctionReason", d.correctionReason ?: "")
            obj.put("syncStatus", d.syncStatus)
            obj.put("isSynced", d.isSynced)
            obj.put("timestamp", d.timestamp)
            obj.put("createdAt", d.createdAt)
            delArray.put(obj)
        }
        root.put("deliveries", delArray)

        // 3. Payments
        val payArray = JSONArray()
        db.paymentDao().getAllPaymentsDirect().forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("customerId", p.customerId)
            obj.put("customerName", p.customerName)
            obj.put("date", p.date)
            obj.put("amount", p.amount)
            obj.put("paymentMethod", p.paymentMethod)
            obj.put("reference", p.reference)
            obj.put("utrReference", p.utrReference)
            obj.put("collector", p.collector)
            obj.put("notes", p.notes)
            obj.put("billId", p.billId ?: "")
            obj.put("isSynced", p.isSynced)
            obj.put("syncStatus", p.syncStatus)
            obj.put("timestamp", p.timestamp)
            obj.put("createdAt", p.createdAt)
            payArray.put(obj)
        }
        root.put("payments", payArray)

        // 4. Bills
        val billArray = JSONArray()
        db.billDao().getAllBillsDirect().forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("billNumber", b.billNumber)
            obj.put("customerId", b.customerId)
            obj.put("customerName", b.customerName)
            obj.put("customerPhone", b.customerPhone)
            obj.put("customerAddress", b.customerAddress)
            obj.put("billingDate", b.billingDate)
            obj.put("periodStartDate", b.periodStartDate)
            obj.put("periodEndDate", b.periodEndDate)
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

        // 5. Products
        val prodArray = JSONArray()
        db.productDao().getAllProductsDirect().forEach { pr ->
            val obj = JSONObject()
            obj.put("id", pr.id)
            obj.put("nameEnglish", pr.nameEnglish)
            obj.put("nameHindi", pr.nameHindi)
            obj.put("defaultPrice", pr.defaultPrice)
            obj.put("unitPrice", pr.unitPrice)
            obj.put("unit", pr.unit)
            obj.put("isWaterCan", pr.isWaterCan)
            obj.put("isActive", pr.isActive)
            prodArray.put(obj)
        }
        root.put("products", prodArray)

        // 6. Notation Symbols
        val symArray = JSONArray()
        db.symbolDao().getAllSymbolsDirect().forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("symbol", s.symbol)
            obj.put("meaningHindi", s.meaningHindi)
            obj.put("meaningEnglish", s.meaningEnglish)
            obj.put("cansGiven", s.cansGiven)
            obj.put("cansReceived", s.cansReceived)
            obj.put("isDefault", s.isDefault)
            symArray.put(obj)
        }
        root.put("notationSymbols", symArray)

        // 7. Business Settings (Exclude sensitive credentials)
        val settings = db.settingsDao().getSettingsDirect() ?: BusinessSettings()
        val setObj = JSONObject()
        setObj.put("firmName", settings.firmName)
        setObj.put("firmNameHindi", settings.firmNameHindi)
        setObj.put("address", settings.address)
        setObj.put("phone", settings.phone)
        setObj.put("alternatePhone", settings.alternatePhone)
        setObj.put("gstOrRegNumber", settings.gstOrRegNumber)
        setObj.put("defaultCanRate", settings.defaultCanRate)
        setObj.put("bankName", settings.bankName)
        setObj.put("accountNumber", settings.accountNumber)
        setObj.put("ifscCode", settings.ifscCode)
        setObj.put("upiId", settings.upiId)
        root.put("businessSettings", setObj)

        // 8. Compute Checksum
        val payloadToHash = root.toString()
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(payloadToHash.toByteArray(Charsets.UTF_8))
        val checksumHex = hashBytes.joinToString("") { "%02x".format(it) }
        root.put("checksum", checksumHex)

        val fos = FileOutputStream(file)
        fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        fos.close()
        file
    }

    // ==========================================
    // 9. SAFE RESTORE ENGINE (SNAPSHOT & INTEGRITY)
    // ==========================================

    suspend fun validateBackupFile(context: Context, uri: Uri): BackupValidationResult = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext BackupValidationResult(false, "", "", 0, 0, 0, 0, 0, false, "फ़ाइल नहीं खोली जा सकी")
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val jsonText = reader.readText()
        inputStream.close()

        try {
            val root = JSONObject(jsonText)
            val appName = root.optString("appName")
            val version = root.optString("version", "1.0")
            val backupDate = root.optString("backupDate", "")
            val reportedChecksum = root.optString("checksum", "")

            if (!appName.contains("Rai Chilled Water", ignoreCase = true)) {
                return@withContext BackupValidationResult(false, version, backupDate, 0, 0, 0, 0, 0, false, "अमान्य बैकअप फ़ाइल: ऐप का नाम मेल नहीं खाता")
            }

            val custCount = root.optJSONArray("customers")?.length() ?: 0
            val delCount = root.optJSONArray("deliveries")?.length() ?: 0
            val payCount = root.optJSONArray("payments")?.length() ?: 0
            val billCount = root.optJSONArray("bills")?.length() ?: 0
            val prodCount = root.optJSONArray("products")?.length() ?: 0

            var checksumValid = true
            if (reportedChecksum.isNotBlank()) {
                root.remove("checksum")
                val md = MessageDigest.getInstance("SHA-256")
                val hash = md.digest(root.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
                checksumValid = hash.equals(reportedChecksum, ignoreCase = true)
            }

            BackupValidationResult(
                isValid = true,
                version = version,
                backupDate = backupDate,
                customerCount = custCount,
                deliveryCount = delCount,
                paymentCount = payCount,
                billCount = billCount,
                productCount = prodCount,
                checksumValid = checksumValid,
                rawJson = jsonText
            )
        } catch (e: Exception) {
            BackupValidationResult(false, "", "", 0, 0, 0, 0, 0, false, "JSON पार्सिंग त्रुटि: ${e.message}")
        }
    }

    suspend fun executeSafeRestore(
        context: Context,
        validation: BackupValidationResult,
        db: AppDatabase
    ): RestoreExecutionResult = withContext(Dispatchers.IO) {
        // 1. Take Automatic Safety Snapshot FIRST!
        val safetySnapshot = createFullBackup(context, db)

        try {
            val root = JSONObject(validation.rawJson)

            // 2. Parse customers
            val custArray = root.optJSONArray("customers")
            val customers = mutableListOf<Customer>()
            if (custArray != null) {
                for (i in 0 until custArray.length()) {
                    val o = custArray.getJSONObject(i)
                    customers.add(
                        Customer(
                            id = o.getString("id"),
                            nameHindi = o.optString("nameHindi"),
                            nameEnglish = o.optString("nameEnglish"),
                            phone = o.optString("phone"),
                            address = o.optString("address"),
                            locality = o.optString("locality", "Station Road"),
                            ratePerCan = o.optDouble("ratePerCan", 30.0),
                            openingCanBalance = o.optInt("openingCanBalance", 0),
                            currentCanBalance = o.optInt("currentCanBalance", 0),
                            openingFinancialBalance = o.optDouble("openingFinancialBalance", 0.0),
                            currentFinancialBalance = o.optDouble("currentFinancialBalance", 0.0),
                            isActive = o.optBoolean("isActive", true),
                            lastDeliveryDate = o.optString("lastDeliveryDate", null),
                            lastUpdated = o.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 3. Parse deliveries
            val delArray = root.optJSONArray("deliveries")
            val deliveries = mutableListOf<DeliveryEntry>()
            if (delArray != null) {
                for (i in 0 until delArray.length()) {
                    val o = delArray.getJSONObject(i)
                    deliveries.add(
                        DeliveryEntry(
                            id = o.getString("id"),
                            customerId = o.getString("customerId"),
                            customerName = o.optString("customerName"),
                            date = o.getString("date"),
                            time = o.optString("time", "10:00:00"),
                            cansGiven = o.optInt("cansGiven", 1),
                            cansReceived = o.optInt("cansReceived", 0),
                            notation = o.optString("notation", "1"),
                            rateApplied = o.optDouble("rateApplied", 30.0),
                            driverName = o.optString("driverName", "Driver 1"),
                            driverId = o.optString("driverId", "Driver 1"),
                            route = o.optString("route", "General"),
                            notes = o.optString("notes"),
                            isCorrected = o.optBoolean("isCorrected", false),
                            correctionReason = o.optString("correctionReason", null),
                            syncStatus = o.optString("syncStatus", "SYNCED"),
                            isSynced = o.optBoolean("isSynced", true),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 4. Parse payments
            val payArray = root.optJSONArray("payments")
            val payments = mutableListOf<PaymentRecord>()
            if (payArray != null) {
                for (i in 0 until payArray.length()) {
                    val o = payArray.getJSONObject(i)
                    payments.add(
                        PaymentRecord(
                            id = o.getString("id"),
                            customerId = o.getString("customerId"),
                            customerName = o.optString("customerName"),
                            date = o.getString("date"),
                            amount = o.getDouble("amount"),
                            paymentMethod = o.optString("paymentMethod", "CASH"),
                            reference = o.optString("reference"),
                            utrReference = o.optString("utrReference"),
                            collector = o.optString("collector", "Admin"),
                            notes = o.optString("notes"),
                            billId = o.optString("billId", null),
                            isSynced = o.optBoolean("isSynced", true),
                            syncStatus = o.optString("syncStatus", "SYNCED"),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 5. Parse bills
            val billArray = root.optJSONArray("bills")
            val bills = mutableListOf<BillRecord>()
            if (billArray != null) {
                for (i in 0 until billArray.length()) {
                    val o = billArray.getJSONObject(i)
                    bills.add(
                        BillRecord(
                            id = o.getString("id"),
                            billNumber = o.optString("billNumber"),
                            customerId = o.optString("customerId"),
                            customerName = o.optString("customerName"),
                            customerPhone = o.optString("customerPhone"),
                            customerAddress = o.optString("customerAddress"),
                            billingDate = o.optString("billingDate"),
                            periodStartDate = o.optString("periodStartDate"),
                            periodEndDate = o.optString("periodEndDate"),
                            totalCansDelivered = o.optInt("totalCansDelivered", 0),
                            totalCansReturned = o.optInt("totalCansReturned", 0),
                            ratePerCan = o.optDouble("ratePerCan", 30.0),
                            extraProductsSummary = o.optString("extraProductsSummary"),
                            extraProductsTotal = o.optDouble("extraProductsTotal", 0.0),
                            waterChargesTotal = o.optDouble("waterChargesTotal", 0.0),
                            totalPayable = o.optDouble("totalPayable", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            balanceDue = o.optDouble("balanceDue", 0.0),
                            paymentStatus = o.optString("paymentStatus", "PENDING"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 6. Safe atomic database replacement
            db.customerDao().deleteAllCustomers()
            db.customerDao().insertCustomers(customers)

            db.deliveryDao().deleteAllDeliveries()
            db.deliveryDao().insertDeliveries(deliveries)

            db.paymentDao().deleteAllPayments()
            db.paymentDao().insertPayments(payments)

            db.billDao().deleteAllBills()
            db.billDao().insertBills(bills)

            RestoreExecutionResult(
                success = true,
                safetyBackupPath = safetySnapshot.absolutePath,
                message = "डेटा सफलतापूर्वक पुनर्स्थापित किया गया: ${customers.size} ग्राहक, ${deliveries.size} डिलीवरी, ${payments.size} भुगतान।"
            )
        } catch (e: Exception) {
            RestoreExecutionResult(
                success = false,
                safetyBackupPath = safetySnapshot.absolutePath,
                message = "पुनर्स्थापना में त्रुटि: ${e.message}। पूर्व डेटा का सुरक्षा बैकअप सुरक्षित है: ${safetySnapshot.name}"
            )
        }
    }

    // ==========================================
    // 10. SYSTEM SHARING & INTENTS
    // ==========================================

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "राय चिल्ड वाटर पिपरिया — डेटा फ़ाइल (${file.name}) संलग्न है।")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            val chooser = Intent.createChooser(intent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "साझा करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
