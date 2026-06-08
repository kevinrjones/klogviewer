# TASKS: Sprint 12C - Structured Entry Inspector UI

## 12C. Structured Entry Inspector UI

### 12C.0. Goal
Deliver an interactive structured-entry inspection experience in the detail pane so users can explore nested payloads, copy field metadata, and create filters directly from inspected fields.

### 12C.1. In Scope
- Structured detail-pane view for typed object/array/scalar payloads.
- Expand/collapse interaction for nested structures.
- Raw payload view toggle (`Structured` / `Raw`).
- Field-level actions: copy path, copy value, filter-by-field, filter-by-value.
- Log-row visual indicator when structured payload data is present.
- UI guardrails for large payloads (truncation/virtualization/progressive rendering).
- UI tests for selection, expansion, copy actions, and filter creation handoff.
- Documentation updates for inspector usage.

### 12C.2. Out of Scope
- New parser ecosystem compatibility work (12D).
- Major filtering grammar changes beyond integration with existing 12B predicates.
- Dashboard/frequency structured grouping changes (12E).
- Deep parser/runtime performance work beyond immediate UI responsiveness safeguards (12E).

### 12C.3. Dependencies
- Required: `TASKS-SPRINT-12A-STRUCTURED-DATA-FOUNDATION.md`.
- Recommended: `TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md` for end-to-end “Filter by this value” workflows.

### 12C.4. Scope-to-Workstream Mapping
- Structured detail rendering (`5`, `6`) -> `ui/components/LogEntryDetails.kt` and related UI state.
- Row marker + list integration (`7`) -> `ui/components/LogList.kt` and row metadata wiring.
- Filter handoff (`8`) -> detail actions to filter intent/query update flow.
- Validation and regressions (`9`) -> Compose/UI tests + interaction tests + regression checks.
- Documentation and discoverability (`10`) -> user docs + sprint progress notes.

### 12C.5. Structured Detail Pane Rendering
- [x] 12C.5.1. Add a structured inspector panel that renders object/array/scalar nodes from typed structured payloads.
- [x] 12C.5.2. Support expandable/collapsible nodes with deterministic ordering for stable navigation.
- [x] 12C.5.3. Display type cues (for example string/number/bool/null/object/array) without cluttering readability.
- [x] 12C.5.4. Keep existing plain content presentation intact for entries without structured data.

### 12C.6. Raw Payload View and Large-Entry Guardrails
- [x] 12C.6.1. Implement `Structured` vs `Raw` toggle in details pane.
- [x] 12C.6.2. Ensure raw mode can display JSON/plain payload text safely with truncation controls.
- [x] 12C.6.3. Add UI safeguards for large payloads (deferred node rendering and/or progressive expansion).
- [x] 12C.6.4. Prevent full-reparse side effects when switching nodes or toggles within the same selected entry.

### 12C.7. Log List Visual Indicators
- [x] 12C.7.1. Add a visible marker in log rows indicating structured payload availability.
- [x] 12C.7.2. Keep marker behavior stable for mixed structured/plain files and parser fallback scenarios.
- [x] 12C.7.3. Preserve existing severity styling and row readability when marker is shown.

### 12C.8. Field Actions and Filter Creation Handoff
- [x] 12C.8.1. Add `Copy field path` action on structured nodes.
- [x] 12C.8.2. Add `Copy value` action on selected node values.
- [x] 12C.8.3. Add `Filter by this field` action that creates an existence/equality predicate.
- [x] 12C.8.4. Add `Filter by this value` action that emits a correctly quoted/escaped predicate.
- [x] 12C.8.5. Ensure generated queries are compatible with 12B grammar and remain editable by users.

### 12C.9. Verification & Quality Gates
- [x] 12C.9.1. Add/extend UI tests for row selection -> detail pane structured rendering.
- [x] 12C.9.2. Add/extend UI tests for node expansion/collapse behavior and stable path rendering.
- [x] 12C.9.3. Add/extend UI tests for raw toggle behavior and payload truncation safeguards.
- [x] 12C.9.4. Add/extend clipboard tests for copy path and copy value actions.
- [x] 12C.9.5. Add/extend integration tests for filter creation actions to query bar semantics.
- [x] 12C.9.6. Add regression checks for non-structured detail rendering and existing list interactions.
- [x] 12C.9.7. Run static analysis with `./gradlew detekt`.
- [x] 12C.9.8. Run relevant module tests for touched modules.
- [x] 12C.9.9. Run broader `./gradlew check` before closing 12C when feasible.

### 12C.10. Documentation and Adoption Notes
- [ ] 12C.10.1. Add user docs showing structured inspector usage, raw toggle behavior, and field actions.
- [x] 12C.10.2. Update sprint epic progress markers for inspector/UI slice completion.
- [x] 12C.10.3. Document known UX limitations deferred to 12E performance/polish work.

Notes:
- `./gradlew detekt` and `./gradlew check` were executed; failures are currently dominated by pre-existing `ui` module static-analysis issues outside this 12C slice.
- Bounded discovered-field column integration remains out of scope for this 12C slice and is left for follow-on work.

### 12C.11. Acceptance Criteria
- [x] Selecting a structured log entry shows an expandable structured tree in the detail pane.
- [x] Users can switch between structured and raw views without breaking selection state.
- [x] Users can copy structured field paths and values from the detail pane.
- [x] Users can create valid filter queries from selected fields/values via inspector actions.
- [x] Log rows with structured payloads show a clear visual indicator.
- [x] Large structured payloads remain inspectable without freezing normal UI interactions.
