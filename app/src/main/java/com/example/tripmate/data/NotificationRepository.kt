package com.example.tripmate.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NotificationType {
    TRIP_UPDATE,
    VOTING,
    EXPENSE,
    DOCUMENT,
    SYSTEM
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean = false,
    val tripId: String? = null
)

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                id = "notif-1",
                title = "Live Activity Voting",
                message = "Companions cast votes on upcoming itinerary stops. Top voted activities are highlighted in your group sheet.",
                timestamp = System.currentTimeMillis() - 15 * 60 * 1000L,
                type = NotificationType.VOTING
            ),
            AppNotification(
                id = "notif-2",
                title = "Travel Documents Synced",
                message = "Flight reservations and stay vouchers are securely stored for one-tap offline timeline access.",
                timestamp = System.currentTimeMillis() - 2 * 3600 * 1000L,
                type = NotificationType.DOCUMENT
            ),
            AppNotification(
                id = "notif-3",
                title = "Smart Route Optimization",
                message = "Day 1 itinerary was chronologically sequenced to minimize transit time between attractions.",
                timestamp = System.currentTimeMillis() - 24 * 3600 * 1000L,
                type = NotificationType.TRIP_UPDATE
            ),
            AppNotification(
                id = "notif-4",
                title = "Multi-Currency Expenses Ready",
                message = "Real-time foreign exchange rates are active for international trip splitting.",
                timestamp = System.currentTimeMillis() - 2 * 24 * 3600 * 1000L,
                type = NotificationType.EXPENSE
            )
        )
    )

    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    fun markAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun removeNotification(id: String) {
        _notifications.value = _notifications.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    fun addNotification(notification: AppNotification) {
        _notifications.value = listOf(notification) + _notifications.value
    }
}
