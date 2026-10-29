package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.ui.theme.AppLanguage
import com.example.util.TransliterationHelper
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSheetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null
) {
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI

    val customers by viewModel.customers.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showPayDialogCustomer by remember { mutableStateOf<Customer?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("CASH") }
    var payUtr by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Dialog state for adding new customer
    var newCustHindi by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }
    var newCustRoute by remember { mutableStateOf("Station Road") }
    var newCustRate by remember { mutableStateOf("30") }

    val routes = listOf("All Routes", "Station Road", "Sadar Bazar", "Subzi Mandi", "Gandhi Chowk", "Bank Road")

    val filteredCustomers = remember(customers, selectedRoute) {
        if (selectedRoute == "All Routes") customers
        else customers.filter { it.locality.equals(selectedRoute, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "दैनिक रूट डिलीवरी (Daily Route)" else "Daily Route Sheet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isHindi) "तारीख: $selectedDate • पिपरिया रूट" else "Date: $selectedDate • Pipariya Route",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.SYNC_SETTINGS) }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(if (isHindi) "English" else "हिंदी", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Route Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                routes.forEach { r ->
                    FilterChip(
                        selected = selectedRoute == r,
                        onClick = { viewModel.selectedRoute.value = r },
                        label = {
                            Text(
                                text = if (isHindi) when (r) {
                                    "All Routes" -> "सभी रूट"
                                    "Station Road" -> "स्टेशन रोड"
                                    "Sadar Bazar" -> "सदर बाजार"
                                    "Subzi Mandi" -> "सब्जी मंडी"
                                    "Gandhi Chowk" -> "गांधी चौक"
                                    "Bank Road" -> "बैंक रोड"
                                    else -> r
                                } else r,
                                fontSize = 11.sp,
                                fontWeight = if (selectedRoute == r) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Route Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "कुल ग्राहक: ${filteredCustomers.size}" else "Total Accounts: ${filteredCustomers.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Text(
                    text = if (isHindi) "पिपरिया जल वितरण प्रणाली" else "Pipariya Delivery Hub",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Customer List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    CustomerRouteCard(
                        customer = customer,
                        isHindi = isHindi,
                        onRapidDelivery = { notation, given, rec ->
                            viewModel.recordDelivery(customer.id, notation, given, rec, customer.ratePerCan)
                        },
                        onPayClick = {
                            showPayDialogCustomer = customer
                            payAmount = customer.currentFinancialBalance.toInt().coerceAtLeast(0).toString()
                        }
                    )
                }
            }
        }
    }

    // Pay Option Dialog with English & Hindi guide translations
    showPayDialogCustomer?.let { cust ->
        Dialog(onDismissRequest = { showPayDialogCustomer = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "भुगतान संग्रह (Record Payment)" else "Record Payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showPayDialogCustomer = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${cust.nameEnglish} • ${cust.nameHindi}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isHindi) "वर्तमान बकाया: ₹${cust.currentFinancialBalance.toInt()} | कैन: ${cust.currentCanBalance}"
                               else "Current Balance: ₹${cust.currentFinancialBalance.toInt()} | Cans: ${cust.currentCanBalance}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text(if (isHindi) "राशि (₹ Amount)" else "Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = payMethod == "CASH",
                            onClick = { payMethod = "CASH" },
                            label = { Text(if (isHindi) "नकद (Cash)" else "Cash") }
                        )
                        FilterChip(
                            selected = payMethod == "UPI",
                            onClick = { payMethod = "UPI" },
                            label = { Text(if (isHindi) "यूपीआई (UPI)" else "UPI") }
                        )
                    }

                    if (payMethod == "UPI") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = payUtr,
                            onValueChange = { payUtr = it },
                            label = { Text(if (isHindi) "यूटीआर / संदर्भ संख्या (UTR No.)" else "UTR Reference") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val amt = payAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.recordPayment(cust.id, amt, payMethod, payUtr, "Ledger Payment")
                                showPayDialogCustomer = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "भुगतान जमा करें (Submit Payment)" else "Submit Payment (₹)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        Dialog(onDismissRequest = { showAddCustomerDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isHindi) "नया ग्राहक जोड़ें (Add Customer)" else "Add New Customer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newCustHindi,
                        onValueChange = { newCustHindi = it },
                        label = { Text(if (isHindi) "ग्राहक का नाम (Customer Name)" else "Customer Name (Hindi/English)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर (Mobile Phone)" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newCustRoute,
                        onValueChange = { newCustRoute = it },
                        label = { Text(if (isHindi) "रूट / मोहल्ला (Route / Locality)" else "Route / Locality") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddCustomerDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "रद्द करें" else "Cancel")
                        }
                        Button(
                            onClick = {
                                if (newCustHindi.isNotBlank()) {
                                    val newId = "CUST-${System.currentTimeMillis() % 10000}"
                                    val cust = Customer(
                                        id = newId,
                                        nameHindi = newCustHindi,
                                        nameEnglish = TransliterationHelper.toEnglish(newCustHindi),
                                        phone = newCustPhone,
                                        locality = newCustRoute,
                                        ratePerCan = newCustRate.toDoubleOrNull() ?: 30.0
                                    )
                                    viewModel.addCustomer(cust)
                                    showAddCustomerDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "जोड़ें" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerRouteCard(
    customer: Customer,
    isHindi: Boolean,
    onRapidDelivery: (notation: String, cansGiven: Int, cansRec: Int) -> Unit,
    onPayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.nameHindi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${customer.nameEnglish} • ${customer.locality}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Balance Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${customer.currentCanBalance} कैन",
                            color = Color(0xFF0369A1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(if (customer.currentFinancialBalance > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "₹${customer.currentFinancialBalance.toInt()}",
                            color = if (customer.currentFinancialBalance > 0) Color(0xFFB91C1C) else Color(0xFF15803D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rapid Notation Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [1] - 1 Can
                NotationChip(label = "1", desc = "1 Can", color = Color(0xFF0284C7), modifier = Modifier.weight(1f)) {
                    onRapidDelivery("1", 1, 0)
                }
                // [2] - 2 Cans
                NotationChip(label = "2", desc = "2 Cans", color = Color(0xFF0284C7), modifier = Modifier.weight(1f)) {
                    onRapidDelivery("2", 2, 0)
                }
                // [2+1] - 2 Delivered, 1 Returned
                NotationChip(label = "2+1", desc = "2+1 Ret", color = Color(0xFF0EA5E9), modifier = Modifier.weight(1.1f)) {
                    onRapidDelivery("2+1", 2, 1)
                }
                // [/] - Empty Returned
                NotationChip(label = "/", desc = "Empty", color = Color(0xFFD97706), modifier = Modifier.weight(1f)) {
                    onRapidDelivery("/", 0, 1)
                }
                // [X] - Closed
                NotationChip(label = "X", desc = "Closed", color = Color(0xFF64748B), modifier = Modifier.weight(1f)) {
                    onRapidDelivery("X", 0, 0)
                }

                // Pay Button
                Button(
                    onClick = onPayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(12.dp))
                    Text(if (isHindi) "जमा" else "Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotationChip(
    label: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
    }
}
