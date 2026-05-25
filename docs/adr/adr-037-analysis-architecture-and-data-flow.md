# ADR-037: Analysis Architecture and Data Flow

## Status
Accepted

## Context

Sprint 9 introduces analysis and visualization capabilities that consume parsed log data and project derived metrics into the UI. We need an explicit boundary between:

- `:core` aggregation logic and domain modeling
- `:ui` rendering and interaction state
- test/mocking seams for future frequency analysis and diffing work

Without a clear architecture, charting logic risks leaking into composables and viewmodel reducers, making incremental features (date ranges, diff windows, advanced interactions) difficult to evolve safely.

## Decision

Adopt a layered analysis flow:

1. `:domain` defines analysis tiny types and query/result models:
   - `TimeBucketSize`, `AnalysisFieldKey`, `FrequencyCount`, `DiffWindow`
   - `TimeSeriesMetricsQuery/Result`, `FieldFrequencyQuery/Result`, `DashboardMetrics`
2. `:domain` defines sealed analysis failure hierarchy (`AnalysisFailure`) and service boundaries:
   - `AnalysisMetricsRepository`
   - `LogAnalysisService`
3. `:core` provides in-memory aggregation implementation (`InMemoryAnalysisMetricsRepository`) and orchestration service (`DefaultLogAnalysisService`).
4. `:ui` maps failures to UI-safe messaging (`AnalysisFailure.toUiMessage()`) and consumes analysis results through viewmodel-managed dashboard state.
5. Dashboard interactions (bucket click-through) feed back into existing filter behavior through `LogFilterService`, preserving one filtering pipeline.

## Consequences

### Positive

- Keeps aggregation logic outside composables.
- Provides clear mocking boundary for unit tests and future adapters.
- Uses tiny types and sealed failures to reduce primitive obsession and unsafe error handling.
- Enables incremental rollout for frequency analysis and diffing on top of existing contracts.

### Negative

- Introduces additional models and conversion steps.
- Requires synchronization between dashboard state and existing window filtering state.

## Alternatives Considered

1. **Compute metrics directly in `KLogViewerViewModel`**
   - Rejected: inflates viewmodel responsibilities and weakens test boundaries.
2. **Compute metrics directly inside composables**
   - Rejected: violates unidirectional flow and complicates performance/cancellation control.
3. **Defer service/repository boundary until later sprint items**
   - Rejected: would force rework when ad-hoc frequency and diff features are added.
