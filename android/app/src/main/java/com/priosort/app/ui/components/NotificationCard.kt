package com.priosort.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.SnoozePreset
import com.priosort.app.intelligence.OtpDetector
import com.priosort.app.ui.theme.PriorityCriticalColor
import com.priosort.app.ui.theme.PriorityHighColor
import com.priosort.app.ui.theme.PriorityLowColor
import com.priosort.app.ui.theme.PriorityMediumColor
import com.priosort.app.ui.theme.PriorityMinimalColor
import com.priosort.app.util.TimeUtils

@Composable
fun NotificationCard(
    notification: NotificationRecord,
    onClick: () -> Unit,
    onSnooze: ((Long) -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showSnoozeMenu by remember { mutableStateOf(false) }

    val indicatorColor = when (notification.priorityLevel) {
        PriorityLevel.CRITICAL -> PriorityCriticalColor
        PriorityLevel.HIGH -> PriorityHighColor
        PriorityLevel.MEDIUM -> PriorityMediumColor
        PriorityLevel.LOW -> PriorityLowColor
        PriorityLevel.MINIMAL -> PriorityMinimalColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Priority stripe indicator on the left
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(if (notification.otpDetected || notification.deliveryStatus != null) 140.dp else 115.dp)
                    .background(indicatorColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header: App name, VIP badge, pin icon, time, priority badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (notification.applicationName?.firstOrNull() ?: 'A').toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = notification.applicationName ?: notification.packageName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )

                        if (notification.isVip) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("VIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                            }
                        }

                        if (notification.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = TimeUtils.formatRelativeTime(notification.receivedAt),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PriorityBadge(level = notification.priorityLevel)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                if (!notification.title.isNullOrBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Body preview
                if (!notification.body.isNullOrBlank()) {
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Delivery Status Tag
                if (notification.deliveryStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFF7ED))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(notification.deliveryStatus, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEA580C))
                    }
                }

                // Quick Action: One-Tap Copy OTP for Critical Verification notifications
                if (notification.otpDetected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val otpRes = OtpDetector.detectOtp(notification.normalizedText)
                    val code = otpRes.extractedCode ?: "CODE"
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Verification code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy OTP",
                            tint = PriorityCriticalColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy OTP ($code)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PriorityCriticalColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer chips: Category, Score, Snooze & Archive Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(category = notification.category)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (notification.transactionAmount != null) {
                            val currencyLabel = when (notification.transactionCurrency) {
                                "INR" -> "₹"
                                "USD" -> "\$"
                                "EUR" -> "€"
                                "GBP" -> "£"
                                else -> "Unknown currency "
                            }
                            Text(
                                text = "$currencyLabel${"%.2f".format(notification.transactionAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (notification.transactionType == "CREDIT") Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "Score: ${notification.basePriorityScore}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Action Icons: Snooze & Archive
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onSnooze != null) {
                            Box {
                                IconButton(
                                    onClick = { showSnoozeMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Snooze, contentDescription = "Snooze", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = showSnoozeMenu,
                                    onDismissRequest = { showSnoozeMenu = false }
                                ) {
                                    SnoozePreset.entries.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.displayName, fontSize = 12.sp) },
                                            onClick = {
                                                onSnooze(System.currentTimeMillis() + preset.durationMs)
                                                showSnoozeMenu = false
                                                Toast.makeText(context, "Snoozed for ${preset.displayName}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (onArchive != null) {
                            IconButton(
                                onClick = onArchive,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
