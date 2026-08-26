package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole

data class PatternImportResult(
    val draft: PatternDraft,
    val diagnostics: List<String> = emptyList()
)

object PatternImporter {
    private val presetMap = mapOf(
        "Logback / Log4J Standard" to
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger - %msg",
        "Serilog Text Layout" to
            "{Timestamp:yyyy-MM-dd HH:mm:ss.SSS} [{Level}] [{ThreadId}] {SourceContext} - {Message}",
        "ISO8601 Simple" to
            "%d{yyyy-MM-ddTHH:mm:ss} %level %logger - %msg",
        "Custom Draft" to
            "%d{yyyy-MM-dd HH:mm:ss} %level [%t] %logger - %msg"
    )

    fun importPattern(patternText: String): PatternImportResult {
        val trimmed = patternText.trim()
        if (trimmed.isBlank()) {
            return PatternImportResult(
                draft = defaultFallbackDraft(),
                diagnostics = listOf("Empty pattern string provided; using default fallback draft.")
            )
        }

        val formatStr = presetMap[trimmed] ?: trimmed
        val diagnostics = mutableListOf<String>()

        val (syntax, segments, placeholders) = parseFormatString(formatStr, diagnostics)

        val name = when {
            presetMap.containsKey(trimmed) -> trimmed
            syntax == "serilog" -> "Serilog Text Layout"
            syntax == "logback" -> "Logback / Log4J Standard"
            else -> "Custom Pattern"
        }

        val draft = PatternDraft(
            name = name,
            originalFormatString = formatStr,
            originalFormatSyntax = syntax,
            segments = segments,
            placeholderAnnotations = placeholders
        )

        return PatternImportResult(draft = draft, diagnostics = diagnostics)
    }

    private fun parseFormatString(
        formatStr: String,
        diagnostics: MutableList<String>
    ): Triple<String, List<PatternSegment>, Map<String, String>> {
        return when {
            formatStr.contains("%") -> {
                val (segments, placeholders) = LogbackPatternParser.parse(formatStr, diagnostics)
                Triple("logback", segments, placeholders)
            }
            formatStr.contains("{") -> {
                val (segments, placeholders) = SerilogPatternParser.parse(formatStr)
                Triple("serilog", segments, placeholders)
            }
            else -> {
                diagnostics.add("Unrecognized format syntax in '$formatStr'; treating as literal custom pattern.")
                Triple("custom", listOf(PatternSegment.Delimiter(PatternDelimiter(value = formatStr))), emptyMap())
            }
        }
    }

    fun extractSerilogPlaceholders(messageTemplate: String): Map<String, String> {
        val placeholders = mutableMapOf<String, String>()
        val regex = Regex("""\{([A-Za-z0-9_]+)(?::[^}]+)?}""")
        regex.findAll(messageTemplate).forEach { match ->
            val propName = match.groupValues[1]
            placeholders[propName] = match.value
        }
        return placeholders
    }

    fun defaultFallbackDraft(): PatternDraft {
        return PatternDraft(
            name = "Default Fallback Pattern",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(
                        role = PatternTokenRole.TIMESTAMP,
                        formatPattern = "yyyy-MM-dd HH:mm:ss.SSS"
                    )
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )
    }
}

private object LogbackPatternParser {
    fun parse(
        formatStr: String,
        diagnostics: MutableList<String>
    ): Pair<List<PatternSegment>, Map<String, String>> {
        val segments = mutableListOf<PatternSegment>()
        val placeholders = mutableMapOf<String, String>()

        val regex = Regex(
            """(%d(?:\{[^}]*})?|%date(?:\{[^}]*})?|%t(?:hread)?|%-?\d*(?:\.\d+)?level|%-?\d*(?:\.\d+)?p|""" +
                """%c(?:\{[^}]*})?|%logger(?:\{[^}]*})?|%m(?:sg)?|%message|%ex(?:exception)?|%throwable|""" +
                """%X\{[^}]+}|%MDC\{[^}]+}|%n|%i|%r|%-?\d*(?:\.\d+)?[a-zA-Z]+(?:\{[^}]*})?|[^%]+)"""
        )

        regex.findAll(formatStr).map { it.value }.forEach { tokenStr ->
            val segment = parseToken(tokenStr, diagnostics, placeholders)
            if (segment != null) {
                segments.add(segment)
            }
        }

        return Pair(segments.ifEmpty { PatternImporter.defaultFallbackDraft().segments }, placeholders)
    }

    private fun parseToken(
        tokenStr: String,
        diagnostics: MutableList<String>,
        placeholders: MutableMap<String, String>
    ): PatternSegment? {
        val knownRole = mapKnownRole(tokenStr)
        val result = when {
            knownRole != null -> PatternSegment.Token(PatternToken(role = knownRole))
            tokenStr.startsWith("%d") || tokenStr.startsWith("%date") -> parseDateToken(tokenStr)
            tokenStr.startsWith("%X{") || tokenStr.startsWith("%MDC{") -> parseMdcToken(tokenStr, placeholders)
            tokenStr == "%n" -> null
            tokenStr.startsWith("%") -> parseUnsupportedSpecifier(tokenStr, diagnostics)
            else -> PatternSegment.Delimiter(PatternDelimiter(value = tokenStr))
        }
        return result
    }

    private fun mapKnownRole(tokenStr: String): PatternTokenRole? {
        val role = when {
            tokenStr.startsWith("%t") || tokenStr.startsWith("%thread") -> PatternTokenRole.THREAD
            tokenStr.startsWith("%p") || tokenStr.contains("level") || tokenStr.contains("-%") -> PatternTokenRole.LEVEL
            else -> mapLogbackContextRole(tokenStr)
        }
        return role
    }

