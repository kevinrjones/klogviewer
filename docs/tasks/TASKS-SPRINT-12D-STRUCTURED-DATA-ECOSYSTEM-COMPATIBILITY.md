# TASKS: Sprint 12D - Structured Data Ecosystem Compatibility

## 12D. Structured Data Ecosystem Compatibility Pack

### 12D.0. Goal
Expand structured parsing and normalization compatibility for real-world JVM, .NET, container, and cloud logging formats using fixture-driven validation and explicit supported-format documentation.

### 12D.1. In Scope
- JVM compatibility targets:
  - LogStash Logback JSON
  - Logback JSON with MDC
  - Spring Boot structured JSON logs
  - Log4j2 JSONLayout / JsonTemplateLayout
- .NET compatibility targets:
  - Microsoft.Extensions.Logging JSON console output
  - Serilog compact JSON
  - Serilog rendered compact JSON
  - Serilog standard JSON output
  - Serilog ASP.NET Core request logs
  - NLog JSON layout
  - log4net JSON-style output where feasible
- Container/cloud wrappers:
  - Docker JSON log wrapper
  - Kubernetes/container runtime wrappers
  - OpenTelemetry-like JSON logs
  - Cloud-provider envelopes with nested app payloads
- Ecosystem-specific aliasing and normalization refinements.
- Fixture files and detection/normalization tests per supported format.
- Documentation of supported formats and known limitations.

### 12D.2. Out of Scope
- New major detail-pane or list UI features (12C/12E).
- Major structured filtering redesign beyond compatibility with existing grammar (12B).
- Heavy performance optimization or parser architecture rewrites unless a blocking compatibility defect requires targeted remediation.
- Rare/unsupported formats with high complexity and low usage unless they are low-cost wins.

### 12D.3. Dependencies
- Required: `TASKS-SPRINT-12A-STRUCTURED-DATA-FOUNDATION.md`.
- Recommended: `TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md` for validating filter semantics against newly normalized aliases.
- Optional: `TASKS-SPRINT-12C-STRUCTURED-DATA-INSPECTOR.md` for richer manual verification UX.

### 12D.4. Scope-to-Workstream Mapping
- JVM parser compatibility (`5`) -> JSON parser normalization/detection fixtures for Logback/Log4j2/Spring conventions.
- .NET compatibility and normalization (`6`) -> MEL/Serilog/NLog/log4net alias and field-priority mapping.
- Container/cloud envelope handling (`7`) -> wrapper unwrapping and nested payload path indexing.
- Compatibility verification (`8`) -> fixture-driven parser + detection + normalization tests.
- Docs and support matrix (`9`) -> supported formats, caveats, and deferred gaps.

### 12D.5. JVM Compatibility Workstream
- [ ] 12D.5.1. Add fixtures for LogStash Logback JSON including MDC and trace/span fields.
- [ ] 12D.5.2. Add fixtures for Spring Boot JSON structured output variants.
- [ ] 12D.5.3. Add fixtures for Log4j2 JSONLayout/JsonTemplateLayout field naming variants.
- [ ] 12D.5.4. Ensure canonical extraction for timestamp/level/message/logger/exception/trace/span across JVM fixtures.
- [ ] 12D.5.5. Validate raw namespace preservation for MDC and nested payload sections.

### 12D.6. .NET Compatibility Workstream
- [ ] 12D.6.1. Add MEL JSON console fixtures covering `Timestamp`, `LogLevel`, `Category`, `EventId`, `Message`, `Scopes`, and exception fields.
- [ ] 12D.6.2. Add Serilog compact fixtures including `@t`, `@mt`, `@m`, `@l`, `@x`, `@i`, `@tr`, and `@sp` combinations.
- [ ] 12D.6.3. Add Serilog rendered compact and standard sink fixtures with `Properties.*` and request metadata fields.
- [ ] 12D.6.4. Add Serilog ASP.NET Core request log fixtures (`RequestPath`, `RequestMethod`, `StatusCode`, `Elapsed`, `RequestId`, `TraceId`, `SpanId`).
- [ ] 12D.6.5. Add NLog JSON layout fixtures with context properties.
- [ ] 12D.6.6. Add log4net JSON-style fixtures where feasible and document unsupported variants.
- [ ] 12D.6.7. Define deterministic rendered-message vs template precedence for Serilog/MEL while preserving both values.

### 12D.7. Container / Cloud / OTel Compatibility Workstream
- [ ] 12D.7.1. Add fixtures for Docker JSON wrapper with nested app-event JSON in `log` field.
- [ ] 12D.7.2. Add fixtures for Kubernetes/CRI wrapper metadata plus structured nested payload.
- [ ] 12D.7.3. Add OTel-like fixtures with `timeUnixNano`, `severityText`, `body`, `resource.*`, and `attributes.*` fields.
- [ ] 12D.7.4. Add cloud-envelope fixtures where application event is nested under provider metadata.
- [ ] 12D.7.5. Validate wrapper metadata is preserved while nested app payload remains canonicalized and filterable.

### 12D.8. Verification & Quality Gates
- [ ] 12D.8.1. Add/extend parser unit tests for each supported ecosystem fixture.
- [ ] 12D.8.2. Add/extend heuristic detection tests to verify parser selection on representative samples.
- [ ] 12D.8.3. Add/extend normalization tests for canonical field extraction and raw field retention.
- [ ] 12D.8.4. Add/extend regression tests for mixed structured/plain files and fallback behavior.
- [ ] 12D.8.5. Add/extend integration tests validating alias-aware filtering on representative compatibility fixtures.
- [ ] 12D.8.6. Run static analysis with `./gradlew detekt`.
- [ ] 12D.8.7. Run relevant module tests for touched modules.
- [ ] 12D.8.8. Run broader `./gradlew check` before closing 12D when feasible.

### 12D.9. Documentation and Support Matrix
- [ ] 12D.9.1. Publish/update supported-ecosystem matrix with examples and expected canonical extraction behavior.
- [ ] 12D.9.2. Document known limitations and explicit non-goals per ecosystem format.
- [ ] 12D.9.3. Update sprint epic progress markers for compatibility-slice completion.
- [ ] 12D.9.4. Add fixture catalog references for future regression triage.

### 12D.10. Acceptance Criteria
- [ ] Representative JVM/.NET/container/cloud fixture sets parse without breaking existing ingestion flows.
- [ ] Canonical fields are extracted consistently for all supported ecosystem fixtures.
- [ ] Serilog compact and rendered compact events preserve both message template and rendered message with documented precedence.
- [ ] MEL and ASP.NET request metadata fields are normalized and remain available as raw fields.
- [ ] Container/cloud wrappers preserve envelope metadata while still parsing nested application logs.
- [ ] Unsupported or partially supported variants are explicitly documented with fallback behavior.
