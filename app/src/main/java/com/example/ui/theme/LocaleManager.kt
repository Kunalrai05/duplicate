package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


enum class AppLanguage {
    HINDI,
    ENGLISH
}

class LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.HINDI)

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
    }

    fun isHindi(): Boolean = currentLanguage == AppLanguage.HINDI
}

val LocalAppLanguage = compositionLocalOf { LanguageManager() }

object AppStrings {
    // Top Bar & Modes
    fun appTitle(isHindi: Boolean) = if (isHindi) "राय चिल्ड वाटर" else "RAI CHILLED WATER"
    fun appSubtitle(isHindi: Boolean) = if (isHindi) "पिपरिया (म.प्र.)" else "Pipariya (M.P.)"
    fun driverMode(isHindi: Boolean) = if (isHindi) "ड्राइवर मोड" else "Driver Mode"
    fun ownerMode(isHindi: Boolean) = if (isHindi) "मालिक / Admin" else "Owner / Admin"

    // Navigation Labels
    fun navRouteSheet(isHindi: Boolean) = if (isHindi) "रूट शीट" else "Route Sheet"
    fun navRegisterScan(isHindi: Boolean) = if (isHindi) "स्कैन शीट" else "Scan Sheet"
    fun navSummary(isHindi: Boolean) = if (isHindi) "व्यापार" else "Summary"
    fun navLedgers(isHindi: Boolean) = if (isHindi) "खाता व बिल" else "Ledgers"
    fun navReminders(isHindi: Boolean) = if (isHindi) "रिमाइंडर" else "Reminders"
    fun navCorrections(isHindi: Boolean) = if (isHindi) "एंट्री सुधार" else "Corrections"
    fun navSymbols(isHindi: Boolean) = if (isHindi) "प्रतीक/चिन्ह" else "Symbols"
    fun navProducts(isHindi: Boolean) = if (isHindi) "उत्पाद / दरें" else "Products"
    fun navBillsHistory(isHindi: Boolean) = if (isHindi) "पुराने बिल" else "Bills History"
    fun navSync(isHindi: Boolean) = if (isHindi) "सिंक इंजन" else "Sync Engine"
    fun navAbout(isHindi: Boolean) = if (isHindi) "गाइड/About" else "About/Guide"

    // Route Sheet Actions
    fun giveCan(isHindi: Boolean) = if (isHindi) "+1 केन दिया" else "+1 Can Given"
    fun receiveCan(isHindi: Boolean) = if (isHindi) "-1 केन लिया" else "-1 Can Return"
    fun customEntry(isHindi: Boolean) = if (isHindi) "कस्टम / 2+1" else "Custom / 2+1"
    fun canBalance(isHindi: Boolean) = if (isHindi) "केन शेष" else "Can Balance"
    fun todayGiven(isHindi: Boolean) = if (isHindi) "आज दिया" else "Today Given"
    fun todayReceived(isHindi: Boolean) = if (isHindi) "आज लिया" else "Today Taken"
    fun searchCustomer(isHindi: Boolean) = if (isHindi) "ग्राहक, दुकान या फोन खोजें..." else "Search customer, shop or phone..."

    // Ledgers & Bills
    fun generateBill(isHindi: Boolean) = if (isHindi) "बिल बनाएं व WhatsApp भेजें" else "Create & Share Bill"
    fun collectPayment(isHindi: Boolean) = if (isHindi) "रकम जमा करें" else "Collect Payment"
    fun editProfile(isHindi: Boolean) = if (isHindi) "प्रोफाइल बदलें" else "Edit Profile"
    fun sendReminder(isHindi: Boolean) = if (isHindi) "रिमाइंडर भेजें" else "Send Reminder"
    fun monthlySheet(isHindi: Boolean) = if (isHindi) "मासिक खाता शीट (Excel Grid)" else "Monthly Sheet (Excel Grid)"
    fun dueAmount(isHindi: Boolean) = if (isHindi) "बकाया राशि" else "Due Amount"
    fun ratePerCan(isHindi: Boolean) = if (isHindi) "दर ₹/केन" else "Rate ₹/Can"

