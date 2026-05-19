# Sprint 7: Advanced Log Formats & Flexible Parsing

## 1. Goal
Transition KLogViewer from a rigid single-format parser to a flexible, template-based engine capable of handling diverse real-world logs (abbreviated levels, varying timestamps, multiline stacks, and structured JSON).

## 2. Scope

### 2.1. Flexible Level Mapping
- Implement `LevelMapper` to support abbreviated names (`INF`, `WRN`, `ERR`) and alternative terminology (`SEVERE`, `CRITICAL`).
- Support prefix-based matching and default level assignment for logs without explicit severity.

### 2.2. Pluggable Parser Strategy (Log Templates)
- Replace `SimpleLogParser` with a `ParserRegistry` supporting regex-based templates.
- Allow users to define capture groups for `timestamp`, `level`, and `content`.
- Support standard `DateTimeFormatter` patterns for custom timestamp parsing.

### 2.3. Multi-line Aggregation
- Implement a `MultilineProcessor` to handle stack traces and indented content.
- Buffer continuation lines until the next "header line" is detected.

### 2.4. Structured Data (JSON) Support
- Implement `JsonLogParser` for basic structured log support.
- Allow mapping of JSON keys to `LogEntry` properties via configuration.

### 2.5. Heuristic Auto-Detection
- Implement a "Heuristic Probe" that tries multiple templates on the first few lines of a file to suggest the best parser.
- Detect JSON format automatically.

## 3. Key Decisions
- **Parser Registry**: Shift from a hardcoded parser to an extensible registry that can be expanded via configuration or future plugins (See ADR 019).
- **Lazy Aggregation**: Multiline processing should happen during the stream ingestion to maintain UI performance (See ADR 020).
- **Level Normalization**: Internally map all external level variations to the core `LogLevel` domain model (See ADR 019).

## 4. Definition of Done
- [x] Users can parse logs with abbreviated levels like `INF` or `ERR`.
- [x] Custom regex templates can be defined to handle non-standard text formats.
- [x] Stack traces are correctly grouped with their parent log entry.
- [x] JSON-formatted logs are automatically detected and parsed into columns.
- [x] All new parsing logic is covered by unit tests in `:core`.
