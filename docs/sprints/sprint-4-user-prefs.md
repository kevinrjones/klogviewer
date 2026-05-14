# Sprint 4: User Preferences

## 1. Goal
Enhance the user experience by providing persistent preferences and tracking recently used items for quick access.

## 2. Scope

### 2.1. User Preferences Persistence
- Persist application settings across sessions (e.g., last window size, position, active theme, default log parser settings).
- Save and restore the last opened workspace or tabs.


### 2.4. Recent Files/Workspaces
- Implement a "Recently Opened" section in the `File` menu.
- Track and display the last 5-10 opened files or directories.

## 3. Key Decisions
- **Persistence Mechanism**: Use a local configuration file (e.g., JSON via `PreferencesRepository`) to store user preferences.
- **MRU Tracking**: Maintain a history of recently opened files and directories for quick access.

## 4. Definition of Done
- [x] User preferences are saved and restored correctly on startup.
- [x] "Recently Opened" menu items are functional and updated correctly.
- [x] Application state (window size/position) persists across sessions.

## 5. Progress Log
- **2026-05-14 13:57**: Initialized Sprint 4 documentation and task list. Created branch `feature/user-preferences`.
