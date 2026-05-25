package com.klogviewer.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LogListColumnWidthTest {

    @Test
    fun `given default message column width when resolved then it is capped at 300`() {
        val width = getColumnWidth("Message", emptyMap())

        expectThat(width).isEqualTo(MAX_DEFAULT_COLUMN_WIDTH.dp)
    }

    @Test
    fun `given user resized message column width over 300 when resolved then resized width is preserved`() {
        val width = getColumnWidth("Message", mapOf("Message" to 450))

        expectThat(width).isEqualTo(450.dp)
    }
}
