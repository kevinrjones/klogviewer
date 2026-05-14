# ADR 009: Structured Data Support

## Status
Proposed

## Context
Standard log files are often plain text, but modern application logs (e.g., Serilog, Logback with JSON encoder) are increasingly structured as JSON or XML. Currently, LogViewer treats every line as a single string, which limits the ability to filter by specific fields or inspect complex nested objects.

## Decision
We will implement first-class support for structured logs, allowing the application to parse, display, and query structured data.

### 1. Structured Log Entry
The `LogEntry` domain model will be extended to support a `data: Map<String, Any?>` field for structured metadata.

### 2. Auto-Detection and Parsers
- Implement a `JsonLogParser` and `XmlLogParser`.
- Add an auto-detection mechanism that attempts to parse log lines as structured data if they match certain patterns (e.g., starting with `{`).

### 3. Tree-View Entry Inspector
In the "Entry Detail Pane" (defined in ADR 008), structured data will be displayed using an interactive tree-view component. This allows users to expand/collapse nested objects and arrays.

### 4. Field-Based Filtering
The UI will allow users to filter logs based on specific fields in the structured data (e.g., `where RequestPath == "/api/login"`).

## Consequences
- **Positive**: Enables deep analysis of modern structured logs.
- **Positive**: Reduces clutter by allowing users to hide specific fields.
- **Negative**: Parsing overhead increases for high-volume logs.
- **Negative**: UI complexity increases to handle dynamic fields and nested structures.
