package com.klogviewer.domain.model

import com.klogviewer.domain.repository.LocalFileSystem

object DirectoryIdentityNormalizer {

    /**
     * Returns true if the path or URI uses a remote scheme (SFTP or S3).
     */
    fun isRemote(pathOrUri: String): Boolean =
        pathOrUri.startsWith("sftp://") || pathOrUri.startsWith("s3://")

    /**
     * Determines whether the given path or URI represents a directory.
     */
    fun isDirectory(pathOrUri: String, localFileSystem: LocalFileSystem? = null): Boolean {
        return if (isRemote(pathOrUri)) {
            pathOrUri.contains("type=directory") || pathOrUri.endsWith("/")
        } else if (localFileSystem != null) {
            localFileSystem.exists(pathOrUri) && localFileSystem.isDirectory(pathOrUri)
        } else {
            pathOrUri.endsWith("/")
        }
    }

    /**
     * Normalizes a file path or URI into a canonical directory identity key.
     *
     * Format specifications:
     * - Local: "local:/absolute/directory/path" (or "local:C:/path" on Windows)
     * - SFTP: "sftp:username@host:port/directory/path"
     * - S3: "s3:bucket/prefix" (or "s3:bucket" for root)
     *
     * @param pathOrUri The file or directory path / URI (local path, sftp:// URI, or s3:// URI)
     * @param isDirectory Whether the path is known to be a directory. If false, extracts the parent directory.
     */
    fun normalize(pathOrUri: String, isDirectory: Boolean = false): String {
        val trimmed = pathOrUri.trim()
        if (trimmed.isEmpty()) return "local:/"

        return when {
            trimmed.startsWith("sftp://") -> normalizeSftp(trimmed, isDirectory)
            trimmed.startsWith("s3://") -> normalizeS3(trimmed, isDirectory)
            else -> normalizeLocal(trimmed, isDirectory)
        }
    }

    /**
     * Normalizes a file path or URI into a canonical file identity key used for
     * per-file pattern overrides. Uses the same scheme-qualified format as
     * directory keys but keeps the full file path (no parent extraction).
     */
    fun normalizeFile(pathOrUri: String): String = normalize(pathOrUri, isDirectory = true)

    /**
     * Extracts the source type string: "LOCAL", "SFTP", or "S3".
     */
    fun extractSourceType(directoryKey: String): String {
        return when {
            directoryKey.startsWith("sftp:") -> "SFTP"
            directoryKey.startsWith("s3:") -> "S3"
            else -> "LOCAL"
        }
    }

    /**
     * Formats a normalized directory key into a user-friendly display string.
     */
    fun formatDisplay(directoryKey: String): String {
        return when {
            directoryKey.startsWith("local:") -> directoryKey.removePrefix("local:")
            directoryKey.startsWith("sftp:") -> "sftp://${directoryKey.removePrefix("sftp:")}"
            directoryKey.startsWith("s3:") -> "s3://${directoryKey.removePrefix("s3:")}"
            else -> directoryKey
        }
    }

    private fun normalizeLocal(rawPath: String, isDirectory: Boolean): String {
        var clean = rawPath.removePrefix("file://").replace('\\', '/')
        while (clean.contains("//")) {
            clean = clean.replace("//", "/")
        }

        val effectiveDir = if (isDirectory || clean.endsWith("/")) {
            clean.removeSuffix("/")
        } else {
            val lastSlash = clean.lastIndexOf('/')
            if (lastSlash > 0) {
                clean.substring(0, lastSlash)
            } else if (lastSlash == 0) {
                "/"
            } else {
                clean
            }
        }

        val finalizedDir = if (effectiveDir.isEmpty()) "/" else effectiveDir
        return "local:$finalizedDir"
    }

    private fun normalizeSftp(uri: String, isDirectory: Boolean): String {
        val parsed = SftpUri.parse(uri)
        if (parsed == null) {
            val stripped = uri.removePrefix("sftp://").substringBefore("?")
            return "sftp:" + normalizeLocal(stripped, isDirectory).removePrefix("local:")
        }

        val rawPath = parsed.path.replace('\\', '/')
        val effectiveDir = if (isDirectory || parsed.isDirectory || rawPath.endsWith("/")) {
            rawPath.removeSuffix("/")
        } else {
            val lastSlash = rawPath.lastIndexOf('/')
            if (lastSlash > 0) rawPath.substring(0, lastSlash) else if (lastSlash == 0) "/" else rawPath
        }

        val finalizedPath = if (effectiveDir.startsWith("/")) effectiveDir else "/$effectiveDir"
        val cleanPath = if (finalizedPath.length > 1) finalizedPath.removeSuffix("/") else finalizedPath
        return "sftp:${parsed.username}@${parsed.host}:${parsed.port}$cleanPath"
    }

    private fun normalizeS3(uri: String, isDirectory: Boolean): String {
        val parsed = S3Uri.parse(uri)
        if (parsed == null) {
            val stripped = uri.removePrefix("s3://").substringBefore("?")
            val bucket = stripped.substringBefore('/')
            val key = stripped.substringAfter('/', "")
            return normalizeS3Parts(bucket, key, isDirectory)
        }

        return normalizeS3Parts(parsed.bucket, parsed.key, isDirectory || parsed.isDirectory)
    }

    private fun normalizeS3Parts(bucket: String, key: String, isDirectory: Boolean): String {
        val cleanKey = key.trim().removePrefix("/").replace('\\', '/')
        if (cleanKey.isEmpty()) {
            return "s3:$bucket"
        }

        val effectivePrefix = if (isDirectory || cleanKey.endsWith("/")) {
            cleanKey.removeSuffix("/")
        } else {
            val lastSlash = cleanKey.lastIndexOf('/')
            if (lastSlash >= 0) cleanKey.substring(0, lastSlash) else ""
        }

        return if (effectivePrefix.isEmpty()) {
            "s3:$bucket"
        } else {
            "s3:$bucket/$effectivePrefix"
        }
    }
}
