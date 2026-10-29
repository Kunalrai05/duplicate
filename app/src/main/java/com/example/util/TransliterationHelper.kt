package com.example.util

object TransliterationHelper {

    private val directWordMap = mapOf(
        "स्टेशन रोड" to "Station Road",
        "सदर बाजार" to "Sadar Bazar",
        "सब्जी मंडी" to "Subzi Mandi",
        "गांधी चौक" to "Gandhi Chowk",
        "बैंक रोड" to "Bank Road",
        "पचमढ़ी रोड" to "Pachmarhi Road",
        "शोभापुर रोड" to "Shobhapur Road",
        "आनंद" to "Anand",
        "रेस्टोरेंट" to "Restaurant",
        "भोजनालय" to "Bhojnalaya",
        "होटल" to "Hotel",
        "शर्मा" to "Sharma",
        "वर्मा" to "Verma",
        "राय" to "Rai",
        "गुप्ता" to "Gupta",
        "पटेल" to "Patel",
        "यादव" to "Yadav",
        "तिवारी" to "Tiwari",
        "सिंह" to "Singh",
        "डेयरी" to "Dairy",
        "मिष्ठान" to "Sweets",
        "दुकान" to "Shop",
        "किराना" to "Kirana",
        "जनरल स्टोर्स" to "General Stores"
    )

    fun toEnglish(hindiText: String): String {
        if (hindiText.isBlank()) return ""
        var text = hindiText
        for ((hi, en) in directWordMap) {
            text = text.replace(hi, en)
        }
        return if (text == hindiText) {
            transliterateHindiToEnglish(hindiText)
        } else {
            text
        }
    }

    private fun transliterateHindiToEnglish(input: String): String {
        val consonants = mapOf(
            'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
            'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "ny",
            'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
            'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
            'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
            'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v", 'श' to "sh",
            'ष' to "sh", 'स' to "s", 'ह' to "h"
        )
        val vowels = mapOf(
            'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
            'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
            'ा' to "a", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
            'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
            'ं' to "n", 'ः' to "h", '्' to ""
        )

        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                consonants.containsKey(c) -> {
                    sb.append(consonants[c])
                    if (i + 1 < input.length && vowels.containsKey(input[i + 1])) {
                        // Vowel sign follows
                    } else if (i + 1 < input.length && input[i + 1] == '्') {
                        // Halant follows, skip default 'a'
                    } else if (i + 1 < input.length && consonants.containsKey(input[i + 1])) {
                        sb.append("a")
                    } else if (i + 1 == input.length) {
                        // End of word
                    }
                }
                vowels.containsKey(c) -> {
                    sb.append(vowels[c])
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        return sb.toString().trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
