package com.klogviewer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

internal const val NO_SOURCE_SHADE_INDEX = -1
internal val SourceShadeIndexSemanticsKey = SemanticsPropertyKey<Int>("sourceShadeIndex")
internal var SemanticsPropertyReceiver.sourceShadeIndex by SourceShadeIndexSemanticsKey

internal val sourceBackgroundLightShades = generateSubtleGrayShades(
    baseArgb = 0xFFFAFAFA,
    count = 50,
    maxOffset = 12,
    isDarkening = true
)

internal val sourceBackgroundDarkShades = generateSubtleGrayShades(
    baseArgb = 0xFF1E1E1E,
    count = 50,
    maxOffset = 12,
    isDarkening = false
)

internal fun generateSubtleGrayShades(
    baseArgb: Long,
    count: Int,
    maxOffset: Int = 12,
    isDarkening: Boolean = true
): List<Color> {
    if (count <= 0) return emptyList()

    val alpha = ((baseArgb ushr 24) and 0xFF).toInt()
    val red = ((baseArgb ushr 16) and 0xFF).toInt()
    val green = ((baseArgb ushr 8) and 0xFF).toInt()
    val blue = (baseArgb and 0xFF).toInt()
    val baseGray = ((red + green + blue) / 3).coerceIn(0, 255)

    return (0 until count).map { index ->
        val offset = if (count > 1) (index * maxOffset) / (count - 1) else 0
        val channel = if (isDarkening) {
            (baseGray - offset).coerceIn(0, 255)
        } else {
            (baseGray + offset).coerceIn(0, 255)
        }

        val shadeArgb = (alpha.toLong() shl 24) or
            (channel.toLong() shl 16) or
            (channel.toLong() shl 8) or
            channel.toLong()

        Color(shadeArgb)
    }
}

internal fun generateDarkerGrayShades(argb: Long, count: Int, step: Int): List<Color> {
    if (count <= 0) return emptyList()

    val alpha = ((argb ushr 24) and 0xFF).toInt()
    val red = ((argb ushr 16) and 0xFF).toInt()
    val green = ((argb ushr 8) and 0xFF).toInt()
    val blue = (argb and 0xFF).toInt()
    val baseGray = ((red + green + blue) / 3).coerceIn(0, 255)
    val safeStep = step.coerceAtLeast(0)

    return (0 until count).map { index ->
        val darkenedChannel = (baseGray.toLong() - index.toLong() * safeStep.toLong())
            .coerceIn(0L, 255L)
            .toInt()

        val shadeArgb = (alpha.toLong() shl 24) or
            (darkenedChannel.toLong() shl 16) or
            (darkenedChannel.toLong() shl 8) or
            darkenedChannel.toLong()

        Color(shadeArgb)
    }
}

internal fun getSourceShadeIndex(sourceId: String?, sourceIds: List<String>): Int {
    if (sourceId.isNullOrBlank() || sourceIds.size <= 1) {
        return NO_SOURCE_SHADE_INDEX
    }
    return stableSourceShadeIndex(sourceId, sourceBackgroundLightShades.size)
}

internal fun getSourceBackgroundColor(sourceShadeIndex: Int, isDarkMode: Boolean): Color {
    if (sourceShadeIndex == NO_SOURCE_SHADE_INDEX) return Color.Transparent
    val shades = if (isDarkMode) sourceBackgroundDarkShades else sourceBackgroundLightShades
    return shades[sourceShadeIndex % shades.size]
}

internal fun matchSourceIndex(sourceId: String?, sourceIds: List<String>): Int {
    if (sourceId.isNullOrBlank() || sourceIds.isEmpty()) return -1

    val exactIndex = sourceIds.indexOf(sourceId)
    val index = if (exactIndex >= 0) {
        exactIndex
    } else {
        val normalizedSource = sourceId.removePrefix("local:").removeSuffix("/").removeSuffix("\\")
        sourceIds.indexOfFirst { candidate ->
            val normalizedCandidate = candidate.removePrefix("local:").removeSuffix("/").removeSuffix("\\")
            normalizedCandidate == normalizedSource ||
                normalizedCandidate.endsWith(normalizedSource) ||
                normalizedSource.endsWith(normalizedCandidate)
        }.coerceAtLeast(0)
    }
    return index
}

internal fun getSourceBadgeColor(sourceId: String?, sourceIds: List<String>, isMissing: Boolean = false): Color {
    return when {
        isMissing -> Color.Red
        sourceId == null || sourceIds.size <= 1 -> Color.Transparent
        else -> {
            val index = matchSourceIndex(sourceId, sourceIds)
            if (index >= 0) {
                val colors = listOf(
                    Color(0xFFE57373), // Red
                    Color(0xFF81C784), // Green
                    Color(0xFF64B5F6), // Blue
                    Color(0xFFFFD54F), // Amber
                    Color(0xFFBA68C8), // Purple
                    Color(0xFF4DB6AC), // Teal
                    Color(0xFFF06292), // Pink
                    Color(0xFFAED581)  // Light Green
                )
                colors[index % colors.size]
            } else {
                Color.Transparent
            }
        }
    }
}

internal fun buildSourceBadgeTooltip(sourceId: String?, isMissing: Boolean): String {
    val fileName = sourceId.extractSourceFileName()
    return if (isMissing) "$fileName (Missing)" else fileName
}

private fun stableSourceShadeIndex(sourceId: String, paletteSize: Int): Int {
    return Math.floorMod(sourceId.hashCode(), paletteSize)
}
