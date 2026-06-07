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

            while (index < path.length) {
                val parsedSegment = parseSegment(path = path, startIndex = index) ?: return null
                segments += Segment(
                    key = parsedSegment.key,
                    indices = parsedSegment.indices
                )
                index = parsedSegment.nextIndex

                if (index == path.length) {
                    break
                }

                if (path[index] != '.') {
                    return null
                }
                index += 1
                if (index >= path.length) {
                    return null
                }
            }

            return StructuredQueryPath(segments = segments.toList())
        }

        private fun parseSegment(path: String, startIndex: Int): ParsedSegment? {
            if (startIndex >= path.length) {
                return null
            }

            var index = startIndex
            val keyBuilder = StringBuilder()

            if (path[index] == '`') {
                index += 1
                var isClosed = false
                while (index < path.length) {
                    val character = path[index]
                    if (character == '`') {
                        if (index + 1 < path.length && path[index + 1] == '`') {
                            keyBuilder.append('`')
                            index += 2
                        } else {
                            index += 1
                            isClosed = true
                            break
                        }
                    } else {
                        keyBuilder.append(character)
                        index += 1
                    }
                }
                if (!isClosed) {
                    return null
                }
            } else {
                while (index < path.length) {
                    val character = path[index]
                    when {
                        character == '.' || character == '[' -> break
                        character == ']' || character == '`' || character.isWhitespace() -> return null
                        character == '\\' -> {
                            if (index + 1 >= path.length) {
                                return null
                            }
                            keyBuilder.append(path[index + 1])
                            index += 2
                        }

                        else -> {
                            keyBuilder.append(character)
                            index += 1
                        }
                    }
                }
            }

            if (keyBuilder.isEmpty()) {
                return null
            }

            val indices = mutableListOf<Int>()
            while (index < path.length && path[index] == '[') {
                index += 1
                val numberStart = index
                while (index < path.length && path[index].isDigit()) {
                    index += 1
                }
                if (numberStart == index || index >= path.length || path[index] != ']') {
                    return null
                }

                val indexValue = path.substring(numberStart, index).toIntOrNull() ?: return null
                indices += indexValue
                index += 1
            }

            return ParsedSegment(
                key = keyBuilder.toString(),
                indices = indices.toList(),
                nextIndex = index
            )
        }
    }
}

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
