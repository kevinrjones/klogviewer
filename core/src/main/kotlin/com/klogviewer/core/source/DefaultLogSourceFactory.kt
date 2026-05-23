package com.klogviewer.core.source

import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.LogSourceFactory
import com.klogviewer.domain.repository.RemoteFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultLogSourceFactory(
    private val sshService: SshService = SshService(),
    private val s3ClientProvider: S3ClientProvider = S3ClientProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSourceFactory {
    
    override fun createSftpSource(config: SftpConfig, parser: LogParser?): LogSource {
        return SftpLogSource(config, parser ?: SimpleLogParser(), sshService = sshService, dispatcher = dispatcher)
    }

    override fun createSftpDirectorySource(config: SftpConfig, remoteFileSystem: RemoteFileSystem): LogSource {
        return SftpDirectoryLogSource(
            config = config,
            remoteFileSystem = remoteFileSystem,
            sshService = sshService,
            dispatcher = dispatcher,
            logSourceFactory = { cfg, client ->
                SftpLogSource(cfg, SimpleLogParser(), sshService = sshService, existingClient = client, dispatcher = dispatcher)
            }
        )
    }

    override fun createS3Source(config: S3Config, parser: LogParser?): LogSource {
        return S3LogSource(config, parser ?: SimpleLogParser(), s3ClientProvider = s3ClientProvider, dispatcher = dispatcher)
    }

    override fun createS3DirectorySource(config: S3Config, remoteFileSystem: RemoteFileSystem): LogSource {
        return S3DirectoryLogSource(
            config = config,
            remoteFileSystem = remoteFileSystem,
            dispatcher = dispatcher,
            logSourceFactory = { cfg ->
                S3LogSource(cfg, SimpleLogParser(), s3ClientProvider = s3ClientProvider, dispatcher = dispatcher)
            }
        )
    }

    override fun createLocalSource(parser: LogParser): LogSource {
        return FileLogSource(parser, dispatcher = dispatcher)
    }
}