    private fun mapLogbackContextRole(tokenStr: String): PatternTokenRole? {
        return when {
            tokenStr.startsWith("%c") || tokenStr.startsWith("%logger") -> PatternTokenRole.LOGGER
            tokenStr.startsWith("%m") || tokenStr.startsWith("%msg") ||
                tokenStr.startsWith("%message") -> PatternTokenRole.MESSAGE
            tokenStr.startsWith("%ex") || tokenStr.startsWith("%exception") ||
                tokenStr.startsWith("%throwable") -> PatternTokenRole.EXCEPTION
            else -> null
        }
    }

    private fun parseDateToken(tokenStr: String): PatternSegment {
        val dateFormat = Regex("""%(?:d|date)(?:\{([^}]*)})?""")
            .find(tokenStr)?.groupValues?.get(1) ?: "yyyy-MM-dd HH:mm:ss.SSS"
        return PatternSegment.Token(
            PatternToken(
                role = PatternTokenRole.TIMESTAMP,
                formatPattern = dateFormat.ifBlank { "yyyy-MM-dd HH:mm:ss.SSS" }
            )
        )
    }

    private fun parseMdcToken(tokenStr: String, placeholders: MutableMap<String, String>): PatternSegment {
        val propName = Regex("""%(?:X|MDC)\{([^}]+)}""").find(tokenStr)?.groupValues?.get(1) ?: "prop"
        placeholders[propName] = "{$propName}"
        return PatternSegment.Token(
            PatternToken(
                role = PatternTokenRole.CUSTOM_PROPERTY,
                customPropertyName = propName
            )
        )
    }

    private fun parseUnsupportedSpecifier(tokenStr: String, diagnostics: MutableList<String>): PatternSegment {
        diagnostics.add("Unsupported Logback specifier '$tokenStr' retained as custom property.")
        val specifierName = tokenStr.removePrefix("%").filter { it.isLetterOrDigit() }.ifBlank { "custom" }
        return PatternSegment.Token(
            PatternToken(
                role = PatternTokenRole.CUSTOM_PROPERTY,
                customPropertyName = specifierName
            )
        )
    }
}

private object SerilogPatternParser {
    fun parse(formatStr: String): Pair<List<PatternSegment>, Map<String, String>> {
        val segments = mutableListOf<PatternSegment>()
        val placeholders = mutableMapOf<String, String>()

        Regex("""(\{[^}]+}|[^{]+)""").findAll(formatStr).map { it.value }.forEach { tokenStr ->
            val segment = parseToken(tokenStr, placeholders)
            if (segment != null) {
                segments.add(segment)
            }
        }

        return Pair(segments.ifEmpty { PatternImporter.defaultFallbackDraft().segments }, placeholders)
    }

    private fun parseToken(
        tokenStr: String,
        placeholders: MutableMap<String, String>
    ): PatternSegment? {
        if (!tokenStr.startsWith("{") || !tokenStr.endsWith("}")) {
            return PatternSegment.Delimiter(PatternDelimiter(value = tokenStr))
        }

        val inner = tokenStr.removeSurrounding("{", "}").trim()
        val namePart = if (inner.contains(":")) inner.substringBefore(":").trim() else inner
        val formatPart = if (inner.contains(":")) inner.substringAfter(":").trim() else null

        val lowerName = namePart.lowercase()
        val result = when {
            lowerName == "newline" -> null
            lowerName in setOf("timestamp", "datetime", "date", "t", "time") -> PatternSegment.Token(
                PatternToken(
                    role = PatternTokenRole.TIMESTAMP,
                    formatPattern = formatPart ?: "yyyy-MM-dd HH:mm:ss.SSS"
                )
            )
            isDateFormat(namePart) -> PatternSegment.Token(
                PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = inner)
            )
            else -> parseKnownOrCustomSerilogToken(lowerName, namePart, placeholders)
        }
        return result
    }

    private fun parseKnownOrCustomSerilogToken(
        lowerName: String,
        namePart: String,
        placeholders: MutableMap<String, String>
    ): PatternSegment {
        val knownRole = mapSerilogStandardRole(lowerName)
        return if (knownRole != null) {
            PatternSegment.Token(PatternToken(role = knownRole))
        } else {
            placeholders[namePart] = "{$namePart}"
            PatternSegment.Token(
                PatternToken(
                    role = PatternTokenRole.CUSTOM_PROPERTY,
                    customPropertyName = namePart
                )
            )
        }
    }

    private fun mapSerilogStandardRole(lowerName: String): PatternTokenRole? {
        return when {
            lowerName.startsWith("level") || lowerName == "l" -> PatternTokenRole.LEVEL
            lowerName in setOf("threadid", "threadname", "thread") -> PatternTokenRole.THREAD
            lowerName in setOf("sourcecontext", "logger", "source", "context") -> PatternTokenRole.LOGGER
            lowerName.startsWith("message") || lowerName in setOf("m", "msg") -> PatternTokenRole.MESSAGE
            lowerName.startsWith("exception") || lowerName in setOf("ex") -> PatternTokenRole.EXCEPTION
            else -> null
        }
    }

    private fun isDateFormat(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("yyyy") || t.contains("hh:mm") || t.contains("mm:ss") ||
            (t.contains("dd") && (t.contains("mm") || t.contains("yy"))) ||
            t.contains("iso8601")
    }
}
