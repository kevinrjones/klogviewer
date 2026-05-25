# ADR-038: Compose Charting Strategy for Dashboard Walking Skeleton

## Status
Accepted

## Context

Sprint 9 requires a Compose-native charting capability for dashboard metrics with interactive click-through to filtering. For 14.2 we need a minimal vertical slice that is:

- fully runnable in the existing UI flow
- interactive enough to validate data-point selection
- easy to evolve into richer chart interactions later (zoom/pan/tooltip)

Adding a heavyweight third-party chart library in the first slice increases integration risk and slows delivery of the foundational architecture milestones.

## Decision

Use a staged charting approach:

1. **Walking skeleton (14.2)**: implement a Compose-native lightweight chart surface with standard Compose primitives (`Surface`, `Row`, `Column`, `Box`) to render time-bucket bars and capture click events.
2. Keep interaction model explicit via MVI intents:
   - `ShowDashboard` / `ShowLogs`
   - `SelectDashboardBucket` / `ClearDashboardBucketFilter`
3. Preserve a chart adapter seam in UI state (`DashboardUiState.Content` with bucket models) so a dedicated charting library can be introduced later without changing core/domain contracts.

## Consequences

### Positive

- Delivers Sprint 9 vertical slice quickly with low integration risk.
- Keeps the UI interactive and testable using existing Compose tooling.
- Defers chart-library lock-in until richer interaction requirements are implemented.

### Negative

- Initial visuals are intentionally basic compared to specialized chart libraries.
- Advanced behaviors (zoom/pan/tooltip) still require future integration work.

## Alternatives Considered

1. **Integrate third-party chart library immediately**
   - Rejected for this slice: larger dependency and theming surface before architecture baseline is proven.
2. **Use static text-only metrics for walking skeleton**
   - Rejected: does not validate the required chart interaction model.
3. **Delay dashboard until full chart feature set is ready**
   - Rejected: conflicts with outside-in walking-skeleton delivery.
