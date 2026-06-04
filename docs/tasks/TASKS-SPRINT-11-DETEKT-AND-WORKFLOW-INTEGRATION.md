# TASKS: Sprint 11 - Detekt and Workflow Integration

## 16. Sprint 11: Detekt Static Analysis & Workflow Integration

### 16.1. Detekt Setup and Dependency Management
- [x] 16.1.1. Add Detekt version and plugin alias to `gradle/libs.versions.toml`.
- [x] 16.1.2. Wire Detekt plugin usage in Gradle Kotlin DSL using version-catalog aliases.
- [x] 16.1.3. Apply Detekt configuration consistently to all Kotlin modules (`:app`, `:ui`, `:core`, `:domain`).

### 16.2. Rule Configuration and Baseline Strategy
- [x] 16.2.1. Create and commit project-wide `detekt.yml` configuration.
- [x] 16.2.2. Generate and commit initial `detekt-baseline.xml` for controlled incremental adoption.
- [x] 16.2.3. Define module/package exclusions only where justified and documented.
- [x] 16.2.4. Enable Detekt report outputs (human-readable and CI-consumable formats).

### 16.3. Local Development Workflow Integration
- [x] 16.3.1. Add/verify local command path for static analysis (`./gradlew detekt`).
- [x] 16.3.2. Decide and implement when Detekt runs in `check` lifecycle for developer workflows.
- [x] 16.3.3. Document remediation workflow for findings (fix, suppression, baseline update policy).
- [x] 16.3.4. Add guidance on suppression hygiene (no blanket suppressions, prefer targeted rationale).

### 16.4. CI Integration and Quality Gates
- [x] 16.4.1. Integrate Detekt execution into CI pipeline with explicit failure behavior.
- [x] 16.4.2. Publish Detekt reports/artifacts from CI for failed builds.
- [x] 16.4.3. Enforce no-new-violations policy against baseline.
- [x] 16.4.4. Define baseline burn-down process and ownership for tightening quality gates.

### 16.5. Documentation and Team Adoption
- [x] 16.5.1. Update `README.md` (or contributor docs) with Detekt setup and run instructions.
- [x] 16.5.2. Document rule-change governance process (how/when rules are added or relaxed).
- [x] 16.5.3. Capture architecture decision in an ADR for static-analysis standardization.

### 16.6. Verification and Regression Safety
- [x] 16.6.1. Validate Detekt runs successfully locally across all modules.
- [x] 16.6.2. Verify CI pass/fail behavior for Detekt violations.
- [x] 16.6.3. Confirm existing build/test workflows remain operational after integration.
