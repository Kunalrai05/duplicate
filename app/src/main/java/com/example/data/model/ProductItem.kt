package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products")
data class ProductItem(
    @PrimaryKey
    val id: String,
    val nameEnglish: String = "",
    val nameHindi: String = "",
    val defaultPrice: Double = 30.0,
    val unitPrice: Double = 30.0,
    val unit: String = "Can",
    val isWaterCan: Boolean = false,
    val isActive: Boolean = true
) : Serializable {
    val displayName: String
        get() = if (nameHindi.isNotBlank() && nameEnglish.isNotBlank()) {
            "$nameHindi ($nameEnglish)"
        } else {
            nameHindi.ifEmpty { nameEnglish }
        }
}

data class BillProductLineItem(
    val productId: String,
    val name: String,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val unit: String = "Can"
) : Serializable {
    val totalPrice: Double
        get() = quantity * unitPrice
}

object DefaultProducts {
    val catalog = listOf(
        ProductItem("PROD-1", "20L Water Can", "20L चिल्ड वाटर केन", 30.0, 30.0, "Can", true, true),
        ProductItem("PROD-2", "Water Camper 20L", "वाटर कैम्पर / जार 20L", 250.0, 250.0, "Pcs", false, true),
        ProductItem("PROD-3", "1L Bottles Box (12 Pcs)", "1L बोतल पेटी (12 बोतल)", 140.0, 140.0, "Box", false, true),
        ProductItem("PROD-4", "Ice Block / Cube", "बर्फ सिल्ली / क्यूब", 50.0, 50.0, "Kg", false, true),
        ProductItem("PROD-5", "Dispenser Stand & Tap", "डिस्पेंसर स्टैंड व नल", 120.0, 120.0, "Pcs", false, true),
        ProductItem("PROD-6", "Can Seal / Cap", "अतिरिक्त केन सील / कैप", 10.0, 10.0, "Pcs", false, true)
    )
}
