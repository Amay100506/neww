package com.priosort.app.ui.inbox

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.ExactDuplicateAnalyzer
import com.priosort.app.sorting.NotificationSorter
import com.priosort.app.ui.settings.NotificationAccessDisclosureDialog
import com.priosort.app.ui.theme.BrandPink
import com.priosort.app.ui.theme.BrandPinkSoft
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInboxScreen(
    viewModel: SmartInboxViewModel,
    onNotificationClick: (NotificationRecord) -> Unit,
    onSettingsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val inboxRows = remember(state.notifications) { buildInboxRows(state.notifications) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDisclosure by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshCaptureStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshCaptureStatus()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PrioSort", fontWeight = FontWeight.Bold)
                        Text("A calmer notification inbox", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Open settings")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CaptureStatusCard(
                    accessGranted = state.accessGranted,
                    connected = state.serviceConnected,
                    paused = state.isCapturePaused,
                    onEnable = {
                        if (state.disclosureAccepted) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        else showDisclosure = true
                    },
                    onOpenSettings = onSettingsClick
                )
            }
            item {
                OutlinedTextField(
                    value = state.searchText,
                    onValueChange = viewModel::setSearch,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search notifications") },
                    shape = RoundedCornerShape(18.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.selectedPriority == null, onClick = { viewModel.setPriorityFilter(null) }, label = { Text("All priorities") })
                    PriorityLevel.entries.forEach { level ->
                        FilterChip(selected = state.selectedPriority == level, onClick = { viewModel.setPriorityFilter(if (state.selectedPriority == level) null else level) }, label = { Text(level.displayName) })
                    }
                }
            }
            if (state.availableApps.isNotEmpty()) item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.selectedApp == null, onClick = { viewModel.setAppFilter(null) }, label = { Text("All apps") })
                    state.availableApps.forEach { (packageName, appName) ->
                        FilterChip(selected = state.selectedApp == packageName, onClick = { viewModel.setAppFilter(if (state.selectedApp == packageName) null else packageName) }, label = { Text(appName) })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.selectedCategory == null, onClick = { viewModel.setCategoryFilter(null) }, label = { Text("All categories") })
                    NotificationCategory.entries.filter { it != NotificationCategory.UNKNOWN }.forEach { category ->
                        FilterChip(selected = state.selectedCategory == category, onClick = { viewModel.setCategoryFilter(if (state.selectedCategory == category) null else category) }, label = { Text(category.displayName) })
                    }
                }
            }
            if (state.isLoading) {
                item {
                    Column(Modifier.fillMaxWidth().padding(42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Loading your inbox…", Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (state.notifications.isEmpty()) {
                item {
                    EmptyInbox(
                        hasPermission = state.accessGranted,
                        filtered = state.searchText.isNotBlank() || state.selectedPriority != null || state.selectedCategory != null || state.selectedApp != null,
                        onEnable = {
                            if (state.disclosureAccepted) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            else showDisclosure = true
                        }
                    )
                }
            } else {
                item {
                    Text("${state.notifications.size} notifications · ${inboxRows.count { it is InboxRow.DuplicateGroup }} duplicate groups", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(inboxRows, key = { it.key }) { row ->
                    when (row) {
                        is InboxRow.Single -> InboxNotificationCard(
                            item = row.notification,
                            onClick = {
                                viewModel.setRead(row.notification.id, true)
                                onNotificationClick(row.notification)
                            },
                            onRead = { viewModel.setRead(row.notification.id, !row.notification.isRead) },
                            onPin = { viewModel.togglePinned(row.notification) },
                            onSnooze = { viewModel.snooze(row.notification.id, it) }
                        )
                        is InboxRow.DuplicateGroup -> {
                            var expanded by remember(row.key) { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DuplicateGroupCard(row.notifications, expanded) { expanded = !expanded }
                                if (expanded) row.notifications.forEach { item ->
                                    InboxNotificationCard(
                                        item = item,
                                        onClick = {
                                            viewModel.setRead(item.id, true)
                                            onNotificationClick(item)
                                        },
                                        onRead = { viewModel.setRead(item.id, !item.isRead) },
                                        onPin = { viewModel.togglePinned(item) },
                                        onSnooze = { viewModel.snooze(item.id, it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDisclosure) {
        NotificationAccessDisclosureDialog(
            onDismiss = { showDisclosure = false },
            onReadPrivacy = onPrivacyClick,
            onAgree = {
                showDisclosure = false
                scope.launch {
                    viewModel.acceptNotificationDisclosure()
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        )
    }
}

private sealed interface InboxRow {
    val key: String
    data class Single(val notification: NotificationRecord) : InboxRow { override val key = "notification-${notification.id}" }
    data class DuplicateGroup(val notifications: List<NotificationRecord>) : InboxRow {
        override val key = "duplicates-${notifications.minOf { it.id }}"
    }
}

/** Collapse only exact same-app title/body repeats whose entire cluster fits inside the burst window. */
private fun buildInboxRows(notifications: List<NotificationRecord>): List<InboxRow> {
    val eligible = notifications.filter { !it.title.isNullOrBlank() && !it.body.isNullOrBlank() }
    val contentGroups = eligible.groupBy { item ->
        listOf(item.packageName, normalizeDuplicateText(item.title!!), normalizeDuplicateText(item.body!!)).joinToString("\u0000")
    }.values
    val duplicateGroups = contentGroups.flatMap { sameContent ->
        val sorted = sameContent.sortedByDescending { it.receivedAt }
        val groups = mutableListOf<List<NotificationRecord>>()
        var current = mutableListOf<NotificationRecord>()
        sorted.forEach { item ->
            if (current.isNotEmpty() && current.first().receivedAt - item.receivedAt > 3 * 60 * 1000L) {
                if (current.size > 1) groups += current.toList()
                current = mutableListOf()
            }
            current += item
        }
        if (current.size > 1) groups += current.toList()
        groups
    }
    val groupByMember = duplicateGroups.flatMap { group -> group.map { it.id to group } }.toMap()
    val emitted = mutableSetOf<Long>()
    return buildList {
        notifications.forEach { item ->
            if (item.id in emitted) return@forEach
            val group = groupByMember[item.id]
            if (group == null) {
                add(InboxRow.Single(item))
                emitted += item.id
            } else {
                add(InboxRow.DuplicateGroup(NotificationSorter.sortNotifications(group)))
                emitted += group.map { it.id }
            }
        }
    }
}

private fun normalizeDuplicateText(value: String): String =
    ExactDuplicateAnalyzer.normalize(value)

@Composable
private fun DuplicateGroupCard(notifications: List<NotificationRecord>, expanded: Boolean, onToggle: () -> Unit) {
    val representative = notifications.firstOrNull() ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${notifications.size} exact duplicate alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(representative.applicationName ?: representative.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(representative.title.orEmpty(), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Highest alert priority: ${representative.priorityLevel.displayName} · ${representative.category.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onToggle) { Text(if (expanded) "Hide duplicates" else "Show all ${notifications.size} alerts") }
        }
    }
}

@Composable
private fun CaptureStatusCard(accessGranted: Boolean, connected: Boolean, paused: Boolean, onEnable: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    when { paused -> "Capture is paused"; !accessGranted -> "Notification access is off"; connected -> "Live capture is on"; else -> "Reconnecting to notifications" },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    when { paused -> "Resume capture in Settings"; !accessGranted -> "Allow access to show notifications here"; connected -> "New notifications appear as they arrive"; else -> "Android may take a moment to reconnect" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!accessGranted || paused) TextButton(onClick = if (!accessGranted) onEnable else onOpenSettings) { Text(if (paused) "Settings" else "Enable") }
        }
    }
}

@Composable
private fun EmptyInbox(hasPermission: Boolean, filtered: Boolean, onEnable: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (!hasPermission) "Give PrioSort notification access" else if (filtered) "No matching notifications" else "Your inbox is clear", style = MaterialTheme.typography.titleLarge)
            Text(
                if (!hasPermission) "Android asks you to grant this permission. PrioSort needs it to read and sort alerts in this inbox."
                else if (filtered) "Try a different search or clear a filter."
                else "New alerts will appear here automatically. Your inbox is ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasPermission) TextButton(onClick = onEnable) { Text("Open notification access") }
        }
    }
}

@Composable
private fun InboxNotificationCard(
    item: NotificationRecord,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onPin: () -> Unit,
    onSnooze: (Long) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val levelColor = when (item.priorityLevel) {
        PriorityLevel.CRITICAL -> BrandPink
        PriorityLevel.HIGH -> BrandPinkSoft
        PriorityLevel.MEDIUM -> MaterialTheme.colorScheme.primary
        PriorityLevel.LOW -> MaterialTheme.colorScheme.secondary
        PriorityLevel.MINIMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.applicationName ?: item.packageName, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.receivedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions for ${item.applicationName ?: "notification"}") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text(if (item.isRead) "Mark unread" else "Mark read") }, onClick = { menuExpanded = false; onRead() })
                        DropdownMenuItem(text = { Text(if (item.isPinned) "Unpin" else "Pin") }, onClick = { menuExpanded = false; onPin() })
                        DropdownMenuItem(text = { Text("Snooze 1 hour") }, onClick = { menuExpanded = false; onSnooze(60 * 60 * 1000L) })
                        DropdownMenuItem(text = { Text("Snooze 1 day") }, onClick = { menuExpanded = false; onSnooze(24 * 60 * 60 * 1000L) })
                    }
                }
            }
            Text(item.title ?: item.body ?: "Notification", style = MaterialTheme.typography.titleMedium, fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!item.title.isNullOrBlank() && !item.body.isNullOrBlank()) {
                Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(colors = CardDefaults.cardColors(containerColor = levelColor.copy(alpha = 0.16f)), shape = RoundedCornerShape(50)) {
                    Text(item.priorityLevel.displayName, Modifier.padding(horizontal = 11.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = levelColor)
                }
                Text(item.category.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.isPinned) Text("Pinned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if (item.isBurst) Text("Burst · ${item.burstCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            val reason = item.priorityOverrideReason ?: item.explanationJson
            if (!reason.isNullOrBlank()) Text(reason.take(120), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
