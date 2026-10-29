package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.DataImportExportManager
import com.example.data.export.DataImportExportManager.FileFormat
import com.example.data.export.DataImportExportManager.RegisterType
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.ui.theme.AppLanguage
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI

    val customers by viewModel.customers.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val settings by viewModel.businessSettings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        if (isHindi) "📥 आयात (Import)" else "📥 Import",
        if (isHindi) "📤 निर्यात (Export)" else "📤 Export",
        if (isHindi) "💾 बैकअप (Backup)" else "💾 Backup",
        if (isHindi) "🔄 पुनर्स्थापना (Restore)" else "🔄 Restore"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "डेटा प्रबंधन सूट v1.1" else "Data Management Suite v1.1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isHindi) "आयात • निर्यात • बैकअप • पुनर्स्थापना" else "Import • Export • Backup • Restore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ImportTabContent(isHindi, db)
                1 -> ExportTabContent(isHindi, customers, deliveries, payments, bills, settings)
                2 -> BackupTabContent(isHindi, db, customers.size, deliveries.size, payments.size, bills.size)
                3 -> RestoreTabContent(isHindi, db)
            }
        }
    }
}

// ==========================================
// 1. IMPORT TAB
// ==========================================

@Composable
fun ImportTabContent(
    isHindi: Boolean,
    db: AppDatabase
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var detectedRegister by remember { mutableStateOf<RegisterType?>(null) }
    var customerPreview by remember { mutableStateOf<DataImportExportManager.CustomerImportPreview?>(null) }
    var genericPreview by remember { mutableStateOf<DataImportExportManager.GenericImportPreview?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var safeMode by remember { mutableStateOf(true) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "SelectedFile"
            isLoading = true
            importStatusMessage = null

            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val isXlsx = uri.toString().endsWith(".xlsx", ignoreCase = true)
                    val rows = if (isXlsx) {
                        DataImportExportManager.parseXlsxFromStream(stream!!)
                    } else {
                        DataImportExportManager.parseCsvFromStream(stream!!)
                    }
                    stream?.close()

                    if (rows.isNotEmpty()) {
                        val detected = DataImportExportManager.autoDetectRegisterType(rows[0])
                        detectedRegister = detected

                        when (detected) {
                            RegisterType.CUSTOMERS -> {
                                val prev = DataImportExportManager.previewCustomerImport(context, uri, db)
                                customerPreview = prev
                                genericPreview = null
                            }
                            RegisterType.DELIVERIES -> {
                                val prev = DataImportExportManager.previewAndImportDeliveries(context, uri, db, commit = false)
                                genericPreview = prev
                                customerPreview = null
                            }
                            RegisterType.PAYMENTS -> {
                                val prev = DataImportExportManager.previewAndImportPayments(context, uri, db, commit = false)
                                genericPreview = prev
                                customerPreview = null
                            }
                            else -> {
                                genericPreview = DataImportExportManager.GenericImportPreview(
                                    registerType = detected,
                                    totalRows = rows.size - 1,
                                    validRows = rows.size - 1,
                                    invalidRows = 0,
                                    previewSummary = "फ़ाइल पंक्तियाँ: ${rows.size - 1}",
                                    errors = emptyList(),
                                    rawRows = rows.take(10)
                                )
                                customerPreview = null
                            }
                        }
                    }
                } catch (e: Exception) {
                    importStatusMessage = "त्रुटि: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step 1 Card: File Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "१. फ़ाइल चुनें (CSV / XLSX)" else "1. Select File (CSV / XLSX)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (isHindi) "ग्राहक, डिलीवरी या भुगतान की CSV अथवा Excel फ़ाइल चुनें। सिस्टम स्वचालित रूप से पहचान करेगा।" else "Select customer, delivery or payment CSV/Excel. System auto-detects register.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "फ़ाइल चुनें (Browse File)" else "Choose File")
                }

                if (selectedFileName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedFileName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Auto Detection & Preview Card
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (detectedRegister != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "पहचाना गया प्रारूप:" else "Detected Register:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                if (isHindi) detectedRegister!!.displayNameHindi else detectedRegister!!.displayNameEnglish,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    // Customer Specific Preview Stats
                    if (customerPreview != null) {
                        val prev = customerPreview!!
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill("कुल", prev.totalRows.toString(), Color.Gray)
                            StatPill("वैध", prev.validRows.toString(), Color(0xFF00796B))
                            StatPill("नए", prev.newCustomersCount.toString(), Color(0xFF1976D2))
                            StatPill("मौजूदा", prev.existingCustomersCount.toString(), Color(0xFFF57C00))
                            if (prev.conflictsCount > 0) {
                                StatPill("टकराव", prev.conflictsCount.toString(), Color.Red)
                            }
                        }

                        // Safe Mode Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (safeMode) "सुरक्षित मोड (Safe Mode - केवल नए जोड़ें)" else "अपडेट मोड (Update Existing Mode)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (safeMode) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F)
                                )
                                Text(
                                    text = if (safeMode) "मौजूदा ग्राहकों के डेटा व शेष राशि में कोई परिवर्तन नहीं होगा।" else "सावधानी: मौजूदा ग्राहकों के विवरण अपडेट होंगे (लेनदेन शेष सुरक्षित रहेगा)।",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = safeMode, onCheckedChange = { safeMode = it })
                        }

                        // Errors list if any
                        if (prev.errors.isNotEmpty()) {
                            Text("त्रुटियाँ / अमान्य पंक्तियाँ (${prev.errors.size}):", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            prev.errors.take(3).forEach { err ->
                                Text("पंक्ति ${err.rowNumber}: ${err.reason}", fontSize = 10.sp, color = Color.Red)
                            }
                        }

                        // Commit Customer Import Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val res = DataImportExportManager.executeCustomerImport(prev, safeMode, db)
                                    importStatusMessage = res.message
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "डेटाबेस में सुरक्षित आयात करें" else "Commit Import to Database")
                        }
                    } else if (genericPreview != null) {
                        val gen = genericPreview!!
                        Text(gen.previewSummary, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                        if (gen.errors.isNotEmpty()) {
                            Text("त्रुटियाँ (${gen.errors.size}):", fontSize = 11.sp, color = Color.Red)
                            gen.errors.take(3).forEach { err ->
                                Text("पंक्ति ${err.rowNumber}: ${err.reason}", fontSize = 10.sp, color = Color.Red)
                            }
                        }

                        if (gen.registerType == RegisterType.DELIVERIES || gen.registerType == RegisterType.PAYMENTS) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        if (gen.registerType == RegisterType.DELIVERIES) {
                                            val res = DataImportExportManager.previewAndImportDeliveries(context, selectedUri!!, db, commit = true)
                                            importStatusMessage = "सफलतापूर्वक ${res.validRows} डिलीवरी ट्रांजेक्शन आयात किए गए।"
                                        } else {
                                            val res = DataImportExportManager.previewAndImportPayments(context, selectedUri!!, db, commit = true)
                                            importStatusMessage = "सफलतापूर्वक ${res.validRows} भुगतान संग्रह आयात किए गए।"
                                        }
                                        isLoading = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isHindi) "ट्रांजेक्शन आयात करें (डुप्लीकेट सुरक्षित)" else "Import Transactions (No Duplicates)")
                            }
                        }
                    }

                    if (importStatusMessage != null) {
                        Text(
                            text = importStatusMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section: Download Sample Templates
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "आयात टेम्प्लेट डाउनलोड करें (Sample Templates)" else "Download Sample Templates",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (isHindi) "सही कॉलम व प्रारूप के साथ नमूना CSV / Excel फ़ाइल डाउनलोड या शेयर करें:" else "Download/share formatted CSV/Excel templates with sample rows:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val templates = listOf(
                    Pair("ग्राहक मास्टर (Customers)", RegisterType.CUSTOMERS),
                    Pair("दैनिक डिलीवरी (Deliveries)", RegisterType.DELIVERIES),
                    Pair("भुगतान संग्रह (Payments)", RegisterType.PAYMENTS),
                    Pair("बिल व इनवॉइस (Bills)", RegisterType.BILLS),
                    Pair("मासिक लेजर (Monthly Ledger)", RegisterType.MONTHLY_LEDGER)
                )

                templates.forEach { (label, type) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val f = DataImportExportManager.generateSampleTemplate(context, type, FileFormat.CSV)
                                    DataImportExportManager.shareFile(context, f, "text/csv", label)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("CSV", fontSize = 10.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    val f = DataImportExportManager.generateSampleTemplate(context, type, FileFormat.XLSX)
                                    DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", label)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Excel", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
        Text(label, fontSize = 10.sp, color = color)
    }
}

// ==========================================
// 2. EXPORT TAB
// ==========================================

@Composable
fun ExportTabContent(
    isHindi: Boolean,
    customers: List<Customer>,
    deliveries: List<com.example.data.model.DeliveryEntry>,
    payments: List<com.example.data.model.PaymentRecord>,
    bills: List<com.example.data.model.BillRecord>,
    settings: com.example.data.model.BusinessSettings
) {
    val context = LocalContext.current
    var selectedCustomerForStatement by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var statementExpanded by remember { mutableStateOf(false) }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Register 1: Customers
        ExportRegisterCard(
            title = if (isHindi) "१. ग्राहक मास्टर रजिस्टर" else "1. Customer Master Register",
            subtitle = if (isHindi) "सभी सक्रिय व निष्क्रिय ग्राहकों की सूची, दर व वर्तमान शेष (${customers.size} ग्राहक)" else "All active/inactive customer accounts & balances (${customers.size})",
            icon = Icons.Default.People,
            onExportCsv = {
                val f = DataImportExportManager.exportCustomers(context, customers, FileFormat.CSV)
                DataImportExportManager.shareFile(context, f, "text/csv", "Customer Master")
            },
            onExportXlsx = {
                val f = DataImportExportManager.exportCustomers(context, customers, FileFormat.XLSX)
                DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Customer Master")
            }
        )

        // Register 2: Deliveries
        ExportRegisterCard(
            title = if (isHindi) "२. दैनिक डिलीवरी व लेनदेन रजिस्टर" else "2. Daily Deliveries & Transactions",
            subtitle = if (isHindi) "केन डिलीवरी, खाली केन वापसी व रूट विवरण (${deliveries.size} रिकॉर्ड)" else "Daily deliveries, returns & route details (${deliveries.size})",
            icon = Icons.Default.LocalShipping,
            onExportCsv = {
                val f = DataImportExportManager.exportDeliveries(context, deliveries, FileFormat.CSV)
                DataImportExportManager.shareFile(context, f, "text/csv", "Deliveries Register")
            },
            onExportXlsx = {
                val f = DataImportExportManager.exportDeliveries(context, deliveries, FileFormat.XLSX)
                DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Deliveries Register")
            }
        )

        // Register 3: Monthly 31-Day Master Ledger
        ExportRegisterCard(
            title = if (isHindi) "३. मासिक 31-दिवसीय विस्तृत लेजर" else "3. Monthly 31-Day Master Ledger",
            subtitle = if (isHindi) "CA/लेखाकार हेतु माह $currentMonth/$currentYear के प्रतिदिन 1-31 केन कॉलम व कुल देय" else "31-day matrix ledger for accounting ($currentMonth/$currentYear)",
            icon = Icons.Default.CalendarMonth,
            onExportCsv = {
                val f = DataImportExportManager.exportMonthlyMasterLedger(context, currentYear, currentMonth, customers, deliveries, payments, FileFormat.CSV)
                DataImportExportManager.shareFile(context, f, "text/csv", "Monthly Ledger")
            },
            onExportXlsx = {
                val f = DataImportExportManager.exportMonthlyMasterLedger(context, currentYear, currentMonth, customers, deliveries, payments, FileFormat.XLSX)
                DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Monthly Ledger")
            }
        )

        // Register 4: Bills & Invoices
        ExportRegisterCard(
            title = if (isHindi) "४. बिल व इनवॉइस रजिस्टर" else "4. Bills & Invoices Register",
            subtitle = if (isHindi) "सभी जारी बिल, कुल देय, भुगतान स्थिति (${bills.size} बिल)" else "All generated invoices & payment statuses (${bills.size})",
            icon = Icons.Default.ReceiptLong,
            onExportCsv = {
                val f = DataImportExportManager.exportBills(context, bills, FileFormat.CSV)
                DataImportExportManager.shareFile(context, f, "text/csv", "Bills Register")
            },
            onExportXlsx = {
                val f = DataImportExportManager.exportBills(context, bills, FileFormat.XLSX)
                DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Bills Register")
            }
        )

        // Register 5: Payment Collections
        ExportRegisterCard(
            title = if (isHindi) "५. भुगतान संग्रह रजिस्टर" else "5. Payment Collections Register",
            subtitle = if (isHindi) "नकद, UPI व बैंक माध्यम से प्राप्त राशि विवरण (${payments.size} रिकॉर्ड)" else "Cash, UPI & Bank collections history (${payments.size})",
            icon = Icons.Default.Payments,
            onExportCsv = {
                val f = DataImportExportManager.exportPayments(context, payments, FileFormat.CSV)
                DataImportExportManager.shareFile(context, f, "text/csv", "Payments Register")
            },
            onExportXlsx = {
                val f = DataImportExportManager.exportPayments(context, payments, FileFormat.XLSX)
                DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Payments Register")
            }
        )

        // Register 6: Single Customer Dual-Ledger Statement (PDF + CSV + XLSX)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "६. एकल ग्राहक दोहरा खाता विवरण" else "6. Single Customer Dual-Ledger Statement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (isHindi) "किसी एक ग्राहक का केन व वित्तीय दोनों लेजर का मुद्रण योग्य PDF पर्ची अथवा Excel विवरण।" else "Customer container & rupee ledger statement in vector PDF or Excel.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Customer Selector
                if (customers.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { statementExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedCustomerForStatement?.let { "${it.nameHindi} (${it.id})" } ?: "ग्राहक चुनें",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = statementExpanded,
                            onDismissRequest = { statementExpanded = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.nameHindi} (${c.nameEnglish.ifBlank { c.id }})", fontSize = 12.sp) },
                                    onClick = {
                                        selectedCustomerForStatement = c
                                        statementExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCustomerForStatement != null) {
                        val cust = selectedCustomerForStatement!!
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val custDels = deliveries.filter { it.customerId == cust.id }
                                    val custPays = payments.filter { it.customerId == cust.id }
                                    val f = DataImportExportManager.exportCustomerStatementPdf(context, cust, custDels, custPays, settings)
                                    DataImportExportManager.shareFile(context, f, "application/pdf", "Customer Statement PDF")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF पर्ची", fontSize = 11.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val f = DataImportExportManager.exportCustomers(context, listOf(cust), FileFormat.XLSX)
                                    DataImportExportManager.shareFile(context, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Statement XLSX")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Excel", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Text(if (isHindi) "डेटाबेस में कोई ग्राहक नहीं है।" else "No customers found in database.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ExportRegisterCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(
                    onClick = onExportCsv,
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV (BOM)", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExportXlsx,
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel (XLSX)", fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// 3. FULL BACKUP TAB
// ==========================================

@Composable
fun BackupTabContent(
    isHindi: Boolean,
    db: AppDatabase,
    customerCount: Int,
    deliveryCount: Int,
    paymentCount: Int,
    billCount: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBackupFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "संपूर्ण डेटाबेस बैकअप v1.1" else "Full Database Backup v1.1",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (isHindi) "इस प्रक्रिया से आपके संपूर्ण व्यवसाय का डेटा (ग्राहक, डिलीवरी, भुगतान, बिल, सेटिंग्स व सिंक कॉन्फ़िगरेशन) एक सुरक्षित SHA-256 चेकसम-सत्यापित JSON फ़ाइल में संग्रहीत हो जाएगा।" else "Generates a complete versioned JSON backup including all customers, deliveries, payments, bills and settings with SHA-256 integrity hash.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatPill("ग्राहक", customerCount.toString(), MaterialTheme.colorScheme.primary)
                    StatPill("डिलीवरी", deliveryCount.toString(), Color(0xFF00796B))
                    StatPill("भुगतान", paymentCount.toString(), Color(0xFF1976D2))
                    StatPill("बिल", billCount.toString(), Color(0xFFF57C00))
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isGenerating = true
                            val f = DataImportExportManager.createFullBackup(context, db)
                            generatedBackupFile = f
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "बैकअप तैयार हो रहा है..." else "Generating...")
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "नया बैकअप जनरेट करें (Generate Backup)" else "Generate Full Backup")
                    }
                }

                if (generatedBackupFile != null) {
                    val f = generatedBackupFile!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isHindi) "बैकअप सफलतापूर्वक जनरेट हुआ!" else "Backup created successfully!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp
                            )
                        }
                        Text(f.name, fontSize = 10.sp, color = Color.DarkGray)

                        Button(
                            onClick = {
                                DataImportExportManager.shareFile(context, f, "application/json", "Rai Water Full Backup")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "बैकअप सुरक्षित साझा करें (Share)" else "Share Backup File")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. RESTORE TAB
// ==========================================

@Composable
fun RestoreTabContent(
    isHindi: Boolean,
    db: AppDatabase
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var validationResult by remember { mutableStateOf<DataImportExportManager.BackupValidationResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var restoreResultMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            isLoading = true
            restoreResultMessage = null

            coroutineScope.launch {
                val res = DataImportExportManager.validateBackupFile(context, uri)
                validationResult = res
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "बैकअप फ़ाइल से पुनर्स्थापना (Safe Restore)" else "Restore from Backup File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (isHindi) "JSON बैकअप फ़ाइल चुनें। सिस्टम पहले अखंडता और संगतता की जांच करेगा। पुनर्स्थापना से ठीक पूर्व वर्तमान डेटा का सुरक्षा बैकअप (Safety Snapshot) अपने आप बन जाएगा।" else "Select a JSON backup file. System validates integrity. A safety snapshot is automatically created before overwriting.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { filePicker.launch("application/json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "JSON बैकअप चुनें (Select Backup)" else "Select Backup File")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (validationResult != null) {
            val v = validationResult!!
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isHindi) "बैकअप फ़ाइल निरीक्षण व पूर्वावलोकन" else "Backup Inspection & Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    if (v.isValid) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (v.checksumValid) "SHA-256 अखंडता सत्यापित (Verified Checksum)" else "चेतावनी: चेकसम बेमेल",
                                color = if (v.checksumValid) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text("बैकअप दिनांक: ${v.backupDate} | वर्शन: ${v.version}", fontSize = 11.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill("ग्राहक", v.customerCount.toString(), MaterialTheme.colorScheme.primary)
                            StatPill("डिलीवरी", v.deliveryCount.toString(), Color(0xFF00796B))
                            StatPill("भुगतान", v.paymentCount.toString(), Color(0xFF1976D2))
                            StatPill("बिल", v.billCount.toString(), Color(0xFFF57C00))
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "डेटा पुनर्स्थापित करें (पुष्टि आवश्यक)" else "Confirm Restore")
                        }
                    } else {
                        Text("अमान्य बैकअप: ${v.errorMessage}", color = Color.Red, fontSize = 12.sp)
                    }

                    if (restoreResultMessage != null) {
                        Text(restoreResultMessage!!, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showConfirmDialog && validationResult != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(if (isHindi) "⚠️ क्या आप वाकई डेटा पुनर्स्थापित करना चाहते हैं?" else "Confirm Restore?") },
            text = {
                Text(
                    if (isHindi)
                        "यह क्रिया मौजूदा डेटाबेस को बैकअप के डेटा (${validationResult!!.customerCount} ग्राहक, ${validationResult!!.deliveryCount} डिलीवरी) से बदल देगी।\n\nआपकी सुरक्षा हेतु वर्तमान डेटा का एक आपातकालीन स्नैपशॉट बैकअप स्वतः सुरक्षित किया जाएगा।"
                    else
                        "This will replace existing database records. An automatic emergency safety snapshot of current data will be saved before restoring."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        coroutineScope.launch {
                            isLoading = true
                            val res = DataImportExportManager.executeSafeRestore(context, validationResult!!, db)
                            restoreResultMessage = res.message
                            isLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(if (isHindi) "हाँ, पुनर्स्थापित करें" else "Yes, Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
