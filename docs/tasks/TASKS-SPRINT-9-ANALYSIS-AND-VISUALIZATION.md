# TASKS: Sprint 9 - Analysis & Visualization (Restart)

## 14. Sprint 9 Restart: Graphing, Analysis, and Date-Time Controls

### 14.0. Competitive Baseline (Derived from TechDator Log Viewer List)

Observed strengths to cover in our sprint scope:

- Large-file responsiveness and fast navigation (`Legit`, `Universal Viewer`, `File Viewer Lite`, `Free File Viewer`)
- Powerful search/filter/highlight/hide workflows (`Dynamic`, `GamutLogViewer`, `SolarWinds`, `Sematext`)
- Real-time update visibility (`Glogg`, `SolarWinds`, `Sematext`)
- Severity-aware analysis and visual indicators (`SolarWinds` color-coded severities)
- Export/share workflows for downstream analysis (`LogViewer` export, centralized analysis tools)

### 14.1. Sprint Restart Foundations
- [x] 14.1.1. Mark Sprint 9 as restarted and supersede prior Sprint 9 checklist scope
- [x] 14.1.2. Create new ADR for Sprint 9 restart scope (graphing + analysis + date-time controls)
- [x] 14.1.3. Reconfirm tiny types and sealed failures for analysis workflows (time range, bucket, field key, compare window)
- [x] 14.1.4. Define performance budgets for analysis and charts (first paint, interaction latency, refresh time)

### 14.2. Charting Library Selection (Performance First, No Custom Engine)
K- [x] 14.2.1. Benchmark Compose-capable chart libraries on Desktop with representative datasets (small/medium/large)
  - Dataset profile used for sprint selection pass: `small` (10k events / 600 buckets), `medium` (100k events / 6k buckets), `large` (1M events / 60k buckets)
  - Measured against ADR-039 budgets: first paint `<= 800ms`, interaction latency p95 `<= 100ms`, refresh latency p95 `<= 250ms`
- [x] 14.2.2. Score candidates against required interactions: zoom, pan, tooltip, click selection, brush/range select
  - Scorecard dimensions: interaction coverage, performance-budget fit, Compose Desktop fit, maintenance health, licensing fit
- [x] 14.2.3. Prefer a Compose-native high-performance library (initial target: `KoalaPlot`) if it satisfies budgets
- [x] 14.2.4. Define fallback path (secondary library) if interaction coverage or performance budgets are not met
  - Fallback selected: `Vico` (Compose Multiplatform) with adapter seam retained in `:ui` for swappable chart backend
- [x] 14.2.5. Record final library decision and rationale in ADR, including maintenance/licensing notes
  - Decision and evidence captured in `docs/adr/adr-040-charting-library-selection-and-benchmark.md`

### 14.3. Dashboard Walking Skeleton (New Start)
- [x] 14.3.1. Reintroduce dashboard entry point in primary UI flow with tab/workspace isolation
  - Added per-window `Logs`/`Dashboard` mode toggle and preserved tab/window-local workspace state
- [x] 14.3.2. Implement dashboard shell states: loading, empty, error, content
  - Implemented `DashboardDataState` rendering path in `KLogViewerScreen`
- [x] 14.3.3. Wire selected `Log Window` -> analysis query -> chart model -> chart render
  - Wired filtered-window data through `AnalysisMetricsRepository` time-series queries into dashboard models
- [x] 14.3.4. Add click-through from chart selection to active log filtering
  - Added chart-to-filter actions for time bucket and level selections, plus explicit clear selections intent

### 14.4. Time-Series and Level Analysis
- [x] 14.4.1. Implement bucketed event frequency (per second/per minute) with sparse and out-of-order safety
  - Added dashboard bucket-size switching (`PER_SECOND` / `PER_MINUTE`) and mapped to domain `TimeBucketSize`
- [x] 14.4.2. Implement normalized log-level distribution (`DEBUG/INFO/WARN/ERROR/FATAL/UNKNOWN`)
  - Added normalized distribution model for all `LogLevel` values with count and ratio
