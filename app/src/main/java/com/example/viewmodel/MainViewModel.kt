package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.ocr.ExtractedDailyEntry
import com.example.data.sync.PushResultDetails
import com.example.data.sync.SyncResult
import com.example.data.sync.WifiSyncService
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ActiveScreen {
    ROUTE_SHEET,
    CUSTOMER_LEDGER,
    BILLS_HISTORY,
    SYNC_SETTINGS,
    PAYMENT_REMINDERS,
    PRODUCT_CATALOG,
    BUSINESS_SETTINGS,
    ABOUT_SYNC_GUIDE,
    SYMBOL_DICTIONARY,
    BUSINESS_SUMMARY,
    REGISTER_SCAN,
    DATA_CENTER
}

enum class SyncOp {
    MORNING_DOWNLOAD,
    EVENING_UPLOAD,
    TEST_CONNECTION,
    SIMULATED_SYNC
}

data class SyncFeedback(
    val title: String = "",
    val message: String = "",
    val isSuccess: Boolean = true,
    val details: PushResultDetails? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val customerDao = db.customerDao()
    private val deliveryDao = db.deliveryDao()
    private val paymentDao = db.paymentDao()
    private val billDao = db.billDao()
    private val productDao = db.productDao()
    private val symbolDao = db.symbolDao()
    private val settingsDao = db.settingsDao()
    private val syncDao = db.syncConfigDao()
    private val scanDao = db.scanDao()

    private val syncService = WifiSyncService(application)

    // Navigation & UI state
    val activeScreen = MutableStateFlow(ActiveScreen.ROUTE_SHEET)
    val appLanguage = MutableStateFlow(AppLanguage.HINDI)

    // Data streams
    val customers: StateFlow<List<Customer>> = customerDao.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawCustomers: StateFlow<List<Customer>>
        get() = customers

    val deliveries: StateFlow<List<DeliveryEntry>> = deliveryDao.getAllDeliveries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeliveries: StateFlow<List<DeliveryEntry>>
        get() = deliveries

    val payments: StateFlow<List<PaymentRecord>> = paymentDao.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<BillRecord>> = billDao.getAllBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductItem>> = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<ProductItem>> = productDao.getActiveProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val symbols: StateFlow<List<NotationSymbol>> = symbolDao.getAllSymbols()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessSettings: StateFlow<BusinessSettings> = settingsDao.getSettings()
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettings())

    val syncConfig: StateFlow<SyncConfig> = syncDao.getSyncConfig()
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncConfig())

    // Route sheet filters
    val selectedRoute = MutableStateFlow("All Routes")
    val selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

    // Sync state
    val isSyncing = MutableStateFlow(false)
    val syncStatusMessage = MutableStateFlow("")
    val activeSyncOp = MutableStateFlow<SyncOp?>(null)
    val syncFeedback = MutableStateFlow<SyncFeedback?>(null)
    val showCelebrationDialog = MutableStateFlow(false)

    init {
        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch {
            if (settingsDao.getSettingsDirect() == null) {
                settingsDao.insertOrUpdate(BusinessSettings())
            }
            if (syncDao.getSyncConfigDirect() == null) {
                syncDao.insertOrUpdate(SyncConfig())
            }

            // Seed default Pipariya customers if empty
            if (customerDao.getCustomerCount() == 0) {
                val initialCustomers = listOf(
                    Customer("CUST-001", "आनंद रेस्टोरेंट & भोजनालय", "Anand Restaurant & Bhojnalaya", "9826011223", "स्टेशन रोड", "Station Road", 30.0, 10, 8, 450.0, 450.0),
                    Customer("CUST-002", "शर्मा जी मिष्ठान भंडार", "Sharma Ji Mishthan Bhandar", "9425033445", "सदर बाजार", "Sadar Bazar", 30.0, 5, 5, 150.0, 150.0),
                    Customer("CUST-003", "पटेल किराना स्टोर्स", "Patel Kirana Stores", "9826544332", "सब्जी मंडी", "Subzi Mandi", 30.0, 4, 3, 300.0, 300.0),
                    Customer("CUST-004", "कृष्णा डेयरी & पनीर सेंटर", "Krishna Dairy & Paneer Center", "9977055667", "गांधी चौक", "Gandhi Chowk", 30.0, 6, 6, 0.0, 0.0),
                    Customer("CUST-005", "वर्मा टी स्टॉल & कैफे", "Verma Tea Stall & Cafe", "9131088776", "बैंक रोड", "Bank Road", 30.0, 3, 2, 90.0, 90.0)
                )
                customerDao.insertCustomers(initialCustomers)
            }

            // Seed notation symbols
            if (symbolDao.getCount() == 0) {
                val initialSymbols = listOf(
                    NotationSymbol("1", "1", "1 कैन दी (सामान्य)", "1 Can Delivered", "1 कैन दी", "1 Can Delivered", 1, 0, 1, 0, true, "दैनिक सामान्य सप्लाय"),
                    NotationSymbol("2", "2", "2 कैन दी", "2 Cans Delivered", "2 कैन दी", "2 Cans Delivered", 2, 0, 2, 0, true, "दोगुनी सप्लाय"),
                    NotationSymbol("2+1", "2+1", "2 दी, 1 खाली ली", "2 Delivered, 1 Returned", "2 दी, 1 खाली ली", "2 Delivered, 1 Returned", 2, 1, 2, 1, true, "2 भरी बोतल दी, 1 खाली वापसी"),
                    NotationSymbol("/", "/", "केवल खाली कैन वापसी", "Empty Can Returned Only", "केवल खाली कैन वापसी", "Empty Can Returned Only", 0, 1, 0, 1, true, "केवल खाली जार लिया"),
                    NotationSymbol("X", "X", "दुकान बंद / अवकाश", "Shop Closed / Absent", "दुकान बंद / अवकाश", "Shop Closed / Absent", 0, 0, 0, 0, true, "ग्राहक अनुपस्थित / बंद"),
                    NotationSymbol("B", "B", "थोक 5 कैन (Bulk Order)", "Bulk 5 Cans", "थोक 5 कैन", "Bulk 5 Cans", 5, 0, 5, 0, true, "बड़ी दुकान / होटल थोक"),
                    NotationSymbol("S", "S", "सैंपल / कॉम्प्लिमेंटरी", "Sample Delivery", "सैंपल", "Sample", 1, 0, 1, 0, true, "फ्री ट्रायल / सैंपल")
                )
                symbolDao.insertSymbols(initialSymbols)
            }

            // Seed products
            if (productDao.getProductCount() == 0) {
                val initialProducts = listOf(
                    ProductItem("PROD-001", "20L Chilled Water Jar", "20 लीटर चिल्ड वाटर जार", 30.0, 30.0, "Can", true, true),
                    ProductItem("PROD-002", "Jar Water Dispenser Stand", "जार वाटर डिस्पेंसर स्टैंड", 250.0, 250.0, "Piece", false, true),
                    ProductItem("PROD-003", "Manual Water Pump", "मैनुअल वाटर हैंड पंप", 120.0, 120.0, "Piece", false, true),
                    ProductItem("PROD-004", "1L Sealed Mineral Water Box", "1 लीटर मिनरल वाटर बॉक्स", 180.0, 180.0, "Box", false, true)
                )
                productDao.insertProducts(initialProducts)
            }
        }
    }

    fun toggleLanguage() {
        appLanguage.value = if (appLanguage.value == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun navigateTo(screen: ActiveScreen) {
        activeScreen.value = screen
    }

    // Delivery rapid entry
    fun recordDelivery(customerId: String, notation: String, cansGiven: Int, cansReceived: Int, rate: Double) {
        viewModelScope.launch {
            val cust = customerDao.getCustomerByIdDirect(customerId)
            val entry = DeliveryEntry(
                id = "DEL-${System.currentTimeMillis()}",
                customerId = customerId,
                customerName = cust?.nameHindi ?: "",
                date = selectedDate.value,
                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                notation = notation,
                cansGiven = cansGiven,
                cansReceived = cansReceived,
                rateApplied = rate,
                route = cust?.locality ?: "General",
                notes = "Rapid Route Entry"
            )
            deliveryDao.insertDelivery(entry)

            if (cust != null) {
                val newCanBal = (cust.currentCanBalance + cansGiven - cansReceived).coerceAtLeast(0)
                val newFinBal = cust.currentFinancialBalance + (cansGiven * rate)
                customerDao.updateCustomer(
                    cust.copy(
                        currentCanBalance = newCanBal,
                        currentFinancialBalance = newFinBal,
                        lastDeliveryDate = selectedDate.value,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun recordCustomDelivery(
        customer: Customer,
        cansGiven: Int,
        cansReceived: Int,
        notation: String,
        notes: String,
        date: String = selectedDate.value
    ) {
        viewModelScope.launch {
            val entry = DeliveryEntry(
                id = "DEL-${System.currentTimeMillis()}",
                customerId = customer.id,
                customerName = customer.nameHindi,
                date = date,
                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                cansGiven = cansGiven,
                cansReceived = cansReceived,
                notation = notation,
                rateApplied = customer.ratePerCan,
                driverName = "Driver 1",
                route = customer.locality,
                notes = notes,
                timestamp = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
            deliveryDao.insertDelivery(entry)

            val newCanBal = (customer.currentCanBalance + cansGiven - cansReceived).coerceAtLeast(0)
            val newFinBal = customer.currentFinancialBalance + (cansGiven * customer.ratePerCan)
            customerDao.updateCustomer(
                customer.copy(
                    currentCanBalance = newCanBal,
                    currentFinancialBalance = newFinBal,
                    lastDeliveryDate = date,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun correctDeliveryEntry(
        entryId: String,
        newGiven: Int,
        newReceived: Int,
        reason: String
    ) {
        viewModelScope.launch {
            val allDeliveriesList = deliveryDao.getAllDeliveries().firstOrNull() ?: emptyList()
            val oldEntry = allDeliveriesList.find { it.id == entryId } ?: return@launch
            val diffGiven = newGiven - oldEntry.cansGiven
            val diffReceived = newReceived - oldEntry.cansReceived

            deliveryDao.updateDelivery(
                oldEntry.copy(
                    cansGiven = newGiven,
                    cansReceived = newReceived,
                    isCorrected = true,
                    correctionReason = reason,
                    timestamp = System.currentTimeMillis()
                )
            )

            val cust = customerDao.getCustomerByIdDirect(oldEntry.customerId)
            if (cust != null) {
                val updatedCanBal = (cust.currentCanBalance + diffGiven - diffReceived).coerceAtLeast(0)
                val updatedFinBal = cust.currentFinancialBalance + (diffGiven * cust.ratePerCan)
                customerDao.updateCustomer(
                    cust.copy(
                        currentCanBalance = updatedCanBal,
                        currentFinancialBalance = updatedFinBal,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Payment recording
    fun recordPayment(customerId: String, amount: Double, method: String, reference: String, notes: String) {
        viewModelScope.launch {
            val cust = customerDao.getCustomerByIdDirect(customerId)
            val payment = PaymentRecord(
                id = "PAY-${System.currentTimeMillis()}",
                customerId = customerId,
                customerName = cust?.nameHindi ?: "",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                amount = amount,
                paymentMethod = method,
                reference = reference,
                utrReference = reference,
                notes = notes
            )
            paymentDao.insertPayment(payment)

            if (cust != null) {
                val newFinBal = cust.currentFinancialBalance - amount
                customerDao.updateCustomer(
                    cust.copy(
                        currentFinancialBalance = newFinBal,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Customer operations
    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            customerDao.insertCustomer(customer)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            customerDao.updateCustomer(customer)
        }
    }

    fun importCustomers(list: List<Customer>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            customerDao.insertCustomers(list)
            onComplete(list.size)
        }
    }

    // Product operations
    fun addProduct(product: ProductItem) {
        viewModelScope.launch {
            productDao.insertProduct(product)
        }
    }

    fun saveProduct(product: ProductItem) {
        viewModelScope.launch {
            productDao.insertProduct(product)
        }
    }

    fun updateProduct(product: ProductItem) {
        viewModelScope.launch {
            productDao.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductItem) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productDao.deleteProductById(id)
        }
    }

    // Symbol operations
    fun addSymbol(symbol: NotationSymbol) {
        viewModelScope.launch {
            symbolDao.insertSymbol(symbol)
        }
    }

    fun saveSymbol(symbol: NotationSymbol) {
        viewModelScope.launch {
            symbolDao.insertSymbol(symbol)
        }
    }

    fun deleteSymbol(id: String) {
        viewModelScope.launch {
            symbolDao.deleteSymbolById(id)
        }
    }

    // Bill operations
    fun saveBill(bill: BillRecord) {
        viewModelScope.launch {
            billDao.insertBill(bill)
        }
    }

    fun deleteBill(bill: BillRecord) {
        viewModelScope.launch {
            billDao.deleteBill(bill)
        }
    }

    // Settings & Config
    fun saveSyncConfig(config: SyncConfig) {
        viewModelScope.launch {
            syncDao.insertOrUpdate(config)
        }
    }

    fun saveBusinessSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            settingsDao.insertOrUpdate(settings)
        }
    }

    // Wi-Fi Sync Operations
    fun downloadMorningRoute() {
        viewModelScope.launch {
            isSyncing.value = true
            activeSyncOp.value = SyncOp.MORNING_DOWNLOAD
            syncStatusMessage.value = "Downloading customer master & route sheet from PC..."

            val cfg = syncDao.getSyncConfigDirect() ?: SyncConfig()
            val (pulledList, msg) = syncService.pullCustomersFromPc(cfg)

            isSyncing.value = false
            if (pulledList != null) {
                customerDao.insertCustomers(pulledList)
                syncFeedback.value = SyncFeedback(
                    title = "Morning Route Downloaded (सुबह का रूट लोड हुआ)",
                    message = "Successfully loaded ${pulledList.size} customers into local device database.",
                    isSuccess = true
                )
                showCelebrationDialog.value = true
            } else {
                syncFeedback.value = SyncFeedback(
                    title = "Route Download Failed",
                    message = msg,
                    isSuccess = false
                )
            }
        }
    }

    fun uploadEveningData() {
        viewModelScope.launch {
            isSyncing.value = true
            activeSyncOp.value = SyncOp.EVENING_UPLOAD
            syncStatusMessage.value = "Uploading evening deliveries & cash collections to PC..."

            val cfg = syncDao.getSyncConfigDirect() ?: SyncConfig()
            val unsyncedDeliveries = deliveryDao.getUnsyncedDeliveries()
            val unsyncedPayments = paymentDao.getUnsyncedPayments()

            val result = syncService.pushDeliveriesAndPayments(cfg, unsyncedDeliveries, unsyncedPayments)
            isSyncing.value = false

            if (result.success) {
                deliveryDao.markDeliveriesSynced(unsyncedDeliveries.map { it.id })
                paymentDao.markPaymentsSynced(unsyncedPayments.map { it.id })
                syncFeedback.value = SyncFeedback(
                    title = "Evening Sync Completed (शाम का डेटा सिंक संपन्न)",
                    message = result.message,
                    isSuccess = true,
                    details = result
                )
                showCelebrationDialog.value = true
            } else {
                syncFeedback.value = SyncFeedback(
                    title = "Sync Upload Failed",
                    message = result.message,
                    isSuccess = false,
                    details = result
                )
            }
        }
    }

    fun testPcConnection() {
        viewModelScope.launch {
            isSyncing.value = true
            activeSyncOp.value = SyncOp.TEST_CONNECTION
            syncStatusMessage.value = "Testing network socket connection to PC..."

            val cfg = syncDao.getSyncConfigDirect() ?: SyncConfig()
            val (ok, msg) = syncService.testConnection(cfg)
            isSyncing.value = false

            syncFeedback.value = SyncFeedback(
                title = if (ok) "PC Connection Verified" else "Connection Failed",
                message = msg,
                isSuccess = ok
            )
        }
    }

    fun triggerSimulatedSync() {
        viewModelScope.launch {
            isSyncing.value = true
            activeSyncOp.value = SyncOp.SIMULATED_SYNC
            syncStatusMessage.value = "Simulating desktop sync protocol..."
            kotlinx.coroutines.delay(1200)

            isSyncing.value = false
            syncFeedback.value = SyncFeedback(
                title = "डेटा सफलतापूर्वक सिंक हो गया (Data Synced Successfully!)",
                message = "Simulated dual-device handshake confirmed. 42 deliveries and ₹1,450 collections acknowledged by PC server.",
                isSuccess = true,
                details = PushResultDetails(
                    success = true,
                    message = "Simulated sync successful",
                    processedDeliveries = 42,
                    processedPayments = 8,
                    totalPaymentAmount = 1450.0,
                    serverTimestamp = System.currentTimeMillis()
                )
            )
            showCelebrationDialog.value = true
        }
    }

    fun dismissFeedback() {
        syncFeedback.value = null
        showCelebrationDialog.value = false
    }

    fun commitExtractedSheet(
        customer: Customer,
        yearMonth: String,
        entries: List<ExtractedDailyEntry>,
        routeOrNote: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val list = entries.map { entry ->
                val dayStr = entry.day.toString().padStart(2, '0')
                val dateStr = "$yearMonth-$dayStr"
                DeliveryEntry(
                    id = "DEL-${customer.id}-$dateStr",
                    customerId = customer.id,
                    customerName = customer.nameHindi,
                    date = dateStr,
                    notation = entry.notation,
                    cansGiven = entry.cansGiven,
                    cansReceived = entry.cansReceived,
                    rateApplied = customer.ratePerCan,
                    route = customer.locality,
                    notes = routeOrNote
                )
            }
            deliveryDao.insertDeliveries(list)

            val totalGiven = entries.sumOf { it.cansGiven }
            val totalReceived = entries.sumOf { it.cansReceived }
            val cust = customerDao.getCustomerByIdDirect(customer.id) ?: customer
            val newCanBal = (cust.currentCanBalance + totalGiven - totalReceived).coerceAtLeast(0)
            val newFinBal = cust.currentFinancialBalance + (totalGiven * cust.ratePerCan)
            customerDao.updateCustomer(
                cust.copy(
                    currentCanBalance = newCanBal,
                    currentFinancialBalance = newFinBal,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            onComplete()
        }
    }
}
