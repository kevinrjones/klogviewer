# TASKS: Sprint 19 - OpenTelemetry Integration

## 18. Sprint 19: OpenTelemetry Integration

### 19.1. Discovery and Architecture
- [ ] 19.1.1. Confirm Sprint 19 OpenTelemetry signal scope (logs, traces, baseline metrics) and explicitly document out-of-scope items.
- [ ] 19.1.2. Define supported OTLP payload versions/profiles for initial compatibility.
- [ ] 19.1.3. Publish an ADR for adapter boundaries across `:core`, `:domain`, and `:ui`.
- [ ] 19.1.4. Define correlation UX flow for trace-first investigation (`logs by trace`, `spans by trace`, `range correlation`).
- [ ] 19.1.5. Build a representative fixture corpus for valid, partial, and malformed OpenTelemetry payloads.

### 19.2. Dependencies and Configuration
- [ ] 19.2.1. Add OpenTelemetry-related dependency versions to `gradle/libs.versions.toml`.
- [ ] 19.2.2. Wire dependencies using Gradle Kotlin DSL aliases in consuming modules.
- [ ] 19.2.3. Add configuration flags for OTLP ingestion enablement.
- [ ] 19.2.4. Add configuration for attribute indexing depth limits and high-cardinality controls.
- [ ] 19.2.5. Add configuration for max payload size, buffering, and backpressure behavior.

### 19.3. Domain Modeling (`:domain`)
- [ ] 19.3.1. Add tiny types for telemetry identifiers (`TraceId`, `SpanId`, and related IDs).
- [ ] 19.3.2. Extend domain model for OpenTelemetry hierarchy (`Resource` -> `Scope` -> `Record`).
- [ ] 19.3.3. Add typed attribute representation (string, bool, numeric, array, key-value list).
- [ ] 19.3.4. Define canonical OpenTelemetry field mappings for logs/spans/metrics.
- [ ] 19.3.5. Define fallback behavior for unknown or missing semantic convention fields.

### 19.4. Ingestion and Adapter Layer (`:core`)
- [ ] 19.4.1. Implement file-based OTLP ingestion path for logs and traces.
- [ ] 19.4.2. Implement one supported live-ingestion OTLP-aligned path.
- [ ] 19.4.3. Implement OpenTelemetry adapter mapping from decoded payloads to domain models.
- [ ] 19.4.4. Map canonical fields (`resource`, `scope`, `traceId`, `spanId`, `parentSpanId`, severity/body/attributes/events).
- [ ] 19.4.5. Implement record-level resilience (skip malformed records, continue stream/file processing).
- [ ] 19.4.6. Add bounded queues and backpressure controls for live ingestion.
- [ ] 19.4.7. Log parser and adapter failures with structured diagnostic context.

### 19.5. Filtering, Search, and Correlation
- [ ] 19.5.1. Extend indexing/query pipeline for OpenTelemetry fields and attributes.
- [ ] 19.5.2. Implement filter/search support for `traceId`, `spanId`, `service.name`, and `span.name`.
- [ ] 19.5.3. Implement correlation action: show logs for selected trace.
- [ ] 19.5.4. Implement correlation action: show spans for selected trace.
- [ ] 19.5.5. Implement correlation action: show related telemetry for selected time range.
- [ ] 19.5.6. Add timeline highlighting for rows sharing the same `traceId`.
- [ ] 19.5.7. Persist correlation/filter context across tabs and project save/load.

### 19.6. Metrics Support and Summaries
- [ ] 19.6.1. Add ingestion support for counters/sums.
- [ ] 19.6.2. Add ingestion support for gauges.
- [ ] 19.6.3. Add initial read/display support for histograms.
- [ ] 19.6.4. Implement lightweight metric summary aggregation for investigation workflows.
- [ ] 19.6.5. Add clear UI messaging for unsupported temporality/aggregation variants.

### 19.7. UI and UX Delivery (`:ui`)
- [ ] 19.7.1. Add OpenTelemetry source/profile options to ingestion dialogs.
- [ ] 19.7.2. Extend entry details pane with sections for resource, scope, trace/span identity, and typed attributes.
- [ ] 19.7.3. Add correlation-focused quick actions to row/context menus.
- [ ] 19.7.4. Add filter-builder suggestions for key OpenTelemetry fields.
- [ ] 19.7.5. Ensure telemetry-specific loading, empty, and error states are implemented.
- [ ] 19.7.6. Add user-facing warnings/remediation for unsupported format, schema mismatch, and oversized payloads.

### 19.8. Export, Interoperability, and Privacy
- [ ] 19.8.1. Implement structured export that preserves telemetry identifiers and key attributes.
- [ ] 19.8.2. Implement fallback export behavior for unsupported/partial fields.
- [ ] 19.8.3. Implement configurable redaction/masking for sensitive telemetry keys.
- [ ] 19.8.4. Enforce conservative network defaults (localhost binding unless explicitly changed).
- [ ] 19.8.5. Validate and constrain ingest payload sizes to reduce abuse risk.

### 19.9. Testing and Quality Hardening
- [ ] 19.9.1. Add unit tests for OTLP parsing and adapter mapping (happy path, malformed, partial).
- [ ] 19.9.2. Add unit tests for typed attribute conversion and fallback behavior.
- [ ] 19.9.3. Add unit tests for filter and correlation logic (`traceId`/`spanId` workflows).
- [ ] 19.9.4. Add integration tests for source -> parser -> domain -> UI state pipeline.
- [ ] 19.9.5. Add integration tests for mixed-source sessions (plain logs + OpenTelemetry).
- [ ] 19.9.6. Add integration tests for export round-trip integrity of key fields.
- [ ] 19.9.7. Add UI tests for telemetry details rendering and correlation interactions.
- [ ] 19.9.8. Add UI tests for OpenTelemetry filter-builder field suggestions.
- [ ] 19.9.9. Run performance checks using large telemetry fixtures and tune indexing/buffering.
- [ ] 19.9.10. Run sprint-end cyclomatic complexity review and decide reduction actions.

### 19.10. Documentation and Rollout
- [ ] 19.10.1. Update `README.md` with OpenTelemetry capabilities, supported inputs, and limitations.
- [ ] 19.10.2. Add operator/developer troubleshooting and configuration documentation.
- [ ] 19.10.3. Publish ADR(s) for data model and adapter boundary decisions.
- [ ] 19.10.4. Update `RELEASE_NOTES.md` and sprint artifacts with OpenTelemetry milestones.
- [ ] 19.10.5. Document privacy and PII/secret handling expectations for imported telemetry.

### 19.11. Acceptance and Sprint Completion
- [ ] 19.11.1. Verify users can ingest OpenTelemetry logs/traces from at least one file source and one live source.
- [ ] 19.11.2. Verify `traceId` and `spanId` are visible and filterable in the UI.
- [ ] 19.11.3. Verify correlation actions reliably surface related telemetry for selected traces.
- [ ] 19.11.4. Verify core metric summaries are available for supported point types.
- [ ] 19.11.5. Verify malformed payloads do not crash the app and produce clear user-facing errors.
- [ ] 19.11.6. Verify all Sprint 19 documentation and ADR deliverables are complete.
- [ ] 19.11.7. Update `docs/project_memory.md` with Sprint 19 shipped scope, decisions, gotchas, and test coverage areas.