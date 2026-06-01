---
sessionId: session-260601-151727-1q8z
---

# Requirements

### Overview & Goals
Create a dedicated Sprint 10 task checklist from `docs/sprints/sprint-10-ui-fixes-and-updates.md` so progress can be tracked with explicit checkbox items.

### Scope
#### In Scope
- Derive an implementation-ready checklist from Sprint 10 scope sections `2.1` through `2.9`.
- Represent each major scope item as one or more concrete checkbox tasks.
- Ensure the checklist is detailed enough to execute feature-by-feature and test-by-test.
- Keep parity with Sprint 10 Definition of Done items.

#### Out of Scope
- Implementing Sprint 10 UI/code changes.
- Running builds/tests as part of this planning-only task.
- Editing unrelated sprint/task documents.

### Functional Requirements
- Provide a new markdown task file for Sprint 10 under `docs/tasks/`.
- Use the existing task-list convention used by prior sprint task docs (hierarchical section numbering + `- [ ]` checkboxes).
- Include feature sections for:
  - in-tab log file drop-down management
  - font selection for log view
  - line selection + clipboard copy
  - toolbar refresh action
  - time filter clear option
  - context menu support
  - drag-and-drop file import
  - source badge hover tooltip
  - per-source row background differentiation
- Include a verification/testing section with checkbox items tied to the Sprint 10 changes.

# Technical Design

### Current Implementation
- Sprint source definition exists in:
  - `docs/sprints/sprint-10-ui-fixes-and-updates.md`
- Existing checklist style references:
  - `docs/tasks/TASKS-SPRINT-6-UI-REDESIGN.md`
  - `docs/tasks/TASKS-SPRINT-8-CONNECTIVITY.md`
  - `docs/tasks/TASKS-SPRINT-9-ANALYSIS-AND-VISUALIZATION.md`
