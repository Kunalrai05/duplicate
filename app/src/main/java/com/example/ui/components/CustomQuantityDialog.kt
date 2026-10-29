package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiSuccessGreen
import com.example.ui.theme.RaiWarningAmber

@Composable
fun CustomQuantityDialog(
    customer: Customer,
    symbols: List<com.example.data.model.NotationSymbol> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (given: Int, received: Int, notation: String, notes: String) -> Unit
) {
    var cansGiven by remember { mutableIntStateOf(1) }
    var cansReceived by remember { mutableIntStateOf(1) }
    var selectedNotation by remember { mutableStateOf("Normal") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "कस्टम एंट्री: ${customer.nameHindi}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RaiNavyDark
                )
                Text(
                    text = customer.locality,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Preset Quick Chips (from user-defined symbols or default)
                Text("प्रतीक / चिन्ह चयन (Select Notation):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (symbols.isNotEmpty()) {
                        symbols.forEach { sym ->
                            FilterChip(
                                selected = selectedNotation == sym.symbol,
                                onClick = {
                                    cansGiven = sym.defaultCansGiven
                                    cansReceived = sym.defaultCansReceived
                                    selectedNotation = sym.symbol
                                },
                                label = { Text("${sym.symbol} (${sym.labelHindi.take(8)})", fontSize = 11.5.sp) }
                            )
                        }
                    } else {
                        listOf("1", "2+1", "Bulk (थोक)", "Normal").forEach { note ->
                            FilterChip(
                                selected = selectedNotation == note,
                                onClick = {
                                    selectedNotation = note
                                    if (note == "2+1") { cansGiven = 2; cansReceived = 1 }
                                    else if (note == "Bulk (थोक)") { cansGiven = 5; cansReceived = 5 }
                                    else { cansGiven = 1; cansReceived = 1 }
                                },
                                label = { Text(note, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cans Given Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("केन दिया (Delivered):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaiSuccessGreen)
                        Text("दर: ₹${customer.ratePerCan.toInt()}/केन", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (cansGiven > 0) cansGiven-- },
                            modifier = Modifier.testTag("btn_dec_given")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus")
                        }
                        Text(
                            text = "$cansGiven",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { cansGiven++ },
                            modifier = Modifier.testTag("btn_inc_given")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cans Received Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("खाली केन लिया (Returned):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RaiWarningAmber)
                        Text("खाली बोतल वापसी", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (cansReceived > 0) cansReceived-- },
                            modifier = Modifier.testTag("btn_dec_received")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus")
                        }
                        Text(
                            text = "$cansReceived",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { cansReceived++ },
                            modifier = Modifier.testTag("btn_inc_received")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notation selector
                Text("नोटेशन (Notation):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                val notationList = if (symbols.isNotEmpty()) symbols.map { it.symbol } else listOf("Normal", "2+1", "Bulk", "Sample")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    notationList.take(4).forEach { noteItem ->
                        FilterChip(
                            selected = selectedNotation == noteItem,
                            onClick = { selectedNotation = noteItem },
                            label = { Text(noteItem, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("टिप्पणी / विवरण (Note)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(cansGiven, cansReceived, selectedNotation, notes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark),
                modifier = Modifier.testTag("btn_confirm_custom_entry")
            ) {
                Text("दर्ज करें (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
