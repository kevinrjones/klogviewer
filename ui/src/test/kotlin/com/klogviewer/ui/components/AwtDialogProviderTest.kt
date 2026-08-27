package com.klogviewer.ui.components

import org.junit.Test
import strikt.api.expectCatching
import strikt.assertions.isSuccess

class AwtDialogProviderTest {

    @Test
    fun `given awt dialog provider when show message dialog called then invocation succeeds`() {
        val provider = AwtDialogProvider()

        expectCatching {
            provider.showMessageDialog("Test Title", "Test Error Message")
        }.isSuccess()
    }
}
