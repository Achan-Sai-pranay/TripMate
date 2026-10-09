package com.example.tripmate.model

import java.util.UUID

/**
 * Category of a stored document or reservation in TripMate (TripIt Standard).
 */
enum class DocumentCategory(val label: String) {
    FLIGHT("Flight / Boarding Pass"),
    HOTEL("Hotel / Lodging"),
    TRAIN("Train / Transit"),
    ACTIVITY_PASS("Activity / Museum Pass"),
    CAR_RENTAL("Rental Car"),
    RESTAURANT("Dining Reservation"),
    GENERAL("General Document")
}

/**
 * File type of the attached media.
 */
enum class DocumentFileType {
    PDF,
    IMAGE,
    NONE
}

/**
 * Represents an attached travel confirmation, booking pass, or document.
 */
data class TripDocument(
    val id: String = UUID.randomUUID().toString(),
    val tripId: String? = null,
    val title: String,
    val category: DocumentCategory = DocumentCategory.GENERAL,
    val confirmationNumber: String? = null,
    val provider: String? = null,              // e.g., "Delta Airlines", "Marriott Tokyo"
    val dayIndex: Int? = null,                 // null = Trip-level; 0..N-1 = Day 1, Day 2, etc.
    val dayLabel: String? = null,              // e.g., "Day 1 (Mon, Oct 12)"
    val linkedItemId: String? = null,          // Activity / stop ID if attached directly to an itinerary item
    val linkedItemTitle: String? = null,       // e.g., "Tokyo Skytree Observation Deck"
    val fileUri: String? = null,               // Absolute internal storage path or content URI
    val fileType: DocumentFileType = DocumentFileType.NONE,
    val fileName: String? = null,              // Original file name (e.g., "boarding_pass.pdf")
    val fileSizeBytes: Long? = null,
    val dateTimeLabel: String? = null,         // e.g., "Oct 12, 10:30 AM" or "Check-in: 3:00 PM"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
