package com.klogviewer.core.source

import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.S3Auth
import com.klogviewer.domain.model.S3Config
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import arrow.core.right

class S3DirectoryDetectionTest {

    @Test
    fun `isS3Directory should return true if multiple objects are found`() = runTest {
        val config = S3Config("test", "bucket", "us-east-1", S3Auth.DefaultChain)
        val mockFs = mockk<S3FileSystem>()
        
        coEvery { mockFs.listS3Objects(any(), "logs") } returns listOf(
            RemoteFile("app.log", "logs/app.log", false, 100, 0),
            RemoteFile("app2.log", "logs/app2.log", false, 100, 0)
        ).right()
        
        coEvery { mockFs.isS3Directory(any(), any()) } answers { callOriginal() }

        expectThat(mockFs.isS3Directory(config, "logs")).isEqualTo(true)
    }

    @Test
    fun `isS3Directory should return true if one object is found with different path`() = runTest {
        val config = S3Config("test", "bucket", "us-east-1", S3Auth.DefaultChain)
        val mockFs = mockk<S3FileSystem>()
        
        coEvery { mockFs.listS3Objects(any(), "logs") } returns listOf(
            RemoteFile("app.log", "logs/app.log", false, 100, 0)
        ).right()
        
        coEvery { mockFs.isS3Directory(any(), any()) } answers { callOriginal() }

        expectThat(mockFs.isS3Directory(config, "logs")).isEqualTo(true)
    }

    @Test
    fun `isS3Directory should return true if one object is found with trailing slash and prefix does not have it`() = runTest {
        val config = S3Config("test", "bucket", "us-east-1", S3Auth.DefaultChain)
        val mockFs = mockk<S3FileSystem>()
        
        coEvery { mockFs.listS3Objects(any(), "logs") } returns listOf(
            RemoteFile("logs", "logs/", true, 0, 0)
        ).right()
        
        coEvery { mockFs.isS3Directory(any(), any()) } answers { callOriginal() }

        expectThat(mockFs.isS3Directory(config, "logs")).isEqualTo(true)
    }

    @Test
    fun `isS3Directory should return false if one object is found with exact path and no trailing slash`() = runTest {
        val config = S3Config("test", "bucket", "us-east-1", S3Auth.DefaultChain)
        val mockFs = mockk<S3FileSystem>()
        
        coEvery { mockFs.listS3Objects(any(), "logs/app.log") } returns listOf(
            RemoteFile("app.log", "logs/app.log", false, 100, 0)
        ).right()
        
        coEvery { mockFs.isS3Directory(any(), any()) } answers { callOriginal() }

        expectThat(mockFs.isS3Directory(config, "logs/app.log")).isEqualTo(false)
    }
}
