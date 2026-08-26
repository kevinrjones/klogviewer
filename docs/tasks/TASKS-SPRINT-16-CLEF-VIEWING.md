# TASKS: Sprint 16 - Serilog Compact Log Event Viewing

## 15. Sprint 16: Serilog Compact Log Event Viewing

### 15.0. Goal
Deliver first-class Serilog compact JSON viewing in KLogViewer using a structured event view with robust detection, override control, and fixture-backed validation.

### 15.1. In Scope
- CLEF-specific detection confidence and classification.
- Canonical extraction/normalization for CLEF reified fields and user properties.
- Structured list/details/filtering/dashboard behavior for compact events.
- User override control and persistence for format interpretation.
- Fixture-driven tests for mixed/malformed/large/live-tail scenarios.

### 15.2. Out of Scope
- Server-side ingestion and query platform capabilities.
- Full command-line parity with `clef-tool` or `seqcli`.
- Distributed tracing visualization beyond trace/span identifiers and filters.
- Complete message-template rendering parity for every Serilog edge case.

### 15.3. Dependencies
- `docs/sprints/sprint-16-clef-viewing.md`
- Structured-data foundations from Sprint 12 artifacts and related parser/domain/ui modules.
- Existing detection and canonicalization utilities in `:core`.

### 15.4. Scope-to-Workstream Mapping
- Research + source grounding (`15.5`) -> CLEF semantics and external tool capability baseline.
- Fixtures (`15.6`) -> deterministic samples for positive/negative/edge scenarios.
- Detection/parser/model (`15.7`, `15.8`) -> format confidence and canonical extraction.
- Rendering UX (`15.9`, `15.10`, `15.11`) -> list/details and copy flows.
- Query + analysis (`15.12`, `15.13`) -> property filtering and grouping behavior.
- Control/perf/resilience (`15.14`, `15.15`, `15.16`) -> override persistence and guardrails.
- Quality/doc/closure (`15.17`, `15.18`, `15.19`) -> tests, docs, verification, and completion evidence.

### 16.5. Research and Gap Analysis
- [ ] 16.5.1. Review CLEF schema and stream semantics from `https://clef-json.org/`.
- [ ] 16.5.2. Capture reified field behavior for `@t`, `@mt`, `@m`, `@l`, `@x`, `@i`, `@r`, `@tr`, `@sp`.
- [ ] 16.5.3. Capture Seq ingestion/media-type details from `https://datalust.co/docs/posting-raw-events`.
- [ ] 16.5.4. Compare KLogViewer against `clef-tool`, Compact Log Viewer, Seq, LogViewPlus, and CLEF-page-listed tools/resources.
- [ ] 16.5.5. Classify findings into must-have, low-risk should-have, deferred, and explicit non-goal.

### 16.6. Fixtures and Sample Data
- [ ] 16.6.1. Add canonical CLEF fixture set with rendered message, template, level, exception, event id, trace/span fields.
- [ ] 16.6.2. Add generic NDJSON fixtures that must not be detected as Serilog compact JSON.
- [ ] 16.6.3. Add mixed-file fixtures (CLEF + plain text) for fallback behavior.
- [ ] 16.6.4. Add malformed-line fixtures with recoverable parser behavior expectations.
- [ ] 16.6.5. Add large-file and live-tail append fixtures for performance and responsiveness checks.

### 16.7. CLEF Detection
- [ ] 16.7.1. Define confidence signals requiring multiple CLEF reified-field indicators.
- [ ] 16.7.2. Implement confidence tiers: strong CLEF, probable CLEF, generic JSON.
- [ ] 16.7.3. Ensure timestamp and level heuristics validate `@t` and `@l` quality.
- [ ] 16.7.4. Ensure detection avoids false positives for generic JSON streams.
- [ ] 16.7.5. Surface non-intrusive detection indicator text: `Detected: Serilog compact JSON`.

### 16.8. Parser and Normalization
- [ ] 16.8.1. Extend structured parser path for newline-delimited compact events.
- [ ] 16.8.2. Preserve raw event text and parsed object for every candidate line.
- [ ] 16.8.3. Normalize reified fields into canonical aliases for list/filter/dashboard usage.
- [ ] 16.8.4. Preserve top-level user properties and stable nested property paths.
- [ ] 16.8.5. Handle unknown or future `@` fields without lossy transformation.

### 16.9. Message Rendering
- [ ] 16.9.1. Prefer rendered message from `@m` when available.
- [ ] 16.9.2. Preserve message template from `@mt` as a first-class field.
- [ ] 16.9.3. Add fallback rendering when `@m` is absent and `@mt` exists.
- [ ] 16.9.4. Expose both rendered and template values in event details view.

### 16.10. List/Table Presentation
- [ ] 16.10.1. Render timestamp from `@t` with existing time-format settings.
- [ ] 16.10.2. Map and render Serilog levels from `@l` (`Verbose`, `Debug`, `Information`, `Warning`, `Error`, `Fatal`).
- [ ] 16.10.3. Surface exception summary from `@x` and event id from `@i`.
- [ ] 16.10.4. Surface trace/span identifiers from `@tr` and `@sp`.
- [ ] 16.10.5. Add controlled user-property columns without overwhelming the table.

### 16.11. Structured Event Details / Inspector
- [ ] 16.11.1. Show raw compact JSON event in details panel.
- [ ] 16.11.2. Show normalized canonical fields beside raw JSON.
- [ ] 16.11.3. Show event properties as searchable/expandable tree.
- [ ] 16.11.4. Format exception text and stack trace for readability.
- [ ] 16.11.5. Add copy actions for raw event, rendered message, template, exception, and selected property path/value.

