# Detekt Workflow

This project uses Detekt as the Kotlin static-analysis quality gate across `:app`, `:ui`, `:core`, and `:domain`.

## Local Commands

- Run static analysis for all modules:

```bash
./gradlew detekt
```

- Regenerate baseline only when approved by maintainers:

```bash
./gradlew detektBaseline
```

Detekt configuration and baseline files:

- `detekt.yml`
- `detekt-baseline.xml`

## Lifecycle Integration

- Detekt is wired into each module `check` lifecycle.
- Running `./gradlew check` now includes Detekt validation for Kotlin modules.

## Findings Remediation Policy

When Detekt reports findings, apply this order:

1. **Fix the code** first.
2. If a rule is not suitable for a concrete case, use a **narrow suppression** (`@Suppress` on the smallest element possible) with rationale.
3. If the violation is legacy and cannot be fixed immediately, propose a baseline update in a dedicated review.

## Suppression Hygiene

- Do not use blanket/file-wide suppressions when a function/property/class level suppression is possible.
- Every suppression must include a short reason that explains why it is needed.
- Prefer replacing suppressions with code fixes during normal feature work.

## Baseline Governance and Burn-down

- `detekt-baseline.xml` is a temporary adoption mechanism and must only be changed in explicit maintenance PRs.
- No-new-violations policy is enforced by CI via `./gradlew detekt` against the existing baseline.
- Baseline burn-down ownership: module maintainers reduce legacy entries incrementally each sprint, prioritizing touched files.

## Rule-Change Governance

- Rule additions/relaxations must be discussed in code review and captured in an ADR when they alter project standards.
- Rule changes must include:
  - rationale,
  - expected developer impact,
  - migration plan (if any).
