package com.priosort.app.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.priosort.app.data.export.RuleBackupPreview
import com.priosort.app.data.preferences.ThemeMode
import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.FocusModeType
import com.priosort.app.domain.FocusModeConfig
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.NaturalLanguageRuleParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.launch

private const val MAX_RULE_IMPORT_BYTES = 1_000_000

private fun readBoundedText(input: InputStream): String {
    input.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_RULE_IMPORT_BYTES) { "Rules file is too large." }
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onPrivacyClick: () -> Unit,
    onSoundStudioClick: () -> Unit,
    onLightStudioClick: () -> Unit,
    onRuleSimulatorClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val search by viewModel.search.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var confirmClear by remember { mutableStateOf(false) }
    var showDisclosure by remember { mutableStateOf(false) }
    var showRuleEditor by remember { mutableStateOf(false) }
    var exportRequested by remember { mutableStateOf(false) }
    var importRequested by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var pendingImportPreview by remember { mutableStateOf<RuleBackupPreview?>(null) }
    var vipText by remember { mutableStateOf("") }
    var focusPackagesText by remember { mutableStateOf("") }
    var retentionSlider by remember { mutableFloatStateOf(state.retentionDays.toFloat()) }
    var workStartSlider by remember { mutableFloatStateOf(state.appSettings.focusModeConfig.workStartHour.toFloat()) }
    var workEndSlider by remember { mutableFloatStateOf(state.appSettings.focusModeConfig.workEndHour.toFloat()) }
    var sleepStartSlider by remember { mutableFloatStateOf(state.appSettings.focusModeConfig.sleepStartHour.toFloat()) }
    var sleepEndSlider by remember { mutableFloatStateOf(state.appSettings.focusModeConfig.sleepEndHour.toFloat()) }
    var fontScaleSlider by remember { mutableFloatStateOf(state.appSettings.fontScale) }
    LaunchedEffect(state.retentionDays) { retentionSlider = state.retentionDays.toFloat() }
    LaunchedEffect(state.appSettings.focusModeConfig) {
        workStartSlider = state.appSettings.focusModeConfig.workStartHour.toFloat()
        workEndSlider = state.appSettings.focusModeConfig.workEndHour.toFloat()
        sleepStartSlider = state.appSettings.focusModeConfig.sleepStartHour.toFloat()
        sleepEndSlider = state.appSettings.focusModeConfig.sleepEndHour.toFloat()
    }
    LaunchedEffect(state.appSettings.fontScale) { fontScaleSlider = state.appSettings.fontScale }
    LaunchedEffect(state.appSettings.vipContacts) { vipText = state.appSettings.vipContacts.joinToString(", ") }
    LaunchedEffect(state.appSettings.focusModeConfig.allowlistedPackageNames) {
        focusPackagesText = state.appSettings.focusModeConfig.allowlistedPackageNames.joinToString(", ")
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshCaptureStatus() }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshCaptureStatus()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }, modifier = modifier.fillMaxSize()) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = viewModel::setSearch,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search settings") },
                shape = RoundedCornerShape(18.dp)
            )

            if (sectionVisible(search, "capture notification access permission status pause")) {
                SettingsSection("Notification access & capture") {
                    Text(
                        when {
                            !state.captureAccessGranted -> "Access is off. Android must grant this permission before PrioSort can read notifications."
                            state.isCapturePaused -> "Capture is paused. Existing inbox items remain saved."
                            state.serviceConnected -> "Notification listener is connected."
                            else -> "Access is granted; Android is reconnecting the listener."
                        }, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = {
                        if (state.disclosureAccepted) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        else showDisclosure = true
                    }) { Text(if (state.captureAccessGranted) "Open Android access settings" else "Enable notification access") }
                    SettingSwitch("Pause capture", "Stop processing new notifications", state.isCapturePaused, viewModel::toggleCapturePaused)
                }
            }

            if (sectionVisible(search, "lights lighting edge glow overlay")) {
                SettingsSection("Light Studio") {
                    Text("Create short, optional edge-light rules. Android may restrict overlays on the lock screen or on some devices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onLightStudioClick) { Text("Open Light Studio") }
                }
            }

            if (sectionVisible(search, "rule simulator dry run conflicts test")) {
                SettingsSection("Rule simulator") {
                    Text("Preview priority and alert rule matches without saving or triggering effects.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRuleSimulatorClick) { Text("Open dry-run simulator") }
                }
            }

            if (sectionVisible(search, "sounds studio audio volume vibration quiet hours")) {
                SettingsSection("Custom sounds") {
                    Text("Create app, category, contact, and phrase-based sounds, then set volume and quiet hours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSoundStudioClick) { Text("Open Sound Studio") }
                }
            }

            if (sectionVisible(search, "priority category preferences vip contacts custom rules")) {
                SettingsSection("Priority, categories & contacts") {
                    Text("Set a default priority by category. OTP and detected fraud alerts keep their safety priority.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NotificationCategory.entries.filter { it != NotificationCategory.UNKNOWN }.forEach { category ->
                        CategoryPriorityRow(
                            category = category,
                            code = state.appSettings.categoryPriorityOverrides[category.name],
                            onSelect = { viewModel.setCategoryPriority(category.name, it) }
                        )
                    }
                    OutlinedTextField(
                        value = vipText,
                        onValueChange = { vipText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("VIP contact names") },
                        supportingText = { Text("Comma-separated. Compared only with sender/title text available in the notification.") }
                    )
                    TextButton(onClick = { viewModel.setVipContacts(vipText.split(',').map(String::trim).filter(String::isNotBlank)) }) { Text("Save VIP contacts") }
                    Text("Priority rules", style = MaterialTheme.typography.titleMedium)
                    state.customRules.forEach { rule ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(rule.ruleName, style = MaterialTheme.typography.titleSmall)
                                Text("${rule.targetField}: ${rule.keywordPattern} → ${rule.assignedPriority?.displayName ?: rule.assignedCategory?.displayName ?: "Match only"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.deleteCustomRule(rule.id) }) { Text("Delete") }
                        }
                    }
                    TextButton(onClick = { showRuleEditor = true }) { Text("Add priority rule") }
                }
            }

            if (sectionVisible(search, "focus modes study work sleep personal custom schedule")) {
                val focus = state.appSettings.focusModeConfig
                val selectedFocus = if (focus.autoScheduleEnabled) focus.resolveActiveMode() else focus.type
                SettingsSection("Focus modes") {
                    Text("Choose how PrioSort ranks non-critical notifications. Critical alerts remain Critical.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(FocusModeType.OFF, FocusModeType.STUDY, FocusModeType.WORK).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { mode ->
                                FilterChip(
                                    selected = selectedFocus == mode,
                                    onClick = { viewModel.setFocusModeConfig(focus.copy(type = mode, autoScheduleEnabled = false)) },
                                    label = { Text(mode.displayName) }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(FocusModeType.SLEEP, FocusModeType.PERSONAL, FocusModeType.CUSTOM).forEach { mode ->
                            FilterChip(
                                selected = selectedFocus == mode,
                                onClick = { viewModel.setFocusModeConfig(focus.copy(type = mode, autoScheduleEnabled = false)) },
                                label = { Text(mode.displayName) }
                            )
                        }
                    }
                    SettingSwitch("Schedule Work and Sleep modes", "Use device local time: Work ${focus.workStartHour}:00–${focus.workEndHour}:00, Sleep ${focus.sleepStartHour}:00–${focus.sleepEndHour}:00, Personal at other times", focus.autoScheduleEnabled) {
                        viewModel.setFocusModeConfig(focus.copy(autoScheduleEnabled = it))
                    }
                    if (focus.autoScheduleEnabled) {
                        Text("Work starts · ${workStartSlider.toInt().toString().padStart(2, '0')}:00")
                        Slider(value = workStartSlider, onValueChange = { workStartSlider = it }, onValueChangeFinished = {
                            viewModel.setFocusModeConfig(focus.copy(workStartHour = workStartSlider.toInt()))
                        }, valueRange = 0f..23f, steps = 22)
                        Text("Work ends · ${workEndSlider.toInt().toString().padStart(2, '0')}:00")
                        Slider(value = workEndSlider, onValueChange = { workEndSlider = it }, onValueChangeFinished = {
                            viewModel.setFocusModeConfig(focus.copy(workEndHour = workEndSlider.toInt()))
                        }, valueRange = 0f..23f, steps = 22)
                        Text("Sleep starts · ${sleepStartSlider.toInt().toString().padStart(2, '0')}:00")
                        Slider(value = sleepStartSlider, onValueChange = { sleepStartSlider = it }, onValueChangeFinished = {
                            viewModel.setFocusModeConfig(focus.copy(sleepStartHour = sleepStartSlider.toInt()))
                        }, valueRange = 0f..23f, steps = 22)
                        Text("Sleep ends · ${sleepEndSlider.toInt().toString().padStart(2, '0')}:00")
                        Slider(value = sleepEndSlider, onValueChange = { sleepEndSlider = it }, onValueChangeFinished = {
                            viewModel.setFocusModeConfig(focus.copy(sleepEndHour = sleepEndSlider.toInt()))
                        }, valueRange = 0f..23f, steps = 22)
                    }
                    OutlinedTextField(
                        value = focusPackagesText,
                        onValueChange = { focusPackagesText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Allowed app package names") },
                        supportingText = { Text("Comma-separated; these apps keep their regular priority during a focus mode.") }
                    )
                    Text("Allowed categories")
                    NotificationCategory.entries.filter { it != NotificationCategory.UNKNOWN }.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { category ->
                                FilterChip(
                                    selected = category in focus.allowlistedCategories,
                                    onClick = {
                                        val updated = if (category in focus.allowlistedCategories) focus.allowlistedCategories - category else focus.allowlistedCategories + category
                                        viewModel.setFocusModeConfig(focus.copy(allowlistedCategories = updated))
                                    },
                                    label = { Text(category.displayName) }
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        val packages = focusPackagesText.split(',').map(String::trim).filter(String::isNotBlank).toSet()
                        viewModel.setFocusModeConfig(focus.copy(allowlistedPackageNames = packages))
                    }) { Text("Save allowed apps") }
                }
            }

            if (sectionVisible(search, "appearance theme dark light system typography accessibility reduce motion font scale text size")) {
                SettingsSection("Appearance & accessibility") {
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.appSettings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                            )
                        }
                    }
                    SettingSwitch("Reduce motion", "PrioSort avoids decorative motion; Android controls system animations", state.appSettings.reduceMotion, viewModel::setReduceMotion)
                    Text("App text size · ${(fontScaleSlider * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = fontScaleSlider,
                        onValueChange = { fontScaleSlider = it },
                        onValueChangeFinished = { viewModel.setFontScale(fontScaleSlider) },
                        valueRange = 0.85f..1.3f,
                        steps = 8
                    )
                    Text("This scales PrioSort text in addition to Android's font-size setting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (sectionVisible(search, "privacy data redaction retention exclusions delete")) {
                SettingsSection("Privacy & local data") {
                    SettingSwitch("Hide verification codes", "Mask detected OTP and account patterns in saved text", state.isRedactionEnabled, viewModel::toggleRedaction)
                    SettingSwitch("Biometric app lock", "Require device biometric authentication when reopening PrioSort. This does not encrypt stored data.", state.appSettings.biometricLockEnabled) { enabled ->
                        if (!enabled) {
                            viewModel.setBiometricLockEnabled(false)
                        } else if (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
                            viewModel.setBiometricLockEnabled(true)
                        } else {
                            Toast.makeText(context, "Set up a biometric unlock in Android Settings before enabling the app lock.", Toast.LENGTH_LONG).show()
                            runCatching { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                        }
                    }
                    Text("Delete saved notifications after ${state.retentionDays} days", style = MaterialTheme.typography.titleSmall)
                    Slider(value = retentionSlider, onValueChange = { retentionSlider = it }, onValueChangeFinished = { viewModel.setRetentionDays(retentionSlider.toInt().coerceIn(1, 30)) }, valueRange = 1f..30f, steps = 28)
                    if (state.packageSettings.any { it.isExcluded }) {
                        Text("Excluded apps", style = MaterialTheme.typography.titleSmall)
                        state.packageSettings.filter { it.isExcluded }.forEach { setting ->
                            SettingSwitch(setting.applicationName ?: setting.packageName, "Excluded from capture", true) { viewModel.togglePackageExcluded(setting.packageName, false) }
                        }
                    }
                    TextButton(onClick = onPrivacyClick) { Text("Read privacy details") }
                    TextButton(onClick = { confirmClear = true }) {
                        androidx.compose.material3.Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Text("  Delete all inbox items")
                    }
                }
            }

            if (sectionVisible(search, "analytics export import backup diagnostics help about app info")) {
                SettingsSection("Analytics, backup & help") {
                    Button(onClick = onAnalyticsClick) { Text("Open local analytics") }
                    Text("Back up priority, sound, and light rule definitions. Notification history and device-specific sound file URIs are excluded; import previews and asks before replacing local rules.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { /* handled by document picker below */ exportRequested = true }) { Text("Export rules") }
                        TextButton(onClick = { importRequested = true }) { Text("Import rules") }
                    }
                    Text("Capture: ${if (state.serviceConnected) "connected" else if (state.captureAccessGranted) "reconnecting" else "permission off"}", style = MaterialTheme.typography.bodySmall)
                    Text("Notifications and sound rules stay on this device. PrioSort cannot change another app’s notification channel or original alert sound.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PrioSort · ${runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "version unavailable"}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = viewModel.exportRulesJson()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) } ?: error("Could not create the file")
            }.onSuccess { Toast.makeText(context, "Rules exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Export failed", Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.let(::readBoundedText) ?: error("Could not read the file")
                val preview = viewModel.previewRulesJson(json)
                json to preview
            }.onSuccess { (json, preview) ->
                pendingImportJson = json
                pendingImportPreview = preview
            }
                .onFailure { Toast.makeText(context, it.message ?: "Import failed", Toast.LENGTH_LONG).show() }
        }
    }
    LaunchedEffect(exportRequested) {
        if (exportRequested) {
            exportLauncher.launch("priosort-rules.json")
            exportRequested = false
        }
    }
    LaunchedEffect(importRequested) {
        if (importRequested) {
            importLauncher.launch(arrayOf("application/json", "text/*"))
            importRequested = false
        }
    }

    pendingImportPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null; pendingImportPreview = null },
            title = { Text("Replace local rules?") },
            text = {
                Text("This file contains ${preview.priorityRuleCount} priority, ${preview.soundRuleCount} sound, and ${preview.lightRuleCount} light rules. Import replaces your current rules. Notification history is not included. Sound files are device-specific, so choose them again after import.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = pendingImportJson ?: return@TextButton
                    pendingImportJson = null
                    pendingImportPreview = null
                    scope.launch {
                        runCatching { viewModel.importRulesJson(json) }
                            .onSuccess { Toast.makeText(context, "Rules imported", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, it.message ?: "Import failed", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Replace rules") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null; pendingImportPreview = null }) { Text("Cancel") }
            }
        )
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
    if (showRuleEditor) {
        PriorityRuleEditor(onDismiss = { showRuleEditor = false }, onSave = { viewModel.addCustomRule(it); showRuleEditor = false })
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all inbox items?") },
            text = { Text("This permanently removes notification history saved on this device. Sound and priority rules are kept.") },
            confirmButton = { TextButton(onClick = { viewModel.clearLocalDatabase(); confirmClear = false }) { Text("Delete all") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CategoryPriorityRow(category: NotificationCategory, code: String?, onSelect: (PriorityLevel?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        TextButton(onClick = { expanded = true }) {
            Text("${category.displayName}: ${code?.let(PriorityLevel::fromCode)?.displayName ?: "Automatic"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Automatic") }, onClick = { expanded = false; onSelect(null) })
            PriorityLevel.entries.forEach { level -> DropdownMenuItem(text = { Text(level.displayName) }, onClick = { expanded = false; onSelect(level) }) }
        }
    }
}

@Composable
private fun PriorityRuleEditor(onDismiss: () -> Unit, onSave: (CustomRule) -> Unit) {
    var naturalDescription by remember { mutableStateOf("") }
    var parseMessage by remember { mutableStateOf<String?>(null) }
    var parsedPreview by remember { mutableStateOf<CustomRule?>(null) }
    var name by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("ANY") }
    var priority by remember { mutableStateOf<PriorityLevel?>(null) }
    var category by remember { mutableStateOf<NotificationCategory?>(null) }
    var targetOpen by remember { mutableStateOf(false) }
    var priorityOpen by remember { mutableStateOf(false) }
    var categoryOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New priority rule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Describe a rule (parsed locally)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(naturalDescription, { naturalDescription = it; parsedPreview = null; parseMessage = null }, label = { Text("Natural-language rule") }, supportingText = { Text("Example: When body contains \"invoice\", set priority to High") }, minLines = 2)
                Button(onClick = {
                    val result = NaturalLanguageRuleParser.parse(naturalDescription)
                    parseMessage = result.message
                    result.rule?.let { draft ->
                        parsedPreview = draft
                        name = draft.ruleName
                        keyword = draft.keywordPattern
                        target = draft.targetField
                        priority = draft.assignedPriority
                        category = draft.assignedCategory
                    }
                }, enabled = naturalDescription.isNotBlank()) { Text("Parse and preview") }
                parseMessage?.let { Text(it, color = if (parsedPreview != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                parsedPreview?.let { draft ->
                    val parsedTargetLabel = when (draft.targetField) {
                        "PACKAGE" -> "app package"
                        "TITLE" -> "title"
                        "BODY" -> "body"
                        else -> "notification text"
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Review parsed rule", style = MaterialTheme.typography.labelLarge)
                            Text("If $parsedTargetLabel contains ‘${draft.keywordPattern}’, assign ${draft.assignedPriority?.displayName ?: draft.assignedCategory?.displayName}.")
                        }
                    }
                }
                OutlinedTextField(name, { name = it; parsedPreview = null; parseMessage = null }, label = { Text("Rule name") }, singleLine = true)
                OutlinedTextField(keyword, { keyword = it; parsedPreview = null; parseMessage = null }, label = { Text("Match phrase") }, singleLine = true)
                DropdownMenuButton("Field: $target", targetOpen, { targetOpen = it }) {
                    listOf("ANY", "TITLE", "BODY", "PACKAGE").forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { target = choice; targetOpen = false; parsedPreview = null; parseMessage = null }) }
                }
                DropdownMenuButton("Priority: ${priority?.displayName ?: "No override"}", priorityOpen, { priorityOpen = it }) {
                    DropdownMenuItem(text = { Text("No override") }, onClick = { priority = null; priorityOpen = false; parsedPreview = null; parseMessage = null })
                    PriorityLevel.entries.forEach { level -> DropdownMenuItem(text = { Text(level.displayName) }, onClick = { priority = level; priorityOpen = false; parsedPreview = null; parseMessage = null }) }
                }
                DropdownMenuButton("Category: ${category?.displayName ?: "No override"}", categoryOpen, { categoryOpen = it }) {
                    DropdownMenuItem(text = { Text("No override") }, onClick = { category = null; categoryOpen = false; parsedPreview = null; parseMessage = null })
                    NotificationCategory.entries.filter { it != NotificationCategory.UNKNOWN }.forEach { item -> DropdownMenuItem(text = { Text(item.displayName) }, onClick = { category = item; categoryOpen = false; parsedPreview = null; parseMessage = null }) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || keyword.isBlank()) error = "Enter a name and match phrase."
                else if (priority == null && category == null) error = "Choose a priority or category override."
                else onSave(CustomRule(ruleName = name.trim(), keywordPattern = keyword.trim(), targetField = target, assignedPriority = priority, assignedCategory = category))
            }) { Text("Save rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DropdownMenuButton(label: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Box {
        TextButton(onClick = { onExpandedChange(true) }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }, content = content)
    }
}

private fun sectionVisible(query: String, keywords: String): Boolean =
    query.isBlank() || keywords.contains(query.trim(), ignoreCase = true)
