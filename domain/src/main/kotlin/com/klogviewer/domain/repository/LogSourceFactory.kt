package com.klogviewer.domain.repository

import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.parser.LogParser

interface LogSourceFactory {
    fun createSftpSource(config: SftpConfig, parser: LogParser? = null): LogSource
    fun createSftpDirectorySource(config: SftpConfig, remoteFileSystem: RemoteFileSystem): LogSource
    fun createLocalSource(parser: LogParser): LogSource
}
