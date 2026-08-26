package com.klogviewer.core.repository

import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.PreferencesSaveOptions
import com.klogviewer.domain.repository.PreferencesSaveResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File

class PreferencesRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should save and load preferences`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val prefs = UserPreferences(
            windowState = WindowStatePreferences(width = 800, height = 600, x = 100, y = 100),
            recentFiles = listOf("/path/to/file.log"),
            isDarkMode = false
        )

        val saveResult = repository.save(prefs)
        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should return defaults if file does not exist`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(UserPreferences())
    }

    @Test
    fun `should save and load complex preferences including tabs`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
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
                            levelFilters = setOf(LogLevel.ERROR.name, LogLevel.WARN.name),
                            isReversed = true
                        )
                    )
                )
            ),
            activeTabId = "tab1"
        )

        val saveResult = repository.save(prefs)
        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should handle corrupted json by returning defaults`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val configFile = File(tempDir, "preferences.json")
        configFile.writeText("invalid json")
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(UserPreferences())
    }
    @Test
    fun `should save and load sftp connections`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val prefs = UserPreferences(
            sftpConnections = listOf(
                SftpConfig(
                    name = "Prod",
                    host = Host("1.2.3.4"),
                    port = Port(2222),
                    username = Username("admin"),
                    auth = SftpAuth.Password("secret"),
                    logFilePath = "/var/log/app.log"
                ),
                SftpConfig(
                    name = "Staging",
                    host = Host("1.2.3.5"),
                    username = Username("dev"),
                    auth = SftpAuth.KeyPair("/home/user/.ssh/id_rsa", "pass"),
                    logFilePath = "/var/log/staging.log"
                )
            )
        )

        val saveResult = repository.save(prefs)
        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)
        
        val loaded = repository.load()
        
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should redact remote credentials in preferences json when keychain storage succeeds`() {
        val secureStore = InMemorySecureCredentialStore()
        val repository = JsonPreferencesRepository(tempDir, secureStore)
        val prefs = UserPreferences(
            sftpConnections = listOf(
                SftpConfig(
                    name = "Prod",
                    host = Host("1.2.3.4"),
                    username = Username("admin"),
                    auth = SftpAuth.Password("sftp-password-value"),
                    logFilePath = "/var/log/app.log"
                ),
                SftpConfig(
                    name = "Staging",
                    host = Host("1.2.3.5"),
                    username = Username("dev"),
                    auth = SftpAuth.KeyPair("/home/user/.ssh/id_rsa", "sftp-passphrase-value"),
                    logFilePath = "/var/log/staging.log"
                )
            ),
            s3Connections = listOf(
                S3Config(
                    name = "AWS",
                    bucket = "logs",
                    region = "eu-west-1",
                    auth = S3Auth.Explicit(
                        accessKey = "AKIAEXAMPLE",
                        secretKey = "s3-secret-value",
                        region = "eu-west-1"
                    ),
                    prefix = "application.log"
                )
            )
        )

        val saveResult = repository.save(prefs)
        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)

        val savedJson = File(tempDir, "preferences.json").readText()
        assertFalse(savedJson.contains("sftp-password-value"))
        assertFalse(savedJson.contains("sftp-passphrase-value"))
        assertFalse(savedJson.contains("s3-secret-value"))
        assertTrue(savedJson.contains(CredentialProtectionService.KEYCHAIN_MARKER))

        val loaded = repository.load()
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should delete stale keychain credentials when remote connections are removed`() {
        val secureStore = InMemorySecureCredentialStore()
        val repository = JsonPreferencesRepository(tempDir, secureStore)
        val initial = UserPreferences(
            sftpConnections = listOf(
                SftpConfig(
                    name = "Prod",
                    host = Host("1.2.3.4"),
                    username = Username("admin"),
                    auth = SftpAuth.Password("cleanup-sftp-password")
                )
            ),
            s3Connections = listOf(
                S3Config(
                    name = "AWS",
                    bucket = "logs",
                    region = "eu-west-1",
                    auth = S3Auth.Explicit(
                        accessKey = "AKIAEXAMPLE",
                        secretKey = "cleanup-s3-secret",
                        region = "eu-west-1"
                    )
                )
            )
        )

        val initialSaveResult = repository.save(initial)
        expectThat(initialSaveResult).isEqualTo(PreferencesSaveResult.Saved)

        val sftpReference = CredentialReferences.sftpPassword("Prod")
        val s3Reference = CredentialReferences.s3SecretKey("AWS")
        expectThat(secureStore.get(sftpReference)).isEqualTo("cleanup-sftp-password")
        expectThat(secureStore.get(s3Reference)).isEqualTo("cleanup-s3-secret")

        val cleanupSaveResult = repository.save(UserPreferences())
        expectThat(cleanupSaveResult).isEqualTo(PreferencesSaveResult.Saved)

        assertNull(secureStore.get(sftpReference))
        assertNull(secureStore.get(s3Reference))
    }

    @Test
    fun `should require plaintext secret confirmation when secure storage is unavailable`() {
        val repository = JsonPreferencesRepository(tempDir, FailingSecureCredentialStore())
        val prefs = UserPreferences(
            sftpConnections = listOf(
                SftpConfig(
                    name = "Prod",
                    host = Host("1.2.3.4"),
                    username = Username("admin"),
                    auth = SftpAuth.Password("plain-secret")
                )
            )
        )

        val saveResult = repository.save(prefs)

        expectThat(saveResult).isEqualTo(PreferencesSaveResult.RequiresPlaintextSecretConfirmation)
        assertFalse(File(tempDir, "preferences.json").exists())
    }

    @Test
    fun `should persist plaintext secret when fallback is explicitly allowed`() {
        val repository = JsonPreferencesRepository(tempDir, FailingSecureCredentialStore())
        val prefs = UserPreferences(
            s3Connections = listOf(
                S3Config(
                    name = "AWS",
                    bucket = "logs",
                    region = "eu-west-1",
                    auth = S3Auth.Explicit(
                        accessKey = "AKIAEXAMPLE",
                        secretKey = "fallback-s3-secret",
                        region = "eu-west-1"
                    )
                )
            )
        )

        val saveResult = repository.save(
            preferences = prefs,
            options = PreferencesSaveOptions(allowPlaintextSecretFallback = true)
        )

        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)
        val savedJson = File(tempDir, "preferences.json").readText()
        assertTrue(savedJson.contains("fallback-s3-secret"))
        assertFalse(savedJson.contains(CredentialProtectionService.KEYCHAIN_MARKER))
        expectThat(repository.load()).isEqualTo(prefs)
    }

    @Test
    fun `should save and load directory pattern mappings`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val draft = PatternDraft(
            id = "draft-1",
            name = "Logback Standard",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(
                        id = "tok-1",
                        role = PatternTokenRole.TIMESTAMP,
                        formatPattern = "yyyy-MM-dd HH:mm:ss.SSS"
                    )
                ),
                PatternSegment.Delimiter(PatternDelimiter(id = "del-1", value = " [")),
                PatternSegment.Token(PatternToken(id = "tok-2", role = PatternTokenRole.THREAD)),
                PatternSegment.Delimiter(PatternDelimiter(id = "del-2", value = "] ")),
                PatternSegment.Token(PatternToken(id = "tok-3", role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(id = "del-3", value = " - ")),
                PatternSegment.Token(PatternToken(id = "tok-4", role = PatternTokenRole.MESSAGE))
            ),
            originalFormatString = "%d [%t] %level - %msg",
            originalFormatSyntax = "LOGBACK",
            placeholderAnnotations = mapOf("{version}" to "version")
        )
        val mapping = DirectoryPatternMapping(
            directoryKey = "local:/var/log/app",
            patternDraft = draft,
            sourceType = "LOCAL",
            createdAt = 1000L,
            lastUsedAt = 2000L
        )
        val prefs = UserPreferences(
            directoryPatternMappings = mapOf("local:/var/log/app" to mapping)
        )

        val saveResult = repository.save(prefs)
        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)

        val loaded = repository.load()
        expectThat(loaded.directoryPatternMappings).isEqualTo(mapOf("local:/var/log/app" to mapping))
    }

    @Test
    fun `should load legacy preferences without directoryPatternMappings with empty default`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val legacyJson = """
            {
              "isDarkMode": false,
              "recentFiles": ["/path/to/log.log"]
            }
        """.trimIndent()
        File(tempDir, "preferences.json").writeText(legacyJson)

        val loaded = repository.load()
        expectThat(loaded.isDarkMode).isEqualTo(false)
        expectThat(loaded.recentFiles).isEqualTo(listOf("/path/to/log.log"))
        expectThat(loaded.directoryPatternMappings).isEqualTo(emptyMap())
    }

    @Test
    fun `should save and load file pattern overrides and per-source window patterns`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val draft = PatternDraft(
            id = "draft-override",
            name = "Access Log Pattern",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(
                        id = "tok-ts",
                        role = PatternTokenRole.TIMESTAMP,
                        formatPattern = "yyyy-MM-dd HH:mm:ss.SSS"
                    )
                ),
                PatternSegment.Delimiter(PatternDelimiter(id = "del-1", value = " ")),
                PatternSegment.Token(PatternToken(id = "tok-msg", role = PatternTokenRole.MESSAGE))
            )
        )
        val override = FilePatternOverride(
            fileKey = "local:/var/log/app/access.log",
            patternDraft = draft,
            sourceType = "LOCAL",
            createdAt = 100L,
            lastUsedAt = 200L
        )
        val prefs = UserPreferences(
            filePatternOverrides = mapOf(override.fileKey to override),
            tabs = listOf(
                TabPreference(
                    id = "tab1",
                    title = "Mixed",
                    activeWindowId = "win1",
                    windows = listOf(
                        WindowPreference(
                            id = "win1",
                            filePath = "/var/log/app/access.log",
                            sourceIds = listOf("/var/log/app/access.log", "/var/log/app/app.log"),
                            sourcePatterns = mapOf(
                                "/var/log/app/access.log" to SourcePatternRef(
                                    parserName = "Access Log Pattern",
                                    fileOverrideKey = override.fileKey
                                ),
                                "/var/log/app/app.log" to SourcePatternRef(
                                    parserName = "Logback Standard",
                                    directoryMappingKey = "local:/var/log/app"
                                )
                            )
                        )
                    )
                )
            )
        )

        val saveResult = repository.save(prefs)
        expectThat(saveResult).isEqualTo(PreferencesSaveResult.Saved)

        val loaded = repository.load()
        expectThat(loaded).isEqualTo(prefs)
    }

    @Test
    fun `should load legacy preferences without sourcePatterns or filePatternOverrides with empty defaults`() {
        val repository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val legacyJson = """
            {
              "tabs": [
                {
                  "id": "tab1",
                  "title": "Legacy",
                  "activeWindowId": "win1",
                  "windows": [
                    {
                      "id": "win1",
                      "filePath": "/var/log/app.log",
                      "parserName": "Logback Standard"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        File(tempDir, "preferences.json").writeText(legacyJson)

        val loaded = repository.load()
        val window = loaded.tabs.first().windows.first()
        expectThat(window.parserName).isEqualTo("Logback Standard")
        expectThat(window.sourcePatterns).isEqualTo(emptyMap())
        expectThat(loaded.filePatternOverrides).isEqualTo(emptyMap())
    }

    @Test
    fun `should migrate mac legacy preferences if target does not exist`() {
        val originalUserHome = System.getProperty("user.home")
        val fakeHome = File(tempDir, "fakeHome").apply { mkdirs() }
        val legacyDir = File(fakeHome, "Library/Application Support/com.klogviewer.app").apply { mkdirs() }
        val legacyConfigFile = File(legacyDir, "preferences.json")
        val legacyPrefs = UserPreferences(isDarkMode = true, recentFiles = listOf("/legacy/path.log"))
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        legacyConfigFile.writeText(json.encodeToString(UserPreferences.serializer(), legacyPrefs))

        try {
            System.setProperty("user.home", fakeHome.absolutePath)
            val repository = JsonPreferencesRepository(
                customConfigDir = null,
                secureCredentialStore = InMemorySecureCredentialStore()
            )
            val newConfigFile = File(fakeHome, ".klogviewer/preferences.json")

            val loaded = repository.load()
            assertTrue(newConfigFile.exists())
            expectThat(loaded.recentFiles).isEqualTo(listOf("/legacy/path.log"))
            assertTrue(loaded.isDarkMode)
        } finally {
            System.setProperty("user.home", originalUserHome)
        }
    }

    private class FailingSecureCredentialStore : SecureCredentialStore {
        override fun put(reference: CredentialReference, secret: String): Boolean = false

        override fun get(reference: CredentialReference): String? = null

        override fun delete(reference: CredentialReference): Boolean = true
    }
}
