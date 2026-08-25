package com.klogviewer.ui.components.pattern

import androidx.compose.ui.graphics.Color
import com.klogviewer.domain.model.PatternTokenRole

object PatternTheme {
    fun roleColor(role: PatternTokenRole, isDarkMode: Boolean): Color {
        return when (role) {
            PatternTokenRole.TIMESTAMP -> if (isDarkMode) Color(0xFF4A90E2) else Color(0xFF1976D2)
            PatternTokenRole.LEVEL -> if (isDarkMode) Color(0xFF2ECC71) else Color(0xFF388E3C)
            PatternTokenRole.THREAD -> if (isDarkMode) Color(0xFF9B59B6) else Color(0xFF7B1FA2)
            PatternTokenRole.LOGGER -> if (isDarkMode) Color(0xFF1ABC9C) else Color(0xFF00796B)
            PatternTokenRole.MESSAGE -> if (isDarkMode) Color(0xFF95A5A6) else Color(0xFF546E7A)
            PatternTokenRole.EXCEPTION -> if (isDarkMode) Color(0xFFE74C3C) else Color(0xFFC62828)
            PatternTokenRole.CUSTOM_PROPERTY -> if (isDarkMode) Color(0xFFF39C12) else Color(0xFFE65100)
        }
    }

    fun delimiterColor(isDarkMode: Boolean): Color {
        return if (isDarkMode) Color(0xFF7F8C8D) else Color(0xFF9E9E9E)
    }

    fun roleBackgroundColor(role: PatternTokenRole, isDarkMode: Boolean): Color {
        val base = roleColor(role, isDarkMode)
        return base.copy(alpha = if (isDarkMode) 0.25f else 0.18f)
    }
}
