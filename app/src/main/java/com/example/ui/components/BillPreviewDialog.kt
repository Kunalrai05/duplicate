package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billing.BillCustomization
import com.example.billing.PdfInvoiceGenerator
import com.example.data.model.BillRecord
import com.example.data.model.BusinessSettings
import com.example.data.model.Customer
import com.example.ui.theme.BorderLight
import com.example.ui.theme.RaiGold
import com.example.ui.theme.RaiNavyDark
import com.example.ui.theme.RaiSuccessGreen
import com.example.ui.theme.SurfaceLight

@Composable
fun BillPreviewDialog(
    billNumber: String,
    billingDate: String,
    periodStart: String,
    periodEnd: String,
    customer: Customer,
    totalCansDelivered: Int,
    totalCansReturned: Int,
    ratePerCan: Double,
    extraProductsSummary: String = "",
    extraProductsTotal: Double = 0.0,
    waterChargesTotal: Double = totalCansDelivered * ratePerCan,
    totalPayable: Double = waterChargesTotal + extraProductsTotal,
    businessSettings: BusinessSettings,
    isHindi: Boolean = true,
    onDismiss: () -> Unit,
    onSaveBill: (() -> Unit)? = null,
    onShareWhatsApp: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = RaiNavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RaiGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "बिल पूर्वावलोकन (Bill Preview • No. $billNumber)" else "Bill Preview • No. $billNumber",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Invoice Document (14.5 x 26 cm aspect ratio simulation)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, RaiNavyDark.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Religious Invocations
                                Text(
                                    text = "|| श्री गणेशाय नमः || ॐ नमः शिवाय ||",
                                    fontSize = 10.5.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Business Name
                                Text(
                                    text = businessSettings.businessNameHindi.ifBlank { businessSettings.businessName },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RaiNavyDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = businessSettings.tagline,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${businessSettings.address} • मो: ${businessSettings.phone}",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (businessSettings.gstOrRegNumber.isNotBlank()) {
                                    Text(
                                        text = "GST / Reg: ${businessSettings.gstOrRegNumber}",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = RaiNavyDark, thickness = 1.2.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Bill Meta Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "सीरियल नं (Bill No): $billNumber", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark)
                                        Text(text = "दिनांक (Date): $billingDate", fontSize = 11.sp, color = Color.DarkGray)
                                        Text(text = "बिल अवधि: $periodStart से $periodEnd", fontSize = 10.5.sp, color = Color.DarkGray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "M/s: ${customer.nameHindi}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark)
                                        Text(text = customer.locality, fontSize = 10.5.sp, color = Color.DarkGray)
                                        Text(text = "फोन: ${customer.phone}", fontSize = 10.5.sp, color = Color.DarkGray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Table Header
                                Surface(color = Color(0xFFEDF2F7), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("विवरण (Particulars)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark, modifier = Modifier.weight(2f))
                                        Text("मात्रा (Qty)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                        Text("दर (Rate)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                        Text("योग (Amount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                    }
                                }

                                // Main Item: Chilled Water Cans
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("चिल्ड वाटर केन (20L)", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
                                    Text("$totalCansDelivered", fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    Text("₹${ratePerCan.toInt()}", fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    Text("₹${waterChargesTotal.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                }

                                // Extra items if any
                                if (extraProductsSummary.isNotBlank() && extraProductsTotal > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(extraProductsSummary, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
                                        Text("-", fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                        Text("-", fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                        Text("₹${extraProductsTotal.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                    }
                                }

                                // Can Return Note
                                Text(
                                    text = "कुल वापसी केन (Empty Cans Returned): $totalCansReturned",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color.LightGray)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Total Payable Row
                                Surface(
                                    color = RaiNavyDark.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("कुल देय राशि (Total Payable):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaiNavyDark)
                                        Text("₹${totalPayable.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = RaiSuccessGreen)
                                    }
                                }

                                Text(
                                    text = "*(नोट: इस बिल में केवल वर्तमान माह/अवधि का शुल्क शामिल है, पिछला बकाया अलग है।)",
                                    fontSize = 9.5.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Bank Details & Terms Box
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("बैंक व भुगतान विवरण (Bank & UPI Details):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark)
                                        Text("बैंक: ${businessSettings.bankName} • खाता धारक: ${businessSettings.accountHolderName}", fontSize = 10.sp, color = Color.DarkGray)
                                        Text("खाता नं: ${businessSettings.accountNumber} • IFSC: ${businessSettings.ifscCode}", fontSize = 10.sp, color = Color.DarkGray)
                                        if (businessSettings.upiId.isNotBlank()) {
                                            Text("UPI ID: ${businessSettings.upiId} (${businessSettings.upiQrNote})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RaiNavyDark)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("नियम: ${businessSettings.billTermsAndConditions}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons: Separate Share & Close
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(if (isHindi) "बंद करें" else "Close")
                        }

                        if (onSaveBill != null) {
                            Button(
                                onClick = {
                                    onSaveBill()
                                    Toast.makeText(context, if (isHindi) "बिल सुरक्षित हो गया! पुराने बिल में दिखेगा।" else "Bill saved! Available in Bills History.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RaiNavyDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("btn_preview_save_bill")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaiGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "बिल सेव करें" else "Save Bill")
                            }
                        }

                        Button(
                            onClick = {
                                onShareWhatsApp()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaiSuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(44.dp)
                                .testTag("btn_preview_share_whatsapp")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "WhatsApp शेयर" else "Share WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
