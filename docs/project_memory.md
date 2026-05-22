# Project Memory

## Overall
**What was shipped**
- Initial project structure defined and documented.
- Sprint 1: Walking Skeleton completed (Multi-module, MVI, Log Loading).
- Sprint 2: UI/UX Refinement completed (Command-Line Chic theme, Layout, Filtering).
- Deepened architecture with reactive `LogSource` streaming.
- Native UI enhancements for file browsing.
- Project renamed to KLogViewer (from LogViewer) across all modules, packages, and documentation.

**Key decisions**
- Adopted MVI for UI architecture to align with functional and immutable principles.
- Chose Arrow's `Either` for error handling to support typed domain failures.
- Selected a Layered Multi-Module structure (`domain`, `core`, `ui`, `app`) for better separation of concerns.
- Committed to using Tiny Types for core domain concepts to enhance type safety.
- Transitioned to a streaming Flow-based `LogSource` to support scalability and real-time monitoring.
- Sprint 5: Recursive Directory Loading completed (Recursive scanning, Merging, Textual source badges).
- Sprint 6: UI Redesign ("Enema") completed (high-density layout, consolidated filters, IDE-style theme).
- UI Refinements: Added scrollbars, further reduced tab bar depth, eliminated line gaps, updated tab bar background to a distinct grey color, and replaced RibbonBar with a unified FilterBar supporting multi-item filtering.
- UI Simplification: Removed redundant toggles from Sidebar and unnecessary file icons from FilterBar to achieve a cleaner, content-focused interface.
- Sidebar Restyling: Replaced standard Material checkboxes with a high-density, hierarchical filter panel featuring square checkboxes, right-aligned counts, and section headers with expand arrows, matching professional IDE patterns. Improved readability by using sentence case for log level names.
- Unified Filtering: Transitioned from "Search" to "Filter" terminology. Added a clear-all "cross" icon to the FilterBar and updated all internal logic to match this terminology.
- UI Density: Further reduced FilterBar height by transitioning to `BasicTextField` and smaller icons, and tightened sidebar filter padding for maximum vertical space.
- Split View: Implemented horizontal split view support with an independent window-based architecture, allowing multiple logs to be viewed, filtered, and sorted independently within a single tab.
- Navbar Tooltips: Integrated `TooltipArea` across all primary UI icons (tabs, filters, splits, details), improving discoverability and accessibility.
- UI Layout Persistence: Implemented full workspace restoration. The application now remembers and reloads all open tabs, split windows, active filters, and loaded files on startup.
- Sprint 7: Advanced Log Formats & Flexible Parsing completed (Template-based parsing, Multiline support, JSON, Heuristic detection).
- UI Integration: Added active parser indication and selection menu to the status bar, and ensured monospace rendering for multiline content in the details pane.
- Fixed a critical UI bug where log grid columns were squashed to the left in horizontal scroll mode.
- Resolved column resizing 'snap back' issue by implementing a robust drag accumulator and using `rememberUpdatedState` in Compose.
- Optimized performance by debouncing preference saves during rapid column resizing.
- Added `IntrinsicSize.Min` to the log header to ensure proper vertical alignment and resize handle visibility.
- Improved layout stability within `horizontalScroll` by removing infinite-width `fillMaxSize` constraints from `LazyColumn`.
- Dynamic Details Pane: Redesigned the log entry details pane to adapt to different log formats. It now hides the "Level" field when unknown (common in structured logs like Nginx) and automatically displays all available fields from the `LogEntry.fields` map.
- Formalized the Ubiquitous Language and established it as a core architectural requirement (ADR-022).
- UI Robustness: Implemented truncation (10k-50k chars) and constraint capping (10k dp) for large log entries to prevent `IllegalArgumentException` in Compose `Constraints`.
- Multi-Log Differentiation: Replaced colored dots with small circles that show the full source path on hover as a tooltip.
- Recent Items Management: Implemented automatic filtering of non-existent files from the "Recent Items" list. Added a cleanup mechanism ("Clear Missing") and individual removal buttons for history management.
- Robust Missing File Handling: Implemented a "File Not Found" confirmation dialog that appears when attempting to open a missing file from the history. This ensures that the current log viewer remains unchanged while offering the user an immediate way to prune broken links from their recent items list. Fixed an issue where the native system file picker would incorrectly trigger alongside this dialog.
- Live File Deletion: Implemented a mechanism to detect when a currently viewed log file is deleted. The UI retains the existing data but highlights the filename in red with a strike-through in the window header, tab row, and status bar.
- Full-Width Backgrounds: Fixed log entry layout to ensure background colors and selection highlights extend to the full width of the scrollable window.
- Synchronized Row Widths: Implemented dynamic width tracking to ensure all rows stretch to the width of the widest row, filling the background to the edge.
- Automatic Scrolling: Implemented auto-scroll (tailing) functionality with a persistent toggle in the toolbar.
- Level Filtering: Added an 'All' option in the sidebar to enable or disable all log levels in one go.
- Keyboard Shortcuts: Added Cmd+W to close active tab, Cmd+N for new tab, and Cmd+C to copy selected logs.
- Multi-selection: Implemented multi-selection in log list (Shift+Click for range, Cmd+Click for toggle).
- ANSI SGR Support: Added support for parsing and displaying ANSI SGR color codes in log files, with a UI toggle in the Filter Bar.
- SFTP Session Restoration: Fixed a critical bug where remote directories were not reloaded correctly on startup. Improved `loadFilesIntoWindow` to correctly handle SFTP directory URIs and prevent double-tailing of sub-files. Added missing `savePreferences` calls to ensure remote source state is persisted immediately when opened.
- Remote File Deletion Detection: Enhanced `SftpDirectoryLogSource` to detect file removal from monitored directories. Implemented UI feedback using red badges and strike-through text for logs from missing sources.
- Auto-Save Connection Details: Centralized SFTP connection persistence to ensure that any connection established (via direct connect, browsing, or directory selection) is automatically added to the user's saved connections list.
- Multi-Source Interleaving: Enhanced `handleLogUpdate` to merge initial logs from multiple sources and ensure chronological interleaving via automatic sorting.
- Dependency Injection for Remote Sources: Refactored ViewModel and SFTP log sources to allow full dependency injection (Dispatcher, Client Provider), enabling fast and reliable automated testing of remote log flows.
- Remote Directory Monitoring: Improved `SftpDirectoryLogSource` to automatically detect and add new files discovered during directory rescans. Fixed bugs where tailing would stop prematurely and directory initialization would hang on file errors. Also fixed a critical bug where logs were cleared every rescan interval.
- SSH Connection Sharing: Implemented a connection pool in `SftpDirectoryLogSource` to share SSH connections across multiple log files (8 sessions per connection), preventing server-side limit exhaustion.
- Data Integrity: Fixed a bug where logs received during remote directory initialization were lost due to being overwritten by an incomplete directory-wide update.
- UI Robustness: Refined "missing file" indicators to only strike through the primary path (file or directory URI) if it fails, while using an Orange color for secondary source failures. Improved `missingSourceIds` clearing logic in the ViewModel.
- Toolbar Disconnect/Reconnect: Added a button to pause/resume log paging (observation). Connection state is persisted and reflected in the UI with color changes in the header and status bar.
- SFTP Session Restoration: Implemented automatic reloading of SFTP log sources on startup by matching stored URIs with connection profiles.
- Missing File Indicators: Added visual indicators (red text, strike-through) for both local and remote files that fail to load or disappear.
- Robust SFTP Connections: Implemented a retry mechanism with exponential backoff for SSH connections and authentication. Added staggered connection loading (200ms delay) in `SftpDirectoryLogSource` to prevent server-side rate limiting when observing multiple remote files.
- Sprint 8: Connectivity & Remote Sources initiated with dedicated feature branch and task tracking.
- SFTP Support: Implemented real-time log tailing over SFTP with support for password and key-pair authentication.
- SFTP Connection Management: Added support for saving, loading, and deleting SFTP connection profiles in user preferences.
- Tab Title Logic: Unified tab title updates across local and remote connections. The tab name now correctly reflects the filename for SFTP log sources.
- SFTP Auto-save: Automatically save or override SFTP connection profiles when connecting to a remote source.
- Dialog Focus Management: Implemented explicit Tab navigation and initial focus for all application dialogs (ADR-025).
- Documentation: Updated README.md with detailed SFTP usage instructions and key file requirements.
- Fixed a `java.lang.IndexOutOfBoundsException` in `ScrollableTabRow` by implementing defensive indexing and ensuring the tab row only renders when tabs are available.
- Fixed a bug where resizing a column in a split pane would resize the column in the focused pane instead of the one being interacted with.
- Robust Directory Monitoring: 
    - Implemented "clean refresh" for monitored directories. When a file is deleted from a directory (local or remote), its logs and source ID are now automatically removed from the window to maintain an accurate view of the directory's contents.
    - Added Selective Log Removal logic to distinguish between directory sub-sources (removed on deletion) and primary user-opened sources (retained with warning on deletion).
    - Suppressed global error states (red status bar) for individual file deletions within a monitored directory, ensuring that warnings are only shown for critical path failures.
- Fixed a resource leak in `KLogViewerViewModel` where background log observation jobs were not cancelled when a load intent for a missing file was processed, causing file locks and test failures on Windows.
- Enhanced UI to make the active window more obvious by adding a subtle left border in split-pane view.
- Fixed a regression where the "Message" column would disappear due to `weight(1f)` squashing in constrained rows.
- Refined window activation: Clicking a non-active window in split-pane view now only activates the window; log entry details are only shown if the window is already active.
- Initiated UI Testing Spike to establish a formal E2E testing strategy using Compose for Desktop and the Robot Pattern.
- Tab & Status Bar Tooltips: Implemented tooltips for tab titles and the status bar, providing instant access to the fully qualified file name/path on hover.
- Refined Directory Monitoring UI: Improved visual feedback for directory views by ignoring sub-file removal for color-coding. Tabs and window headers now only turn red if the directory itself is missing, while merged file views retain orange warnings for missing files.
- SFTP cancellation reliability: fixed a remote tail cancellation deadlock that prevented adding a second SFTP source to an active tab/workspace.

**Gotchas**
- Initial discussion on `Result` vs `Either` highlighted the importance of typed errors in functional design.
- `FileDialog` via `AwtWindow` requires manual state reset on close to avoid dialog re-triggering.
- Blocking remote reads may not react to coroutine cancellation unless the underlying SSH command/session/input stream is explicitly closed.

## Sprint: Walking Skeleton Implementation
**Title**: Sprint 1 Completion
**Date/time completed**: 2026-05-12 11:30
**What was shipped**
- Layered multi-module project structure (`domain`, `core`, `ui`, `app`).
- Core domain models and Tiny Types for type safety.
- `LogParser` and initial `LogService` for parsing and loading log files.
- MVI-based UI layer with `KLogViewerViewModel` and Compose components.
- BDD (Cucumber) and TDD (JUnit/Strikt) test suites.

**Key decisions**
- Used `value class` for Tiny Types to ensure performance with safety.
- Implemented unidirectional data flow (MVI) in the UI layer.
- Offloaded file I/O to `Dispatchers.IO` in the ViewModel.

**Gotchas**
- Gradle 9.3 requires `junit-platform-launcher` to be explicitly added to the test runtime classpath.
- Cucumber docstrings can introduce leading whitespace that may cause regex parsing to fail if not handled (fixed by adding `.trim()` in the parser).

**Test coverage areas**
- `SimpleLogParser`: Unit tests for various log levels and invalid lines.
- `KLogViewerViewModel`: BDD test covering the end-to-end flow of loading a log file and updating the state.

## Task: Architectural Deepening - Streaming LogSource
**Title**: LogSource Implementation
**Date/time completed**: 2026-05-12 11:50
**What was shipped**
- `LogSource` repository interface in `:domain`.
- `FileLogSource` implementation in `:core` using Kotlin Flow.
- Delta-based `LogUpdate` models for efficient UI updates.
- Refactored `KLogViewerViewModel` to consume streams instead of full lists.

**Key decisions**
- Internalized concurrency (`Dispatchers.IO`) within the `LogSource` implementation.
- Used `LogUpdate` sealed interface to distinguish between initial load and subsequent appends.

**Gotchas**
- Asynchronous flow collection in BDD tests required using `flow.first()` with timeout to avoid race conditions.

**Test coverage areas**
- `FileLogSource`: Integration via BDD tests.
- `KLogViewerViewModel`: Updated BDD steps to handle Flow collection.

## Task: UI Enhancements - File Browsing
**Title**: File Browsing Implementation
**Date/time completed**: 2026-05-12 12:05
**What was shipped**
- Integrated native `FileDialog` into the Compose for Desktop UI.
- Enhanced `FileSelector` component with a "Browse" button.
- Updated MVI model with `SelectPath` intent for decoupled path selection.

