# ADR-031: Categorizing MVI Intents to Reduce Dispatch Complexity

## Context
As the `KLogViewerViewModel` evolved, the `handleIntent()` method grew significantly in size as it had to dispatch every single intent type defined in `KLogViewerIntent`. While standard in MVI, this led to high cyclomatic complexity and reduced readability in the main intent routing method.

## Decision
We will introduce a hierarchy of sealed interfaces within `KLogViewerIntent` to group related intents. `handleIntent()` will now act as a high-level category dispatcher, delegating specific intents to smaller, focused handler methods.

The introduced categories are:
- `WorkspaceIntent`: Loading files, adding to workspace, path selection, and clearing logs.
- `UiToggleIntent`: Theme, sidebar, sort order, auto-scroll, and connection toggles.
- `FilterIntent`: Adding/removing filter queries and level filtering.
- `TabWindowIntent`: Tab and window lifecycle management (add, close, switch, split, column width, parser change).
- `EntryIntent`: Log entry selection and clipboard operations.
- `DialogIntent`: Showing and dismissing various dialogs.
- `RecentItemsIntent`: History management.
- `SftpIntent`: Remote operations (already existed but was integrated into the new pattern).

## Consequences
- **Pros**:
    - Significantly reduced cyclomatic complexity in `handleIntent()`.
    - Improved readability by grouping related logic.
    - Stronger type safety in handler methods (they now take specific sealed interface subtypes).
    - Easier to navigate and maintain intent-related code.
- **Cons**:
    - Slightly more verbose intent definitions in `KLogViewerIntent.kt`.
    - Requires one extra step of dispatching for each intent.

## Implementation Details
- `KLogViewerIntent` now contains nested sealed interfaces for each category.
- `handleIntent()` uses a `when` block to dispatch to `handleWorkspaceIntent()`, `handleUiToggleIntent()`, etc.
- `ChangeParser` was moved from a special case in `handleIntent()` into the `TabWindowIntent` category for consistency.
