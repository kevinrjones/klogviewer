package com.klogviewer.core.repository

import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.repository.PreferencesSaveOptions
import com.klogviewer.domain.repository.PreferencesSaveResult
import com.klogviewer.domain.repository.PreferencesRepository
import kotlinx.serialization.json.Json
import java.io.File
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class JsonPreferencesRepository(
    private val customConfigDir: File? = null,
    private val secureCredentialStore: SecureCredentialStore = OsKeychainCredentialStore()
) : PreferencesRepository {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val credentialProtectionService = CredentialProtectionService(secureCredentialStore)
    
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

    override fun load(): UserPreferences {
        val storedPreferences = loadStoredPreferences()
        return credentialProtectionService.resolveAfterLoad(storedPreferences)
    }

    override fun save(
        preferences: UserPreferences,
        options: PreferencesSaveOptions
    ): PreferencesSaveResult {
        try {
            val previousPreferences = loadStoredPreferences()
            val protectionResult = credentialProtectionService.protectForPersistence(
                preferences = preferences,
                allowPlaintextSecretFallback = options.allowPlaintextSecretFallback
            )
            val protectedPreferences = when (protectionResult) {
                CredentialProtectionService.ProtectionResult.RequiresPlaintextSecretFallbackConsent -> {
                    logger.warn { "Secure credential storage unavailable; plaintext fallback requires explicit user consent" }
                    return PreferencesSaveResult.RequiresPlaintextSecretConfirmation
                }

                is CredentialProtectionService.ProtectionResult.Protected -> protectionResult.preferences
            }

            val content = json.encodeToString(UserPreferences.serializer(), protectedPreferences)
            configFile.writeText(content)
            credentialProtectionService.cleanupRemovedCredentials(previousPreferences, protectedPreferences)
            logger.debug { "Saved preferences to ${configFile.absolutePath}" }
            return PreferencesSaveResult.Saved
        } catch (e: Exception) {
            logger.error(e) { "Failed to save preferences to ${configFile.absolutePath}" }
            return PreferencesSaveResult.Failed(e.message)
        }
    }

    private fun loadStoredPreferences(): UserPreferences {
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
}
