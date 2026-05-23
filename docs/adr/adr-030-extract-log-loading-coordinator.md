# ADR-030: Extracting Log Loading Orchestration to LogLoadingCoordinator

## Context
The `KLogViewerViewModel` continued to handle a large number of responsibilities, particularly around the orchestration of log loading from various sources (local files, directories, SFTP). This led to high complexity in the ViewModel, especially in managing coroutine jobs, heuristic detection, and updating window states during the loading process.

## Decision
We will extract the log loading and connection orchestration logic from `KLogViewerViewModel` into a dedicated class: `LogLoadingCoordinator`.

The `LogLoadingCoordinator` will own the following responsibilities:
- Managing log loading jobs for each window (`logJobs`).
- Coordinating the sequence of steps for loading logs: heuristic detection, state updates, flow creation, and collection.
- Handling SFTP connection orchestration (single file, multiple files, directories).
- Managing redundant path filtering and parser result mapping.
- Handling log loading failures and reporting errors to the UI.

The ViewModel will delegate all loading-related operations to this coordinator.

## Consequences
- **Pros**:
    - Further reduction in `KLogViewerViewModel` complexity (reduced by ~400 lines).
    - Clearer separation of concerns: ViewModel handles MVI intent routing and high-level state management, while the coordinator handles the specialized logic of loading and connecting to log sources.
    - Improved testability of the loading orchestration logic.
    - Centralized management of log observation jobs.
- **Cons**:
    - Another component added to the ViewModel's dependencies.
    - Requires callbacks or event flows to update the ViewModel's state or trigger events.

## Implementation Details
- `LogLoadingCoordinator` is initialized with all necessary dependencies (FileSystems, LogSources, etc.) and the ViewModel's scope and state.
- Callbacks are used to update state (`onHandleLogUpdate`) and show errors (`onShowError`) to maintain the ViewModel's role as the state owner.
- `SftpIntentHandler` now delegates its loading-related intents to the `LogLoadingCoordinator`.
