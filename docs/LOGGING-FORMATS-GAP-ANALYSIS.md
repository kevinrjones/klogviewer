# Gap Analysis: Logging Format Support

This document analyzes the differences between various logging formats and the current capabilities of KLogViewer, identifying gaps and proposing strategies for handling them.

## 1. Current State Summary

The current `SimpleLogParser` implementation is optimized for a specific, standard text-based format:
- **Pattern**: `YYYY-MM-DD HH:mm:ss[.SSS] [LEVEL] content`
- **Levels**: Strictly matches `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`.
- **Structure**: Single-line only, regex-based.

## 2. Identified Gaps

### 2.1. Log Level Variations
| Gap | Description | Examples |
| :--- | :--- | :--- |
| **Abbreviated Names** | Many frameworks use short codes for levels. | `INF`, `WRN`, `ERR`, `FTL`, `DBUG`, `I`, `W`, `E` |
| **Alternative Terminology** | Different frameworks use different names for the same severity. | `WARNING` (Log4j), `SEVERE` (JUL), `CRITICAL` (Serilog), `TRACE`, `VERBOSE`, `NOTICE` |
| **Numeric Levels** | Syslog and some binary formats use integers. | 0 (Emergency) to 7 (Debug) |
| **Missing Levels** | Simple stdout logs or redirected output often lack levels entirely. | `Hello World` |

### 2.2. Timestamp Formats
| Gap | Description | Examples |
| :--- | :--- | :--- |
| **ISO8601 / RFC3339** | Standard web and cloud formats. | `2026-05-14T16:53:00Z`, `2026-05-14T16:53:00+00:00` |
| **Locale-Specific** | Formats common in traditional web servers. | `14/May/2026:16:53:00 +0000` (Apache) |
| **Unix Epoch** | Raw numeric timestamps. | `1715694780`, `1715694780000` |
| **Implicit Year** | Standard Syslog often omits the year. | `May 14 16:53:00` |

### 2.3. Log Structure & Encoding
| Gap | Description | Examples |
| :--- | :--- | :--- |
| **Structured (JSON)** | Modern "Cloud Native" logs. | `{"ts": "...", "level": "INFO", "msg": "..."}` |
| **Key-Value (logfmt)** | Common in Go/Heroku ecosystems. | `time=... level=info msg="hello world" user_id=123` |
| **Delimited** | CSV or TSV logs. | `2026-05-14,INFO,Application started` |

### 2.4. Multiline Entries
| Gap | Description |
| :--- | :--- |
| **Stack Traces** | Exceptions that span multiple lines. |
| **Indented Content** | Messages that continue on new lines with leading whitespace. |
| **JSON Fragments** | Pretty-printed JSON logs. |

---

## 3. Proposed Strategies

### 3.1. Flexible Level Mapping
Instead of strict string matching, implement a `LevelMapper` that supports:
- **Alias Maps**: `{"INF": INFO, "WARNING": WARN, "ERR": ERROR}`.
- **Prefix Matching**: Treat any level starting with `D` as `DEBUG` if it's in the level column.
- **Defaulting**: Allow a "Default Level" (e.g., `INFO`) for logs where a level cannot be parsed.

### 3.2. Pluggable Parser Strategy (Log Templates)
Transition from a single `LogParser` to a `ParserRegistry` that can select parsers based on file content or extension:
- **Regex Templates**: Allow users to define a custom regex and map capture groups to `timestamp`, `level`, and `content`.
- **Date Format Strings**: Support standard `DateTimeFormatter` patterns (e.g., `dd/MMM/yyyy:HH:mm:ss Z`).

### 3.3. Multi-line Aggregation
Implement a `MultilineProcessor` in the `LogSource` layer that:
- Identifies "Header Lines" (lines that match an entry-start pattern).
- Buffers "Continuation Lines" (lines that don't match the pattern or start with whitespace/tabs) until the next header is found.

### 3.4. Structured Data Support
Introduce a `JsonLogParser` that:
- Detects if a file is JSON-formatted (starts with `{`).
- Allows mapping JSON keys to `LogEntry` properties via configuration.
- Automatically handles nested objects or arrays in the content view.

### 3.5. Auto-Detection Logic
On opening a file, run a "Heuristic Probe":
1. Try parsing the first 10 lines with known templates.
2. If JSON is detected, suggest a JSON mapping.
3. If no template matches, offer to "Quick Create" a regex template based on the first line.