**Key decisions**
- Used `java.awt.FileDialog` via `AwtWindow` to provide a native OS feel for file selection.
- Maintained MVI purity by routing file selection through the ViewModel's state.

**Gotchas**
- `AwtWindow` requires careful handling of the `onCloseRequest` to ensure the dialog state is reset in the Composable.

**Test coverage areas**
- UI components: `FileSelector` and `KLogViewerScreen` (verified via manual run and build).

## Sprint: UI/UX Refinement
**Title**: Sprint 2 Completion
**Date/time completed**: 2026-05-12 12:55
**What was shipped**
- "Command-Line Chic" design system with Light/Dark mode support.
- Refined layout with collapsible Sidebar and persistent Status Bar.
- Real-time Log Level filtering and Text Search.
- Intelligent Highlighting for IDs, IPs, and Timestamps.
- Line numbering in a dedicated gutter.

**Key decisions**
- Used `CompositionLocal` for providing theme-specific log colors.
- Implemented background thread filtering in `ViewModel` to ensure UI responsiveness.
- Added `material-icons-extended` for a richer UI icon set.
- Used `AnnotatedString` for efficient multi-pattern highlighting.

**Gotchas**
- `Icons.AutoMirrored` should be used for certain icons like `MenuOpen` to support RTL.
- `LazyColumn` required `itemsIndexed` to reliably display line numbers.
- `junit-platform-launcher` is required for Gradle 9.3 test execution in `:ui` module.

**Test coverage areas**
- `LogHighlighter`: Unit tests for regex-based highlighting and search term bolding.
- UI Layout: Verified via build and compilation.

## Task: Roadmap Reorganization
**Title**: Sprint 4 Split & Roadmap Refinement
**Date/time completed**: 2026-05-14 14:35
**What was shipped**
- Split Sprint 4 into three distinct sprints: Sprint 4 (User Preferences), Sprint 5 (Recursive Directory Loading), and Sprint 6 (UI Redesign).
- Created `docs/sprints/sprint-5-recursive-loading.md` and `docs/sprints/sprint-6-ui-redesign.md`.
- Created corresponding task list files: `TASKS-SPRINT-5-RECURSIVE-LOADING.md` and `TASKS-SPRINT-6-UI-REDESIGN.md`.
- Renamed and renumbered future placeholder sprints (7-11) to maintain a continuous roadmap.

**Key decisions**
- Decided to isolate "Enema" (UI Redesign) into its own sprint to focus on desktop-centric UX polish after core features are stable.
- Elevated Recursive Directory Loading to a dedicated sprint due to its complexity and impact on the core `LogSource` architecture.

**Gotchas**
- Renumbering existing documentation files requires careful updates to internal titles and task IDs to avoid cross-referencing errors.

## Task: State-of-the-Art Review and Gap Analysis
**Title**: Desktop UI Pattern Review & Gap Analysis
**Date/time completed**: 2026-05-13 11:55
**What was shipped**
- `docs/FEATURES.md`: Comprehensive review of state-of-the-art desktop log viewers and detailed gap analysis (Current vs. Proposed vs. SOTA).
- Integrated **Sematext** into the gap analysis as a cloud-native benchmark for high-density UI and ad-hoc analysis.
- `docs/adr/adr-008-desktop-centric-ui.md`: Architectural decision to transition from "Mobile Chic" to a "Desktop-Centric" UI pattern.
- Identified roadmap opportunities: Remote source support, structured data parsing, and statistical visualization.

**Key decisions**
- Move from Material-centric `Scaffold` to a multi-pane workspace layout.
- Introduce Ribbon/Tool Bars and native Menu Bars to improve discoverability and feature depth.
- Align UI evolution with the upcoming multi-log and tabbed interface work (Sprint 3).
- Explicitly target high-performance virtualized grids as a competitive requirement.

**Gotchas**
- Market review highlights that high performance (handling >1GB files) is a baseline expectation for desktop log viewers, not just a stretch goal.
- Balancing "Desktop-Native" feel with "Compose Material" components requires careful selection of UI libraries and custom styling.

**Test coverage areas**
- N/A (Documentation and Requirements phase).

## Task: Multi-Log Support - Tabs and Interleaving
**Title**: Tabs & Interleaving Implementation
**Date/time completed**: 2026-05-13 18:30
**What was shipped**
- Tabbed workspace architecture with `TabState` and `KLogViewerState` refactoring.
- Chronological multi-log interleaving via `MergedLogSource`.
- Source identification badges in the log list.
- Multi-file selection support in native `FileDialog`.
- MVI intents for tab management (`AddTab`, `CloseTab`, `SwitchTab`).

**Key decisions**
- Used a "Workspace-Centric" state model to support independent views per tab.
- Implemented in-memory chronological merging for initial log loads.
- Added `sourceId` to the `LogEntry` domain model for attribution.
- Integrated a `ScrollableTabRow` to handle many open tabs.

**Gotchas**
- AWT `FileDialog` requires `isMultipleMode = true` to enable multi-file selection.
- Merging multiple flows requires careful coordination of `Initial` vs `Appended` updates (Sprint 3 focuses on `Initial` load).
- Smart casting of nullable properties from other modules in Kotlin requires local variable assignment.

**Test coverage areas**
- `MergedLogSource`: Unit tests for chronological merging and source attribution.
- BDD Tests: Updated to verify end-to-end flow with the new tabbed state.

## Task: Product Roadmap and Sprint Definition
**Title**: Roadmap Definition Completion
**Date/time completed**: 2026-05-13 11:50
**What was shipped**
- `docs/adr/adr-009-structured-data-support.md`: Strategy for JSON/XML and tree-view handling.
- `docs/adr/adr-010-remote-log-sources.md`: Strategy for SFTP, Cloud, and network streams.
- `docs/adr/adr-011-data-visualization-strategy.md`: Strategy for charting and analysis.
- `docs/adr/adr-012-workspace-persistence.md`: Strategy for session saving and project files.
- `docs/adr/adr-013-plugin-architecture.md`: Strategy for extensibility.
- `docs/sprints/sprint-7-advanced-log-formats.md`: Detailed plan for Sprint 7.
- `docs/sprints/sprint-8-connectivity.md`: Detailed plan for Sprint 8.
- `docs/sprints/sprint-9-analysis-and-visualization.md`: Detailed plan for Sprint 9.
- `docs/sprints/sprint-10-power-user-tools.md`: Detailed plan for Sprint 10.
- `docs/sprints/sprint-11-extensibility-and-release.md`: Detailed plan for Sprint 11.
- `docs/sprints/sprint-12-structured-data.md`: Detailed plan for Sprint 12.

**Key decisions**
- Structured the roadmap around five core themes: Data, Connectivity, Analysis, Persistence, and Platform.
- Committed to a modular plugin architecture to support ecosystem growth.
- Prioritized workspace persistence to enhance power-user productivity.
- Adopted a lazy-parsing strategy for structured logs to maintain UI performance.

**Gotchas**
- Credential management for remote sources requires native OS integration to remain secure.
- Indexing strategy for >10GB files is a critical requirement for Sprint 8.

**Test coverage areas**
- N/A (Documentation and Planning phase).

## Task: Source Identification and UI Polish
**Title**: Source Identification and UI Polish
**Date/time completed**: 2026-05-14 08:35
**What was shipped**
- Dedicated `SourceBadge` component with dynamic hash-based coloring.
- Updated `LogEntryRow` to display source badges for interleaved logs.
- "Add to Workspace" capability in the UI and MVI model.
- Dynamic source color generation for visual distinction.
- Integration tests for tab state preservation and interleaving.

**Key decisions**
- Used hash-based Hue generation for stable, diverse source colors without a central registry.
- Implemented `AddToWorkspace` by merging new paths with current tab state and reloading via `MergedLogSource`.
- Added new integration test suite (`com.klogviewer.integration`) to verify multi-tab behavior.

**Gotchas**
- Hash-based colors can sometimes produce low-contrast combinations; added fixed saturation and value for better readability.
- Multi-file selection in `FileDialog` returns a `List<File>`, which needs to be mapped to absolute paths for the domain layer.
- `writeText` in tests overwrites files; use `trimIndent` with multi-line strings for clear test data.

**Test coverage areas**
- `SourceBadge`: Visual component (verified via manual run).
- `TabManagementTest`: Integration test for independent search/logs per tab.
- `InterleavingIntegrationTest`: Integration test for adding files to workspace and chronological merging.

## Task: Desktop UI Transition
**Title**: Desktop UI Transition (Sprint 3 Finalization)
**Date/time completed**: 2026-05-14 08:45
**What was shipped**
- Native OS Menu Bar with File, Edit, and View menus.
- Ribbon-style Toolbar replacing the Material TopBar.
- Multi-pane Split View for log entry inspection.
- High-density, compact log grid with selection support.
- Unified dialog management via MVI state.

**Key decisions**
- Migrated FileDialog state to `KLogViewerState` to allow triggering from both the Menu Bar and Ribbon.
- Implemented `RibbonBar` as a group of functional sections (File, View, Filters) for better discoverability.
- Used a 250dp detail pane for log entry inspection, triggered by row clicks.
- Reduced row padding and increased information density in `LogList` to meet power-user requirements.

**Gotchas**
- Compose for Desktop `MenuBar` must be defined within the `Window` scope.
- Row selection in `LazyColumn` requires managing `selectedEntry` state in `TabState`.
- Icons for `ViewSidebar` were deprecated; moved towards modern variants where suggested by compiler.

**Test coverage areas**
- `KLogViewerViewModel`: Verified new intents for selection and dialog management.
- Integration: Verified that state changes (filtering, selection) work correctly across the new UI components.

## Task: Enhanced Grid and Tailing
**Title**: Enhanced Grid & Local Tailing
**Date/time completed**: 2026-05-14 09:10
**What was shipped**
- Log Grid Headers for Timestamp, Level, and Message.
- Real-time Local File Tailing (watching for updates).
- Support for streaming appends in `MergedLogSource`.
- Polling-based change detection in `FileLogSource`.

**Key decisions**
- Used polling in `FileLogSource` for cross-platform reliability in detecting appends.
- Switched `MergedLogSource` to `channelFlow` to support concurrent streaming from multiple files.
- Added visible column headers to fulfill "Professional Desktop UI" requirements.

**Gotchas**
- Tailing an interleaved view requires careful merging of live flows; current implementation appends entries as they arrive to maintain responsiveness.
- `RandomAccessFile.readLine()` uses ISO-8859-1; required manual conversion to UTF-8 for correct character representation.

**Test coverage areas**
- `FileLogTailingTest`: Unit test verifying that file appends are detected and emitted as `LogUpdate.Appended`.

## Task: Logging Integration
**Title**: Logging Infrastructure and Instrumentation
**Date/time completed**: 2026-05-14 11:30
**What was shipped**
- Integrated SLF4J + Logback + logstash-logback-encoder with kotlin-logging facade.
- Configured `logback.xml` with console and rolling JSON appenders.
- Instrumented `FileLogSource`, `SimpleLogParser`, `MergedLogSource`, and `KLogViewerViewModel` with extensive logging.
- Added explicit error logging for all failure paths in the log loading and parsing pipeline.
- Implemented high-density UI with horizontal scrolling and consolidated sidebar filters (Sprint 6).

**Key decisions**
- Used `kotlin-logging` to leverage idiomatic Kotlin features like lazy log message evaluation.
- Implemented structured JSON logging via Logstash encoder to facilitate machine readability and future observability integration.
- Placed `logback.xml` in the `app` module to centralize runtime logging configuration.

**Gotchas**
- Adding `logback-classic` as a `testRuntimeOnly` dependency in the `core` module was necessary to enable logging output during unit tests.

**Test coverage areas**
- `core` module: Verified that existing tests pass with logging instrumentation.
- `app` module: Startup log verification.

## Task: Window Management
**Title**: Initial Window Size and Centering
**Date/time completed**: 2026-05-14 11:35
**What was shipped**
- Increased default window size to 1200x800 for better high-density grid visibility.
- Configured window to start centered on the screen.

**Key decisions**
- Used `rememberWindowState` to manage window dimensions and position independently of the `Window` lifecycle.
- Selected 1200x800 as a standard "desktop-centric" size that accommodates the sidebar, log grid, and future details pane.

**Gotchas**
- None.

**Test coverage areas**
- UI: Verified via manual run (simulated) and compilation.


## Task: Log Parsing Robustness
**Title**: Parser Regex Enhancement
**Date/time completed**: 2026-05-14 11:45
**What was shipped**
- Enhanced `SimpleLogParser` to support professional log formats.
- Support for milliseconds in timestamps.
- Support for `[THREAD] LEVEL` and `[LEVEL]` interchangeably.

**Key decisions**
- Adopted a heuristic-based parsing logic that identifies the Log Level from either the bracketed field or the subsequent word, maximizing compatibility across different loggers.
- Ensured `SimpleLogParser` remains "simple" by using a single regex and light post-processing instead of a full grammar-based parser.

