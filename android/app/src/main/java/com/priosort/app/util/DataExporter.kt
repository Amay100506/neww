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
                fun redact(value: String?): String? = value?.let(TextRedactor::redactSensitiveText)

                record.copy(
                    title = redact(record.title),
                    body = redact(record.body),
                    normalizedText = redact(record.normalizedText).orEmpty(),
                    actionDescription = redact(record.actionDescription),
                    deadlineText = redact(record.deadlineText),
                    priorityOverrideReason = redact(record.priorityOverrideReason),
                    explanationJson = redact(record.explanationJson).orEmpty(),
                )
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
