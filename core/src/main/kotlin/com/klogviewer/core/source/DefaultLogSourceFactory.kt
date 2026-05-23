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
    private val sshService: SshService = SshService(),
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

    override fun createLocalSource(parser: LogParser): LogSource {
        return FileLogSource(parser, dispatcher = dispatcher)
    }
}
