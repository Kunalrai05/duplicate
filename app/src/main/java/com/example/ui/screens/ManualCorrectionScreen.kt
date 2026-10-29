package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryEntry
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiSuccessGreen
import com.example.ui.theme.RaiWarningAmber
import com.example.ui.theme.RaiWarningAmberContainer
import com.example.ui.theme.SurfaceLight
import com.example.viewmodel.MainViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.viewmodel.ActiveScreen

@Composable
fun ManualCorrectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDeliveries by viewModel.allDeliveries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var entryToEdit by remember { mutableStateOf<DeliveryEntry?>(null) }

    val filteredList = allDeliveries.filter { entry ->
        searchQuery.isBlank() ||
                entry.customerName.contains(searchQuery, ignoreCase = true) ||
                entry.route.contains(searchQuery, ignoreCase = true) ||
                entry.date.contains(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Back navigation
        BackHandler {
            viewModel.navigateTo(ActiveScreen.BUSINESS_SUMMARY)
        }

        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(ActiveScreen.BUSINESS_SUMMARY) },
                        modifier = Modifier.size(36.dp).testTag("btn_back_from_corrections")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Summary",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "डिलीवरी एंट्री सुधार (Manual Entry Correction)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ड्राइवर की गलत गिनती या विवाद पर सीधे संख्या सुधारें • खाता स्वतः अपडेट होगा",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ग्राहक का नाम, मार्ग या दिनांक खोजें...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = RaiNavyDark)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("correction_search_field")
                )
            }
        }

        // Deliveries List for Correction
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "कोई डिलीवरी रिकॉर्ड नहीं मिला",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { entry ->
                    CorrectionEntryCard(
                        entry = entry,
                        onEditClick = { entryToEdit = entry }
                    )
                }
            }
        }
    }

    // Direct Edit Entry Dialog
    entryToEdit?.let { entry ->
        EditEntryCorrectionDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onSave = { newGiven, newReceived, reason ->
                viewModel.correctDeliveryEntry(
                    entryId = entry.id,
                    newGiven = newGiven,
                    newReceived = newReceived,
                    reason = reason
                )
                Toast.makeText(context, "एंट्री संशोधित की गई! खाता बैलेंस पुनः परिकलित हुआ।", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CorrectionEntryCard(
    entry: DeliveryEntry,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, if (entry.isCorrected) RaiWarningAmber else BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("correction_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = RaiNavyDark
                    )
                    Text(
                        text = "दिनांक: ${entry.date} (${entry.time}) • ${entry.route}",
                        fontSize = 11.5.sp,
                        color = Color.Gray
                    )
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RaiNavyDark),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_edit_entry_${entry.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = RaiNavyDark, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("संशोधन करें", fontSize = 11.sp, color = RaiNavyDark, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantities Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F8F4),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("दिया गया केन (Given)", fontSize = 10.5.sp, color = RaiSuccessGreen)
                        Text("${entry.cansGiven} जार", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = RaiSuccessGreen)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF7ED),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("वापस लिया (Returned)", fontSize = 10.5.sp, color = RaiWarningAmber)
                        Text("${entry.cansReceived} खाली जार", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = RaiWarningAmber)
                    }
                }
            }

            // Audit Correction Tag
            if (entry.isCorrected) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RaiWarningAmberContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = RaiWarningAmber, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "संशोधित ऑडिट: ${entry.correctionReason ?: "संख्या सुधारी गई"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RaiWarningAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditEntryCorrectionDialog(
    entry: DeliveryEntry,
    onDismiss: () -> Unit,
    onSave: (newGiven: Int, newReceived: Int, reason: String) -> Unit
) {
    var newGiven by remember { mutableIntStateOf(entry.cansGiven) }
    var newReceived by remember { mutableIntStateOf(entry.cansReceived) }
    var reason by remember { mutableStateOf("ड्राइवर गिनती भूल / मिसकाउंट सुधार") }

    val presetReasons = listOf(
        "ड्राइवर से गिनती भूल हुई",
        "ग्राहक ने बाद में 1 केन लौटाया",
        "गलत ग्राहक कार्ड पर टैप हुआ था",
        "स्पेशल डिस्काउंट / कॉम्प्लिमेंटरी"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("डिलीवरी सुधारें (Edit Count)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(entry.customerName, fontSize = 13.sp, color = RaiNavyDark)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "मूल प्रविष्टि: दिया ${entry.cansGiven} | लिया ${entry.cansReceived}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // New Cans Given
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("सही दिया गया केन:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaiSuccessGreen)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (newGiven > 0) newGiven-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus")
                        }
                        Text(
                            text = "$newGiven",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(onClick = { newGiven++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // New Cans Received
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("सही वापस लिया केन:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaiWarningAmber)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (newReceived > 0) newReceived-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus")
                        }
                        Text(
                            text = "$newReceived",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(onClick = { newReceived++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("सुधार का कारण (Reason):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                presetReasons.take(2).forEach { r ->
                    FilterChip(
                        selected = reason == r,
                        onClick = { reason = r },
                        label = { Text(r, fontSize = 11.sp) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("विशिष्ट कारण लिखें") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(newGiven, newReceived, reason)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark),
                modifier = Modifier.testTag("btn_confirm_edit_entry")
            ) {
                Text("सुधार सुरक्षित करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
}
