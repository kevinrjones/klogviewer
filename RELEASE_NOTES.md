# KLogViewer 1.7.0 Release Notes

**Release date:** 2026-06-03  
**Release type:** UI fixes and workflow improvements release  
**Scope:** Sprint 10 deliverables shipped since `1.6.x`

## Highlights

- Completed Sprint 10 UI fixes and updates with improved tab-scoped source management and safer in-context actions.
- Added destination-aware file drag-and-drop import (log view, tab bar, and welcome-page-in-tab behavior).
- Improved daily log-review ergonomics with monospaced font controls, multi-line copy, context actions, and time-filter reset.
- Improved multi-source readability with source badge filename tooltips and deterministic per-source row shading.

## New Features

### Tab and Source Workflow

- Replaced per-tab source controls with active-tab scoped source dropdown management.
- Added per-source remove control behavior while preserving tab/workspace/source state consistency.
- Added drag-and-drop file import into the app window:
  - drop on log view adds sources to the active tab;
  - drop on tab bar creates a new tab and loads dropped files;
  - drop on welcome page in an open tab loads files into that same tab window.
- Added multi-file drop handling and non-blocking feedback for invalid/unsupported dropped items.

### Log View Interaction

- Added fixed-width font selection and persistence for log rendering.
- Added single/multi-line selection with shared clipboard copy behavior.
- Added right-click context menu actions (`Copy`, `Refresh`, `Clear`) with state-aware enablement.

### Filtering and Readability

- Added `Reset` option to time filter controls to clear time bounds and show all loaded rows.
- Added source badge hover tooltips that display source filenames in multi-source tabs.
- Added deterministic per-source gray row background differentiation for improved visual tracking.

## Improvements

- Kept new UI actions on existing shared intent/handler paths to avoid divergent behavior between toolbar/menu/context/drop flows.
- Preserved existing workspace persistence, source metadata behavior, filtering semantics, and live-update consistency across Sprint 10 changes.

## Fixes and Reliability

- Improved refresh parity and context-action consistency by reusing existing connection/filter flows.
- Hardened drag-and-drop handling with path validation, mixed-validity drop support, and snackbar-based non-blocking feedback.
- Expanded interaction and integration test coverage for Sprint 10 features, including drag-drop destination behavior and welcome-tab drop handling.
- Completed regression checks for workspace persistence and live-update consistency.

## Upgrade Notes

- No migration steps are required for this release.
- Existing workspaces and preferences remain compatible.

## Known Issues

- No new customer-facing known issues were identified for this release.