- Relevant implementation touchpoints (to make checklist steps actionable):
  - Toolbar/time filter controls: `ui/src/main/kotlin/com/klogviewer/ui/components/FilterBar.kt`
  - Main screen composition and window/tab workspace wiring: `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
  - Log row rendering, source badges/tooltips, row backgrounds, selection behavior: `ui/src/main/kotlin/com/klogviewer/ui/components/LogList.kt`
  - Intent/state model: `ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerIntent.kt`, `.../KLogViewerState.kt`
  - Refresh/connect behavior: `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/UiToggleIntentHandler.kt`
  - Time filter preset/clear behavior: `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/FilterIntentHandler.kt`, `.../TimeRangeFilterSupport.kt`
  - Copy-selected handling: `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/EntryIntentHandler.kt`, `.../KLogViewerViewModel.kt`
  - Desktop menu and shortcuts entrypoint: `app/src/main/kotlin/Main.kt`

### Key Decisions
- Create a dedicated Sprint 10 checklist file: `docs/tasks/TASKS-SPRINT-10-UI-FIXES-AND-UPDATES.md`.
- Follow numbering continuity after Sprint 9 (`14.x`) by using a Sprint 10 task header/section in `15.x` format.
- Keep each checkbox item implementation-focused (deliverable behavior), not micro-tasks like imports/formatting.
- Include explicit verification checkboxes in the same document rather than a separate artifact.

### Proposed Changes
- Add Sprint 10 task checklist with:
  - top-level sprint heading
  - grouped subsections matching Sprint 10 scope
  - detailed checkbox subtasks for each feature
  - a final verification block for unit/UI/integration validation expectations
- Add traceability by aligning each section to the originating Sprint 10 scope (`2.1`–`2.9`) and Definition of Done.
- Phrase checklist items so they are independently completable and easy to mark complete.

### File Structure
- **Add** `docs/tasks/TASKS-SPRINT-10-UI-FIXES-AND-UPDATES.md`

# Delivery Steps

### ✓ Step 1: Map sprint-10 scope into executable workstreams
Sprint 10 requirements are decomposed into implementation workstreams with clear ownership boundaries.
- Parse `docs/sprints/sprint-10-ui-fixes-and-updates.md` and extract every scope + definition-of-done statement.
- Group items into actionable sections that match existing project vocabulary (`Tab`, `Log Window`, `Gutter`, `Filter`, `Workspace`).
- Cross-reference each workstream to concrete UI/ViewModel files so each task is implementation-ready.

### ✓ Step 2: Draft the sprint-10 checkbox checklist document
A new Sprint 10 tasks document exists with hierarchical numbering and trackable checkbox items.
- Create `docs/tasks/TASKS-SPRINT-10-UI-FIXES-AND-UPDATES.md` using the established task format from prior sprint task files.
- Add sectioned `- [ ]` items for all Sprint 10 features (2.1–2.9), including detailed substeps where needed.
- Include dedicated verification checkboxes that cover UI behavior, intent/state handling, and regression-sensitive flows.

### ✓ Step 3: Validate completeness and traceability of the checklist
The checklist is internally consistent, complete, and directly traceable to Sprint 10 goals.
- Verify every Sprint 10 scope item and Definition of Done bullet is represented by at least one checkbox task.
- Confirm numbering, naming, and phrasing match conventions in existing `docs/tasks/TASKS-SPRINT-*` files.
- Ensure the final checklist is ready for progress tracking without requiring interpretation.

### ✓ Step 4: Add active-window source dropdown UI for 15.1
Sprint 10 item `15.1.1`/`15.1.2` is implemented with a source management drop-down scoped to the active tab/window.
- Add a source dropdown control in `KLogViewerScreen` for the active window header.
- Render one entry per loaded source and include a checked remove control for each source entry.
- Keep the control scoped to the active tab/window so it never targets inactive windows.

### ✓ Step 5: Implement source removal flow and state consistency for 15.1
Sprint 10 item `15.1.3`/`15.1.4` is implemented with deterministic state updates after removing a source.
- Add/update intent + handler logic to remove one selected source from the active window only.
- Ensure `filePath`, `sourceIds`, `missingSourceIds`, visible logs, and metadata remain consistent after removal.
- Persist workspace updates after source removal.

### ✓ Step 6: Add and run tests for 15.1 behavior
Sprint 10 verification for 15.1 is covered by automated tests and passing execution.
- Add or extend UI/state tests to validate dropdown rendering, remove action behavior, and active-window scoping.
- Run all relevant UI/viewmodel tests for modified code paths and fix any regressions.
- Confirm tests pass and the implementation is ready to mark `15.1` complete.

### ✓ Step 7: Reposition 15.1 dropdown into the existing displayed-log path field
User clarification for Sprint 10 `15.1` is applied by moving source dropdown behavior into the existing header path field area.
- Replace the separate source-management row in `KLogViewerScreen` with a dropdown anchored in the existing file-path display section.
- Keep one entry per loaded source with checked `[x]` removal control in that dropdown.
- Preserve active-window scoping and existing header readability/tooltip behavior.

### ✓ Step 8: Align tests and re-verify 15.1 after UI relocation
Relocated dropdown behavior is covered by tests and validated with passing execution.
- Update or add UI tests to assert dropdown presence/behavior at the header path-field location.
- Run all relevant UI/viewmodel tests for changed files and fix regressions.
- Confirm Sprint 10 `15.1` behavior still matches removal/state consistency expectations.

### ✓ Step 9: Show fully qualified source paths in the header dropdown
Follow-up UI request for Sprint 10 `15.1` is applied by showing full source paths in dropdown entries.
- Update dropdown item rendering in `KLogViewerScreen` to display each source path as fully qualified text.
- Keep dropdown behavior scoped to active window and preserve existing remove-action wiring.
- Replace symbolic remove affordance text with explicit `Remove` action wording.

### ✓ Step 10: Re-verify dropdown rendering and removal action labels
Updated dropdown text behavior is covered by UI tests and passing execution.
- Update/add UI tests to assert full path visibility and the `Remove` action label.
- Run relevant UI tests for modified components.
- Confirm no regressions in active-window source removal behavior.

### ✓ Step 11: Improve dropdown width and per-entry hover tooltip behavior
Follow-up UI request for Sprint 10 `15.1` is applied by widening the source dropdown and exposing full paths via tooltip on hover.
- Increase dropdown menu width so long source paths are more readable in each entry.
- Add a hover tooltip for each dropdown entry path that shows the full fully-qualified filename.
- Ensure tooltip text is never truncated and remains aligned with active-window source scope.

### ✓ Step 12: Re-verify widened dropdown and tooltip behavior
Dropdown width and tooltip updates are covered by tests and validated with passing execution.
- Update/add UI tests to validate tooltip-ready path entries and dropdown rendering after width changes.
- Run relevant UI tests for modified components.
- Confirm no regressions in active-window source removal behavior.

### ✓ Step 13: Replace source removal with per-source hide/show behavior
Follow-up UI request for Sprint 10 `15.1` is applied by toggling source visibility instead of removing sources from the active window.
- Replace remove intent/handler behavior with hide/show toggling for each source in the active window.
- Ensure hiding a source removes its log rows from the visible list while preserving the source in window metadata.
- Update dropdown action labels to `Hide`/`Show` based on current source visibility state.
- Render shown source names with darker emphasis in the header path-display text.

### ✓ Step 14: Re-verify hide/show behavior and header styling
Hide/show source behavior is covered by tests and validated with passing execution.
- Update/add UI tests for hide/show action labels, visible-log updates, and shown-source text emphasis in the header.
- Run relevant UI/viewmodel tests for modified files and fix regressions.
- Confirm no regressions in active-window source scoping or dropdown behavior.

### ✓ Step 15: Increase shown/hidden filename color emphasis
Follow-up UI request for Sprint 10 `15.1` is applied by increasing the visual contrast between shown and hidden source names.
- Update source-name color selection in `KLogViewerScreen` so shown names are more clearly darker than hidden names.
- Keep styling theme-aware (light/dark) and avoid reducing readability in either mode.
- Preserve existing source ordering and hide/show interaction behavior.

### ✓ Step 16: Re-verify emphasized color differentiation
Stronger shown-vs-hidden color differentiation is covered by tests and validated with passing execution.
- Update/add UI tests to assert a larger measurable contrast between shown and hidden source name rendering.
- Run relevant UI/viewmodel tests for modified files and fix regressions.
- Confirm no regressions in active-window hide/show behavior.