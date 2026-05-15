package com.logviewer.core.parser

import com.logviewer.domain.parser.LogParser

class ParserRegistry {
    private val templates = mutableMapOf<String, LogTemplate>()

    init {
        registerDefaultTemplates()
    }

    fun register(template: LogTemplate) {
        templates[template.name] = template
    }

    fun getTemplate(name: String): LogTemplate? = templates[name]

    fun getParser(templateName: String): LogParser? {
        return templates[templateName]?.let { TemplateLogParser(it) }
    }

    fun getAllTemplates(): List<LogTemplate> = templates.values.toList()

    private fun registerDefaultTemplates() {
        register(LogTemplate(
            name = "Standard",
            regex = """^(?<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d{3})?(?:\s+[+-]\d{2}:?\d{2})?)\s+(?:\[(?<metadata>.*?)\]\s+)?(?<level>\[.*?\]|\S+)\s+(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd HH:mm:ss[.SSS][ XXX]"
        ))
        
        register(LogTemplate(
            name = "Syslog",
            regex = """^(?<timestamp>[A-Z][a-z]{2}\s+\d+\s+\d{2}:\d{2}:\d{2})\s+(?<hostname>\S+)\s+(?<process>[^:\[\s]+)(?:\[(?<pid>\d+)\])?:\s+(?<content>.*)$""",
            timestampPattern = "MMM d HH:mm:ss",
            columns = listOf("Timestamp", "Hostname", "Process", "Content")
        ))
        
        register(LogTemplate(
            name = "ISO8601",
            regex = """^(?<timestamp>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z?)\s+(?<level>\S+)\s+(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][X]"
        ))

        register(LogTemplate(
            name = "Apache",
            regex = """^(?<clientIp>\S+)\s+-\s+(?<user>\S+)\s+\[(?<timestamp>\d{2}/[A-Z][a-z]{2}/\d{4}:\d{2}:\d{2}:\d{2}\s+[+-]\d{4})\]\s+"(?<request>.*?)"\s+(?<status>\d+)\s+(?<bytes>\S+)(?:\s+"(?<referrer>.*?)"\s+"(?<agent>.*?)")?.*$""",
            timestampPattern = "dd/MMM/yyyy:HH:mm:ss Z",
            columns = listOf("Timestamp", "Client IP", "Request", "Status", "Bytes")
        ))

        register(LogTemplate(
            name = "CSV",
            regex = """^(?<timestamp>[^,]+),(?<level>[^,]+),(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd"
        ))
    }
}
