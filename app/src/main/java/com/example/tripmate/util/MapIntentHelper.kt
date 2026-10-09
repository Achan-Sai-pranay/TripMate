package com.example.tripmate.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapIntentHelper {

    /**
     * Launches external map app with fallback to Google Maps web in browser.
     * Never fails silently.
     */
    fun launchMap(
        context: Context,
        query: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val encodedQuery = Uri.encode(query.trim())

        // 1. Try native geo URI if coordinates or query available
        val geoUri = if (latitude != null && longitude != null) {
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedQuery)")
        } else {
            Uri.parse("geo:0,0?q=$encodedQuery")
        }

        val nativeIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(nativeIntent)
            return
        } catch (_: Exception) {
            // Native geo handler failed or not installed. Fall through to web fallback.
        }

        // 2. Web browser fallback with standard Google Maps query
        val webUri = if (latitude != null && longitude != null) {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
        }

        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No app or browser available to open map", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches Google search or reservation query for places.
     */
    fun launchSearchOrReservation(context: Context, query: String) {
        val webUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No browser available to open search", Toast.LENGTH_SHORT).show()
        }
    }
}
