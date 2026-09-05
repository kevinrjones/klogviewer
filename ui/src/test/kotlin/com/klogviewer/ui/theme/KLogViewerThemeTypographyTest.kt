package com.klogviewer.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Typography as M3Typography
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
    fun `given app theme when typography resolved then hierarchy scale is verified`() = runComposeUiTest {
        var typography: Typography? = null
        var m3Typography: M3Typography? = null

        setContent {
            KLogViewerTheme {
                typography = MaterialTheme.typography
                m3Typography = M3MaterialTheme.typography
            }
        }

        waitForIdle()

        val resolvedTypography = requireNotNull(typography)

        expectThat(resolvedTypography.body1.fontFamily).isEqualTo(FontFamily.SansSerif)
        expectThat(resolvedTypography.body2.fontFamily).isEqualTo(FontFamily.SansSerif)
        expectThat(resolvedTypography.subtitle1.fontFamily).isEqualTo(FontFamily.SansSerif)
        expectThat(resolvedTypography.caption.fontFamily).isEqualTo(FontFamily.SansSerif)

        expectThat(resolvedTypography.h1.fontSize).isEqualTo(20.sp)
        expectThat(resolvedTypography.h2.fontSize).isEqualTo(18.sp)
        expectThat(resolvedTypography.h3.fontSize).isEqualTo(16.sp)
        expectThat(resolvedTypography.subtitle1.fontSize).isEqualTo(14.sp)
        expectThat(resolvedTypography.body1.fontSize).isEqualTo(13.sp)
        expectThat(resolvedTypography.body2.fontSize).isEqualTo(13.sp)
        expectThat(resolvedTypography.caption.fontSize).isEqualTo(11.sp)
        expectThat(resolvedTypography.overline.fontSize).isEqualTo(10.sp)
        expectThat(resolvedTypography.body1.lineHeight).isEqualTo(18.sp)
        expectThat(resolvedTypography.h1.letterSpacing).isEqualTo((-0.2).sp)

        val resolvedM3Typography = requireNotNull(m3Typography)

        expectThat(resolvedM3Typography.titleLarge.fontSize).isEqualTo(16.sp)
        expectThat(resolvedM3Typography.titleMedium.fontSize).isEqualTo(14.sp)
        expectThat(resolvedM3Typography.bodyMedium.fontSize).isEqualTo(13.sp)
        expectThat(resolvedM3Typography.bodySmall.fontSize).isEqualTo(11.sp)
        expectThat(resolvedM3Typography.labelLarge.fontSize).isEqualTo(13.sp)
        expectThat(resolvedM3Typography.labelSmall.fontSize).isEqualTo(10.sp)
        expectThat(resolvedM3Typography.bodyMedium.lineHeight).isEqualTo(18.sp)
    }
}
