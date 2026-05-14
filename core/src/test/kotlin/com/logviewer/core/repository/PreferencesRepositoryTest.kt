package com.logviewer.core.repository

import com.logviewer.domain.model.TabPreference
import com.logviewer.domain.model.UserPreferences
import com.logviewer.domain.model.WindowPreference
import com.logviewer.domain.model.WindowStatePreferences
import com.logviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File

class PreferencesRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should save and load preferences`() {
        val repository = PreferencesRepository(tempDir)
        val prefs = UserPreferences(
            windowState = WindowStatePreferences(width = 800, height = 600, x = 100, y = 100),
            recentFiles = listOf("/path/to/file.log"),
            isDarkMode = false
        )

        repository.save(prefs)
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should return defaults if file does not exist`() {
        val repository = PreferencesRepository(tempDir)
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(UserPreferences())
    }

    @Test
    fun `should save and load complex preferences including tabs`() {
        val repository = PreferencesRepository(tempDir)
        val prefs = UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "tab1",
                    title = "Tab 1",
                    activeWindowId = "win1",
                    windows = listOf(
                        WindowPreference(
                            id = "win1",
                            filePath = "file1.log",
                            sourceIds = listOf("file1.log"),
                            filterQueries = listOf("error"),
                            levelFilters = setOf(LogLevel.ERROR, LogLevel.WARN),
                            isReversed = true
                        )
                    )
                )
            ),
            activeTabId = "tab1"
        )

        repository.save(prefs)
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should handle corrupted json by returning defaults`() {
        val repository = PreferencesRepository(tempDir)
        val configFile = File(tempDir, "preferences.json")
        configFile.writeText("invalid json")
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(UserPreferences())
    }
}
