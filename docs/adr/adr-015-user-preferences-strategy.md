# ADR 015: User Preferences Persistence Strategy

## Status
Accepted

## Context
The application needs to persist user-specific settings such as window size, position, and Most Recently Used (MRU) files/directories across sessions. Additionally, users expect their active workspace—including open tabs, split windows, and active filters—to be restored when the application restarts.

## Decision
We will use a JSON-based configuration file stored in platform-specific application data directories.

### Persistence Mechanism
- **Format**: JSON (using `kotlinx-serialization`) for its readability, standard support, and ease of use with Kotlin data classes.
- **Storage Locations**:
    - **macOS**: `~/Library/Application Support/com.logviewer.app/preferences.json`
    - **Windows**: `%APPDATA%\LogViewer\preferences.json`
    - **Linux**: `~/.config/logviewer/preferences.json` (following XDG Base Directory Specification)

### Key Data to Persist
- **Window State**: Width, height, X position, Y position.
- **MRU Files**: A list of the most recently opened log files.
- **MRU Directories**: A list of the most recently opened directories.
- **UI Preferences**: Theme (Dark/Light), Sidebar visibility.
- **UI Layout**: 
    - Full configuration of all open tabs (IDs, titles).
    - Configuration of split windows within each tab (log file paths, active filters, sort order).
    - The currently active tab and active window focus.

### Implementation Details
- A `UserPreferences` data class in the `domain` module.
- A `PreferencesRepository` in the `core` module responsible for I/O and path resolution.
- The `Main.kt` will load preferences on startup to configure the initial window state.
- `LogViewerViewModel` will load and restore the UI layout (tabs/splits) and trigger automatic log reloading.
- Preferences are saved automatically whenever the UI configuration changes (e.g., adding a tab, splitting a window, loading a file, changing a filter).

## Consequences
- **Pros**:
    - "Pick up where you left off" experience for users.
    - Native feel by following OS standards.
    - Easy to debug and manually edit settings if needed (JSON).
- **Cons**:
    - Preferences file size increases with the number of open tabs/splits.
    - Automatic log reloading on startup may increase initial load time if many large files were open.
