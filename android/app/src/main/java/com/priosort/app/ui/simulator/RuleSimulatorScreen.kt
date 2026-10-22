package com.priosort.app.ui.simulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.priosort.app.domain.PriorityLevel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RuleSimulatorScreen(viewModel: RuleSimulatorViewModel, modifier: Modifier = Modifier) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Rule Simulator") }) }, modifier = modifier.fillMaxSize()) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Dry-run a notification through the current priority, sound, and light rules. This never plays audio, vibrates, or opens an overlay.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Notification title") }, singleLine = true)
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Notification body") }, minLines = 3)
            OutlinedTextField(packageName, { packageName = it }, Modifier.fillMaxWidth(), label = { Text("Package name (optional)") }, singleLine = true)
            OutlinedTextField(sender, { sender = it }, Modifier.fillMaxWidth(), label = { Text("Sender identity (optional)") }, singleLine = true)
            Button(onClick = { viewModel.simulate(title, body, packageName, sender) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text("Run dry-run")
            }
            if (state.loading) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.result?.let { result ->
                ResultCard("Priority decision") {
                    Text("${result.priority.displayName} (${result.priority.code}) · ${result.score}/100", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Category: ${result.category.displayName} (${(result.confidence * 100).toInt()}% rule confidence)")
                    Text("Score: U ${result.breakdown.scoreBreakdown.urgencyScore} · A ${result.breakdown.scoreBreakdown.actionScore} · D ${result.breakdown.scoreBreakdown.deadlineScore} · I ${result.breakdown.scoreBreakdown.importanceScore} · R ${result.breakdown.scoreBreakdown.recencyScore}")
                    result.breakdown.overrideReason?.let { Text("Override: $it") }
                    result.breakdown.explanations.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                ResultCard("Sound") { Text(result.soundDecision); result.soundMatches.forEachIndexed { index, rule -> Text("${if (index == 0) "Winner" else "Skipped"}: ${rule.name}") } }
                ResultCard("Edge light") { Text(result.lightDecision); result.lightMatches.forEachIndexed { index, rule -> Text("${if (index == 0) "Winner" else "Skipped"}: ${rule.name}") } }
                if (result.warnings.isNotEmpty()) ResultCard("Rule matches and warnings") { result.warnings.forEach { Text("• $it") } }
                Text("The result is a dry-run. The app does not save this notification or execute any alert.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
