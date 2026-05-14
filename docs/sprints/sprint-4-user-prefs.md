# Sprint 4: User Preferences & UI Refinement

## 1. Goal
Enhance the user experience by providing persistent preferences, improving directory-based log loading, and streamlining the UI for a more professional desktop-centric feel.

## 2. Scope

### 2.1. User Preferences Persistence
- Persist application settings across sessions (e.g., last window size, position, active theme, default log parser settings).
- Save and restore the last opened workspace or tabs.

### 2.2. Directory Log Loading
- Implement recursive directory watching and loading.
- Allow users to select a folder and automatically discover/interleave all log files within it and its subdirectories.

### 2.3. UI "Enema" (Refinement)
- Remove redundant log level selectors from the sidebar and navigation bar as requested.
- Streamline the UI layout to reduce clutter and focus on the log grid.
- Improve the visual hierarchy of the Ribbon Bar.

### 2.4. Recent Files/Workspaces
- Implement a "Recently Opened" section in the `File` menu.
- Track and display the last 5-10 opened files or directories.

## 3. Key Decisions
- **Persistence Mechanism**: Use a local configuration file (e.g., JSON or TOML in `~/.logviewer/config.json`) to store user preferences.
- **Recursive Watcher**: Use a dedicated `DirectoryLogSource` or extend `MergedLogSource` to handle dynamic file discovery in directories.
- **UI Simplification**: Consolidate filtering into the Ribbon Bar or a dedicated filter pane, removing legacy mobile-first sidebar elements.

## 4. Definition of Done
- [ ] User preferences are saved and restored correctly on startup.
- [ ] Selecting a directory loads and interleaves all log files found recursively.
- [ ] Log level controls are removed from the sidebar and nav bar, relocated if necessary.
- [ ] "Recently Opened" menu items are functional and updated correctly.
- [ ] The UI feels more "clean" and desktop-oriented.

## 5. Progress Log
- **2026-05-14 13:57**: Initialized Sprint 4 documentation and task list. Created branch `feature/user-preferences`.
