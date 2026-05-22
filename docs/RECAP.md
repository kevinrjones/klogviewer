# 2026-05-20
16:45

### Improved Remote Directory Monitoring and Visual Feedback

Refined the behavior of monitored directories when files are deleted and optimized the UI to prevent false-positive error states.

#### Changes:
- **Automatic Refresh**: Updated `KLogViewerViewModel` to automatically remove logs and source IDs from the window when a file is deleted from a monitored directory. This ensures the window accurately reflects the current contents of the directory.
- **Selective Log Removal**: Implemented a distinction between primary sources (manually opened files) and sub-sources (discovered via directory monitoring). Manually opened files preserve their logs even when missing (to maintain context), while directory sub-sources are refreshed cleanly.
- **Error State Suppression**: Suppressed global error states (red status bar) when individual files in a monitored directory are deleted. The error state is now only triggered if the primary path (the directory itself) becomes inaccessible.
- **UI Consistency**: 
    - Standardized the use of Red (critical error) and Orange (secondary warning) across the Tab, Window Header, and Status Bar.
    - Fixed a bug where the status bar incorrectly turned red for sub-file deletions.
- **Testing**:
    - Added `SftpFileRemovalRefreshTest.kt` to verify that logs and source IDs are correctly removed for directory sub-sources.
    - Added `DirectoryErrorSuppressionTest.kt` to verify that sub-file deletions do not trigger global error states.

15:45

### SFTP Persistence and Reliability Fixes

Fixed a critical bug where SFTP directories were not being re-opened correctly on application startup and ensured log parser settings are correctly persisted.

#### Changes:
- **SFTP Directory Reload**: Improved `SftpUri` to support an `isDirectory` flag via a query parameter (`?type=directory`). This allows the application to distinguish between remote files and remote directories when restoring the session from user preferences.
- **Persistence**: 
    - Added `parserName` to `WindowPreference` and `LogWindow` models.
    - Updated `KLogViewerViewModel` to correctly save and restore the selected log parser for both local and remote windows.
- **Dependency Injection**: 
    - Refactored `KLogViewerViewModel` to allow injection of `CoroutineDispatcher` and `SshClientProvider`.
    - Ensured that all SFTP log sources (file and directory) use the injected dependencies, improving testability and robustness.
- **Testing**: 
    - Fixed hanging/slow tests in `SftpLogSourceTest` by replacing `Dispatchers.Unconfined` with `Dispatchers.IO` for blocking SSH IO mocks.
    - Updated multiple integration tests (`SftpBrowsingTest`, `SftpPersistenceTest`, `SftpReloadTest`) to properly mock the SFTP source factory, preventing real SSH connection attempts during automated testing.
- **Robustness**: Ensured that `SftpDirectoryLogSource` correctly propagates the log parser and uses the shared connection pool during reload.

15:45

### SFTP Session Restoration and Multi-Source Interleaving Improvements

Finalized the fix for remote directory restoration and improved the reliability of multi-source windows.

#### Changes:
- **Directory Restoration**: Updated `loadFilesIntoWindow` to correctly dispatch SFTP directory URIs to `SftpDirectoryLogSource`, ensuring remote directories are re-opened as directories upon restart.
- **Persistence Fix**: Added missing `savePreferences()` calls when connecting to remote sources (file, directory, or multiple files). This ensures the window state is saved immediately when a connection is established.
- **Auto-Save Centralization**: Implemented a `saveSftpConnection` helper to ensure connection details (host, user, etc.) are automatically saved to the connection list even when connecting via the remote browser or directory selection.
- **Remote File Deletion Detection**: Enhanced `SftpDirectoryLogSource` to detect when files are removed from a monitored directory. Logs from missing sources are now visually marked with a red badge and strike-through text in the UI to provide clear feedback.
- **Double Tailing Prevention**: Implemented `filterRedundantPaths` to automatically remove sub-files from the load list if their parent directory is also being loaded, preventing redundant log streams and UI badges.
- **Multi-Source Interleaving**:
    - Refactored `handleLogUpdate` to support additive `Initial` updates. When multiple files or directories are loaded into one window, their initial content is merged instead of being overwritten by the last source to load.
    - Added automatic chronological sorting for windows with multiple sources, ensuring interleaved logs are always displayed in the correct order.
- **Quality Assurance**: 
    - Verified the fix with a new integration test for directory restoration.
    - Fixed regressions in `InterleavingIntegrationTest` caused by the lack of explicit sorting in the new additive update logic.
    - Optimized all SFTP-related tests to run instantaneously in virtual time using `runTest` and injected test dispatchers.

## 15:00

### SFTP Remote Directory Scaling and Reliability

Fixed a data loss bug during remote directory loading and optimized SSH connection usage.

#### Changes:
- **Connection Sharing**: Implemented a simple SSH connection pool in `SftpDirectoryLogSource`. It now shares connections (up to 8 sessions per connection) instead of opening a new SSH connection for every single log file, significantly reducing server overhead and avoiding session limits.
- **Data Integrity**: Fixed a bug where `Appended` updates received during the directory's initialization phase were being overwritten by the final `Initial` update. Now all logs received before initialization are correctly aggregated.
- **Robustness**: Updated `SftpFileSystem` to filter out `.` and `..` entries from remote directory listings.
- **Testing**: Added a comprehensive test case in `SftpDirectoryLogSourceTest` to verify log preservation during initialization.

Fixed critical bugs in SFTP log sources that caused logs to be cleared periodically and improved the accuracy of "missing file" indicators in the UI.

#### Changes:
- **SFTP Tailing**: 
    - Re-implemented the `SftpLogSource` loop to use a blocking `readLine()` on `Dispatchers.IO` after an initial non-blocking phase. This ensures reliable tailing while still allowing for fast initial loads of existing data.
- **Directory Monitoring**:
    - Fixed a bug in `SftpDirectoryLogSource` where logs were being cleared every 5 seconds due to redundant `Initial` update emissions during rescans.
    - Improved directory error reporting by emitting failures with the directory's own URI.
- **UI & State**:
    - Refined `KLogViewerScreen` to only show red/strike-through in the header if the primary path (the directory or file opened) is missing. Secondary failures (e.g., one file in a monitored directory) are now indicated with an Orange color without striking through the main title.
    - Updated `KLogViewerViewModel` to more reliably clear missing status when sources are initialized or logs are received.
- **Testing**: Verified fixes with `SftpDirectoryLogSourceTest` and `SftpLogSourceTest`.

## 14:00

### Remote Directory Monitoring Enhancements

Improved `SftpDirectoryLogSource` to automatically detect and add new files appearing in monitored remote directories.

#### Changes:
- **Core**:
    - Refactored `SftpDirectoryLogSource` to support a `LogSource` factory, enabling isolated unit testing.
    - Verified that new files discovered during remote directory rescans are automatically tailed and their content is appended to the active log view.
