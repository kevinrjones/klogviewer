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

### 2.5. Desktop UI Transition
- Transition from "Mobile Chic" to professional Desktop patterns.
- Implement Ribbon-style toolbar and native Menu Bar.
- Introduce Split-Pane layout for entry details.

## 3. Key Decisions
- **Workspace-Centric State**: Refactor `LogViewerState` to manage a collection of `TabState` objects.
- **LogEntry Attribution**: Add `sourceId` to `LogEntry` to track origin.
- **Chronological Merging**: Implement a merge algorithm in `MergedLogSource` to combine multiple streams.
- **MVI Extension**: Add new intents for tab management (`AddTab`, `CloseTab`, `SwitchTab`).

## 4. Definition of Done
- [x] Multiple log files can be opened in separate tabs.
- [x] Multiple log files can be merged and viewed chronologically in a single tab.
- [x] Source badges are visible and correctly colored in interleaved views.
- [x] Filters and search work correctly on interleaved streams.
- [x] Switching tabs preserves the state of each view.

## 5. Progress Log
- **2026-05-14 08:11**: Implemented Tabbed infrastructure (8.2) and Multi-File Interleaving (8.3).
- **2026-05-14 08:35**: Implemented Source Identification (8.4) including `SourceBadge` and dynamic coloring. Added "Add to Workspace" capability. Verified with integration tests (8.5).
- **2026-05-14 08:45**: Completed Desktop UI Transition (8.6). Implemented native Menu Bar, Ribbon Bar, Split-Pane detail view, and high-density grid. All Sprint 3 tasks are now complete.