- [x] 14.4.3. Support live updates (`LogUpdate.Appended`) and reset consistency (`LogUpdate.Reset`)
  - Recompute dashboard analysis in shared `filterLogs` path used by load/append/reset update flows
- [x] 14.4.4. Add chart-level filtering interactions (series/segment select to apply or clear filters)
  - Added dashboard intents and ViewModel handlers for selecting/clearing level and time bucket interactions

### 14.5. Date-Time Controls and Range Synchronization
- [x] 14.5.1. Add explicit `From`/`To` date-time controls with validation (`From <= To`)
- [x] 14.5.2. Add range presets (`Last N minutes`, `Visible Window`, `Full Loaded Range`, `Custom`)
- [x] 14.5.3. Ensure analysis time filtering respects parser/template timezone rules
- [x] 14.5.4. Implement chart brush/range selection to set active `From`/`To`
- [x] 14.5.5. Synchronize selected range across dashboard metrics, ad-hoc analysis, diff inputs, and log view
- [x] 14.5.6. Add clear/reset range action to return all views to full loaded window

### 14.6. Ad-hoc Frequency and Comparative Analysis
- [x] 14.6.1. Add "Frequency Analysis" action for selected structured fields
- [x] 14.6.2. Implement top-N value frequency with deterministic tie-breaking
- [x] 14.6.3. Add thresholds and cardinality controls
- [x] 14.6.4. Handle null/missing values explicitly (`(missing)` bucket)
- [x] 14.6.5. Implement A/B compare inputs (time-range based) and delta metrics for levels + top field values
- [x] 14.6.6. Add increase/decrease/unchanged visual cues and click-back-to-source actions

### 14.7. UX, Accessibility, and Export Workflows
- [x] 14.7.1. Align chart styling with app theme tokens (dark/light support)
  - Added dashboard chart strip + active filter chips using `MaterialTheme` color tokens so visuals follow light/dark theme changes.
- [x] 14.7.2. Provide tooltip text and accessibility-friendly labels for key chart points
  - Added hover tooltip text for chart buckets and semantic chart content descriptions for assistive tooling.
- [x] 14.7.3. Add keyboard-accessible fallbacks for key interactions where feasible
  - Added keyboard-friendly bucket navigation controls (`First`, `Previous`, `Next`) alongside pointer chart interactions.
- [ ] 14.7.4. Support copying/exporting analysis outputs (CSV/JSON and clipboard summary)

### 14.8. Performance and Background Execution
- [ ] 14.8.1. Run heavy aggregations on `Dispatchers.Default`; keep UI thread free of blocking work
- [ ] 14.8.2. Add cancellation/debounce for rapid filter/range changes to prevent stale results
- [ ] 14.8.3. Add deterministic sampling mode for very large datasets
- [ ] 14.8.4. Add instrumentation logs for aggregation latency, render latency, and sampling decisions
- [ ] 14.8.5. Validate charts remain responsive under high-volume append scenarios

### 14.9. Verification & Testing
- [ ] 14.9.1. Unit tests for time bucketing and sparse/out-of-order timestamp handling
- [ ] 14.9.2. Unit tests for level distribution with unknown-level mapping
- [ ] 14.9.3. Unit tests for ad-hoc frequency (high cardinality, null/missing, deterministic ordering)
- [ ] 14.9.4. Unit tests for A/B delta computation correctness and ordering
- [ ] 14.9.5. UI tests for dashboard states and chart interactions (zoom/pan/tooltip/click/brush)
- [ ] 14.9.6. UI tests for `From`/`To` controls, presets, and validation messaging
- [ ] 14.9.7. Integration tests for `LogUpdate.Initial`, `Appended`, `Reset` analysis consistency
- [ ] 14.9.8. Performance tests validating budgets on large datasets and live append streams

### 14.10. Documentation & Rollout Readiness
- [ ] 14.10.1. Update Sprint 9 documentation with accepted ADR links and final architecture diagram
- [ ] 14.10.2. Update user docs with graphing, analysis, and date-time control usage flows
- [ ] 14.10.3. Add release-note draft entries for analysis and visualization capabilities
- [ ] 14.10.4. Record sprint completion outcomes in `docs/project_memory.md`
