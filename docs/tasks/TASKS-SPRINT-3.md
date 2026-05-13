# TASKS: Sprint 3 - Multi-Log Support

## 8. Sprint 3: Multi-Log Support

### 8.1. Initialize Sprint 3 Documentation
- [x] 8.1.1. Create `docs/sprints/sprint-3-log.md`
- [x] 8.1.2. Create `docs/adr/adr-006-multi-log-interleaving.md`
- [x] 8.1.3. Create `docs/adr/adr-007-tabbed-interface-architecture.md`

### 8.2. Implement Tabbed Infrastructure
- [ ] 8.2.1. Define `TabState` in `:ui` module
- [ ] 8.2.2. Refactor `LogViewerState` to support `List<TabState>`
- [ ] 8.2.3. Update `LogViewerViewModel` to manage multiple `TabState` instances
- [ ] 8.2.4. Implement `TabRow` component for tab switching
- [ ] 8.2.5. Implement `AddTab`, `CloseTab`, and `SwitchTab` intents

### 8.3. Implement Multi-File Loading and Interleaving
- [ ] 8.3.1. Update `LogEntry` domain model with `sourceId: String?`
- [ ] 8.3.2. Implement `MergedLogSource` in `:core`
- [ ] 8.3.3. Update `LogViewerViewModel` to handle multi-file selection
- [ ] 8.3.4. Implement chronological merging logic

### 8.4. Source Identification and UI Polish
- [ ] 8.4.1. Implement `SourceBadge` component
- [ ] 8.4.2. Update `LogEntryRow` to display source badges
- [ ] 8.4.3. Implement dynamic source coloring
- [ ] 8.4.4. Add "Add to Workspace" capability in Sidebar/FileSelector

### 8.5. Verification
- [ ] 8.5.1. Unit test `MergedLogSource`
- [ ] 8.5.2. Integration test tab state preservation
- [ ] 8.5.3. Manual verification of interleaving two log files
