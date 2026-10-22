package com.priosort.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationAccessDisclosureDialog(onDismiss: () -> Unit, onReadPrivacy: () -> Unit, onAgree: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before enabling notification access") },
        text = { Text("PrioSort reads notifications from other apps so it can classify and display them in your inbox. Notification text is processed locally and stored on this device, subject to your retention and redaction settings. PrioSort does not send notification content or OTPs to a remote service. You can revoke access in Android settings or clear saved records at any time.") },
        confirmButton = { TextButton(onClick = onAgree) { Text("Agree and continue") } },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = onReadPrivacy) { Text("Privacy details") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Privacy policy") }, navigationIcon = {
            IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        })
    }, modifier = modifier.fillMaxSize()) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PolicySection("Information accessed", "After you enable Notification Access, Android provides PrioSort notification text, posting time, app package, and app label. PrioSort processes notification text locally to classify it and show it in your inbox.")
            PolicySection("Local processing", "Notification content and OTPs are not sent to a remote service. The optional Python tools in the project are offline developer utilities and are not connected to the Android app.")
            PolicySection("Sensitive information", "When Hide verification codes is enabled, PrioSort attempts to mask detected codes and account/card patterns in saved fields. Detection is heuristic and can miss sensitive text. With redaction disabled, original text may be stored.")
            PolicySection("Storage and deletion", "Notification records and sound rules are stored in the app's private Room database. Preferences are stored locally with DataStore. The database is not separately encrypted by PrioSort; it relies on Android's app sandbox and device security. Retention cleanup may be deferred by Android. App backup is disabled. You can delete records from Settings.")
            PolicySection("Your choices", "Notification Access is optional and can be revoked in Android settings. You can pause capture, exclude apps, adjust redaction and retention, disable custom sounds, or delete saved records. Custom sounds are additional PrioSort alerts; Android and source apps control their own notification sounds.")
            PolicySection("Contact", "Before public distribution, the publisher must provide a monitored privacy contact and host this policy at a stable URL.")
            Text("Last updated: October 22, 2026", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Text(body, style = MaterialTheme.typography.bodyMedium)
}
