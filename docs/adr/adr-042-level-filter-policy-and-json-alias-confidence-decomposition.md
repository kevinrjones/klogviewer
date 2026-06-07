# ADR-042: Level-Filter and JSON Detection Policy Decomposition

## Status

Accepted

## Context

Sprint 12A structured-data updates introduced behavior that was correct at runtime but increasingly hard to maintain:

- Level-filter policy drifted across UI state, intent handling, filtering service, and ViewModel reconciliation.
- Level-filter runtime contracts regressed to raw string sets crossing state, intents, filtering, and preferences boundaries.
- JSON canonical alias definitions were duplicated between `JsonLogParser` and `HeuristicProbe`.
- JSON confidence scoring logic was embedded directly in `HeuristicProbe`, increasing branching density in an already central probe flow.

This increased coupling, made behavior harder to reason about, and raised risk of semantic drift during future structured-data work (`12B`–`12E`).

## Decision

1. **Adopt a typed level-filter key contract**
   - Introduce `LevelFilterKey` (`domain`) as a tiny type with normalization/factory helpers.
   - Keep raw-string conversion only at boundaries (preference persistence and parser/IO-derived values).

2. **Centralize level-filter behavior in a dedicated policy collaborator**
   - Introduce `LevelFilterPolicy` (`ui/viewmodel`) as the canonical home for:
     - available-level derivation + ordering,
     - single toggle,
     - toggle-all semantics,
     - reconciliation after log updates,
     - entry-level matching and raw/typed conversion helpers.
   - Keep `KLogViewerViewModel` focused on orchestration.

3. **Create one canonical alias catalog for parser and probe**
   - Introduce `CanonicalFieldAliases` (`core/parser`) with canonical keys, alias precedence lists, and grouped sets for confidence scoring.
   - Require both `JsonLogParser` and `HeuristicProbe` to consume this catalog instead of local duplicates.

4. **Extract JSON confidence scoring into a focused scorer**
   - Introduce `JsonConfidenceScorer` (`core/parser`) to own confidence weights, penalties, and score assembly.
   - Keep `HeuristicProbe` orchestration-focused (sample collection + parser decision), delegating confidence computations.

## Consequences

### Positive

- Level-filter semantics now have one policy owner with a typed runtime contract.
- ViewModel complexity is reduced by removing embedded reconciliation policy.
- Alias precedence updates are now single-source and less drift-prone.
- JSON confidence logic is independently testable via dedicated scorer tests.
- Regression tests remain green for parser detection and UI filtering behavior.

### Negative

- Introduces additional policy/collaborator classes that require consistent usage in future changes.
- Adds conversion seams between typed and raw filter representations at persistence boundaries.

## Alternatives Considered

1. **Keep existing behavior and only rename/refactor in-place**
   - Rejected: does not remove policy scattering or boundary drift.

2. **Revert to `LogLevel` enum-only filters everywhere**
   - Rejected: cannot safely represent heterogeneous runtime keys from structured/raw level values.

3. **Keep alias lists duplicated but enforce sync with tests**
   - Rejected: still creates two maintenance surfaces and slower policy evolution.

4. **Leave confidence scoring in `HeuristicProbe` and only document constants**
   - Rejected: keeps branching complexity in probe orchestration and reduces scorer-level testability.