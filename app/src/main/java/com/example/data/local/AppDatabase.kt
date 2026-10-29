package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY locality ASC, nameHindi ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY locality ASC, nameHindi ASC")
    fun getAllActiveCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: String): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: String): Customer?

    @Query("SELECT DISTINCT locality FROM customers WHERE locality != '' ORDER BY locality ASC")
    fun getDistinctLocalities(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET currentCanBalance = :newBalance WHERE id = :customerId")
    suspend fun updateCanBalance(customerId: String, newBalance: Int)

    @Query("UPDATE customers SET currentFinancialBalance = :newBalance WHERE id = :customerId")
    suspend fun updateFinancialBalance(customerId: String, newBalance: Double)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: String)

    @Query("DELETE FROM customers")
    suspend fun clearAll()

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}

@Dao
interface DeliveryEntryDao {
    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun getAllDeliveries(): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM deliveries WHERE date = :date ORDER BY createdAt DESC")
    fun getDeliveriesByDate(date: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM deliveries WHERE date = :date ORDER BY createdAt DESC")
    fun getDeliveriesForDate(date: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId ORDER BY date DESC, createdAt DESC")
    fun getDeliveriesForCustomer(customerId: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND date = :date LIMIT 1")
    suspend fun getCustomerDeliveryForDate(customerId: String, date: String): DeliveryEntry?

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getCustomerDeliveriesInRange(customerId: String, startDate: String, endDate: String): List<DeliveryEntry>

    @Query("SELECT * FROM deliveries WHERE customerId = :customerId AND date LIKE :yearMonthPrefix || '%' ORDER BY date ASC")
    fun getCustomerDeliveriesForMonth(customerId: String, yearMonthPrefix: String): Flow<List<DeliveryEntry>>

    @Query("SELECT * FROM deliveries WHERE isSynced = 0 OR syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getUnsyncedDeliveries(): List<DeliveryEntry>

    @Query("SELECT * FROM deliveries WHERE isSynced = 0 OR syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingSyncDeliveries(): List<DeliveryEntry>

    @Query("SELECT COUNT(*) FROM deliveries WHERE isSynced = 0 OR syncStatus = 'PENDING'")
    fun getPendingDeliveriesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(entry: DeliveryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(entries: List<DeliveryEntry>)

    @Update
    suspend fun updateDelivery(entry: DeliveryEntry)

    @Query("UPDATE deliveries SET isSynced = 1, syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markDeliveriesSynced(ids: List<String>)

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteDelivery(id: String)

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteDeliveryById(id: String)

    @Query("SELECT SUM(cansGiven) FROM deliveries WHERE date = :date")
    fun getTotalCansGivenForDate(date: String): Flow<Int?>

    @Query("SELECT SUM(cansReceived) FROM deliveries WHERE date = :date")
    fun getTotalCansReceivedForDate(date: String): Flow<Int?>

    @Query("DELETE FROM deliveries")
    suspend fun deleteAllDeliveries()
}

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payments ORDER BY date DESC, createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE date = :date")
    fun getPaymentsForDate(date: String): Flow<List<PaymentRecord>>

    @Query("SELECT SUM(amount) FROM payments WHERE date = :date")
    fun getTotalCollectedForDate(date: String): Flow<Double?>

    @Query("SELECT * FROM payments WHERE isSynced = 0 OR syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getUnsyncedPayments(): List<PaymentRecord>

    @Query("SELECT * FROM payments WHERE isSynced = 0 OR syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingSyncPayments(): List<PaymentRecord>

    @Query("SELECT COUNT(*) FROM payments WHERE isSynced = 0 OR syncStatus = 'PENDING'")
    fun getPendingPaymentsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentRecord>)

    @Query("UPDATE payments SET isSynced = 1, syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markPaymentsSynced(ids: List<String>)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}

@Dao
interface BillRecordDao {
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<BillRecord>>

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getBillsForCustomer(customerId: String): Flow<List<BillRecord>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): BillRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillRecord>)

    @Update
    suspend fun updateBill(bill: BillRecord)

    @Delete
    suspend fun deleteBill(bill: BillRecord)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: String)

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getBillCount(): Int

    @Query("SELECT billNumber FROM bills ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBillNumber(): String?

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY nameHindi ASC")
    fun getAllProducts(): Flow<List<ProductItem>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY nameHindi ASC")
    fun getActiveProducts(): Flow<List<ProductItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductItem>)

    @Update
    suspend fun updateProduct(product: ProductItem)

    @Delete
    suspend fun deleteProduct(product: ProductItem)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

@Dao
interface NotationSymbolDao {
    @Query("SELECT * FROM notation_symbols ORDER BY symbol ASC")
    fun getAllSymbols(): Flow<List<NotationSymbol>>

    @Query("SELECT * FROM notation_symbols WHERE symbol = :symbol LIMIT 1")
    suspend fun getSymbolBySymbol(symbol: String): NotationSymbol?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbol(symbol: NotationSymbol)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbols(symbols: List<NotationSymbol>)

    @Update
    suspend fun updateSymbol(symbol: NotationSymbol)

    @Query("DELETE FROM notation_symbols WHERE symbol = :symbol")
    suspend fun deleteSymbol(symbol: String)

    @Query("DELETE FROM notation_symbols WHERE symbol = :id OR id = :id")
    suspend fun deleteSymbolById(id: String)

    @Query("SELECT COUNT(*) FROM notation_symbols")
    suspend fun getCount(): Int
}

@Dao
interface BusinessSettingsDao {
    @Query("SELECT * FROM business_settings WHERE id = 'primary_settings' LIMIT 1")
    fun getSettings(): Flow<BusinessSettings?>

    @Query("SELECT * FROM business_settings WHERE id = 'primary_settings' LIMIT 1")
    fun getSettingsFlow(): Flow<BusinessSettings?>

    @Query("SELECT * FROM business_settings WHERE id = 'primary_settings' LIMIT 1")
    suspend fun getSettingsDirect(): BusinessSettings?

    @Query("SELECT * FROM business_settings WHERE id = 'primary_settings' LIMIT 1")
    suspend fun getSettingsOnce(): BusinessSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: BusinessSettings)

    @Update
    suspend fun update(settings: BusinessSettings)
}

@Dao
interface SyncConfigDao {
    @Query("SELECT * FROM sync_config WHERE id = 1 LIMIT 1")
    fun getSyncConfig(): Flow<SyncConfig?>

    @Query("SELECT * FROM sync_config WHERE id = 1 LIMIT 1")
    suspend fun getSyncConfigDirect(): SyncConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: SyncConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: SyncConfig)
}

@Dao
interface ScanAttachmentDao {
    @Query("SELECT * FROM scan_attachments ORDER BY createdAt DESC")
    fun getAllScans(): Flow<List<ScanAttachment>>

    @Query("SELECT * FROM scan_attachments WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    suspend fun getPendingScans(): List<ScanAttachment>

    @Query("SELECT COUNT(*) FROM scan_attachments WHERE syncStatus = 'PENDING'")
    fun getPendingScansCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanAttachment)

    @Update
    suspend fun updateScan(scan: ScanAttachment)

    @Query("UPDATE scan_attachments SET syncStatus = 'SYNCED', syncError = NULL WHERE id = :id")
    suspend fun markScanSynced(id: String)

    @Query("UPDATE scan_attachments SET syncStatus = 'FAILED', syncError = :error WHERE id = :id")
    suspend fun markScanFailed(id: String, error: String)

    @Query("DELETE FROM scan_attachments WHERE id = :id")
    suspend fun deleteScanById(id: String)

    @Query("DELETE FROM scan_attachments")
    suspend fun deleteAllScans()
}

@Database(
    entities = [
        Customer::class,
        DeliveryEntry::class,
        PaymentRecord::class,
        BillRecord::class,
        ProductItem::class,
        NotationSymbol::class,
        BusinessSettings::class,
        SyncConfig::class,
        ScanAttachment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryEntryDao
    fun deliveryEntryDao(): DeliveryEntryDao = deliveryDao()

    abstract fun paymentDao(): PaymentRecordDao
    fun paymentRecordDao(): PaymentRecordDao = paymentDao()

    abstract fun billDao(): BillRecordDao
    fun billRecordDao(): BillRecordDao = billDao()

    abstract fun productDao(): ProductDao

    abstract fun symbolDao(): NotationSymbolDao
    fun notationSymbolDao(): NotationSymbolDao = symbolDao()

    abstract fun settingsDao(): BusinessSettingsDao
    fun businessSettingsDao(): BusinessSettingsDao = settingsDao()

    abstract fun syncConfigDao(): SyncConfigDao

    abstract fun scanDao(): ScanAttachmentDao
    fun scanAttachmentDao(): ScanAttachmentDao = scanDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rai_chilled_water.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
