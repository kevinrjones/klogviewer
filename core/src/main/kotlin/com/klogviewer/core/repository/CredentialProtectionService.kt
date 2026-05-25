package com.klogviewer.core.repository

import com.klogviewer.domain.model.S3Auth
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpAuth
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.model.UserPreferences

internal class CredentialProtectionService(
    private val secureCredentialStore: SecureCredentialStore
) {
    fun protectForPersistence(
        preferences: UserPreferences,
        allowPlaintextSecretFallback: Boolean
    ): ProtectionResult {
        val protectedSftpConnections = mutableListOf<SftpConfig>()
        preferences.sftpConnections.forEach { config ->
            when (
                val protectedConfig = protectSftpConfig(
                    config = config,
                    allowPlaintextSecretFallback = allowPlaintextSecretFallback
                )
            ) {
                is ProtectedValue.Success -> protectedSftpConnections += protectedConfig.value
                ProtectedValue.RequiresConsent -> return ProtectionResult.RequiresPlaintextSecretFallbackConsent
            }
        }

        val protectedS3Connections = mutableListOf<S3Config>()
        preferences.s3Connections.forEach { config ->
            when (
                val protectedConfig = protectS3Config(
                    config = config,
                    allowPlaintextSecretFallback = allowPlaintextSecretFallback
                )
            ) {
                is ProtectedValue.Success -> protectedS3Connections += protectedConfig.value
                ProtectedValue.RequiresConsent -> return ProtectionResult.RequiresPlaintextSecretFallbackConsent
            }
        }

        return ProtectionResult.Protected(
            preferences.copy(
                sftpConnections = protectedSftpConnections,
                s3Connections = protectedS3Connections
            )
        )
    }

    fun resolveAfterLoad(preferences: UserPreferences): UserPreferences =
        preferences.copy(
            sftpConnections = preferences.sftpConnections.map(::resolveSftpConfig),
            s3Connections = preferences.s3Connections.map(::resolveS3Config)
        )

    fun cleanupRemovedCredentials(previous: UserPreferences, current: UserPreferences) {
        val staleReferences = credentialReferences(previous) - credentialReferences(current)
        staleReferences.forEach { secureCredentialStore.delete(it) }
    }

    private fun credentialReferences(preferences: UserPreferences): Set<CredentialReference> =
        preferences.sftpConnections.flatMap(::credentialReferences).toSet() +
            preferences.s3Connections.flatMap(::credentialReferences).toSet()

    private fun credentialReferences(config: SftpConfig): List<CredentialReference> =
        when (val auth = config.auth) {
            is SftpAuth.Password -> listOf(CredentialReferences.sftpPassword(config.name))
            is SftpAuth.KeyPair -> auth.passphrase
                ?.let { listOf(CredentialReferences.sftpPassphrase(config.name)) }
                ?: emptyList()
        }

    private fun credentialReferences(config: S3Config): List<CredentialReference> =
        when (config.auth) {
            is S3Auth.Explicit -> listOf(CredentialReferences.s3SecretKey(config.name))
            else -> emptyList()
        }

    private fun protectSftpConfig(
        config: SftpConfig,
        allowPlaintextSecretFallback: Boolean
    ): ProtectedValue<SftpConfig> {
        val protectedAuth = when (val auth = config.auth) {
            is SftpAuth.Password -> {
                val reference = CredentialReferences.sftpPassword(config.name)
                when (
                    val protectedPassword = protectSecret(
                        secret = auth.password,
                        reference = reference,
                        allowPlaintextSecretFallback = allowPlaintextSecretFallback
                    )
                ) {
                    is ProtectedValue.Success -> auth.copy(password = protectedPassword.value)
                    ProtectedValue.RequiresConsent -> return ProtectedValue.RequiresConsent
                }
            }
            is SftpAuth.KeyPair -> {
                val passphraseReference = CredentialReferences.sftpPassphrase(config.name)
                when (
                    val protectedPassphrase = protectNullableSecret(
                        secret = auth.passphrase,
                        reference = passphraseReference,
                        allowPlaintextSecretFallback = allowPlaintextSecretFallback
                    )
                ) {
                    is ProtectedValue.Success -> auth.copy(passphrase = protectedPassphrase.value)
                    ProtectedValue.RequiresConsent -> return ProtectedValue.RequiresConsent
                }
            }
        }
        return ProtectedValue.Success(config.copy(auth = protectedAuth))
    }

    private fun resolveSftpConfig(config: SftpConfig): SftpConfig {
        val resolvedAuth = when (val auth = config.auth) {
            is SftpAuth.Password -> {
                val reference = CredentialReferences.sftpPassword(config.name)
                auth.copy(password = resolveSecret(auth.password, reference))
            }
            is SftpAuth.KeyPair -> {
                val passphraseReference = CredentialReferences.sftpPassphrase(config.name)
                auth.copy(passphrase = resolveNullableSecret(auth.passphrase, passphraseReference))
            }
        }
        return config.copy(auth = resolvedAuth)
    }

    private fun protectS3Config(
        config: S3Config,
        allowPlaintextSecretFallback: Boolean
    ): ProtectedValue<S3Config> {
        val protectedAuth = when (val auth = config.auth) {
            is S3Auth.Explicit -> {
                val reference = CredentialReferences.s3SecretKey(config.name)
                when (
                    val protectedSecretKey = protectSecret(
                        secret = auth.secretKey,
                        reference = reference,
                        allowPlaintextSecretFallback = allowPlaintextSecretFallback
                    )
                ) {
                    is ProtectedValue.Success -> auth.copy(secretKey = protectedSecretKey.value)
                    ProtectedValue.RequiresConsent -> return ProtectedValue.RequiresConsent
                }
            }
            else -> auth
        }
        return ProtectedValue.Success(config.copy(auth = protectedAuth))
    }

    private fun resolveS3Config(config: S3Config): S3Config {
        val resolvedAuth = when (val auth = config.auth) {
            is S3Auth.Explicit -> {
                val reference = CredentialReferences.s3SecretKey(config.name)
                auth.copy(secretKey = resolveSecret(auth.secretKey, reference))
            }
            else -> auth
        }
        return config.copy(auth = resolvedAuth)
    }

    private fun protectSecret(
        secret: String,
        reference: CredentialReference,
        allowPlaintextSecretFallback: Boolean
    ): ProtectedValue<String> {
        if (secret.isEmpty() || secret == KEYCHAIN_MARKER) return ProtectedValue.Success(secret)
        if (secureCredentialStore.put(reference, secret)) return ProtectedValue.Success(KEYCHAIN_MARKER)
        return if (allowPlaintextSecretFallback) {
            ProtectedValue.Success(secret)
        } else {
            ProtectedValue.RequiresConsent
        }
    }

    private fun protectNullableSecret(
        secret: String?,
        reference: CredentialReference,
        allowPlaintextSecretFallback: Boolean
    ): ProtectedValue<String?> {
        if (secret == null || secret.isEmpty() || secret == KEYCHAIN_MARKER) return ProtectedValue.Success(secret)
        if (secureCredentialStore.put(reference, secret)) return ProtectedValue.Success(KEYCHAIN_MARKER)
        return if (allowPlaintextSecretFallback) {
            ProtectedValue.Success(secret)
        } else {
            ProtectedValue.RequiresConsent
        }
    }

    private fun resolveSecret(secret: String, reference: CredentialReference): String {
        if (secret != KEYCHAIN_MARKER) return secret
        return secureCredentialStore.get(reference).orEmpty()
    }

    private fun resolveNullableSecret(secret: String?, reference: CredentialReference): String? {
        if (secret == null || secret != KEYCHAIN_MARKER) return secret
        return secureCredentialStore.get(reference)
    }

    companion object {
        internal const val KEYCHAIN_MARKER = "__KLOGVIEWER_KEYCHAIN__"
    }

    sealed interface ProtectionResult {
        data class Protected(val preferences: UserPreferences) : ProtectionResult
        data object RequiresPlaintextSecretFallbackConsent : ProtectionResult
    }

    private sealed interface ProtectedValue<out T> {
        data class Success<T>(val value: T) : ProtectedValue<T>
        data object RequiresConsent : ProtectedValue<Nothing>
    }
}
