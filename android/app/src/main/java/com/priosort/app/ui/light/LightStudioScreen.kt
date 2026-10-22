package com.priosort.app.ui.light

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.priosort.app.domain.LightEdge
import com.priosort.app.domain.LightMatchType
import com.priosort.app.domain.LightPattern
import com.priosort.app.domain.LightRepeatPolicy
import com.priosort.app.domain.LightRule
import kotlinx.coroutines.delay

private val lightPalette = listOf(
    0xFFFF4F9A.toInt() to "Pink", 0xFF56C8FF.toInt() to "Blue",
    0xFF8BE28B.toInt() to "Green", 0xFFFFC857.toInt() to "Amber",
    0xFFC19BFF.toInt() to "Violet", 0xFFFFFFFF.toInt() to "White"
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LightStudioScreen(viewModel: LightStudioViewModel, modifier: Modifier = Modifier) {
    val rules by viewModel.rules.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val apps by viewModel.observedApps.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var overlayPermission by remember { mutableStateOf(viewModel.canDrawOverlay()) }
    var showEditor by remember { mutableStateOf(false) }
    var previewingId by remember { mutableStateOf<Long?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) overlayPermission = viewModel.canDrawOverlay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); viewModel.stopPreview() }
    }
    LaunchedEffect(previewingId) {
        if (previewingId != null) {
            delay(5_000)
            previewingId = null
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Light Studio") }) }, modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Short edge alerts for your notifications", style = MaterialTheme.typography.titleLarge)
                        Text("Overlay visibility depends on Android version, lock-screen state, and device maker. Edge lights only run for a few seconds while the screen is on.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!overlayPermission) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                            }) { Text("Allow display over other apps") }
                        } else {
                            Text("Overlay permission is available on this device.", color = MaterialTheme.colorScheme.primary)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Live edge alerts", style = MaterialTheme.typography.titleSmall)
                                Text(if (overlayPermission) "Play the highest matching enabled rule for new notifications" else "Allow overlay permission first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.edgeLightsEnabled,
                                enabled = overlayPermission,
                                onCheckedChange = viewModel::setEnabled
                            )
                        }
                        Button(onClick = { showEditor = true }) { Text("Add light rule") }
                    }
                }
            }
            if (rules.isEmpty()) {
                item { Text("No light rules yet. Previews work in the app; live edge alerts require overlay permission.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(rules, key = { it.id }) { rule ->
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                Text("${rule.matchType.name.lowercase().replaceFirstChar(Char::uppercase)}: ${rule.matchValue} · ${rule.pattern.name.lowercase().replace('_', ' ')} · ${rule.repeatPolicy.name.lowercase().replace('_', ' ')}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = rule.enabled, onCheckedChange = { viewModel.toggleRule(rule, it) })
                        }
                        if (previewingId == rule.id) InAppLightPreview(rule = rule)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (overlayPermission) {
                                    previewingId = null
                                    viewModel.preview(rule.copy(durationMs = 5_000))
                                } else previewingId = rule.id
                            }) { Text(if (overlayPermission) "Preview overlay" else "Preview in app") }
                            if (overlayPermission) TextButton(onClick = viewModel::stopPreview) { Text("Stop") }
                            TextButton(onClick = { viewModel.deleteRule(rule.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    if (showEditor) LightRuleEditor(apps, onDismiss = { showEditor = false }, onSave = { viewModel.saveRule(it); showEditor = false })
}

@Composable
private fun InAppLightPreview(rule: LightRule) {
    val transition = rememberInfiniteTransition(label = "light-preview")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = rule.intensity,
        animationSpec = infiniteRepeatable(tween((1000 / rule.speed.coerceIn(0.25f, 3f)).toInt().coerceAtLeast(200)), RepeatMode.Reverse),
        label = "preview-alpha"
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(rule.colorArgb).copy(alpha = alpha)))
            Text("Animated in-app preview · ${rule.durationMs / 1000}s live timeout", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(rule.colorArgb).copy(alpha = alpha)))
        }
    }
}

@Composable
private fun LightRuleEditor(apps: List<Pair<String, String>>, onDismiss: () -> Unit, onSave: (LightRule) -> Unit) {
    var name by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf(LightMatchType.APP) }
    var matchValue by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(lightPalette.first().first) }
    var pattern by remember { mutableStateOf(LightPattern.PULSE) }
    var repeatPolicy by remember { mutableStateOf(LightRepeatPolicy.UNTIL_TIMEOUT) }
    var edges by remember { mutableStateOf(LightEdge.entries.toSet()) }
    var speed by remember { mutableFloatStateOf(1f) }
    var intensity by remember { mutableFloatStateOf(0.8f) }
    var duration by remember { mutableFloatStateOf(5f) }
    var matchMenu by remember { mutableStateOf(false) }
    var patternMenu by remember { mutableStateOf(false) }
    var appMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New light rule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Rule name") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { matchMenu = true }) { Text("Match by ${matchType.name.lowercase()}") }
                    DropdownMenu(matchMenu, { matchMenu = false }) {
                        LightMatchType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.name.lowercase().replaceFirstChar(Char::uppercase)) }, onClick = { matchType = type; matchMenu = false }) }
                    }
                }
                Text("Repeat")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LightRepeatPolicy.entries.forEach { choice ->
                        FilterChip(selected = repeatPolicy == choice, onClick = { repeatPolicy = choice }, label = { Text(choice.name.lowercase().replace('_', ' ')) })
                    }
                }
                OutlinedTextField(matchValue, { matchValue = it }, label = { Text(matchType.name.lowercase().replaceFirstChar(Char::uppercase) + " match") }, singleLine = true)
                if (matchType == LightMatchType.APP && apps.isNotEmpty()) {
                    Box {
                        TextButton(onClick = { appMenu = true }) { Text("Choose observed app") }
                        DropdownMenu(appMenu, { appMenu = false }) {
                            apps.forEach { (pkg, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { matchValue = pkg; appMenu = false }) }
                        }
                    }
                }
                Box {
                    OutlinedButton(onClick = { patternMenu = true }) { Text("Pattern: ${pattern.name.lowercase().replace('_', ' ')}") }
                    DropdownMenu(patternMenu, { patternMenu = false }) {
                        LightPattern.entries.forEach { item -> DropdownMenuItem(text = { Text(item.name.lowercase().replace('_', ' ')) }, onClick = { pattern = item; patternMenu = false }) }
                    }
                }
                Text("Color")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    lightPalette.forEach { (value, label) -> FilterChip(selected = color == value, onClick = { color = value }, label = { Text(label) }) }
                }
                Text("Edges")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LightEdge.entries.forEach { edge -> FilterChip(selected = edge in edges, onClick = { edges = if (edge in edges && edges.size > 1) edges - edge else edges + edge }, label = { Text(edge.name.lowercase().replaceFirstChar(Char::uppercase)) }) }
                }
                Text("Speed · ${"%.1f".format(speed)}×")
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.25f..3f)
                Text("Intensity · ${(intensity * 100).toInt()}%")
                Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0.1f..1f)
                Text("Duration · ${duration.toInt()}s (maximum 20s)")
                Slider(value = duration, onValueChange = { duration = it }, valueRange = 2f..20f, steps = 17)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || matchValue.isBlank()) error = "Enter a rule name and match value."
                else onSave(LightRule(name = name.trim(), matchType = matchType, matchValue = matchValue.trim(), colorArgb = color, pattern = pattern, repeatPolicy = repeatPolicy, speed = speed, durationMs = duration.toLong() * 1000, intensity = intensity, edges = edges))
            }) { Text("Save rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
