package com.priosort.app.intelligence

import com.priosort.app.data.local.DuplicateNotificationContent

/** Counts redundant copies under the same exact-content, same-app, three-minute rule used by Inbox. */
object ExactDuplicateAnalyzer {
    private const val WINDOW_MS = 3 * 60 * 1000L

    fun redundantCopyCount(records: List<DuplicateNotificationContent>): Int {
        val groups = records.asSequence()
            .filter { !it.title.isNullOrBlank() && !it.body.isNullOrBlank() }
            .groupBy { listOf(it.packageName, normalize(it.title!!), normalize(it.body!!)).joinToString("\u0000") }
            .values
        return groups.sumOf { group ->
            var redundant = 0
            var cluster = mutableListOf<DuplicateNotificationContent>()
            group.sortedByDescending { it.receivedAt }.forEach { item ->
                if (cluster.isNotEmpty() && cluster.first().receivedAt - item.receivedAt > WINDOW_MS) {
                    redundant += (cluster.size - 1).coerceAtLeast(0)
                    cluster = mutableListOf()
                }
                cluster += item
            }
            redundant + (cluster.size - 1).coerceAtLeast(0)
        }
    }

    fun normalize(value: String): String = value.trim().lowercase()
        .replace(Regex("\\s+"), " ")
        .trimEnd('.', '!', '?', ':', ';')
}
