package com.priosort.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.ui.theme.PriorityCriticalColor
import com.priosort.app.ui.theme.PriorityHighColor
import com.priosort.app.ui.theme.PriorityLowColor
import com.priosort.app.ui.theme.PriorityMediumColor
import com.priosort.app.ui.theme.PriorityMinimalColor

@Composable
fun PriorityBadge(level: PriorityLevel, modifier: Modifier = Modifier) {
    val bgColor = when (level) {
        PriorityLevel.CRITICAL -> PriorityCriticalColor
        PriorityLevel.HIGH -> PriorityHighColor
        PriorityLevel.MEDIUM -> PriorityMediumColor
        PriorityLevel.LOW -> PriorityLowColor
        PriorityLevel.MINIMAL -> PriorityMinimalColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "${level.code} • ${level.displayName}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
