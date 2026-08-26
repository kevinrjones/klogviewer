package com.klogviewer.ui.components.pattern

import androidx.compose.ui.graphics.Color
import com.klogviewer.domain.model.PatternTokenRole
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class PatternThemeTest {

    @Test
    fun `given thread role in dark mode when roleColor requested then returns blue-gray slate`() {
        val darkThreadColor = PatternTheme.roleColor(PatternTokenRole.THREAD, isDarkMode = true)
        expectThat(darkThreadColor).isEqualTo(Color(0xFF607D8B))
    }

    @Test
    fun `given thread role in light mode when roleColor requested then returns darker blue-gray slate`() {
        val lightThreadColor = PatternTheme.roleColor(PatternTokenRole.THREAD, isDarkMode = false)
        expectThat(lightThreadColor).isEqualTo(Color(0xFF455A64))
    }

    @Test
    fun `given all roles when colors checked then none use legacy purple`() {
        val legacyPurpleDark = Color(0xFF9B59B6)
        val legacyPurpleLight = Color(0xFF7B1FA2)

        PatternTokenRole.entries.forEach { role ->
            val darkColor = PatternTheme.roleColor(role, isDarkMode = true)
            val lightColor = PatternTheme.roleColor(role, isDarkMode = false)

            expectThat(darkColor).isNotEqualTo(legacyPurpleDark)
            expectThat(lightColor).isNotEqualTo(legacyPurpleLight)
        }
    }

    @Test
    fun `given all roles when roleBackgroundColor requested then correct alpha is applied`() {
        PatternTokenRole.entries.forEach { role ->
            val darkBase = PatternTheme.roleColor(role, isDarkMode = true)
            val darkBg = PatternTheme.roleBackgroundColor(role, isDarkMode = true)
            expectThat(darkBg).isEqualTo(darkBase.copy(alpha = 0.25f))

            val lightBase = PatternTheme.roleColor(role, isDarkMode = false)
            val lightBg = PatternTheme.roleBackgroundColor(role, isDarkMode = false)
            expectThat(lightBg).isEqualTo(lightBase.copy(alpha = 0.18f))
        }
    }

    @Test
    fun `given delimiter when color requested then returns correct theme delimiter color`() {
        val darkDelimiter = PatternTheme.delimiterColor(isDarkMode = true)
        expectThat(darkDelimiter).isEqualTo(Color(0xFF7F8C8D))

        val lightDelimiter = PatternTheme.delimiterColor(isDarkMode = false)
        expectThat(lightDelimiter).isEqualTo(Color(0xFF9E9E9E))
    }
}