- **Testing**:
    - Added `SftpDirectoryLogSourceTest.kt` with a comprehensive test case for dynamic file discovery and log appending.
- **Robustness**: Ensures that long-running remote monitoring sessions remain up-to-date as new log files (e.g., rotated logs or new service instances) are created.

## 13:45
 
 ### Toolbar Disconnect/Reconnect and Connection State Persistence

### Toolbar Disconnect/Reconnect and Connection State Persistence

Added a disconnect/reconnect button to the toolbar and implemented connection state persistence.

#### Changes:
- **Connection Management**: Added `isConnected` state to `LogWindow` and persisted it in `UserPreferences`.
- **Toolbar UI**: Integrated a new toggle button in `FilterBar` using `Link` and `LinkOff` icons to represent connection status.
- **Visual Feedback**:
    - Disconnected windows now show a subtle background highlight in the header.
    - The Status Bar turns Gray and appends "(Disconnected)" to the path when a window is disconnected.
- **ViewModel Logic**:
    - `ToggleConnection` intent cancels active log observation jobs when disconnecting.
    - Reconnecting restarts log observation using stored source IDs and parsers.
    - Application startup respects the `isConnected` state, only reloading logs for windows marked as connected.
- **Testing**: Added `ConnectionToggleTest.kt` to verify state transitions, job management, and preference persistence.

## 13:10

### SFTP Session Restoration and Missing File Indicators

Implemented automatic reloading of SFTP log sources on application startup and added visual indicators for missing or failed log sources.

#### Changes:
- **SFTP Restoration**: Updated `KLogViewerViewModel` to parse SFTP URIs and match them against saved configurations during initialization. This allows previously open remote log streams to be restored automatically.
- **Missing File Indicators**: 
    - Enhanced `LogFailure` with `sourceId` to track which specific source failed to load.
    - Updated `KLogViewerViewModel` to populate `missingSourceIds` when a `LogFailure` occurs.
    - Ensured that both local and remote files that fail to load are marked in the UI with a red bar and strike-through text decoration.
- **MergedLogSource**: Refactored to be more generic, accepting a list of flows, and improved robustness to proceed with partial results if some sources fail during initial merge.
- **Verification**: Added `SftpReloadTest.kt` integration test and confirmed that failed remote connections are correctly signaled in the application state.

## 12:45

### Robust SFTP Connections: Retry Mechanism and Staggered Loading

Addressed `TransportException`s during SFTP connections by implementing a retry mechanism and optimizing simultaneous connection attempts.

#### Changes:
- **Core Utility**: Created `RetryUtils.kt` providing a generic `withRetry` helper with exponential backoff for suspend functions.
- **SFTP Stability**: Updated `SftpLogSource` and `SftpFileSystem` to use the retry mechanism for initial SSH connections and authentication. This mitigates transient network issues and server-side rate limiting (e.g., `Connection reset` or `Server closed connection during identification exchange`).
- **Directory Loading Optimization**: Added a staggered delay (200ms per file) when discovering multiple files in `SftpDirectoryLogSource`. This prevents hammering the SSH server with dozens of simultaneous connection requests, which previously triggered server-side safety limits.
- **Verification**: Verified the implementation with new `RetryUtilsTest` and confirmed that existing SFTP unit and integration tests continue to pass.

# 2026-05-19

## 14:05

### Code Review: Exhaustiveness and Testing Cleanup

Addressed critical feedback from code review to improve build reliability and test maintainability.

#### Changes:
- **UI Code**: Fixed non-exhaustive `when` expressions and statements in `KLogViewerScreen.kt` by adding `else` branches. This ensures the application compiles correctly in environments with stricter Kotlin compiler settings for enums.
- **UI Tests**: Refactored `KLogViewerComplexUiTest.kt` to remove redundant `@OptIn(ExperimentalTestApi::class)` annotations. Since the class was already annotated, method-level annotations were unnecessary and cluttered the code.
- **Verification**: Executed the `:ui:desktopTest` suite to confirm that the changes did not introduce regressions and that all UI tests continue to pass.

# 2026-05-12

## 11:38

### Recap of Sprint 1: Walking Skeleton Completion

The first phase of the KLogViewer project has been successfully completed, establishing a "Walking Skeleton" that proves the end-to-end flow from reading a log file to displaying it in a reactive UI.

#### Core Achievements:
- **Architecture & Infrastructure**: 
    - Established a layered multi-module project structure (`domain`, `core`, `ui`, `app`) using Gradle Kotlin DSL and a Version Catalog for dependency management.
    - Implemented four key Architectural Decision Records (ADRs) covering Multi-Module architecture, Functional Error Handling with Arrow `Either`, UI architecture with MVI, and the use of Tiny Types.
- **Domain & Business Logic**:
    - Defined core domain models (`LogEntry`, `LogLevel`) using Tiny Types (`LogFilePath`, `LogTimestamp`, `LogContent`) to ensure domain integrity and type safety.
    - Implemented a `SimpleLogParser` for standard Log4j-like text formats and a `LogService` for functional file loading.
- **UI Layer**:
    - Built a predictable UI using the MVI (Model-View-Intent) pattern with Compose for Desktop.
    - Created a `KLogViewerViewModel` that manages immutable state and processes user intents (loading files, clearing logs).
    - Developed reactive UI components with support for log level color coding (e.g., Blue for INFO, Red for ERROR).
- **Quality Assurance**:
    - Applied TDD to the log parsing logic using JUnit 5 and Strikt.
    - Established a BDD (Behavior Driven Development) suite using Cucumber JVM to verify the end-to-end user story of loading and viewing log files.
- **Documentation**:
    - Initialized `docs/TASKS.md` for status tracking and `docs/project_memory.md` for long-term project context.

## 12:08

### UI Enhancement: File Browsing Support

Implemented native file browsing capability to allow users to easily select log files from their local disk.

#### Changes:
- **UI Components**:
    - Added a "Browse" button to the `FileSelector` header.
    - Integrated `java.awt.FileDialog` using Compose's `AwtWindow` to provide a native file selection experience.
- **MVI Architecture**:
    - Added `KLogViewerIntent.SelectPath` to handle file selection without immediate loading, keeping the UI state in sync.
    - Updated `KLogViewerViewModel` to handle the new intent and update the `filePath` in the state.
- **Maintenance**:
    - Updated `TASKS.md` and `project_memory.md` to reflect the completion of this feature.

## 12:55

### Sprint 2: UI/UX Refinement Completion

Transformed the application from a "Walking Skeleton" into a professional-grade log viewer with a custom "Command-Line Chic" identity.

#### Core Achievements:
- **Visual Identity**: Implemented a custom theme with Industrial Dark and Clean Light palettes.
- **Enhanced Layout**: Introduced a collapsible left sidebar for filters and a persistent bottom status bar for file metadata.
- **Analysis Tools**: Implemented real-time log level filtering and text search with background processing.
- **Intelligent Highlighting**: Developed a regex-based highlighting engine for IDs, IP addresses, and timestamps.
- **Line Numbering**: Added a dedicated gutter for line numbers.

