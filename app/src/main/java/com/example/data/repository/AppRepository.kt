package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.BillRecord
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.data.model.DefaultProducts
import com.example.data.model.DeliveryEntry
import com.example.data.model.NotationSymbol
import com.example.data.model.PaymentRecord
import com.example.data.model.ProductItem
import com.example.data.model.ScanAttachment
import com.example.data.model.SyncConfig
import com.example.data.model.ThemeMode
import com.example.data.ocr.ExtractedDailyEntry
import com.example.data.sync.SyncResult
import com.example.data.sync.WifiSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val customerDao = database.customerDao()
    private val deliveryDao = database.deliveryEntryDao()
    private val paymentDao = database.paymentRecordDao()
    private val scanDao = database.scanAttachmentDao()
    private val syncConfigDao = database.syncConfigDao()
    private val notationDao = database.notationSymbolDao()
    private val productDao = database.productDao()
    private val billDao = database.billRecordDao()
    private val settingsDao = database.businessSettingsDao()
    private val syncService = WifiSyncService(context)

    val businessSettings: Flow<BusinessSettings> = settingsDao.getSettingsFlow().map { it ?: BusinessSettings() }

    val allActiveCustomers: Flow<List<Customer>> = customerDao.getAllActiveCustomers()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val localities: Flow<List<String>> = customerDao.getDistinctLocalities()
    val allDeliveries: Flow<List<DeliveryEntry>> = deliveryDao.getAllDeliveries()
    val allPayments: Flow<List<PaymentRecord>> = paymentDao.getAllPayments()
    val allScans: Flow<List<ScanAttachment>> = scanDao.getAllScans()
    val syncConfig: Flow<SyncConfig?> = syncConfigDao.getSyncConfig()
    val allSymbols: Flow<List<NotationSymbol>> = notationDao.getAllSymbols()
    val allProducts: Flow<List<ProductItem>> = productDao.getAllProducts()
    val activeProducts: Flow<List<ProductItem>> = productDao.getActiveProducts()
    val allBills: Flow<List<BillRecord>> = billDao.getAllBills()

    val pendingDeliveriesCount: Flow<Int> = deliveryDao.getPendingDeliveriesCount()
    val pendingPaymentsCount: Flow<Int> = paymentDao.getPendingPaymentsCount()
    val pendingScansCount: Flow<Int> = scanDao.getPendingScansCount()

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntry>> =
        deliveryDao.getDeliveriesForDate(date)

    fun getCustomerById(id: String): Flow<Customer?> = customerDao.getCustomerById(id)

    fun getDeliveriesForCustomer(customerId: String): Flow<List<DeliveryEntry>> =
        deliveryDao.getDeliveriesForCustomer(customerId)

    fun getDeliveriesForCustomerMonth(customerId: String, yearMonth: String): Flow<List<DeliveryEntry>> =
        deliveryDao.getCustomerDeliveriesForMonth(customerId, yearMonth)

    suspend fun getCustomerDeliveriesInRange(customerId: String, startDate: String, endDate: String): List<DeliveryEntry> =
        withContext(Dispatchers.IO) {
            deliveryDao.getCustomerDeliveriesInRange(customerId, startDate, endDate)
        }

    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentRecord>> =
        paymentDao.getPaymentsForCustomer(customerId)

    fun getTotalCansGivenForDate(date: String): Flow<Int?> = deliveryDao.getTotalCansGivenForDate(date)
    fun getTotalCansReceivedForDate(date: String): Flow<Int?> = deliveryDao.getTotalCansReceivedForDate(date)
    fun getTotalCollectedForDate(date: String): Flow<Double?> = paymentDao.getTotalCollectedForDate(date)

    fun getSavedColorPalette(): com.example.ui.theme.AppColorPalette {
        val prefs = context.getSharedPreferences("rai_app_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getString("selected_color_palette_id", com.example.ui.theme.AppColorPalette.DEFAULT.id) ?: com.example.ui.theme.AppColorPalette.DEFAULT.id
        return com.example.ui.theme.AppColorPalette.fromId(savedId)
    }

    fun saveColorPalette(palette: com.example.ui.theme.AppColorPalette) {
        val prefs = context.getSharedPreferences("rai_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_color_palette_id", palette.id).apply()
    }

    suspend fun resetCustomerAndEntryData() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("rai_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("seed_handled_v1", true).apply()
        customerDao.deleteAllCustomers()
        deliveryDao.deleteAllDeliveries()
        paymentDao.deleteAllPayments()
        scanDao.deleteAllScans()
        billDao.deleteAllBills()
    }

    suspend fun initializeSeedDataIfEmpty() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("rai_app_prefs", Context.MODE_PRIVATE)
        val hasBeenResetOrInitialized = prefs.getBoolean("seed_handled_v1", false)

        val count = customerDao.getCustomerCount()
        if (count == 0 && !hasBeenResetOrInitialized) {
            prefs.edit().putBoolean("seed_handled_v1", true).apply()
            val initialCustomers = listOf(
                Customer(
                    id = "CUST-001",
                    nameHindi = "ग्राम शक्ती",
                    nameEnglish = "Gram Shakti",
                    phone = "8962203040",
                    address = "पेट्रोल पम्प के बाजू में ५४६०",
                    locality = "स्टेशन रोड / Station Road",
                    ratePerCan = 30.0,
                    openingCanBalance = 2,
                    currentCanBalance = 4,
                    openingFinancialBalance = 0.0,
                    currentFinancialBalance = 720.0
                ),
                Customer(
                    id = "CUST-002",
                    nameHindi = "राजेश शर्मा (होटल नटराज)",
                    nameEnglish = "Rajesh Sharma (Hotel Natraj)",
                    phone = "9826012345",
                    address = "स्टेशन रोड, पिपरिया",
                    locality = "स्टेशन रोड / Station Road",
                    ratePerCan = 30.0,
                    openingCanBalance = 4,
                    currentCanBalance = 6,
                    openingFinancialBalance = 600.0,
                    currentFinancialBalance = 1200.0
                ),
                Customer(
                    id = "CUST-003",
                    nameHindi = "विजय गुप्ता (स्वीट्स)",
                    nameEnglish = "Vijay Gupta Sweets",
                    phone = "9425098765",
                    address = "मेन मार्केट, घंटाघर के पास, पिपरिया",
                    locality = "मेन मार्केट / Main Market",
                    ratePerCan = 30.0,
                    openingCanBalance = 2,
                    currentCanBalance = 3,
                    openingFinancialBalance = 0.0,
                    currentFinancialBalance = 450.0
                ),
                Customer(
                    id = "CUST-004",
                    nameHindi = "डॉ. अनिल वर्मा (क्लीनिक)",
                    nameEnglish = "Dr. Anil Verma Clinic",
                    phone = "9893123456",
                    address = "हॉस्पिटल चौराहा, पिपरिया",
                    locality = "हॉस्पिटल चौराहा / Hospital Chowk",
                    ratePerCan = 35.0,
                    openingCanBalance = 1,
                    currentCanBalance = 2,
                    openingFinancialBalance = 350.0,
                    currentFinancialBalance = 700.0
                ),
                Customer(
                    id = "CUST-005",
                    nameHindi = "महेश पटेल (किराना भंडार)",
                    nameEnglish = "Mahesh Patel Kirana",
                    phone = "9754123890",
                    address = "शोभापुर रोड, पिपरिया",
                    locality = "शोभापुर रोड / Shobhapur Road",
                    ratePerCan = 30.0,
                    openingCanBalance = 3,
                    currentCanBalance = 4,
                    openingFinancialBalance = 150.0,
                    currentFinancialBalance = 300.0
                ),
                Customer(
                    id = "CUST-006",
                    nameHindi = "सुरेश राय (कार्यालय)",
                    nameEnglish = "Suresh Rai Office",
                    phone = "9826789123",
                    address = "सिविल लाइन्स, पिपरिया",
                    locality = "सिविल लाइन्स / Civil Lines",
                    ratePerCan = 35.0,
                    openingCanBalance = 2,
                    currentCanBalance = 2,
                    openingFinancialBalance = 0.0,
                    currentFinancialBalance = 0.0
                )
            )
            customerDao.insertCustomers(initialCustomers)

            // Seed default sync configuration
            val currentConfig = syncConfigDao.getSyncConfigDirect()
            if (currentConfig == null) {
                syncConfigDao.insertOrUpdateConfig(
                    SyncConfig(
                        id = 1,
                        pcHost = "192.168.1.105",
                        pcPort = 5000,
                        pullCustomersPath = "/api/sync/pull",
                        pushDeliveriesPath = "/api/sync/push",
                        uploadScanPath = "/api/sync/upload_scan",
                        targetPcScanFolder = "./scans/"
                    )
                )
            }

            if (notationDao.getCount() == 0) {
                val initialSymbols = listOf(
                    NotationSymbol("SYM-1", "1", "1 केन दिया (सामान्य)", "1 Can Delivered", 1, 0, true, "दैनिक सामान्य सप्लाय"),
                    NotationSymbol("SYM-2", "2", "2 केन दिया", "2 Cans Delivered", 2, 0, true, "दोगुनी सप्लाय"),
                    NotationSymbol("SYM-3", "2+1", "2 दिया, 1 खाली लिया", "2 Delivered, 1 Returned", 2, 1, true, "2 भरी बोतल दी, 1 खाली वापसी"),
                    NotationSymbol("SYM-4", "/", "केवल खाली केन वापसी", "1 Empty Returned Only", 0, 1, true, "केवल खाली जार लिया"),
                    NotationSymbol("SYM-5", "X", "दुकान बंद / अवकाश", "Shop Closed / No Delivery", 0, 0, true, "ग्राहक अनुपस्थित / बंद"),
                    NotationSymbol("SYM-6", "B", "थोक 5 केन (Bulk Order)", "Bulk 5 Cans", 5, 5, true, "बड़ी दुकान / होटल थोक"),
                    NotationSymbol("SYM-7", "S", "सैंपल / कॉम्प्लिमेंटरी", "Sample / Complementary", 1, 0, true, "फ्री ट्रायल / सैंपल")
                )
                notationDao.insertSymbols(initialSymbols)
            }
        }

        // Initialize products catalog
        if (productDao.getProductCount() == 0) {
            productDao.insertAll(DefaultProducts.catalog)
        }

        // Initialize sample bill if empty
        if (billDao.getBillCount() == 0) {
            billDao.insertBill(
                BillRecord(
                    id = "BILL-5156",
                    billNumber = "5156",
                    billingDate = "2026-10-24",
                    periodStartDate = "2026-10-01",
                    periodEndDate = "2026-10-24",
                    customerId = "CUST-001",
                    customerName = "ग्राम शक्ती",
                    customerPhone = "8962203040",
                    customerAddress = "पेट्रोल पम्प के बाजू में",
                    totalCansDelivered = 24,
                    totalCansReturned = 24,
                    ratePerCan = 30.0,
                    extraProductsSummary = "",
                    extraProductsTotal = 0.0,
                    waterChargesTotal = 720.0,
                    discount = 0.0,
                    totalPayable = 720.0,
                    isImported = false,
                    paymentStatus = "PENDING"
                )
            )
        }
    }

    // Product CRUD
    suspend fun saveProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // Bill CRUD
    suspend fun saveBill(bill: BillRecord) = withContext(Dispatchers.IO) {
        billDao.insertBill(bill)
    }

    suspend fun deleteBill(bill: BillRecord) = withContext(Dispatchers.IO) {
        billDao.deleteBill(bill)
    }

    suspend fun getNextBillNumber(): String = withContext(Dispatchers.IO) {
        val latest = billDao.getLatestBillNumber()
        val num = latest?.filter { it.isDigit() }?.toIntOrNull()
        if (num != null) {
            (num + 1).toString()
        } else {
            "5157"
        }
    }

    suspend fun saveSymbol(symbol: NotationSymbol) = withContext(Dispatchers.IO) {
        notationDao.insertSymbol(symbol)
    }

    suspend fun deleteSymbol(id: String) = withContext(Dispatchers.IO) {
        notationDao.deleteSymbolById(id)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun importCustomers(customers: List<Customer>) = withContext(Dispatchers.IO) {
        customerDao.insertCustomers(customers)
    }

    suspend fun recordDeliveryRapidTap(
        customer: Customer,
        cansGivenDelta: Int,
        cansReceivedDelta: Int,
        driverName: String = "Driver 1",
        notation: String = "Normal",
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val today = getTodayString()
        val existingToday = deliveryDao.getCustomerDeliveryForDate(customer.id, today)

        if (existingToday != null && !existingToday.isCorrected) {
            val newGiven = (existingToday.cansGiven + cansGivenDelta).coerceAtLeast(0)
            val newReceived = (existingToday.cansReceived + cansReceivedDelta).coerceAtLeast(0)
            val updatedEntry = existingToday.copy(
                cansGiven = newGiven,
                cansReceived = newReceived,
                time = getCurrentTimeString(),
                timestamp = System.currentTimeMillis(),
                notation = if (notation != "Normal") notation else existingToday.notation,
                notes = if (notes.isNotBlank()) notes else existingToday.notes,
                syncStatus = "PENDING"
            )
            deliveryDao.updateDelivery(updatedEntry)
        } else {
            val newEntry = DeliveryEntry(
                id = UUID.randomUUID().toString(),
                customerId = customer.id,
                customerName = customer.nameHindi,
                date = today,
                time = getCurrentTimeString(),
                cansGiven = cansGivenDelta.coerceAtLeast(0),
                cansReceived = cansReceivedDelta.coerceAtLeast(0),
                notation = notation,
                driverName = driverName,
                route = customer.locality,
                notes = notes,
                timestamp = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
            deliveryDao.insertDelivery(newEntry)
        }

        val currentCust = customerDao.getCustomerByIdDirect(customer.id) ?: customer
        val newCanBal = currentCust.currentCanBalance + cansGivenDelta - cansReceivedDelta
        customerDao.updateCanBalance(customer.id, newCanBal)

        val addedDue = cansGivenDelta * currentCust.ratePerCan
        val newFinBal = currentCust.currentFinancialBalance + addedDue
        customerDao.updateFinancialBalance(customer.id, newFinBal)
    }

    suspend fun recordCustomDelivery(
        customer: Customer,
        cansGiven: Int,
        cansReceived: Int,
        notation: String,
        notes: String,
        date: String = getTodayString(),
        driverName: String = "Driver 1"
    ) = withContext(Dispatchers.IO) {
        val entry = DeliveryEntry(
            id = UUID.randomUUID().toString(),
            customerId = customer.id,
            customerName = customer.nameHindi,
            date = date,
            time = getCurrentTimeString(),
            cansGiven = cansGiven,
            cansReceived = cansReceived,
            notation = notation,
            driverName = driverName,
            route = customer.locality,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
        deliveryDao.insertDelivery(entry)

        val currentCust = customerDao.getCustomerByIdDirect(customer.id) ?: customer
        val newCanBal = currentCust.currentCanBalance + cansGiven - cansReceived
        val newFinBal = currentCust.currentFinancialBalance + (cansGiven * currentCust.ratePerCan)
        customerDao.updateCanBalance(customer.id, newCanBal)
        customerDao.updateFinancialBalance(customer.id, newFinBal)
    }

    /**
     * Commits all daily entries of an extracted/scanned monthly sheet (1 to 31)
     */
    suspend fun commitExtractedSheet(
        customer: Customer,
        yearMonth: String, // e.g. "2026-10"
        entries: List<ExtractedDailyEntry>,
        routeOrNote: String
    ) = withContext(Dispatchers.IO) {
        var totalGivenDelta = 0
        var totalReceivedDelta = 0
        val deliveryList = mutableListOf<DeliveryEntry>()

        for (item in entries) {
            val dayStr = item.day.toString().padStart(2, '0')
            val fullDate = "$yearMonth-$dayStr"

            val existing = deliveryDao.getCustomerDeliveryForDate(customer.id, fullDate)
            if (existing != null) {
                totalGivenDelta += (item.cansGiven - existing.cansGiven)
                totalReceivedDelta += (item.cansReceived - existing.cansReceived)

                val updated = existing.copy(
                    cansGiven = item.cansGiven,
                    cansReceived = item.cansReceived,
                    notation = item.notation,
                    notes = "$routeOrNote • शीट प्रविष्टि",
                    timestamp = System.currentTimeMillis(),
                    syncStatus = "PENDING"
                )
                deliveryDao.updateDelivery(updated)
            } else {
                totalGivenDelta += item.cansGiven
                totalReceivedDelta += item.cansReceived

                val newEntry = DeliveryEntry(
                    id = UUID.randomUUID().toString(),
                    customerId = customer.id,
                    customerName = customer.nameHindi,
                    date = fullDate,
                    time = "10:00:00",
                    cansGiven = item.cansGiven,
                    cansReceived = item.cansReceived,
                    notation = item.notation,
                    driverName = "Auto-Sheet",
                    route = customer.locality,
                    notes = "$routeOrNote • शीट प्रविष्टि",
                    timestamp = System.currentTimeMillis(),
                    syncStatus = "PENDING"
                )
                deliveryList.add(newEntry)
            }
        }

        if (deliveryList.isNotEmpty()) {
            deliveryDao.insertDeliveries(deliveryList)
        }

        val currentCust = customerDao.getCustomerByIdDirect(customer.id) ?: customer
        val newCanBal = currentCust.currentCanBalance + totalGivenDelta - totalReceivedDelta
        val newFinBal = currentCust.currentFinancialBalance + (totalGivenDelta * currentCust.ratePerCan)
        customerDao.updateCanBalance(customer.id, newCanBal)
        customerDao.updateFinancialBalance(customer.id, newFinBal)
    }

    suspend fun correctDeliveryEntry(
        entryId: String,
        newGiven: Int,
        newReceived: Int,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val allEntries = deliveryDao.getAllDeliveries().firstOrNull() ?: emptyList()
        val oldEntry = allEntries.find { it.id == entryId } ?: return@withContext

        val givenDiff = newGiven - oldEntry.cansGiven
        val receivedDiff = newReceived - oldEntry.cansReceived

        val corrected = oldEntry.copy(
            cansGiven = newGiven,
            cansReceived = newReceived,
            isCorrected = true,
            correctionReason = reason,
            syncStatus = "PENDING",
            timestamp = System.currentTimeMillis()
        )
        deliveryDao.updateDelivery(corrected)

        val customer = customerDao.getCustomerByIdDirect(oldEntry.customerId)
        if (customer != null) {
            val updatedCanBal = customer.currentCanBalance + givenDiff - receivedDiff
            val updatedFinBal = customer.currentFinancialBalance + (givenDiff * customer.ratePerCan)
            customerDao.updateCanBalance(customer.id, updatedCanBal)
            customerDao.updateFinancialBalance(customer.id, updatedFinBal)
        }
    }

    suspend fun recordPayment(
        customerId: String,
        customerName: String,
        amount: Double,
        method: String,
        reference: String,
        notes: String,
        billId: String? = null
    ) = withContext(Dispatchers.IO) {
        val payment = PaymentRecord(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            customerName = customerName,
            date = getTodayString(),
            amount = amount,
            paymentMethod = method,
            reference = reference,
            collector = "Admin/Driver",
            notes = notes,
            billId = billId,
            timestamp = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
        paymentDao.insertPayment(payment)

        val customer = customerDao.getCustomerByIdDirect(customerId)
        if (customer != null) {
            val newFinBal = customer.currentFinancialBalance - amount
            customerDao.updateFinancialBalance(customerId, newFinBal)
        }

        if (!billId.isNullOrBlank()) {
            val bill = billDao.getBillById(billId)
            if (bill != null) {
                val newPaid = bill.paidAmount + amount
                val newBalance = (bill.totalPayable - newPaid).coerceAtLeast(0.0)
                val newStatus = if (newPaid >= bill.totalPayable) "PAID" else "PARTIAL"
                billDao.insertBill(
                    bill.copy(
                        paidAmount = newPaid,
                        balanceDue = newBalance,
                        paymentStatus = newStatus
                    )
                )
            }
        }
    }

    suspend fun saveScanPhoto(
        localFilePath: String,
        fileName: String,
        route: String,
        customerNote: String,
        fileSize: Long
    ) = withContext(Dispatchers.IO) {
        val scan = ScanAttachment(
            id = UUID.randomUUID().toString(),
            localFilePath = localFilePath,
            fileName = fileName,
            captureDate = getTodayString(),
            route = route,
            customerNote = customerNote,
            fileSize = fileSize,
            timestamp = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
        scanDao.insertScan(scan)
    }

    suspend fun saveSyncConfig(config: SyncConfig) = withContext(Dispatchers.IO) {
        syncConfigDao.insertOrUpdateConfig(config)
    }

    suspend fun testPcConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = syncConfigDao.getSyncConfigDirect() ?: SyncConfig()
        syncService.testConnection(config)
    }

    suspend fun executeWifiSync(): SyncResult = withContext(Dispatchers.IO) {
        val config = syncConfigDao.getSyncConfigDirect() ?: SyncConfig()

        var syncedDeliveriesCount = 0
        var syncedPaymentsCount = 0
        var syncedScansCount = 0
        var pulledCustomersCount = 0
        var totalAmountCollected = 0.0
        var serverTs = System.currentTimeMillis()

        try {
            val pendingDeliveries = deliveryDao.getPendingSyncDeliveries()
            val pendingPayments = paymentDao.getPendingSyncPayments()

            if (pendingDeliveries.isNotEmpty() || pendingPayments.isNotEmpty()) {
                val pushResult = syncService.pushDeliveriesAndPayments(
                    config,
                    pendingDeliveries,
                    pendingPayments
                )
                if (pushResult.success) {
                    deliveryDao.markDeliveriesSynced(pendingDeliveries.map { it.id })
                    paymentDao.markPaymentsSynced(pendingPayments.map { it.id })
                    syncedDeliveriesCount = pushResult.processedDeliveries
                    syncedPaymentsCount = pushResult.processedPayments
                    totalAmountCollected = pushResult.totalPaymentAmount
                    if (pushResult.serverTimestamp > 0) {
                        serverTs = pushResult.serverTimestamp
                    }
                } else {
                    return@withContext SyncResult.Failure("Push to PC failed: ${pushResult.message}")
                }
            }

            val pendingScans = scanDao.getPendingScans()
            for (scan in pendingScans) {
                val (scanOk, scanMsg) = syncService.uploadScanPhoto(config, scan)
                if (scanOk) {
                    scanDao.markScanSynced(scan.id)
                    syncedScansCount++
                } else {
                    scanDao.markScanFailed(scan.id, scanMsg)
                }
            }

            val (pulledCustomers, _) = syncService.pullCustomersFromPc(config)
            if (pulledCustomers != null && pulledCustomers.isNotEmpty()) {
                customerDao.insertCustomers(pulledCustomers)
                pulledCustomersCount = pulledCustomers.size
            }

            val successMsg = "Synced $syncedDeliveriesCount deliveries, $syncedPaymentsCount payments (₹${totalAmountCollected.toInt()}), $syncedScansCount scans. Pulled $pulledCustomersCount customers."
            syncConfigDao.insertOrUpdateConfig(
                config.copy(
                    lastSyncTime = serverTs,
                    lastSyncMessage = successMsg,
                    lastSyncSuccess = true
                )
            )

            SyncResult.Success(
                message = successMsg,
                syncedDeliveries = syncedDeliveriesCount,
                syncedPayments = syncedPaymentsCount,
                syncedScans = syncedScansCount,
                pulledCustomers = pulledCustomersCount,
                totalPaymentAmount = totalAmountCollected,
                serverTimestamp = serverTs
            )
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Sync error"
            syncConfigDao.insertOrUpdateConfig(
                config.copy(
                    lastSyncTime = System.currentTimeMillis(),
                    lastSyncMessage = errorMsg,
                    lastSyncSuccess = false
                )
            )
            SyncResult.Failure(errorMsg)
        }
    }

    suspend fun simulateSync(): SyncResult = withContext(Dispatchers.IO) {
        val pendingDeliveries = deliveryDao.getPendingSyncDeliveries()
        val pendingPayments = paymentDao.getPendingSyncPayments()
        val pendingScans = scanDao.getPendingScans()
        val simulatedRevenue = pendingPayments.sumOf { it.amount }

        if (pendingDeliveries.isNotEmpty()) {
            deliveryDao.markDeliveriesSynced(pendingDeliveries.map { it.id })
        }
        if (pendingPayments.isNotEmpty()) {
            paymentDao.markPaymentsSynced(pendingPayments.map { it.id })
        }
        for (scan in pendingScans) {
            scanDao.markScanSynced(scan.id)
        }

        val config = syncConfigDao.getSyncConfigDirect() ?: SyncConfig()
        val now = System.currentTimeMillis()
        val msg = "डेटा सफलतापूर्वक सिंक हो गया (Data Synced Successfully!) • ${pendingDeliveries.size} डिलीवरी, ${pendingPayments.size} भुगतान (₹${simulatedRevenue.toInt()})"
        syncConfigDao.insertOrUpdateConfig(
            config.copy(
                lastSyncTime = now,
                lastSyncMessage = msg,
                lastSyncSuccess = true
            )
        )

        SyncResult.Success(
            message = msg,
            syncedDeliveries = pendingDeliveries.size,
            syncedPayments = pendingPayments.size,
            syncedScans = pendingScans.size,
            pulledCustomers = 0,
            totalPaymentAmount = simulatedRevenue,
            serverTimestamp = now
        )
    }

    suspend fun pullCustomersOnly(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = syncConfigDao.getSyncConfigDirect() ?: SyncConfig()
        val (pulledList, msg) = syncService.pullCustomersFromPc(config)
        if (pulledList != null) {
            if (pulledList.isNotEmpty()) {
                customerDao.insertCustomers(pulledList)
            }
            val resMsg = "सफलतापूर्वक ${pulledList.size} ग्राहक व आज की दरें PC से डाउनलोड हुईं!"
            syncConfigDao.insertOrUpdateConfig(
                config.copy(
                    lastSyncTime = System.currentTimeMillis(),
                    lastSyncMessage = resMsg,
                    lastSyncSuccess = true
                )
            )
            Pair(true, resMsg)
        } else {
            Pair(false, msg)
        }
    }

    suspend fun pushDeliveriesOnly(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = syncConfigDao.getSyncConfigDirect() ?: SyncConfig()
        val pendingDeliveries = deliveryDao.getPendingSyncDeliveries()
        val pendingPayments = paymentDao.getPendingSyncPayments()
        val pendingScans = scanDao.getPendingScans()

        if (pendingDeliveries.isEmpty() && pendingPayments.isEmpty() && pendingScans.isEmpty()) {
            return@withContext Pair(true, "कोई नया डेटा भेजने के लिए लंबित नहीं है (No pending deliveries to push).")
        }

        var totalAmountCollected = 0.0
        var serverTs = System.currentTimeMillis()

        if (pendingDeliveries.isNotEmpty() || pendingPayments.isNotEmpty()) {
            val pushResult = syncService.pushDeliveriesAndPayments(config, pendingDeliveries, pendingPayments)
            if (pushResult.success) {
                deliveryDao.markDeliveriesSynced(pendingDeliveries.map { it.id })
                paymentDao.markPaymentsSynced(pendingPayments.map { it.id })
                totalAmountCollected = pushResult.totalPaymentAmount
                if (pushResult.serverTimestamp > 0) {
                    serverTs = pushResult.serverTimestamp
                }
            } else {
                return@withContext Pair(false, pushResult.message)
            }
        }

        for (scan in pendingScans) {
            val (scanOk, scanMsg) = syncService.uploadScanPhoto(config, scan)
            if (scanOk) {
                scanDao.markScanSynced(scan.id)
            } else {
                scanDao.markScanFailed(scan.id, scanMsg)
            }
        }

        val successMsg = "सफलतापूर्वक ${pendingDeliveries.size} डिलीवरी व ${pendingPayments.size} भुगतान (₹${totalAmountCollected.toInt()}) PC पर अपलोड हुए!"
        syncConfigDao.insertOrUpdateConfig(
            config.copy(
                lastSyncTime = serverTs,
                lastSyncMessage = successMsg,
                lastSyncSuccess = true
            )
        )
        Pair(true, successMsg)
    }

    suspend fun addNewCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun getBusinessSettingsOnce(): BusinessSettings = withContext(Dispatchers.IO) {
        val existing = settingsDao.getSettingsOnce()
        if (existing != null) {
            existing
        } else {
            val defaults = BusinessSettings()
            settingsDao.insertOrUpdate(defaults)
            defaults
        }
    }

    suspend fun saveBusinessSettings(settings: BusinessSettings) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) = withContext(Dispatchers.IO) {
        val current = settingsDao.getSettingsOnce() ?: BusinessSettings()
        settingsDao.insertOrUpdate(current.copy(themeMode = themeMode.name))
    }

    suspend fun restoreDatabaseFromBackup(
        customers: List<Customer>,
        deliveries: List<DeliveryEntry>,
        bills: List<com.example.data.model.BillRecord>,
        payments: List<com.example.data.model.PaymentRecord>,
        settings: BusinessSettings?
    ) = withContext(Dispatchers.IO) {
        if (customers.isNotEmpty()) {
            customerDao.insertCustomers(customers)
        }
        if (deliveries.isNotEmpty()) {
            deliveryDao.insertDeliveries(deliveries)
        }
        if (bills.isNotEmpty()) {
            bills.forEach { billDao.insertBill(it) }
        }
        if (payments.isNotEmpty()) {
            payments.forEach { paymentDao.insertPayment(it) }
        }
        if (settings != null) {
            settingsDao.insertOrUpdate(settings)
        }
    }
}

