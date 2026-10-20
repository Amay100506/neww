package com.priosort.app.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.priosort.app.domain.NotificationRecord

object DataExporter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serializes notification records into a formatted JSON string.
     * Enforces redaction if requested.
     */
    fun exportToJson(notifications: List<NotificationRecord>, enforceRedaction: Boolean = true): String {
        val sanitized = if (enforceRedaction) {
            notifications.map { record ->
                if (record.otpDetected) {
                    record.copy(body = TextRedactor.redactOtp(record.body))
                } else {
                    record
                }
            }
        } else {
            notifications
        }
        return gson.toJson(sanitized)
    }

    /**
     * Parses a JSON string into a list of NotificationRecord domain objects.
     */
    fun importFromJson(jsonString: String): List<NotificationRecord> {
        val type = object : TypeToken<List<NotificationRecord>>() {}.type
        return gson.fromJson(jsonString, type) ?: emptyList()
    }
}
