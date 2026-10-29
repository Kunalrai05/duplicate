package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillCustomization
import com.example.data.model.BillProductLineItem
import com.example.data.model.BillRecord
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.PaymentRecord
import com.example.data.model.ProductItem
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RaiGold
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiSuccessGreen
import com.example.ui.theme.RaiWarningAmber
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEditorDialog(
    customer: Customer,
    deliveries: List<DeliveryEntry>,
    payments: List<PaymentRecord>,
    viewModel: MainViewModel,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onConfirmAndSendWhatsApp: (BillCustomization) -> Unit
) {
    val context = LocalContext.current
    val activeProducts by viewModel.activeProducts.collectAsState()

    // Date formatting helper
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())
    val firstDayOfMonth = "${today.take(7)}-01"

    // Editable Serial Number & Billing Date
    var serialNumber by remember { mutableStateOf("5156") }
    var billingDate by remember { mutableStateOf(today) }

    // Date Range Selection (Requirement 9: Select from which date till which to generate bill)
    var fromDate by remember { mutableStateOf(firstDayOfMonth) }
    var toDate by remember { mutableStateOf(today) }
    var showPreviewModal by remember { mutableStateOf(false) }

    // Calendar helper
    fun showDatePicker(currentVal: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        if (currentVal.isNotBlank()) {
            val parts = currentVal.split("-")
            if (parts.size == 3) {
                calendar.set(Calendar.YEAR, parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH))
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val m = (month + 1).toString().padStart(2, '0')
                val d = dayOfMonth.toString().padStart(2, '0')
                onDateSelected("$year-$m-$d")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Filter customer deliveries within selected date range
    val periodDeliveries = remember(deliveries, fromDate, toDate, customer.id) {
        deliveries.filter {
            it.customerId == customer.id && it.date >= fromDate && it.date <= toDate
        }
    }

    var totalDeliveredCans by remember {
        mutableIntStateOf(periodDeliveries.sumOf { it.cansGiven }.coerceAtLeast(1))
    }
    var totalReturnedCans by remember {
        mutableIntStateOf(periodDeliveries.sumOf { it.cansReceived })
    }

    // Auto-update delivered/returned count when period changes
    LaunchedEffect(periodDeliveries) {
        val given = periodDeliveries.sumOf { it.cansGiven }
        if (given > 0) {
            totalDeliveredCans = given
            totalReturnedCans = periodDeliveries.sumOf { it.cansReceived }
        }
    }

    // Editable Rate Per Can (Default 30, with 25, 35, 40 options)
    var selectedRate by remember { mutableDoubleStateOf(customer.ratePerCan.takeIf { it > 0 } ?: 30.0) }

    // Extra Products
    val extraProducts = remember { mutableStateListOf<BillProductLineItem>() }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // Calculations (Requirement 9: strictly does NOT include due amount!)
    val waterCharges = totalDeliveredCans * selectedRate
    val extraCharges = extraProducts.sumOf { it.totalPrice }
    val defaultCalculatedTotal = (waterCharges + extraCharges).coerceAtLeast(0.0)

    // Manual total override
    var isManualTotalOverride by remember { mutableStateOf(false) }
    var manualTotalText by remember { mutableStateOf(defaultCalculatedTotal.toInt().toString()) }

    LaunchedEffect(defaultCalculatedTotal) {
        if (!isManualTotalOverride) {
            manualTotalText = defaultCalculatedTotal.toInt().toString()
        }
    }

    val effectiveTotal = if (isManualTotalOverride) {
        manualTotalText.toDoubleOrNull() ?: defaultCalculatedTotal
    } else {
        defaultCalculatedTotal
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "बिल बनाएं व संपादित करें" else "Create & Edit Bill",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "${customer.nameHindi} • ${customer.phone}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section 1: Serial Number & Billing Date (Requirement 8)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = serialNumber,
                                onValueChange = { serialNumber = it },
                                label = { Text(if (isHindi) "सीरियल नं / Bill No" else "Serial No") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = billingDate,
                                onValueChange = { billingDate = it },
                                label = { Text(if (isHindi) "बिलिंग दिनांक" else "Billing Date") },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker(billingDate) { billingDate = it } }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }

                    // Section 2: Bill Period Selection (Requirement 9)
                    item {
                        Text(
                            text = if (isHindi) "बिल अवधि चुनें (From - To Date):" else "Select Bill Period:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker(fromDate) { fromDate = it } },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(fromDate, fontSize = 11.5.sp)
                            }

                            OutlinedButton(
                                onClick = { showDatePicker(toDate) { toDate = it } },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(toDate, fontSize = 11.5.sp)
                            }
                        }
                    }

                    // Section 3: Price Per Can Selector (Default 30, with 25, 35, 40 options)
                    item {
                        Text(
                            text = if (isHindi) "केन दर (Price per Can):" else "Rate per Can:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(25.0, 30.0, 35.0, 40.0).forEach { r ->
                                FilterChip(
                                    selected = selectedRate == r,
                                    onClick = { selectedRate = r },
                                    label = { Text("₹${r.toInt()}${if (r == 30.0) " (Default)" else ""}", fontSize = 11.5.sp) }
                                )
                            }
                        }
                    }

                    // Section 4: Water Cans Quantities (Delivered / Returned)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = RaiSuccessGreen.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, RaiSuccessGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(if (isHindi) "केन दिया (Delivered)" else "Delivered", fontSize = 11.sp, color = RaiSuccessGreen, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (totalDeliveredCans > 0) totalDeliveredCans-- },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            text = "$totalDeliveredCans",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { totalDeliveredCans++ },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = RaiWarningAmber.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, RaiWarningAmber.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(if (isHindi) "केन लिया (Returned)" else "Returned", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (totalReturnedCans > 0) totalReturnedCans-- },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            text = "$totalReturnedCans",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { totalReturnedCans++ },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 5: Extra Products Slot
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "अतिरिक्त उत्पाद (Extra Products):" else "Extra Products:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { showAddProductDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isHindi) "+ सामान जोड़ें" else "+ Add Item", fontSize = 11.5.sp)
                            }
                        }
                    }

                    items(extraProducts) { item ->
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    Text("${item.quantity} ${item.unit} x ₹${item.unitPrice.toInt()} = ₹${item.totalPrice.toInt()}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(
                                    onClick = { extraProducts.remove(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Section 6: Bill Total (Strictly excludes previous dues!)
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = RaiGold.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, RaiGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(if (isHindi) "कुल देय राशि (Bill Total)" else "Total Bill Amount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = if (isHindi) "*(पिछला बकाया शामिल नहीं है)" else "*(Excludes past balance)",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF7A6520)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        OutlinedTextField(
                                            value = manualTotalText,
                                            onValueChange = {
                                                manualTotalText = it
                                                isManualTotalOverride = true
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Bottom Action Buttons (Even, clean 2-row layout)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button 1: Preview Bill
                        OutlinedButton(
                            onClick = { showPreviewModal = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_preview_bill_editor")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "पूर्वावलोकन" else "Preview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Button 2: Direct Save Bill to Database
                        Button(
                            onClick = {
                                val billRecord = BillRecord(
                                    id = "BILL-$serialNumber-${System.currentTimeMillis() % 10000}",
                                    billNumber = serialNumber,
                                    billingDate = billingDate,
                                    periodStartDate = fromDate,
                                    periodEndDate = toDate,
                                    customerId = customer.id,
                                    customerName = customer.nameHindi,
                                    customerPhone = customer.phone,
                                    customerAddress = customer.address,
                                    totalCansDelivered = totalDeliveredCans,
                                    totalCansReturned = totalReturnedCans,
                                    ratePerCan = selectedRate,
                                    extraProductsSummary = extraProducts.joinToString(", ") { "${it.name} (${it.quantity})" },
                                    extraProductsTotal = extraCharges,
                                    waterChargesTotal = waterCharges,
                                    totalPayable = effectiveTotal,
                                    paidAmount = 0.0,
                                    balanceDue = effectiveTotal,
                                    isImported = false,
                                    paymentStatus = "PENDING"
                                )
                                viewModel.saveBill(billRecord)
                                Toast.makeText(context, if (isHindi) "बिल #$serialNumber सुरक्षित हो गया! 'पुराने बिल' में उपलब्ध है।" else "Bill #$serialNumber saved to database!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("btn_direct_save_bill")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaiGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "बिल सेव करें" else "Save Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Button 3: WhatsApp Share (Full width)
                    Button(
                        onClick = {
                            val customization = BillCustomization(
                                customBillNumber = serialNumber,
                                billingDate = billingDate,
                                periodStartDate = fromDate,
                                periodEndDate = toDate,
                                customRatePerCan = selectedRate,
                                customTotalCansDelivered = totalDeliveredCans,
                                customTotalCansReturned = totalReturnedCans,
                                additionalProducts = extraProducts.toList(),
                                customTotalPayable = effectiveTotal,
                                customNotes = "अवधि: $fromDate से $toDate"
                            )

                            val billRecord = BillRecord(
                                id = "BILL-$serialNumber-${System.currentTimeMillis() % 10000}",
                                billNumber = serialNumber,
                                billingDate = billingDate,
                                periodStartDate = fromDate,
                                periodEndDate = toDate,
                                customerId = customer.id,
                                customerName = customer.nameHindi,
                                customerPhone = customer.phone,
                                customerAddress = customer.address,
                                totalCansDelivered = totalDeliveredCans,
                                totalCansReturned = totalReturnedCans,
                                ratePerCan = selectedRate,
                                extraProductsSummary = extraProducts.joinToString(", ") { "${it.name} (${it.quantity})" },
                                extraProductsTotal = extraCharges,
                                waterChargesTotal = waterCharges,
                                totalPayable = effectiveTotal,
                                paidAmount = 0.0,
                                balanceDue = effectiveTotal,
                                isImported = false,
                                paymentStatus = "PENDING"
                            )
                            viewModel.saveBill(billRecord)

                            onConfirmAndSendWhatsApp(customization)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaiSuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_confirm_bill_whatsapp")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "WhatsApp बिल शेयर करें" else "Share Bill on WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPreviewModal) {
        val customization = BillCustomization(
            customBillNumber = serialNumber,
            billingDate = billingDate,
            periodStartDate = fromDate,
            periodEndDate = toDate,
            customRatePerCan = selectedRate,
            customTotalCansDelivered = totalDeliveredCans,
            customTotalCansReturned = totalReturnedCans,
            additionalProducts = extraProducts.toList(),
            customTotalPayable = effectiveTotal,
            customNotes = "अवधि: $fromDate से $toDate"
        )
        val billRecordForSave = BillRecord(
            id = "BILL-$serialNumber-${System.currentTimeMillis() % 10000}",
            billNumber = serialNumber,
            billingDate = billingDate,
            periodStartDate = fromDate,
            periodEndDate = toDate,
            customerId = customer.id,
            customerName = customer.nameHindi,
            customerPhone = customer.phone,
            customerAddress = customer.address,
            totalCansDelivered = totalDeliveredCans,
            totalCansReturned = totalReturnedCans,
            ratePerCan = selectedRate,
            extraProductsSummary = extraProducts.joinToString(", ") { "${it.name} (${it.quantity})" },
            extraProductsTotal = extraCharges,
            waterChargesTotal = waterCharges,
            totalPayable = effectiveTotal,
            isImported = false,
            paymentStatus = "PENDING"
        )

        BillPreviewDialog(
            billNumber = serialNumber,
            billingDate = billingDate,
            periodStart = fromDate,
            periodEnd = toDate,
            customer = customer,
            totalCansDelivered = totalDeliveredCans,
            totalCansReturned = totalReturnedCans,
            ratePerCan = selectedRate,
            extraProductsSummary = extraProducts.joinToString(", ") { "${it.name} (${it.quantity})" },
            extraProductsTotal = extraCharges,
            waterChargesTotal = waterCharges,
            totalPayable = effectiveTotal,
            businessSettings = viewModel.businessSettings.value,
            isHindi = isHindi,
            onDismiss = { showPreviewModal = false },
            onSaveBill = {
                viewModel.saveBill(billRecordForSave)
            },
            onShareWhatsApp = {
                viewModel.saveBill(billRecordForSave)
                onConfirmAndSendWhatsApp(customization)
            }
        )
    }

    // Add Extra Product Selection Dialog
    if (showAddProductDialog) {
        var selectedProduct by remember { mutableStateOf<ProductItem?>(activeProducts.firstOrNull()) }
        var quantityText by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text(if (isHindi) "अतिरिक्त उत्पाद जोड़ें" else "Add Extra Product", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(if (isHindi) "उत्पाद चुनें:" else "Select Product:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    activeProducts.forEach { prod ->
                        FilterChip(
                            selected = selectedProduct?.id == prod.id,
                            onClick = { selectedProduct = prod },
                            label = { Text("${if (isHindi) prod.nameHindi else prod.displayName} (₹${prod.unitPrice.toInt()})", fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(if (isHindi) "मात्रा (Quantity)" else "Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prod = selectedProduct
                        val qty = quantityText.toIntOrNull() ?: 1
                        if (prod != null) {
                            extraProducts.add(
                                BillProductLineItem(
                                    productId = prod.id,
                                    name = if (isHindi) prod.nameHindi else prod.nameEnglish,
                                    quantity = qty,
                                    unitPrice = prod.unitPrice,
                                    unit = prod.unit
                                )
                            )
                        }
                        showAddProductDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark)
                ) {
                    Text(if (isHindi) "जोड़ें" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
