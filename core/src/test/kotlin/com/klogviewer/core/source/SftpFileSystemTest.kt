package com.klogviewer.core.source

import arrow.core.getOrElse
import com.klogviewer.domain.model.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class SftpFileSystemTest {

    @Test
    fun `should list remote files using mocked SFTP client`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/var/log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockSftp = mockk<SFTPClient>(relaxed = true)
        
        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.newSFTPClient() } returns mockSftp
        
        val mockEntry1 = mockk<RemoteResourceInfo> {
            every { name } returns "file1.log"
            every { isDirectory } returns false
            every { attributes.size } returns 1024L
            every { attributes.mtime } returns 123456789L
        }
        val mockEntry2 = mockk<RemoteResourceInfo> {
            every { name } returns "subdir"
            every { isDirectory } returns true
            every { attributes.size } returns 4096L
            every { attributes.mtime } returns 123456790L
        }
        
        every { mockSftp.ls("/var/log") } returns listOf(mockEntry1, mockEntry2)
        
        val fileSystem = SftpFileSystem(SshService(provider))

        // Act
        val result = fileSystem.listFiles(config, "/var/log")

        // Assert
        val files = result.getOrElse { emptyList() }
        expectThat(files).hasSize(2)
        
        val file1 = files.find { it.name == "file1.log" }!!
        expectThat(file1.path).isEqualTo("/var/log/file1.log")
        expectThat(file1.isDirectory).isEqualTo(false)
        expectThat(file1.size).isEqualTo(1024L)
        
        val dir1 = files.find { it.name == "subdir" }!!
        expectThat(dir1.path).isEqualTo("/var/log/subdir")
        expectThat(dir1.isDirectory).isEqualTo(true)
        Unit
    }
}
