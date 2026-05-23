package com.klogviewer.core.source

import com.klogviewer.domain.model.SftpConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import net.schmizz.sshj.SSHClient

private val logger = KotlinLogging.logger {}

class SshClientPool(
    private val config: SftpConfig,
    private val sshService: SshService,
    private val maxSessionsPerClient: Int = 8
) {
    private val clients = mutableListOf<Pair<SSHClient, Int>>()

    suspend fun getOrCreateClient(): SSHClient {
        synchronized(clients) {
            val available = clients.find { 
                it.second < maxSessionsPerClient && it.first.isConnected && it.first.isAuthenticated 
            }
            if (available != null) {
                val index = clients.indexOf(available)
                clients[index] = available.first to available.second + 1
                return available.first
            }
        }

        val client = sshService.connectAndAuthenticate(config)
        
        synchronized(clients) {
            clients.add(client to 1)
        }
        return client
    }

    fun releaseClient(client: SSHClient) {
        synchronized(clients) {
            val entry = clients.find { it.first == client }
            if (entry != null) {
                val index = clients.indexOf(entry)
                if (entry.second > 1) {
                    clients[index] = entry.first to entry.second - 1
                } else {
                    clients.removeAt(index)
                    try { client.disconnect(); client.close() } catch (_: Exception) {}
                }
            }
        }
    }

    fun close() {
        synchronized(clients) {
            clients.forEach {
                try { it.first.disconnect(); it.first.close() } catch (_: Exception) {}
            }
            clients.clear()
        }
    }
}
