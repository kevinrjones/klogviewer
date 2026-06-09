# TASKS: Sprint 12E - Structured Data Performance, Dashboard, and Polish

## 12E. Structured Data Performance, Dashboard, and Polish

### 12E.0. Goal
Scale structured logging support for larger datasets and live streams, integrate structured dimensions into dashboard analysis, and finalize rollout polish/documentation for Sprint 12.

### 12E.1. In Scope
- Lazy parsing and caching strategy for structured payload expansion.
- Guardrails for payload size, nesting depth, discovered fields, and auto-columns.
- High-cardinality handling for IDs/URLs/user keys.
- Dashboard/frequency analysis integration for structured fields and canonical aliases.
- Column selection workflow for discovered structured paths.
- Optional user-configurable field mappings where needed for safe extensibility.
- Performance and memory regression tests on large/mixed datasets.
- Final documentation, release notes updates, and sprint closure checks.

### 12E.2. Out of Scope
- New ecosystem compatibility initiatives beyond fixes found during this slice’s validation.
- Major parser rewrites unless required by measured performance bottlenecks.
- New primary UI features unrelated to structured scaling/polish.

### 12E.3. Dependencies
- Required: `TASKS-SPRINT-12A-STRUCTURED-DATA-FOUNDATION.md`.
- Required: `TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md`.
- Required: `TASKS-SPRINT-12C-STRUCTURED-DATA-INSPECTOR.md`.
- Required: `TASKS-SPRINT-12D-STRUCTURED-DATA-ECOSYSTEM-COMPATIBILITY.md`.

### 12E.4. Scope-to-Workstream Mapping
- Lazy parse/cache and limits (`5`, `6`) -> parser projection cache and index lifecycle management.
- Dashboard + columns (`7`, `8`) -> ViewModel analytics flow and field discovery/selection UX.
- Configurability + polish docs (`9`) -> settings/preferences and documentation updates.
- Performance verification (`10`) -> synthetic + fixture-based perf/memory/regression suites.
- Release readiness (`11`) -> sprint docs, recap/project-memory, release notes.

### 12E.5. Lazy Parsing and Caching Strategy
- [x] 12E.5.1. Define cache boundaries for typed tree/path index materialization per log entry.
- [x] 12E.5.2. Implement bounded cache eviction policy (for example LRU) with deterministic behavior.
- [x] 12E.5.3. Ensure list rendering uses minimal canonical projection and defers heavy expansion.
- [x] 12E.5.4. Prevent cache churn from causing repeated reparsing during normal selection/filter workflows.

### 12E.6. Limits and High-Cardinality Controls
- [x] 12E.6.1. Implement configurable limits for payload size, nesting depth, and indexed array breadth.
- [x] 12E.6.2. Implement discovered-field and auto-column limits to prevent UI overload.
- [x] 12E.6.3. Add high-cardinality safeguards for dashboard top-N aggregation and `(other)` bucketing where applicable.
- [x] 12E.6.4. Keep direct filtering by high-cardinality fields available even when not auto-promoted to columns.

### 12E.7. Dashboard and Frequency Integration
- [x] 12E.7.1. Extend dashboard dimension discovery to include canonical and selected structured paths.
- [x] 12E.7.2. Ensure frequency analysis works for structured values with `(missing)` semantics preserved.
- [x] 12E.7.3. Validate mixed structured/plain datasets still produce stable dashboard summaries.
- [x] 12E.7.4. Add dashboard-level regression checks for compatibility with prior analysis behavior.

### 12E.8. Column Selection and Discoverability
- [x] 12E.8.1. Add/extend column selection for discovered structured fields with sensible defaults.
- [x] 12E.8.2. Ensure canonical columns remain stable while allowing opt-in ecosystem-specific columns.
- [x] 12E.8.3. Preserve user column preferences across sessions where preference persistence already exists.
- [x] 12E.8.4. Validate column behavior in mixed data files and parser fallback scenarios.

### 12E.9. Settings, Mapping Extensions, and Final Polish
- [x] 12E.9.1. Evaluate need for user-configurable alias/mapping extensions and implement if low-risk.
- [x] 12E.9.2. Document default mapping behavior vs user overrides.
- [x] 12E.9.3. Align final UX copy/tooltips/help text for structured filtering, inspector, and dashboard usage.
- [x] 12E.9.4. Capture deferred follow-ups for post-Sprint 12 roadmap.

### 12E.10. Verification & Quality Gates
- [x] 12E.10.1. Add/extend performance tests using large JSON/mixed-format datasets and live-tail-like ingestion patterns.
- [x] 12E.10.2. Add/extend memory/regression tests for cache limits, deep nesting, and high-cardinality fields.
- [x] 12E.10.3. Add/extend integration tests for dashboard grouping on structured fields.
- [x] 12E.10.4. Add/extend UI tests for discovered-column selection and persistence behavior.
- [x] 12E.10.5. Re-run regression checks for plain-text/template workflows across list/filter/dashboard/details.
- [x] 12E.10.6. Run static analysis with `./gradlew detekt`.
- [x] 12E.10.7. Run relevant module tests for touched modules.
- [x] 12E.10.8. Run broader `./gradlew check` before closing 12E.

### 12E.11. Documentation, Release Notes, and Closure
- [x] 12E.11.1. Update sprint epic with completion notes across slices 12A-12E.
- [x] 12E.11.2. Update user docs for structured filtering, inspector usage, supported ecosystems, and performance limits.
- [x] 12E.11.3. Update release notes with structured-data capabilities and known limitations.
- [x] 12E.11.4. Update `docs/project_memory.md` with shipped scope, key decisions, gotchas, and test coverage areas.

### 12E.12. Acceptance Criteria
- [x] Large structured files remain usable without UI freezes under documented limits.
- [x] Dashboard can analyze and group by selected structured fields while preserving prior behavior for plain logs.
- [x] Users can choose discovered structured columns and keep stable defaults for canonical columns.
- [x] High-cardinality fields are filterable without overwhelming dashboard auto-discovery.
- [x] Performance/memory guardrails are documented, tested, and enforced.
- [x] Sprint 12 documentation and release notes clearly describe shipped behavior and known limits.

### 12E.13. Verification Notes

- `./gradlew :domain:test --tests com.klogviewer.domain.model.LogEntryTest :ui:test --tests com.klogviewer.ui.viewmodel.LogLoadingCoordinatorColumnMergeTest` was executed; result: passed.
- `./gradlew :ui:test --tests com.klogviewer.ui.test.KLogViewerUiTest.givenFileSelected_whenLoaded_thenLogsAreDisplayed` was executed; result: failed before alias-deduping fix, then passed after fix.
- `./gradlew detekt` was executed; result: passed.
- `./gradlew check` was executed; result: passed.
- Any known blockers:
    - None.
- Sprint 12E-specific validation completed:
    - Verified canonical-column stability with discovered-column cap (`8`) and dashboard discovered-field cap (`200`) while preserving high-cardinality filtering workflows.
    - Verified mixed parser-fallback column behavior and persisted column preference merge behavior via `LogLoadingCoordinatorColumnMergeTest`.
    - Verified per-entry compatibility projection memoization and explicit raw-field precedence via `LogEntryTest`.
