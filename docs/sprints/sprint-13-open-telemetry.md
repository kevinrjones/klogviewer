# Sprint 13: OpenTelemetry Integration

## 1. Sprint Goal and Scope
Deliver first-class OpenTelemetry support so KLogViewer can ingest, inspect, correlate, and analyze telemetry data (logs, traces, and metrics) in a workflow consistent with existing log analysis features.

### In Scope
- OpenTelemetry (OTLP) ingestion path for logs and traces, plus metric summaries where practical.
- Domain model and parser extensions to represent OpenTelemetry resources, scopes, attributes, trace/span identity, and metric points.
- UI affordances for OpenTelemetry-specific filtering, correlation, and detail inspection.
- Export and interoperability support for common OpenTelemetry pipelines.

### Out of Scope
- Building a full OpenTelemetry collector replacement.
- Distributed tracing backend storage at scale beyond local/session-oriented analysis.
- Long-term telemetry retention and multi-tenant server deployment.

## 2. User-Facing Value
- Engineers can inspect OpenTelemetry telemetry in one place without switching tools for basic diagnostics.
- Users can correlate logs to traces/spans quickly to reduce mean time to root cause.
- Teams can validate instrumentation quality by seeing resources, attributes, and semantic conventions directly in the viewer.

## 3. OpenTelemetry Use Cases for KLogViewer
- Investigate a failing request by filtering logs with a `traceId`, then drilling into related spans.
- Compare service-level error spikes against metric changes in the same time window.
- Verify that OpenTelemetry attributes (service name, version, deployment environment) are emitted correctly.
- Load OTLP exports from CI test runs and review regressions before release.
- Analyze mixed inputs (plain logs + OpenTelemetry logs/traces) within one timeline.

## 4. Functional Requirements
- Support importing OpenTelemetry logs and traces from file-based payloads and stream-based ingestion.
- Parse OTLP payload structure into existing and extended domain representations.
- Surface canonical OpenTelemetry fields:
  - `resource` attributes
  - `scope` (instrumentation scope)
  - `traceId`, `spanId`, `parentSpanId`
  - severity/text body/attributes/events for logs and spans
- Provide filtering and search on OpenTelemetry keys and values.
- Provide correlation actions:
  - "Show logs for this trace"
  - "Show spans for this trace"
  - "Show related telemetry in selected time range"
- Support exporting selected OpenTelemetry-derived rows in a portable structured format.

## 5. Non-Functional Requirements
- Keep interactive filtering responsive for large datasets (target: sub-second query response for common filters on typical workstation-sized sessions).
- Use bounded memory strategies for large OTLP payloads and streams.
- Maintain compatibility with existing non-OpenTelemetry log workflows.
- Ensure deterministic parsing behavior with clear error reporting for malformed telemetry data.

## 6. Architecture and Design Approach
- Add an OpenTelemetry adapter layer that maps OTLP payloads into KLogViewer domain models.
- Isolate protocol decoding from UI concerns:
  - Decoder/Adapter in `:core`
  - Shared telemetry domain in `:domain`
  - Presentation mapping in `:ui`
- Reuse existing timeline/filter pipeline where possible, extending field indexing for telemetry attributes.
- Keep extension points explicit so additional protocol profiles can be added without rewriting core ingestion.

## 7. Dependency and Configuration Strategy
- Add OpenTelemetry protocol dependencies in `libs.versions.toml` and wire via Gradle Kotlin DSL aliases.
- Prefer minimal dependency surface:
  - OTLP protobuf/data model parsing dependencies only where needed
  - Avoid bundling collector/runtime components not needed by desktop viewer workflows
- Add feature toggles/configuration for:
  - OTLP ingestion enablement
  - attribute indexing depth limits
  - maximum payload size and buffering policies

## 8. Telemetry Data Model Considerations
- Preserve OpenTelemetry identity boundaries:
  - Resource -> Scope -> Record (log/span/metric)
- Support typed attribute values (string, bool, numeric, array, key-value list) without flattening away type information.
- Add tiny-type wrappers where appropriate for identifiers (`TraceId`, `SpanId`) to reduce accidental misuse.
- Define clear fallback behavior for unknown/missing semantic convention fields.

## 9. Log Ingestion and Export Support
- Ingestion:
  - File import of OpenTelemetry-encoded payloads.
  - Network ingestion profile for OTLP-aligned payloads where listener support exists.
- Export:
  - Structured export retaining OpenTelemetry identifiers and key attributes.
  - Safe fallback export for unsupported/partial fields.

## 10. Trace and Span Correlation
- Add trace-first navigation from log details and table context menus.
- Show span ancestry fields in detail panes (`spanId`, `parentSpanId`) when present.
- Support timeline highlighting for all telemetry rows sharing a `traceId`.
- Preserve correlation context through filters, tab persistence, and project save/load.

## 11. Metrics Support
- Support ingestion of core metric point types needed for investigation workflows:
  - counters/sums
  - gauges
  - histograms (initial read/display support)
- Provide lightweight metric summaries and trend visualization hooks in the dashboard.
- Explicitly mark unsupported metric temporality/aggregation variants with clear UI messaging.

