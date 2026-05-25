# TASKS: Sprint 9 - Analysis & Visualization

## 14. Sprint 9: Analysis & Visualization

### 14.1. Foundations & Architecture
- [x] 14.1.1. Create ADR for analysis architecture, aggregation boundaries, and data flow (`:core` -> `:ui`)
- [x] 14.1.2. Create ADR for Compose-native charting library selection and interaction model
- [x] 14.1.3. Define tiny types for analysis concepts (time bucket size, field key, frequency count, diff window)
- [x] 14.1.4. Define sealed error hierarchy for analysis/visualization failures and map to UI-safe messages
- [x] 14.1.5. Establish repository and service interfaces for metrics and ad-hoc analysis (mocking boundary)

### 14.2. Walking Skeleton: Dashboard Vertical Slice
- [x] 14.2.1. Add a dedicated `Dashboard` view entry point reachable from the primary UI flow
- [x] 14.2.2. Render a basic dashboard screen shell with loading, empty, error, and content states
- [x] 14.2.3. Wire a minimal end-to-end pipeline: selected `Log Window` -> metrics query -> single chart render
- [x] 14.2.4. Add click-through from dashboard data points to existing log filtering behavior
- [x] 14.2.5. Ensure dashboard state is isolated per tab/workspace and does not leak between tabs

### 14.3. Time-Series Frequency Metrics
- [ ] 14.3.1. Implement bucketed frequency aggregation for events per second and per minute
- [ ] 14.3.2. Support configurable time range presets (last N minutes, visible window, full loaded range)
- [ ] 14.3.3. Handle sparse timestamps and out-of-order entries without chart corruption
- [ ] 14.3.4. Add incremental recomputation path for appended log updates (`LogUpdate.Appended`)
- [ ] 14.3.5. Add reset handling path for source resets (`LogUpdate.Reset`) to keep charts consistent
- [ ] 14.3.6. Add date-time range controls (`From`/`To`) to filter analysis windows with explicit validation (`From <= To`)
- [ ] 14.3.7. Apply selected date-time range consistently across dashboard metrics, frequency analysis, and diff inputs
- [ ] 14.3.8. Ensure date-time range filtering respects timezone parsing rules used by active log templates

### 14.4. Log Level Distribution Metrics
- [ ] 14.4.1. Implement aggregation for normalized `Log Level` counts (DEBUG/INFO/WARN/ERROR/FATAL)
- [ ] 14.4.2. Preserve and expose "unknown/unmapped" level bucket where applicable
- [ ] 14.4.3. Implement distribution chart rendering with theme-aware level colors
- [ ] 14.4.4. Support interactive selection of level segments to apply/clear filters in the active log view

### 14.5. Ad-hoc Frequency Analysis
- [ ] 14.5.1. Add UI action to run "Frequency Analysis" on any selected structured field
- [ ] 14.5.2. Implement top-N value frequency aggregation with deterministic tie-breaking
- [ ] 14.5.3. Add controls for cardinality limits and minimum frequency thresholds
- [ ] 14.5.4. Handle missing/null field values explicitly (e.g., `(missing)` bucket)
- [ ] 14.5.5. Support exporting or copying ad-hoc frequency results for external use

### 14.6. Log Diffing Analysis
- [ ] 14.6.1. Define diff inputs (A/B time ranges or A/B log selections) and validation rules
- [ ] 14.6.2. Implement comparative metrics for level distribution deltas between A and B
- [ ] 14.6.3. Implement comparative metrics for top field-value frequency deltas between A and B
- [ ] 14.6.4. Add clear visual cues for increases/decreases and unchanged values
- [ ] 14.6.5. Add navigation/filter actions from diff rows back into corresponding source logs

### 14.7. Charting Engine Integration & Interactions
- [ ] 14.7.1. Integrate the selected Compose-native charting library into `:ui`
- [ ] 14.7.2. Standardize chart adapters for line, bar, and distribution visualizations
- [ ] 14.7.3. Implement interactive behaviors: zoom, pan, hover/tooltip, and click-to-filter
- [ ] 14.7.4. Ensure chart components follow app theme tokens and support dark/light palettes
- [ ] 14.7.5. Provide accessibility-friendly labels and keyboard navigation fallbacks where feasible
- [ ] 14.7.6. Implement brush/range-selection on the time-series chart to set the active `From`/`To` filter window
- [ ] 14.7.7. Synchronize chart-selected time ranges with the log view so only entries in the selected window are shown
- [ ] 14.7.8. Add clear/reset range interaction to return both chart and log view to the full loaded time window

### 14.8. Performance, Sampling, and Background Work
- [ ] 14.8.1. Implement sampling strategy for very large datasets with deterministic sampling mode
- [ ] 14.8.2. Execute heavy aggregations on `Dispatchers.Default` and keep UI thread free of blocking work
- [ ] 14.8.3. Add cancellation/debounce for rapid filter or range changes to avoid stale computations
- [ ] 14.8.4. Add instrumentation and logging around aggregation latency and sample size decisions
- [ ] 14.8.5. Define and enforce performance budgets for dashboard refresh and interaction responsiveness

### 14.9. UI/UX Polish and Workspace Behavior
- [ ] 14.9.1. Align dashboard and analysis panels with existing Compose desktop layout patterns
- [ ] 14.9.2. Persist dashboard panel preferences and selected analysis settings in workspace state
- [ ] 14.9.3. Ensure analysis views gracefully handle source disconnections and file removal scenarios
- [ ] 14.9.4. Add concise user guidance text/tooltips for frequency analysis and diff interpretation

### 14.10. Verification & Testing
- [ ] 14.10.1. Unit tests for time bucketing and frequency aggregation edge cases
- [ ] 14.10.2. Unit tests for level distribution aggregation and unknown-level handling
- [ ] 14.10.3. Unit tests for ad-hoc field frequency aggregation (high cardinality, null/missing values)
- [ ] 14.10.4. Unit tests for diff computation correctness and stable ordering
- [ ] 14.10.5. UI tests for dashboard states (loading/error/empty/content)
- [ ] 14.10.6. UI tests for interactive chart actions (zoom/pan/click-to-filter)
- [ ] 14.10.7. Integration tests for incremental updates from `LogUpdate.Initial`, `Appended`, and `Reset`
- [ ] 14.10.8. Performance tests validating sampling behavior and response-time budgets on large inputs
- [ ] 14.10.9. Unit tests for date-time range filtering boundaries (inclusive/exclusive edges, invalid ranges)
- [ ] 14.10.10. UI tests for `From`/`To` date-time controls and validation messaging
- [ ] 14.10.11. Integration tests for chart range-selection -> log view filtering synchronization

### 14.11. Documentation & Rollout Readiness
- [ ] 14.11.1. Update sprint documentation with final architecture and accepted ADR links
- [ ] 14.11.2. Update user-facing docs with dashboard/frequency/diff usage flows
- [ ] 14.11.3. Add release-note draft entries for analysis and visualization capabilities
- [ ] 14.11.4. Record sprint outcomes, key decisions, gotchas, and test coverage in `docs/project_memory.md`
