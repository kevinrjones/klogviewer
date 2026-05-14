# ADR 019: Template-based Log Parsing and Level Mapping

## Status
Proposed

## Context
The current `SimpleLogParser` is hardcoded for a specific timestamp and log level format. Real-world logs vary significantly in their timestamp representations (ISO8601, Unix epoch, custom patterns) and log level terminology (`INF` vs `INFO`, `SEVERE` vs `ERROR`). To support a wide variety of logs without code changes, we need a flexible, configuration-driven parsing architecture.

## Decision
We will transition to a template-based parsing system centered around a `ParserRegistry` and extensible `LogTemplate`s.

### 1. Parser Registry
Instead of a single parser, the system will maintain a `ParserRegistry`. When a file is opened, the registry will identify the appropriate parser template through heuristic auto-detection or user selection.

### 2. Regex-based Log Templates
A `LogTemplate` will define how to extract core fields from a raw log line using a Regular Expression with named capture groups:
- `timestamp`: The raw timestamp string.
- `level`: The raw log level string.
- `content`: The main log message.

### 3. Flexible Level Mapping
To normalize varying log level names into our internal `LogLevel` domain model, we will implement a `LevelMapper`.
- **Alias Mapping**: A configurable map of external strings to internal levels (e.g., `{"WRN": WARN, "ERR": ERROR}`).
- **Prefix Matching**: Option to match levels by their first letter (e.g., `D` -> `DEBUG`).
- **Defaulting**: A fallback level for entries where no level can be identified.

### 4. Custom Timestamp Parsing
Each `LogTemplate` will specify a `DateTimeFormatter` pattern to parse the extracted `timestamp` capture group into a `LocalDateTime` or `Instant`.

### 5. Heuristic Auto-Detection
The `ParserRegistry` will implement a "Heuristic Probe" that attempts to match the first few lines of a log file against registered templates to suggest the most likely candidate.

## Consequences
- **Positive**: Users can support new log formats by defining simple regex-based templates.
- **Positive**: Internal logic remains clean by normalizing all external variations into a standard domain model.
- **Positive**: Improved user experience through automatic format detection.
- **Negative**: Slight performance overhead for regex-based parsing compared to optimized string splitting.
- **Negative**: Increased complexity in managing and persisting user-defined templates.
