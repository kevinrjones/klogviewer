# UI Regression Testing Strategy

## 1. Purpose

This document defines a focused strategy for reducing UI regressions in KLogViewer through layered automated tests,
clear ownership, and predictable triage.

Goals:

- Catch UI regressions before merge with stable, high-signal checks.
- Prioritize user-critical workflows first.
- Use screenshot testing as a targeted complement to functional UI tests.

## 2. Current Baseline

KLogViewer already has foundational testing assets:

- Broad testing strategy in `docs/TESTING.md`.
- UI architecture and test decisions in:
    - `docs/adr/adr-023-ui-testing-framework.md`
    - `docs/adr/adr-024-robot-pattern.md`
- Existing Compose desktop UI suites:
    - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerSmokeTest.kt`
    - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerUiTest.kt`
    - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerComplexUiTest.kt`
- Robot abstractions in `ui/src/test/kotlin/com/klogviewer/ui/robot/`.
- UI execution task available via `./gradlew :ui:desktopTest`.

### Baseline Strengths

- Core journeys are already covered (load file, filter by level, search, tab/split interactions).
- Robot Pattern reduces test fragility from selector changes.
- Headless-oriented setup and mocking patterns are documented and in use.

### Known Gaps

- No dedicated, project-level policy for preventing visual regressions.
- No explicit prioritization matrix that ties workflow criticality to test depth.
- Visual consistency risks (theme, spacing, ANSI rendering) rely heavily on manual checks.
- Ownership and triage conventions for UI regressions are not centralized.

## 3. Layered Regression-Prevention Model

### Layer A: Unit Tests (fastest feedback)

Scope:

- Pure logic used by UI state transitions (formatting, filtering, parsing helpers, mappers).

Primary purpose:

- Prevent behavioral drift in small, deterministic units before UI tests run.

### Layer B: Integration Tests (state orchestration)

Scope:

- ViewModel/repository interactions and state flows that drive Compose screens.

Primary purpose:

- Validate that intents and data updates produce expected screen state for each `Workspace`, `Tab`, `Log Window`, and
  `Filter` scenario.

### Layer C: Functional UI Tests (default for UI behavior)

Scope:

- User interactions and outcomes: file load, filter/search behavior, split view, tab switching, selection behavior.

Primary purpose:

- Verify what users can do and what state/UI semantics they observe.

Default assertion style:

- Semantics/test tag assertions and behavior assertions (not pixels).

### Layer D: Visual Regression Tests (targeted)

Scope:

- Narrow set of rendering-sensitive surfaces where behavior assertions are insufficient.

Primary purpose:

- Detect unintended visual changes in layout, theme contrast, ANSI styling, and high-density log presentation.

Rule:

- Visual regression checks are additive and must never replace functional assertions for business behavior.

## 4. Prioritized Coverage Plan (What to Test First)

Priority is based on user impact and regression frequency risk.

| Priority | Workflow                                              | Why First                                                       | Primary Layer     | Supporting Layer |
|----------|-------------------------------------------------------|-----------------------------------------------------------------|-------------------|------------------|
| P0       | File load into active Log Window                      | Entry point for all analysis; failures block core usage         | Functional UI     | Integration      |
| P0       | Filter by Log Level and text Filter                   | High-frequency interaction; directly affects trust in results   | Functional UI     | Unit/Integration |
| P0       | Search and highlight behavior                         | Core investigation workflow; easy to regress via UI refactors   | Functional UI     | Integration      |
| P1       | Split View interactions (active window and selection) | Complex interaction model with higher regression risk           | Functional UI     | Integration      |
| P1       | Tab creation/switching/state retention                | Multi-context analysis flow; regressions are disruptive         | Functional UI     | Integration      |
| P2       | Rendering consistency (themes/ANSI/spacing)           | Important for usability and readability but not always blocking | Visual Regression | Functional UI    |

## 5. Coverage Matrix by Workflow

| Workflow                                       | Unit     | Integration | Functional UI | Visual Regression |
|------------------------------------------------|----------|-------------|---------------|-------------------|
| Load file and show initial entries             | Optional | Required    | Required      | Optional          |
| Filter by level / text                         | Optional | Required    | Required      | No                |
| Search term and result visibility              | Optional | Required    | Required      | No                |
| Split View activation and independent behavior | No       | Required    | Required      | Optional          |
| Tabs and workspace state behavior              | No       | Required    | Required      | No                |
| Theme / ANSI color readability                 | No       | No          | Optional      | Required          |

## 6. Quality Gates

Before merge:

- Relevant unit/integration tests must pass for touched behavior.
- `:ui:desktopTest` must pass for changed UI flows.
- If change touches rendering-sensitive surfaces, visual checks defined in this strategy must pass.

Release readiness:

- P0 and P1 workflow coverage remains green.
- Any open P0 UI regression is release-blocking.

## 7. Ownership Model

- Feature author owns adding/updating tests for changed UI behavior.
- Reviewer verifies test-layer choice and regression risk coverage.
- UI maintainers own the strategy and periodic recalibration of priorities.

## 8. Regression Triage Flow

When a regression is detected:

1. Classify as behavioral or visual.
2. Determine affected workflow priority (P0/P1/P2).
3. Create/assign issue with failing test evidence.
4. Fix and backfill missing test coverage at the appropriate layer.
5. Record recurring root causes in `docs/RECAP.md` and update this strategy when patterns repeat.

## 9. Screenshot Testing Policy

### 9.1 Candidate Surfaces in KLogViewer

Screenshot checks are recommended for surfaces where pixel/visual fidelity is the value:

- ANSI color rendering in log content (foreground, emphasis, reset behavior).
- Theme integrity (Industrial Dark / Clean Light contrast, spacing rhythm, typography balance).
- Dense log-row layout and column-header alignment.
- Split View visual boundaries and active-window highlighting.
- Icon and badge rendering where subtle visual drift affects usability.

Screenshot checks are **not** recommended as primary validation for:

- `Filter` logic outcomes.
- `Search` matching behavior.
- `Tab` switching/state persistence.
- `Workspace` interaction semantics (selection, focus transitions).
- Any behavior already covered by deterministic semantics/state assertions.

### 9.2 Decision Rules: Screenshot vs Functional Assertion

Use this rule set during test design:

1. If the expected outcome can be validated through state/semantics with high confidence, use a functional assertion.
2. If the regression risk is primarily about appearance (spacing, contrast, rendering), add screenshot coverage.
3. If both behavior and rendering matter, keep functional assertions as primary and add a small screenshot assertion set
   for visual guardrails.
4. Never replace an existing stable functional test with a screenshot-only test.

### 9.3 Baseline Image Management

- Store baselines in a dedicated, version-controlled location (for example, `ui/src/test/resources/visual-baselines/`).
- Keep baseline names deterministic using a convention:
    - `<surface>__<theme>__<density>__<scenario>.png`
- Baseline updates require explicit reviewer approval with rationale in PR notes.
- Baseline update PRs must include:
    - before/after diff snapshots,
    - reason for visual change,
    - confirmation that functional tests remain green.

### 9.4 CI Stability & Environment Normalization

To reduce false positives, normalize screenshot execution inputs:

- Fixed OS image and JDK version for screenshot jobs.
- Fixed fonts and font rendering configuration.
- Fixed locale/time zone.
- Fixed UI scale/density and deterministic window size.
- Headless display setup consistent across runs.

Additional flakiness controls:

- Capture screenshots only after Compose idle/synchronization points.
- Avoid animations/transitions in screenshot scenarios where possible.
- Keep screenshot scenarios narrow and deterministic (single concern per baseline).

## 10. Failure Triage, Ownership, and Rollout

### 10.1 Triage Rules for Visual Failures

On screenshot mismatch:

1. Confirm reproducibility in the normalized environment.
2. Classify failure:
    - expected visual update,
    - unintended UI regression,
    - infra/noise (font/rendering drift).
3. For expected update: refresh baseline in same PR with explicit rationale.
4. For unintended regression: block merge and attach visual diff evidence.
5. For infra/noise: open follow-up for stabilization and keep quality gate strict for confirmed regressions.

### 10.2 Ownership Expectations

- Change author: owns initial diagnosis and proposed fix/baseline update.
- Reviewer: validates classification and ensures decision rules were applied.
- UI maintainers: approve baseline policy exceptions and monitor recurring instability.

### 10.3 Phased Adoption Roadmap

Phase 0 (now):

- Functional UI tests remain the primary gate for behavior.
- Prepare conventions, naming, and CI normalization contract.

Phase 1 (pilot):

- Add a minimal pilot set for ANSI rendering and theme/layout surfaces.
- Track false-positive rate and time-to-triage.

Phase 2 (expand):

- Add high-value Split View and density-sensitive surfaces.
- Retain strict cap on baseline count to control maintenance cost.

Phase 3 (steady-state):

- Fold stable visual checks into default CI quality gates.
- Reassess coverage quarterly based on regression history.

### 10.4 Success Metrics

Use these indicators to evaluate regression reduction:

- Fewer escaped UI regressions in P0/P1 workflows per release.
- Screenshot false-positive rate maintained below an agreed threshold.
- Mean time to triage visual failures trending down.
- No increase in flaky test reruns for `:ui:desktopTest`.