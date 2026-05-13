# Sprint 3: Multi-Log Support (Tabs & Interleaving)

## 1. Goal
Introduce the ability to view multiple log files simultaneously through a tabbed interface and a unified interleaved view, enabling better correlation of events across different sources.

## 2. Scope

### 2.1. Tabbed Interface
- Support for multiple open tabs, each representing a log view (single file or interleaved).
- Navigation between tabs with a `TabRow` component.
- Persistence of filters, search query, and scroll position per tab.

### 2.2. Interleaved View
- Merge multiple selected log files into a single chronological stream.
- Chronological merging based on log timestamps.
- Support for selecting multiple files in the file picker to create an interleaved view.

### 2.3. Source Identification
- Visual badges in interleaved views to identify the source file of each log entry.
- Dynamic color assignment to sources for easy visual distinction.

### 2.4. Workspace Management
- Add or remove files from the current view.
- Support for adding files to an existing tab or creating new ones.

## 3. Key Decisions
- **Workspace-Centric State**: Refactor `LogViewerState` to manage a collection of `TabState` objects.
- **LogEntry Attribution**: Add `sourceId` to `LogEntry` to track origin.
- **Chronological Merging**: Implement a merge algorithm in `MergedLogSource` to combine multiple streams.
- **MVI Extension**: Add new intents for tab management (`AddTab`, `CloseTab`, `SwitchTab`).

## 4. Definition of Done
- [ ] Multiple log files can be opened in separate tabs.
- [ ] Multiple log files can be merged and viewed chronologically in a single tab.
- [ ] Source badges are visible and correctly colored in interleaved views.
- [ ] Filters and search work correctly on interleaved streams.
- [ ] Switching tabs preserves the state of each view.
