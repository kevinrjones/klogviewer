# ADR 015: User Preferences Persistence Strategy

## Status
Proposed

## Context
The application needs to persist user-specific settings such as window size, position, and Most Recently Used (MRU) files/directories across sessions. These settings should be stored in a way that respects standard operating system conventions for Mac, Windows, and Linux to ensure a native and reliable experience.

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
- **MRU Files**: A list of the most recently opened log files (limited to 5 for quick access, but storing more for the "Show All" feature).
- **MRU Directories**: A list of the most recently opened directories.
- **UI Preferences**: Theme (Dark/Light), Sidebar visibility.

### Implementation Details
- A `UserPreferences` data class in the `domain` module.
- A `PreferencesRepository` in the `core` module responsible for I/O and path resolution.
- The `Main.kt` will load preferences on startup to configure the initial window state.
- Preferences will be saved automatically on application close and optionally when key settings change.

## Consequences
- **Pros**:
    - Native feel by following OS standards.
    - Easy to debug and manually edit settings if needed (JSON).
    - Decoupled from the UI framework (Compose).
- **Cons**:
    - Requires manual handling of platform-specific paths.
    - JSON parsing adds a small overhead on startup.