**Gotchas**
- The `destructured` assignment in the previous implementation was too rigid for optional fields; switched to indexed group access for better control.

**Test coverage areas**
- `LogParserTest`: Added test cases for actual application logs and verified backwards compatibility with older formats.

## Task: Reverse Order View
**Title**: Reverse Order Toggle Implementation
**Date/time completed**: 2026-05-14 12:45
**What was shipped**
- Implemented a "Reverse Order" (Newest First) toggle in the Ribbon Bar.
- Dynamic sorting logic that handles both initial loads and real-time appends.
- Persistent sort state per tab.

**Key decisions**
- Applied the reversal at the filtering stage to ensure that search results and filtered views are also reversed when the toggle is active.
- Chose to maintain the sort order state within `TabState` to ensure a consistent experience when switching between tabs.

**Gotchas**
- Real-time appends to a reversed list appear at the top, which can be disorienting if not expected; added a clear "Newest First" / "Oldest First" label to the toggle.
- Integration tests needed to wait for the filtered list to update specifically, as the raw logs list updates independently.

**Test coverage areas**
- `LogSortingTest`: Integration test verifying that toggling sort order works correctly for both initial loads and real-time appends.

## Task: User Preferences and MRU tracking
**Title**: Cross-Platform Preferences Persistence
**Date/time completed**: 2026-05-14 14:30
**What was shipped**
- `PreferencesRepository` for saving/loading JSON configuration.
- Persistence for window state (width, height, x, y, maximized).
- Persistence for theme and sidebar visibility.
- MRU tracking for files and directories with a "Recently Opened" menu.
- "Show All Recent Items" dialog.

**Key decisions**
- Used `kotlinx-serialization` for JSON-based config files.
- Followed OS-specific standards for app data locations (Library/Application Support on macOS, %APPDATA% on Windows, ~/.config on Linux).
- Included a `customConfigDir` parameter in `PreferencesRepository` to facilitate isolated unit and integration testing.

**Gotchas**
- Window position in Compose Desktop can be tricky when using `Alignment.Center` vs `Absolute`; ensured that only absolute positions are saved to avoid jumping on next launch if the screen resolution changed.
- Required updating all existing integration tests to provide a temporary preferences directory.

**Test coverage areas**
- `PreferencesRepositoryTest`: Unit tests for save/load and error handling.
- Integration: Updated all tab and sorting integration tests to verify compatibility with the preferences system.

## Sprint: UI/UX Redesign
**Title**: Sprint 6 Completion ("Enema")
**Date/time completed**: 2026-05-14 14:55
**What was shipped**
- High-density log list with 0dp vertical padding and no-wrap lines.
- Shared horizontal scrolling for the entire log list and headers.
- Consolidated level filters from toolbar to sidebar with entry counts.
- Narrowed tab bar for increased vertical space.
- IDE-style Dark Mode with dark gray color palette (`#2B2B2B`).

**Key decisions**
- Prioritized information density over Material Design whitespace for desktop productivity.
- Used hash-based counts in `TabState` to provide immediate feedback in the sidebar.
- Adopted Darcula-inspired colors for the dark theme to reduce eye strain.
- Removed redundant filters to simplify the main interface.

**Gotchas**
- Shared horizontal scrolling for `LazyColumn` and its header requires careful layout coordination to ensure they scroll together.
- Reducing padding to 0dp requires adjusting line-height or font-size if text appears too cramped; maintained 12sp for legibility.

**Test coverage areas**
- `KLogViewerViewModel`: Verified `levelCounts` calculation.
- UI Layout: Verified through manual inspection and compilation.

## Task: UI Refinements and Polish
**Title**: UI Refinements (Sprint 6 Polish)
**Date/time completed**: 2026-05-14 15:17
**What was shipped**
- Native-style vertical and horizontal scrollbars in the log grid.
- Ultra-high density log rows (0dp padding, no vertical gap).
- Optimized tab bar depth (32dp) for maximum content visibility.
- Improved log entry row interactions using `Modifier.clickable`.
- Updated Tab Bar background to use a distinct grey color (`#323232` Dark / `#E0E0E0` Light) to provide better visual separation from the rest of the UI.

**Key decisions**
- Switched from `Surface(onClick)` to `Modifier.clickable` to bypass Material Design's minimum touch target size (48dp), enabling ultra-high density rows.
- Used fixed-height (32dp) tab rows to maximize vertical real estate for logs.
- Integrated `VerticalScrollbar` and `HorizontalScrollbar` for better navigation of large log files and long lines.
- Introduced a specific `TabBackground` color that is slightly different from the `Surface` color (used for the Ribbon Bar) to ensure the tab area stands out visually.

**Gotchas**
- Material `Tab` and `ScrollableTabRow` components have internal constraints that can be overridden by explicit `Modifier.height`.
- Horizontal scrollbars should be placed below the content they scroll to avoid overlapping with the last visible items.
- When changing background colors, ensure `contentColor` is updated (e.g., from `onPrimary` to `onSurface`) to maintain readability.

## Task: Sidebar Filter Styling
**Title**: Sidebar Filter Restyling (Sprint 6 Polish)
**Date/time completed**: 2026-05-14 15:40
**What was shipped**
- Redesigned `LogLevelToggle` with square checkboxes and right-aligned counts.
- Implemented hierarchical layout for Sidebar filters with section headers ("FILTERS") and sub-sections ("Levels (5)").
- Added expand/collapse arrows and proper indentation for filter entries.
- High-density filter rows with optimized vertical padding.
- Changed log level names to sentence case (e.g., "Debug", "Info") for a cleaner look.

**Key decisions**
- Used custom `Box`-based checkboxes to match the specific "IDE-style" look requested by the user, bypassing Material Design's default checkbox constraints.
- Adopted a hierarchical list pattern to improve organization and scalability of filters.
- Right-aligned counts to provide a cleaner visual scan of log distribution.

**Gotchas**
- Custom checkboxes require manual handling of checked states and icons, but provide much better control over high-density layouts.
- Indentation (start padding) is crucial for conveying the hierarchical relationship between the category header and its items.

## Task: Filter Bar Renaming & Cleanup
**Title**: Filter Bar Renaming and Clear All (Sprint 6 Polish)
**Date/time completed**: 2026-05-14 15:55
**What was shipped**
- Renamed all "Search" functionality to "Filter" throughout the application (UI and internal code).
- Added a "cross" icon to the Filter Bar to clear all active filters and current input text.
- Changed placeholder text from "Search..." to "Filter...".
- Renamed `SearchBar` component to `FilterBar`.

**Key decisions**
- Aligned internal code (intents, state properties, functions) with the user-facing "Filter" language to maintain a clean and consistent domain language.
- Placed the "clear all" cross icon inside the filter input area for intuitive access.
- Updated `LogHighlighter` and its tests to reflect the new "filter" terminology.

**Gotchas**
- Renaming core concepts like "Search" to "Filter" requires a thorough sweep of the codebase, including tests and documentation, to avoid confusion.
- The "clear" icon should handle both the text state and the list of active filter chips.

**Test coverage areas**
- `LogHighlighterTest`: Updated to verify "filter" highlighting.
- `TabManagementTest`: Updated to verify independent filter query management.

## Task: Density Improvements
**Title**: Filter Bar and Sidebar Density Improvements (Sprint 6 Polish)
**Date/time completed**: 2026-05-14 16:00
**What was shipped**
- Reduced the height of the `FilterBar` by switching to `BasicTextField` and reducing icon sizes.
- Tightened vertical padding in the `Sidebar` filter rows (from 4dp to 2dp).
- Updated `FilterBar` icons to 28dp (from 32dp) and Divider heights to 20dp (from 24dp).
- Replaced the high-padding Material `TextField` with a custom `BasicTextField` implementation for an ultra-compact filter area.

**Key decisions**
- Used `BasicTextField` with a custom `decorationBox` to completely control the height of the filter input area, bypassing Material Design's default 56dp minimum.
- Reduced overall vertical padding throughout the tool area to maximize the space available for log content.

**Gotchas**
- `BasicTextField` requires manual handling of placeholder text and text styles that were previously managed by the standard `TextField`.
- Small icon targets (28dp) are acceptable for desktop-centric applications with mouse/pointer input.

**Test coverage areas**
- UI Layout: Verified via successful build and manual visual consistency checks.

## Task: Horizontal Split View
**Title**: Horizontal Split View Support (Sprint 6 Feature)
**Date/time completed**: 2026-05-14 16:35
**What was shipped**
- Refactored `TabState` to support multiple `LogWindow` instances per tab.
- Added a "Split Horizontal" button to the `FilterBar`.
- Implemented vertical stacking of splits within a tab, allowing simultaneous viewing of multiple log files.
- Added window focus management, where clicking a split makes it active for filtering and sidebar controls.
- Added a "Close" button for individual split windows.

**Key decisions**
- Used a flexible window-based architecture instead of fixed splits, allowing for future expansion to vertical splits or arbitrary layouts.
- Decided to make the newly created split active by default but start with no logs, allowing the user to explicitly load a new file.
- Maintained backward compatibility for tab titles by only auto-updating the title when a single window is present.

**Gotchas**
- Refactoring `TabState` required widespread changes across the ViewModel and test suites.
- Focus management is critical; users must know which split they are currently filtering. Visual feedback (a subtle background tint and "Active" badge) was added.

**Test coverage areas**
- `TabManagementTest`: Added integration test for multiple splits and independent log loading.
- Regression: Updated all existing integration and BDD tests to work with the new window-based state.

## Task: Navbar Tooltips
**Title**: Navbar Tooltips Implementation (Sprint 6 Polish)
**Date/time completed**: 2026-05-14 16:45
**What was shipped**
- Integrated `TooltipArea` across all primary UI icons in the `FilterBar`, `LogTabRow`, and `LogWindow`.
- Created a reusable `TooltipWrapper` component to ensure consistent tooltip styling (yellowish background, shadow, 600ms delay).
- Added tooltips for: Add File, Toggle Theme, Toggle Sidebar, Split Horizontal, Sort Order, Clear Filters, Remove Filter Chip, Close Tab, Add Tab, Close Split, and Close Details.
- Renamed `SearchBarIcon` to `FilterBarIcon` to align with the renamed component.

**Key decisions**
- Used `androidx.compose.foundation.TooltipArea` to provide a native-feeling tooltip experience in the desktop environment.
- Opted for a subtle 600ms delay to prevent tooltips from appearing too aggressively during rapid mouse movements.
- Encapsulated the tooltip styling in a shared wrapper to maintain design consistency and reduce code duplication.

**Gotchas**
- `TooltipArea` is an experimental API in Compose Foundation and requires the `@OptIn(ExperimentalFoundationApi::class)` annotation.
- Tooltip placement (CursorPoint) was adjusted to avoid overlapping with the icon being hovered.

**Test coverage areas**
- UI Components: Verified through successful compilation and passing UI module unit tests.

## Task: UI Layout Persistence
**Title**: UI Layout Persistence (Sprint 6 Refinement)
**Date/time completed**: 2026-05-14 16:35
**What was shipped**
- Full workspace restoration on application startup, including all open tabs and split windows.
- Persistent filtering state (search queries, log level filters) and sort order per split window.
- Automatic reloading of log files into their respective windows upon startup.
- Real-time persistence: UI layout and filter changes are saved to `preferences.json` as they happen.

**Key decisions**
- Refactored `KLogViewerViewModel` to support background loading of logs for all windows during initialization, ensuring a seamless startup experience.
- Extended `UserPreferences` with serializable `TabPreference` and `WindowPreference` structures to capture the complex UI state.
- Decided to trigger `savePreferences` on layout and filter changes to prevent data loss in case of unexpected application closure.

**Gotchas**
- JUnit 5 requires test methods to return `Unit` (void). Using expression body with `expectThat` (which returns a builder) prevented tests from being recognized until an explicit `Unit` was added.
- `LogLevel` enum required the `@Serializable` annotation to be included in the preference JSON.

**Test coverage areas**
- `PersistenceIntegrationTest`: Verified that tabs/splits are restored and logs are reloaded.
- `PreferencesRepositoryTest`: Verified serialization of complex UI preference structures.

## Task: Log Format Gap Analysis
**Title**: Log Format Gap Analysis (Requirement Analysis)
**Date/time completed**: 2026-05-14 17:05
**What was shipped**
- `docs/LOGGING-FORMATS-GAP-ANALYSIS.md`: Detailed analysis of current log parsing limitations and gaps in supporting diverse real-world log formats.
- Updated `docs/tasks/TASKS-LOGGING.md` with new prioritized tasks for advanced format support.

**Key decisions**
- Identified five primary gap areas: Log Level Mapping, Timestamp Formats, Structured (JSON) Support, Multiline entries, and Auto-detection heuristics.
- Proposed a strategy of transitioning from a single parser to a "Template-based Parser Registry" to support extensibility without code changes.

