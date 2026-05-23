# ADR-029: ViewModel Refactoring for Reduced Cyclomatic Complexity

## Context
The `KLogViewerViewModel` has grown significantly (over 1200 lines), leading to high cyclomatic complexity and reduced maintainability. It currently handles diverse responsibilities including MVI state management, SFTP operations, log filtering, tab/window management, persistence, and recent items tracking.

## Decision
We will refactor `KLogViewerViewModel` by decomposing it into focused, single-responsibility components. This follows the recommendation to split intent handling and complex logic into dedicated handlers and services.

The following components will be created:

1. **`LogUpdateReducer`**: Handles the logic for merging `LogUpdate` objects into the existing list of logs for a `LogWindow`.
2. **`LogFilterService`**: Encapsulates the logic for filtering logs based on search queries and level filters.
3. **`RecentItemsManager`**: Manages the list of recent files and directories, including persistence updates.
4. **`PreferencesStateMapper`**: Handles the conversion between `KLogViewerState` and `UserPreferences`, isolating the persistence mapping logic.
5. **`TabWindowController`**: Manages the lifecycle and state transitions for tabs and windows (adding, closing, switching, splitting).
6. **`SftpIntentHandler`**: Handles SFTP-related intents, including connection management, browsing, and navigation.

## Consequences
- **Pros**:
    - Significantly reduced file size and complexity for `KLogViewerViewModel`.
    - Improved testability as each component can be unit-tested in isolation.
    - Better adherence to the Single Responsibility Principle (SRP).
    - Clearer mapping between UI intents and their implementation.
- **Cons**:
    - Increased number of files in the project.
    - Potential for slightly more boilerplate in the ViewModel for delegation.

## Implementation Details
- Services that are purely functional (like `LogUpdateReducer`) will be implemented as objects or classes with pure functions.
- Controllers that need to update state will either return new state fragments or operate on `KLogViewerState` passed as an argument.
- The ViewModel will remain the central hub for MVI state and intent routing but will delegate most of the heavy lifting.
