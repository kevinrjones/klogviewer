# TASKS: Sprint 7 - Advanced Log Formats

## 12. Sprint 7: Advanced Log Formats & Flexible Parsing

### 12.1. Flexible Level Mapping (ADR 019)
- [x] 12.1.1. Implement `LevelMapper` utility to normalize external level strings
- [x] 12.1.2. Support common abbreviations (INF, WRN, ERR, FTL, DBUG)
- [x] 12.1.3. Support alternative terminology (SEVERE, CRITICAL, WARNING, TRACE)
- [x] 12.1.4. Implement prefix-based matching (e.g., 'D' -> DEBUG)
- [x] 12.1.5. Allow default level assignment for logs without explicit severity
- [x] 12.1.6. Preserve original level names in UI display while maintaining normalization

### 12.2. Pluggable Parser Strategy (ADR 019)
- [x] 12.2.1. Create `ParserRegistry` to manage multiple `LogTemplate`s
- [x] 12.2.2. Define `LogTemplate` data class with regex and named capture groups
- [x] 12.2.3. Implement generic `TemplateLogParser` that uses `LogTemplate`
- [x] 12.2.4. Support standard `DateTimeFormatter` patterns for custom timestamp parsing
- [x] 12.2.5. Integrate `LevelMapper` into the template parsing flow

### 12.3. Multiline Log Aggregation (ADR 020)
- [x] 12.3.1. Implement `MultilineProcessor` for log ingestion
- [x] 12.3.2. Identify "Header Lines" using active `LogTemplate`
- [x] 12.3.3. Buffer "Continuation Lines" (indented or non-matching lines)
- [x] 12.3.4. Concatenate buffered lines into a single `LogEntry` content
- [x] 12.3.5. Implement buffer size limits and flush timeouts to prevent memory issues

### 12.4. Structured Data (JSON) Support
- [x] 12.4.1. Implement `JsonLogParser` for basic structured log support
- [x] 12.4.2. Allow mapping of JSON keys (e.g., "msg", "timestamp", "level") to `LogEntry` fields
- [x] 12.4.3. Handle nested JSON objects by serializing them to strings in the content field
- [x] 12.4.4. Ensure JSON parsing is as efficient as text parsing for large files

### 12.5. Heuristic Auto-Detection
- [x] 12.5.1. Implement "Heuristic Probe" to test templates against the first few lines of a file
- [x] 12.5.2. Automatically detect JSON format (starts with `{`)
- [x] 12.5.3. Rank templates based on match quality and select the best candidate
- [x] 12.5.4. Update `LogViewerViewModel` to use the heuristic probe when opening new files

### 12.6. UI Integration & Feedback
- [ ] 12.6.1. Update UI to indicate which parser is being used for a file
- [ ] 12.6.2. Ensure the "Entry Detail Pane" correctly renders multiline content with preserved formatting
- [ ] 12.6.3. Add basic UI for selecting/confirming the detected log format

### 12.7. Verification & Testing
- [x] 12.7.1. Unit tests in `:core` for `LevelMapper`
- [x] 12.7.2. Unit tests in `:core` for `ParserRegistry` and `TemplateLogParser`
- [x] 12.7.3. Integration tests for `MultilineProcessor` with stack traces
- [x] 12.7.4. Unit tests for `JsonLogParser` with various JSON layouts
- [x] 12.7.5. Verify heuristic detection with sample log files (Log4j, Syslog, JSON)
- [x] 12.7.6. Fix regression in `Standard` log level detection for timezone-aware entries
- [x] 12.7.7. Improve parsing robustness against trailing whitespace in templates
- [x] 12.7.8. Ensure `SimpleLogParser` populates `fields` map for consistent UI details
- [x] 12.7.9. Fix false positive `logfmt` detection and improve `Standard` regex robustness
- [x] 12.8. Ensure the last log column expands to fit its content automatically
- [x] 12.9. Preserve raw values for unrecognized log levels and improve look-ahead detection
