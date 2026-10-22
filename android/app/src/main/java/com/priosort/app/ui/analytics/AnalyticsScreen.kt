package com.priosort.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.util.TimeUtils
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AnalyticsScreen(viewModel: AnalyticsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val distribution = listOf(
        PriorityLevel.CRITICAL to state.criticalCount,
        PriorityLevel.HIGH to state.highCount,
        PriorityLevel.MEDIUM to state.mediumCount,
        PriorityLevel.LOW to state.lowCount,
        PriorityLevel.MINIMAL to state.minimalCount
    )
    val dailyCounts = state.dailyNotificationCounts.associate { it.day to it.count }
    val activityTimeline = (6 downTo 0).map { daysAgo ->
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        date to (dailyCounts[date.toString()] ?: 0)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Analytics") }) }, modifier = modifier.fillMaxSize()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Your activity, on this device", style = MaterialTheme.typography.headlineSmall)
            Text("Counts reflect notifications currently saved in your inbox. Deleted or expired items are not included.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Captured", state.totalCount.toString(), Modifier.weight(1f))
                MetricCard("Verification alerts", state.otpCount.toString(), Modifier.weight(1f))
            }
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notification activity · last 7 days", style = MaterialTheme.typography.titleLarge)
                    ProvideChartStyle(m3ChartStyle()) {
                        Chart(
                            chart = columnChart(),
                            model = entryModelOf(*activityTimeline.map { it.second as Number }.toTypedArray()),
                            modifier = Modifier.fillMaxWidth().height(170.dp).semantics {
                                contentDescription = activityTimeline.joinToString(", ") { (date, count) -> "$date: $count notifications" }
                            }
                        )
                    }
                    activityTimeline.forEach { (date, count) ->
                        Text("${date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))} · $count", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Financial activity by currency", style = MaterialTheme.typography.titleLarge)
                    if (state.transactionTotals.isEmpty()) {
                        Text("No transaction amounts are available in the saved inbox yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.transactionTotals.forEach { total ->
                            val currency = total.transactionCurrency.takeIf { it != "UNKNOWN" } ?: "Unknown currency"
                            Text("$currency · ${total.transactionType.lowercase().replaceFirstChar(Char::uppercase)} · ${"%.2f".format(total.total)}")
                        }
                    }
                    Text("Searchable transaction ledger", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.transactionSearch,
                        onValueChange = viewModel::setTransactionSearch,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search merchant, app, type, amount, currency") }
                    )
                    if (state.recentTransactions.isEmpty()) {
                        Text(if (state.transactionSearch.isBlank()) "No transactions with parsed amounts are saved." else "No matching transactions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.recentTransactions.forEach { transaction ->
                            val currency = transaction.transactionCurrency?.takeIf { it != "UNKNOWN" } ?: "Unknown currency"
                            val merchant = transaction.transactionMerchant?.takeIf(String::isNotBlank)
                                ?: transaction.applicationName?.takeIf(String::isNotBlank)
                                ?: transaction.packageName
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(merchant, style = MaterialTheme.typography.bodyMedium)
                                Text("${transaction.transactionType?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Transaction"} · $currency ${transaction.transactionAmount?.let { "%.2f".format(it) } ?: "amount unavailable"} · ${TimeUtils.formatRelativeTime(transaction.receivedAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            MetricCard("User feedback", state.feedbackCount.toString(), Modifier.fillMaxWidth())
            MetricCard("Burst notifications", state.burstCount.toString(), Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Burst rate", "${if (state.totalCount == 0) 0 else state.burstCount * 100 / state.totalCount}%", Modifier.weight(1f))
                MetricCard("Manual override rate", "${if (state.totalCount == 0) 0 else state.manualOverrideCount * 100 / state.totalCount}%", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Exact duplicate copies", state.duplicateCount.toString(), Modifier.weight(1f))
                MetricCard("Exact duplicate rate", "${if (state.totalCount == 0) 0 else state.duplicateCount * 100 / state.totalCount}%", Modifier.weight(1f))
            }
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Priority distribution", style = MaterialTheme.typography.titleLarge)
                    if (state.totalCount == 0) {
                        Text("Your saved notification counts will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ProvideChartStyle(m3ChartStyle()) {
                            Chart(
                                chart = columnChart(),
                                model = entryModelOf(*distribution.map { it.second as Number }.toTypedArray()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .semantics {
                                        contentDescription = distribution.joinToString(", ") { (level, count) ->
                                            "${level.displayName}: $count"
                                        }
                                    }
                            )
                        }
                        distribution.forEach { (level, count) ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(level.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text("$count · ${((count * 100f) / state.totalCount).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                LinearProgressIndicator(
                                    progress = { (count.toFloat() / state.totalCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
