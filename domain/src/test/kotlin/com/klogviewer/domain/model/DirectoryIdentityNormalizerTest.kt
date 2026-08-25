package com.klogviewer.domain.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DirectoryIdentityNormalizerTest {

    @Test
    fun `should normalize local file paths to parent directory`() {
        val file1 = "/var/log/application.log"
        expectThat(DirectoryIdentityNormalizer.normalize(file1)).isEqualTo("local:/var/log")

        val file2 = "/var/log/subdir/nested.log"
        expectThat(DirectoryIdentityNormalizer.normalize(file2)).isEqualTo("local:/var/log/subdir")

        val file3 = "C:\\Users\\admin\\logs\\app.log"
        expectThat(DirectoryIdentityNormalizer.normalize(file3)).isEqualTo("local:C:/Users/admin/logs")

        val fileWithScheme = "file:///var/log/app.log"
        expectThat(DirectoryIdentityNormalizer.normalize(fileWithScheme)).isEqualTo("local:/var/log")
    }

    @Test
    fun `should normalize local directory paths`() {
        val dir1 = "/var/log"
        expectThat(DirectoryIdentityNormalizer.normalize(dir1, isDirectory = true)).isEqualTo("local:/var/log")

        val dir2 = "/var/log/"
        expectThat(DirectoryIdentityNormalizer.normalize(dir2, isDirectory = true)).isEqualTo("local:/var/log")

        val root = "/"
        expectThat(DirectoryIdentityNormalizer.normalize(root, isDirectory = true)).isEqualTo("local:/")
    }

    @Test
    fun `should normalize sftp uris`() {
        val sftpFile = "sftp://admin@192.168.1.50:22/var/log/syslog.log"
        expectThat(DirectoryIdentityNormalizer.normalize(sftpFile)).isEqualTo("sftp:admin@192.168.1.50:22/var/log")

        val sftpDir = "sftp://admin@192.168.1.50:22/var/log/?type=directory"
        expectThat(DirectoryIdentityNormalizer.normalize(sftpDir)).isEqualTo("sftp:admin@192.168.1.50:22/var/log")

        val sftpDirSlash = "sftp://admin@192.168.1.50:22/var/log/"
        val normalizedSftpDir = DirectoryIdentityNormalizer.normalize(sftpDirSlash, isDirectory = true)
        expectThat(normalizedSftpDir).isEqualTo("sftp:admin@192.168.1.50:22/var/log")
    }

    @Test
    fun `should normalize s3 uris`() {
        val s3File = "s3://my-prod-bucket/logs/2026/08/app.log"
        expectThat(DirectoryIdentityNormalizer.normalize(s3File)).isEqualTo("s3:my-prod-bucket/logs/2026/08")

        val s3Dir = "s3://my-prod-bucket/logs/2026/?type=directory"
        expectThat(DirectoryIdentityNormalizer.normalize(s3Dir)).isEqualTo("s3:my-prod-bucket/logs/2026")

        val s3Root = "s3://my-prod-bucket/app.log"
        expectThat(DirectoryIdentityNormalizer.normalize(s3Root)).isEqualTo("s3:my-prod-bucket")

        val s3BucketOnly = "s3://my-prod-bucket"
        val normalizedS3Bucket = DirectoryIdentityNormalizer.normalize(s3BucketOnly, isDirectory = true)
        expectThat(normalizedS3Bucket).isEqualTo("s3:my-prod-bucket")
    }

    @Test
    fun `should extract source type correctly`() {
        expectThat(DirectoryIdentityNormalizer.extractSourceType("local:/var/log")).isEqualTo("LOCAL")
        expectThat(DirectoryIdentityNormalizer.extractSourceType("sftp:admin@host:22/var/log")).isEqualTo("SFTP")
        expectThat(DirectoryIdentityNormalizer.extractSourceType("s3:my-bucket/logs")).isEqualTo("S3")
    }

    @Test
    fun `should format display keys correctly`() {
        expectThat(DirectoryIdentityNormalizer.formatDisplay("local:/var/log")).isEqualTo("/var/log")
        val sftpFormatted = DirectoryIdentityNormalizer.formatDisplay("sftp:admin@host:22/var/log")
        expectThat(sftpFormatted).isEqualTo("sftp://admin@host:22/var/log")
        expectThat(DirectoryIdentityNormalizer.formatDisplay("s3:my-bucket/logs")).isEqualTo("s3://my-bucket/logs")
    }
}
