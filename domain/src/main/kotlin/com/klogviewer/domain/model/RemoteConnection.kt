package com.klogviewer.domain.model

sealed interface SftpAuth {
    data class Password(val password: String) : SftpAuth
    data class KeyPair(val privateKeyPath: String, val passphrase: String? = null) : SftpAuth
}

data class SftpConfig(
    val host: Host,
    val port: Port = Port(22),
    val username: Username,
    val auth: SftpAuth
)
