package com.logviewer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun SourceBadge(
    sourceId: String,
    modifier: Modifier = Modifier
) {
    val color = rememberSourceColor(sourceId)
    
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = sourceId,
            style = MaterialTheme.typography.overline,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun rememberSourceColor(sourceId: String): Color {
    // Generate a stable color based on hash
    val hash = sourceId.hashCode()
    val hue = abs(hash % 360).toFloat()
    // We want relatively vibrant but readable colors
    return Color.hsv(hue, 0.7f, 0.8f)
}
