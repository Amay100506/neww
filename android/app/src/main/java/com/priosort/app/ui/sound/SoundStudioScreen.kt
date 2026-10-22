package com.priosort.app.ui.sound

import android.content.Intent
import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.priosort.app.data.repository.SoundPlayback
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.FocusModeType
import com.priosort.app.domain.PriorityLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundStudioScreen(viewModel: SoundStudioViewModel, modifier: Modifier = Modifier) {
    val rules by viewModel.rules.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val observedApps by viewModel.observedApps.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<SoundRule?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var startText by remember { mutableStateOf("22:00") }
    var endText by remember { mutableStateOf("07:00") }
    var quietMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(settings.quietHoursStart, settings.quietHoursEnd) {
        startText = settings.quietHoursStart.toClockText()
        endText = settings.quietHoursEnd.toClockText()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sound Studio") }) },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Make important alerts sound like yours", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "PrioSort plays an extra sound for matching alerts. It cannot replace or silence the sound chosen by another app; change that app’s notification settings separately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                StudioCard {
                    SettingSwitch("Custom alerts", "Play matching sounds from PrioSort", settings.soundEnabled, viewModel::setSoundsEnabled)
                    Text("Overall volume", style = MaterialTheme.typography.titleSmall)
                    Slider(value = settings.globalVolume, onValueChange = viewModel::setVolume)
                    SettingSwitch("Vibration", "Allow vibration for matching rules", settings.vibrationEnabled, viewModel::setVibration)
                }
            }
            item {
                StudioCard {
                    SettingSwitch("Quiet hours", "Mute PrioSort custom alerts on a schedule", settings.quietHoursEnabled) {
                        val start = startText.toMinutesOrNull() ?: settings.quietHoursStart
                        val end = endText.toMinutesOrNull() ?: settings.quietHoursEnd
                        viewModel.setQuietHours(it, start, end)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(startText, { startText = it }, Modifier.weight(1f), label = { Text("Start · HH:mm") }, singleLine = true)
                        OutlinedTextField(endText, { endText = it }, Modifier.weight(1f), label = { Text("End · HH:mm") }, singleLine = true)
                    }
                    OutlinedButton(onClick = {
                        val start = startText.toMinutesOrNull()
                        val end = endText.toMinutesOrNull()
                        if (start == null || end == null) quietMessage = "Enter times as HH:mm, for example 22:00."
                        else {
                            viewModel.setQuietHours(settings.quietHoursEnabled, start, end)
                            quietMessage = "Quiet hours saved"
                        }
                    }) { Text("Save quiet hours") }
                    quietMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sound rules", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { editingRule = null; showEditor = true }) { Text("Add rule") }
                }
            }
            if (rules.isEmpty()) {
                item {
                    StudioCard {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("No custom sounds yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add a rule for an app, category, contact, or phrase. Rules stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(rules, key = { it.id }) { rule ->
                SoundRuleCard(
                    rule = rule,
                    onEdit = { editingRule = rule; showEditor = true },
                    onToggle = { viewModel.toggleRule(rule, it) },
                    onDelete = { viewModel.deleteRule(rule.id) },
                    onVolume = { viewModel.saveRule(rule.copy(volume = it)) },
                    onPreview = {
                        val uri = rule.soundUri
                        if (uri == null || !SoundPlayback.play(context, rule, settings.globalVolume)) {
                            Toast.makeText(context, "Sound unavailable. Choose the file again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            item {
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset sound settings") }
            }
        }
    }

    if (showEditor) {
        SoundRuleEditor(
            initialRule = editingRule,
            apps = observedApps,
            onDismiss = { showEditor = false; editingRule = null },
            onSave = { rule -> viewModel.saveRule(rule); showEditor = false; editingRule = null }
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset sound settings?") },
            text = { Text("This deletes your sound rules and restores PrioSort sound defaults. Your notification history stays unchanged.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetDefaults(); confirmReset = false }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SoundRuleEditor(initialRule: SoundRule?, apps: List<Pair<String, String>>, onDismiss: () -> Unit, onSave: (SoundRule) -> Unit) {
    val context = LocalContext.current
    var name by remember(initialRule?.id) { mutableStateOf(initialRule?.name.orEmpty()) }
    var matchType by remember(initialRule?.id) { mutableStateOf(initialRule?.matchType ?: SoundMatchType.APP) }
    var matchValue by remember(initialRule?.id) { mutableStateOf(initialRule?.matchValue.orEmpty()) }
    var soundUri by remember(initialRule?.id) { mutableStateOf(initialRule?.soundUri) }
    var volume by remember(initialRule?.id) { mutableFloatStateOf(initialRule?.volume ?: 0.8f) }
    var vibration by remember(initialRule?.id) { mutableStateOf(initialRule?.vibrationEnabled ?: true) }
    var vibrationPattern by remember(initialRule?.id) { mutableStateOf(initialRule?.vibrationPatternMs ?: listOf(0L, 140L)) }
    var silent by remember(initialRule?.id) { mutableStateOf(initialRule?.silent ?: false) }
    var cooldownSeconds by remember(initialRule?.id) { mutableFloatStateOf((initialRule?.cooldownMs ?: 0L).div(1000f)) }
    var repeatCount by remember(initialRule?.id) { mutableStateOf(initialRule?.repeatCount ?: 1) }
    var allowDuringQuietHours by remember(initialRule?.id) { mutableStateOf(initialRule?.allowDuringQuietHours ?: false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var matchPickerExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            soundUri = uri.toString()
            error = null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "New sound rule" else "Edit sound rule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Rule name") }, singleLine = true)
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { menuExpanded = true }) { Text("Match by: ${matchType.label}") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        SoundMatchType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.label) }, onClick = { matchType = type; menuExpanded = false }) }
                    }
                }
                OutlinedTextField(
                    value = matchValue,
                    onValueChange = { matchValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(matchType.label) },
                    placeholder = { Text(matchType.example()) },
                    singleLine = true
                )
                if (matchType in setOf(SoundMatchType.APP, SoundMatchType.CATEGORY, SoundMatchType.PRIORITY, SoundMatchType.FOCUS_MODE)) {
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { matchPickerExpanded = true }) {
                            Text("Choose ${matchType.label.lowercase()}")
                        }
                        DropdownMenu(expanded = matchPickerExpanded, onDismissRequest = { matchPickerExpanded = false }) {
                            when (matchType) {
                                SoundMatchType.APP -> apps.forEach { (packageName, appName) ->
                                    DropdownMenuItem(text = { Text("$appName · $packageName") }, onClick = { matchValue = packageName; matchPickerExpanded = false })
                                }
                                SoundMatchType.CATEGORY -> NotificationCategory.entries.filter { it != NotificationCategory.UNKNOWN }.forEach { category ->
                                    DropdownMenuItem(text = { Text(category.displayName) }, onClick = { matchValue = category.name; matchPickerExpanded = false })
                                }
                                SoundMatchType.PRIORITY -> PriorityLevel.entries.forEach { level -> DropdownMenuItem(text = { Text("${level.code} · ${level.displayName}") }, onClick = { matchValue = level.code; matchPickerExpanded = false }) }
                                SoundMatchType.FOCUS_MODE -> FocusModeType.entries.forEach { mode -> DropdownMenuItem(text = { Text(mode.displayName) }, onClick = { matchValue = mode.name; matchPickerExpanded = false }) }
                                else -> Unit
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null)
                    Text(if (soundUri == null) " Choose audio" else " Audio selected · change")
                }
                Text("Built-in sounds", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Device notification" to RingtoneManager.TYPE_NOTIFICATION,
                        "Device ringtone" to RingtoneManager.TYPE_RINGTONE,
                        "Device alarm" to RingtoneManager.TYPE_ALARM
                    ).forEach { (label, type) ->
                        val presetUri = RingtoneManager.getDefaultUri(type)?.toString()
                        FilterChip(
                            selected = presetUri != null && soundUri == presetUri,
                            onClick = {
                                if (presetUri == null) error = "$label sound is not configured on this device."
                                else { soundUri = presetUri; error = null }
                            },
                            label = { Text(label) }
                        )
                    }
                }
                if (soundUri != null) {
                    TextButton(onClick = {
                        val selectedUri = soundUri
                        if (selectedUri == null || !SoundPlayback.play(context, SoundRule(name = "Preview", matchType = matchType, matchValue = matchValue, soundUri = selectedUri, volume = volume), 1f)) {
                            Toast.makeText(context, "This audio file is unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Preview") }
                }
                Text("Rule volume", style = MaterialTheme.typography.labelLarge)
                Slider(value = volume, onValueChange = { volume = it })
                SettingSwitch("Vibrate", "Use vibration for this rule", vibration, { vibration = it })
                if (vibration) {
                    Text("Vibration pattern")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Short" to listOf(0L, 100L), "Long" to listOf(0L, 350L), "Double" to listOf(0L, 100L, 120L, 100L)).forEach { (label, pattern) ->
                            FilterChip(selected = vibrationPattern == pattern, onClick = { vibrationPattern = pattern }, label = { Text(label) })
                        }
                    }
                }
                SettingSwitch("Silent", "Mute PrioSort's custom alert for this match", silent, { silent = it })
                SettingSwitch("Allow during quiet hours", "This rule may alert during the global quiet-hours window; Android Do Not Disturb still applies", allowDuringQuietHours, { allowDuringQuietHours = it })
                Text("Cooldown · ${cooldownSeconds.toInt()} seconds")
                Slider(value = cooldownSeconds, onValueChange = { cooldownSeconds = it }, valueRange = 0f..300f, steps = 59)
                Text("Repeat playback · $repeatCount time${if (repeatCount == 1) "" else "s"}")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..4).forEach { count -> FilterChip(selected = repeatCount == count, onClick = { repeatCount = count }, label = { Text(count.toString()) }) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || matchValue.isBlank()) error = "Add a name and a match value."
                else if (!silent && soundUri.isNullOrBlank() && !vibration) error = "Choose audio, vibration, or silent mode."
                else onSave(SoundRule(
                    id = initialRule?.id ?: 0, name = name.trim(), matchType = matchType, matchValue = matchValue.trim(),
                    soundUri = soundUri, volume = volume, vibrationEnabled = vibration, silent = silent,
                    vibrationPatternMs = vibrationPattern, cooldownMs = cooldownSeconds.toLong() * 1000, repeatCount = repeatCount,
                    allowDuringQuietHours = allowDuringQuietHours
                ))
            }) { Text(if (initialRule == null) "Save rule" else "Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SoundRuleCard(rule: SoundRule, onEdit: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onVolume: (Float) -> Unit, onPreview: () -> Unit) {
    StudioCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text("${rule.matchType.label}: ${rule.matchValue}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString()
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                val audioLabel = when {
                    rule.silent -> "Silent PrioSort alert"
                    rule.soundUri == null -> "Vibration only"
                    rule.soundUri == notificationUri -> "Device notification sound"
                    rule.soundUri == ringtoneUri -> "Device ringtone"
                    rule.soundUri == alarmUri -> "Device alarm sound"
                    else -> "Custom audio selected"
                }
                Text(audioLabel, style = MaterialTheme.typography.bodySmall)
                Text("${rule.cooldownMs / 1000}s cooldown · ${rule.repeatCount} play${if (rule.repeatCount == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (rule.allowDuringQuietHours) Text("Allowed during quiet hours (DND still applies)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit ${rule.name}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${rule.name}") }
        }
        if (!rule.silent && rule.soundUri != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPreview) { Text("Preview") }
                Text("Volume", style = MaterialTheme.typography.labelSmall)
                var slider by remember(rule.id, rule.volume) { mutableFloatStateOf(rule.volume) }
                Slider(value = slider, onValueChange = { slider = it }, onValueChangeFinished = { onVolume(slider) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StudioCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

private fun SoundMatchType.example(): String = when (this) {
    SoundMatchType.APP -> "com.example.app"
    SoundMatchType.CATEGORY -> "WORK_AND_EDUCATION"
    SoundMatchType.PRIORITY -> "P1"
    SoundMatchType.FOCUS_MODE -> "WORK"
    SoundMatchType.CONTACT -> "Contact name"
    SoundMatchType.KEYWORD -> "Contains this phrase"
}

private fun Int.toClockText(): String = "%02d:%02d".format(this / 60, this % 60)

private fun String.toMinutesOrNull(): Int? {
    val pieces = split(":")
    if (pieces.size != 2) return null
    val hours = pieces[0].toIntOrNull() ?: return null
    val minutes = pieces[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}
