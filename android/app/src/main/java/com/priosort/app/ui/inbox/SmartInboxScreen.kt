package com.priosort.app.ui.inbox

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priosort.app.domain.FocusModeType
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.ui.components.NotificationCard
import com.priosort.app.ui.components.PriorityFilterRow
import com.priosort.app.ui.theme.PriorityCriticalColor
import com.priosort.app.ui.theme.PriorityHighColor
import com.priosort.app.ui.theme.PriorityLowColor
import com.priosort.app.ui.theme.PriorityMediumColor
import com.priosort.app.ui.theme.PriorityMinimalColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInboxScreen(
    viewModel: SmartInboxViewModel,
    onNotificationClick: (NotificationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSandboxDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PrioSort Smart Inbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (state.isGroupedView) "View: Priority Sections" else "View: Ranked Priority Stream",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleGroupedView() }) {
                        Icon(
                            imageVector = if (state.isGroupedView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle Grouped View",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showSandboxDialog = true },
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Create, contentDescription = "Test Sandbox")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Test Sandbox", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FloatingActionButton(
                    onClick = { viewModel.simulateExecutiveSummaryScenario() },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AddAlert, contentDescription = "Simulate Stream")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Simulate Stream", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Permission Banner if notification access not granted
            if (!state.isAccessGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PriorityCriticalColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notification Listener Access is required to prioritize notifications.",
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityCriticalColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Summary Counters: Critical & High Priority
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Critical Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PriorityCriticalColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${state.criticalCount}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Critical (P0)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF991B1B))
                            Text("Action required", fontSize = 10.sp, color = Color(0xFF7F1D1D))
                        }
                    }
                }

                // High Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PriorityHighColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${state.highCount}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("High (P1)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF9A3412))
                            Text("Important alerts", fontSize = 10.sp, color = Color(0xFF7C2D12))
                        }
                    }
                }
            }

            // Focus Mode Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Focus Mode:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                FocusModeType.entries.forEach { mode ->
                    val isSelected = state.activeFocusMode == mode
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            .clickable { viewModel.setFocusMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search by title, body, or application...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Priority Filter Chips
            PriorityFilterRow(
                selectedPriority = state.selectedPriority,
                onSelectPriority = { viewModel.onPriorityFilterSelected(it) }
            )

            // Dynamic Priority Reordered Notification List
            if (state.notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notifications found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Tap 'Simulate Stream' or 'Test Sandbox' below to test.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else if (state.isGroupedView) {
                // Grouped by Priority Tier Sections
                val grouped = state.notifications.groupBy { it.priorityLevel }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PriorityLevel.entries.forEach { level ->
                        val itemsForLevel = grouped[level]
                        if (!itemsForLevel.isNullOrEmpty()) {
                            item {
                                SectionHeader(level, itemsForLevel.size)
                            }
                            items(itemsForLevel, key = { it.id }) { notif ->
                                NotificationCard(
                                    notification = notif,
                                    onClick = { onNotificationClick(notif) },
                                    onSnooze = { untilMs -> viewModel.snoozeNotification(notif, untilMs) },
                                    onArchive = { viewModel.archiveNotification(notif) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Pure Dynamic Ranked Stream
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification) },
                            onSnooze = { untilMs -> viewModel.snoozeNotification(notification, untilMs) },
                            onArchive = { viewModel.archiveNotification(notification) }
                        )
                    }
                }
            }
        }

        // Live Test Sandbox Modal Dialog
        if (showSandboxDialog) {
            TestSandboxDialog(
                onDismiss = { showSandboxDialog = false },
                onInject = { title, body, appName, pkg ->
                    viewModel.injectCustomNotification(title, body, appName, pkg)
                    showSandboxDialog = false
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(level: PriorityLevel, count: Int) {
    val (color, title) = when (level) {
        PriorityLevel.CRITICAL -> PriorityCriticalColor to "🔴 CRITICAL (P0) • IMMEDIATE ATTENTION"
        PriorityLevel.HIGH -> PriorityHighColor to "🟠 HIGH PRIORITY (P1)"
        PriorityLevel.MEDIUM -> PriorityMediumColor to "🔵 MEDIUM PRIORITY (P2)"
        PriorityLevel.LOW -> PriorityLowColor to "🟢 LOW PRIORITY (P3)"
        PriorityLevel.MINIMAL -> PriorityMinimalColor to "⚪ MINIMAL & PROMOTIONAL (P4)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = "$count items", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TestSandboxDialog(
    onDismiss: () -> Unit,
    onInject: (title: String, body: String, appName: String, pkg: String) -> Unit
) {
    var title by remember { mutableStateOf("HDFC Bank Alert") }
    var body by remember { mutableStateOf("Your OTP is 482731 for NetBanking transaction. Do not share.") }
    var appName by remember { mutableStateOf("HDFC Bank") }
    var pkg by remember { mutableStateOf("com.snapwork.hdfc") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Interactive Notification Sandbox", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Pick a quick preset:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {
                            title = "SBI Verification OTP"
                            body = "Your SBI login OTP is 912834. Do not disclose to anyone."
                            appName = "State Bank of India"
                            pkg = "com.sbi.lotusintouch"
                        },
                        label = { Text("OTP (P0)", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            title = "URGENT: Fraud Alert"
                            body = "Unauthorized transaction of Rs 45,000 detected on card ending 8821."
                            appName = "HDFC Bank"
                            pkg = "com.snapwork.hdfc"
                        },
                        label = { Text("Fraud (P0)", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            title = "Project Deadline Due Today"
                            body = "CS501 Term Report submission deadline is due today at 11:59 PM."
                            appName = "Canvas Student"
                            pkg = "com.instructure.cndv"
                        },
                        label = { Text("Deadline (P1)", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            title = "Flipkart Flash Sale"
                            body = "Flat 60% off on all sneakers! Limited time offer, shop now!"
                            appName = "Flipkart"
                            pkg = "com.flipkart.android"
                        },
                        label = { Text("Sale Ad (P4)", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Notification Body") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Application Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInject(title, body, appName, pkg) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text("Prioritize & Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
