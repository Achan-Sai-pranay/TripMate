package com.example.tripmate.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tripmate.model.ExpenseCategory

object ActivityIconMapper {
    fun iconFor(title: String): ImageVector {
        val t = title.lowercase()
        return when {
            listOf("breakfast", "lunch", "dinner", "cafe", "food", "biryani", "restaurant").any { it in t } -> Icons.Filled.Restaurant
            listOf("fort", "palace", "museum", "temple", "monument").any { it in t } -> Icons.Filled.Castle
            listOf("shop", "market", "bazaar", "mall").any { it in t } -> Icons.Filled.LocalMall
            else -> Icons.Filled.Explore
        }
    }

    fun categoryFor(title: String): ExpenseCategory {
        val t = title.lowercase()
        return when {
            listOf("hotel", "resort", "stay", "check-in", "check in", "hostel", "homestay").any { it in t } -> ExpenseCategory.STAY
            listOf("breakfast", "lunch", "dinner", "cafe", "food", "restaurant", "biryani", "snack", "tea", "coffee").any { it in t } -> ExpenseCategory.FOOD
            listOf("taxi", "cab", "train", "flight", "metro", "bus", "drive", "ferry", "auto", "transfer").any { it in t } -> ExpenseCategory.TRANSPORT
            listOf("shop", "market", "bazaar", "mall", "souvenir").any { it in t } -> ExpenseCategory.SHOPPING
            listOf("fort", "palace", "museum", "temple", "monument", "beach", "safari", "trek", "park", "tour", "lake", "viewpoint").any { it in t } -> ExpenseCategory.ACTIVITIES
            else -> ExpenseCategory.ACTIVITIES
        }
    }
}
