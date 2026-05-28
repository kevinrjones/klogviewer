# Dashboard A/B Comparison

This document describes exactly how the dashboard **A/B comparison** works in the current implementation.

## Scope and data source

- The A/B comparison panel is rendered in the dashboard sidebar (`ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`).
- Comparison is computed from the window's **current `filteredLogs`** set, not the unfiltered raw logs (`ui/src/main/kotlin/com/klogviewer/ui/viewmodel/KLogViewerViewModel.kt`, `buildDashboardDataState(...)`).
- Computation runs in `computeComparisonState(...)` and is stored in `DashboardComparisonState` (`ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerState.kt`).

## UI controls

The panel exposes two ranges:

- **Baseline**
  - `From`
  - `To`
- **Comparison**
  - `From`
  - `To`

Actions:

- `Run compare`
- `Clear compare`

Output sections:

- `Level deltas`
- `Field deltas`

## Input parsing and validation

Each range endpoint is free-text and parsed into `Instant?` via `TimeRangeFilterSupport.parseInstantOrNull(...)`.

Supported parsing includes:

- ISO instants / offset date-times
- common log date-time patterns
- epoch values (seconds/millis)

Validation messages come from `TimeRangeFilterSupport.validationMessage(...)`:

- `Could not parse 'From' date/time`
- `Could not parse 'To' date/time`
- `From must be before or equal to To`

Notes:

- `From` and/or `To` may be blank; blanks become open-ended bounds (`null`).
- Validation is range-local: baseline and comparison ranges are validated independently.

## When comparison is computed

`computeComparisonState(...)` computes deltas only when **all** conditions are true:

1. Baseline range has at least one non-blank endpoint.
2. Comparison range has at least one non-blank endpoint.
3. Neither range has a validation error.

If any condition fails, both `levelDeltas` and `fieldDeltas` are returned empty.

## Run and clear behavior

- Editing any baseline/comparison text field updates parsed instant + validation and clears existing deltas, but does **not** immediately rerun filtering/comparison.
- `Run compare` triggers `filterLogs(windowId)`, which rebuilds dashboard state and recomputes A/B deltas.
- `Clear compare` resets `comparisonState` to default empty ranges and empty deltas, then re-filters.

## Exact comparison logic

### Window selection

- Baseline and comparison windows are represented as `DiffWindow(from, to)`.
- Entry inclusion is inclusive on both bounds (`DiffWindow.contains(...)`).
- Only entries with non-null `entry.instant` participate in A/B windows (`filterInWindow(...)`).

### Level deltas

For each `LogLevel`:

- `baselineCount = count(level in baselineEntries)`
- `comparisonCount = count(level in comparisonEntries)`
- `delta = comparisonCount - baselineCount`
- `direction`:
  - `INCREASE` if `delta > 0`
  - `DECREASE` if `delta < 0`
  - `UNCHANGED` if `delta == 0`

### Field deltas

Field deltas are computed only when a dashboard frequency field is selected.

For each distinct field value seen in either range:

- Missing field values are bucketed as `(missing)`.
- `baselineCount` and `comparisonCount` are computed per value.
- `delta = comparisonCount - baselineCount`.

Filtering and ordering:

1. Keep only values where `max(baselineCount, comparisonCount) >= threshold`.
2. Sort by:
   - absolute delta descending (`abs(delta)`)
   - then comparison count descending
   - then value ascending
3. Apply `take(cardinalityLimit)`.
4. Apply `take(topN)`.

In practice, field delta output size is capped by `min(cardinalityLimit, topN)`.

## Coupling with frequency controls

The A/B field-delta computation uses the same dashboard controls as frequency analysis:

- selected field
- threshold
- top N
- cardinality limit

Changing those values affects both frequency list output and A/B field-delta output on next recomputation.

## Relevant implementation references

- `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/KLogViewerViewModel.kt`
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/TimeRangeFilterSupport.kt`
- `domain/src/main/kotlin/com/klogviewer/domain/model/AnalysisModels.kt`
- `ui/src/test/kotlin/com/klogviewer/ui/viewmodel/DashboardIntentTest.kt`
