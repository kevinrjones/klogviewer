# Project Memory

## Overall
**What was shipped**
- Initial project structure defined and documented.
- Sprint 1: Walking Skeleton completed (Multi-module, MVI, Log Loading).
- Sprint 2: UI/UX Refinement completed (Command-Line Chic theme, Layout, Filtering).
- Deepened architecture with reactive `LogSource` streaming.
- Native UI enhancements for file browsing.

**Key decisions**
- Adopted MVI for UI architecture to align with functional and immutable principles.
- Chose Arrow's `Either` for error handling to support typed domain failures.
- Selected a Layered Multi-Module structure (`domain`, `core`, `ui`, `app`) for better separation of concerns.
- Committed to using Tiny Types for core domain concepts to enhance type safety.
- Transitioned to a streaming Flow-based `LogSource` to support scalability and real-time monitoring.

**Gotchas**
- Initial discussion on `Result` vs `Either` highlighted the importance of typed errors in functional design.
- `FileDialog` via `AwtWindow` requires manual state reset on close to avoid dialog re-triggering.

## Sprint: Walking Skeleton Implementation
**Title**: Sprint 1 Completion
**Date/time completed**: 2026-05-12 11:30
**What was shipped**
- Layered multi-module project structure (`domain`, `core`, `ui`, `app`).
- Core domain models and Tiny Types for type safety.
- `LogParser` and initial `LogService` for parsing and loading log files.
- MVI-based UI layer with `LogViewerViewModel` and Compose components.
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
- `LogViewerViewModel`: BDD test covering the end-to-end flow of loading a log file and updating the state.

## Task: Architectural Deepening - Streaming LogSource
**Title**: LogSource Implementation
**Date/time completed**: 2026-05-12 11:50
**What was shipped**
- `LogSource` repository interface in `:domain`.
- `FileLogSource` implementation in `:core` using Kotlin Flow.
- Delta-based `LogUpdate` models for efficient UI updates.
- Refactored `LogViewerViewModel` to consume streams instead of full lists.

**Key decisions**
- Internalized concurrency (`Dispatchers.IO`) within the `LogSource` implementation.
- Used `LogUpdate` sealed interface to distinguish between initial load and subsequent appends.

**Gotchas**
- Asynchronous flow collection in BDD tests required using `flow.first()` with timeout to avoid race conditions.

**Test coverage areas**
- `FileLogSource`: Integration via BDD tests.
- `LogViewerViewModel`: Updated BDD steps to handle Flow collection.

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
- UI components: `FileSelector` and `LogViewerScreen` (verified via manual run and build).

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
- Renamed and renumbered future placeholder sprints (7-10) to maintain a continuous roadmap.

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
- Tabbed workspace architecture with `TabState` and `LogViewerState` refactoring.
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
- `docs/sprints/sprint-4-structured-data.md`: Detailed plan for Sprint 4.
- `docs/sprints/sprint-5-connectivity.md`: Detailed plan for Sprint 5.
- `docs/sprints/sprint-6-analysis-and-visualization.md`: Detailed plan for Sprint 6.
- `docs/sprints/sprint-7-power-user-tools.md`: Detailed plan for Sprint 7.
- `docs/sprints/sprint-8-extensibility-and-release.md`: Detailed plan for Sprint 8.

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
- Added new integration test suite (`com.logviewer.integration`) to verify multi-tab behavior.

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
- Migrated FileDialog state to `LogViewerState` to allow triggering from both the Menu Bar and Ribbon.
- Implemented `RibbonBar` as a group of functional sections (File, View, Filters) for better discoverability.
- Used a 250dp detail pane for log entry inspection, triggered by row clicks.
- Reduced row padding and increased information density in `LogList` to meet power-user requirements.

**Gotchas**
- Compose for Desktop `MenuBar` must be defined within the `Window` scope.
- Row selection in `LazyColumn` requires managing `selectedEntry` state in `TabState`.
- Icons for `ViewSidebar` were deprecated; moved towards modern variants where suggested by compiler.

**Test coverage areas**
- `LogViewerViewModel`: Verified new intents for selection and dialog management.
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
- Instrumented `FileLogSource`, `SimpleLogParser`, `MergedLogSource`, and `LogViewerViewModel` with extensive logging.
- Added explicit error logging for all failure paths in the log loading and parsing pipeline.

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
