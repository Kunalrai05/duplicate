package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.ui.theme.RaiGold
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiSuccessGreen
import com.example.ui.theme.RaiWaterBlue
import com.example.util.TransliterationHelper
import java.util.Locale

@Composable
fun CustomerProfileDetailDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSaveProfile: (Customer) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }

    var nameHindi by remember { mutableStateOf(customer.nameHindi) }
    var nameEnglish by remember { mutableStateOf(customer.nameEnglish) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(customer.address) }
    var locality by remember { mutableStateOf(customer.locality) }
    var ratePerCan by remember { mutableDoubleStateOf(customer.ratePerCan) }
    var currentCanBalance by remember { mutableIntStateOf(customer.currentCanBalance) }
    var currentDueText by remember { mutableStateOf(customer.currentFinancialBalance.toInt().toString()) }
    var isActive by remember { mutableStateOf(customer.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = RaiNavyDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditMode) "प्रोफाइल संपादित करें" else "ग्राहक विवरण",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RaiNavyDark
                    )
                }

                if (!isEditMode) {
                    Button(
                        onClick = { isEditMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark),
                        modifier = Modifier.testTag("btn_toggle_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("बदलें", fontSize = 11.sp)
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEditMode) {
                    // View Mode Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(customer.nameHindi, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RaiNavyDark)
                                Text(customer.nameEnglish, fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("मोबाइल: ${customer.phone}", fontSize = 13.sp)
                                Text("रूट/क्षेत्र: ${customer.locality}", fontSize = 13.sp)
                                Text("पता: ${customer.address}", fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F1FC),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("दर प्रति केन", fontSize = 10.5.sp, color = RaiWaterBlue)
                                            Text("₹${customer.ratePerCan.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F1FC),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("केन शेष (जार)", fontSize = 10.5.sp, color = RaiWaterBlue)
                                            Text("${customer.currentCanBalance}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEECEB),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("कुल बकाया ₹", fontSize = 10.5.sp, color = Color(0xFFDC2626))
                                            Text("₹${customer.currentFinancialBalance.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Edit Mode Form
                    item {
                        OutlinedTextField(
                            value = nameHindi,
                            onValueChange = {
                                nameHindi = it
                                if (it.isNotBlank()) {
                                    nameEnglish = TransliterationHelper.hindiToEnglish(it)
                                }
                            },
                            label = { Text("ग्राहक का नाम (हिंदी) • स्वतः अनुवाद") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_cust_name_hindi")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = nameEnglish,
                            onValueChange = {
                                nameEnglish = it
                                if (it.isNotBlank()) {
                                    nameHindi = TransliterationHelper.englishToHindi(it)
                                }
                            },
                            label = { Text("Name (English)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_cust_name_english")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("मोबाइल नंबर (WhatsApp Bill Number)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_cust_phone")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("पता (Address / Landmark)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = locality,
                            onValueChange = { locality = it },
                            label = { Text("रूट / क्षेत्र (Locality)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("दर प्रति केन ₹ (Price per Can):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(25.0, 30.0, 35.0, 40.0).forEach { r ->
                                FilterChip(
                                    selected = ratePerCan == r,
                                    onClick = { ratePerCan = r },
                                    label = { Text("₹${r.toInt()}", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currentCanBalance.toString(),
                                onValueChange = { currentCanBalance = it.toIntOrNull() ?: 0 },
                                label = { Text("केन शेष (जार)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = currentDueText,
                                onValueChange = { currentDueText = it },
                                label = { Text("बकाया राशि ₹") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("सक्रिय ग्राहक स्थिति (Active Customer):", fontSize = 12.5.sp)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(
                    onClick = {
                        val finalHindi = nameHindi.trim().ifBlank { TransliterationHelper.englishToHindi(nameEnglish.trim()) }
                        val finalEnglish = nameEnglish.trim().ifBlank { TransliterationHelper.hindiToEnglish(nameHindi.trim()) }
                        val updated = customer.copy(
                            nameHindi = finalHindi,
                            nameEnglish = finalEnglish,
                            phone = phone.trim(),
                            address = address.trim(),
                            locality = locality.trim(),
                            ratePerCan = ratePerCan,
                            currentCanBalance = currentCanBalance,
                            currentFinancialBalance = currentDueText.toDoubleOrNull() ?: customer.currentFinancialBalance,
                            isActive = isActive,
                            lastUpdated = System.currentTimeMillis()
                        )
                        onSaveProfile(updated)
                        isEditMode = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark),
                    modifier = Modifier.testTag("btn_save_customer_profile")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("सहेजें (Save)")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("बंद करें")
                }
            }
        },
        dismissButton = {
            if (isEditMode) {
                TextButton(onClick = { isEditMode = false }) {
                    Text("रद्द करें")
                }
            }
        }
    )
}
