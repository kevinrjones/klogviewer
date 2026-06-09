package com.klogviewer.ui.viewmodel

internal data class StructuredQueryPath(
    val segments: List<Segment>
) {
    data class Segment(
        val key: String,
        val indices: List<Int>
    )

    fun normalizedPath(): String {
        return segments.joinToString(separator = ".") { segment ->
            buildString {
                append(escapePathSegment(segment.key))
                segment.indices.forEach { index ->
                    append('[')
                    append(index)
                    append(']')
                }
            }
        }
    }

    fun toLookupCandidates(): List<String> {
        return segments.fold(listOf("")) { acc, segment ->
            val baseToken = buildString {
                append(escapePathSegment(segment.key))
                segment.indices.forEach { index ->
                    append('[')
                    append(index)
                    append(']')
                }
            }
            val segmentTokens = if (segment.indices.isEmpty()) {
                listOf(baseToken, "$baseToken[]")
            } else {
                listOf(baseToken)
            }

            acc.flatMap { prefix ->
                segmentTokens.map { token ->
                    if (prefix.isBlank()) token else "$prefix.$token"
                }
            }
        }.distinct()
    }

    companion object {
        fun parse(rawPath: String): StructuredQueryPath? {
            val path = rawPath.trim()
            if (path.isEmpty()) {
                return null
            }

            val segments = mutableListOf<Segment>()
            var index = 0
            var malformed = false

            while (index < path.length && !malformed) {
                val parsedSegment = parseSegment(path = path, startIndex = index)
                if (parsedSegment == null) {
                    malformed = true
                } else {
                    segments += Segment(
                        key = parsedSegment.key,
                        indices = parsedSegment.indices
                    )
                    index = parsedSegment.nextIndex

                    if (index < path.length) {
                        val hasSegmentDelimiter = path[index] == '.'
                        index += 1
                        malformed = !hasSegmentDelimiter || index >= path.length
                    }
                }
            }

            return if (malformed) null else StructuredQueryPath(segments = segments.toList())
        }

        private fun parseSegment(path: String, startIndex: Int): ParsedSegment? {
            if (startIndex >= path.length) {
                return null
            }

            val parsedKey = parseSegmentKey(path = path, startIndex = startIndex)
            val parsedSegment = parsedKey
                ?.takeIf { key -> key.value.isNotEmpty() }
                ?.let { key ->
                    parseSegmentIndices(path = path, startIndex = key.nextIndex)
                        ?.let { indices ->
                            ParsedSegment(
                                key = key.value,
                                indices = indices.values,
                                nextIndex = indices.nextIndex
                            )
                        }
                }
            return parsedSegment
        }

        private fun parseSegmentKey(path: String, startIndex: Int): ParsedSegmentKey? {
            return if (path[startIndex] == '`') {
                parseQuotedSegmentKey(path = path, startIndex = startIndex)
            } else {
                parseUnquotedSegmentKey(path = path, startIndex = startIndex)
            }
        }

        private fun parseQuotedSegmentKey(path: String, startIndex: Int): ParsedSegmentKey? {
            var index = startIndex + 1
            val keyBuilder = StringBuilder()
            var isClosed = false
            while (index < path.length && !isClosed) {
                val character = path[index]
                if (character == '`') {
                    val isEscapedBacktick = index + 1 < path.length && path[index + 1] == '`'
                    if (isEscapedBacktick) {
                        keyBuilder.append('`')
                        index += 2
                    } else {
                        index += 1
                        isClosed = true
                    }
                } else {
                    keyBuilder.append(character)
                    index += 1
                }
            }

            return if (isClosed) {
                ParsedSegmentKey(value = keyBuilder.toString(), nextIndex = index)
            } else {
                null
            }
        }

        private fun parseUnquotedSegmentKey(path: String, startIndex: Int): ParsedSegmentKey? {
            var index = startIndex
            val keyBuilder = StringBuilder()
            var malformed = false
            while (index < path.length && !malformed) {
                val character = path[index]
                when {
                    character == '.' || character == '[' -> break
                    character == ']' || character == '`' || character.isWhitespace() -> malformed = true
                    character == '\\' -> {
                        val escaped = path.getOrNull(index + 1)
                        if (escaped == null) {
                            malformed = true
                        } else {
                            keyBuilder.append(escaped)
                            index += 2
                        }
                    }

                    else -> {
                        keyBuilder.append(character)
                        index += 1
                    }
                }
            }

            val parsedKey = keyBuilder
                .takeIf { key -> key.isNotEmpty() && !malformed }
                ?.toString()
            return parsedKey?.let { key -> ParsedSegmentKey(value = key, nextIndex = index) }
        }

        private fun parseSegmentIndices(path: String, startIndex: Int): ParsedSegmentIndices? {
            val indices = mutableListOf<Int>()
            var index = startIndex
            var malformed = false

            while (index < path.length && path[index] == '[' && !malformed) {
                index += 1
                val numberStart = index
                while (index < path.length && path[index].isDigit()) {
                    index += 1
                }

                val hasDigits = numberStart < index
                val hasClosingBracket = index < path.length && path[index] == ']'
                val indexValue = if (hasDigits) path.substring(numberStart, index).toIntOrNull() else null

                if (indexValue == null || !hasClosingBracket) {
                    malformed = true
                } else {
                    indices += indexValue
                    index += 1
                }
            }

            return if (malformed) {
                null
            } else {
                ParsedSegmentIndices(values = indices.toList(), nextIndex = index)
            }
        }
    }
}

private data class ParsedSegmentKey(
    val value: String,
    val nextIndex: Int
)

private data class ParsedSegmentIndices(
    val values: List<Int>,
    val nextIndex: Int
)

private data class ParsedSegment(
    val key: String,
    val indices: List<Int>,
    val nextIndex: Int
)

private fun escapePathSegment(segment: String): String {
    return segment
        .replace("\\", "\\\\")
        .replace(".", "\\.")
        .replace("[", "\\[")
        .replace("]", "\\]")
}
