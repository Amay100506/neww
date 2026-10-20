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
import com.priosort.app.domain.NotificationCategory

@Composable
fun CategoryBadge(category: NotificationCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE2E8F0))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.displayName,
            color = Color(0xFF334155),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
