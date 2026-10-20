package com.priosort.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.priosort.app.domain.PriorityLevel

@Composable
fun PriorityFilterRow(
    selectedPriority: PriorityLevel?,
    onSelectPriority: (PriorityLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        FilterChip(
            selected = selectedPriority == null,
            onClick = { onSelectPriority(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0F172A),
                selectedLabelColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(6.dp))

        PriorityLevel.entries.forEach { level ->
            FilterChip(
                selected = selectedPriority == level,
                onClick = { onSelectPriority(if (selectedPriority == level) null else level) },
                label = { Text("${level.code} ${level.displayName}") }
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}
