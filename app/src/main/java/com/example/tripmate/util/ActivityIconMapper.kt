package com.example.tripmate.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

object ActivityIconMapper {
    fun iconFor(title: String): ImageVector {
        val t = title.lowercase()
        return when {
            listOf("breakfast", "lunch", "dinner", "cafe", "food", "biryani").any { it in t } -> Icons.Filled.Restaurant
            listOf("fort", "palace", "museum", "temple", "monument").any { it in t } -> Icons.Filled.Castle
            listOf("shop", "market", "bazaar").any { it in t } -> Icons.Filled.LocalMall
            else -> Icons.Filled.Explore
        }
    }
}
