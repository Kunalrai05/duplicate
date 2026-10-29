package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun BusinessSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI
    val settings by viewModel.businessSettings.collectAsState()

    var firmHindi by remember(settings) { mutableStateOf(settings.firmNameHindi) }
    var firmEng by remember(settings) { mutableStateOf(settings.firmName) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var rate by remember(settings) { mutableStateOf(settings.defaultCanRate.toInt().toString()) }
    var upi by remember(settings) { mutableStateOf(settings.upiId) }
    var savedNotice by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "व्यवसाय सेटिंग्स (Business Settings)" else "Business Settings",
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
            if (savedNotice) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Text(
                        text = if (isHindi) "सेटिंग्स सफलतापूर्वक सहेजी गईं!" else "Settings saved successfully!",
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "फर्म व बिलिंग जानकारी" else "Firm & Billing Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = firmHindi,
                        onValueChange = { firmHindi = it },
                        label = { Text(if (isHindi) "फर्म का नाम (हिंदी)" else "Firm Name (Hindi)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = firmEng,
                        onValueChange = { firmEng = it },
                        label = { Text(if (isHindi) "फर्म का नाम (English)" else "Firm Name (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isHindi) "संपर्क नंबर" else "Business Contact Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isHindi) "दुकान/संयंत्र का पता (पिपरिया)" else "Address (Pipariya)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text(if (isHindi) "डिफ़ॉल्ट दर प्रति 20L कैन (₹)" else "Default Rate Per 20L Can (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = upi,
                        onValueChange = { upi = it },
                        label = { Text(if (isHindi) "यूपीआई आईडी (QR कोड के लिए)" else "UPI ID (for Reminders & Bills)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.saveBusinessSettings(
                                settings.copy(
                                    firmNameHindi = firmHindi,
                                    firmName = firmEng,
                                    phone = phone,
                                    address = address,
                                    defaultCanRate = rate.toDoubleOrNull() ?: 30.0,
                                    upiId = upi
                                )
                            )
                            savedNotice = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "सेटिंग्स सुरक्षित करें (Save Profile)" else "Save Profile")
                    }
                }
            }
        }
    }
}
