# ADR-039: Sprint 9 Restart Foundations for Graphing, Analysis, and Date-Time Controls

## Status

Accepted

## Context

Sprint 9 was restarted with a new scope focused on high-performance graphing, analysis workflows, and synchronized
date-time controls.

The previous Sprint 9 checklist included earlier dashboard slices that were later rolled back. We need one explicit
restart baseline so all remaining Sprint 9 execution references the same scope and constraints.

We also need to reconfirm the domain contracts already in place for analysis workflows and set measurable performance
budgets before chart-library selection and implementation work proceeds.

## Decision

1. **Scope supersession**
    - Treat `docs/tasks/TASKS-SPRINT-9-ANALYSIS-AND-VISUALIZATION.md` (Restart) as the authoritative Sprint 9 checklist.
    - Prior Sprint 9 checklist completion state is superseded for execution planning and rollout readiness.

2. **Reconfirmed analysis tiny types and sealed failures**
    - Keep the existing analysis tiny-type model as the canonical contract:
        - `TimeBucketSize` (bucket granularity)
        - `AnalysisFieldKey` (structured-field key)
        - `FrequencyCount` (validated non-negative counts)
        - `DiffWindow` (time-range window used directly and as A/B compare window input)
    - Keep sealed failure hierarchy `AnalysisFailure` as the typed error contract for analysis flows:
        - `NoTimestampData`
        - `InvalidTimeBucketSize`
        - `InvalidFieldKey`
        - `InvalidFrequencyCount`
        - `InvalidDiffWindow`
        - `FieldUnavailable`
        - `Unexpected`

3. **Performance budgets (gates for 14.2+ work)**
    - Define and use these baseline budgets for Desktop analysis/charting validation:
        - **First paint** (dashboard metrics + initial chart render): `<= 800ms` on representative medium dataset.
        - **Interaction latency** (click select / hover tooltip / range preset apply): `<= 100ms` p95.
        - **Refresh time** (recompute + rerender after appended updates or range/filter changes): `<= 250ms` p95.
    - Any selected charting library must satisfy these budgets under Sprint 9 benchmark scenarios before final adoption.

## Consequences

### Positive

- Eliminates Sprint 9 scope ambiguity after restart.
- Preserves existing type-safe analysis contracts and typed-error handling.
- Introduces explicit performance gates early, reducing risk of late chart-library rework.

### Negative

- Budgets may require trade-offs (sampling, simplification, or fallback library) for very large datasets.
- Existing historical records can appear inconsistent unless readers follow the restart checklist and this ADR together.

## Alternatives Considered

1. **Keep previous Sprint 9 completion marks as-is and continue incrementally**
    - Rejected: creates execution ambiguity and mixes superseded and active scope.
2. **Redefine new tiny types/failures for restart**
    - Rejected: current contracts already satisfy restart foundations and avoid unnecessary churn.
3. **Delay performance budgets until after library integration**
    - Rejected: increases risk of selecting a library that cannot meet responsiveness requirements.