## 12. UI and UX Changes
- Add an OpenTelemetry source/profile option in ingestion dialogs where applicable.
- Extend entry details panel with sections for resource, scope, trace/span, and typed attributes.
- Add correlation-focused quick actions in context menus.
- Add filter builder suggestions for OpenTelemetry fields (e.g., `traceId`, `service.name`, `span.name`).
- Ensure loading/empty/error states are present for telemetry-specific views.

## 13. Error Handling and Resilience
- Fail malformed record parsing at record granularity (skip bad records, keep processing stream/file).
- Log parser/adapter exceptions with structured context for diagnostics.
- Provide user-facing warnings with actionable remediation (unsupported format, oversized payload, schema mismatch).
- Apply backpressure and bounded queues for live ingestion to avoid UI starvation.

## 14. Security and Privacy Considerations
- Treat telemetry attributes as potentially sensitive:
  - support redaction/masking of configured keys
  - avoid verbose accidental exposure in logs
- Keep network listener defaults conservative (localhost binding unless explicitly changed).
- Validate and constrain payload sizes to reduce abuse risk.
- Document handling expectations for PII/secrets in imported telemetry.

## 15. Testing Strategy
- Unit tests:
  - OTLP parsing and adapter mapping (happy path, malformed payloads, partial fields).
  - Correlation and filter behavior for `traceId`/`spanId` workflows.
  - Typed attribute conversion and fallback handling.
- Integration tests:
  - Ingestion pipeline from source -> parser -> domain -> UI state.
  - Mixed-source sessions (plain logs plus OpenTelemetry telemetry).
  - Export round-trip integrity for key fields.
- UI tests:
  - Telemetry details rendering and correlation actions.
  - Filter builder OpenTelemetry field suggestions.

## 16. Documentation Updates
- Update `README.md` with OpenTelemetry capabilities, supported inputs, and limitations.
- Add operator/developer docs for configuration and troubleshooting.
- Add an ADR documenting architecture choices for OpenTelemetry adapter boundaries and data model decisions.
- Update release notes and sprint/task artifacts to include OpenTelemetry milestones.

## 17. Acceptance Criteria
- [ ] Users can ingest OpenTelemetry logs and traces from at least one file-based source and one supported live-ingestion path.
- [ ] `traceId` and `spanId` are visible and filterable in the UI.
- [ ] Correlation actions reliably surface related telemetry for a selected trace.
- [ ] Core metric summaries are available for supported point types.
- [ ] Malformed payloads do not crash the app; users receive clear errors.
- [ ] Documentation and ADR for OpenTelemetry integration are completed.

## 18. Risks and Mitigations
- **Risk**: Payload/schema diversity across SDK versions.
  - **Mitigation**: Version-tolerant parser strategy and compatibility fixtures.
- **Risk**: High-cardinality attributes degrade performance.
  - **Mitigation**: Configurable indexing limits and selective indexing defaults.
- **Risk**: Correlation UX becomes complex.
  - **Mitigation**: Start with trace-first workflows and progressive disclosure in UI.
- **Risk**: Sensitive data exposure.
  - **Mitigation**: Redaction controls, conservative logging, and explicit privacy guidance.

## 19. Detailed Task Breakdown

### 19.1. Discovery and Design
- [ ] Define supported OpenTelemetry signal subset for Sprint 13 (logs, traces, baseline metrics).
- [ ] Publish ADR for adapter boundaries, domain model extensions, and correlation design.
- [ ] Produce sample fixture corpus covering representative OTLP payload variations.

### 19.2. Core Ingestion and Modeling
- [ ] Add dependencies and protocol decoding wiring in Gradle/version catalog.
- [ ] Implement OTLP adapter(s) in `:core` and map to domain models.
- [ ] Introduce tiny types and typed attribute representations in `:domain` where required.
- [ ] Add resilient record-level error handling and bounded buffering behavior.

### 19.3. Filtering and Correlation
- [ ] Extend indexing/query pipeline for OpenTelemetry resource and trace/span fields.
- [ ] Implement correlation use cases (`logs by trace`, `spans by trace`, `range correlation`).
- [ ] Persist OpenTelemetry filter state in project/workspace persistence.

### 19.4. Metrics and Visualization
- [ ] Implement baseline metric point ingestion and summary aggregation.
- [ ] Integrate metric summaries into dashboard surfaces where applicable.
- [ ] Mark unsupported metric variants clearly in UI.

### 19.5. UI/UX Delivery
- [ ] Add OpenTelemetry source/profile options to ingestion workflows.
- [ ] Extend detail panes for resource/scope/trace/span/attributes.
- [ ] Add context menu actions for trace-driven investigation workflows.
- [ ] Ensure loading/error/empty states for telemetry-specific screens.

### 19.6. Quality and Hardening
- [ ] Add unit and integration tests for parsing, mapping, filtering, and correlation.
- [ ] Add UI tests for telemetry rendering and interaction flows.
- [ ] Run performance checks on large telemetry fixtures and tune indexing/buffering.

### 19.7. Documentation and Release Readiness
- [ ] Update `README.md`, troubleshooting docs, and release notes with OpenTelemetry behavior.
- [ ] Document known limitations and rollout guidance for early adopters.
- [ ] Verify sprint acceptance criteria and complete sprint recap updates.
