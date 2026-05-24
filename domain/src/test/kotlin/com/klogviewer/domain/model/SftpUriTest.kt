package com.klogviewer.domain.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SftpUriTest {

    @Test
    fun `toString should include trailing slash when isDirectory is true`() {
        val sftpUri = SftpUri("user", "host", 22, "/logs", isDirectory = true)
        expectThat(sftpUri.toString()).isEqualTo("sftp://user@host:22/logs/?type=directory")
    }

    @Test
    fun `parse and toString should preserve trailing slash for directories`() {
        val uri = "sftp://user@host:22/logs/?type=directory"
        val parsed = SftpUri.parse(uri)
        
        expectThat(parsed?.isDirectory).isEqualTo(true)
        expectThat(parsed?.toString()).isEqualTo("sftp://user@host:22/logs/?type=directory")
    }
}
