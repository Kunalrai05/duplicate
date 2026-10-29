package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notation_symbols")
data class NotationSymbol(
    @PrimaryKey
    val symbol: String, // e.g. "1", "2", "2+1", "/", "X", "B", "S"
    val id: String = symbol,
    val labelHindi: String = "",
    val labelEnglish: String = "",
    val descriptionHindi: String = "",
    val descriptionEnglish: String = "",
    val defaultCansGiven: Int = 1,
    val defaultCansReceived: Int = 0,
    val defaultCansDelivered: Int = defaultCansGiven,
    val defaultCansReturned: Int = defaultCansReceived,
    val isSystemDefault: Boolean = true,
    val description: String = ""
) : Serializable
