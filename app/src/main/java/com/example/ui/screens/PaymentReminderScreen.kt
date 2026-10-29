package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageTone
import com.example.ui.theme.AppLanguage
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReminderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI
    val customers by viewModel.customers.collectAsState()

    var selectedTone by remember { mutableStateOf(MessageTone.POLITE) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val customersWithDue = remember(customers) {
        customers.filter { it.currentFinancialBalance > 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "भुगतान तगादा व रिमाइंडर (Payment Reminders)" else "Payment Reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isHindi) "व्हाट्सएप तगादा संदेश व यूपीआई लिंक" else "WhatsApp Reminder Templates & UPI Generator",
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
                .padding(12.dp)
        ) {
            copiedNotice?.let { notice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Text(text = notice, fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(10.dp))
                }
            }

            // Tone Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTone == MessageTone.POLITE,
                    onClick = { selectedTone = MessageTone.POLITE },
                    label = { Text(if (isHindi) "विनम्र (Polite)" else "Polite") }
                )
                FilterChip(
                    selected = selectedTone == MessageTone.FIRM,
                    onClick = { selectedTone = MessageTone.FIRM },
                    label = { Text(if (isHindi) "सख्त (Firm)" else "Firm") }
                )
                FilterChip(
                    selected = selectedTone == MessageTone.URGENT,
                    onClick = { selectedTone = MessageTone.URGENT },
                    label = { Text(if (isHindi) "अति आवश्यक (Urgent)" else "Urgent") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isHindi) "बकायादार ग्राहक सूची (${customersWithDue.size}):" else "Outstanding Accounts (${customersWithDue.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customersWithDue, key = { it.id }) { cust ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cust.nameHindi, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${cust.nameEnglish} • ${cust.locality}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("₹${cust.currentFinancialBalance.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFDC2626))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val message = when (selectedTone) {
                                MessageTone.POLITE ->
                                    "नमस्ते ${cust.nameHindi}, राय चिल्ड वॉटर पिपरिया से आपका ₹${cust.currentFinancialBalance.toInt()} का बकाया है। कृपया सुविधानुसार भुगतान करें।"
                                MessageTone.FIRM ->
                                    "प्रिय ग्राहक ${cust.nameHindi}, राय चिल्ड वॉटर से आपके ₹${cust.currentFinancialBalance.toInt()} काफी समय से शेष हैं। कृपया आज ही भुगतान करें।"
                                MessageTone.URGENT ->
                                    "अति आवश्यक सूचना: ${cust.nameHindi}, आपके खाते में ₹${cust.currentFinancialBalance.toInt()} बकाया हैं। जल आपूर्ति निरंतर रखने हेतु तुरंत भुगतान करें।"
                                else ->
                                    "नमस्ते ${cust.nameHindi}, राय चिल्ड वॉटर पिपरिया से आपका ₹${cust.currentFinancialBalance.toInt()} का बकाया है। कृपया शीघ्र भुगतान करें।"
                            }

                            Text(
                                text = message,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Reminder", message))
                                    copiedNotice = "Copied WhatsApp reminder for ${cust.nameEnglish}!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "व्हाट्सएप संदेश कॉपी करें" else "Copy WhatsApp Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
