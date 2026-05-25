package com.klogviewer.core.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Base64

private val logger = KotlinLogging.logger {}

interface SecureCredentialStore {
    fun put(reference: CredentialReference, secret: String): Boolean
    fun get(reference: CredentialReference): String?
    fun delete(reference: CredentialReference): Boolean
}

data class CredentialReference(val account: String)

object CredentialReferences {
    fun sftpPassword(connectionName: String): CredentialReference =
        CredentialReference(account = "sftp-password:${encode(connectionName)}")

    fun sftpPassphrase(connectionName: String): CredentialReference =
        CredentialReference(account = "sftp-passphrase:${encode(connectionName)}")

    fun s3SecretKey(connectionName: String): CredentialReference =
        CredentialReference(account = "s3-secret-key:${encode(connectionName)}")

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}

class OsKeychainCredentialStore internal constructor(
    private val serviceName: String,
    private val osName: String,
    private val commandExecutor: CommandExecutor
) : SecureCredentialStore {
    constructor(
        serviceName: String = "com.klogviewer.app",
        osName: String = System.getProperty("os.name")
    ) : this(serviceName = serviceName, osName = osName, commandExecutor = ProcessCommandExecutor())
    override fun put(reference: CredentialReference, secret: String): Boolean {
        val success = when (platform()) {
            Platform.MAC -> putMac(reference, secret)
            Platform.LINUX -> putLinux(reference, secret)
            Platform.WINDOWS -> putWindows(reference, secret)
            Platform.UNSUPPORTED -> false
        }
        if (!success) {
            logger.warn { "Failed to write credential '${reference.account}' to OS keychain" }
        }
        return success
    }

    override fun get(reference: CredentialReference): String? {
        val secret = when (platform()) {
            Platform.MAC -> getMac(reference)
            Platform.LINUX -> getLinux(reference)
            Platform.WINDOWS -> getWindows(reference)
            Platform.UNSUPPORTED -> null
        }
        if (secret == null) {
            logger.warn { "Failed to read credential '${reference.account}' from OS keychain" }
        }
        return secret
    }

    override fun delete(reference: CredentialReference): Boolean {
        val success = when (platform()) {
            Platform.MAC -> deleteMac(reference)
            Platform.LINUX -> deleteLinux(reference)
            Platform.WINDOWS -> deleteWindows(reference)
            Platform.UNSUPPORTED -> false
        }
        if (!success) {
            logger.warn { "Failed to delete credential '${reference.account}' from OS keychain" }
        }
        return success
    }

    private fun putMac(reference: CredentialReference, secret: String): Boolean =
        execute(
            command = listOf(
                "security",
                "add-generic-password",
                "-U",
                "-s",
                serviceName,
                "-a",
                reference.account,
                "-w",
                secret
            )
        ).success

    private fun getMac(reference: CredentialReference): String? =
        execute(
            command = listOf(
                "security",
                "find-generic-password",
                "-s",
                serviceName,
                "-a",
                reference.account,
                "-w"
            )
        ).takeIf { it.success }?.output

    private fun deleteMac(reference: CredentialReference): Boolean {
        val result = execute(
            command = listOf(
                "security",
                "delete-generic-password",
                "-s",
                serviceName,
                "-a",
                reference.account
            )
        )
        return result.success || result.output?.contains("could not be found", ignoreCase = true) == true
    }

    private fun putLinux(reference: CredentialReference, secret: String): Boolean =
        execute(
            command = listOf(
                "secret-tool",
                "store",
                "--label=$serviceName",
                "service",
                serviceName,
                "account",
                reference.account
            ),
            standardInput = secret
        ).success

    private fun getLinux(reference: CredentialReference): String? =
        execute(
            command = listOf(
                "secret-tool",
                "lookup",
                "service",
                serviceName,
                "account",
                reference.account
            )
        ).takeIf { it.success }?.output

    private fun deleteLinux(reference: CredentialReference): Boolean {
        val result = execute(
            command = listOf(
                "secret-tool",
                "clear",
                "service",
                serviceName,
                "account",
                reference.account
            )
        )
        return result.success || result.exitCode == 1
    }

    private fun putWindows(reference: CredentialReference, secret: String): Boolean =
        execute(
            command = listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                windowsPutScript(reference, secret)
            )
        ).success

    private fun getWindows(reference: CredentialReference): String? =
        execute(
            command = listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                windowsGetScript(reference)
            )
        ).takeIf { it.success }?.output

    private fun deleteWindows(reference: CredentialReference): Boolean =
        execute(
            command = listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                windowsDeleteScript(reference)
            )
        ).success

    private fun windowsPutScript(reference: CredentialReference, secret: String): String {
        val escapedServiceName = escapePowerShell(serviceName)
        val escapedAccount = escapePowerShell(reference.account)
        val escapedSecret = escapePowerShell(secret)
        return """
            ${'$'}vault = New-Object Windows.Security.Credentials.PasswordVault
            try {
                ${'$'}existing = ${'$'}vault.Retrieve('$escapedServiceName', '$escapedAccount')
                ${'$'}vault.Remove(${'$'}existing)
            } catch {}
            ${'$'}credential = New-Object Windows.Security.Credentials.PasswordCredential('$escapedServiceName', '$escapedAccount', '$escapedSecret')
            ${'$'}vault.Add(${'$'}credential)
        """.trimIndent()
    }

    private fun windowsGetScript(reference: CredentialReference): String {
        val escapedServiceName = escapePowerShell(serviceName)
        val escapedAccount = escapePowerShell(reference.account)
        return """
            ${'$'}vault = New-Object Windows.Security.Credentials.PasswordVault
            try {
                ${'$'}credential = ${'$'}vault.Retrieve('$escapedServiceName', '$escapedAccount')
                ${'$'}credential.RetrievePassword()
                Write-Output ${'$'}credential.Password
            } catch {
                exit 1
            }
        """.trimIndent()
    }

    private fun windowsDeleteScript(reference: CredentialReference): String {
        val escapedServiceName = escapePowerShell(serviceName)
        val escapedAccount = escapePowerShell(reference.account)
        return """
            ${'$'}vault = New-Object Windows.Security.Credentials.PasswordVault
            try {
                ${'$'}credential = ${'$'}vault.Retrieve('$escapedServiceName', '$escapedAccount')
                ${'$'}vault.Remove(${'$'}credential)
            } catch {
                exit 0
            }
        """.trimIndent()
    }

    private fun escapePowerShell(value: String): String = value.replace("'", "''")

    private fun platform(): Platform {
        val platform = osName.lowercase()
        return when {
            platform.contains("mac") -> Platform.MAC
            platform.contains("win") -> Platform.WINDOWS
            platform.contains("linux") || platform.contains("nix") || platform.contains("nux") -> Platform.LINUX
            else -> Platform.UNSUPPORTED
        }
    }

    private fun execute(command: List<String>, standardInput: String? = null): CommandResult =
        commandExecutor.execute(command, standardInput)

    private enum class Platform {
        MAC,
        WINDOWS,
        LINUX,
        UNSUPPORTED
    }

    internal fun interface CommandExecutor {
        fun execute(command: List<String>, standardInput: String?): CommandResult
    }

    internal data class CommandResult(
        val success: Boolean,
        val output: String?,
        val exitCode: Int?
    )

    private class ProcessCommandExecutor : CommandExecutor {
        override fun execute(command: List<String>, standardInput: String?): CommandResult {
            return try {
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                writeStandardInput(process, standardInput)
                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                val exitCode = process.waitFor()
                CommandResult(
                    success = exitCode == 0,
                    output = output.takeIf { it.isNotEmpty() },
                    exitCode = exitCode
                )
            } catch (e: Exception) {
                logger.error(e) { "OS keychain command failed" }
                CommandResult(success = false, output = null, exitCode = null)
            }
        }

        private fun writeStandardInput(process: Process, standardInput: String?) {
            if (standardInput == null) {
                process.outputStream.close()
                return
            }
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(standardInput)
            }
        }
    }
}
