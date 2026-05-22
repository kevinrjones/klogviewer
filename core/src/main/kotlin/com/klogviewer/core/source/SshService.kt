package com.klogviewer.core.source

import com.klogviewer.core.util.withRetry
import com.klogviewer.domain.model.SftpAuth
import com.klogviewer.domain.model.SftpConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

class SshService(
    private val sshClientProvider: SshClientProvider = DefaultSshClientProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun connectAndAuthenticate(config: SftpConfig): SSHClient = withRetry(maxRetries = 3) {
        withContext(dispatcher) {
            val client = sshClientProvider.createClient()
            client.addHostKeyVerifier(PromiscuousVerifier())
            client.connect(config.host.value, config.port.value)

            try {
                when (val auth = config.auth) {
                    is SftpAuth.Password -> client.authPassword(config.username.value, auth.password)
                    is SftpAuth.KeyPair -> {
                        val keyProvider = if (!auth.passphrase.isNullOrBlank()) {
                            client.loadKeys(auth.privateKeyPath, auth.passphrase)
                        } else {
                            client.loadKeys(auth.privateKeyPath)
                        }
                        client.authPublickey(config.username.value, keyProvider)
                    }
                }
                client
            } catch (e: Exception) {
                try { client.disconnect(); client.close() } catch (_: Exception) {}
                throw e
            }
        }
    }
}
