package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel

@Composable
fun LogList(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(logs) { entry ->
            LogEntryRow(entry)
        }
    }
}

@Composable
fun LogEntryRow(entry: LogEntry) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = entry.timestamp.value,
            color = Color.DarkGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(150.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "[${entry.level}]",
            color = getLevelColor(entry.level),
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(70.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.content.value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun getLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.DEBUG -> Color.Gray
    LogLevel.INFO -> Color(0xFF2196F3) // Blue
    LogLevel.WARN -> Color(0xFFFFC107) // Amber
    LogLevel.ERROR -> Color.Red
    LogLevel.FATAL -> Color(0xFFD32F2F) // Dark Red
    LogLevel.UNKNOWN -> Color.Black
}
