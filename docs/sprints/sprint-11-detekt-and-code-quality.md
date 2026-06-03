# Sprint 11: Detekt Static Analysis & Workflow Integration

## 1. Goal
Introduce Detekt as the standard Kotlin static-analysis tool for KLogViewer and integrate it into local development and CI workflows so code-quality rules are enforced consistently.

## 2. Scope

### 2.1. Detekt Foundation
- Add Detekt plugin/version entries to `gradle/libs.versions.toml`.
- Apply Detekt through Gradle Kotlin DSL using version-catalog plugin aliases.
- Configure Detekt for all Kotlin modules (`:app`, `:ui`, `:core`, `:domain`) with shared defaults.

### 2.2. Rules, Baseline, and Reporting
- Create a project Detekt configuration file (`detekt.yml`) aligned with existing Kotlin style and architecture expectations.
- Define a baseline strategy (`detekt-baseline.xml`) to allow incremental adoption without blocking all existing issues at once.
- Enable actionable report outputs for local and CI usage (console + machine-readable reports).

### 2.3. Development Workflow Integration
- Add a standard local verification command path (`./gradlew detekt`, and/or include in `check` once the baseline strategy is in place).
- Document how to run Detekt locally, interpret findings, and apply/remediate suppressions responsibly.
- Define suppression policy (`@Suppress`, config excludes, baseline use) to keep rules maintainable and prevent blanket suppression.

### 2.4. CI/CD Integration
- Add Detekt execution to CI build workflow and make failure behavior explicit.
- Ensure CI artifacts/reports can be reviewed when Detekt fails.
- Phase in stricter gating as the baseline shrinks.

## 3. Key Decisions
- **Incremental Adoption First**: Start with a baseline and reduce it over time to avoid delivery disruption.
- **Single Source of Truth**: Maintain one shared `detekt.yml` and one governed baseline to prevent per-module drift.
- **Fail Fast in CI**: Treat new violations as build failures once baseline protections are in place.

## 4. Definition of Done
- [ ] Detekt is configured via version catalog and wired into all Kotlin modules.
- [ ] `detekt.yml` and baseline strategy are committed and documented.
- [ ] Local development workflow includes clear Detekt run/fix steps.
- [ ] CI runs Detekt and enforces agreed failure policy.
- [ ] Team documentation explains rule management and suppression policy.
