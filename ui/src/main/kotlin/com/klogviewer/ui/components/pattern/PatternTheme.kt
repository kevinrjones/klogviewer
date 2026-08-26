package com.klogviewer.ui.components.pattern

import androidx.compose.ui.graphics.Color
import com.klogviewer.domain.model.PatternTokenRole

object PatternTheme {
    private const val TIMESTAMP_DARK_HEX = 0xFF4A90E2
    private const val TIMESTAMP_LIGHT_HEX = 0xFF1976D2
    private const val LEVEL_DARK_HEX = 0xFF2ECC71
    private const val LEVEL_LIGHT_HEX = 0xFF388E3C
    private const val THREAD_DARK_HEX = 0xFF607D8B
    private const val THREAD_LIGHT_HEX = 0xFF455A64
    private const val LOGGER_DARK_HEX = 0xFF1ABC9C
    private const val LOGGER_LIGHT_HEX = 0xFF00796B
    private const val MESSAGE_DARK_HEX = 0xFF95A5A6
    private const val MESSAGE_LIGHT_HEX = 0xFF546E7A
    private const val EXCEPTION_DARK_HEX = 0xFFE74C3C
    private const val EXCEPTION_LIGHT_HEX = 0xFFC62828
    private const val CUSTOM_PROPERTY_DARK_HEX = 0xFFF39C12
    private const val CUSTOM_PROPERTY_LIGHT_HEX = 0xFFE65100
    private const val DELIMITER_DARK_HEX = 0xFF7F8C8D
    private const val DELIMITER_LIGHT_HEX = 0xFF9E9E9E

    fun roleColor(role: PatternTokenRole, isDarkMode: Boolean): Color {
        return when (role) {
            PatternTokenRole.TIMESTAMP ->
                if (isDarkMode) Color(TIMESTAMP_DARK_HEX) else Color(TIMESTAMP_LIGHT_HEX)
            PatternTokenRole.LEVEL ->
                if (isDarkMode) Color(LEVEL_DARK_HEX) else Color(LEVEL_LIGHT_HEX)
            PatternTokenRole.THREAD ->
                if (isDarkMode) Color(THREAD_DARK_HEX) else Color(THREAD_LIGHT_HEX)
            PatternTokenRole.LOGGER ->
                if (isDarkMode) Color(LOGGER_DARK_HEX) else Color(LOGGER_LIGHT_HEX)
            PatternTokenRole.MESSAGE ->
                if (isDarkMode) Color(MESSAGE_DARK_HEX) else Color(MESSAGE_LIGHT_HEX)
            PatternTokenRole.EXCEPTION ->
                if (isDarkMode) Color(EXCEPTION_DARK_HEX) else Color(EXCEPTION_LIGHT_HEX)
            PatternTokenRole.CUSTOM_PROPERTY ->
                if (isDarkMode) Color(CUSTOM_PROPERTY_DARK_HEX) else Color(CUSTOM_PROPERTY_LIGHT_HEX)
        }
    }

    fun delimiterColor(isDarkMode: Boolean): Color {
        return if (isDarkMode) Color(DELIMITER_DARK_HEX) else Color(DELIMITER_LIGHT_HEX)
    }

    fun roleBackgroundColor(role: PatternTokenRole, isDarkMode: Boolean): Color {
        val base = roleColor(role, isDarkMode)
        return base.copy(alpha = if (isDarkMode) 0.25f else 0.18f)
    }
}