**Gotchas**
- Existing `SimpleLogParser` is too rigid for production use with varying third-party logs (e.g., abbreviated levels or ISO8601 timestamps).

**Test coverage areas**
- N/A (Analysis and Documentation phase).

## Task: Sprint Planning (Advanced Logging)
**Title**: Sprint 7 Planning: Advanced Log Formats
**Date/time completed**: 2026-05-14 21:30
**What was shipped**
- `docs/sprints/sprint-7-advanced-log-formats.md`: Detailed sprint plan for implementing flexible parsing, multiline support, and JSON logs.
- Renumbered existing future sprints (8-12) to maintain a consistent roadmap.
- Updated `docs/project_memory.md` and roadmap references.

**Key decisions**
- Prioritized Advanced Log Formats as the immediate next sprint following the Gap Analysis.
- Included initial JSON support in the next sprint to address modern cloud log requirements early.

**Gotchas**
- Renumbering future sprints requires updating multiple files and internal headers to avoid confusion.

**Test coverage areas**
- N/A (Planning phase).

## Task: Advanced Logging ADRs
**Title**: Architectural Decisions for Advanced Log Formats
**Date/time completed**: 2026-05-14 21:40
**What was shipped**
- `docs/adr/adr-019-template-based-log-parsing.md`: Decision to implement a `ParserRegistry` with regex-based `LogTemplate`s and flexible `LevelMapper`.
- `docs/adr/adr-020-multiline-log-aggregation.md`: Decision to implement a `MultilineProcessor` for handling stack traces and continuation lines.
- Updated `docs/sprints/sprint-7-advanced-log-formats.md` to reference these new ADRs.

**Key decisions**
- Adopted regex-based templates with named capture groups to provide the highest level of flexibility for end-users.
- Decided to perform multiline aggregation at the ingestion layer (`LogSource`) to ensure searching and filtering work on complete log units.
- Formalized the "Heuristic Probe" strategy for automatic log format detection.

**Gotchas**
- Regex parsing and multiline buffering introduce memory and CPU overhead that must be monitored during implementation.

**Test coverage areas**
- N/A (Architecture and Documentation phase).

## Task: Advanced Logging Tasks
**Title**: Task Management for Advanced Log Formats
**Date/time completed**: 2026-05-14 21:45
**What was shipped**
- `docs/tasks/TASKS-SPRINT-7-ADVANCED-LOGGING.md`: Hierarchical task list for implementing flexible level mapping, template-based parsing, multiline aggregation, and JSON support.
- Updated `docs/tasks/TASKS-LOGGING.md` to link to the new detailed sprint tasks.

**Key decisions**
- Structured tasks to follow the architectural decisions in ADR 019 and ADR 020.
- Organized tasks by feature areas: Level Mapping, Parser Strategy, Multiline Aggregation, JSON Support, and Heuristic Detection.

**Gotchas**
- Ensuring numbering consistency with previous sprints (starting Sprint 7 tasks at 12.x) to maintain a logical project flow.

**Test coverage areas**
- N/A (Planning and Documentation phase).

## Task: Flexible Level Mapping
**Title**: Flexible Level Mapping Implementation
**Date/time completed**: 2026-05-15 07:15
**What was shipped**
- `LevelMapper` utility for normalizing external log levels (e.g., "INF" to `LogLevel.INFO`).
- Support for common abbreviations (DBUG, INF, WRN, ERR, FTL).
- Support for alternative terminology (TRACE, INFORMATION, WARNING, SEVERE, CRITICAL).
- Prefix-based matching support (e.g., 'D' -> `LogLevel.DEBUG`).
- Default level assignment for logs without explicit severity.

**Key decisions**
- Used a configurable `LevelMapper` that can be integrated into different parsers.
- Implemented prefix matching as an optional feature to avoid false positives in strict formats.
- Normalized all inputs to uppercase and trimmed whitespace before mapping.
- Extended `LevelMapper` to automatically strip common wrappers like brackets `[]` and parentheses `()`, improving compatibility with diverse log headers.
- Integrated `LevelMapper` into `SimpleLogParser` to ensure consistent level normalization across all parsing strategies.

**Gotchas**
- Prefix matching can be aggressive; it should only be enabled for formats where the level is a single character or starts with a unique character.

**Test coverage areas**
- `LevelMapperTest`: Unit tests for standard levels, abbreviations, alternative terminology, prefix matching, and default levels.

## Task: Parsing Robustness Improvements
**Title**: Parsing Robustness Improvements (Timezones and Metadata)
**Date/time completed**: 2026-05-15 07:35
**What was shipped**
- Enhanced `SimpleLogParser` and "Standard" `LogTemplate` to support timezone offsets (e.g., `+01:00`) in timestamps.
- Improved level detection in `TemplateLogParser` to handle optional fields like thread names in brackets before the log level.
- Updated `TimestampParser` to support optional milliseconds and timezone offsets using `[ XXX]` pattern.
- Fixed a race condition in `LogSortingTest` integration test.

**Key decisions**
- Used named groups (`metadata`) in the Standard template regex to capture optional fields between timestamp and level.
- Updated `TemplateLogParser` to fallback to checking the `metadata` group if the `level` group doesn't contain a valid log level.
- Switched to `XXX` in `DateTimeFormatter` to support timezone offsets with colons (like `+01:00`).

**Gotchas**
- Increased parsing complexity slightly, which exposed a race condition in an integration test that wasn't waiting for the async filtering to complete.

**Test coverage areas**
- `LogParserTest`: Added test case for timezone offsets.
- `TemplateLogParserTest`: Added test case for `metadata` group level fallback.
- `TimestampParserTest`: Added comprehensive tests for optional timestamp parts (millis, timezone).
- `LogSortingTest`: Improved stability of integration test.

## Task: Split Window Header Refinement
**Title**: Split Window Header Refinement (File Path Visibility)
**Date/time completed**: 2026-05-15 07:45
**What was shipped**
- Updated `KLogViewerScreen.kt` to always display the fully qualified file path in each split window's header.
- Aligned file path to the left and window controls (Active badge, Close button) to the right.
- Ensured the header row is visible even for single splits if a file is loaded.

**Key decisions**
- Placed the file path in the same visual area as the "Active" badge to maintain UI density.
- Used `Arrangement.SpaceBetween` to separate path info from window actions.
- Applied `TextOverflow.Ellipsis` and `weight(1f)` to gracefully handle long file paths.

**Gotchas**
- The header row was previously hidden for single-split tabs; it is now visible when a file path is present, which is a slight change in behavior but meets the "always displayed" requirement.

**Test coverage areas**
- UI layout verified via code inspection and build consistency.

## Task: Advanced Log Parsing Implementation
**Title**: Advanced Log Parsing (Sprint 7 Core)
**Date/time completed**: 2026-05-15 08:55
**What was shipped**
- Enhanced `LevelMapper` with support for `VERBOSE`, `NOTICE`, and numeric levels (0-7).
- Added Unix Epoch support (seconds and milliseconds) to `TimestampParser`.
- Implemented `LogfmtParser` for basic key-value log parsing.
- Expanded `ParserRegistry` with default templates for `ISO8601`, `Apache`, and `CSV`.
- Improved `HeuristicProbe` to automatically detect `logfmt` files.
- Added `GapAnalysisTest.kt` covering all 14+ log variations identified in the Gap Analysis.

**Key decisions**
- Integrated Unix Epoch detection directly into `TimestampParser` by checking for numeric input.
- Added `isLogfmt` check to `HeuristicProbe` using a regex-based heuristic.
- Used `[X]` in `ISO8601` template to support optional `Z` suffix.

**Gotchas**
- Prefix matching in `LevelMapper` can cause false positives (e.g., "Some" mapping to `ERROR` because it starts with 'S'), so it's disabled by default or used with caution in the probe.

**Test coverage areas**
- `GapAnalysisTest`: Comprehensive coverage for diverse levels, timestamps, structures, and multiline logs.
- `LevelMapperTest`: Updated with new level mappings.
- `TimestampParserTest`: Updated with Unix Epoch cases.

## Task: Resizable UI Columns
**Title**: Resizable Columns and Persistence
**Date/time completed**: 2026-05-15 09:35
**What was shipped**
- Implemented resizable columns in the `LogList` grid with interactive drag handles.
- Added `columnWidths` state to `LogWindow` and `WindowPreference`.
- Implemented persistence for custom column widths across application sessions.
- Added `UpdateColumnWidth` intent to the MVI model.
- Integrated resize handles into `LogListHeader` with visual cursor feedback (`E_RESIZE_CURSOR`).

**Key decisions**
- Used `pointerInput` and `detectDragGestures` in the header to handle resizing without needing a full `DataGrid` component.
- Stored widths in a `Map<String, Int>` to allow flexible, column-specific overrides while maintaining default fallbacks.
- Applied dynamic widths to both `LogListHeader` and `LogEntryRow` to ensure perfect column alignment.
- Coerced minimum column width to 40dp to prevent columns from disappearing during resize.

**Gotchas**
- Resizing flexible columns (like "Message") requires assigning them an initial fixed width when the user starts dragging.
- Column alignment between header and rows is maintained by using the same `getColumnWidth` logic and layout constraints.
- `Modifier.weight(1f)` inside a `horizontalScroll` container results in 0-width columns; default fixed widths must be provided for unconstrained containers.

**Test coverage areas**
- `PersistenceIntegrationTest`: Added `should persist column widths` test case to verify end-to-end state preservation.
- Regression verification: Confirmed grid alignment and column visibility across standard and custom log formats.

## Task: Dynamic Log Entry Details
**Title**: Dynamic Details Pane Implementation
**Date/time completed**: 2026-05-15 11:35
**What was shipped**
- Redesigned `LogEntryDetails` to show dynamic fields based on the log entry's content.
- Conditional "Level" display: hide level if it is `UNKNOWN`.
- Automated field display: iterate through and show all custom fields from structured logs (e.g., Nginx, Apache).
- Enhanced "Content" box: only show if content is not blank and added support for multi-term highlighting.
- Integrated highlighting into the details pane to match the main log grid.

**Key decisions**
- Used the presence of fields in `LogEntry.fields` to automatically adapt the details pane to different log formats (e.g., Syslog, JSON, Apache).
- Optimized vertical space by hiding redundant or empty sections ("Level" when unknown, "Content" when blank).
- Passed `filterQueries` and `isDarkMode` to the details pane to ensure visual consistency with the main list.

**Gotchas**
- Adding new parameters to `LogEntryDetails` required updating the call site in `KLogViewerScreen` and importing `LogLevel` in the component file.
- `LogHighlighter.highlight` returns an `AnnotatedString` which is correctly handled by the `Text` composable, but it needs current theme context.

**Test coverage areas**
- UI: Verified through compilation and regression pass of existing integration tests.

## Task: UI Expansion for Last Column
**Title**: Auto-expanding Last Column for Better Content Visibility
**Date/time completed**: 2026-05-15 12:10
**What was shipped**
- Implemented auto-expansion for the last column in the log grid (typically "Message").
- Transitioned from fixed-width to `widthIn(min = defaultWidth)` for the last column, allowing it to grow based on the longest visible line.
- Updated `LogListHeader` to accept a `Modifier` and use `fillMaxWidth()` to maintain alignment with expanded rows.
- Enhanced `LogListHeader` to use `Modifier.weight(1f)` for the last column, ensuring the header background covers the full width of the scrollable area.

**Key decisions**
- Used `Modifier.widthIn(min = widthDp)` for the last column in `LogEntryRow` to allow natural text width to determine the row's width in a horizontal scroll container.
- Applied `Modifier.fillMaxWidth()` to both the header and the `LazyColumn` within the shared `Column` parent of the `horizontalScroll`, causing them to share the width of the widest visible child.
- Maintained fixed widths for all manually resized columns (including the last one if dragged) to respect user overrides.

**Gotchas**
- Using `weight(1f)` in a `horizontalScroll` container only works if the parent has a width derived from another child or a fixed constraint. By making the header `fillMaxWidth()` and the rows content-determined, the header expands to match the longest row.
- Selection highlights in `LogEntryRow` might have slightly different widths if rows vary significantly in length, but this is a common and acceptable pattern in log viewers.

**Test coverage areas**
- UI: Verified through project build and code inspection of layout constraints.

## Task: Level Name Preservation
**Title**: Preserving Original Level Names in UI
**Date/time completed**: 2026-05-15 13:10
**What was shipped**
- Updated `SimpleLogParser` and `TemplateLogParser` to store the exact string captured from the log file (e.g., `INF`, `[INF]`, `DBUG`) in the `fields["level"]` map.
- Refined the level promotion logic to re-add brackets `[]` when a level is promoted from the metadata group, ensuring the displayed value is faithful to the original line format.
- Updated `LogList` UI to prioritize the raw level string for display while maintaining existing color-coding based on the normalized `LogLevel`.
- Updated `LogEntryDetails` to show the raw level name without brackets, improving readability while honoring the "as they are" requirement.

