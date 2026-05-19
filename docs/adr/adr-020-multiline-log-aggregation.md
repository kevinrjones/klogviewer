# ADR 020: Multiline Log Aggregation

## Status
Proposed

## Context
Many log files contain entries that span multiple lines, most notably Java stack traces, indented JSON fragments, or multi-line log messages. Currently, KLogViewer treats every line as a separate entry, which breaks the context of stack traces and makes searching and filtering difficult.

## Decision
We will implement a `MultilineProcessor` within the `LogSource` ingestion layer to aggregate related lines into single `LogEntry` objects.

### 1. Header Line Detection
The processor will use the active `LogTemplate` (defined in ADR 019) to identify "Header Lines" — lines that represent the start of a new log entry (e.g., lines that have a timestamp and level).

### 2. Continuation Buffering
Lines that do not match the header pattern, or that match specific "continuation" rules (e.g., starting with a tab or whitespace), will be buffered and appended to the preceding header line's content.

### 3. Aggregation Logic
The content of a `LogEntry` will be the concatenation of the header line content and all subsequent continuation lines.
- **Buffer Flush**: A buffered entry is flushed when a new header line is encountered or the end of the stream is reached.
- **Max Buffer Size**: To prevent memory issues with runaway continuation lines, a configurable maximum buffer size (e.g., 50 lines or 64KB) will be enforced.

### 4. UI Representation
The UI will display aggregated entries as a single row in the log list. The "Entry Detail Pane" will be responsible for rendering the full multiline content, preserving formatting (newlines and indentation).

### 5. Search and Filtering
Filters and searches will operate on the entire aggregated content of the `LogEntry`, ensuring that searching for a class name in a stack trace correctly identifies the parent log entry.

## Consequences
- **Positive**: Stack traces and complex log messages are preserved as single units of work.
- **Positive**: Improved search results as context is maintained.
- **Negative**: Increased complexity in the stream ingestion logic.
- **Negative**: Potential performance impact during indexing/parsing due to buffering and concatenation.
- **Negative**: "Tail" performance might be slightly delayed as the system waits for the next header line to confirm the end of a multiline entry (mitigated by a short flush timeout).
