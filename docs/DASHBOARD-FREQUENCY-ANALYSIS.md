# Dashboard Frequency Analysis

This document describes exactly how dashboard **frequency analysis** works in the current implementation.

## Scope and data source

- Frequency analysis is rendered in the dashboard sidebar (`ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`).
- It is computed from the window's current `filteredLogs` (`buildDashboardDataState(...)` in `KLogViewerViewModel`).
- Core frequency computation is delegated to `AnalysisMetricsRepository.frequencyAnalysis(...)` using `FieldFrequencyQuery` (`core/src/main/kotlin/com/klogviewer/core/analysis/InMemoryAnalysisMetricsRepository.kt`).

## Field and control model

Dashboard frequency state (in `DashboardDataState.Content`) includes:

- `availableFrequencyFields`
- `selectedFrequencyField`
- `frequencyTopN`
- `frequencyThreshold`
- `frequencyCardinalityLimit`
- `frequencyItems`
- `selectedFrequencyValue`

### Available fields

- Built from the keys present in `filteredLogs` metadata fields.
- Distinct + sorted ascending.
- If prior selection is no longer present, selection falls back to first available field (or `null` if none).

### Default and constrained values

During dashboard recomputation:

- `frequencyTopN` default = `10`, coerced to `>= 1`
- `frequencyThreshold` default = `1`, coerced to `>= 1`
- `frequencyCardinalityLimit` default = `100`, coerced to `>= 1`

Intent handlers also coerce user edits to `>= 1` before storing state.

## Exact computation pipeline

Implemented in `computeFrequencyItems(...)`.

1. If no `selectedFrequencyField`, return empty list.
2. Validate selected field via `AnalysisFieldKey.from(field)`.
   - blank/invalid field => empty list.
3. Query repository with:
   - `entries = filteredLogs`
   - `fieldKey = selected field`
   - `limit = frequencyCardinalityLimit`
   - `window = DiffWindow.Unbounded`
4. Repository behavior (`InMemoryAnalysisMetricsRepository`):
   - for unbounded window, includes all entries (timestamp not required)
   - groups by field value
   - missing field values are grouped as `(missing)`
   - sorts by count descending, then value ascending
   - applies `limit`
5. ViewModel post-processing:
   - sorts again by count desc, then value asc
   - filters items where `count >= frequencyThreshold`
   - applies `take(frequencyTopN)`
   - maps to `DashboardFieldFrequencyItem(value, count)`

Final list size is effectively bounded by both cardinality and top-N after threshold filtering.

## Interaction behavior (selection and filtering)

### Selecting a frequency value

Clicking a frequency row calls `SelectFrequencyValue`:

- If clicked value is not currently selected:
  - set it as `selectedFrequencyValue`
  - remove any existing dashboard field queries
  - add new query token: `@field:<selectedField>=<selectedValue>`
- If clicked value is already selected:
  - clear `selectedFrequencyValue`
  - remove dashboard field queries

After either action, logs are re-filtered.

### How the generated query matches logs

`LogFilterService.matchesQuery(...)` treats `@field:` tokens as structured field filters:

- parses key/value from `@field:key=value`
- resolves missing entry values as `(missing)`
- applies `contains(value, ignoreCase = true)` on the field value

This is substring matching (case-insensitive), not exact equality.

### Changing selected frequency field

`UpdateFrequencyField` behavior:

- stores new `selectedFrequencyField`
- clears `selectedFrequencyValue`
- clears A/B `fieldDeltas`
- removes all existing `@field:` queries

Then logs are re-filtered and dashboard data is rebuilt.

## Coupling with A/B comparison

Frequency controls are also reused by A/B field deltas:

- selected field determines which metadata key is diffed
- threshold/top-N/cardinality affect field delta output filtering

So changing frequency controls affects both the frequency list and A/B field-delta results.

## User flow: graphing + date-time controls + frequency analysis

1. Open a loaded log window and switch to **Dashboard** mode.
2. Review the time-series chart and pick bucket granularity (`Per second` / `Per minute`) for the current troubleshooting horizon.
3. Narrow the active scope with date-time controls:
   - set `From` / `To` directly, or
   - apply a preset (`Last N minutes`, `Visible Window`, `Full Loaded Range`, `Custom`), or
   - select a chart bucket/range to push the same bounds into `From` / `To`.
4. In **Frequency Analysis**:
   - choose `Analyze field`,
   - tune `Top N`, `Threshold`, and `Cardinality limit`,
   - click a value to apply/remove the generated `@field:` filter token.
5. If needed, clear dashboard selections to return to the broader loaded time window and recompute metrics.

### Large-dataset behavior

- For very large filtered datasets, dashboard analysis can run in deterministic sampling mode.
- Sampling metadata (`mode`, `originalCount`, `sampledCount`) is carried in dashboard state so users can interpret chart/frequency outputs with the correct scope context.

## Relevant implementation references

- `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/KLogViewerViewModel.kt`
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/LogFilterService.kt`
- `core/src/main/kotlin/com/klogviewer/core/analysis/InMemoryAnalysisMetricsRepository.kt`
- `domain/src/main/kotlin/com/klogviewer/domain/model/AnalysisModels.kt`
- `ui/src/test/kotlin/com/klogviewer/ui/viewmodel/DashboardIntentTest.kt`
