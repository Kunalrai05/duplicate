package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Customer
import com.example.data.ocr.ExtractedDailyEntry
import com.example.data.ocr.SheetOcrEngine
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RaiGold
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiSuccessGreen
import com.example.ui.theme.RaiWarningAmber
import com.example.ui.theme.RaiWaterBlue
import com.example.ui.theme.SurfaceLight
import com.example.viewmodel.MainViewModel
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetVerificationScreen(
    viewModel: MainViewModel,
    isHindi: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.rawCustomers.collectAsState()
    val pendingScanFilePath by viewModel.pendingVerificationScanFile.collectAsState()

    var selectedCustomer by remember(customers) {
        mutableStateOf(
            customers.find { it.nameHindi.contains("ग्राम शक्ती") } ?: customers.firstOrNull()
        )
    }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }
    var selectedMonthYear by remember { mutableStateOf("2026-10") } // e.g. "2026-10"
    var routeName by remember { mutableStateOf("स्टेशन रोड डिलीवरी रजिस्टर") }
    var isPhotoPreviewExpanded by remember { mutableStateOf(false) }

    // Calendar Picker Helper for Month/Date
    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, _ ->
                val m = (month + 1).toString().padStart(2, '0')
                selectedMonthYear = "$year-$m"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Dynamic extraction: read from real captured/uploaded file instead of repeating sample data
    val dailyRows = remember(pendingScanFilePath, selectedCustomer?.id) {
        val imageFile = if (pendingScanFilePath != null && File(pendingScanFilePath!!).exists()) {
            File(pendingScanFilePath!!)
        } else {
            val sampleTarget = File(context.filesDir, "scans/SAMPLE_SHEET_GRAM_SHAKTI.jpg")
            val primarySample = File("/public/assets/aistudio/20261024_182359g_15730b6e.jpg")
            val fallbackSample = File("/public/assets/aistudio/20261024_182359_1aaaa7db.jpg")
            when {
                sampleTarget.exists() -> sampleTarget
                primarySample.exists() -> primarySample
                else -> fallbackSample
            }
        }
        val result = SheetOcrEngine.processSheetImage(imageFile, customerHint = selectedCustomer?.nameHindi)
        mutableStateListOf<ExtractedDailyEntry>().apply {
            addAll(result.entries)
        }
    }

    val totalGiven = dailyRows.sumOf { it.cansGiven }
    val totalReceived = dailyRows.sumOf { it.cansReceived }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Banner
        Surface(
            color = RaiNavyDark,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "केन जांचें (Sheet Entry Review)" else "Sheet Entry Verification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isHindi) "1 से 31 तारीख की प्रविष्टियां जांचें व बदलें" else "Review & edit date-wise entries (1 to 31)",
                            fontSize = 11.5.sp,
                            color = Color(0xFFC7D7EC)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "दिया: $totalGiven | लिया: $totalReceived",
                            color = RaiGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Customer & Date Selection Card with High Contrast (Fixes text visibility issue in both light & dark mode)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isHindi) "ग्राहक व माह चुनें (Customer & Month)" else "Select Customer & Month",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCustomerDropdownExpanded,
                        onExpandedChange = { isCustomerDropdownExpanded = it },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.nameHindi ?: (if (isHindi) "ग्राहक चुनें" else "Customer"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHindi) "ग्राहक" else "Customer") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RaiNavyDark) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = RaiNavyDark,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("sheet_verify_customer_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = isCustomerDropdownExpanded,
                            onDismissRequest = { isCustomerDropdownExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = { Text("${cust.nameHindi} (${cust.locality})") },
                                    onClick = {
                                        selectedCustomer = cust
                                        isCustomerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Month Selector with Calendar Picker
                    OutlinedTextField(
                        value = selectedMonthYear,
                        onValueChange = { selectedMonthYear = it },
                        readOnly = true,
                        label = { Text(if (isHindi) "माह (कैलेंडर)" else "Month") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = RaiNavyDark,
                                modifier = Modifier.clickable { showDatePicker() }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = RaiNavyDark,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker() }
                            .testTag("sheet_verify_month_picker")
                    )
                }

                // Photo preview toggle
                pendingScanFilePath?.let { filePath ->
                    val file = File(filePath)
                    if (file.exists()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPhotoPreviewExpanded = !isPhotoPreviewExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = RaiWaterBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "📸 स्कैन शीट फोटो देखें (${file.name})" else "📸 View Scanned Photo (${file.name})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RaiWaterBlue
                                )
                            }
                            Icon(
                                if (isPhotoPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = RaiWaterBlue
                            )
                        }

                        AnimatedVisibility(visible = isPhotoPreviewExpanded) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Scanned Sheet Photo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Table Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "दिनांक" else "Date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.width(65.dp)
                )
                Text(
                    text = if (isHindi) "केन दिया (Out)" else "Given (Out)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RaiSuccessGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = if (isHindi) "केन लिया (In)" else "Returned (In)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RaiWarningAmber,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = if (isHindi) "प्रतीक" else "Symbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(55.dp)
                )
            }
        }

        // Table Rows List (Days 1 to 31)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            itemsIndexed(dailyRows) { index, row ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("sheet_day_row_${row.day}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date label
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RaiNavyDark.copy(alpha = 0.08f),
                            modifier = Modifier.width(60.dp)
                        ) {
                            Text(
                                text = "${row.day.toString().padStart(2, '0')} तारीख",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaiNavyDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Cans Given Stepper
                        Row(
                            modifier = Modifier.weight(1.2f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (row.cansGiven > 0) {
                                        val newG = row.cansGiven - 1
                                        dailyRows[index] = row.copy(
                                            cansGiven = newG,
                                            notation = if (newG == 0) "X" else "$newG"
                                        )
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Given", modifier = Modifier.size(15.dp))
                            }
                            Text(
                                text = "${row.cansGiven}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaiSuccessGreen,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = {
                                    val newG = row.cansGiven + 1
                                    dailyRows[index] = row.copy(
                                        cansGiven = newG,
                                        notation = "$newG"
                                    )
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Given", modifier = Modifier.size(15.dp))
                            }
                        }

                        // Cans Received Stepper
                        Row(
                            modifier = Modifier.weight(1.2f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (row.cansReceived > 0) {
                                        val newR = row.cansReceived - 1
                                        dailyRows[index] = row.copy(cansReceived = newR)
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Received", modifier = Modifier.size(15.dp))
                            }
                            Text(
                                text = "${row.cansReceived}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaiWarningAmber,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = {
                                    val newR = row.cansReceived + 1
                                    dailyRows[index] = row.copy(cansReceived = newR)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Received", modifier = Modifier.size(15.dp))
                            }
                        }

                        // Notation Symbol Clickable Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .width(55.dp)
                                .clickable {
                                    val nextSymbol = when (row.notation) {
                                        "1" -> "2"
                                        "2" -> "2+1"
                                        "2+1" -> "X"
                                        "X" -> "1"
                                        else -> "1"
                                    }
                                    val (g, r) = when (nextSymbol) {
                                        "1" -> Pair(1, 1)
                                        "2" -> Pair(2, 2)
                                        "2+1" -> Pair(3, 3)
                                        "X" -> Pair(0, 0)
                                        else -> Pair(1, 1)
                                    }
                                    dailyRows[index] = row.copy(cansGiven = g, cansReceived = r, notation = nextSymbol)
                                }
                        ) {
                            Text(
                                text = row.notation,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onFinished,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }

                Button(
                    onClick = {
                        val cust = selectedCustomer
                        if (cust != null) {
                            viewModel.commitExtractedSheet(
                                customer = cust,
                                yearMonth = selectedMonthYear,
                                entries = dailyRows,
                                routeOrNote = routeName,
                                onComplete = {
                                    Toast.makeText(
                                        context,
                                        if (isHindi) "31 दिनों की प्रविष्टियां स्थानीय डेटाबेस व सिंक कतार में सुरक्षित हो गईं!"
                                        else "31 days entries saved to local DB and sync queue!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onFinished()
                                }
                            )
                        } else {
                            Toast.makeText(context, "कृपया ग्राहक चुनें", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(46.dp)
                        .testTag("btn_commit_sheet_entries")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaiGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isHindi) "सत्यापित करें व सहेजें (OK)" else "Verify & Save (OK)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
