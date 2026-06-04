# ADR-041: Detekt Static Analysis Standardization

## Status

Accepted

## Context

Sprint 11 task `16` requires integrating a consistent static-analysis workflow across all Kotlin modules (`:app`, `:ui`, `:core`, `:domain`) and enforcing it in local and CI workflows.

Before this decision, the project had no unified Detekt configuration, no baseline strategy, and no CI quality gate for Kotlin static analysis.

## Decision

1. **Adopt Detekt as the project-wide Kotlin static analysis standard**
   - Use a shared root configuration file: `detekt.yml`.
   - Use a shared root baseline file: `detekt-baseline.xml`.
   - Apply Detekt plugin through version-catalog aliasing in module Gradle scripts.

2. **Enforce local workflow integration**
   - Standard local command: `./gradlew detekt`.
   - Integrate Detekt into each module `check` lifecycle.

3. **Enforce CI quality gate**
   - Run `./gradlew detekt` in CI before tests/package stages.
   - Fail CI explicitly on Detekt violations.
   - Publish Detekt reports as CI artifacts when Detekt fails.

4. **Adopt incremental baseline governance**
   - Baseline is temporary and supports incremental adoption.
   - Enforce no-new-violations policy against baseline.
   - Track baseline burn-down by module maintainers during normal feature/refactor work.

5. **Rule and suppression governance**
   - Prefer code fixes over suppressions.
   - Allow only narrow, justified suppressions.
   - Require documented rationale for rule changes and capture major policy shifts in ADRs.

## Consequences

### Positive

- Establishes a single, repeatable static-analysis standard for all Kotlin modules.
- Prevents new static-analysis debt while allowing controlled incremental cleanup.
- Improves CI feedback quality by publishing diagnostics artifacts for failures.

### Negative

- Initial setup introduces additional build time in local and CI checks.
- Baseline maintenance requires disciplined review to avoid long-term debt stagnation.

## Alternatives Considered

1. **Run Detekt only in CI**
   - Rejected: slower developer feedback loop and higher CI churn.

2. **No baseline, fail on all existing issues immediately**
   - Rejected: high adoption friction and likely delivery disruption.

3. **Use per-module independent Detekt policies**
   - Rejected: inconsistent standards and increased maintenance burden.
