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
- Sprint 6: UI Redesign ("Enema") completed (high-density layout, consolidated filters, IDE-style theme).
- UI Refinements: Added scrollbars, further reduced tab bar depth, eliminated line gaps, updated tab bar background to a distinct grey color, and replaced RibbonBar with a unified FilterBar supporting multi-item filtering.
- UI Simplification: Removed redundant toggles from Sidebar and unnecessary file icons from FilterBar to achieve a cleaner, content-focused interface.
- Sidebar Restyling: Replaced standard Material checkboxes with a high-density, hierarchical filter panel featuring square checkboxes, right-aligned counts, and section headers with expand arrows, matching professional IDE patterns. Improved readability by using sentence case for log level names.
- Unified Filtering: Transitioned from "Search" to "Filter" terminology. Added a clear-all "cross" icon to the FilterBar and updated all internal logic to match this terminology.
- UI Density: Further reduced FilterBar height by transitioning to `BasicTextField` and smaller icons, and tightened sidebar filter padding for maximum vertical space.
- Split View: Implemented horizontal split view support with an independent window-based architecture, allowing multiple logs to be viewed, filtered, and sorted independently within a single tab.
- Navbar Tooltips: Integrated `TooltipArea` across all primary UI icons (tabs, filters, splits, details), improving discoverability and accessibility.
- UI Layout Persistence: Implemented full workspace restoration. The application now remembers and reloads all open tabs, split windows, active filters, and loaded files on startup.
- Fixed a critical UI bug where log grid columns were squashed to the left in horizontal scroll mode.
- Resolved column resizing 'snap back' issue by implementing a robust drag accumulator and using `rememberUpdatedState` in Compose.
- Optimized performance by debouncing preference saves during rapid column resizing.
- Added `IntrinsicSize.Min` to the log header to ensure proper vertical alignment and resize handle visibility.
- Improved layout stability within `horizontalScroll` by removing infinite-width `fillMaxSize` constraints from `LazyColumn`.
- Dynamic Details Pane: Redesigned the log entry details pane to adapt to different log formats. It now hides the "Level" field when unknown (common in structured logs like Nginx) and automatically displays all available fields from the `LogEntry.fields` map.

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
- `LogViewerViewModel`: Verified `levelCounts` calculation.
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
- Refactored `LogViewerViewModel` to support background loading of logs for all windows during initialization, ensuring a seamless startup experience.
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
- Updated `LogViewerScreen.kt` to always display the fully qualified file path in each split window's header.
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
- Adding new parameters to `LogEntryDetails` required updating the call site in `LogViewerScreen` and importing `LogLevel` in the component file.
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
