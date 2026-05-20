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

data class SftpUri(
    val username: String,
    val host: String,
    val port: Int,
    val path: String,
    val isDirectory: Boolean = false
) {
    companion object {
        fun parse(uri: String): SftpUri? {
            if (!uri.startsWith("sftp://")) return null
            val regex = Regex("sftp://([^@]+)@([^:]+):(\\d+)([^?]*)(.*)")
            val match = regex.matchEntire(uri) ?: return null
            val rawPath = match.groupValues[4]
            val query = match.groupValues[5]
            
            return SftpUri(
                username = match.groupValues[1],
                host = match.groupValues[2],
                port = match.groupValues[3].toInt(),
                path = rawPath,
                isDirectory = query == "?type=directory"
            )
        }
    }

    override fun toString(): String {
        val base = "sftp://$username@$host:$port$path"
        return if (isDirectory) "$base?type=directory" else base
    }
}