**Key decisions**
- Decided to store the raw value in the `fields` map even for standard levels, allowing the UI to bypass the normalized enum name for display purposes.
- Maintained the use of `LogLevel` enum for internal logic (colors, filters) to ensure that `INF` and `INFO` are treated as the same category.
- Decided that recognized levels should still be displayed in brackets in the main grid for visual consistency, using the original text inside the brackets.

**Gotchas**
- When promoting a level from a bracketed metadata group, the brackets are often stripped by the regex; they must be explicitly re-added if they were intended to be part of the "raw" representation.

**Test coverage areas**
- Core: Updated `LogParserTest.kt` with explicit assertions for raw level strings in timezone-aware and metadata-heavy log lines.
- UI: Verified consistency between the grid and details pane through code inspection.

## Task: Domain Documentation - Ubiquitous Language
**Title**: Ubiquitous Language Formalization
**Date/time completed**: 2026-05-15 13:55
**What was shipped**
- Formal `docs/UBIQUITOUS_LANGUAGE.md` document.
- `ADR-022: Formalization of Ubiquitous Language`.
- Updated `RECAP.md` with the new documentation milestone.

**Key decisions**
- Centralized all domain terminology in a single, version-controlled markdown file to prevent "concept drift".
- Mandated alignment between code (e.g., `LogEntry`, `LogSource`) and the Ubiquitous Language via ADR.
- Chose "Interleaving" as the formal term for multi-log merging to align with industry standards and existing ADRs.

**Gotchas**
- Identifying all implicit terms (like "Update" vs "Packet") required a deep dive into both code and existing ADRs to ensure current usage was captured accurately.

**Test coverage areas**
- Documentation-only task; verified terminological alignment with existing domain model tests.

## Task: UI Robustness - Large Log Entry Handling
**Title**: Large Log Entry Handling
**Date/time completed**: 2026-05-15 14:40
**What was shipped**
- Truncation logic in `LogList` (10k chars) and `LogEntryDetails` (50k chars).
- Maximum width constraint (10k dp) for log columns in `LogList`.
- Resize protection cap (10k dp) for manual column dragging.

**Key decisions**
- Chose truncation as a primary defense to ensure both measurement safety and rendering performance.
- Selected 10,000.dp as a safe upper bound for Compose's bit-packed `Constraints` system while still allowing for very wide log lines.
- Decided to truncate in the UI layer to maintain full search/filter capabilities in the ViewModel layer.

**Gotchas**
- `horizontalScroll` with `softWrap = false` is a "perfect storm" for Compose crashes as it invites infinite measurement.
- Vertical overflow is also possible in the details pane if entries are extremely large (multi-megabyte single lines), requiring similar truncation there.

**Test coverage areas**
- Build verification and manual inspection of truncation logic.

## Task: Multi-Log Visual Differentiation
**Title**: Multi-Log Visual Differentiation
**Date/time completed**: 2026-05-15 15:20
**What was shipped**
- Colored badges (dots) in the gutter for source identification.
- Pale grey background shading based on `sourceId` for interleaved entries.
- Dynamic gutter sizing (50dp/60dp) based on source count.
- Path-based `sourceId` in `FileLogSource` for reliable differentiation.

**Key decisions**
- Used full paths for `sourceId` to ensure uniqueness across different directories.
- Implemented dynamic gutter width to keep the UI clean when viewing only a single log.
- Selected subtle grey shades to provide grouping without competing with level-based text colors.

**Gotchas**
- Changing `sourceId` from filename to full path requires updating all tests that assert on this field.
- Misalignment between header and rows can easily occur if gutter width is not kept in sync.

**Test coverage areas**
- `MergedLogSourceTest` (unit)
- `InterleavingIntegrationTest` (integration)
- `LogSortingTest` (integration)

## Task: Full-Width Log Entry Backgrounds
**Title**: Full-Width Log Entry Backgrounds
**Date/time completed**: 2026-05-15 15:45
**What was shipped**
- Full-width background coverage for log entries in `LogList`.
- Automatic expansion of the "Message" column to fill viewport width.

**Key decisions**
- Applied `Modifier.fillMaxWidth()` to both the container `Box` and the child `Row` of each log entry to ensure they stretch to the full width provided by the `LazyColumn` (which itself fills the scrollable viewport).
- Added `Modifier.weight(1f)` to the final column to ensure it matches the header's layout logic, preventing the background from terminating at the end of the text.

**Gotchas**
- In a `horizontalScroll` context, `fillMaxWidth()` on an item inside `LazyColumn` results in the item being as wide as the viewport or the widest item, ensuring full-width backgrounds even if the content is short.

**Test coverage areas**
- Build verification and UI alignment inspection.

## Task: Fix Message Column Visibility
**Title**: Fix Message Column Visibility
**Date/time completed**: 2026-05-15 16:00
**What was shipped**
- Fixed regression where "Message" column was hidden by `weight(1f)` squashing.
- Dynamic layout in `LogList` that respects both viewport width and content width.
- Use of `BoxWithConstraints` to ensure background colors cover the full viewport even with short content.

**Key decisions**
- Used `widthIn(min = viewportWidth)` instead of `fillMaxWidth()` to allow expansion beyond viewport.
- Replaced `weight(1f)` with manual `minWidth` calculation for the last column to prevent squashing.
- Maintained 10,000dp safety cap for horizontal measurement.

**Gotchas**
- `fillMaxWidth()` inside `horizontalScroll` restricts width to viewport, which is fatal for weighted columns when fixed columns already consume significant space.

**Test coverage areas**
- `InterleavingIntegrationTest`
- `LogSortingTest`

## Task: Synchronized Row Widths
**Title**: Synchronized Row Widths for Full-Width Backgrounds
**Date/time completed**: 2026-05-15 16:20
**What was shipped**
- Dynamic row width synchronization in `LogList`.
- Consistent background coverage for short rows in horizontally scrolled views.

**Key decisions**
- Used `onSizeChanged` on each row to track the maximum width encountered during composition.
- Applied `max(viewportWidth, widestSeenWidth)` as a minimum width constraint for both rows and the header.
- Passed `widestRowWidth` state down to children to ensure immediate alignment upon recomposition.

**Test coverage areas**
- `InterleavingIntegrationTest`
- `LogSortingTest`

## Task: Automatic Scrolling
**Title**: Automatic Scrolling (Log Tailing)
**Date/time completed**: 2026-05-15 15:20
**What was shipped**
- Automatic scrolling to the end of the log list when new entries are added.
- Persistent "Auto-scroll" toggle in the toolbar for user control.

**Key decisions**
- Used `LaunchedEffect(logs.size)` in `LogList` to trigger scroll actions whenever the log count changes.
- Chose `scrollToItem` for immediate jumping to the end, ensuring users always see the latest logs when auto-scroll is active.
- Integrated the toggle into `FilterBar` for easy access alongside other view controls.
- Persisted the `isAutoScrollEnabled` flag in `UserPreferences` to maintain user choice across application restarts.

**Gotchas**
- Initially used `animateScrollToItem`, but switched to `scrollToItem` to ensure performance and immediate feedback on high-frequency log updates.

**Test coverage areas**
- `PersistenceIntegrationTest` (verified state persistence)
- `InterleavingIntegrationTest` (verified no regressions in log merging)
- Manual UI verification for icon tinting and tooltip correctness.

## Task: Fix Library Upgrade Compilation Errors
**Title**: Fix Library Upgrade Compilation Errors
**Date/time completed**: 2026-05-15 18:05
**What was shipped**
- Fixed compilation error in `LogList.kt` by replacing deprecated `rememberRipple()` with the new `ripple()` API from Compose 1.7+.
- Fixed `AwtWindow` import in `KLogViewerScreen.kt` (moved from `androidx.compose.ui.window` to `androidx.compose.ui.awt`).
- Cleaned up redundant `else` branch in `when` expression in `KLogViewerScreen.kt` for better type safety.
- Restored missing BDD step definitions in `app/src/test/kotlin/com/klogviewer/bdd/steps/LogLoadingSteps.kt`.
- Added missing `kotlinx-coroutines-test` dependency to `app` module and version catalog.

**Key decisions**
- Used `UnconfinedTestDispatcher` in BDD tests to ensure predictable execution of log loading logic.
- Leveraged `backgroundScope` in `runTest` to manage long-running coroutines like log tailing in tests.
- Re-implemented BDD steps to use `FileLogSource` and `SimpleLogParser` directly for focused feature verification.

**Gotchas**
- Library upgrades (Compose 1.11.0, Kotlin 2.3.21) introduced strict deprecation-as-errors and moved AWT-related classes.
- Infinite loops in log tailing caused tests to hang unless properly managed with `backgroundScope` and test dispatchers.

**Test coverage areas**
- BDD Tests: Verified log loading and entry parsing via `RunCucumberTest`.
- Integration Tests: Verified multi-tab and interleaving logic.
- UI Compilation: Ensured all composables compile with the latest versions.

## Task: README Spruce-up
**Title**: README Spruce-up
**Date/time completed**: 2026-05-15 17:40
**What was shipped**
- Created a comprehensive `README.md` in the root directory.
- Added professional badges (Kotlin, Compose for Desktop, License).
- Highlighted core application features: multiple tabs, horizontal split panes, interleaved logs, real-time tailing, and advanced parsing.
- Included placeholders and descriptions for screenshots in `docs/images/screenshots`.
- Provided clear "Getting Started" instructions for running and building the application.

**Key decisions**
- Chose a professional, "senior project manager" tone for the README.
- Organized features into logical sections to highlight the desktop-centric nature of KLogViewer.
- Included a technology stack section to provide immediate context for developers.

**Gotchas**
- The project lacked a root README, so it was created from scratch using information gathered from `docs/RECAP.md` and `docs/FEATURES.md`.

**Test coverage areas**
- N/A (Documentation change).

## Task: Project Renaming
**Title**: Project Renaming to KLogViewer
**Date/time completed**: 2026-05-15 17:30
**What was shipped**
- Renamed project to KLogViewer in `settings.gradle.kts` and `app/build.gradle.kts`.
- Refactored package structure from `com.logviewer` to `com.klogviewer` across all modules.
- Renamed core classes and components: `LogViewerViewModel` -> `KLogViewerViewModel`, `LogViewerState` -> `KLogViewerState`, etc.
- Updated all references in code, tests, and documentation.
- Renamed internal log files and preference keys where applicable.

**Key decisions**
- Chose "KLogViewer" as the new name to highlight the Kotlin-first nature of the project.
- Performed a deep rename of packages and symbols to ensure full consistency and avoid legacy "LogViewer" references in the codebase.
- Maintained "log viewer" as a descriptive term in prose while using "KLogViewer" as the product name.

**Gotchas**
- Renaming packages required updating `build.gradle.kts` files and all import statements across the project.
- AWT `FileDialog` and window titles needed explicit updates to the new brand.

**Test coverage areas**
- Build verification: Ensured all modules compile and link correctly under the new name.
- Integration/BDD tests: Verified that all tests pass with the new package structure.

## Task: Show FQN in Recent Items
**Title**: Show FQN in Recent Items
**Date/time completed**: 2026-05-15 22:15
**What was shipped**
- Updated the "Recently Opened" menu in `Main.kt` to display the Fully Qualified Name (full path) instead of just the filename.
- Ensured consistent use of full paths across `Main.kt` and `RecentItemsDialog`.

**Key decisions**
- Switched from `File(path).name` to raw `path` in the `MenuBar` implementation to fulfill the FQN requirement.
- Verified that existing components like `StatusBar` and `RecentItemsDialog` already provided full path information.

**Test coverage areas**
- Integration tests: `PersistenceIntegrationTest` and `TabManagementTest` verified that path-based state management remains correct.
- Manual verification: Checked menu rendering logic.

## Task: CI/CD and Packaging
**Title**: CI/CD and Packaging with GitHub Actions
**Date/time completed**: 2026-05-15 22:30
**What was shipped**
- Created `.github/workflows/build.yml` for automated multi-platform builds.
- Configured matrix strategy for macOS, Windows, and Linux.
- Automated generation of installers (DMG, MSI, DEB) and standalone executables (zipped app images).
- Updated `README.md` with Build Status, Platforms, and CI/CD documentation.
- Marked task as completed in `notes.md`.

**Key decisions**
- Used `ubuntu-latest`, `windows-latest`, and `macos-latest` to ensure native packaging for each target OS.
- Zipped the `createDistributable` output to provide "plain executables" as requested.
- Linked to GitHub Actions artifacts in the README for easy access to deployable units.

**Gotchas**
- Windows zipping requires `Compress-Archive` in PowerShell (`pwsh`) for a seamless CI experience.
- The `package` task for Compose for Desktop is OS-specific, requiring the matrix build approach.

