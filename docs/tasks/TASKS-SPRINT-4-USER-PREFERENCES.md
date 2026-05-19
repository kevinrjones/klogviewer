# TASKS: Sprint 4 - User Preferences

## 9. Sprint 4: User Preferences

### 9.1. Initialize Sprint 4 Documentation
- [x] 9.1.1. Create `docs/sprints/sprint-4-user-prefs.md`
- [x] 9.1.2. Create ADR for User Preferences Persistence Strategy

### 9.2. User Preferences Persistence
- [x] 9.2.1. Define `UserPreferences` data model (WindowState, MRU lists)
- [x] 9.2.2. Implement `PreferencesRepository` with platform-specific paths
- [x] 9.2.3. Integrate preferences into `KLogViewerViewModel`
- [x] 9.2.4. Save window state (size/position) on close and restore on startup
- [x] 9.2.5. Implement `kotlinx-serialization` for JSON-based storage

### 9.3. [MOVED] Recursive Directory Loading
- Moved to Sprint 5 (`TASKS-SPRINT-5-RECURSIVE-LOADING.md`)

### 9.4. [MOVED] UI Simplification ("Enema")
- Moved to Sprint 6 (`TASKS-SPRINT-6-UI-REDESIGN.md`)

### 9.5. Recent Files Menu
- [x] 9.5.1. Implement tracking of recently opened files/directories
- [x] 9.5.2. Update `MenuBar` to include "Recently Opened" sub-menu
- [x] 9.5.3. Implement intent to open a recently used item
- [x] 9.5.4. Implement "Show All" recent items dialog
- [x] 9.5.5. Hide non-existent items from recent list and offer cleanup

### 9.6. Verification & Polish
- [x] 9.6.1. Unit tests for `PreferencesRepository`
- [ ] 9.6.2. [MOVED] Unit tests for recursive directory scanner (Sprint 5)
- [ ] 9.6.3. [MOVED] Manual verification of the "Clean" UI (Sprint 6)
