package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SyncFeedbackDialog
import com.example.ui.components.SyncSuccessCelebrationDialog
import com.example.ui.screens.*
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.RaiChilledWaterTheme
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RaiChilledWaterTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val isHindi = language == AppLanguage.HINDI
    val settings by viewModel.businessSettings.collectAsState()

    val feedback by viewModel.syncFeedback.collectAsState()
    val showCelebration by viewModel.showCelebrationDialog.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = activeScreen != ActiveScreen.ROUTE_SHEET) {
        viewModel.navigateTo(ActiveScreen.ROUTE_SHEET)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(20.dp)
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = settings.firmNameHindi,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "${settings.firmName} • पिपरिया",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DrawerItem(
                        icon = Icons.Default.LocalShipping,
                        label = if (isHindi) "🚚 दैनिक रूट डिलीवरी" else "🚚 Daily Route Sheet",
                        selected = activeScreen == ActiveScreen.ROUTE_SHEET,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.ROUTE_SHEET)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.AccountBalance,
                        label = if (isHindi) "👥 ग्राहक खाता बही (Ledger)" else "👥 Customer Ledger",
                        selected = activeScreen == ActiveScreen.CUSTOMER_LEDGER,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.CUSTOMER_LEDGER)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.Sync,
                        label = if (isHindi) "⚡ पीसी सिंक इंजन (PC Sync)" else "⚡ PC Sync Engine",
                        selected = activeScreen == ActiveScreen.SYNC_SETTINGS,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.SYNC_SETTINGS)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.ReceiptLong,
                        label = if (isHindi) "🧾 बिलिंग व इनवॉइस" else "🧾 Bills & Invoices",
                        selected = activeScreen == ActiveScreen.BILLS_HISTORY,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.BILLS_HISTORY)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    DrawerItem(
                        icon = Icons.Default.Inventory,
                        label = if (isHindi) "📦 उत्पाद कैटलॉग व दर" else "📦 Product Catalog",
                        selected = activeScreen == ActiveScreen.PRODUCT_CATALOG,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.PRODUCT_CATALOG)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.NotificationsActive,
                        label = if (isHindi) "🔔 भुगतान तगादा (Reminders)" else "🔔 Payment Reminders",
                        selected = activeScreen == ActiveScreen.PAYMENT_REMINDERS,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.PAYMENT_REMINDERS)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.DocumentScanner,
                        label = if (isHindi) "📷 रजिस्टर स्कैन (Sheet OCR)" else "📷 Register Scan & OCR",
                        selected = activeScreen == ActiveScreen.REGISTER_SCAN,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.REGISTER_SCAN)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.FormatListBulleted,
                        label = if (isHindi) "🔣 संकेत शब्दकोश (Symbols)" else "🔣 Notation Symbols",
                        selected = activeScreen == ActiveScreen.SYMBOL_DICTIONARY,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.SYMBOL_DICTIONARY)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.Insights,
                        label = if (isHindi) "📈 व्यापार सारांश (Analytics)" else "📈 Business Analytics",
                        selected = activeScreen == ActiveScreen.BUSINESS_SUMMARY,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.BUSINESS_SUMMARY)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    DrawerItem(
                        icon = Icons.Default.Settings,
                        label = if (isHindi) "⚙️ व्यवसाय सेटिंग्स" else "⚙️ Business Settings",
                        selected = activeScreen == ActiveScreen.BUSINESS_SETTINGS,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.BUSINESS_SETTINGS)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.HelpOutline,
                        label = if (isHindi) "📖 सिंक मार्गदर्शिका" else "📖 Sync & Network Guide",
                        selected = activeScreen == ActiveScreen.ABOUT_SYNC_GUIDE,
                        onClick = {
                            viewModel.navigateTo(ActiveScreen.ABOUT_SYNC_GUIDE)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = activeScreen == ActiveScreen.ROUTE_SHEET,
                        onClick = { viewModel.navigateTo(ActiveScreen.ROUTE_SHEET) },
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Route", modifier = Modifier.size(20.dp)) },
                        label = { Text(if (isHindi) "रूट" else "Route", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == ActiveScreen.CUSTOMER_LEDGER,
                        onClick = { viewModel.navigateTo(ActiveScreen.CUSTOMER_LEDGER) },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Ledger", modifier = Modifier.size(20.dp)) },
                        label = { Text(if (isHindi) "खाता" else "Ledger", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == ActiveScreen.BILLS_HISTORY,
                        onClick = { viewModel.navigateTo(ActiveScreen.BILLS_HISTORY) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Bills", modifier = Modifier.size(20.dp)) },
                        label = { Text(if (isHindi) "बिल" else "Bills", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == ActiveScreen.SYNC_SETTINGS,
                        onClick = { viewModel.navigateTo(ActiveScreen.SYNC_SETTINGS) },
                        icon = { Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(20.dp)) },
                        label = { Text(if (isHindi) "सिंक" else "Sync", fontSize = 10.sp) }
                    )
                }
            }
        ) { paddingValues ->
            val mod = Modifier
                .fillMaxSize()
                .padding(paddingValues)

            when (activeScreen) {
                ActiveScreen.ROUTE_SHEET -> RouteSheetScreen(
                    viewModel = viewModel,
                    modifier = mod,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                )
                ActiveScreen.CUSTOMER_LEDGER -> CustomerLedgerScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.SYNC_SETTINGS -> SyncSettingsScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.ABOUT_SYNC_GUIDE -> AboutSyncGuideScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.BILLS_HISTORY -> BillsHistoryScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.PAYMENT_REMINDERS -> PaymentReminderScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.BUSINESS_SETTINGS -> BusinessSettingsScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.PRODUCT_CATALOG -> ProductCatalogScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.SYMBOL_DICTIONARY -> SymbolDictionaryScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.BUSINESS_SUMMARY -> BusinessSummaryScreen(viewModel = viewModel, modifier = mod)
                ActiveScreen.REGISTER_SCAN -> RegisterScanScreen(viewModel = viewModel, modifier = mod)
            }

            // Dual Celebratory Success Dialog
            if (showCelebration && feedback != null) {
                SyncSuccessCelebrationDialog(
                    feedback = feedback!!,
                    onDismiss = { viewModel.dismissFeedback() }
                )
            } else if (feedback != null && !feedback!!.isSuccess) {
                SyncFeedbackDialog(
                    feedback = feedback!!,
                    onDismiss = { viewModel.dismissFeedback() }
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        label = { Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
