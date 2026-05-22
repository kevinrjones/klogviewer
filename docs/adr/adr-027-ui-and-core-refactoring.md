# ADR 027: UI and Core Refactoring

## Status
Accepted

## Context
Following the successful refactoring of `KLogViewerViewModel`, other large and complex files in the application needed similar tidying up to improve maintainability, readability, and adherence to the project's coding guidelines (small functions, descriptive naming, decomposition).

The primary targets were:
- `KLogViewerScreen.kt`: A massive Composable (~400 lines) handling various responsibilities.
- `DirectoryLogSource.kt`: A complex `LogSource` implementation with nested coroutine logic and state management.
- `LogList.kt`: Containing large Composable functions for rendering log entries and headers.

## Decision
We decided to apply the `code-tidy-up` pattern across these components:

1.  **UI Decomposition**:
    - Decomposed `KLogViewerScreen` into logical sub-composables: `DialogHandler`, `LogTopBar`, `LogBottomBar`, `LogWindowList`, and `LogWindowItem`.
    - Extracted `RecentItemsDialog` to its own file to reduce the size of `KLogViewerScreen.kt`.
    - Simplified `LogEntryRow` in `LogList.kt` by extracting `LogGutter` and `LogEntryCell`.

2.  **Core Logic Refactoring**:
    - Decomposed `DirectoryLogSource.observeLogs` by extracting focused private functions for directory scanning, handling new/removed files, and initial load detection.
    - Used `ProducerScope` extensions and private helper functions to manage the complex `channelFlow` logic.

## Consequences
- **Improved Maintainability**: Smaller, focused functions are easier to understand, test, and modify.
- **Enhanced Readability**: The UI structure is now clearly visible in the top-level composables.
- **Better Error Isolation**: Logic for specific features (like SFTP dialogs or directory scanning) is now better isolated.
- **Slightly Increased File Count**: Moving `RecentItemsDialog` to its own file adds a new file but reduces the complexity of the main screen file.
