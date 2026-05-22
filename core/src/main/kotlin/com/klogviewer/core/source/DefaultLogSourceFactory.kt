package com.klogviewer.core.source

import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.LogSourceFactory
import com.klogviewer.domain.repository.RemoteFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultLogSourceFactory(
    private val sshClientProvider: SshClientProvider = DefaultSshClientProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSourceFactory {
    
    override fun createSftpSource(config: SftpConfig, parser: LogParser?): LogSource {
        return SftpLogSource(config, parser ?: SimpleLogParser(), sshClientProvider = sshClientProvider, dispatcher = dispatcher)
    }

    override fun createSftpDirectorySource(config: SftpConfig, remoteFileSystem: RemoteFileSystem): LogSource {
        return SftpDirectoryLogSource(
            config = config,
            remoteFileSystem = remoteFileSystem,
            sshClientProvider = sshClientProvider,
            dispatcher = dispatcher,
            logSourceFactory = { cfg, client ->
                SftpLogSource(cfg, SimpleLogParser(), sshClientProvider = sshClientProvider, existingClient = client, dispatcher = dispatcher)
            }
        )
    }

    override fun createLocalSource(parser: LogParser): LogSource {
        return FileLogSource(parser, dispatcher = dispatcher)
    }
}
