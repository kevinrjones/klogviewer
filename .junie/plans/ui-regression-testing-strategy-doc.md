---
sessionId: session-260521-121935-1nen
---

# Requirements

### Overview & Goals
Create a dedicated testing strategy document in `docs/` focused on reducing UI regressions in KLogViewer, with a clear recommendation on where screenshot testing should and should not be used.

### Scope
#### In Scope
- Produce a new documentation artifact in `docs/` (no production code changes).
- Define a practical, layered strategy for preventing UI regressions (unit/integration/UI/E2E).
- Include screenshot/visual regression testing guidance, including adoption criteria and guardrails.
- Map strategy recommendations to the current project test stack and workflows.

#### Out of Scope
- Implementing new tests.
- Adding new dependencies or changing Gradle configuration.
- Refactoring existing production or test code.

### Functional Requirements
- The document must explain the current baseline and known gaps in UI regression protection.
- The document must define a prioritized plan (what to test first and why).
- The document must include a screenshot testing section with:
  - candidate surfaces in this app,
  - baseline management expectations,
  - CI stability considerations,
  - clear “use screenshot vs functional UI assertion” decision rules.
- The document must include actionable conventions for test ownership, quality gates, and triage when regressions are found.

# Technical Design

### Current Implementation
Based on the current codebase and docs:
- Existing general strategy doc: `docs/TESTING.md` (already includes a lightweight “Functional UI Tests vs Visual Regression” section).
- Existing UI-test architecture references:
  - `docs/adr/adr-023-ui-testing-framework.md`
  - `docs/adr/adr-024-robot-pattern.md`
- Existing UI test suites and Robot pattern usage:
  - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerSmokeTest.kt`
  - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerUiTest.kt`
  - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerComplexUiTest.kt`
  - `ui/src/test/kotlin/com/klogviewer/ui/robot/*`
- Current build/test setup confirms desktop UI test execution via `desktopTest` in `ui/build.gradle.kts`.

### Key Decisions
- Create a **new focused strategy document** for UI regression mitigation rather than overloading `docs/TESTING.md`.
- Keep `docs/TESTING.md` as the broad testing overview and link to the new focused strategy.
- Treat screenshot testing as a targeted complement to functional UI tests, not a replacement.

### Proposed Changes
- Add a new document (proposed path): `docs/UI-REGRESSION-TESTING-STRATEGY.md` containing:
  - regression problem framing and objectives,
  - test-layer responsibilities (unit, integration, functional UI, visual/screenshot),
  - “critical user journey” coverage matrix for this app’s UI flows,
  - screenshot testing rollout plan (pilot scope, baseline policy, CI execution model, review workflow),
  - flakiness controls and maintenance policy,
  - phased adoption roadmap with measurable success criteria.
- Update `docs/TESTING.md` with a short cross-reference section to the new strategy doc so teams can discover it from the existing testing entry point.

### File Structure
- `docs/UI-REGRESSION-TESTING-STRATEGY.md` (new)
- `docs/TESTING.md` (small update: link/reference only)

# Delivery Steps

### ✓ Step 1: Draft focused UI regression strategy document
A dedicated strategy document exists in `docs/` that defines how UI regressions will be reduced.
- Create `docs/UI-REGRESSION-TESTING-STRATEGY.md` with a clear problem statement and goals.
- Document the current baseline using existing assets (`docs/TESTING.md`, ADR-023/024, and current `ui` test suites).
- Define layered responsibilities across unit, integration, functional UI, and visual regression testing.
- Add a prioritized coverage plan for critical KLogViewer UI workflows (file load, filtering, search, split panes, tab/window behavior).

### ✓ Step 2: Define screenshot testing policy and rollout
The strategy includes a concrete, low-risk screenshot-testing approach tailored to this project.
- Specify where screenshot tests are valuable (layout/theming/ANSI rendering surfaces) vs where functional assertions are preferred.
- Define baseline image management, environment normalization, and flakiness mitigations for CI.
- Provide failure triage rules and ownership expectations so regressions are handled consistently.
- Describe phased adoption milestones and success metrics to evaluate regression reduction.

### ✓ Step 3: Link strategy into existing testing documentation
The new strategy is discoverable from current project docs without changing source code.
- Add a concise cross-link from `docs/TESTING.md` to `docs/UI-REGRESSION-TESTING-STRATEGY.md`.
- Ensure terminology aligns with project language in `docs/UBIQUITOUS_LANGUAGE.md` and established testing conventions.
- Keep all changes documentation-only, matching the request to avoid implementation/code changes.