    // Monthly Excel Sheet
    fun dateHeader(isHindi: Boolean) = if (isHindi) "दिनांक" else "Date"
    fun canOutHeader(isHindi: Boolean) = if (isHindi) "केन दिया (Out)" else "Can Delivered (Out)"
    fun canInHeader(isHindi: Boolean) = if (isHindi) "केन लिया (In)" else "Can Returned (In)"
    fun dailyNet(isHindi: Boolean) = if (isHindi) "दैनिक अंतर" else "Daily Net"
    fun cumulativeBalance(isHindi: Boolean) = if (isHindi) "संचयी शेष" else "Balance"
    fun notationHeader(isHindi: Boolean) = if (isHindi) "प्रतीक" else "Symbol"

    // Scan & Extraction
    fun scanHeaderTitle(isHindi: Boolean) = if (isHindi) "रजिस्टर स्कैन व प्रविष्टि (Sheet Scanner)" else "Register Scan & Entry"
    fun scanSubtitle(isHindi: Boolean) = if (isHindi) "कागजी रजिस्टर की फोटो खींचें • प्रविष्टियां स्वतः दर्ज होंगी" else "Capture sheet photo • Entries auto-extracted"
    fun takePhoto(isHindi: Boolean) = if (isHindi) "कैमरा से फोटो खींचें" else "Take Camera Photo"
    fun pickGallery(isHindi: Boolean) = if (isHindi) "गैलरी से चुनें" else "Choose from Gallery"
    fun loadSampleSheet(isHindi: Boolean) = if (isHindi) "सैंपल शीट लोड करें" else "Load Sample Sheet"
    fun autoExtractEntries(isHindi: Boolean) = if (isHindi) "स्वचालित प्रविष्टियां निकालें (Auto-Extract)" else "Auto-Extract Entries"
    fun selectDateCalendar(isHindi: Boolean) = if (isHindi) "कैलेंडर से दिनांक चुनें" else "Select Date from Calendar"
    fun selectCustomer(isHindi: Boolean) = if (isHindi) "ग्राहक चुनें" else "Select Customer"

    // Excel & Actions
    fun exportExcel(isHindi: Boolean) = if (isHindi) "एक्सेल/CSV निर्यात (Export)" else "Export Excel/CSV"
    fun importExcel(isHindi: Boolean) = if (isHindi) "एक्सेल/CSV आयात (Import)" else "Import Excel/CSV"
    fun addCustomer(isHindi: Boolean) = if (isHindi) "नया ग्राहक जोड़ें" else "Add New Customer"
    fun importBillPhoto(isHindi: Boolean) = if (isHindi) "बिल फोटो आयात करें (Import Bill)" else "Import Bill Photo"
    fun saveAndSync(isHindi: Boolean) = if (isHindi) "सत्यापित करें व सहेजें (OK)" else "Verify & Save (OK)"

    // Settings & Theme
    fun navSettings(isHindi: Boolean) = if (isHindi) "सेटिंग्स व प्रोफाइल" else "Settings & Profile"
    fun settingsTitle(isHindi: Boolean) = if (isHindi) "व्यापार सेटिंग्स व प्रोफाइल" else "Business Settings & Profile"
    fun themeSettings(isHindi: Boolean) = if (isHindi) "डार्क एवं लाइट मोड (Appearance)" else "Dark & Light Mode"
    fun lightMode(isHindi: Boolean) = if (isHindi) "लाइट मोड (Light)" else "Light Mode"
    fun darkMode(isHindi: Boolean) = if (isHindi) "डार्क मोड (Dark)" else "Dark Mode"
    fun systemMode(isHindi: Boolean) = if (isHindi) "सिस्टम अनुसार (System)" else "System Default"
    fun messageTemplates(isHindi: Boolean) = if (isHindi) "पेमेंट रिमाइंडर संदेश टोन (Message Tones)" else "Payment Reminder Message Tones"
    fun bankDetails(isHindi: Boolean) = if (isHindi) "बैंक व UPI विवरण" else "Bank & UPI Details"
    fun businessDetails(isHindi: Boolean) = if (isHindi) "फर्म विवरण, पता व टैगलाइन" else "Business Info, Address & Tagline"
}

