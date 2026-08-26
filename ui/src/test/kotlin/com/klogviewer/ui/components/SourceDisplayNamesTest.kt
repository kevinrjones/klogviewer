package com.klogviewer.ui.components

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SourceDisplayNamesTest {

    @Test
    fun `given unique file names then short names are the file names`() {
        val names = buildSourceDisplayNames(listOf("/var/log/app.log", "/var/log/access.log"))

        expectThat(names["/var/log/app.log"]).isEqualTo("app.log")
        expectThat(names["/var/log/access.log"]).isEqualTo("access.log")
    }

    @Test
    fun `given duplicate file names then short names are disambiguated with parent directory`() {
        val names = buildSourceDisplayNames(
            listOf("/services/api/application.log", "/services/web/application.log")
        )

        expectThat(names["/services/api/application.log"]).isEqualTo("api/application.log")
        expectThat(names["/services/web/application.log"]).isEqualTo("web/application.log")
    }

    @Test
    fun `given remote uris then short names use the file name`() {
        val names = buildSourceDisplayNames(
            listOf("sftp://admin@host:22/var/log/remote.log", "/var/log/local.log")
        )

        expectThat(names["sftp://admin@host:22/var/log/remote.log"]).isEqualTo("remote.log")
        expectThat(names["/var/log/local.log"]).isEqualTo("local.log")
    }
}
