# TASKS: Sprint 12A - Structured Data Foundation

## 12A. Structured Data Foundation

### 12A.0. Goal
Establish the minimum safe structured-data foundation for parsing and normalization so JSON structured logs are ingestible, typed structured values are representable, and existing plain-text/template flows remain stable.

### 12A.1. In Scope
- Structured field/domain model introduction (`typed tree + flattened path index`) with backward-compatible projection.
- Flattened path generation rules for objects and arrays.
- JSON parser hardening for mixed validity records and graceful fallback.
- Baseline canonical normalization for `timestamp`, `level`, `message`, `logger`, `exception`, `trace.id`, and `span.id`.
- Parser detection confidence improvements and fallback behavior.
- Raw-field preservation alongside canonical projections.
- Unit fixtures for generic JSON plus common JVM/.NET JSON examples.
- Regression protection for existing plain-text/template parsing.
- Developer documentation for model/normalization/detection contracts.

### 12A.2. Out of Scope
- Advanced structured filtering grammar and query parser enhancements (12B).
- Full structured detail-tree inspector UX and context actions (12C).
- Dashboard grouping/aggregation redesign for structured fields (12E).
- Ecosystem-wide normalization pack beyond baseline aliases (12D).
- Deep performance tuning and memory optimizations beyond safe guardrails (12E).
- Full XML feature scope unless a trivial best-effort parser adapter is already low-risk.

### 12A.3. Dependencies
- None. This is the prerequisite slice for the rest of Sprint 12.

### 12A.4. Scope-to-Workstream Mapping
- Domain model + compatibility projection (`3`, `4`) -> `domain/model/LogEntry.kt`, structured value contract types, compatibility adapters.
- Parser hardening + confidence detection (`5`, `6`) -> `core/parser/JsonLogParser.kt`, `core/parser/HeuristicProbe.kt`, parser selection flow.
- Baseline normalization + raw preservation (`7`) -> parser normalization helpers and canonical mapping rules.
- Validation and regression safety (`8`, `10`) -> `core` parser tests, `ui`/integration regression tests for plain/template logs.
- Documentation and sprint traceability (`9`) -> sprint/task docs and developer notes.

### 12A.5. Domain Structured Model and Backward Compatibility
- [x] 12A.5.1. Introduce a typed structured value contract (`string|number|boolean|null|object|array`) suitable for parser output and detail rendering.
- [x] 12A.5.2. Define flattened path-index contract (path -> typed value/multi-value) including deterministic array path handling (`items[0].id`) and any-match compatibility.
- [x] 12A.5.3. Add compatibility projection so current `LogEntry.fields: Map<String, String>` consumers continue to function during rollout.
- [x] 12A.5.4. Preserve raw structured payload representation for inspection/export alongside canonical and flat projections.
- [x] 12A.5.5. Document data contract invariants (null handling, repeated keys, overwrite rules, and canonical-vs-raw precedence).

### 12A.6. JSON Parser Hardening and Detection Confidence
- [x] 12A.6.1. Harden JSON parsing to avoid fatal ingestion failures on invalid lines in mixed files.
- [x] 12A.6.2. Emit structured parse confidence signals usable by parser auto-selection.
- [x] 12A.6.3. Extend heuristic detection scoring factors (parse success ratio, canonical key hits, malformed ratio).
- [x] 12A.6.4. Keep parser override in status bar authoritative over auto-detection.
- [x] 12A.6.5. Ensure fallback to `TemplateLogParser`/`SimpleLogParser` remains deterministic when confidence is low.

### 12A.7. Baseline Canonical Normalization and Raw Preservation
- [x] 12A.7.1. Define baseline alias mapping for canonical fields:
  - timestamp (`timestamp`, `@timestamp`, `time`, `ts`, `@t`, `Timestamp`)
  - level (`level`, `severity`, `lvl`, `@l`, `LogLevel`, `Level`)
  - message (`message`, `msg`, `body`, `@m`, `@mt`, `Message`)
  - logger (`logger`, `logger_name`, `SourceContext`, `Category`, `CategoryName`)
  - exception (`exception`, `error`, `stackTrace`, `Exception`, `@x`)
  - trace/span (`traceId|TraceId|@tr`, `spanId|SpanId|@sp`)
- [x] 12A.7.2. Apply additive canonical projection rules without destructive renaming of source fields.
- [x] 12A.7.3. Define rendered-message vs template-message precedence defaults while preserving both when available.
- [x] 12A.7.4. Preserve ecosystem namespaces as raw fields (`Properties.*`, `attributes.*`, wrapper metadata) where present.

### 12A.8. Fixtures, Unit Tests, and Regression Coverage
- [x] 12A.8.1. Add parser fixtures for generic JSON object logs (flat + nested + arrays + nulls).
- [x] 12A.8.2. Add baseline JVM fixtures (Logstash-style JSON, Log4j2-style JSON field names, Spring JSON variant).
- [x] 12A.8.3. Add baseline .NET fixtures (MEL JSON console, Serilog compact keys minimum set).
- [x] 12A.8.4. Add tests for malformed JSON fallback behavior and confidence-threshold parser selection.
- [x] 12A.8.5. Add/extend regression tests proving plain-text/template parsing behavior is unchanged.
- [x] 12A.8.6. Add tests validating raw + canonical coexistence and path-index generation.

### 12A.9. Documentation and Decision Tracking
- [x] 12A.9.1. Update sprint epic progress markers in `docs/sprints/sprint-12-structured-data.md` for completed foundation items.
- [x] 12A.9.2. Add developer notes for structured model contract, path flattening rules, and baseline alias mapping.
- [x] 12A.9.3. Document known exclusions deferred to 12B/12C/12D/12E.

### 12A.10. Verification & Quality Gates
- [x] 12A.10.1. Run/verify `:core` unit tests covering parser hardening, normalization, and path indexing.
- [x] 12A.10.2. Run/verify affected `:ui` and integration regression tests that validate plain-text/template behavior.
- [x] 12A.10.3. Run static analysis with `./gradlew detekt`.
- [x] 12A.10.4. Run relevant module tests for changed modules.
- [x] 12A.10.5. Run broader `./gradlew check` before closing 12A when feasible.

### 12A.11. Acceptance Criteria
- [x] Opening a one-line JSON log file can auto-select JSON parsing when confidence is sufficient.
- [x] Structured payloads retain raw source fields while exposing canonical fields for baseline concepts.
- [x] Nested objects/arrays are represented in a typed structure and flattened path index contract.
- [x] Baseline canonical aliasing works for representative JVM/.NET JSON fixtures.
- [x] Low-confidence or malformed records degrade gracefully to existing template/simple parsing behavior.
- [x] Existing plain-text syslog/template parsing behavior remains unchanged.
