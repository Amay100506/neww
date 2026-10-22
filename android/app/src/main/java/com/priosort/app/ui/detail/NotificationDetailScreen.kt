package com.priosort.app.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.NotificationFeedbackType
import com.priosort.app.domain.CustomRule
import com.priosort.app.ui.components.CategoryBadge
import com.priosort.app.ui.components.PriorityBadge
import com.priosort.app.ui.theme.PriorityCriticalColor
import com.priosort.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notificationId: Long,
    viewModel: NotificationDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var suggestedRule by remember { mutableStateOf<CustomRule?>(null) }
    var showReminderConfirmation by remember { mutableStateOf(false) }
    var pendingReminder by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    val context = LocalContext.current
    val reminderPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingReminder
        pendingReminder = null
        if (granted && pending != null) {
            viewModel.scheduleReminder(pending.first, pending.second)
            Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
        } else if (!granted) {
            Toast.makeText(context, "Allow PrioSort notifications in Android Settings to receive reminders", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(notificationId) {
        suggestedRule = null
        showReminderConfirmation = false
        pendingReminder = null
        viewModel.loadNotification(notificationId)
    }

    val record by viewModel.notification.collectAsState()
    val feedbackSent by viewModel.feedbackSent.collectAsState()
    val appImportance by viewModel.appImportance.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    record?.let { r ->
                        IconButton(onClick = { viewModel.togglePin() }) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin",
                                tint = if (r.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteNotification(onBackClick) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.onSurface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val notif = record
        if (notif == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading notification details...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Main Content Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = notif.applicationName ?: notif.packageName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            PriorityBadge(level = notif.priorityLevel)
                        }

                        Text(
                            text = notif.packageName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!notif.title.isNullOrBlank()) {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (!notif.body.isNullOrBlank()) {
                            Text(
                                text = notif.body,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadge(category = notif.category)
                            Text(
                                text = "Received: ${TimeUtils.formatRelativeTime(notif.receivedAt)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!notif.isActiveOnSystem) {
                            Text("Dismissed from Android notifications · kept in your PrioSort inbox", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (notif.isBurst) {
                            Text("Similar notification burst: ${notif.burstCount} alerts from this app within 3 minutes. Extra PrioSort sound and light alerts are suppressed.", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Formula Breakdown Card (S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 Scoring Model Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Formula: S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ScoreComponentBar("Urgency (U)", notif.urgencyScore, "30%")
                        ScoreComponentBar("Action (A)", notif.actionScore, "25%")
                        ScoreComponentBar("Deadline (D)", notif.deadlineScore, "25%")
                        ScoreComponentBar("User Importance (I)", notif.importanceScore, "15%")
                        ScoreComponentBar("Recency Decay (R)", notif.recencyScore, "5%")

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Calculated Base Score:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${notif.basePriorityScore} / 100", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Explainability Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🧠 Explainability Trace",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (notif.priorityOverrideReason != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "⚡ Rule Override: ${notif.priorityOverrideReason}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• Category classified as: ${notif.category.displayName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (notif.otpDetected) {
                            Text(text = "• Genuine OTP / verification code confirmed", fontSize = 12.sp, color = PriorityCriticalColor, fontWeight = FontWeight.Bold)
                        }
                        if (notif.actionDetected) {
                            Text(text = "• Action required: ${notif.actionDescription ?: "User response"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        if (notif.deadlineText != null && notif.deadlineResolutionStatus != "ABSENT") {
                            val deadlineDescription = if (notif.deadlineResolutionStatus == "RESOLVED" && notif.deadlineAt != null) {
                                val deadlineDate = java.time.Instant.ofEpochMilli(notif.deadlineAt)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm", java.util.Locale.getDefault()))
                                "Resolved deadline: $deadlineDate (${notif.deadlineText})"
                            } else {
                                "Deadline signal: ${notif.deadlineText} · ${notif.deadlineResolutionStatus.lowercase().replace('_', ' ')}"
                            }
                            Text(text = "• $deadlineDescription", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (notif.actionDetected || (notif.deadlineText != null && notif.deadlineResolutionStatus !in setOf("ABSENT", "NONE"))) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { showReminderConfirmation = true }) {
                                Text("Set a reminder")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Feedback & Correction Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✏️ User Correction Controls",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Correct the category or priority. PrioSort reorders immediately.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Feedback", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(NotificationFeedbackType.TOO_HIGH, NotificationFeedbackType.TOO_LOW).forEach { type ->
                                FilterChip(selected = feedbackSent == type, onClick = { viewModel.sendFeedback(type); suggestedRule = viewModel.suggestedRule(type) }, label = { Text(type.label) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(NotificationFeedbackType.WRONG_CATEGORY, NotificationFeedbackType.WRONG_RULE).forEach { type ->
                                FilterChip(selected = feedbackSent == type, onClick = { viewModel.sendFeedback(type); suggestedRule = viewModel.suggestedRule(type) }, label = { Text(type.label) })
                            }
                        }
                        feedbackSent?.let { Text("Feedback saved: ${it.label}", color = MaterialTheme.colorScheme.primary) }
                        if (feedbackSent != null) {
                            TextButton(onClick = { suggestedRule = viewModel.suggestedRule(feedbackSent!!) }) { Text("Review a suggested rule") }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        androidx.compose.foundation.layout.Box {
                            TextButton(onClick = { categoryMenuExpanded = true }) { Text("Category: ${notif.category.displayName} · Change") }
                            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                                NotificationCategory.entries.filter { it != NotificationCategory.UNKNOWN }.forEach { category ->
                                    DropdownMenuItem(text = { Text(category.displayName) }, onClick = {
                                        categoryMenuExpanded = false
                                        viewModel.updateCategory(category)
                                    })
                                }
                            }
                        }
                        Text("Change Priority Level:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PriorityLevel.entries.forEach { level ->
                                val isSelected = notif.priorityLevel == level
                                OutlinedButton(
                                    onClick = { viewModel.updatePriority(level) },
                                    modifier = Modifier.weight(1f),
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.onSurface) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(
                                        text = level.code,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Default importance for ${notif.applicationName ?: "this app"} · ${appImportance.toInt()}/100", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Affects future notifications from this app.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = appImportance, onValueChange = viewModel::updateAppImportance, onValueChangeFinished = viewModel::saveAppImportance, valueRange = 0f..100f)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.excludeApplication()
                                onBackClick()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exclude App from Capture", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
    suggestedRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { suggestedRule = null },
            title = { Text("Save suggested priority rule?") },
            text = { Text("This will apply ${rule.assignedPriority?.displayName ?: rule.assignedCategory?.displayName} to future notifications from ${record?.applicationName ?: rule.keywordPattern}. The rule stays off the notification history and can be deleted in Settings.") },
            confirmButton = { TextButton(onClick = { viewModel.saveSuggestedRule(rule); suggestedRule = null }) { Text("Save rule") } },
            dismissButton = { TextButton(onClick = { suggestedRule = null }) { Text("Cancel") } }
        )
    }
    if (showReminderConfirmation) {
        val current = record
        val now = System.currentTimeMillis()
        val suggestedAt = current?.deadlineAt?.takeIf { it > now }?.let { (it - 30 * 60 * 1000L).coerceAtLeast(now + 60_000L) }
            ?: (now + 60 * 60 * 1000L)
        val suggestedLabel = java.time.Instant.ofEpochMilli(suggestedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm", java.util.Locale.getDefault()))
        AlertDialog(
            onDismissRequest = { showReminderConfirmation = false },
            title = { Text("Confirm reminder") },
            text = {
                Text("PrioSort will remind you about this alert from ${current?.applicationName ?: "this app"} around $suggestedLabel. Android may defer reminders to save battery. The reminder will not include the notification message.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showReminderConfirmation = false
                    if (Build.VERSION.SDK_INT >= 33 &&
                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingReminder = current?.id?.let { it to suggestedAt }
                        reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        current?.let { viewModel.scheduleReminder(it.id, suggestedAt) }
                        Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Schedule") }
            },
            dismissButton = { TextButton(onClick = { showReminderConfirmation = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ScoreComponentBar(label: String, value: Float, weight: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$label ($weight):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value.toInt()}/100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline
        )
    }
}
