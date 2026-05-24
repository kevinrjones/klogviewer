package com.klogviewer.domain.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class S3UriTest {

    @Test
    fun `parse and toString should preserve trailing slash for directories`() {
        val uri = "s3://bucket/logs/"
        val parsed = S3Uri.parse(uri)
        
        expectThat(parsed?.isDirectory).isEqualTo(true)
        expectThat(parsed?.key).isEqualTo("logs/")
        
        expectThat(parsed?.toString()).isEqualTo("s3://bucket/logs/?type=directory")
    }

    @Test
    fun `toString should include trailing slash when isDirectory is true`() {
        val s3Uri = S3Uri("bucket", "logs", isDirectory = true)
        expectThat(s3Uri.toString()).isEqualTo("s3://bucket/logs/?type=directory")
    }

    @Test
    fun `parse should handle uri with trailing slash and no query param`() {
        val uri = "s3://bucket/logs/"
        val parsed = S3Uri.parse(uri)
        expectThat(parsed?.isDirectory).isEqualTo(true)
        expectThat(parsed?.key).isEqualTo("logs/")
    }

    @Test
    fun `parse should handle uri with no trailing slash and no query param`() {
        val uri = "s3://bucket/logs"
        val parsed = S3Uri.parse(uri)
        expectThat(parsed?.isDirectory).isEqualTo(false)
        expectThat(parsed?.key).isEqualTo("logs")
    }

    @Test
    fun `parse should handle uri with query param and no trailing slash`() {
        val uri = "s3://bucket/logs?type=directory"
        val parsed = S3Uri.parse(uri)
        expectThat(parsed?.isDirectory).isEqualTo(true)
        expectThat(parsed?.key).isEqualTo("logs")
        expectThat(parsed?.toString()).isEqualTo("s3://bucket/logs/?type=directory")
    }
}
