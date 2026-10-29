package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class MessageTone {
    SHORT,
    HUMBLE,
    SWEET,
    DIRECT,
    PROFESSIONAL,
    POLITE,
    FIRM,
    URGENT
}

@Entity(tableName = "business_settings")
data class BusinessSettings(
    @PrimaryKey
    val id: String = "primary_settings",

    // 1. Business Info Presets
    val firmName: String = "Rai Chilled Water Services",
    val firmNameHindi: String = "राय चिल्ड वॉटर सप्लायर्स",
    val businessName: String = "Rai Chilled Water Services",
    val businessNameHindi: String = "राय चिल्ड वॉटर सप्लायर्स",
    val tagline: String = "शुद्ध एवं शीतल जल सेवा | Daily Chilled Water Supply",
    val address: String = "स्टेशन रोड, पिपरिया, जिला नर्मदापुरम (म.प्र.) 461775",
    val phone: String = "9826012345",
    val alternatePhone: String = "9425012345",
    val gstOrRegNumber: String = "MP-PIP-2026-CW",
    val billTermsAndConditions: String = "कृपया खाली केन सुरक्षित रखें व समय पर वापस करें।",
    val defaultCanRate: Double = 30.0,

    // 2. Bank & Payment Details
    val bankName: String = "State Bank of India (SBI)",
    val accountHolderName: String = "Rai Chilled Water Supplier",
    val accountNumber: String = "398712345678",
    val ifscCode: String = "SBIN0001234",
    val upiId: String = "raichilledwater@upi",
    val upiQrNote: String = "Google Pay / PhonePe / Paytm / BHIM UPI",

    // 3. Message Tone Templates (All editable in settings)
    val defaultTone: String = "HUMBLE", // "SHORT", "HUMBLE", "SWEET", "DIRECT", "PROFESSIONAL"

    val templateShort: String = """नमस्ते {customer_name} जी, राय चिल्ड वाटर का बिल बकाया ₹{balance} है ({cans} केन)। कृपया UPI: {upi_id} पर भुगतान करें। धन्यवाद!""",

    val templateHumble: String = """आदरणीय {customer_name} जी, सादर प्रणाम।
राय चिल्ड वाटर सप्लायर (पिपरिया) की ओर से यह आपके बकाया बिल का स्मरण संदेश है:
• कुल बकाया राशि: ₹{balance}
• आपके पास केन शेष: {cans} जार
आपसे विनम्र निवेदन है कि समयानुसार भुगतान करने की कृपा करें।
UPI: {upi_id} ({bank_name})
सधन्यवाद,
{business_name}""",

    val templateSweet: String = """प्यारे {customer_name} जी, आशा है आप और आपका परिवार सकुशल हैं! 💧
आपकी शीतल जल सेवा का इस माह का बिल ₹{balance} शेष है (केन: {cans} जार)।
जब भी आपको सुविधा हो, कृपया UPI: {upi_id} पर भुगतान कर दें।
आपके निरंतर सहयोग के लिए बहुत-बहुत धन्यवाद!
शुभकामनाएं,
{business_name}""",

    val templateDirect: String = """सूचना: {customer_name} जी,
राय चिल्ड वाटर बिल विवरण:
• कुल देय राशि: ₹{balance}
• खाली केन बकाया: {cans}
कृपया आज ही UPI {upi_id} या बैंक A/C {account_no} पर भुगतान सुनिश्चित करें।
- {business_name}""",

    val templateProfessional: String = """प्रिय ग्राहक {customer_name},
राय चिल्ड वाटर सप्लायर के खाता विवरण अनुसार आपका अंतिम बकाया निम्नलिखित है:
• देय राशि: ₹{balance}
• बकाया केन: {cans}
बैंक विवरण:
• बैंक: {bank_name}
• खाता धारक: {account_holder}
• खाता सं.: {account_no}
• IFSC: {ifsc}
• UPI ID: {upi_id}
कृपया भुगतान पश्चात रसीद या स्क्रीनशॉट साझा करें।
भवदीय,
{business_name}
फोन: {phone}""",

    // 4. Appearance
    val themeMode: String = "SYSTEM" // "LIGHT", "DARK", "SYSTEM"
) {
    fun formatMessage(tone: MessageTone, customerName: String, balance: String, cans: String): String {
        val rawTemplate = when (tone) {
            MessageTone.SHORT -> templateShort
            MessageTone.HUMBLE, MessageTone.POLITE -> templateHumble
            MessageTone.SWEET -> templateSweet
            MessageTone.DIRECT, MessageTone.FIRM, MessageTone.URGENT -> templateDirect
            MessageTone.PROFESSIONAL -> templateProfessional
        }
        return rawTemplate
            .replace("{customer_name}", customerName)
            .replace("{balance}", balance)
            .replace("{cans}", cans)
            .replace("{upi_id}", upiId)
            .replace("{bank_name}", bankName)
            .replace("{account_holder}", accountHolderName)
            .replace("{account_no}", accountNumber)
            .replace("{ifsc}", ifscCode)
            .replace("{business_name}", businessNameHindi.ifEmpty { businessName })
            .replace("{address}", address)
            .replace("{phone}", phone)
    }
}
