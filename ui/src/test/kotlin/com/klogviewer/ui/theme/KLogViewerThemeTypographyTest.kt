package com.klogviewer.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@OptIn(ExperimentalTestApi::class)
class KLogViewerThemeTypographyTest {

    @Test
    fun `given app theme when typography is resolved then ui uses sans serif at thirteen points`() = runComposeUiTest {
        var typography: Typography? = null

        setContent {
            KLogViewerTheme {
                typography = MaterialTheme.typography
            }
        }

        waitForIdle()

        val resolvedTypography = requireNotNull(typography)

        expectThat(resolvedTypography.body1.fontFamily).isEqualTo(FontFamily.SansSerif)
        expectThat(resolvedTypography.body2.fontFamily).isEqualTo(FontFamily.SansSerif)
        expectThat(resolvedTypography.subtitle1.fontFamily).isEqualTo(FontFamily.SansSerif)
        expectThat(resolvedTypography.caption.fontFamily).isEqualTo(FontFamily.SansSerif)

        expectThat(resolvedTypography.body1.fontSize).isEqualTo(13.sp)
        expectThat(resolvedTypography.body2.fontSize).isEqualTo(13.sp)
        expectThat(resolvedTypography.subtitle1.fontSize).isEqualTo(13.sp)
        expectThat(resolvedTypography.caption.fontSize).isEqualTo(13.sp)
    }
}
