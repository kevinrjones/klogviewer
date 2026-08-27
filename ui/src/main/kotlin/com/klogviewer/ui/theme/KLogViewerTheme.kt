package com.klogviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.Shapes as M3Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val UI_FONT_FAMILY = FontFamily.SansSerif

private fun uiTextStyle(
    size: TextUnit = 13.sp,
    weight: FontWeight = FontWeight.Normal
): TextStyle {
    return TextStyle(
        fontFamily = UI_FONT_FAMILY,
        fontWeight = weight,
        fontSize = size
    )
}

data class LogLevelColors(
    val trace: Color,
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
        trace = Color.LightGray,
        debug = Color.DarkGray,
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
            trace = KLogViewerColors.DarkTrace,
            debug = KLogViewerColors.DarkDebug,
            info = KLogViewerColors.DarkInfo,
            warn = KLogViewerColors.DarkWarn,
            error = KLogViewerColors.DarkError,
            fatal = KLogViewerColors.DarkFatal,
            unknown = Color.Gray
        )
    } else {
        LogLevelColors(
            trace = KLogViewerColors.LightTrace,
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
        h1 = uiTextStyle(size = 20.sp, weight = FontWeight.Bold),
        h2 = uiTextStyle(size = 18.sp, weight = FontWeight.Bold),
        h3 = uiTextStyle(size = 16.sp, weight = FontWeight.Bold),
        h4 = uiTextStyle(size = 15.sp, weight = FontWeight.Bold),
        h5 = uiTextStyle(size = 14.sp, weight = FontWeight.Bold),
        h6 = uiTextStyle(size = 13.sp, weight = FontWeight.Bold),
        subtitle1 = uiTextStyle(size = 14.sp, weight = FontWeight.Medium),
        subtitle2 = uiTextStyle(size = 13.sp, weight = FontWeight.Medium),
        body1 = uiTextStyle(size = 13.sp, weight = FontWeight.Normal),
        body2 = uiTextStyle(size = 13.sp, weight = FontWeight.Normal),
        button = uiTextStyle(size = 13.sp, weight = FontWeight.Medium),
        caption = uiTextStyle(size = 11.sp, weight = FontWeight.Normal),
        overline = uiTextStyle(size = 10.sp, weight = FontWeight.Medium)
    )

    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = KLogViewerColors.DarkPrimary,
            onPrimary = KLogViewerColors.DarkOnPrimary,
            background = KLogViewerColors.DarkBackground,
            onBackground = KLogViewerColors.DarkOnBackground,
            surface = KLogViewerColors.DarkSurface,
            onSurface = KLogViewerColors.DarkOnSurface,
            surfaceVariant = Color(0xFF323537)
        )
    } else {
        lightColorScheme(
            primary = KLogViewerColors.LightPrimary,
            onPrimary = KLogViewerColors.LightOnPrimary,
            background = KLogViewerColors.LightBackground,
            onBackground = KLogViewerColors.LightOnBackground,
            surface = KLogViewerColors.LightSurface,
            onSurface = KLogViewerColors.LightOnSurface,
            surfaceVariant = Color(0xFFEBEBEB)
        )
    }

    val m3Shapes = M3Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(4.dp),
        extraLarge = RoundedCornerShape(4.dp)
    )

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(4.dp)
        ),
        content = {
            androidx.compose.material3.MaterialTheme(
                colorScheme = m3ColorScheme,
                shapes = m3Shapes
            ) {
                CompositionLocalProvider(
                    LocalLogLevelColors provides logLevelColors,
                    LocalCustomColors provides customColors,
                    content = content
                )
            }
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
