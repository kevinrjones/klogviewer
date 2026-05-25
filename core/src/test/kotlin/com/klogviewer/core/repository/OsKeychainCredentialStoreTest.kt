package com.klogviewer.core.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OsKeychainCredentialStoreTest {
    @Test
    fun `should use macos security commands for put get and delete`() {
        val executor = RecordingCommandExecutor(
            mutableListOf(
                OsKeychainCredentialStore.CommandResult(success = true, output = null, exitCode = 0),
                OsKeychainCredentialStore.CommandResult(success = true, output = "mac-secret", exitCode = 0),
                OsKeychainCredentialStore.CommandResult(success = true, output = null, exitCode = 0)
            )
        )
        val store = OsKeychainCredentialStore(
            serviceName = "com.klogviewer.app",
            osName = "Mac OS X",
            commandExecutor = executor
        )
        val reference = CredentialReference("mac-account")

        assertTrue(store.put(reference, "secret-value"))
        assertEquals("mac-secret", store.get(reference))
        assertTrue(store.delete(reference))

        assertEquals(
            listOf(
                "security",
                "add-generic-password",
                "-U",
                "-s",
                "com.klogviewer.app",
                "-a",
                "mac-account",
                "-w",
                "secret-value"
            ),
            executor.invocations[0].command
        )
        assertNull(executor.invocations[0].standardInput)

        assertEquals(
            listOf(
                "security",
                "find-generic-password",
                "-s",
                "com.klogviewer.app",
                "-a",
                "mac-account",
                "-w"
            ),
            executor.invocations[1].command
        )

        assertEquals(
            listOf(
                "security",
                "delete-generic-password",
                "-s",
                "com.klogviewer.app",
                "-a",
                "mac-account"
            ),
            executor.invocations[2].command
        )
    }

    @Test
    fun `should use linux secret-tool commands for put get and delete`() {
        val executor = RecordingCommandExecutor(
            mutableListOf(
                OsKeychainCredentialStore.CommandResult(success = true, output = null, exitCode = 0),
                OsKeychainCredentialStore.CommandResult(success = true, output = "linux-secret", exitCode = 0),
                OsKeychainCredentialStore.CommandResult(success = false, output = null, exitCode = 1)
            )
        )
        val store = OsKeychainCredentialStore(
            serviceName = "com.klogviewer.app",
            osName = "Linux",
            commandExecutor = executor
        )
        val reference = CredentialReference("linux-account")

        assertTrue(store.put(reference, "linux-secret-value"))
        assertEquals("linux-secret", store.get(reference))
        assertTrue(store.delete(reference))

        assertEquals(
            listOf(
                "secret-tool",
                "store",
                "--label=com.klogviewer.app",
                "service",
                "com.klogviewer.app",
                "account",
                "linux-account"
            ),
            executor.invocations[0].command
        )
        assertEquals("linux-secret-value", executor.invocations[0].standardInput)

        assertEquals(
            listOf(
                "secret-tool",
                "lookup",
                "service",
                "com.klogviewer.app",
                "account",
                "linux-account"
            ),
            executor.invocations[1].command
        )

        assertEquals(
            listOf(
                "secret-tool",
                "clear",
                "service",
                "com.klogviewer.app",
                "account",
                "linux-account"
            ),
            executor.invocations[2].command
        )
    }

    @Test
    fun `should use windows powershell commands for put get and delete`() {
        val executor = RecordingCommandExecutor(
            mutableListOf(
                OsKeychainCredentialStore.CommandResult(success = true, output = null, exitCode = 0),
                OsKeychainCredentialStore.CommandResult(success = true, output = "windows-secret", exitCode = 0),
                OsKeychainCredentialStore.CommandResult(success = true, output = null, exitCode = 0)
            )
        )
        val store = OsKeychainCredentialStore(
            serviceName = "com.klogviewer.app",
            osName = "Windows 11",
            commandExecutor = executor
        )
        val reference = CredentialReference("win-account's")

        assertTrue(store.put(reference, "pa'ss"))
        assertEquals("windows-secret", store.get(reference))
        assertTrue(store.delete(reference))

        assertEquals(listOf("powershell", "-NoProfile", "-NonInteractive", "-Command"), executor.invocations[0].command.take(4))
        assertTrue(executor.invocations[0].command.last().contains("PasswordVault"))
        assertTrue(
            executor.invocations[0].command.last().contains(
                "PasswordCredential('com.klogviewer.app', 'win-account''s', 'pa''ss')"
            )
        )

        assertEquals(listOf("powershell", "-NoProfile", "-NonInteractive", "-Command"), executor.invocations[1].command.take(4))
        assertTrue(executor.invocations[1].command.last().contains("Retrieve('com.klogviewer.app', 'win-account''s')"))

        assertEquals(listOf("powershell", "-NoProfile", "-NonInteractive", "-Command"), executor.invocations[2].command.take(4))
        assertTrue(executor.invocations[2].command.last().contains("Retrieve('com.klogviewer.app', 'win-account''s')"))
    }

    @Test
    fun `should return unsupported responses when platform is unknown`() {
        val executor = RecordingCommandExecutor(mutableListOf())
        val store = OsKeychainCredentialStore(
            serviceName = "com.klogviewer.app",
            osName = "Solaris",
            commandExecutor = executor
        )
        val reference = CredentialReference("unknown-account")

        assertFalse(store.put(reference, "value"))
        assertNull(store.get(reference))
        assertFalse(store.delete(reference))
        assertTrue(executor.invocations.isEmpty())
    }

    private class RecordingCommandExecutor(
        private val results: MutableList<OsKeychainCredentialStore.CommandResult>
    ) : OsKeychainCredentialStore.CommandExecutor {
        val invocations = mutableListOf<CommandInvocation>()

        override fun execute(command: List<String>, standardInput: String?): OsKeychainCredentialStore.CommandResult {
            invocations += CommandInvocation(command, standardInput)
            if (results.isEmpty()) {
                return OsKeychainCredentialStore.CommandResult(success = false, output = null, exitCode = null)
            }
            return results.removeAt(0)
        }
    }

    private data class CommandInvocation(
        val command: List<String>,
        val standardInput: String?
    )
}
