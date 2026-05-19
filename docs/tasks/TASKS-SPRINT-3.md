# TASKS: Sprint 3 - Multi-Log Support

## 8. Sprint 3: Multi-Log Support

### 8.1. Initialize Sprint 3 Documentation
- [x] 8.1.1. Create `docs/sprints/sprint-3-log.md`
- [x] 8.1.2. Create `docs/adr/adr-006-multi-log-interleaving.md`
- [x] 8.1.3. Create `docs/adr/adr-007-tabbed-interface-architecture.md`

### 8.2. Implement Tabbed Infrastructure
- [x] 8.2.1. Define `TabState` in `:ui` module
- [x] 8.2.2. Refactor `KLogViewerState` to support `List<TabState>`
- [x] 8.2.3. Update `KLogViewerViewModel` to manage multiple `TabState` instances
- [x] 8.2.4. Implement `TabRow` component for tab switching
- [x] 8.2.5. Implement `AddTab`, `CloseTab`, and `SwitchTab` intents

### 8.3. Implement Multi-File Loading and Interleaving
- [x] 8.3.1. Update `LogEntry` domain model with `sourceId: String?`
- [x] 8.3.2. Implement `MergedLogSource` in `:core`
- [x] 8.3.3. Update `KLogViewerViewModel` to handle multi-file selection
- [x] 8.3.4. Implement chronological merging logic

### 8.4. Source Identification and UI Polish
- [x] 8.4.1. Implement `SourceBadge` component
- [x] 8.4.2. Update `LogEntryRow` to display source badges
- [x] 8.4.3. Implement dynamic source coloring
- [x] 8.4.4. Add "Add to Workspace" capability in Sidebar/FileSelector

### 8.5. Verification
- [x] 8.5.1. Unit test `MergedLogSource`
- [x] 8.5.2. Integration test tab state preservation
- [x] 8.5.3. Manual verification of interleaving two log files

### 8.6. Desktop UI Transition (Requirements from SOTA Review)
- [x] 8.6.1. Implement native Menu Bar using `WindowScope`
- [x] 8.6.2. Replace TopBar with a Ribbon/Toolbar component
- [x] 8.6.3. Implement Split-Pane layout for Log Entry Details
- [x] 8.6.4. Transition to high-density Grid view for log entries

### 8.7. Enhanced Grid and Tailing
- [x] 8.7.1. Implement Column Headers for the Log Grid
- [ ] 8.7.2. Support resizable columns (Stretch)
- [x] 8.7.3. Implement Local File Tailing in `FileLogSource`
- [x] 8.7.4. Update `MergedLogSource` to support real-time merges
- [x] 8.7.5. Implement "Reverse Order" (Newest First) toggle in UI

### 8.8. Window Management
- [x] 8.8.1. Increase default window size to 1200x800
- [x] 8.8.2. Center window on startup
