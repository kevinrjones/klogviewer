---
sessionId: session-260527-165619-tpe4
---

# Requirements

### Overview & Goals
Deliver a reliable dashboard **events-over-time** bar chart that helps users spot frequency spikes while preserving the existing level-distribution behavior.

### Scope
#### In Scope
- Render event counts per time bucket (`PER_SECOND`, `PER_MINUTE`) in chronological order.
- Keep using the existing charting stack (`KoalaPlot`) and existing dashboard data contracts.
- Ensure x/y axis formatting matches the requested behavior.
- Ensure hover tooltip shows bucket start, bucket end, and event count.
- Ensure bucket click applies the existing dashboard time filter and supports clear-selection.
- Add/update automated tests for ordering and click-to-filter behavior.

#### Out of Scope
- Switching chart libraries or introducing a second chart engine.
- Reworking level-distribution logic/visualization.
- Introducing synthetic zero-count buckets if upstream data does not already provide them.

### Functional Requirements
1. Time-series bars are shown in ascending bucket timestamp order.
2. Y-values equal the exact number of log events in each bucket.
3. X-axis labels are bucket-start times:
   - `HH:mm:ss` for second buckets
   - `HH:mm` for minute buckets
4. Y-axis starts at `0` and displays integer counts only.
5. Hover tooltip shows bucket start, bucket end, and count.
6. Clicking a bucket applies the existing dashboard time filter for that bucket.
7. A clear-selection action removes chart-applied filtering.
8. Level distribution behavior remains unchanged.

# Technical Design

### Current Implementation
- `ui/src/main/kotlin/com/klogviewer/ui/components/KoalaPlotCharts.kt`
  - `KoalaPlotTimeSeriesChart(...)` already uses `KoalaPlot` `XYGraph` + `VerticalBarPlot`, includes tooltip + click callback.
- `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
  - `DashboardContent(...)` and `LogTimeFrequencyPanel(...)` both render `KoalaPlotTimeSeriesChart` and wire bucket clicks to dashboard intents.
  - Clear-selection UX already exists via `Clear selections` and removable active filter chips.
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/KLogViewerViewModel.kt`
  - `buildDashboardDataState(...)` maps domain time-series buckets into `DashboardTimeBucket`.
  - Bucket-size mapping uses `DashboardBucketSize.toDomainBucketSize()`.
  - Selection/filter behavior is handled via `SelectDashboardTimeBucket` / `SelectDashboardTimeRange` and `clearDashboardSelections(...)`.
- `core/src/main/kotlin/com/klogviewer/core/analysis/InMemoryAnalysisMetricsRepository.kt`
  - `timeSeriesMetrics(...)` groups by bucket start and emits sorted buckets using `toSortedMap()`.
  - Current contract emits only buckets present in data (no guaranteed zero-fill gaps).

### Key Decisions
- Keep `KoalaPlot` and strengthen behavior inside existing chart/viewmodel flow instead of introducing any new chart abstraction.
- Preserve current bucket data model semantics (no new zero-bucket synthesis).
- Keep filter application in the existing dashboard intent path, ensuring click-to-apply and clear-selection remain deterministic.
- Avoid changes to `KoalaPlotLevelDistributionChart(...)` and level-distribution computation paths.

### Proposed Changes
- `KoalaPlotTimeSeriesChart(...)` (`KoalaPlotCharts.kt`)
  - Verify/adjust bar data preparation to always consume chronologically ordered buckets from state.
  - Ensure axis formatting strictly follows selected bucket size (`HH:mm:ss` vs `HH:mm`).
  - Keep y-axis zero-based with integer label rendering.
  - Normalize tooltip copy to explicit bucket start/end/count fields.
- Dashboard wiring (`KLogViewerScreen.kt`)
  - Ensure bucket click path uses the existing time-bucket filter behavior expected by dashboard state.
  - Keep and validate explicit clear-selection action in dashboard controls.