### 16.12. Filtering and Search
- [ ] 16.12.1. Add/validate level filters for mapped Serilog levels.
- [ ] 16.12.2. Add/validate search across rendered message, message template, and exception text.
- [ ] 16.12.3. Add/validate property-path filtering for reified and user fields.
- [ ] 16.12.4. Add/validate trace id, span id, and event id filtering semantics.
- [ ] 16.12.5. Support missing-property semantics and quoted values with spaces.
- [ ] 16.12.6. Preserve compatibility with plain-text and generic JSON filter behavior.

### 16.13. Dashboard and Frequency Analysis
- [ ] 16.13.1. Include CLEF canonical fields in frequency analysis inputs.
- [ ] 16.13.2. Support grouping by level, event id, trace id, span id, source context, request path, and status code where available.
- [ ] 16.13.3. Enforce high-cardinality guardrails for trace/request/user identifiers.
- [ ] 16.13.4. Keep dashboard behavior stable for non-CLEF datasets.

### 16.14. UI Override and Persistence
- [ ] 16.14.1. Add user control text such as `View as structured events` and `Use structured event layout`.
- [ ] 16.14.2. Allow override per window/tab/source for detected format interpretation.
- [ ] 16.14.3. Persist override preference and allow resetting to autodetect.
- [ ] 16.14.4. Confirm primary user-facing copy avoids `CLEF mode`.

### 16.15. Performance and Guardrails
- [ ] 16.15.1. Avoid repeated parse/projection work for already-seen events.
- [ ] 16.15.2. Keep deep/nested property expansion lazy and bounded.
- [ ] 16.15.3. Add guardrails for wide events and large exception payloads.
- [ ] 16.15.4. Maintain live-tail responsiveness under sustained append rate.

### 16.16. Error Handling and Fallback Behavior
- [ ] 16.16.1. Continue processing after malformed JSON lines with explicit error accounting.
- [ ] 16.16.2. Handle mixed CLEF/plain text files without stream abort.
- [ ] 16.16.3. Handle missing `@l`, `@m`, `@mt`, `@t` with deterministic UI fallback.
- [ ] 16.16.4. Handle unknown levels safely while preserving original field value.
- [ ] 16.16.5. Keep pretty-printed multi-line JSON out of default CLEF stream interpretation.

### 16.17. Tests (Fixture-Driven Coverage Checklist)
- [ ] 16.17.1. Detection confidence tests for strong/probable/generic classification.
- [ ] 16.17.2. Tests proving generic JSON is not falsely detected as Serilog compact JSON.
- [ ] 16.17.3. Serilog level mapping tests for expected values and unknown-level handling.
- [ ] 16.17.4. Timestamp extraction tests from `@t`.
- [ ] 16.17.5. Rendered message extraction tests from `@m`.
- [ ] 16.17.6. Message template preservation tests from `@mt`.
- [ ] 16.17.7. Template-rendering fallback tests where implemented.
- [ ] 16.17.8. Exception extraction/display tests from `@x`.
- [ ] 16.17.9. Event-id extraction tests from `@i`.
- [ ] 16.17.10. Trace/span extraction tests from `@tr`/`@sp`.
- [ ] 16.17.11. User-property preservation tests for top-level fields.
- [ ] 16.17.12. Nested object/array property-path tests.
- [ ] 16.17.13. Mixed CLEF/plain text file handling tests.
- [ ] 16.17.14. Malformed JSON line resilience tests.
- [ ] 16.17.15. Large-file and live-tail append behavior tests.
- [ ] 16.17.16. Override persistence tests.
- [ ] 16.17.17. Property-path filtering tests.
- [ ] 16.17.18. Dashboard grouping tests by CLEF fields.
- [ ] 16.17.19. Copy/export tests for raw event and selected values.
- [ ] 16.17.20. Run `./gradlew check` for affected modules before sprint closure.

### 16.18. Documentation Updates
- [ ] 16.18.1. Keep sprint and task docs aligned with final implementation scope and terminology.
- [ ] 16.18.2. Update user docs for structured event view behavior, detection indicator, and override control.
- [ ] 16.18.3. Update roadmap references impacted by Sprint 16 insertion and downstream renumbering.
- [ ] 16.18.4. Update `docs/project_memory.md` at sprint/task completion with shipped scope, decisions, gotchas, and test coverage.

### 16.19. Verification and Closure
- [ ] 16.19.1. Verify required references and numbering updates are consistent across docs.
- [ ] 16.19.2. Verify no primary user-facing `CLEF mode` copy in sprint/task artifacts.
- [ ] 16.19.3. Capture explicit deliverables summary:
  - New sprint doc content
  - New tasks doc content
  - Files requiring renumber updates
  - Old-to-new sprint renumber map
  - Assumptions
  - Product-owner confirmation items
- [ ] 16.19.4. Record final acceptance sign-off notes in project memory/recap workflow docs.

### 16.20. Acceptance Criteria
- [ ] Structured event view supports first-class compact-event rendering with `@t`, `@l`, `@m`/`@mt`, `@x`, `@i`, `@tr`, `@sp` handling.
- [ ] Hybrid detection/override workflow is documented and implementation-ready.
- [ ] Property-path filtering and dashboard grouping include key CLEF fields with high-cardinality safeguards.
- [ ] Fixture-driven tests cover detection, parsing, resilience, live-tail, and copy/export workflows.
- [ ] Roadmap/task references remain consistent after Sprint 16 insertion and downstream renumbering.

### 16.21. Verification Notes
- Reference search examples:
  - `rg -n "Sprint 1[4-7]|TASKS-SPRINT-1[4-7]" README.md docs`
  - `rg -n "CLEF mode" docs/sprints docs/tasks`
- Validation checklist:
  - Fixture coverage complete for required scenarios.
  - Relevant module tests and `./gradlew check` complete.
  - Documentation and roadmap references updated consistently.
