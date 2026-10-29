package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.AppLanguage
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.MainViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI

    val config by viewModel.syncConfig.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMsg by viewModel.syncStatusMessage.collectAsState()

    var serverIp by remember(config) { mutableStateOf(config.pcServerIp) }
    var serverPort by remember(config) { mutableStateOf(config.pcServerPort.toString()) }
    var pin by remember(config) { mutableStateOf(config.securityPin) }
    var remoteUrl by remember(config) { mutableStateOf(config.remoteSyncUrl) }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "पीसी सिंक इंजन (PC Sync Engine)" else "PC Sync Engine & Remote Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isHindi) "स्थानीय वाई-फाई और रिमोट लिंकिंग" else "Local Wi-Fi & Tailscale Remote Sync",
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
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.ABOUT_SYNC_GUIDE) }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Guide", tint = MaterialTheme.colorScheme.primary)
                    }
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
                .padding(12.dp)
        ) {
            // Copy Notice Banner
            copyNotice?.let { notice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Text(
                        text = notice,
                        fontSize = 12.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Sync In-Progress Banner
            if (isSyncing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = syncMsg, fontSize = 13.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Medium)
                    }
                }
            }

            // PILLAR 1: DAILY ROUTINE (Morning Pull & Evening Push)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "दैनिक रूटीन (Daily Routine Sync)" else "Daily Routine Synchronization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi)
                            "सुबह रूट पर निकलने से पहले ग्राहक सूची लोड करें, और शाम को दैनिक डिलीवरी अपलोड करें।"
                        else
                            "Download morning routes before departure and upload evening deliveries & cash collections.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Morning Download Button
                    Button(
                        onClick = { viewModel.downloadMorningRoute() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "🌅 सुबह का रूट डाउनलोड करें (Download Morning Route)" else "🌅 Download Morning Route (PC → Phone)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Evening Upload Button
                    Button(
                        onClick = { viewModel.uploadEveningData() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "🌙 शाम का डेटा अपलोड करें (Upload Evening Deliveries)" else "🌙 Upload Evening Data (Phone → PC)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PILLAR 2: CONNECTION & SECURITY PIN
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "सर्वर सेटिंग्स व सुरक्षा पिन (PIN Security)" else "Server Settings & Security PIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = serverIp,
                            onValueChange = { serverIp = it },
                            label = { Text(if (isHindi) "पीसी आईपी (PC IP)" else "PC IP Address", fontSize = 11.sp) },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = serverPort,
                            onValueChange = { serverPort = it },
                            label = { Text("Port", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // PIN Row with New PIN generator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text(if (isHindi) "सुरक्षा पिन (6-Digit PIN)" else "Security PIN", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedButton(
                            onClick = { pin = (100000 + Random.nextInt(900000)).toString() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "🎲 नया पिन" else "🎲 New PIN", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remote Sync URL (Tailscale / Cloudflare)
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text(if (isHindi) "रिमोट लिंक (Tailscale / Cloudflare URL)" else "Remote URL (Tailscale / Internet)", fontSize = 11.sp) },
                        placeholder = { Text("http://100.x.x.x:5000", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveSyncConfig(
                                    config.copy(
                                        pcServerIp = serverIp,
                                        pcServerPort = serverPort.toIntOrNull() ?: 5000,
                                        securityPin = pin,
                                        remoteSyncUrl = remoteUrl
                                    )
                                )
                                viewModel.testPcConnection()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "कनेक्शन जांचें" else "Test PC", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val targetUrl = if (remoteUrl.isNotBlank()) remoteUrl else "http://$serverIp:$serverPort"
                                val shareText = "💧 Rai Chilled Water Mobile Sync Link:\nURL: $targetUrl\nSecurity PIN: $pin"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Sync Link", shareText))
                                copyNotice = "Copied setup link & PIN to clipboard!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "📋 लिंक कॉपी करें" else "📋 Copy Link", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PILLAR 3: DESKTOP SIMULATOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "डेस्कटॉप सिम्युलेटर (Desktop Simulator)" else "Desktop Simulator Test",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi)
                            "बिना कंप्यूटर के सिंक स्क्रीन, बधाई पॉपअप और डेटा ट्रांसफर का परीक्षण करें।"
                        else
                            "Test and verify the dual celebratory sync dialog without requiring a physical PC connection.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.triggerSimulatedSync() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "⚡ सिंक सिमुलेशन चलाएं (Test / Simulate Sync)" else "⚡ Test / Simulate Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