- Dashboard state shaping (`KLogViewerViewModel.kt`, if needed)
  - Harden time-series ordering at the state-construction seam so UI rendering is deterministic regardless of upstream ordering assumptions.
  - Keep selected bucket synchronization with active time filter intact.

### File Structure
- **Modify** `ui/src/main/kotlin/com/klogviewer/ui/components/KoalaPlotCharts.kt`
- **Modify** `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
- **Modify (if needed)** `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/KLogViewerViewModel.kt`
- **Modify tests** `ui/src/test/kotlin/com/klogviewer/ui/viewmodel/DashboardIntentTest.kt`

# Testing

### Validation Approach
- Use existing ViewModel-level dashboard tests as the primary verification seam for ordering + filter interactions.
- Keep assertions deterministic around bucket ordering and bucket-driven time-filter application/clearing.

### Key Scenarios
- Dashboard content builds time-series buckets in ascending `from` order.
- Selecting a bucket applies matching `timeFilterFromInstant` / `timeFilterToInstant`.
- Clearing selection removes chart-applied filter state.
- Bucket-size switch preserves expected time-label format behavior via chart input/state.

### Edge Cases
- Sparse/out-of-order timestamps still render chronologically.
- Empty/single-bucket datasets keep valid axis behavior.
- Existing level-distribution scenarios remain unaffected.

### Test Changes
- Update/add tests in `DashboardIntentTest` for:
  - chronological bucket ordering assertions,
  - click-to-filter application,
  - clear-selection behavior after bucket interaction,
  - non-regression on level-distribution state where relevant.

# Delivery Steps

### ✓ Step 1: Align dashboard time-series state and ordering
Dashboard state provides a deterministically ordered bucket list for chart rendering.

- Update `buildDashboardDataState(...)` path in `KLogViewerViewModel` (if needed) to enforce ascending bucket order before assigning `DashboardDataState.Content.timeSeries`.
- Keep bucket-size mapping (`PER_SECOND`/`PER_MINUTE`) unchanged via `DashboardBucketSize.toDomainBucketSize()`.
- Preserve existing no-zero-fill behavior by consuming repository output as-is (no synthetic empty buckets).

### ✓ Step 2: Refine bar chart rendering, axes, and tooltip
The time-series chart renders reliable event-count bars with required axis and tooltip formatting.

- Update `KoalaPlotTimeSeriesChart(...)` in `KoalaPlotCharts.kt` to ensure bar data and label formatting follow bucket size and chronological order.
- Ensure y-axis remains zero-based and labels remain integer-only.
- Adjust tooltip content to show explicit bucket start, bucket end, and event count while keeping theme-based styling.

### ✓ Step 3: Finalize bucket click and clear-selection behavior in dashboard flow
Clicking a chart bucket applies the existing time filter and users can clear that chart-driven selection.

- Verify/update bucket click intent wiring in `KLogViewerScreen.kt` (`DashboardContent` and `LogTimeFrequencyPanel`) to use the established dashboard time-filter path.
- Preserve current clear-selection UX (`Clear selections` control and active-filter clearing) and ensure it resets chart-driven bucket selection.
- Confirm this change does not alter level-distribution interaction logic.

### ✓ Step 4: Update dashboard tests for ordering and click-to-filter
Automated tests cover chronological bucketing and bucket interaction behavior expected by the dashboard.

- Extend/update `DashboardIntentTest.kt` with assertions for sorted bucket order in dashboard content.
- Add/adjust bucket interaction tests to verify filter apply + clear-selection flow.
- Keep existing level-distribution assertions in place as non-regression coverage.

### ✓ Step 5: Explain pointer-to-bar index mapping for drag selection
Document the coordinate-mapping approach before code changes.

- Explain how pointer X positions over the chart plot area map to discrete bar indexes.
- Define clamping/rounding behavior at chart bounds and sparse tick positions.

### ✓ Step 6: Implement drag-to-select range on KoalaPlot time-series bars
Enable drag gestures across the existing chart to select a contiguous bucket range.

- Keep `KoalaPlot` and existing `DashboardTimeBucket` model unchanged.
- Reuse existing dashboard time-range selection callback path.
- Preserve single-bucket click behavior and existing tooltip/axis behavior.

### ✓ Step 7: Add or update tests for range-index mapping and dashboard range apply
Provide deterministic coverage for the new drag range interaction behavior.

- Add unit-level assertions for pointer-coordinate to index mapping edge cases.
- Verify drag-selected range resolves to expected bucket `from`/`to` and applies via existing intent path.

### ✓ Step 8: Inspect current selection feedback and restate implementation plan
Confirm current chart/filter feedback gaps and restate a concrete plan before code edits.

- Inspect `KoalaPlotTimeSeriesChart(...)` selection rendering and hover behavior.
- Inspect dashboard + compact panel active-filter chip rendering for time selection visibility.
- Restate the implementation plan and ask clarifying questions only if requirements are ambiguous.

### ✓ Step 9: Implement chart visual feedback for single and range selection
Make chart selection state obvious for both single-bucket and drag-range selections.

- Add explicit selected/unselected/range bar styling using theme colors and non-color cues.
- Add optional subtle range overlay/background where practical without changing chart library/model.
- Preserve tooltip behavior and existing click/drag interactions.

### ✓ Step 10: Add interaction/accessibility feedback and active time-filter chips
Expose clear selection state in hover semantics and chip UI for dashboard + compact panel.

- Add hover interactivity cues where Compose Desktop supports pointer cursor feedback.
- Add/adjust semantics/content descriptions for unselected, selected, and range-item bars.
- Update active-filter chips to show selected time bucket/range with remove action in both views.

### ✓ Step 11: Add or update tests for selection-feedback state mapping
Cover selection-visualization state and active-filter label behavior.

- Add focused tests for time-selection mapping into chart/chip UI state.
- Add tests for range-label rendering and clear-selection removal behavior.
- Keep existing click-to-filter and drag-to-range behavior assertions passing.

### ✓ Step 12: Run targeted verification and finalize task notes
Validate the full change set and record completion notes.

- Run relevant `:ui:test` targets covering chart mapping and dashboard intent flows.
- Ensure no regressions in existing dashboard interaction tests.
- Record task completion in `docs/project_memory.md`.

### ✓ Step 13: Add live drag-selection visual feedback in chart
Ensure bar highlighting appears while the pointer is actively dragging, before filter state is committed.

- Compute a transient drag-selected index range from `dragStartX` + current pointer X using existing pointer-to-index mapping.
- Merge transient drag range with committed selection range for bar fill/border/top-marker + overlay rendering.
- Preserve tooltip, click, and range-commit behavior after drag end.

### ✓ Step 14: Extend tests for in-progress drag range mapping and visual selection state
Cover the transient selection behavior independently of committed dashboard filter state.

- Add/adjust focused tests around transient drag-range precedence and fallback to committed selection.
- Keep existing pointer mapping and range normalization assertions passing.

### ✓ Step 15: Run targeted verification and record completion notes
Validate the follow-up fix and capture sprint memory update.

- Run relevant `:ui:test` targets for chart mapping + dashboard intent support.
- Update `docs/project_memory.md` with this follow-up completion note.

### ✓ Step 16: Restrict bar highlight styling to active drag interaction
Ensure selected-color/border feedback is transient and only visible during pointer drag.

- Update chart selection-range resolution to use in-progress drag coordinates only for visual state.
- Preserve click/drag filter commit behavior while removing post-refresh committed highlight styling.

### ✓ Step 17: Update focused tests for transient-only highlight behavior
Keep deterministic mapping coverage aligned with transient drag preview requirements.

- Replace committed-range fallback assertions with transient-only expectations.
- Keep existing pointer mapping and range normalization assertions passing.

### ✓ Step 18: Run targeted verification and record completion notes
Validate the follow-up tweak and capture memory update.

- Run relevant `:ui:test` targets for chart pointer mapping + dashboard intent support.
- Update `docs/project_memory.md` with this follow-up completion note.