#### Technical Improvements:
- Optimized UI responsiveness by moving filtering logic to `Dispatchers.Default`.
- Added unit tests for the highlighting engine.
- Integrated `material-icons-extended` for enhanced visual feedback.

## 12:15

### Sprint Documentation Update

Finalized Sprint 1 by updating all tracking documents to reflect its completion and the additional architectural and UI improvements.

#### Changes:
- **Sprint Document**: Updated `docs/sprints/sprint-1-walking-skeleton.md` to "Completed" status, marking all tasks and definition of done items.
- **Project Memory**: Enhanced `docs/project_memory.md` with detailed sections for the Streaming LogSource architecture deepening and the File Browsing UI enhancement.
- **Status Alignment**: Ensured `TASKS.md` and project history are fully synchronized with the actual implementation state.


# 2026-05-14

## 08:11

### Desktop Transition, Roadmap Definition, and Multi-Log Support

Completed the analysis and planning phase for the desktop transition and implemented core multi-log management features.

#### Analysis & Roadmap:
- **SOTA & Gap Analysis**: Conducted an industry review (LogViewPlus, Tailviewer, Sematext) and established a gap analysis in `docs/FEATURES.md`. Defined requirements for a professional desktop UI, including ribbon bars and high-density grids.
- **Product Roadmap**: Defined the evolution of KLogViewer through Sprints 4-8. Created comprehensive sprint plans (`docs/sprints/sprint-4.md` to `sprint-8.md`) and architectural blueprints via 5 new ADRs (`adr-009` to `adr-013`) covering structured data, connectivity, visualization, persistence, and extensibility.
- **Architectural Shift**: Authored `adr-008-desktop-centric-ui.md` to guide the move from mobile Material Design to a multi-pane, ribbon-based desktop workspace.

#### Core Implementation (Sprint 3):
- **Tabbed Interface Architecture**: Refactored the UI from a single-file model to a multi-tab workspace. Implemented `TabState` to track independent logs, filters, and search queries per tab.
- **Multi-Log Interleaving**: Developed `MergedLogSource` to chronologically interleave entries from multiple log files, providing a unified view of distributed system events.
- **UI & UX**:
    - Added a native `TabRow` for workspace navigation.
    - Enhanced the log list with source identification badges.
    - Updated `KLogViewerViewModel` to manage concurrent loading jobs and tab lifecycle.
- **Quality Assurance**: Verified the interleaving logic with unit tests (`MergedLogSourceTest`) and ensured regression safety by updating the BDD integration suite.

## 09:10

### Sprint 3 Finalization: Real-time Support and Desktop UI Polish

Completed the missing pieces of the professional desktop experience by implementing real-time log tailing and refining the grid UI with column headers.

