package com.example.billing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.DeliveryEntry
import com.example.data.model.PaymentRecord
import com.example.data.model.BillProductLineItem
import com.example.data.model.BusinessSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BillCustomization(
    val customRatePerCan: Double = 30.0,
    val customTotalCansDelivered: Int = 1,
    val customTotalCansReturned: Int = 0,
    val additionalProducts: List<BillProductLineItem> = emptyList(),
    val customTotalPayable: Double? = null,
    val discount: Double = 0.0,
    val customBillNumber: String = "5156",
    val billingDate: String = "",
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val customNotes: String = ""
)

object PdfInvoiceGenerator {

    // Dimensions for 145 mm x 260 mm stationery at 72 points per inch:
    // Width: (145 / 25.4) * 72 = 411 points
    // Height: (260 / 25.4) * 72 = 737 points
    const val PAGE_WIDTH = 411
    const val PAGE_HEIGHT = 737

    fun generateBillPdf(
        context: Context,
        customer: Customer,
        deliveries: List<DeliveryEntry>,
        payments: List<PaymentRecord>,
        customization: BillCustomization = BillCustomization(),
        businessSettings: BusinessSettings = BusinessSettings()
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.rgb(11, 20, 38)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val margin = 14f

        // 1. Outer Border
        canvas.drawRoundRect(
            RectF(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin),
            6f,
            6f,
            borderPaint
        )

        // 2. Header Banner Background (Deep Navy)
        fillPaint.color = Color.rgb(11, 20, 38)
        canvas.drawRoundRect(
            RectF(margin + 2, margin + 2, PAGE_WIDTH - margin - 2, 85f),
            4f,
            4f,
            fillPaint
        )

        // Gold Accent line
        fillPaint.color = Color.rgb(212, 175, 55)
        canvas.drawRect(margin + 2, 85f, PAGE_WIDTH - margin - 2, 88f, fillPaint)

        // Draw app logo in header if available
        try {
            val logoResId = context.resources.getIdentifier("rai_logo", "drawable", context.packageName)
            if (logoResId != 0) {
                val origBitmap = BitmapFactory.decodeResource(context.resources, logoResId)
                if (origBitmap != null) {
                    val scaledLogo = Bitmap.createScaledBitmap(origBitmap, 52, 52, true)
                    canvas.drawBitmap(scaledLogo, margin + 10, margin + 10, paint)
                }
            }
        } catch (_: Exception) {}

        // Business Title from Settings
        paint.color = Color.rgb(244, 222, 133)
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        canvas.drawText(businessSettings.businessNameHindi.ifEmpty { "राय चिल्ड वाटर सप्लायर" }, margin + 68, margin + 24, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${businessSettings.businessName.ifEmpty { "RAI CHILLED WATER SUPPLIER" }} • ${businessSettings.address.take(30)}", margin + 68, margin + 38, paint)

        paint.textSize = 8.5f
        paint.color = Color.rgb(200, 220, 240)
        canvas.drawText("मोबा.- ${businessSettings.phone} • ${businessSettings.tagline.take(32)}", margin + 68, margin + 52, paint)

        var y = 104f

        val billDateStr = customization.billingDate.ifBlank {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }
        val billNo = customization.customBillNumber

        // Header info: Serial No & Date
        paint.color = Color.rgb(11, 20, 38)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("डिलीवरी बिल / CASH MEMO", margin + 6, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        canvas.drawText("सीरियल नं / Bill No: $billNo", PAGE_WIDTH - margin - 140, y, paint)
        y += 14f
        canvas.drawText("दिनांक / Date: $billDateStr", PAGE_WIDTH - margin - 140, y, paint)

        // Customer Details Box (Matching the paper card format)
        y += 8f
        val custBoxTop = y
        val custBoxBottom = y + 68f
        fillPaint.color = Color.rgb(245, 248, 252)
        canvas.drawRoundRect(
            RectF(margin + 4, custBoxTop, PAGE_WIDTH - margin - 4, custBoxBottom),
            4f,
            4f,
            fillPaint
        )
        canvas.drawRoundRect(
            RectF(margin + 4, custBoxTop, PAGE_WIDTH - margin - 4, custBoxBottom),
            4f,
            4f,
            borderPaint
        )

        paint.color = Color.rgb(11, 20, 38)
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("नाम श्री (Customer): ${customer.nameHindi}", margin + 12, y + 16, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("पता (Address): ${customer.address.ifBlank { customer.locality }}", margin + 12, y + 32, paint)
        canvas.drawText("मोबाइल नं: ${customer.phone}", margin + 12, y + 46, paint)

        val periodText = if (customization.periodStartDate.isNotBlank() && customization.periodEndDate.isNotBlank()) {
            "माह / अवधि: ${customization.periodStartDate} से ${customization.periodEndDate}"
        } else {
            "माह: ${SimpleDateFormat("MMMM-yyyy", Locale.getDefault()).format(Date())}"
        }
        canvas.drawText(periodText, margin + 12, y + 60, paint)

        y = custBoxBottom + 12f

        // Table Header
        fillPaint.color = Color.rgb(11, 20, 38)
        canvas.drawRect(margin + 4, y, PAGE_WIDTH - margin - 4, y + 22f, fillPaint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("विवरण (Particulars)", margin + 10, y + 15, paint)
        canvas.drawText("मात्रा (Qty)", margin + 200, y + 15, paint)
        canvas.drawText("दर (Rate)", margin + 270, y + 15, paint)
        canvas.drawText("रकम (Amount)", margin + 325, y + 15, paint)

        y += 22f

        // Row 1: Water Cans
        val waterCansDelivered = customization.customTotalCansDelivered
        val rate = customization.customRatePerCan
        val waterAmount = waterCansDelivered * rate

        paint.color = Color.rgb(11, 20, 38)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f

        fun drawLineItem(name: String, qty: String, rateStr: String, amtStr: String) {
            canvas.drawLine(margin + 4, y, PAGE_WIDTH - margin - 4, y, borderPaint)
            canvas.drawText(name, margin + 10, y + 15, paint)
            canvas.drawText(qty, margin + 205, y + 15, paint)
            canvas.drawText(rateStr, margin + 275, y + 15, paint)
            canvas.drawText(amtStr, margin + 330, y + 15, paint)
            y += 20f
        }

        drawLineItem("20L चिल्ड वाटर केन", "$waterCansDelivered", "₹${rate.toInt()}", "₹${waterAmount.toInt()}")

        // Extra Products
        var extraProductsTotal = 0.0
        customization.additionalProducts.forEach { item ->
            val itemTot = item.quantity * item.unitPrice
            extraProductsTotal += itemTot
            drawLineItem(item.name, "${item.quantity} ${item.unit}", "₹${item.unitPrice.toInt()}", "₹${itemTot.toInt()}")
        }

        // Bottle Movement Sub-box
        y += 8f
        fillPaint.color = Color.rgb(240, 245, 250)
        canvas.drawRoundRect(RectF(margin + 4, y, PAGE_WIDTH - margin - 4, y + 26f), 3f, 3f, fillPaint)
        paint.color = Color.rgb(11, 20, 38)
        paint.textSize = 9f
        canvas.drawText("केन दिया (Delivered): $waterCansDelivered  |  केन लिया (Returned): ${customization.customTotalCansReturned}  |  शुद्ध अंतर: ${waterCansDelivered - customization.customTotalCansReturned}", margin + 10, y + 16, paint)
        y += 34f

        // Bill Total Box (Strictly excluding previous dues!)
        val calculatedTotal = waterAmount + extraProductsTotal - customization.discount
        val finalPayable = customization.customTotalPayable ?: calculatedTotal

        fillPaint.color = Color.rgb(244, 222, 133)
        canvas.drawRoundRect(
            RectF(PAGE_WIDTH - margin - 180, y, PAGE_WIDTH - margin - 4, y + 36f),
            4f,
            4f,
            fillPaint
        )
        paint.color = Color.rgb(11, 20, 38)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("कुल बिल राशि (Total):", PAGE_WIDTH - margin - 172, y + 16, paint)
        paint.textSize = 13f
        canvas.drawText("₹${finalPayable.toInt()}", PAGE_WIDTH - margin - 172, y + 30, paint)

        y += 50f

        // Terms & Signatures
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(100, 110, 120)
        canvas.drawText(businessSettings.billTermsAndConditions.ifEmpty { "नोट: खाली केन अमानत है, गुम होने पर ₹250 प्रति केन देय होगा।" }, margin + 8, y, paint)
        y += 12f
        canvas.drawText("UPI ID: ${businessSettings.upiId} • बैंक: ${businessSettings.bankName} • A/C: ${businessSettings.accountNumber} • IFSC: ${businessSettings.ifscCode}", margin + 8, y, paint)
        y += 12f
        canvas.drawText("${businessSettings.tagline} • धन्यवाद!", margin + 8, y, paint)

        // Signature Lines
        y = PAGE_HEIGHT - margin - 40f
        paint.color = Color.rgb(11, 20, 38)
        paint.textSize = 9f
        canvas.drawLine(margin + 10, y, margin + 110, y, borderPaint)
        canvas.drawText("ग्राहक के हस्ताक्षर", margin + 16, y + 14, paint)

        canvas.drawLine(PAGE_WIDTH - margin - 120, y, PAGE_WIDTH - margin - 10, y, borderPaint)
        canvas.drawText("कृते: ${businessSettings.businessNameHindi.ifEmpty { "राय चिल्ड वाटर" }}", PAGE_WIDTH - margin - 110, y + 14, paint)

        pdfDocument.finishPage(page)

        // Write to cache
        return try {
            val fileName = "BILL_${billNo}_${customer.nameEnglish.replace(" ", "_")}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun shareBillViaWhatsApp(
        context: Context,
        pdfFile: File,
        customer: Customer,
        customAmount: Double? = null
    ) {
        val amt = customAmount ?: customer.currentFinancialBalance
        shareBillToWhatsApp(context, pdfFile, customer.phone, customer.nameHindi, amt)
    }

    fun shareBillToWhatsApp(
        context: Context,
        pdfFile: File,
        customerPhone: String,
        customerName: String,
        amount: Double
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val cleanPhone = customerPhone.filter { it.isDigit() }
            val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

            val caption = "नमस्ते $customerName जी, राय चिल्ड वाटर सप्लायर पिपरिया से आपका बिल संलग्न है। कुल राशि: ₹${amount.toInt()}। धन्यवाद!"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                if (formattedPhone.isNotBlank()) {
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }

            // Fallback to any app if WhatsApp isn't installed
            val chooser = Intent.createChooser(intent, "Share Bill via WhatsApp")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp शेयर करने में असमर्थ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