**Test coverage areas**
- CI Pipeline: Verified that `gradle test` runs before packaging on all platforms.
- Packaging: Verified that `package` and `createDistributable` tasks are correctly invoked for each platform.

## Task: Fix ScrollableTabRow IndexOutOfBoundsException (Refined)
**Title**: Fix ScrollableTabRow IndexOutOfBoundsException (Refined)
**Date/time completed**: 2026-05-17 17:15
**What was shipped**
- Implemented a custom safe `indicator` for `ScrollableTabRow` that performs bounds checking against `tabPositions`.
- Maintained defensive indexing (`coerceIn`) for the `selectedTabIndex` calculation.
- Ensured proper imports for `TabRowDefaults.tabIndicatorOffset`.

**Key decisions**
- Provided an explicit `indicator` lambda to `ScrollableTabRow` to prevent the default indicator from accessing out-of-sync `tabPositions`.
- Used `if (selectedTabIndex < tabPositions.size)` check inside the indicator to gracefully handle frames where the index and positions are not yet aligned.

**Gotchas**
- Simple defensive indexing of the `selectedTabIndex` is insufficient because Compose's internal `TabRow` logic may use the new index with old children measurements during a single frame of recomposition.

**Test coverage areas**
- `TabManagementTest`: Verified that tab switching and addition still work correctly.
- UI Compilation: Verified that custom indicator and imports are correct.

## Task: Fix Split Pane Column Resizing
**Title**: Fix Split Pane Column Resizing
**Date/time completed**: 2026-05-17 17:45
**What was shipped**
- Updated `KLogViewerIntent.UpdateColumnWidth` to include `windowId`, enabling targeted resizing.
- Implemented `updateWindow` helper in `KLogViewerState` to allow updating any window by ID across all tabs.
- Refactored `KLogViewerScreen` to pass the specific window ID to the resize intent.
- Verified that resizing a column in a non-focused split pane correctly updates that pane without affecting the focused one.

**Key decisions**
- Decoupled column resizing from focus management to provide a more intuitive user experience.
- Added a general `updateWindow` method to the state to simplify future targeted window updates.

**Gotchas**
- Changing intent constructors requires updating all call sites, including integration tests that mock or simulate user intents.

**Test coverage areas**
- `TabManagementTest`: Added `should resize columns independently in different split windows`.
- `PersistenceIntegrationTest`: Updated to verify that resizing still persists correctly with the new intent structure.

## Task: Add 'All' option for Level Filtering
**Title**: Add 'All' option for Level Filtering
**Date/time completed**: 2026-05-17 18:00
**What was shipped**
- Implemented `ToggleAllLevels` intent to allow bulk selection/deselection of log levels.
- Updated `Sidebar.kt` to include an "All" toggle at the top of the levels list.
- Refactored `LogLevelToggle` in `Sidebar.kt` to be more flexible, supporting both individual levels and the "All" option.
- Synchronized "All" checkbox state with the current selection (checked only if all levels are enabled).

**Key decisions**
- "All" checkbox toggles to enabled if any levels are currently disabled, and toggles to disabled only if all are currently enabled.
- Reused the existing custom checkbox and row style for the "All" option to maintain UI consistency.

**Gotchas**
- The "All" option's count reflects the total number of logs in the window, which is the sum of counts for all individual levels.

**Test coverage areas**
- `TabManagementTest`: Added `should toggle all levels at once` to verify the logic for bulk selection and deselection.

## Task: Enhance Active Window Visibility
**Title**: Enhance Active Window Visibility
**Date/time completed**: 2026-05-17 18:15
**What was shipped**
- Added a subtle left border (3dp width, primary color with 50% alpha) to the active window when multiple windows are open in a tab.
- Used `Modifier.drawBehind` for efficient rendering of the targeted border.

**Key decisions**
- Chose a subtle color and width to improve focus without being visually overwhelming.
- Only show the border when there are multiple windows (split view) to avoid clutter in single-window tabs.

**Gotchas**
- `Modifier.drawBehind` requires explicit imports for `Offset` and `drawBehind` which might not be automatically suggested in some environments.

**Test coverage areas**
- UI Compilation: Verified that the new modifier and imports are correct and build successfully.

## Task: Refine Window Activation and Selection
**Title**: Refine Window Activation and Selection
**Date/time completed**: 2026-05-17 18:45
**What was shipped**
- Modified `KLogViewerScreen.kt` to decouple window activation from entry selection.
- Clicking on a log entry in a non-active window now only triggers `SwitchWindow`.
- Clicking on a log entry in an already active window triggers `SelectEntry` to show details.

**Key decisions**
- Improved user experience in split-pane mode by preventing accidental detail panel opening when just trying to focus a window.
- Leveraged the existing `isWindowActive` local state in the Composable to guard the `SelectEntry` intent.

**Gotchas**
- The selection in the window remains unchanged (or null) when clicking to activate, requiring a second click to view details.

**Test coverage areas**
- UI Logic: Manually verified the click handling logic in `KLogViewerScreen.kt`.
- Regressions: Ran `TabManagementTest` to ensure general window and tab management still works.

## Task: Add Keyboard Shortcuts and Multi-selection
**Title**: Add Keyboard Shortcuts and Multi-selection
**Date/time completed**: 2026-05-17 17:00
**What was shipped**
- Standard shortcuts: Cmd+W (Close Tab), Cmd+N (New Tab), Cmd+C (Copy).
- Multi-selection support: Shift+Click for range selection and Cmd/Ctrl+Click for toggling selection.
- Clipboard integration: Selected lines are joined with newlines and copied to the system clipboard.
- Refactored `LogWindow` to use indices for selection to handle multi-select and avoid equality issues.

**Key decisions**
- Used `MenuBar` shortcuts for standard OS integration and discoverability.
- Used `PointerEventType.Release` in `LogList` to detect modifiers during clicks without breaking ripple effects entirely.
- Indices are used for selection instead of `LogEntry` objects to better support range selection.

**Gotchas**
- Range selection (Shift+Click) requires tracking the `lastSelectedIndex` to define the anchor.
- `Modifier.clickable` swallowed modifiers, necessitating a move to `Modifier.pointerInput` for click detection in rows.

**Test coverage areas**
- `TabManagementTest`: Added `should support multi-selection via ToggleEntrySelection` to verify the logic.
- Integration: Verified `MenuBar` shortcuts compilation and wiring.

## Task: ANSI SGR Color Support
**Title**: ANSI SGR Color Support
**Date/time completed**: 2026-05-17 19:15
**What was shipped**
- ANSI SGR Parsing: Implemented a stateful ANSI parser in `LogHighlighter` that handles foreground colors (30-37, 90-97), bold (1), and resets (0).
- UI Toggle: Added a 'Palette' icon to the `FilterBar` to enable or disable ANSI color interpretation per window.
- Persistence: Saved the `showAnsiColors` preference in `UserPreferences`.
- Details Pane Support: Integrated ANSI colors into the log details view.

**Key decisions**
- Decided to strip ANSI codes from the visible text when colors are enabled, ensuring a clean reading experience while applying styles via `AnnotatedString`.
- Used a dedicated `Palette` icon in the `FilterBar` for easy access alongside other view-related toggles.

**Gotchas**
- ANSI codes must be parsed before other highlights (like timestamps or filter queries) to ensure the indices for those highlights remain correct after the codes are stripped.

**Test coverage areas**
- `LogHighlighterTest`: Added tests for ANSI parsing with both enabled and disabled states, verifying string stripping and style application.

## Task: UI Testing Spike Planning
**Title**: UI Testing Strategy & Roadmap
**Date/time completed**: 2026-05-17 19:30
**What was shipped**
- `spike/uitesting` git branch.
- `docs/sprints/sprint-ui-testing-spike.md`: Comprehensive strategy for UI testing.
- `docs/tasks/TASKS-UI-TESTING-SPIKE.md`: Structured task list for the spike.
**Key decisions**
- Adopted the **Robot Pattern** for maintainable UI tests (ADR-024).
- Selected **ComposeTestRule** (JUnit 4) as the primary testing framework (ADR-023).
- Committed to headless CI/CD execution using Xvfb.
- Decided to introduce a `DialogProvider` abstraction to facilitate mocking of blocking AWT calls like `FileDialog`.
**Gotchas**
- Existing Cucumber BDD tests are limited to the ViewModel layer and do not exercise the actual Compose UI components or AWT integrations.

## Task: UI Testing Spike - Part 1 & 2
**Title**: UI Testing Infrastructure and Pattern Definition
**Date/time completed**: 2026-05-18 21:45
**What was shipped**
- ADR-023: UI Testing Framework Selection.
- ADR-024: Robot Pattern for UI Testing.
- Added UI testing dependencies to `libs.versions.toml` (`ui-test-junit4`, `junit4`).
- Configured `:ui:desktopTest` task in Gradle.
- Integrated Xvfb setup and automated UI test execution in GitHub Actions CI/CD workflow.
**Key decisions**
- Standardized on `androidx.compose.ui:ui-test-junit4` despite being a Desktop project to ensure compatibility with standard Robot patterns.
- Introduced a dedicated `desktopTest` Gradle task to isolate UI tests from faster unit tests.
- Opted for `xvfb-run` in CI to ensure stable headless execution for Compose Desktop.
**Gotchas**
- Compose UI tests on Desktop still require JUnit 4, necessitating a dual-JUnit environment (JUnit 5 for unit tests, JUnit 4 for UI tests).
- Xvfb requires additional system libraries (Mesa, GLX) on standard Ubuntu runners.
**Test coverage areas**
- Infrastructure: Verified CI workflow syntax and Gradle task availability.
- Pattern: Defined the roadmap for `BaseRobot` and specific UI Robots in ADR-024.

## Task: UI Testing Spike - Part 3 & 4.1
**Title**: Robot Pattern Implementation and Smoke Testing
**Date/time completed**: 2026-05-18 21:40
**What was shipped**
- `BaseRobot`, `LogListRobot`, `SidebarRobot`, and `MainRobot` for fluent UI testing.
- `DialogProvider` abstraction and `AwtDialogProvider` implementation to allow mocking of AWT `FileDialog`.
- `testTag` decorations across `LogList`, `Sidebar`, `FilterBar`, and `KLogViewerScreen`.
- `KLogViewerSmokeTest` verifying application launch and component presence.
- Added `MockK` and `JUnit Vintage` dependencies for UI testing support.
**Key decisions**
- Switched to `org.jetbrains.compose.ui:ui-test-junit4` for compatibility with Compose Desktop.
- Introduced `DialogProvider` to the `KLogViewerScreen` via dependency injection (defaulting to AWT) to enable headless testing of file operations.
- Decided to use JUnit 4 for UI tests while maintaining JUnit 5 for unit tests, enabled by the Vintage engine.
**Gotchas**
- `androidx.compose.ui:ui-test-junit4` contains stubs that throw `NotImplementedError` on Desktop; the JetBrains-specific version must be used.
- Lazy components like `LazyColumn` require careful assertion of child counts as only visible items are typically present in the semantics tree.
**Test coverage areas**
- UI: Verified main screen components are displayed on launch.
- Pattern: Verified Robot DSL works as intended in the smoke test.

## Task: UI Testing Expansion - Complex Behaviors
**Title**: Extended UI Test Suite for Split Panes and Multi-selection
**Date/time completed**: 2026-05-19 10:40
**What was shipped**
- Extended UI testing framework to cover complex UI behaviors (splits, multi-selection, resizing).
- Added `WindowRobot` to the Robot Pattern for scoped interactions within specific split windows.
- Implemented `KLogViewerComplexUiTest.kt` with tests for Independent Column Resizing, Multi-selection (Shift/Meta), and Tab switching.
- Enhanced `LogList.kt` with `selected` semantics and `column_header_$column` tags for better testability.
- Added comprehensive guidance in `docs/TESTING.md` comparing Functional UI Tests and Visual Regression.
**Key decisions**
- Scoped robots using `hasAnyAncestor(hasTestTag("window_$windowId"))` to ensure interactions target the correct pane in split views.
- Used `performKeyInput` to simulate modifier keys (Shift, Meta) during mouse clicks for multi-selection tests.
- Implemented custom `assertWidthIsNotEqualTo` and `SemanticsMatcher` for identifying windows to overcome limitations in standard `androidx.compose.ui.test` matchers.
- Decided to stick with Functional UI tests for resizing logic instead of screenshots, as they are more robust and directly verify model state updates.
**Gotchas**
- Compose Testing's `click()` in `performMouseInput` does not consistently support `keyboardModifiers` across all versions; manual `keyDown/Up` via `performKeyInput` is more reliable.
- Some nodes in the LogList (headers, handles) require `useUnmergedTree = true` to be found because they are part of complex, non-interactive semantics nodes.
- `onNodeWithTag` with `substring = true` is not available in the current Compose version; used a custom `SemanticsMatcher` with `startsWith` instead.
**Test coverage areas**
- UI: Independent column resizing in split panes.
- UI: Multi-selection with Shift and Meta/Ctrl modifiers.
- UI: Persistent state maintenance during tab switching.

