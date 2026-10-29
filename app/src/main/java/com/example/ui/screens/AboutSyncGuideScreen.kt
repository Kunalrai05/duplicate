package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
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
fun AboutSyncGuideScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "सिंक मार्गदर्शिका (Sync Guide)" else "Sync & Network Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.SYNC_SETTINGS) }) {
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
            GuideStepCard(
                stepNum = "1",
                title = if (isHindi) "स्थानीय वाई-फाई कनेक्शन (Local Wi-Fi)" else "Same Wi-Fi Network Setup",
                body = if (isHindi)
                    "• सुनिश्चित करें कि पीसी और मोबाइल दोनों एक ही वाई-फाई या फोन हॉटस्पॉट से जुड़े हों।\n• पीसी ऐप के 'Sync Server' टैब पर जाएं और सर्वर चालू (START) करें।"
                else
                    "• Connect PC and phone to the same Wi-Fi router or phone mobile hotspot.\n• On PC app, open the Sync Server tab and click 'Start Server' (Port 5000)."
            )

            Spacer(modifier = Modifier.height(10.dp))

            GuideStepCard(
                stepNum = "2",
                title = if (isHindi) "सुरक्षा पिन व प्रमाणीकरण (PIN Authentication)" else "Security PIN & Verification",
                body = if (isHindi)
                    "• पीसी स्क्रीन पर प्रदर्शित 6-अंकीय पिन मोबाइल ऐप में दर्ज करें।\n• मोबाइल से 'कनेक्शन जांचें' बटन दबाकर पुष्टि करें।"
                else
                    "• Enter the 6-digit security PIN displayed on the PC into the mobile app.\n• Tap 'Test PC' to verify handshake before daily delivery run."
            )

            Spacer(modifier = Modifier.height(10.dp))

            GuideStepCard(
                stepNum = "3",
                title = if (isHindi) "दैनिक रूटीन (Daily Routine Workflow)" else "Daily Routine Operations",
                body = if (isHindi)
                    "• 🌅 सुबह: 'सुबह का रूट डाउनलोड करें' पर टैप करके ताज़ा ग्राहक सूची लें।\n• 🚚 दिनभर: डिलीवरी दर्ज करें और नकद/UPI भुगतान लें (इंटरनेट की आवश्यकता नहीं)।\n• 🌙 शाम: दुकान लौटने पर 'शाम का डेटा अपलोड करें' पर टैप करें।"
                else
                    "• 🌅 Morning: Tap 'Download Morning Route' to get updated customer list & rates.\n• 🚚 Daytime: Record deliveries and cash collections offline without internet.\n• 🌙 Evening: Return to plant and tap 'Upload Evening Deliveries' to finalize accounts."
            )

            Spacer(modifier = Modifier.height(10.dp))

            GuideStepCard(
                stepNum = "4",
                title = if (isHindi) "रिमोट सिंक (4G / विभिन्न स्थान / टेलस्केल)" else "Remote Sync (4G / Tailscale Mesh VPN)",
                body = if (isHindi)
                    "• यदि वाहन चालक बाहर 4G डेटा पर हैं, तो टेलस्केल (Tailscale) या क्लाउडफ्लेयर टनल का उपयोग करें।\n• पीसी से 'Setup Link' कॉपी करके व्हाट्सएप पर भेजें और मोबाइल में पेस्ट करें।"
                else
                    "• For 4G mobile data syncing across city areas, install Tailscale VPN on PC & phone.\n• Paste the Tailscale IP (100.x.x.x:5000) into Remote URL field."
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.navigateTo(ActiveScreen.SYNC_SETTINGS) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isHindi) "सिंक सेटिंग्स पर वापस जाएं" else "Return to Sync Settings")
            }
        }
    }
}

@Composable
fun GuideStepCard(
    stepNum: String,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stepNum, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
