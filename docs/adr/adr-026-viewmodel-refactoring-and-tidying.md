# ADR 026: ViewModel Refactoring and Tidying

## Status
Proposed

## Context
The `KLogViewerViewModel` had grown significantly (over 1100 lines), making it difficult to maintain and read. Specifically, the `handleIntent` and `loadFilesIntoWindow` functions were excessively long and handled multiple responsibilities.

## Decision
We decided to refactor `KLogViewerViewModel` by extracting large, multi-responsibility functions into smaller, focused private helper functions. 

The `handleIntent` dispatcher was broken down into categorized handlers:
- `handleWorkspaceIntent`
- `handleFilterIntent`
- `handleUiToggleIntent`
- `handleTabWindowIntent`
- `handleEntryIntent`
- `handleSftpIntent`
- `handleDialogIntent`
- `handleRecentItemsIntent`

The `loadFilesIntoWindow` logic was decomposed into steps:
- SFTP single path handling
- State preparation
- Heuristic detection
- Flow creation
- Failure handling

The `handleLogUpdate` logic was also decomposed into calculation helpers for logs, missing source IDs, and discovered source IDs.

## Consequences
- Improved readability: The main dispatcher and core logic functions are now high-level sequences of steps.
- Better maintainability: Changes to specific logic (e.g. SFTP, filtering) are isolated in smaller functions.
- Easier testing: While still private, these functions provide clearer boundaries for future unit testing or potential extraction into separate classes (e.g. UseCases or separate Intent Handlers).
- Minimal impact: The public API and behavior of the ViewModel remain unchanged, verified by existing integration tests.