## Sprint: Recursive Directory Loading
**Title**: Sprint 5 Completion
**Date/time completed**: 2026-05-19 11:45
**What was shipped**
- `DirectoryScanner` for recursive log discovery with glob pattern support.
- `DirectoryLogSource` for real-time merging and monitoring of entire directory trees.
- Native "Open Directory" support with workspace persistence for directory paths.
- Small circle source badges with tooltips showing the full source path in the log gutter for multi-source identification.
- Dynamic `sourceIds` tracking in `KLogViewerViewModel`.
- JSON schema auto-detection and key mapping integrated with directory loading.
- Robust missing file handling with "File Not Found" confirmation dialog and history pruning.

**Key decisions**
- Chose small circles with tooltips over textual badges to keep the gutter compact while still providing full source information.
- Decoupled `DirectoryScanner` from `LogSource` for better testability.

**Gotchas**
- Initially missed handling nullable `sourceId` in the ViewModel's unique ID extraction logic, causing a compilation error (fixed).
- Native directory selection on macOS requires toggling the `apple.awt.fileDialogForDirectories` system property.

**Test coverage areas**
- `DirectoryScannerTest`: Recursive discovery and glob filtering.
- `DirectoryLogSourceTest`: Initial merge of multiple files within a directory.
- `InterleavingIntegrationTest`: Multi-source merging and chronological sorting.

## Task: Fix GitHub Actions Build Failure (Mesa/GLX)
**Title**: Update Linux Dependencies for GitHub Actions
**Date/time completed**: 2026-05-19 12:40
**What was shipped**
- Updated `.github/workflows/build.yml` to replace obsolete Mesa package names with their modern equivalents.
- Replaced `libegl1-mesa`, `libgles2-mesa`, and `libgl1-mesa-glx` with `libegl1`, `libgles2`, and `libgl1`.
**Key decisions**
- Switched to generic library names (`libgl1`, etc.) which are standard in Ubuntu 22.04+ (used by `ubuntu-latest`).
**Gotchas**
- Recent Ubuntu versions have restructured the Mesa package distribution, removing the `-mesa` suffix from many library packages and obsoleting `libgl1-mesa-glx`.
**Test coverage areas**
- CI Pipeline: Resolved the `apt-get install` failure on Linux runners.

## Task: Fix GitHub Actions Build Failure (Windows Shell)
**Title**: Fix PowerShell Syntax Error on Windows CI
**Date/time completed**: 2026-05-19 12:45
**What was shipped**
- Updated `.github/workflows/build.yml` to set `shell: bash` as the default for the build job.
- This ensures that Bash-style `if` statements and `./gradlew` calls work consistently across Linux, macOS, and Windows.
**Key decisions**
- Chose to set a job-level default shell to minimize boilerplate and ensure all current and future scripts use a consistent Bash environment.
**Gotchas**
- GitHub Actions defaults to PowerShell (`pwsh`) on Windows runners, which is incompatible with Bash syntax like `if [ ... ]; then`.
**Test coverage areas**
- CI Pipeline: Resolved the `ParserError` on Windows runners.

## Task: Fix UI Test Failures (CI/CD)
**Title**: Fix UI Test Timeouts and Assertions
**Date/time completed**: 2026-05-19 13:00
**What was shipped**
- Updated `KLogViewerUiTest.kt` and `KLogViewerComplexUiTest.kt` to use real temporary files, ensuring they pass the ViewModel's existence checks.
- Refactored `KLogViewerScreen.kt` to use `LaunchedEffect` for dialog handling, improving reliability and idiomatic Compose usage.
- Resolved `ComposeTimeoutException` caused by empty log lists and `AssertionError` caused by clicking non-existent rows.
**Key decisions**
- Switched to `LaunchedEffect(pendingDialog)` to ensure dialog logic runs correctly in response to state changes and doesn't block the main thread unnecessarily.
- Used `File.createTempFile` in tests to guarantee file existence without relying on hardcoded paths.
**Gotchas**
- The ViewModel's `loadFilesIntoWindow` returns early if files do not exist, which was causing tests to wait for UI elements that would never appear.
**Test coverage areas**
- UI: Verified `KLogViewerUiTest` and `KLogViewerComplexUiTest` pass locally with the new infrastructure.

## Task: Fix Intermittent Test Failures (CI/CD) - Resource Leaks & Race Conditions
**Title**: Fix File Locking and Race Conditions in Integration Tests
**Date/time completed**: 2026-05-19 21:55
**What was shipped**
- Updated `KLogViewerViewModel` to use `cancelAndJoin()` for log observation jobs, ensuring that background file-tailing jobs are fully stopped before starting new ones or finishing tests.
- Updated `FileLogSource` to rethrow `CancellationException`, preventing unnecessary error logging and ensuring clean coroutine termination.
- Enhanced `RecentItemsTest.kt` and `PersistenceIntegrationTest.kt` with robust `tearDown` methods that wait for background resource release.
- Fixed a race condition in `PersistenceIntegrationTest.kt` by updating the preference save wait loop to verify actual data presence rather than just tab count.
**Key decisions**
- Used `cancelAndJoin()` instead of just `cancel()` to avoid file handle leaks on Windows where background jobs might still hold a `RandomAccessFile` lock.
- Added explicit `delay` in tests to provide a buffer for OS-level file releases, which is sometimes necessary on Windows runners.
**Gotchas**
- The `isLoading` state flag in `KLogViewerViewModel` was being set before `savePreferences()` was called, leading to a race condition where tests would check the saved preferences file before it was written.
- JUnit 5's `@TempDir` cleanup was failing on Windows because `RandomAccessFile` holds a hard lock that persists until the coroutine reaches its next suspension point (like `delay`).
**Test coverage areas**
- Integration: `PersistenceIntegrationTest`, `RecentItemsTest`.
- Core: `FileLogSource` (cancellation safety).

## Task: Code Review Fixes
**Title**: Address Code Review Feedback
**Date/time completed**: 2026-05-19 14:05
**What was shipped**
- Fixed non-exhaustive `when` expressions and statements in `KLogViewerScreen.kt` by adding `else` branches.
- Removed redundant `@OptIn(ExperimentalTestApi::class)` annotations in `KLogViewerComplexUiTest.kt`.
**Key decisions**
- Used `else` branches in `when` blocks to satisfy exhaustiveness requirements in stricter Kotlin environments, while accepting redundant code warnings in others.
**Gotchas**
- Kotlin's smart-casting can make `when` appear exhaustive to some compilers but not others, especially when used in combination with nullability and enums.

## Task: Update Deprecated Testing API
**Title**: Migrate from `createComposeRule()` to `runComposeUiTest`
**Date/time completed**: 2026-05-19 13:55
**What was shipped**
- Replaced deprecated `createComposeRule()` (JUnit 4 rule) with `androidx.compose.ui.test.v2.runComposeUiTest` (functional API) in all UI tests.
- Updated `BaseRobot` and all derived robots (`MainRobot`, `LogListRobot`, `SidebarRobot`, `WindowRobot`) to use the `ComposeUiTest` interface.
- Migrated `KLogViewerUiTest.kt`, `KLogViewerComplexUiTest.kt`, and `KLogViewerSmokeTest.kt` to the new testing pattern.
**Key decisions**
- Decided to use the `v2` version of `runComposeUiTest` to align with the latest Compose recommendations and ensure future compatibility.
- Added `@OptIn(ExperimentalTestApi::class)` to handle the experimental status of the new testing API.
**Gotchas**
- The new `runComposeUiTest` API has a slightly different signature for some methods like `waitUntil` (parameter order changed), which required minor adjustments in `BaseRobot.kt`.
- `ComposeUiTest` is still experimental, necessitating opt-in annotations.
**Test coverage areas**
- UI: All desktop UI tests in `:ui` module (15 tests total).


## Sprint: Advanced Log Formats & Flexible Parsing
**Title**: Sprint 7 Completion
**Date/time completed**: 2026-05-19 15:30
**What was shipped**
- Template-based parsing engine with regex support and named capture groups.
- `ParserRegistry` with default templates for Standard, Syslog, Apache, ISO8601, and CSV.
- `MultilineProcessor` for aggregating stack traces and indented logs.
- `JsonLogParser` with automatic field mapping and structured data support.
- `HeuristicProbe` for automatic detection of log formats and column layout.
- UI Indication: Displayed the active parser name in the `StatusBar`.
- UI Selection: Added a dropdown menu with an arrow indicator to the `StatusBar` for manual parser selection/confirmation.
- Enhanced `LogEntryDetails` with monospace formatting and high-density field display.

**Key decisions**
- Used `ProbeResult` to carry both the parser and its human-readable name for UI display.
- Implemented `ChangeParser` intent to allow users to override heuristic detection.
- Decided to reuse `HeuristicProbe` logic for JSON mapping even when manually selected to ensure correct columns.

**Gotchas**
- Changing the `ProbeResult` data class required updating several test cases to include the `parserName` assertion.
- `StatusBar` height (24dp) is tight for `DropdownMenu` triggers; ensured the click target covers the full text area.

**Test coverage areas**
- `LevelMapper`: Comprehensive normalization tests.
- `HeuristicProbe`: Detection accuracy for JSON, Syslog, and Standard formats.
- `MultilineProcessor`: Buffer limits and header detection.
- `JsonLogParser`: Mapping and nested object serialization.
- `TemplateLogParser`: Capture group extraction and level fallback logic.

## Task: Sprint 8 Initiation - Connectivity & Remote Sources
**Title**: Sprint 8 Initiation
**Date/time completed**: 2026-05-20 07:50
**What was shipped**
- Created `feature/connectivity` branch.
- Initialized Sprint 8 task list in `docs/tasks/TASKS-SPRINT-8-CONNECTIVITY.md`.
- Defined hierarchical tasks for SFTP/SSH, S3, and Network log sources.

**Key decisions**
- Followed the existing pattern of sprint-specific task files.
- Aligned task structure with ADR 010 and the sprint 8 scope document.

**Gotchas**
- None.

**Test coverage areas**
- N/A (Project management and documentation phase).

## Task: SFTP Remote Log Source Implementation
**Title**: SFTP Implementation & Documentation
**Date/time completed**: 2026-05-20 11:15
**What was shipped**
- `SftpLogSource` for real-time log tailing over SSH.
- Support for Password and Key-Pair authentication.
- Key-Pair support with optional passphrase for encrypted private keys.
- `SftpConnectionDialog` UI for connecting to remote servers.
- Improved error handling for SFTP: detects non-existent files and remote command failures.
- Updated `README.md` with detailed SFTP usage instructions and key file requirements.
- Standardized `LogFailure` with a `message` property for better UI error reporting.

**Key decisions**
- Used `sshj` for SSH/SFTP implementation due to its modern API and Ed25519 support.
- Implemented `tail -f` over SSH to achieve efficient real-time updates for remote files.
- Added optional passphrase support to handle encrypted private keys (ADR-010 refinement).
- Implemented exit status and stderr monitoring for remote commands to provide clear feedback on failures.
- Ensured `Initial` load is always emitted (even if empty) to correctly manage UI state (e.g., hiding spinners).
- Documented that users must select the **private** key file (not public) in the connection dialog.

**Gotchas**
- `sshj`'s `loadKeys` throws `NullPointerException` if a null passphrase is passed to the two-argument version; solved by conditionally calling the single-argument overload.
- `tail -f` might hang or exit without output if the file is missing; needed to monitor `exitStatus` and `stderr`.
- Integration tests for SFTP can be slow; used `MockK` and `PipedInputStream` for fast, reliable unit testing of the streaming logic.

**Test coverage areas**
- `SftpLogSourceTest`: Unit tests for connection, password/key auth (with/without passphrase), error handling (invalid paths), and real-time log streaming from empty/non-empty files.

## Task: Dialog Focus Management (UX Refinement)
**Title**: Dialog Focus Management
**Date/time completed**: 2026-05-20 11:35
**What was shipped**
- Explicit Tab and Shift+Tab navigation in all `AlertDialog` based dialogs.
- Initial focus for the first input field or primary button when dialogs open.
- Enhanced `SftpConnectionDialog` with `FocusManager` and `KeyboardActions`.
- ADR-025 documenting the dialog focus management strategy.

**Key decisions**
- Used `onPreviewKeyEvent` on root dialog containers to capture and manually move focus with `FocusManager.moveFocus()`.
- Implemented `FocusRequester` for consistent initial focus across different dialog types.
- Standardized `ImeAction.Next` and `onNext` actions for all `TextField` components in dialogs.

**Gotchas**
- In `RecentItemsDialog`, focus should only be requested on the first item of the first non-empty list (Files or Directories).

**Test coverage areas**
- N/A (UI behavior refinement, manual verification required).

