# Sprint 4: Structured Data & Advanced Parsing

## 1. Goal
Move beyond plain text logs by providing first-class support for JSON and XML structured data, enabling deep inspection and field-based filtering.

## 2. Scope

### 2.1. Structured Parsers
- Implement `JsonLogParser` and `XmlLogParser`.
- Add auto-detection logic for structured formats.
- Support for custom field mapping (e.g., mapping "msg" to content, "lvl" to level).

### 2.2. Tree-View Inspector
- Implement a hierarchical tree-view component for the entry detail pane.
- Support for expanding/collapsing nodes in large JSON/XML payloads.
- Syntax highlighting for structured data.

### 2.3. Advanced Filtering
- Enable filtering by specific keys in structured logs.
- Support for "Filter by value" from the tree-view context menu.

## 3. Key Decisions
- **Flexible Data Model**: Use `Map<String, Any?>` in `LogEntry` to store structured fields.
- **Lazy Parsing**: Perform full structured parsing only when an entry is selected for inspection to preserve performance during scrolling.

## 4. Definition of Done
- [ ] JSON and XML logs are automatically recognized and parsed.
- [ ] Nested structured data can be explored in a tree-view in the detail pane.
- [ ] Users can filter the log list based on specific fields in the structured data.
- [ ] Custom regex/column mapping can be configured for non-standard formats.
