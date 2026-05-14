package com.logviewer.ui.theme

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
fun LogViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors(
            primary = LogViewerColors.DarkPrimary,
            primaryVariant = LogViewerColors.DarkPrimary.copy(alpha = 0.8f),
            secondary = LogViewerColors.DarkPrimary,
            background = LogViewerColors.DarkBackground,
            surface = LogViewerColors.DarkSurface,
            onPrimary = LogViewerColors.DarkOnPrimary,
            onBackground = LogViewerColors.DarkOnBackground,
            onSurface = LogViewerColors.DarkOnSurface
        )
    } else {
        lightColors(
            primary = LogViewerColors.LightPrimary,
            primaryVariant = LogViewerColors.LightPrimary.copy(alpha = 0.8f),
            secondary = LogViewerColors.LightPrimary,
            background = LogViewerColors.LightBackground,
            surface = LogViewerColors.LightSurface,
            onPrimary = LogViewerColors.LightOnPrimary,
            onBackground = LogViewerColors.LightOnBackground,
            onSurface = LogViewerColors.LightOnSurface
        )
    }

    val logLevelColors = if (darkTheme) {
        LogLevelColors(
            debug = LogViewerColors.DarkDebug,
            info = LogViewerColors.DarkInfo,
            warn = LogViewerColors.DarkWarn,
            error = LogViewerColors.DarkError,
            fatal = LogViewerColors.DarkFatal,
            unknown = Color.Gray
        )
    } else {
        LogLevelColors(
            debug = LogViewerColors.LightDebug,
            info = LogViewerColors.LightInfo,
            warn = LogViewerColors.LightWarn,
            error = LogViewerColors.LightError,
            fatal = LogViewerColors.LightFatal,
            unknown = Color.Black
        )
    }

    val customColors = if (darkTheme) {
        CustomColors(
            tabBackground = LogViewerColors.DarkTabBackground
        )
    } else {
        CustomColors(
            tabBackground = LogViewerColors.LightTabBackground
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

object LogViewerTheme {
    val logColors: LogLevelColors
        @Composable
        get() = LocalLogLevelColors.current

    val customColors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
