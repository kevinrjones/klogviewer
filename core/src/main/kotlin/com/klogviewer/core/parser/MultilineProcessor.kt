package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class MultilineProcessor(
    private val template: LogTemplate,
    private val maxBufferLines: Int = 500
) {
    private val parser = TemplateLogParser(template)
    private val headerRegex = template.regex.toRegex()
    
    private var currentEntry: LogEntry? = null
    private var bufferLineCount = 0

    /**
     * Processes a single line. Returns a completed [LogEntry] if a new header line is detected.
     */
    fun process(line: String): LogEntry? {
        val trimmedLine = line.trim()
        val isHeader = headerRegex.matches(trimmedLine)
        
        return if (isHeader) {
            val finishedEntry = currentEntry
            currentEntry = parser.parse(trimmedLine).getOrNull()
            bufferLineCount = 1
            finishedEntry
        } else {
            if (currentEntry != null) {
                if (bufferLineCount < maxBufferLines) {
                    currentEntry = currentEntry!!.copy(
                        content = LogContent(currentEntry!!.content.value + "\n" + line)
                    )
                    bufferLineCount++
                } else if (bufferLineCount == maxBufferLines) {
                    currentEntry = currentEntry!!.copy(
                        content = LogContent(currentEntry!!.content.value + "\n... [truncated due to buffer limit]")
                    )
                    bufferLineCount++
                    logger.warn { "Max buffer lines reached for entry starting with ${currentEntry?.timestamp?.value}" }
                }
            }
            null
        }
    }

    /**
     * Flushes the current buffer and returns the last [LogEntry], if any.
     */
    fun flush(): LogEntry? {
        val entry = currentEntry
        currentEntry = null
        bufferLineCount = 0
        return entry
    }
}
