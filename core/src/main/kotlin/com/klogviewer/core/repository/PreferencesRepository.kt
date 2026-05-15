package com.klogviewer.core.repository

import com.klogviewer.domain.model.UserPreferences
import kotlinx.serialization.json.Json
import java.io.File
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class PreferencesRepository(private val customConfigDir: File? = null) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val configFile: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val appName = "KLogViewer"
        val userHome = System.getProperty("user.home")
        
        val dir = customConfigDir ?: when {
            os.contains("mac") -> File(userHome, "Library/Application Support/com.klogviewer.app")
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (appData != null) File(appData, appName) else File(userHome, "AppData/Roaming/$appName")
            }
            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME")
                if (xdgConfig != null) File(xdgConfig, appName.lowercase()) else File(userHome, ".config/${appName.lowercase()}")
            }
        }
        
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (created) {
                logger.info { "Created preferences directory: ${dir.absolutePath}" }
            }
        }
        
        File(dir, "preferences.json")
    }

    fun load(): UserPreferences {
        return if (configFile.exists()) {
            try {
                val content = configFile.readText()
                json.decodeFromString<UserPreferences>(content)
            } catch (e: Exception) {
                logger.error(e) { "Failed to load preferences from ${configFile.absolutePath}" }
                UserPreferences()
            }
        } else {
            logger.info { "Preferences file not found, using defaults" }
            UserPreferences()
        }
    }

    fun save(preferences: UserPreferences) {
        try {
            val content = json.encodeToString(UserPreferences.serializer(), preferences)
            configFile.writeText(content)
            logger.debug { "Saved preferences to ${configFile.absolutePath}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save preferences to ${configFile.absolutePath}" }
        }
    }
}
