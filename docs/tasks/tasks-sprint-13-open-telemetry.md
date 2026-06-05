# TASKS: Sprint 13 - OpenTelemetry Integration

## 18. Sprint 13: OpenTelemetry Integration

### 18.1. Discovery and Architecture
- [ ] 18.1.1. Confirm Sprint 13 OpenTelemetry signal scope (logs, traces, baseline metrics) and explicitly document out-of-scope items.
- [ ] 18.1.2. Define supported OTLP payload versions/profiles for initial compatibility.
- [ ] 18.1.3. Publish an ADR for adapter boundaries across `:core`, `:domain`, and `:ui`.
- [ ] 18.1.4. Define correlation UX flow for trace-first investigation (`logs by trace`, `spans by trace`, `range correlation`).
- [ ] 18.1.5. Build a representative fixture corpus for valid, partial, and malformed OpenTelemetry payloads.

### 18.2. Dependencies and Configuration
- [ ] 18.2.1. Add OpenTelemetry-related dependency versions to `gradle/libs.versions.toml`.
- [ ] 18.2.2. Wire dependencies using Gradle Kotlin DSL aliases in consuming modules.
- [ ] 18.2.3. Add configuration flags for OTLP ingestion enablement.
- [ ] 18.2.4. Add configuration for attribute indexing depth limits and high-cardinality controls.
- [ ] 18.2.5. Add configuration for max payload size, buffering, and backpressure behavior.

### 18.3. Domain Modeling (`:domain`)
- [ ] 18.3.1. Add tiny types for telemetry identifiers (`TraceId`, `SpanId`, and related IDs).
- [ ] 18.3.2. Extend domain model for OpenTelemetry hierarchy (`Resource` -> `Scope` -> `Record`).
- [ ] 18.3.3. Add typed attribute representation (string, bool, numeric, array, key-value list).
- [ ] 18.3.4. Define canonical OpenTelemetry field mappings for logs/spans/metrics.
- [ ] 18.3.5. Define fallback behavior for unknown or missing semantic convention fields.

### 18.4. Ingestion and Adapter Layer (`:core`)
- [ ] 18.4.1. Implement file-based OTLP ingestion path for logs and traces.
- [ ] 18.4.2. Implement one supported live-ingestion OTLP-aligned path.
- [ ] 18.4.3. Implement OpenTelemetry adapter mapping from decoded payloads to domain models.
- [ ] 18.4.4. Map canonical fields (`resource`, `scope`, `traceId`, `spanId`, `parentSpanId`, severity/body/attributes/events).
- [ ] 18.4.5. Implement record-level resilience (skip malformed records, continue stream/file processing).
- [ ] 18.4.6. Add bounded queues and backpressure controls for live ingestion.
- [ ] 18.4.7. Log parser and adapter failures with structured diagnostic context.

### 18.5. Filtering, Search, and Correlation
- [ ] 18.5.1. Extend indexing/query pipeline for OpenTelemetry fields and attributes.
- [ ] 18.5.2. Implement filter/search support for `traceId`, `spanId`, `service.name`, and `span.name`.
- [ ] 18.5.3. Implement correlation action: show logs for selected trace.
- [ ] 18.5.4. Implement correlation action: show spans for selected trace.
- [ ] 18.5.5. Implement correlation action: show related telemetry for selected time range.
- [ ] 18.5.6. Add timeline highlighting for rows sharing the same `traceId`.
- [ ] 18.5.7. Persist correlation/filter context across tabs and project save/load.

### 18.6. Metrics Support and Summaries
- [ ] 18.6.1. Add ingestion support for counters/sums.
- [ ] 18.6.2. Add ingestion support for gauges.
- [ ] 18.6.3. Add initial read/display support for histograms.
- [ ] 18.6.4. Implement lightweight metric summary aggregation for investigation workflows.
- [ ] 18.6.5. Add clear UI messaging for unsupported temporality/aggregation variants.

### 18.7. UI and UX Delivery (`:ui`)
- [ ] 18.7.1. Add OpenTelemetry source/profile options to ingestion dialogs.
- [ ] 18.7.2. Extend entry details pane with sections for resource, scope, trace/span identity, and typed attributes.
- [ ] 18.7.3. Add correlation-focused quick actions to row/context menus.
- [ ] 18.7.4. Add filter-builder suggestions for key OpenTelemetry fields.
- [ ] 18.7.5. Ensure telemetry-specific loading, empty, and error states are implemented.
- [ ] 18.7.6. Add user-facing warnings/remediation for unsupported format, schema mismatch, and oversized payloads.

### 18.8. Export, Interoperability, and Privacy
- [ ] 18.8.1. Implement structured export that preserves telemetry identifiers and key attributes.
- [ ] 18.8.2. Implement fallback export behavior for unsupported/partial fields.
- [ ] 18.8.3. Implement configurable redaction/masking for sensitive telemetry keys.
- [ ] 18.8.4. Enforce conservative network defaults (localhost binding unless explicitly changed).
- [ ] 18.8.5. Validate and constrain ingest payload sizes to reduce abuse risk.

### 18.9. Testing and Quality Hardening
- [ ] 18.9.1. Add unit tests for OTLP parsing and adapter mapping (happy path, malformed, partial).
- [ ] 18.9.2. Add unit tests for typed attribute conversion and fallback behavior.
- [ ] 18.9.3. Add unit tests for filter and correlation logic (`traceId`/`spanId` workflows).
- [ ] 18.9.4. Add integration tests for source -> parser -> domain -> UI state pipeline.
- [ ] 18.9.5. Add integration tests for mixed-source sessions (plain logs + OpenTelemetry).
- [ ] 18.9.6. Add integration tests for export round-trip integrity of key fields.
- [ ] 18.9.7. Add UI tests for telemetry details rendering and correlation interactions.
- [ ] 18.9.8. Add UI tests for OpenTelemetry filter-builder field suggestions.
- [ ] 18.9.9. Run performance checks using large telemetry fixtures and tune indexing/buffering.
- [ ] 18.9.10. Run sprint-end cyclomatic complexity review and decide reduction actions.

### 18.10. Documentation and Rollout
- [ ] 18.10.1. Update `README.md` with OpenTelemetry capabilities, supported inputs, and limitations.
- [ ] 18.10.2. Add operator/developer troubleshooting and configuration documentation.
- [ ] 18.10.3. Publish ADR(s) for data model and adapter boundary decisions.
- [ ] 18.10.4. Update `RELEASE_NOTES.md` and sprint artifacts with OpenTelemetry milestones.
- [ ] 18.10.5. Document privacy and PII/secret handling expectations for imported telemetry.

### 18.11. Acceptance and Sprint Completion
- [ ] 18.11.1. Verify users can ingest OpenTelemetry logs/traces from at least one file source and one live source.
- [ ] 18.11.2. Verify `traceId` and `spanId` are visible and filterable in the UI.
- [ ] 18.11.3. Verify correlation actions reliably surface related telemetry for selected traces.
- [ ] 18.11.4. Verify core metric summaries are available for supported point types.
- [ ] 18.11.5. Verify malformed payloads do not crash the app and produce clear user-facing errors.
- [ ] 18.11.6. Verify all Sprint 13 documentation and ADR deliverables are complete.
- [ ] 18.11.7. Update `docs/project_memory.md` with Sprint 13 shipped scope, decisions, gotchas, and test coverage areas.