## Task: SFTP Connection Management
**Title**: SFTP Connection Management
**Date/time completed**: 2026-05-20 11:45
**What was shipped**:
- Persistent storage for multiple SFTP connection profiles in User Preferences.
- UI dropdown in `SftpConnectionDialog` for selecting and managing (loading/deleting) saved connections.
- "Save" functionality to persist current connection details for future use.
**Key decisions**:
- Made `SftpConfig` and related types serializable to simplify persistence in the existing `PreferencesRepository`.
- Added a `name` field to `SftpConfig` as the primary identifier for saved connections.
**Gotchas**:
- Adding a new field to the beginning of a data class constructor broke existing test cases and call sites; required manual update of all `SftpConfig` instantiations.
- `IconButton` inside `DropdownMenuItem` requires careful handling to ensure deletion doesn't accidentally trigger a connection attempt (though Compose standard behavior typically handles this).
**Test coverage areas**:
- `PreferencesRepositoryTest`: Verified saving and loading of multiple SFTP connection profiles.
- `SftpLogSourceTest`: Verified existing functionality remains intact after model updates.

## Task: Tab and Status Bar Tooltips
**Title**: Tab and Status Bar Tooltips
**Date/time completed**: 2026-05-20 17:15
**What was shipped**:
- Hover tooltips for all log tabs showing the full file path(s).
- Hover tooltips for the status bar file path.
**Key decisions**:
- Used the existing `TooltipWrapper` component for consistency across the UI.
- For tabs with multiple windows or merged sources, the tooltip joins all relevant paths with newlines.
**Gotchas**:
- `TooltipArea` can sometimes conflict with child clickable elements, but wrapping only the text label in the status bar and tab row avoided these issues.
**Test coverage areas**:
- Manual verification of tooltip rendering and content.

## Task: CI Stability & State Management Fixes
**Title**: CI Stability and Directory State Management Fixes
**Date/time completed**: 2026-05-20 21:30
**What was shipped**:
- Fixed a bug in `KLogViewerViewModel` where deleted files within a monitored directory were not correctly identified as missing in the UI state.
- Hardened `SftpLogSourceTest` to eliminate race conditions in the `Initial` load detection by ensuring data is written to the pipe before observation starts.
- Added explicit SSH `exitStatus` mocking in tests to ensure clean flow termination and robust state verification.
**Key decisions**:
- Standardized `missingSourceIds` updates in `handleLogUpdate` to always include missing sources, regardless of whether they are primary or directory sub-sources.
- Adopted pre-observation data seeding in SFTP tests to guarantee stable `Initial` vs `Appended` update separation across different OS environments.
**Gotchas**:
- Linux CI runners were so fast that they could start the log observer before the test's background writer had put any data in the pipe, resulting in unexpected empty initial loads.
**Test coverage areas**:
- `FileDeletionTest`: Verified directory sub-source deletion detection and state update.
- `SftpLogSourceTest`: Robustness verification of remote log tailing logic.

## Task: Refined Missing File Handling
**Title**: Refined Missing File Handling
**Date/time completed**: 2026-05-20 22:30
**What was shipped**:
- Removed the intrusive "File Not Found" dialog that appeared during session restoration or when opening missing files.
- Implemented a more seamless UX where missing files are immediately opened as red, strike-through tabs/windows without blocking the user with a dialog.
- Cleaned up obsolete `MISSING_FILE` dialog logic and associated state properties from `KLogViewerViewModel` and `KLogViewerState`.
**Key decisions**:
- Consolidated missing file indication into the standard window error/missing-source flow.
- Removed the automatic "Remove from List" prompt to prevent annoyance during app startup, relying instead on the existing "Clear Missing" functionality in the Recent Items dialog for history cleanup.
**Gotchas**:
- Removing the early return in `loadFilesIntoWindow` required ensuring that all downstream processors (parser, probe) gracefully handle empty input from missing files.
**Test coverage areas**:
- `SessionRestorationTest`: Verified that app startup with missing files correctly populates tabs with error states and no dialogs.
- `RecentItemsTest`: Updated to verify the new "open as missing" behavior when selecting from history.

## Task: SFTP Cancellation Deadlock Fix
**Title**: Reliable SFTP Stream Cancellation for Tab Add/Replace
**Date/time completed**: 2026-05-22 11:46
**What was shipped**:
- Refactored `SftpLogSource` cancellation handling to actively close input stream, command, and session when observation is cancelled.
- Reworked the remote read loop to avoid unbounded blocking when no data is ready, while preserving initial and appended update semantics.
- Bounded SSH client teardown with timeout-protected cleanup to avoid indefinite waits in `cancelAndJoin` paths.
- Added a regression test (`should cancel promptly when remote read is blocking`) that reproduces and protects against the deadlock.
**Key decisions**:
- Used a structured-concurrency cancellation watcher within `coroutineScope` to trigger resource closure immediately on parent job cancellation.
- Preserved non-cancellable cleanup but wrapped it in `withTimeoutOrNull` to prevent hangs during disconnect/close.
- Kept the existing parser/update pipeline intact to minimize behavioral change outside cancellation lifecycle boundaries.
**Gotchas**:
- `Job.invokeOnCompletion(onCancelling = true)` is an internal coroutines API in this setup; switched to a stable watcher coroutine pattern.
- A deterministic reproducer required a custom non-interruptible `InputStream` in tests to model real-world non-cooperative blocking reads.
**Test coverage areas**:
- `SftpLogSourceTest`: 7/7 passing including the new blocking-read cancellation regression.
- `LogLoadingIntegrationTest`: 2/2 passing (local load flow baseline unaffected).
- `SftpBrowsingTest`: 3/3 passing (SFTP connect/browse/add-to-workspace flows).
- `RecentItemsTest`: 3/3 passing (history/missing item behavior unchanged).
- `PersistenceIntegrationTest`: 4/4 passing (state restore and preference persistence unaffected).

## Task: ViewModel Refactoring and Tidying
**Title**: ViewModel Refactoring and Tidying
**Date/time completed**: 2026-05-22 13:30
**What was shipped**:
- Significant refactoring of `KLogViewerViewModel` (over 1200 lines) to improve maintainability.
- Extracted massive `handleIntent` dispatcher into categorized private handlers.
- Decomposed complex `loadFilesIntoWindow` logic into a readable sequence of steps.
- Simplified `handleLogUpdate` state update logic by extracting calculation helpers.
- Extracted preference restoration logic from the `init` block.
**Key decisions**:
- Grouped intent handlers by domain (Workspace, Filter, UI, Tab/Window, Entry, SFTP, Dialog, Recent Items) to reduce cognitive load when navigating the ViewModel.
- Prioritized small, well-named functions (Clean Code principles) over large blocks of MVI logic.
- Maintained existing state flow and behavior to ensure zero regression for end-users.
- Documented the refactoring strategy in ADR-026.
**Gotchas**:
- Care was taken to ensure that `scope.launch` and `cancelAndJoin` lifecycles were correctly preserved during function extraction.
- Sorting logic in `handleLogUpdate` must only be applied when multiple sources are present to avoid unnecessary overhead for single-file views.
**Test coverage areas**:
- `LogLoadingIntegrationTest`: 2/2 passing (verifying load flows).
- `TabManagementTest`: 5/5 passing (verifying intents and state transitions).
- `SftpBrowsingTest`: 3/3 passing (verifying remote logic).
- `ConnectionToggleTest`: 6/6 passing (verifying connection lifecycle).

For each sprint/task
**Title**: SFTP Source Refactoring
**Date/time completed**: 2026-05-22 13:30
**What was shipped**: Decomposed `SftpLogSource` and `SftpDirectoryLogSource` into modular services.
**Key decisions**: 
- Extracted SSH auth to `SshService`.
- Extracted pooling to `SshClientPool`.
- Extracted remote tailing to `RemoteLogTailer`.
- Extracted directory load aggregation to `LogInitialLoadCoordinator`.
**Gotchas**: Constructor changes required updates to all SFTP-related unit tests.
**Test coverage areas**: `SftpLogSource`, `SftpDirectoryLogSource`, `SftpFileSystem` (unit tests), `SftpBrowsingTest` (integration).
**Title**: Comprehensive UI and Core Logic Tidy-up
**Date/time completed**: 2026-05-22 14:15
**What was shipped**:
- Decomposed massive `KLogViewerScreen` composable into modular components (`DialogHandler`, `LogTopBar`, `LogBottomBar`, `LogWindowList`, `LogWindowItem`).
- Extracted `RecentItemsDialog` to a standalone file.
- Refactored `DirectoryLogSource` to use small private functions for scanning and job management.
- Tidied up `LogList.kt` by extracting `LogGutter` and `LogEntryCell` from the row rendering logic.
**Key decisions**:
- Prioritized UI decomposition to make the main screen more readable and AI-navigable.
- Used `ProducerScope` extensions in `DirectoryLogSource` to keep `channelFlow` logic clean.
- Maintained consistent naming and parameter passing across new private composables.
**Gotchas**:
- When extracting Dialog logic, ensured `LaunchedEffect` for dialog triggers remained functional in the new `DialogHandler`.
- Carefully managed imports for state types (`LogWindow`, `TabState`) to resolve compilation issues in decomposed files.
**Title**: ViewModel Decomposition and Cyclomatic Complexity Reduction
**Date/time completed**: 2026-05-22 15:45
**What was shipped**:
- Major decomposition of `KLogViewerViewModel` into 6 focused services and handlers.
- Reduced `KLogViewerViewModel` file size from ~1235 to 906 lines while improving clarity.
- Implemented `LogUpdateReducer` for functional log merging logic.
- Implemented `LogFilterService` for log filtering and sorting.
- Implemented `RecentItemsManager` for management of recently accessed paths.
- Implemented `PreferencesStateMapper` to isolate state-preference conversion logic.
- Implemented `TabWindowController` to handle tab and window state transitions.
- Implemented `SftpIntentHandler` to handle complex SFTP navigation and connection intents.
**Key decisions**:
- Moved logic from private methods in the ViewModel to dedicated components to adhere to SRP.
- Used a nested `SftpIntent` interface to allow the specialized handler to process all remote-related actions.
- Preserved existing state management patterns and job cancellation logic to ensure reliability.
- Documented the architecture changes in ADR-029.
**Gotchas**:
- `SftpIntent` and `SftpUri` imports needed careful management as they were moved across packages/layers.
- Ensured that callback delegation from handlers back to the ViewModel correctly maintained the coroutine scope and job tracking.
**Test coverage areas**:
- `LogLoadingIntegrationTest`: 2/2 passing.
- `TabManagementTest`: 5/5 passing.
- `SftpBrowsingTest`: 3/3 passing.
- `RecentItemsTest`: 3/3 passing.
- `PersistenceIntegrationTest`: 4/4 passing.

For each sprint/task
**Title**: Deep Decomposition of ViewModel and Loading Coordinator
**Date/time completed**: 2026-05-22 15:15
**What was shipped**:
- Extracted `WorkspaceLogLoader` from `LogLoadingCoordinator` to isolate path resolution and heuristic logic.
- Decomposed `KLogViewerViewModel` into 8 focused intent handlers (Workspace, UI, Filter, TabWindow, Entry, Dialog, RecentItems, Sftp).
- Reduced ViewModel size from ~1235 lines to ~218 lines while preserving full MVI functionality.
**Key decisions**:
- Used a dependency-based approach where the ViewModel orchestrates focused handlers that share state via callbacks.
- Isolated "thinking" logic (WorkspaceLogLoader) from "orchestration" logic (LogLoadingCoordinator).
- Documented the architecture changes in ADR-032.
**Gotchas**:
- Callback delegation for debounced operations (like `savePreferences`) needed careful handling to maintain state consistency.
**Test coverage areas**:
- `LogLoadingIntegrationTest`: 2/2 passing.
- `TabManagementTest`: 5/5 passing.
- `SftpBrowsingTest`: 3/3 passing.
- `RecentItemsTest`: 3/3 passing.
- `PersistenceIntegrationTest`: 4/4 passing.
- Full suite of integration tests verified.

**Title**: Refactor SftpDirectoryLogSource for Reduced Complexity
**Date/time completed**: 2026-05-22 15:30
**What was shipped**:
- Extracted `RemoteDirectoryFileObserver` to orchestrate per-file observation in remote directories.
- Simplified `SftpDirectoryLogSource` by delegating file job management and update handling.
- Reduced cyclomatic complexity of `SftpDirectoryLogSource.observeLogs()`.
**Key decisions**:
- Isolated the management of `activeSources` and individual file coroutine jobs into a dedicated observer registry.
- Standardized the update handling flow between directory and file sources.
- Documented the architecture changes in ADR-033.
**Gotchas**:
- Ordering of initial load completion check and observer initialization state is critical for correct log aggregation.
**Test coverage areas**:
- `SftpDirectoryLogSourceTest`: 3/3 passing.
- `SftpLogSourceTest`: 7/7 passing.
- `SftpBrowsingTest`: 3/3 passing.
