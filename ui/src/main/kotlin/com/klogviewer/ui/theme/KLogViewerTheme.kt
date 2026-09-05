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
    weight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified
): TextStyle {
    return TextStyle(
        fontFamily = UI_FONT_FAMILY,
        fontWeight = weight,
        fontSize = size,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing
    )
}

private val UI_DISPLAY_PRIMARY = uiTextStyle(
    size = 20.sp,
    weight = FontWeight.Bold,
    lineHeight = 24.sp,
    letterSpacing = (-0.2).sp
)
private val UI_DISPLAY_SECONDARY = uiTextStyle(
    size = 18.sp,
    weight = FontWeight.Bold,
    lineHeight = 22.sp,
    letterSpacing = (-0.1).sp
)
private val UI_SECTION_TITLE = uiTextStyle(
    size = 16.sp,
    weight = FontWeight.Bold,
    lineHeight = 20.sp
)
private val UI_SUBTITLE_PRIMARY = uiTextStyle(
    size = 14.sp,
    weight = FontWeight.Medium,
    lineHeight = 18.sp
)
private val UI_SUBTITLE_SECONDARY = uiTextStyle(
    size = 13.sp,
    weight = FontWeight.Medium,
    lineHeight = 17.sp
)
private val UI_BODY = uiTextStyle(
    size = 13.sp,
    lineHeight = 18.sp
)
private val UI_METADATA = uiTextStyle(
    size = 11.sp,
    lineHeight = 15.sp
)
private val UI_ACTION = uiTextStyle(
    size = 13.sp,
    weight = FontWeight.Medium,
    lineHeight = 16.sp,
    letterSpacing = 0.1.sp
)
private val UI_METADATA_ACTION = uiTextStyle(
    size = 11.sp,
    weight = FontWeight.Medium,
    lineHeight = 14.sp,
    letterSpacing = 0.1.sp
)
private val UI_OVERLINE = uiTextStyle(
    size = 10.sp,
    weight = FontWeight.Medium,
    lineHeight = 12.sp,
    letterSpacing = 0.5.sp
)

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
    val contentBackground: Color,
    val toolbarSurface: Color,
    val selectedRow: Color,
    val tabBackground: Color,
    val statusBarError: Color,
    val statusBarDisconnected: Color,
    val statusBarOnError: Color,
    val statusBarOnDisconnected: Color
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
        contentBackground = Color.White,
        toolbarSurface = Color.LightGray,
        selectedRow = Color.Cyan,
        tabBackground = Color.LightGray,
        statusBarError = Color(0xFFB71C1C),
        statusBarDisconnected = Color(0xFF455A64),
        statusBarOnError = Color.White,
        statusBarOnDisconnected = Color.White
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
            contentBackground = KLogViewerColors.DarkContentBackground,
            toolbarSurface = KLogViewerColors.DarkToolbarSurface,
            selectedRow = KLogViewerColors.DarkSelectedRow,
            tabBackground = KLogViewerColors.DarkTabBackground,
            statusBarError = KLogViewerColors.DarkStatusBarError,
            statusBarDisconnected = KLogViewerColors.DarkStatusBarDisconnected,
            statusBarOnError = KLogViewerColors.DarkStatusBarOnError,
            statusBarOnDisconnected = KLogViewerColors.DarkStatusBarOnDisconnected
        )
    } else {
        CustomColors(
            contentBackground = KLogViewerColors.LightContentBackground,
            toolbarSurface = KLogViewerColors.LightToolbarSurface,
            selectedRow = KLogViewerColors.LightSelectedRow,
            tabBackground = KLogViewerColors.LightTabBackground,
            statusBarError = KLogViewerColors.LightStatusBarError,
            statusBarDisconnected = KLogViewerColors.LightStatusBarDisconnected,
            statusBarOnError = KLogViewerColors.LightStatusBarOnError,
            statusBarOnDisconnected = KLogViewerColors.LightStatusBarOnDisconnected
        )
    }

    val typography = Typography(
        h1 = UI_DISPLAY_PRIMARY,
        h2 = UI_DISPLAY_SECONDARY,
        h3 = UI_SECTION_TITLE,
        h4 = UI_SECTION_TITLE.copy(fontSize = 15.sp, lineHeight = 19.sp),
        h5 = UI_SUBTITLE_PRIMARY.copy(fontWeight = FontWeight.Bold),
        h6 = UI_SUBTITLE_SECONDARY.copy(fontWeight = FontWeight.Bold),
        subtitle1 = UI_SUBTITLE_PRIMARY,
        subtitle2 = UI_SUBTITLE_SECONDARY,
        body1 = UI_BODY,
        body2 = UI_BODY,
        button = UI_ACTION,
        caption = UI_METADATA,
        overline = UI_OVERLINE
    )

    val m3Typography = androidx.compose.material3.Typography(
        displayLarge = UI_DISPLAY_PRIMARY,
        displayMedium = UI_DISPLAY_SECONDARY,
        displaySmall = UI_SECTION_TITLE,
        headlineLarge = UI_DISPLAY_PRIMARY,
        headlineMedium = UI_DISPLAY_SECONDARY,
        headlineSmall = UI_SECTION_TITLE,
        titleLarge = UI_SECTION_TITLE,
        titleMedium = UI_SUBTITLE_PRIMARY,
        titleSmall = UI_SUBTITLE_SECONDARY,
        bodyLarge = UI_BODY,
        bodyMedium = UI_BODY,
        bodySmall = UI_METADATA,
        labelLarge = UI_ACTION,
        labelMedium = UI_METADATA_ACTION,
        labelSmall = UI_OVERLINE
    )

    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = KLogViewerColors.DarkPrimary,
            onPrimary = KLogViewerColors.DarkOnPrimary,
            background = KLogViewerColors.DarkBackground,
            onBackground = KLogViewerColors.DarkOnBackground,
            surface = KLogViewerColors.DarkSurface,
            onSurface = KLogViewerColors.DarkOnSurface,
            surfaceVariant = KLogViewerColors.DarkSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = KLogViewerColors.LightPrimary,
            onPrimary = KLogViewerColors.LightOnPrimary,
            background = KLogViewerColors.LightBackground,
            onBackground = KLogViewerColors.LightOnBackground,
            surface = KLogViewerColors.LightSurface,
            onSurface = KLogViewerColors.LightOnSurface,
            surfaceVariant = KLogViewerColors.LightSurfaceVariant
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
                typography = m3Typography,
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
