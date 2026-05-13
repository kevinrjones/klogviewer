# ADR 007: Tabbed Interface Architecture

## Status
Proposed

## Context
As we support multiple concurrent log views, a single state for logs in `LogViewerState` is no longer sufficient. We need a way to manage multiple independent views, each with its own file(s), filters, and UI state.

## Decision
We will refactor the MVI state to be "Workspace-Centric", supporting a list of tabs.

### 1. TabState
A new `TabState` data class will be introduced to encapsulate all data specific to a single tab:
- `id: String` (Unique identifier)
- `title: String` (Display name)
- `logs: List<LogEntry>`
- `filteredLogs: List<LogEntry>`
- `searchQuery: String`
- `levelFilters: Set<LogLevel>`
- `scrollPosition: Int` (Optional enhancement)
- `sourceIds: List<String>` (The files contributing to this tab)

### 2. LogViewerState Refactoring
`LogViewerState` will now hold:
- `tabs: List<TabState>`
- `activeTabId: String?`
- Global settings (e.g., `isDarkMode`, `isSidebarExpanded`)

### 3. Navigation and Lifecycle
- `LogViewerIntent` will include `AddTab`, `CloseTab`, and `SwitchTab`.
- Opening a new file via the UI will either create a new tab or add to the current one based on user choice (e.g., Shift+Click to interleave).

## Consequences
- **Positive**: Clean separation of state between different log views.
- **Positive**: Scaling to multiple files is natural.
- **Negative**: Increased complexity in the `ViewModel` to manage multiple active loading jobs.
- **Negative**: Higher memory footprint for maintaining multiple sets of log entries.
