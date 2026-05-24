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

@Serializable
sealed interface S3Auth {
    @Serializable
    data object DefaultChain : S3Auth
    @Serializable
    data class Profile(val profileName: String) : S3Auth
    @Serializable
    data class Explicit(val accessKey: String, val secretKey: String, val region: String) : S3Auth
}

@Serializable
data class S3Config(
    val name: String,
    val bucket: String,
    val region: String?,
    val auth: S3Auth,
    val prefix: String = ""
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
        val pathWithSlash = if (isDirectory && !path.endsWith("/")) "$path/" else path
        val effectivePath = if (pathWithSlash.startsWith("/")) pathWithSlash else "/$pathWithSlash"
        val base = "sftp://$username@$host:$port$effectivePath"
        return if (isDirectory) "$base?type=directory" else base
    }
}

data class S3Uri(
    val bucket: String,
    val key: String,
    val isDirectory: Boolean = false
) {
    companion object {
        fun parse(uri: String): S3Uri? {
            if (!uri.startsWith("s3://")) return null
            val parts = uri.removePrefix("s3://").split("/", limit = 2)
            if (parts.isEmpty()) return null
            val bucket = parts[0]
            val rest = if (parts.size > 1) parts[1] else ""
            val isDirectory = rest.endsWith("/") || uri.contains("?type=directory")
            val key = rest.substringBefore("?")
            
            return S3Uri(bucket, key, isDirectory)
        }
    }

    override fun toString(): String {
        val effectiveKey = if (isDirectory && !key.endsWith("/") && key.isNotEmpty()) "$key/" else key
        val base = "s3://$bucket/$effectiveKey"
        return if (isDirectory) "$base?type=directory" else base
    }
}
