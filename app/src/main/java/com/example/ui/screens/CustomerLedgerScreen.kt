package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun CustomerLedgerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI

    val customers by viewModel.customers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showGuideDialog by remember { mutableStateOf(false) }
    var selectedCustomerForPay by remember { mutableStateOf<Customer?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("CASH") }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            val q = searchQuery.trim().lowercase()
            customers.filter {
                it.nameHindi.lowercase().contains(q) ||
                it.nameEnglish.lowercase().contains(q) ||
                it.phone.contains(q) ||
                it.locality.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "ग्राहक खाता बही (Customer Ledger)" else "Customer Ledger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isHindi) "डुअल-लेजर: भौतिक कैन व वित्तीय बकाया" else "Dual-Ledger: Physical Cans & Financial Dues",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.ROUTE_SHEET) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Guide Button with proper bilingual translation
                    OutlinedButton(
                        onClick = { showGuideDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "मार्गदर्शिका (Guide)" else "User Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(if (isHindi) "English" else "हिंदी", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = if (isHindi) "ग्राहक का नाम, फोन या मोहल्ला खोजें..." else "Search customer name, phone, or route...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            // Total Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val totalCans = customers.sumOf { it.currentCanBalance }
                    val totalDue = customers.sumOf { it.currentFinancialBalance }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalCans",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHindi) "कुल कैन बाहर (Cans Outside)" else "Total Cans Outside",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "₹${totalDue.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = if (isHindi) "कुल बाजार बकाया (Total Due)" else "Total Market Due",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Customer Accounts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
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
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${customer.nameEnglish} • ${customer.locality}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "📞 ${customer.phone}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF0369A1)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${customer.currentFinancialBalance.toInt()}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.currentFinancialBalance > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    )
                                    Text(
                                        text = "${customer.currentCanBalance} कैन (₹${customer.ratePerCan.toInt()}/कैन)",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            selectedCustomerForPay = customer
                                            payAmount = customer.currentFinancialBalance.toInt().coerceAtLeast(0).toString()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (isHindi) "भुगतान (Pay)" else "Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Guide Dialog
    if (showGuideDialog) {
        Dialog(onDismissRequest = { showGuideDialog = false }) {
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
                            text = if (isHindi) "खाता बही मार्गदर्शिका (Ledger Guide)" else "Customer Ledger Guide",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showGuideDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHindi)
                            "1. डुअल-लेजर प्रणाली:\n• कैन खाता: ग्राहक के पास कितनी भौतिक 20L जार जमा हैं।\n• वित्तीय खाता: ग्राहक की शुद्ध बकाया राशि (₹)।\n\n2. 'भुगतान (Pay)' विकल्प:\n• जब ग्राहक नकद या UPI से पैसे दे, तो 'भुगतान' पर क्लिक करें। राशि दर्ज करके सबमिट करते ही बकाया स्वतः घट जाएगा।\n\n3. अनुवाद व लिप्यंतरण:\n• हिंदी नाम और अंग्रेजी नाम दोनों स्क्रीन पर एक साथ दिखते हैं ताकि आप व आपका स्टाफ बिना भ्रम के काम कर सकें।"
                        else
                            "1. Dual-Ledger System:\n• Can Balance: Tracks physical 20L chilled water jars held by each account.\n• Financial Balance: Tracks active monetary outstanding dues (₹).\n\n2. 'Pay' Action Button:\n• Whenever cash or UPI collections are received, click 'Pay' to log payment records. The ledger balance auto-adjusts instantly.\n\n3. Transliteration:\n• Hindi & English customer names are linked side-by-side to ensure seamless operation for all staff.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showGuideDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "समझ गया (Got It)" else "Understood")
                    }
                }
            }
        }
    }

    // Pay Option Dialog
    selectedCustomerForPay?.let { cust ->
        Dialog(onDismissRequest = { selectedCustomerForPay = null }) {
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
                        text = if (isHindi) "भुगतान संग्रह (Collect Payment)" else "Record Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${cust.nameEnglish} • ${cust.nameHindi}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text(if (isHindi) "जमा राशि (₹ Amount)" else "Amount Received (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                            label = { Text(if (isHindi) "ऑनलाइन (UPI)" else "UPI") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val amt = payAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.recordPayment(cust.id, amt, payMethod, "", "Ledger Collection")
                                selectedCustomerForPay = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "भुगतान दर्ज करें (Submit)" else "Confirm Payment")
                    }
                }
            }
        }
    }
}
