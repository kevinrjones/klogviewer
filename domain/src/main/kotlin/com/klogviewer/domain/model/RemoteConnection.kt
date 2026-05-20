package com.klogviewer.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface SftpAuth {
    @Serializable
    data class Password(val password: String) : SftpAuth
    @Serializable
    data class KeyPair(val privateKeyPath: String, val passphrase: String? = null) : SftpAuth
}

@Serializable
data class SftpConfig(
    val name: String,
    val host: Host,
    val port: Port = Port(22),
    val username: Username,
    val auth: SftpAuth,
    val logFilePath: String = ""
)
