package com.example.tripmate.model

import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val flagEmoji: String
)

/**
 * Metadata stored when an expense is entered in a foreign transaction currency.
 * Encoded cleanly into the `notes` column of `ExpenseRow` so it is 100% backward compatible
 * with existing Supabase tables without schema errors.
 */
data class CurrencyMeta(
    val originalCurrency: String,
    val originalAmount: Double,
    val exchangeRate: Double, // Value of 1 foreign unit in primary trip currency (INR)
    val userNotes: String? = null
) {
    companion object {
        private const val PREFIX = "[FX:"
        private const val SUFFIX = "]"

        /**
         * Encodes currency metadata and optional user notes into a single notes string.
         * Format: `[FX:EUR:45.00:89.50] Note text`
         */
        fun encode(
            originalCurrency: String,
            originalAmount: Double,
            exchangeRate: Double,
            userNotes: String? = null
        ): String {
            val tag = String.format(
                Locale.US,
                "%s%s:%.2f:%.4f%s",
                PREFIX,
                originalCurrency.uppercase().trim(),
                originalAmount,
                exchangeRate,
                SUFFIX
            )
            return if (!userNotes.isNullOrBlank()) {
                "$tag ${userNotes.trim()}"
            } else {
                tag
            }
        }

        /**
         * Decodes currency metadata from notes.
         * Returns null if notes does not contain foreign currency metadata.
         */
        fun decode(rawNotes: String?): CurrencyMeta? {
            if (rawNotes.isNullOrBlank() || !rawNotes.startsWith(PREFIX)) return null
            val closingIdx = rawNotes.indexOf(SUFFIX)
            if (closingIdx == -1) return null

            val tagContent = rawNotes.substring(PREFIX.length, closingIdx)
            val parts = tagContent.split(":")
            if (parts.size < 3) return null

            val code = parts[0].trim().uppercase()
            val origAmount = parts[1].toDoubleOrNull() ?: return null
            val rate = parts[2].toDoubleOrNull() ?: return null

            val remainingNotes = rawNotes.substring(closingIdx + SUFFIX.length).trim().takeIf { it.isNotEmpty() }

            return CurrencyMeta(
                originalCurrency = code,
                originalAmount = origAmount,
                exchangeRate = rate,
                userNotes = remainingNotes
            )
        }
    }
}

/**
 * Curated list of standard world currencies commonly used by international travelers.
 */
val SUPPORTED_CURRENCIES = listOf(
    CurrencyInfo("INR", "₹", "Indian Rupee", "🇮🇳"),
    CurrencyInfo("USD", "$", "US Dollar", "🇺🇸"),
    CurrencyInfo("EUR", "€", "Euro", "🇪🇺"),
    CurrencyInfo("GBP", "£", "British Pound", "🇬🇧"),
    CurrencyInfo("JPY", "¥", "Japanese Yen", "🇯🇵"),
    CurrencyInfo("AED", "AED", "UAE Dirham", "🇦🇪"),
    CurrencyInfo("THB", "฿", "Thai Baht", "🇹🇭"),
    CurrencyInfo("SGD", "S$", "Singapore Dollar", "🇸🇬"),
    CurrencyInfo("AUD", "A$", "Australian Dollar", "🇦🇺"),
    CurrencyInfo("CAD", "C$", "Canadian Dollar", "🇨🇦"),
    CurrencyInfo("CHF", "CHF", "Swiss Franc", "🇨🇭"),
    CurrencyInfo("MYR", "RM", "Malaysian Ringgit", "🇲🇾"),
    CurrencyInfo("IDR", "Rp", "Indonesian Rupiah", "🇮🇩"),
    CurrencyInfo("VND", "₫", "Vietnamese Dong", "🇻🇳"),
    CurrencyInfo("KRW", "₩", "South Korean Won", "🇰🇷"),
    CurrencyInfo("TRY", "₺", "Turkish Lira", "🇹🇷"),
    CurrencyInfo("SAR", "SAR", "Saudi Riyal", "🇸🇦"),
    CurrencyInfo("QAR", "QAR", "Qatari Riyal", "🇶🇦"),
    CurrencyInfo("NPR", "NPR", "Nepalese Rupee", "🇳🇵"),
    CurrencyInfo("LKR", "LKR", "Sri Lankan Rupee", "🇱🇰"),
    CurrencyInfo("CNY", "¥", "Chinese Yuan", "🇨🇳")
)

fun getCurrencyInfo(code: String): CurrencyInfo {
    return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
        ?: CurrencyInfo(code.uppercase(), code.uppercase(), code.uppercase(), "🌐")
}
