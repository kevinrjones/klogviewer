package com.klogviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class LogLevelColors(
    val debug: Color,
    val info: Color,
    val warn: Color,
    val error: Color,
    val fatal: Color,
    val unknown: Color
)

data class CustomColors(
    val tabBackground: Color
)

val LocalLogLevelColors = staticCompositionLocalOf {
    LogLevelColors(
        debug = Color.Gray,
        info = Color.Blue,
        warn = Color.Yellow,
        error = Color.Red,
        fatal = Color.Magenta,
        unknown = Color.Black
    )
}

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        tabBackground = Color.LightGray
    )
}

@Composable
fun KLogViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors(
            primary = KLogViewerColors.DarkPrimary,
            primaryVariant = KLogViewerColors.DarkPrimary.copy(alpha = 0.8f),
            secondary = KLogViewerColors.DarkPrimary,
            background = KLogViewerColors.DarkBackground,
            surface = KLogViewerColors.DarkSurface,
            onPrimary = KLogViewerColors.DarkOnPrimary,
            onBackground = KLogViewerColors.DarkOnBackground,
            onSurface = KLogViewerColors.DarkOnSurface
        )
    } else {
        lightColors(
            primary = KLogViewerColors.LightPrimary,
            primaryVariant = KLogViewerColors.LightPrimary.copy(alpha = 0.8f),
            secondary = KLogViewerColors.LightPrimary,
            background = KLogViewerColors.LightBackground,
            surface = KLogViewerColors.LightSurface,
            onPrimary = KLogViewerColors.LightOnPrimary,
            onBackground = KLogViewerColors.LightOnBackground,
            onSurface = KLogViewerColors.LightOnSurface
        )
    }

    val logLevelColors = if (darkTheme) {
        LogLevelColors(
            debug = KLogViewerColors.DarkDebug,
            info = KLogViewerColors.DarkInfo,
            warn = KLogViewerColors.DarkWarn,
            error = KLogViewerColors.DarkError,
            fatal = KLogViewerColors.DarkFatal,
            unknown = Color.Gray
        )
    } else {
        LogLevelColors(
            debug = KLogViewerColors.LightDebug,
            info = KLogViewerColors.LightInfo,
            warn = KLogViewerColors.LightWarn,
            error = KLogViewerColors.LightError,
            fatal = KLogViewerColors.LightFatal,
            unknown = Color.Black
        )
    }

    val customColors = if (darkTheme) {
        CustomColors(
            tabBackground = KLogViewerColors.DarkTabBackground
        )
    } else {
        CustomColors(
            tabBackground = KLogViewerColors.LightTabBackground
        )
    }

    val typography = Typography(
        body1 = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        subtitle1 = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        ),
        caption = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp
        )
    )

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = Shapes(),
        content = {
            CompositionLocalProvider(
                LocalLogLevelColors provides logLevelColors,
                LocalCustomColors provides customColors,
                content = content
            )
        }
    )
}

object KLogViewerTheme {
    val logColors: LogLevelColors
        @Composable
        get() = LocalLogLevelColors.current

    val customColors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
