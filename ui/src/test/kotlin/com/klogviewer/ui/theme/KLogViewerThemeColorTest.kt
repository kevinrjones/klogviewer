package com.klogviewer.ui.theme

import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material.MaterialTheme as M2MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.graphics.Color
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@OptIn(ExperimentalTestApi::class)
class KLogViewerThemeColorTest {

    @Test
    fun `given light and dark themes when status colors resolve then semantic tokens stay aligned`() =
        runComposeUiTest {
            var darkCustomColors: CustomColors? = null
            var lightCustomColors: CustomColors? = null
            var darkSurfaceVariant: Color? = null
            var lightSurfaceVariant: Color? = null
            var darkOnPrimary: Color? = null
            var lightOnPrimary: Color? = null

            setContent {
                KLogViewerTheme(darkTheme = true) {
                    darkCustomColors = KLogViewerTheme.customColors
                    darkSurfaceVariant = M3MaterialTheme.colorScheme.surfaceVariant
                    darkOnPrimary = M2MaterialTheme.colors.onPrimary
                }
                KLogViewerTheme(darkTheme = false) {
                    lightCustomColors = KLogViewerTheme.customColors
                    lightSurfaceVariant = M3MaterialTheme.colorScheme.surfaceVariant
                    lightOnPrimary = M2MaterialTheme.colors.onPrimary
                }
            }

            waitForIdle()

            val dark = requireNotNull(darkCustomColors)
            val light = requireNotNull(lightCustomColors)

            expectThat(dark.statusBarError).isEqualTo(KLogViewerColors.DarkStatusBarError)
            expectThat(dark.statusBarDisconnected).isEqualTo(KLogViewerColors.DarkStatusBarDisconnected)
            expectThat(dark.statusBarOnError).isEqualTo(KLogViewerColors.DarkStatusBarOnError)
            expectThat(dark.statusBarOnDisconnected).isEqualTo(KLogViewerColors.DarkStatusBarOnDisconnected)
            expectThat(darkSurfaceVariant).isEqualTo(KLogViewerColors.DarkSurfaceVariant)
            expectThat(darkOnPrimary).isEqualTo(KLogViewerColors.DarkOnPrimary)
            expectThat(light.statusBarError).isEqualTo(KLogViewerColors.LightStatusBarError)
            expectThat(light.statusBarDisconnected).isEqualTo(KLogViewerColors.LightStatusBarDisconnected)
            expectThat(light.statusBarOnError).isEqualTo(KLogViewerColors.LightStatusBarOnError)
            expectThat(light.statusBarOnDisconnected).isEqualTo(KLogViewerColors.LightStatusBarOnDisconnected)
            expectThat(lightSurfaceVariant).isEqualTo(KLogViewerColors.LightSurfaceVariant)
            expectThat(lightOnPrimary).isEqualTo(KLogViewerColors.LightOnPrimary)
        }
}
