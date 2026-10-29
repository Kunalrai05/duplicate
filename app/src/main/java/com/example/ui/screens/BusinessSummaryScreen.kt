package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppLanguage
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSummaryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI

    val customers by viewModel.customers.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val totalCansOutside = customers.sumOf { it.currentCanBalance }
    val totalMarketDue = customers.sumOf { it.currentFinancialBalance }
    val todayDeliveries = deliveries.filter { it.date == selectedDate }
    val todayCansGiven = todayDeliveries.sumOf { it.cansGiven }
    val todayCansReceived = todayDeliveries.sumOf { it.cansReceived }
    val todayRevenue = todayDeliveries.sumOf { it.cansGiven.toDouble() * it.rateApplied }
    val todayPayments = payments.filter { it.date == selectedDate }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "व्यापार सारांश (Business Summary)" else "Business Analytics Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.ROUTE_SHEET) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Hero Card matching user screenshot with Excel Center & Entry Sudhar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_summary_hero"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF136473)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Rai Chilled Water (Pipariya)",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Today's Summary • $selectedDate",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "सिंक पूर्ण" else "Sync Ready",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "खाता व बिल" button matching original and screenshot
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(ActiveScreen.CUSTOMER_LEDGER) }
                                .testTag("btn_khata_bill_summary")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "खाता व बिल" else "Khata & Bills",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // "एंट्री सुधार" / "एंट्री जाँचे" button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(ActiveScreen.REGISTER_SCAN) }
                                .testTag("btn_entry_sudhar_summary")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "एंट्री सुधार" else "Entry Sudhar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isHindi) "आज का परिचालन ($selectedDate):" else "Today's Route Operations ($selectedDate):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetricCard(
                    title = if (isHindi) "आज दी गई कैन" else "Cans Delivered",
                    value = "$todayCansGiven",
                    subtitle = "₹${todayRevenue.toInt()} बिक्री",
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = if (isHindi) "खाली कैन वापस" else "Empty Returned",
                    value = "$todayCansReceived",
                    subtitle = "वापस आई",
                    color = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetricCard(
                    title = if (isHindi) "आज नकद/UPI वसूली" else "Collections Today",
                    value = "₹${todayPayments.toInt()}",
                    subtitle = "संग्रह राशि",
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = if (isHindi) "कुल सक्रिय ग्राहक" else "Active Accounts",
                    value = "${customers.size}",
                    subtitle = "पंजीकृत",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isHindi) "बाजार परिसंपत्ति व बकाया (Market Status):" else "Overall Market Assets & Outstanding:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHindi) "दुकानों/घरों में कुल कैन (20L Jars Outside):" else "Total 20L Jars with Customers:", fontSize = 13.sp)
                        Text("$totalCansOutside कैन", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0284C7))
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHindi) "कुल बाजार बकाया (Total Market Due):" else "Total Outstanding Receivables:", fontSize = 13.sp)
                        Text("₹${totalMarketDue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        }
    }
}
