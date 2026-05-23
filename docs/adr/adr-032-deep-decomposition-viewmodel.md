# ADR-032: Deep Decomposition of KLogViewerViewModel and LogLoadingCoordinator

## Context
Following the initial extraction of `LogLoadingCoordinator` (ADR-030) and intent categorization (ADR-031), the `KLogViewerViewModel` still held significant dispatch logic, and the `LogLoadingCoordinator` had grown into a large component (~450 lines) handling both high-level orchestration and low-level path/heuristic logic.

## Decision
We will perform a deep decomposition of both components to achieve better separation of concerns and reduce cyclomatic complexity.

### 1. Extracting WorkspaceLogLoader
We introduced `WorkspaceLogLoader` to hold the pure, non-orchestration logic for:
- Redundant path filtering.
- Heuristic parser detection.
- Log flow creation (both local and SFTP).
- SFTP URI/Config resolution.

`LogLoadingCoordinator` now delegates these tasks to `WorkspaceLogLoader` and focuses strictly on job management, state updates, and high-level flow (Loading -> Heuristic -> Loading Results -> Observation).

### 2. Extracting Intent Handlers
We extracted the remaining intent handling logic from `KLogViewerViewModel` into dedicated handler classes:
- `WorkspaceIntentHandler`: Loading, adding to workspace, path selection, and clearing.
- `UiToggleIntentHandler`: Theme, sidebar, sorting, auto-scroll, ANSI, and connection toggles.
- `FilterIntentHandler`: Query and level filter management.
- `TabWindowIntentHandler`: Tab/Window lifecycle and column management.
- `EntryIntentHandler`: Selection and clipboard operations.
- `DialogIntentHandler`: Dialog visibility management.
- `RecentItemsIntentHandler`: History management.

## Consequences
- **Pros**:
    - `KLogViewerViewModel` size reduced from ~1235 lines to ~218 lines.
    - `LogLoadingCoordinator` size reduced by ~100 lines and became more focused.
    - Improved testability: focused handlers can be tested in isolation more easily.
    - Better adherence to the Single Responsibility Principle.
    - Reduced cyclomatic complexity across the presentation layer.
- **Cons**:
    - Increased number of files in the `viewmodel` package.
    - One extra level of indirection for intent handling.

## Implementation Details
- Handlers are instantiated in the ViewModel and passed necessary dependencies/callbacks.
- Callback-based communication (e.g., `onSavePreferences`, `onFilterLogs`) is used to maintain the ViewModel as the central hub for state and events.
- All core integration tests were verified to ensure no regressions in MVI behavior.