#### Core Achievements:
- **Local File Tailing**: Enhanced `FileLogSource` to watch for local file changes. Implemented an efficient polling mechanism that detects appends and truncations, emitting reactive `LogUpdate` events.
- **Real-time Multi-Log Support**: Refactored `MergedLogSource` to use `channelFlow`, enabling concurrent streaming from multiple log sources. Appends from any source are now unified in the interleaved view in real-time.
- **Professional Grid UI**: Added visible column headers (Line #, Timestamp, Level, Message) to the `LogList` component, fulfilling the "High-Density Grid" requirement derived from the SOTA review.
- **UI Bug Fixes**: Corrected a layout issue in `KLogViewerScreen` where the main content area was not correctly utilizing remaining space in the `Row` when the sidebar was expanded.

#### Quality Assurance:
- **New Unit Tests**: Implemented `FileLogTailingTest` to verify that file appends are correctly detected and emitted.
- **Integration Safety**: Verified that existing tab management and interleaving tests remain passing with the new streaming architecture.

## 11:35

### Window Management and UI Refinement

Adjusted the application's initial presentation to align with desktop-centric standards.

#### Changes:
- **Window Configuration**: Set the default window size to 1200x800 and enabled centering on startup via `rememberWindowState`.
- **UX**: Improved the initial impression of the application, ensuring that the high-density grid and sidebar have sufficient space immediately upon launch.

## 11:45

### Bug Fix: Log Parsing for App Logs

Resolved an issue where the application failed to parse its own log files due to a strict regex.

#### Changes:
- **Parser Robustness**: Updated `SimpleLogParser` with a flexible regex that supports milliseconds and the `[THREAD] LEVEL` format used by Logback.
- **Backwards Compatibility**: Maintained support for the simpler Sprint 1 log format.
- **TDD**: Added a regression test with actual application log samples.

## 12:45

### Feature: Reverse Order View

Added the ability to view log files in reverse order (newest entries at the top).

#### Changes:
- **MVI State**: Added `isReversed` flag to `TabState`.
- **Sorting Logic**: Updated `KLogViewerViewModel` to apply `reversed()` to filtered logs when the toggle is active.
- **UI**: Added a "Reverse Order" toggle button to the `RibbonBar` under the "View" group.
- **Persistence**: The sort order is maintained per tab and respected when new logs are appended in real-time.

## 14:30

### Feature: User Preferences and MRU tracking

Implemented a persistent preference system that respects OS conventions and tracks recently used items.

#### Changes:
- **Persistence**: Added `PreferencesRepository` using `kotlinx-serialization` to save/load JSON config from platform-specific app data folders.
- **Window Management**: The application now restores its previous size, position, and maximization state on startup.
- **MRU Menu**: Added a "Recently Opened" sub-menu to the File menu, showing up to 5 files and 5 directories.
- **Recent Items Dialog**: Implemented a "More..." option that opens a dialog listing all historical recent items.
- **Cross-Platform**: Designed for macOS, Windows, and Linux using standard paths like `Library/Application Support`, `%APPDATA%`, and `~/.config`.

## 21:30

### Log Format Analysis & Sprint 7 Planning

Conducted a gap analysis of logging format support and established a new sprint to address these gaps.

#### Core Achievements:
- **Gap Analysis**: Documented limitations in `SimpleLogParser` regarding level mapping, timestamps, multiline entries, and structured data in `docs/LOGGING-FORMATS-GAP-ANALYSIS.md`.
- **Sprint 7 Definition**: Created `docs/sprints/sprint-7-advanced-log-formats.md` to implement `LevelMapper`, `ParserRegistry` (Templates), `MultilineProcessor`, and `JsonLogParser`.
- **Roadmap Refinement**: Renumbered future sprints (8-12) to integrate the new logging format priorities into the long-term plan.

## 21:40

### Advanced Logging Architecture (ADRs)

Formalized the architectural decisions for the next phase of log parsing.

#### Core Achievements:
- **ADR 019: Template-based Parsing**: Decision to use a `ParserRegistry` with regex templates, `LevelMapper`, and heuristic auto-detection.
- **ADR 020: Multiline Aggregation**: Decision to implement a `MultilineProcessor` at the ingestion layer for stack trace support.
- **Traceability**: Linked Sprint 7 technical tasks to these new architectural foundations.


# 2026-05-15

## 07:15

### Core: Flexible Level Mapping Implementation

Implemented a robust `LevelMapper` to normalize diverse log level formats from various logging frameworks.

#### Core Achievements:
- **Normalization**: Supports abbreviations (DBUG, INF, WRN, ERR, FTL), alternative terminology (TRACE, INFORMATION, WARNING, SEVERE, CRITICAL), and numeric Syslog levels (0-7).
- **Flexibility**: Strip common wrappers like brackets `[]` and parentheses `()` automatically.
- **Integration**: Integrated into `SimpleLogParser` to ensure consistent color-coding and filtering across all strategies.

## 07:35

### Core: Parsing Robustness (Timezones and Metadata)

Enhanced parsing logic to handle complex log headers and ensure reliable sorting.

#### Core Achievements:
- **Timezone Support**: Added support for ISO8601 offsets (e.g., `+01:00`) in `TimestampParser`.
- **Metadata Fallback**: Improved `TemplateLogParser` to identify log levels even when preceded by optional metadata like thread names or category tags.
- **Stability**: Resolved a race condition in `LogSortingTest` to ensure predictable integration test results.

## 07:45

### UI Refinement: Split Window Header Enhancements

Improved workspace context by ensuring file paths are always visible in multi-log environments.

#### Changes:
- **Visibility**: Updated `LogWindow` headers to display the fully qualified file path, ensuring users can distinguish between similar logs in different splits.
- **Layout**: Optimized header space by left-aligning the path and right-aligning window controls.

## 08:55

### Sprint 7 Core: Advanced Log Parsing Implementation

Successfully implemented the foundational components for advanced log parsing, significantly expanding the range of supported log formats.

#### Core Achievements:
- **Flexible Level Normalization**: Enhanced `LevelMapper` to support abbreviated names (`INF`, `WRN`, `ERR`), alternative terminology (`VERBOSE`, `NOTICE`, `SEVERE`), and numeric levels (Syslog RFC 5424).
- **Timezone & Epoch Support**: Improved `TimestampParser` to handle timezone offsets and Unix Epoch timestamps (seconds and milliseconds), providing robust time parsing for cloud and legacy logs.
- **Pluggable Parser Strategy**: 
    - Implemented `ParserRegistry` with default templates for `ISO8601`, `Apache`, `Syslog`, and `CSV`.
    - Developed `LogfmtParser` for structured key-value log formats common in the Go/Heroku ecosystem.
- **Heuristic Auto-Detection**: Enhanced `HeuristicProbe` to automatically detect `logfmt` and JSON formats, and select the best template from the registry based on file content.
- **Multiline Aggregation**: Integrated `MultilineProcessor` to correctly group stack traces and indented content with their parent log entries.

## 09:35

### UI Refinement: Resizable Columns and Persistence

Enhanced the log viewing experience by allowing users to interactively resize column widths, with full persistence across sessions.

#### Core Achievements:
- **Interactive Resizing**: Implemented drag-to-resize functionality for the `LogList` grid. Users can now adjust column widths by dragging header edges, with visual cursor feedback.
- **Dynamic Width Management**: Transitioned from fixed/flexible column widths to a state-driven model where `LogWindow` tracks specific widths for each column.
- **Workspace Persistence**: Extended the user preference system to save and restore custom column widths per window, ensuring the user's workspace layout is preserved across application restarts.
- **MVI Architecture**: Integrated the resizing logic into the MVI flow via a new `UpdateColumnWidth` intent, ensuring clean state propagation and persistence triggers.
- **UI UX Polish**: Added minimum width constraints to prevent column disappearance and ensured perfect alignment between headers and log entry rows.

## 11:35

### Feature: Dynamic Log Entry Details

Redesigned the details pane to automatically adapt to the structure of the selected log entry.

#### Core Achievements:
- **Adaptive Layout**: Automatically displays all custom fields from structured logs (JSON, Logfmt, Apache).
- **Smart Visibility**: Conditionally hides the "Level" field when unknown and the "Content" section when empty to maximize vertical space.
- **Highlighting**: Integrated the regex highlighting engine into the details view for visual consistency with the main grid.

## 12:10

### UI Refinement: Auto-expanding Last Column

Improved content visibility by allowing the "Message" column to naturally expand to fit long log lines in the horizontal scroll view.

#### Changes:
- **Dynamic Sizing**: Transitioned to `widthIn(min = defaultWidth)` for the last column, allowing it to grow based on content while respecting manual user overrides.
- **Full-Width Coverage**: Updated header and row layout constraints to ensure they share the width of the widest visible entry.

## 13:10

### UI Refinement: Level Name Preservation

Updated the UI to prioritize the raw level string from the log file while maintaining normalized color-coding.

#### Changes:
- **Fidelity**: Stored the exact level string (e.g., `INF`, `DBUG`, `[INF]`) in the entry fields to ensure the UI remains faithful to the original source.
- **Contextual Display**: Maintained bracketed display in the grid for visual structure while showing clean names in the details pane.

## 13:55

### Domain Documentation: Ubiquitous Language Formalization

Established a formal "Ubiquitous Language" to ensure terminological consistency between the business domain, documentation, and codebase.

#### Core Achievements:
- **Centralization**: Created `docs/UBIQUITOUS_LANGUAGE.md` as the single source of truth for project terminology.
- **Architecture**: Formalized this requirement via `ADR-022`, mandating that all new code and documentation align with the established language.

## 14:29

### UI Robustness: Large Log Entry Handling

Implemented safety measures to prevent application crashes when loading log files with extremely long lines or humongous entries.

#### Core Achievements:
- **Constraint Capping**: Added a 10,000.dp maximum width constraint to the log message column in `LogList`. This prevents Compose's `Constraints` bit-packing from overflowing when rendered inside a horizontal scroll area.
- **Intelligent Truncation**:
    - Truncated log messages to 10,000 characters in the `LogList` view for improved performance and layout stability.
    - Truncated log content to 50,000 characters in the `LogEntryDetails` pane to prevent vertical size overflow crashes while still providing ample detail.
    - Truncated general metadata fields to 10,000 characters in the details pane.
- **Resize Protection**: Capped manual column resizing to a maximum of 10,000.dp to ensure the UI remains within safe measurement limits.
- **Performance Optimization**: Reduced the overhead of running complex regex highlighting on extremely large strings by applying truncation before processing.

## 15:15

### Feature: Multi-Log Visual Differentiation

Implemented visual indicators to help users distinguish between different log files when they are interleaved in the same window.

#### Core Achievements:
- **Source Badges**: Added colored dots in the log grid's gutter to identify the source of each entry.
- **Background Shading**: Implemented alternating pale grey backgrounds for log entries based on their source, facilitating visual grouping during interleaving.
- **Dynamic Gutter**: The gutter width now adapts automatically (50dp to 60dp) to accommodate badges only when multiple sources are present, maximizing screen space for single-file views.
- **ID Consistency**: Updated `FileLogSource` to use full file paths as `sourceId`, ensuring reliable mapping between entries and UI-defined sources even when files have identical names.

## 15:40

### UI Refinement: Full-Width Log Entry Backgrounds

Fixed a layout issue where log entry background colors and selection highlights would only cover the text area rather than extending to the full width of the window/scrollable area.

#### Changes:
- **LogList.kt**: Applied `Modifier.fillMaxWidth()` to the `LogEntryRow` container and its internal `Row`.
- **LogList.kt**: Implemented `Modifier.weight(1f)` for the final column ("Message") to ensure it occupies all remaining horizontal space when not manually resized, aligning its behavior with the header.

## 15:55

### Fix: Message Column Visibility Regression

Resolved a regression where the "Message" column would disappear when the total width of columns exceeded the viewport width.

#### Changes:
- **LogList.kt**: Introduced `BoxWithConstraints` to obtain the viewport width (`maxWidth`).
- **LogList.kt**: Replaced `Modifier.fillMaxWidth()` with `Modifier.widthIn(min = viewportWidth)` on rows and headers. This allows items to be at least as wide as the viewport while still growing to accommodate overflowing content.
- **LogList.kt**: Replaced `Modifier.weight(1f)` on the last column with a dynamic `widthIn(min = minWidth)` calculation.

## 16:15

### UI Refinement: Synchronized Row Widths for Full-Width Backgrounds

Ensured that all log rows stretch to the width of the widest visible row, providing consistent background color and selection highlight coverage across the entire horizontal scrollable area.

#### Changes:
- **LogList.kt**: Implemented `widestRowWidth` state in `LogList` to track the maximum width among composed log entries.
- **LogList.kt**: Applied `onSizeChanged` to each `LogEntryRow` to dynamically update the shared `widestRowWidth`.
- **Visual Consistency**: This ensures that when a long message extends the horizontal scroll range, shorter rows grow to match, filling the background colors to the edge.

## 15:15

### Feature: Automatic Scrolling (Tail)

Implemented automatic scrolling to the end of the log list when new entries are added, controlled by an "Auto-scroll" toggle in the toolbar.

#### Changes:
- **KLogViewerState.kt**: Added `isAutoScrollEnabled` flag to `LogWindow` (default: true).
- **KLogViewerIntent.kt**: Added `ToggleAutoScroll` intent.
- **UserPreferences.kt**: Updated `WindowPreference` to persist the auto-scroll state.
- **LogList.kt**: Added a `LaunchedEffect` that monitors `logs.size` and calls `verticalScrollState.scrollToItem(logs.size - 1)` when auto-scroll is enabled.

## 17:30

### Project Renaming: KLogViewer

Successfully renamed the project from LogViewer to KLogViewer across the entire codebase, build system, and documentation.

#### Core Achievements:
- **Refactoring**: Renamed packages from `com.logviewer` to `com.klogviewer` across all modules.
- **Component Renaming**: Updated core classes including `LogViewerViewModel`, `LogViewerState`, `LogViewerIntent`, and `LogViewerTheme` to their `KLogViewer` counterparts.
- **UI & Branding**: Updated the application window title and internal log messages to reflect the new name.
- **Consistency**: Renamed internal JSON log files to `klogviewer.json` for alignment.

## 17:40

### Documentation: README Spruce-up

Created a professional and informative `README.md` to improve project visibility and developer onboarding.

#### Core Achievements:
- **Branding**: Added professional badges for Kotlin, Compose for Desktop, and MIT License.
- **Feature Highlights**: Documented core capabilities including Multiple Tabs, Horizontal Split Panes, Interleaved Log Streams, and Advanced Heuristic Parsing.
- **Onboarding**: Provided a comprehensive technology stack overview and a "Getting Started" guide for both running and packaging the application.

## 18:05

### Maintenance: Library Upgrade Compilation Fixes

Resolved compilation errors and test regressions following the upgrade to Kotlin 2.3.21 and Compose 1.11.0.

#### Core Achievements:
- **API Migration**: Replaced deprecated `rememberRipple()` with the new `ripple()` API in `LogList.kt`.
- **AWT Integration**: Updated `AwtWindow` imports to its new location in `androidx.compose.ui.awt`.
- **Test Restoration**: Re-implemented BDD step definitions in `LogLoadingSteps.kt` using `UnconfinedTestDispatcher` and `backgroundScope`.
- **Dependencies**: Added `kotlinx-coroutines-test` to the version catalog and test dependencies.

## 22:15

### UI Enhancement: FQN in Recently Opened List

Improved the "Recently Opened" menu to provide better context by displaying the full path of files and directories.

#### Changes:
- **Main.kt**: Updated the `MenuBar` implementation to use the raw file path instead of the simple filename for "Recent Files" and "Recent Directories" items.
- **Consistency**: Verified that the `StatusBar` and `RecentItemsDialog` already provided full path information.

## 22:30

### CI/CD: Automated Multi-Platform Packaging

Integrated GitHub Actions to automate the build, test, and distribution process for KLogViewer.

#### Core Achievements:
- **Automated Workflow**: Created `.github/workflows/build.yml` using a matrix strategy to target macOS, Windows, and Linux.
- **Multi-Platform Installers**: Configured automated generation of `.dmg` (macOS), `.msi` (Windows), and `.deb` (Linux) installers.
- **Standalone Distribution**: Implemented tasks to create and upload zipped standalone executables for all three platforms.
- **Documentation**: Updated `README.md` with build status badges and direct links to GitHub Actions artifacts.


# 2026-05-16

## 08:36

### Project Documentation: RECAP Maintenance

Performed a comprehensive recap of all project activities from the last entry and reorganized the `RECAP.md` file into a strict chronological order.

#### Core Achievements:
- **Audit**: Reviewed all recent git commits and `project_memory.md` entries to ensure no tasks were missed in the project history.
- **Refactoring**: Reorganized the `RECAP.md` file to correct chronological sequencing of entries from May 12th to May 16th.
- **Traceability**: Captured key technical decisions and achievements for Project Renaming, README enhancement, Library upgrades, and CI/CD integration.


# 2026-05-17

## 17:00

### Feature: Keyboard Shortcuts and Multi-selection

Added standard keyboard shortcuts and multi-selection support for improved efficiency and better clipboard integration.

#### Changes:
- **Main.kt**: Added shortcuts to `MenuBar`: Cmd+W (Close Tab), Cmd+N (New Tab), Cmd+C (Copy).
- **KLogViewerIntent.kt**: Added `CopySelected` and `ToggleEntrySelection` intents.
- **LogWindow**: Added `selectedIndices` and `lastSelectedIndex` to track multi-select state.
- **KLogViewerViewModel.kt**: Implemented range selection and toggle selection logic, plus clipboard integration using AWT.
- **LogList.kt**: Updated `LogEntryRow` to detect Shift and Cmd/Ctrl modifiers on click using `pointerInput`.
- **Testing**: Added integration test for multi-selection logic in `TabManagementTest`.

## 17:15

### Fix: ScrollableTabRow IndexOutOfBoundsException (Refined)

Resolved a persistent crash in the tab bar that occurred when adding new tabs, which was caused by an out-of-sync recomposition in Compose's `ScrollableTabRow`.

#### Changes:
- **KLogViewerScreen.kt**: Implemented a custom `indicator` lambda for `ScrollableTabRow` that performs explicit bounds checking against the `tabPositions` list before accessing the `selectedTabIndex`.
- **KLogViewerScreen.kt**: Added explicit import for `androidx.compose.material.TabRowDefaults.tabIndicatorOffset`.
- **Robustness**: Maintained defensive `coerceIn` logic for `selectedTabIndex` to ensure it always stays within the valid range of the current tab list, providing double-layered protection.

## 17:45

### Fix: Split Pane Column Resizing

Fixed an issue where resizing a column in a split pane would incorrectly resize the column in the pane that currently has focus, rather than the one being interacted with.

#### Changes:
- **KLogViewerIntent.kt**: Extended `UpdateColumnWidth` intent to include `windowId`.
- **KLogViewerState.kt**: Added `updateWindow(windowId, block)` helper to support targeted window updates across any tab.
- **KLogViewerScreen.kt**: Updated the `onColumnResize` callback to pass the specific window's ID to the ViewModel.
- **KLogViewerViewModel.kt**: Refactored the `UpdateColumnWidth` handler to use the new `updateWindow` helper for precise state modification.
- **Testing**: Added an integration test in `TabManagementTest` to verify independent column resizing in multi-split layouts.
- **Robustness**: Updated `RECAP.md` and `project_memory.md` to document the fix.

## 18:00

### Feature: Add 'All' option for Level Filtering

Added an 'All' option to the log level filters in the sidebar, allowing users to enable or disable all levels with a single click.

#### Changes:
- **KLogViewerIntent.kt**: Added `ToggleAllLevels` intent.
- **KLogViewerViewModel.kt**: Implemented logic to bulk toggle levels.
- **Sidebar.kt**: Added "All" checkbox at the top of the level list with a total log count.
- **KLogViewerScreen.kt**: Wired the new intent to the sidebar UI.
- **Testing**: Added integration test `should toggle all levels at once` to `TabManagementTest.kt`.

## 13:00

### Fix: UI Test Failures and Dialog Logic

Resolved persistent UI test failures (timeouts and assertions) on both local and CI environments, while improving the robustness of the dialog handling logic.

#### Changes:
- **KLogViewerUiTest.kt**: Switched from hardcoded non-existent paths to `File.createTempFile` for reliable test execution.
- **KLogViewerComplexUiTest.kt**: Implemented temporary file usage to satisfy file existence checks during multi-selection tests.
- **KLogViewerScreen.kt**: Refactored dialog handling from `SideEffect` to `LaunchedEffect(pendingDialog)`, ensuring actions are triggered once per state change and are better integrated with the Compose lifecycle.
- **Cleanup**: Removed redundant `else` branches in exhaustive `when` expressions for `DialogType`.
- **Verification**: Confirmed that `:ui:desktopTest` now passes successfully (14 tests completed).

## 18:15

### UI: Enhance Active Window Visibility

Improved the visibility of the active window in split-pane mode by adding a subtle left border.

#### Changes:
- **KLogViewerScreen.kt**: Added a 3dp left border to the active window using `Modifier.drawBehind`.
- **KLogViewerScreen.kt**: Added necessary imports for `drawBehind` and `Offset`.
- **Visuals**: The border uses `MaterialTheme.colors.primary` with 50% alpha for a subtle yet distinct indicator.
- **Logic**: The border only appears when a tab contains more than one window (i.e., when splits are active).

## 18:45

### UI: Refine Window Activation and Selection

Refined the behavior of clicking on log entries in a split-pane view to decouple window activation from entry selection, preventing accidental opening of the detail panel.

#### Changes:
- **KLogViewerScreen.kt**: Modified the `onEntryClick` callback for `LogList` to perform bounds checking on the window's active state.
- **Logic**: If a window is not active, the first click on any log entry now only activates that window (`SwitchWindow`).
- **Logic**: If the window is already active, clicking a log entry triggers the selection intent (`SelectEntry`) and opens the detail panel.
- **UX**: This change ensures that users can switch focus between split panes without losing their place or being interrupted by a detail panel they didn't intend to open.

## 19:15

### Feature: ANSI SGR Color Support

Added support for parsing and displaying ANSI SGR color codes in log files, with a UI toggle in the Filter Bar.

#### Changes:
- **LogHighlighter.kt**: Implemented stateful ANSI parser using `AnnotatedString.Builder`. Supports foreground colors, bright colors, bold, and reset.
- **KLogViewerState.kt**: Added `showAnsiColors` flag to `LogWindow`.
- **KLogViewerIntent.kt**: Added `ToggleAnsiColors` intent.
- **FilterBar.kt**: Added a `Palette` icon to toggle ANSI colors.
- **UserPreferences.kt**: Added `showAnsiColors` to `WindowPreference` for persistence.
- **LogList.kt / LogEntryDetails.kt**: Passed the flag down to the highlighter.
- **Testing**: Added unit tests in `LogHighlighterTest.kt` to verify parsing and stripping of ANSI codes.

## 19:30

### Planning: UI Testing Spike

Initiated a spike to establish a formal UI testing strategy for KLogViewer, bridging the gap between ViewModel-level verification and full end-to-end UI testing.

#### Core Achievements:
- **Branch Creation**: Created `spike/uitesting` branch for focused experimentation.
- **Sprint Definition**: Authored `docs/sprints/sprint-ui-testing-spike.md` detailing the roadmap from no UI testing to full coverage.
- **Task Management**: Established `docs/tasks/TASKS-UI-TESTING-SPIKE.md` to track research, infrastructure setup, and pattern implementation.
- **Testing Roadmap**: Defined a 4-phase plan covering Infrastructure, the Robot Pattern, Side-Effect handling, and CI/CD integration.

# 2026-05-18

## 07:13

### Recap of UI Testing Spike Initiation and Workspace Enhancements

Performed a comprehensive recap of the work completed during the recent transition to the UI Testing Spike and the final polish of the Sprint 6 workspace features.

#### Key Achievements:
- **UI Testing Framework Selection**: Committed to using **ComposeTestRule** and the **Robot Pattern** for the next phase of development.
- **Branch Infrastructure**: Established the `spike/uitesting` branch and corresponding roadmap documentation.
- **Workspace Interactivity**: Finalized a suite of power-user features including multi-selection, keyboard shortcuts, and bulk filtering.
- **Visual & UX Refinement**: Enhanced active window identification and refined split-pane interaction logic for better productivity.
- **Legacy Log Support**: Added support for ANSI SGR colors, ensuring KLogViewer can handle terminal-originated logs gracefully.
- **Traceability**: All recent changes have been documented in `project_memory.md` and the task lists are up to date.

# 2026-05-19

## 11:45

### Sprint 5 Completion and Source Badging

Finalized Sprint 5 by ensuring complete directory loading support and enhancing the UI with descriptive source badges for interleaved logs.

#### Core Achievements:
- **Directory Loading**: Fully integrated `DirectoryLogSource` into the application, enabling recursive log file discovery and real-time merging of directory contents.
- **Refined Source Badging**: 
    - Replaced textual badges with small color-coded circles to minimize gutter width.
    - Integrated `TooltipWrapper` to display the fully qualified file name (e.g., `/var/log/app.log`) when hovering over the source circle.
    - Updated the `KLogViewerViewModel` to dynamically track and expose all discovered `sourceIds` to the UI, ensuring badges appear correctly even as new files are added to a watched directory.
- **UI & Workspace Management**:
    - Added "Open Directory..." to the main menu and integrated it with native directory selection dialogs.
    - Updated "Recently Opened" to distinguish between files and directories.
- **JSON Support Alignment**: Verified that `HeuristicProbe` correctly identifies JSON schemas and maps keys even when loading from directories.

#### Quality Assurance:
- **Integration Testing**: Verified the dynamic source ID tracking and badge rendering logic through the `InterleavingIntegrationTest` and manual review.
- **Test Coverage**: Ensured all core tests for recursive scanning and merging remain passing.

## 13:45

### Fix: Intermittent Test Failures and Resource Leaks

Resolved intermittent integration test failures (race conditions and IOExceptions) across Ubuntu and Windows CI runners while improving overall test robustness.

#### Changes:
- **ViewModel**: Added `KLogViewerViewModel.clear()` to cancel its internal coroutine scope, ensuring that background tasks (like file tailing) are stopped when tests or components are disposed.
- **Integration Tests**: Updated `TabManagementTest`, `PersistenceIntegrationTest`, `RecentItemsTest`, `LogSortingTest`, and `InterleavingIntegrationTest` to use `@AfterEach` hooks for ViewModel cleanup.
- **Race Condition Fixes**: Refactored `TabManagementTest.kt` and `PersistenceIntegrationTest.kt` to wait for asynchronous operations (log filtering and preference saving) using `withTimeout` and `first { ... }` blocks, ensuring assertions run only after state changes are complete.
- **Windows Support**: Fixed `IOException` during `@TempDir` cleanup on Windows by ensuring all file handles held by tailing jobs are released before the test finishes.
- **Verification**: Confirmed all integration tests (16 tests) and UI tests (14 tests) pass successfully in a single local run.

## 13:55

### Migration: Modern Testing API

Migrated all UI tests from the deprecated JUnit 4 `createComposeRule()` to the modern `androidx.compose.ui.test.v2.runComposeUiTest` functional API.

#### Changes:
- **Test Infrastructure**: Updated `BaseRobot`, `MainRobot`, `LogListRobot`, `SidebarRobot`, and `WindowRobot` to use the `ComposeUiTest` interface instead of `ComposeTestRule`.
- **Test Suites**: Refactored `KLogViewerUiTest.kt`, `KLogViewerComplexUiTest.kt`, and `KLogViewerSmokeTest.kt` to follow the `runComposeUiTest { ... }` pattern.
- **Experimental API**: Added `@OptIn(ExperimentalTestApi::class)` to handle the experimental status of the new multiplatform-ready testing API.
- **Robustness**: Fixed parameter mismatch in `waitUntil` calls within `BaseRobot.kt` caused by API changes in the new testing framework.
- **Verification**: Executed `./gradlew :ui:desktopTest` and confirmed that all 15 UI tests pass successfully without deprecation warnings regarding the rule.

## 14:05

### Task: Code Review Fixes

Addressed code review feedback and improved code quality in the UI layer.

#### Changes:
- **KLogViewerScreen.kt**: Fixed non-exhaustive `when` expressions by adding `else` branches to satisfy stricter compiler requirements.
- **KLogViewerComplexUiTest.kt**: Removed redundant `@OptIn(ExperimentalTestApi::class)` annotations where they were no longer needed after the API migration.

## 15:30

### Sprint 7: Advanced Log Formats & Flexible Parsing

Successfully completed Sprint 7, introducing a highly flexible and powerful log parsing engine that supports a wide variety of industry-standard and custom log formats.

#### Core Achievements:
- **Template-Based Parsing**: Implemented a regex-driven `ParserRegistry` allowing for easy extension and support for Standard, Syslog, Apache, ISO8601, and CSV formats.
- **Structured Data (JSON)**: Added `JsonLogParser` with automatic field mapping, providing deep visibility into cloud-native and structured logs.
- **Multiline Support**: Integrated `MultilineProcessor` to correctly aggregate stack traces and indented content, ensuring search and filtering work on complete log events.
- **Heuristic Detection**: Developed `HeuristicProbe` to automatically identify the most appropriate parser for a given file, providing a "zero-config" experience.
- **UI Integration**: Added active parser name indication and a manual override menu to the status bar, and ensured monospace rendering for complex log content.

## 21:55

### CI/CD Stability: Resource Management and Race Conditions

Resolved persistent cross-platform test failures (particularly on Windows) by improving coroutine management and resource cleanup.

#### Core Achievements:
- **Coroutine Safety**: Updated `KLogViewerViewModel` to use `cancelAndJoin()` for log observation jobs, ensuring file handles are fully released before new ones are opened.
- **Exception Handling**: Modified `FileLogSource` to correctly propagate `CancellationException`, preventing erroneous error logging during job cancellation.
- **Test Robustness**: Enhanced integration tests with explicit wait loops and `tearDown` delays, specifically addressing Windows-only file locking issues during `@TempDir` cleanup.
- **Race Condition Resolution**: Fixed a race condition in `PersistenceIntegrationTest` by verifying actual data presence in saved preferences rather than relying on side-effects like tab count.

# 2026-05-20

## 07:40

### UI Polish: Status Bar Enhancements

Improved the discoverability and usability of the log format selection menu.

#### Changes:
- **Visual Cues**: Added a Material Design `ArrowDropDown` icon next to the active parser name in the `StatusBar`.
- **UX**: Unified the format label and arrow into a single clickable row, providing a larger target area for triggering the parser selection menu.

## 07:48

### Branch Creation: Sprint 8 - Connectivity & Remote Sources

Initiated Sprint 8 by creating the dedicated feature branch and initializing task tracking for the connectivity features.

#### Changes:
- **Git**: Created and checked out the `feature/connectivity` branch.
- **Documentation**: Created `docs/tasks/TASKS-SPRINT-8-CONNECTIVITY.md` with the full task breakdown for SFTP, S3, and Network log sources.

## 11:15

### SFTP Remote Log Source Implementation

Implemented the first major feature of Sprint 8: real-time log tailing from remote servers via SFTP.

#### Changes:
- **Core Engine**: Developed `SftpLogSource` using the `sshj` library. It implements the `LogSource` interface and supports real-time tailing using `tail -f` over SSH.
- **Authentication**: Added support for both Password and Key-Pair authentication. Key-Pair support includes optional passphrase handling for encrypted keys.
- **UI**: Created a new `SftpConnectionDialog` accessible via **File > Connect to SFTP...**. The dialog supports host, port, username, and flexible authentication selection.
- **Robustness**: Fixed a `NullPointerException` in `sshj` when using key-pairs without passphrases and standardized error reporting across the log source pipeline.
- **Documentation**: Updated `README.md` with detailed instructions on how to connect to SFTP sources and specifically what key files to select.
- **Verification**: Verified the implementation with a comprehensive suite of unit tests in `SftpLogSourceTest.kt` using mocked SSH clients.

## 11:25

### Fixed SFTP Silent Failures

Addressed an issue where invalid SFTP log paths caused the UI to hang with a spinner.

#### Changes:
- **Core**: Updated `SftpLogSource` to monitor the SSH command's `exitStatus` and `stderr`.
- **Robustness**: If `tail` fails (e.g., file not found), the error is now captured and emitted as a `LogFailure.FileError`, allowing the UI to show a descriptive message.
- **State Management**: Ensured that an `Initial` log update is emitted even for empty files, which correctly stops the loading spinner in the UI.
- **Testing**: Added unit tests to `SftpLogSourceTest.kt` to verify error propagation for missing files and correct handling of empty remote files.

## 11:35

### Dialog Focus Management

Implemented explicit focus management and Tab navigation for all custom dialogs.

#### Changes:
- **Core UI**: Added `FocusManager` and `FocusRequester` to `SftpConnectionDialog`, `RecentItemsDialog`, and the missing file dialog.
- **Navigation**: Implemented `onPreviewKeyEvent` handlers to intercept Tab/Shift+Tab and move focus manually, ensuring standard desktop behavior.
- **UX**: Ensured all dialogs get initial focus when opened.
- **Architecture**: Created **ADR 025** to document the strategy for focus management in the application.
- **Tracking**: Updated Sprint 8 tasks and project memory to reflect completion of this UX refinement.

## 11:45

### Feature: SFTP Connection Management

Implemented the ability to save and manage multiple SFTP connections in user preferences.

#### Core Achievements:
- **Persistence**: Updated `UserPreferences` to store a list of named `SftpConfig` objects.
- **Serialization**: Enabled `kotlinx.serialization` for SFTP configuration models and tiny types.
- **UI Management**: Enhanced `SftpConnectionDialog` with a "Saved Connections" dropdown and a "Save" button to manage configurations.
- **CRUD Operations**: Added support for saving, loading, and deleting SFTP connection profiles from the UI.
- **Robustness**: Ensured all existing SFTP tests remain passing after model updates and added verification for preference persistence.

## 12:20

### Fix: Tab Title for SFTP Connections

Resolved an issue where the tab title was not being updated when connecting to an SFTP log source.

#### Changes:
- **ViewModel**: Updated `connectSftp` in `KLogViewerViewModel` to extract the remote filename from the path and update the tab title state.
- **Consistency**: Unified the tab title update logic across local file loading and remote SFTP connections.
- **Testing**: Added `SftpTabTitleTest.kt` to verify that the tab title correctly reflects the remote filename upon connection.
- **Verification**: Confirmed all integration and unit tests pass, ensuring no regressions in tab management.

## 17:15

### Feat: Tab and Status Bar Tooltips

Enhanced the UI by adding tooltips to tabs and the status bar, allowing users to see the fully qualified file name/path by hovering.

#### Changes:
- **UI**: 
    - Wrapped tab titles in `KLogViewerScreen.kt` with `TooltipWrapper`. The tooltip shows a newline-separated list of all file paths currently loaded in the tab.
    - Updated `StatusBar.kt` to wrap the file path display with `TooltipWrapper`, ensuring the full path is always accessible even if truncated in the UI.
- **Task Tracking**: Updated `TASKS-SPRINT-8-CONNECTIVITY.md` with the new tooltip task (13.4.14).
- **Verification**: Confirmed the build succeeds and verified the tooltip logic for both single-file and multi-file (merged) log views.

## 21:30

### CI Stability & State Management Fixes

Hardened the SFTP log source and improved state consistency for directory monitoring.

#### Changes:
- **ViewModel**: Fixed a bug where deleted files within a monitored directory were not correctly identified as missing in the UI state. Standardized `missingSourceIds` updates to include both primary and sub-sources.
- **SFTP Reliability**: Hardened `SftpLogSourceTest` by ensuring data is written to the pipe before observation starts, eliminating race conditions in `Initial` load detection on fast CI runners.
- **Resource Management**: Added explicit SSH `exitStatus` mocking to ensure clean flow termination and robust state verification.

## 21:43

### Version Update and Resource Cleanup

Maintenance and minor UI updates.

#### Changes:
- **UI**: Updated the Sidebar version label to `v1.3.1` to reflect recent improvements and bug fixes.
- **Core**: Enhanced `SftpLogSourceTest` stability by ensuring pre-observation data writes and clean coroutine cancellation.

## 22:30

### Refined Missing File Handling

Streamlined the user experience when dealing with missing or deleted files by removing intrusive dialogs.

#### Changes:
- **UX Improvement**: Removed the "File Not Found" dialog that appeared during session restoration or when opening missing files from history.
- **Seamless State**: Missing files are now immediately opened as red, strike-through tabs/windows, consistent with the existing window error flow.
- **Cleanup**: Removed obsolete `MISSING_FILE` dialog logic and associated state properties. Relied on the "Clear Missing" button in the Recent Items dialog for history pruning.

# 2026-05-21

## 14:20

### UI Regression Testing Strategy and Integration Hardening

Established a formal strategy for reducing UI regressions and added comprehensive integration tests.

#### Changes:
- **Documentation**: Authored `docs/UI-REGRESSION-TESTING-STRATEGY.md` and updated `docs/TESTING.md` to define baseline management, CI normalization, and screenshot-testing decision rules.
- **Integration Tests**: Added `LogLoadingIntegrationTest.kt` to verify end-to-end loading of single files and full directories, ensuring stable source ID and log entry counts.
- **Terminology**: Aligned all testing documentation with the project's Ubiquitous Language (Workspace, Tab, Log Window, Filter).
