# ADR-040: Sprint 9 Charting Library Selection and Benchmark Gate

## Status

Accepted

## Context

Sprint 9 restart task `14.2` requires selecting a Compose-capable charting library before dashboard reintroduction.

ADR-039 established non-negotiable Desktop performance budgets for this selection stage:

- first paint `<= 800ms` (medium dataset)
- interaction latency `<= 100ms` p95
- refresh latency `<= 250ms` p95

`14.2` also requires interaction coverage (`zoom`, `pan`, `tooltip`, `click selection`, `brush/range select`) and explicit fallback planning.

## Decision

1. **Selection protocol**
   - Candidate set for Sprint 9 selection pass:
     - `KoalaPlot` (initial target)
     - `Vico` (secondary candidate)
   - Representative dataset profile used for the selection pass:
     - `small`: `10k` events / `600` rendered buckets
     - `medium`: `100k` events / `6k` rendered buckets
     - `large`: `1M` events / `60k` rendered buckets
   - Scored dimensions:
     - performance budget fit
     - required interaction coverage
     - Compose Desktop integration fit with current codebase
     - maintenance health and release activity
     - licensing fit for this project

2. **Selected primary library**
   - Select `KoalaPlot` as Sprint 9 primary charting library for implementation.
   - Rationale:
     - Compose Multiplatform-native API with Desktop support.
     - Interaction support aligns with required dashboard behaviors (including zoom/pan capable XY graph flows and composable input handling for selection/tooltip behavior).
     - The selection pass met ADR-039 budget gates for Sprint 9 dataset profiles.
     - Licensing fit: MIT.

3. **Fallback path**
   - Select `Vico` as the fallback library.
   - Trigger fallback when either condition occurs during implementation/verification:
     - required interaction parity cannot be reached with acceptable complexity; or
     - ADR-039 budgets fail on Sprint 9 verification scenarios.
   - Preserve a chart adapter seam in `:ui` so chart backend can be switched without changing `:domain` analysis contracts.
   - Licensing fit: Apache-2.0.

4. **Maintenance and adoption notes**
   - Both candidates are actively maintained Compose-capable charting projects with current Maven artifacts.
   - Selection preference remains library-first and no custom chart engine is authorized for Sprint 9.

## Consequences

### Positive

- Unblocks `14.3` with a clear charting dependency decision.
- Keeps Sprint 9 aligned with performance-first gates rather than ad-hoc visual implementation.
- Reduces risk by predefining a production-ready fallback (`Vico`) before dashboard implementation expands.

### Negative

- Keeping a fallback path requires maintaining an adapter seam and avoiding direct library-specific coupling in domain-facing state.
- Full confidence still depends on downstream execution in `14.8` and `14.9` under append-heavy and regression-test scenarios.

## Alternatives Considered

1. **Use only KoalaPlot with no fallback**
   - Rejected: increases schedule risk if interaction/performance edge cases appear during `14.3+` implementation.
2. **Select Vico as primary immediately**
   - Rejected: `14.2.3` preference is to adopt `KoalaPlot` when it satisfies gates; the selection pass supports that preference.
3. **Build a custom Compose chart surface**
   - Rejected: conflicts with Sprint 9 restart direction (`no custom chart engine`).
