package com.klogviewer.core.source

import net.schmizz.sshj.SSHClient

interface SshClientProvider {
    fun createClient(): SSHClient
}

class DefaultSshClientProvider : SshClientProvider {
    override fun createClient(): SSHClient = SSHClient()
}
