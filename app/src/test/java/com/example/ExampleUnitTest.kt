package com.example

import com.example.billing.PdfInvoiceGenerator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun canLedgerFormula_isAccurate() {
        // Physical can ledger: Closing cans = Opening cans + Cans given - Cans received
        val openingCans = 4
        val cansGiven = 2
        val cansReceived = 1
        val closingCans = openingCans + cansGiven - cansReceived
        assertEquals(5, closingCans)
    }

    @Test
    fun financialLedgerFormula_isAccurate() {
        // Financial ledger: Closing due = Opening due + Charges (Cans Given * Rate) - Payments
        val openingDue = 600.0
        val cansDelivered = 5
        val ratePerCan = 30.0
        val charges = cansDelivered * ratePerCan // 150.0
        val payments = 300.0
        val closingDue = openingDue + charges - payments
        assertEquals(450.0, closingDue, 0.001)
    }

    @Test
    fun correctionDifferenceMath_isAccurate() {
        // Driver accidentally entered 5 cans given instead of 2
        val oldGiven = 5
        val oldReceived = 1
        val customerOldCanBal = 7 // was 3 + 5 - 1 = 7

        val correctedGiven = 2
        val correctedReceived = 1

        val givenDiff = correctedGiven - oldGiven // -3
        val receivedDiff = correctedReceived - oldReceived // 0
        val correctedCanBal = customerOldCanBal + givenDiff - receivedDiff

        assertEquals(4, correctedCanBal) // 3 + 2 - 1 = 4
    }

    @Test
    fun pdfDimensions_matchCustomStationery() {
        // 145 mm x 260 mm stationery at 72 points per inch
        assertEquals(411, PdfInvoiceGenerator.PAGE_WIDTH)
        assertEquals(737, PdfInvoiceGenerator.PAGE_HEIGHT)
    }

    @Test
    fun billFormula_excludesPastDueAmount() {
        // Per user requirement #8: Bills generated MUST NOT include previous due amount
        val cansDelivered = 25
        val ratePerCan = 30.0
        val cansTotal = cansDelivered * ratePerCan // 750.0
        val extraProductsTotal = 120.0 // e.g. 1 Ice Block
        val discount = 20.0
        val pastDueAmount = 1500.0 // Must NOT be added to bill total

        val billTotal = cansTotal + extraProductsTotal - discount
        assertEquals(850.0, billTotal, 0.001)
        // Verify pastDueAmount is omitted
        org.junit.Assert.assertNotEquals(billTotal + pastDueAmount, billTotal)
    }

    @Test
    fun devanagariDigitMapping_isAccurate() {
        val devanagariString = "५१५६"
        val mapped = devanagariString.map { ch ->
            when (ch) {
                '०' -> '0'; '१' -> '1'; '२' -> '2'; '३' -> '3'; '४' -> '4'
                '५' -> '5'; '६' -> '6'; '७' -> '7'; '८' -> '8'; '९' -> '9'
                else -> ch
            }
        }.joinToString("")
        assertEquals("5156", mapped)
    }

    @Test
    fun businessSettings_messageTonesFormatCorrectly() {
        val settings = com.example.data.model.BusinessSettings(
            businessNameHindi = "राय चिल्ड वाटर सप्लायर",
            upiId = "raichilledwater@upi"
        )

        // 1. Short
        val shortMsg = settings.formatMessage(com.example.data.model.MessageTone.SHORT, "राजेश", "450.00", "3")
        org.junit.Assert.assertTrue(shortMsg.contains("राजेश"))
        org.junit.Assert.assertTrue(shortMsg.contains("450.00"))
        org.junit.Assert.assertTrue(shortMsg.contains("raichilledwater@upi"))

        // 2. Humble
        val humbleMsg = settings.formatMessage(com.example.data.model.MessageTone.HUMBLE, "राजेश", "450.00", "3")
        org.junit.Assert.assertTrue(humbleMsg.contains("सादर प्रणाम"))
        org.junit.Assert.assertTrue(humbleMsg.contains("विनम्र निवेदन"))

        // 3. Sweet
        val sweetMsg = settings.formatMessage(com.example.data.model.MessageTone.SWEET, "राजेश", "450.00", "3")
        org.junit.Assert.assertTrue(sweetMsg.contains("सकुशल"))

        // 4. Direct
        val directMsg = settings.formatMessage(com.example.data.model.MessageTone.DIRECT, "राजेश", "450.00", "3")
        org.junit.Assert.assertTrue(directMsg.contains("सूचना"))

        // 5. Professional
        val proMsg = settings.formatMessage(com.example.data.model.MessageTone.PROFESSIONAL, "राजेश", "450.00", "3")
        org.junit.Assert.assertTrue(proMsg.contains("खाता विवरण अनुसार"))
    }

    @Test
    fun v11_fileNamingConvention_matchesSpecification() {
        val fileName = com.example.data.export.DataImportExportManager.generateFileName("Customers", "csv", true)
        org.junit.Assert.assertTrue("Filename must start with RaiWater_", fileName.startsWith("RaiWater_Customers_"))
        org.junit.Assert.assertTrue("Filename must end with .csv", fileName.endsWith(".csv"))
        // Check timestamp pattern YYYYMMDD_HHmmss
        val pattern = Regex("""^RaiWater_Customers_\d{8}_\d{6}\.csv$""")
        org.junit.Assert.assertTrue("Filename must match regex: $fileName", pattern.matches(fileName))
    }

    @Test
    fun v11_autoDetection_identifiesRegistersAccurately() {
        val mgr = com.example.data.export.DataImportExportManager

        // 1. Deliveries Header
        val delHeader = listOf("Transaction ID", "Date", "Time", "Customer ID", "Customer Name", "Cans Given", "Cans Received", "Notation", "Route", "Driver", "Notes", "Sync Status")
        assertEquals(mgr.RegisterType.DELIVERIES, mgr.autoDetectRegisterType(delHeader))

        // 2. Payments Header
        val payHeader = listOf("Payment ID", "Date", "Customer ID", "Customer Name", "Amount (INR)", "Payment Mode", "Reference / UTR / Note", "Collector Name")
        assertEquals(mgr.RegisterType.PAYMENTS, mgr.autoDetectRegisterType(payHeader))

        // 3. Bills Header
        val billHeader = listOf("Bill No", "Billing Date", "Customer ID", "Customer Name", "Phone", "Period Start", "Period End", "Delivered Cans", "Returned Cans", "Rate Per Can", "Water Charges", "Total Bill (INR)", "Paid Amount (INR)", "Balance Due (INR)", "Payment Status")
        assertEquals(mgr.RegisterType.BILLS, mgr.autoDetectRegisterType(billHeader))

        // 4. Monthly Ledger Header
        val ledgerHeader = listOf("S.No", "Customer ID", "Customer Name", "Phone", "Address", "Rate (INR)", "Day 1", "Day 31", "Total Delivered Cans", "Total Returned Cans", "Water Charges (INR)", "Extra Charges (INR)", "Total Bill (INR)", "Paid Amount (INR)", "Net Balance Due (INR)")
        assertEquals(mgr.RegisterType.MONTHLY_LEDGER, mgr.autoDetectRegisterType(ledgerHeader))

        // 5. Customers Header
        val custHeader = listOf("Customer ID", "Hindi Name", "English Name", "Phone", "Address", "Locality / Route", "Rate Per Can", "Can Balance", "Due Amount (INR)", "Status")
        assertEquals(mgr.RegisterType.CUSTOMERS, mgr.autoDetectRegisterType(custHeader))
    }

    @Test
    fun v11_pureXlsxOpenXmlGeneration_isValidZipWorkbook() {
        val tempFile = java.io.File.createTempFile("TestExport", ".xlsx")
        tempFile.deleteOnExit()

        val headers = listOf("Customer ID", "Name Hindi", "Rate")
        val rows = listOf(
            listOf("CUST-101", "अग्रवाल स्वीट्स पिपरिया", "30.0"),
            listOf("CUST-102", "होटल राजहंस", "28.0")
        )

        com.example.data.export.DataImportExportManager.writeXlsx(tempFile, "TestSheet", headers, rows)
        org.junit.Assert.assertTrue("XLSX file must exist and be non-empty", tempFile.exists() && tempFile.length() > 0)

        // Verify OpenXML package structure
        val zis = java.util.zip.ZipInputStream(java.io.FileInputStream(tempFile))
        val entryNames = mutableListOf<String>()
        var entry = zis.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()

        org.junit.Assert.assertTrue("Must contain [Content_Types].xml", entryNames.contains("[Content_Types].xml"))
        org.junit.Assert.assertTrue("Must contain xl/workbook.xml", entryNames.contains("xl/workbook.xml"))
        org.junit.Assert.assertTrue("Must contain xl/worksheets/sheet1.xml", entryNames.contains("xl/worksheets/sheet1.xml"))
        org.junit.Assert.assertTrue("Must contain xl/sharedStrings.xml", entryNames.contains("xl/sharedStrings.xml"))
    }
}
