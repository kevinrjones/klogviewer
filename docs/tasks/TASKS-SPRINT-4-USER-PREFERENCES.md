# TASKS: Sprint 4 - User Preferences & UI Refinement

## 9. Sprint 4: User Preferences & UI Refinement

### 9.1. Initialize Sprint 4 Documentation
- [x] 9.1.1. Create `docs/sprints/sprint-4-user-prefs.md`
- [x] 9.1.2. Create ADR for User Preferences Persistence Strategy

### 9.2. User Preferences Persistence
- [x] 9.2.1. Define `UserPreferences` data model (WindowState, MRU lists)
- [x] 9.2.2. Implement `PreferencesRepository` with platform-specific paths
- [x] 9.2.3. Integrate preferences into `LogViewerViewModel`
- [x] 9.2.4. Save window state (size/position) on close and restore on startup
- [x] 9.2.5. Implement `kotlinx-serialization` for JSON-based storage

### 9.3. Recursive Directory Loading
- [ ] 9.3.1. Implement `DirectoryScanner` for recursive log discovery
- [ ] 9.3.2. Extend `MergedLogSource` or create `DirectoryLogSource` to handle dynamic file lists
- [ ] 9.3.3. Update UI to allow selecting directories

### 9.4. UI Simplification ("Enema")
- [ ] 9.4.1. Remove log level selectors from Sidebar
- [ ] 9.4.2. Remove log level selectors from Top/Nav bar if any remain
- [ ] 9.4.3. Refine `RibbonBar` layout for better space utilization
- [ ] 9.4.4. Ensure all filtering is accessible via the Ribbon Bar or keyboard shortcuts

### 9.5. Recent Files Menu
- [x] 9.5.1. Implement tracking of recently opened files/directories
- [x] 9.5.2. Update `MenuBar` to include "Recently Opened" sub-menu
- [x] 9.5.3. Implement intent to open a recently used item
- [x] 9.5.4. Implement "Show All" recent items dialog

### 9.6. Verification & Polish
- [x] 9.6.1. Unit tests for `PreferencesRepository`
- [ ] 9.6.2. Unit tests for recursive directory scanner
- [ ] 9.6.3. Manual verification of the "Clean" UI
