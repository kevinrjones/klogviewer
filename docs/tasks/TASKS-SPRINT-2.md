# Sprint 2: UI/UX Refinement Tasks

## 7. Sprint 2: UI/UX Refinement

### 7.1. Foundation & Design System
- [x] 7.1.1. Initialize Sprint 2 Documentation and Task Tracking
- [x] 7.1.2. Define "Command-Line Chic" color palettes in `KLogViewerColors.kt`
    - [x] 7.1.2.1. Industrial Dark palette (Background: #121212, Accent: #00A3E0)
    - [x] 7.1.2.2. Clean Light palette
- [x] 7.1.3. Implement `KLogViewerTheme.kt` with Dark/Light mode support
- [x] 7.1.4. Configure `JetBrains Mono` as the default monospaced font for log content
- [x] 7.1.5. Update `KLogViewerState` with `isDarkMode` flag
- [x] 7.1.6. Update `KLogViewerViewModel` with `ToggleTheme` intent
- [x] 7.1.7. Refactor `LogList` and `FileSelector` to use theme tokens

### 7.2. Layout & Components
- [x] 7.2.1. Implement collapsible `Sidebar.kt` on the left
- [x] 7.2.2. Implement `StatusBar.kt` for file metadata
    - [x] 7.2.2.1. Display file path
    - [x] 7.2.2.2. Display line count
    - [x] 7.2.2.3. Display encoding
- [x] 7.2.3. Update `KLogViewerScreen.kt` to use `Scaffold` with sidebar and status bar
- [x] 7.2.4. Refactor Top Bar into a clean, integrated header with Search entry

### 7.3. Interactive Filtering & Search
- [x] 7.3.1. Extend `KLogViewerState` with `searchQuery` and `levelFilters`
- [x] 7.3.2. Implement `UpdateSearch` and `ToggleLevel` intents in `KLogViewerViewModel`
- [x] 7.3.3. Implement reactive filtering logic in `KLogViewerViewModel`
- [x] 7.3.4. Add `LogLevelFilter` group to the Sidebar
- [x] 7.3.5. Implement real-time search feedback in the Top Bar (highlight matches count)

### 7.4. Intelligent Highlighting & Line Numbering
- [x] 7.4.1. Implement regex-based `LogHighlighter.kt` utility
    - [x] 7.4.1.1. Regex for UUIDs/IDs
    - [x] 7.4.1.2. Regex for IPv4/IPv6 addresses
    - [x] 7.4.1.3. Regex for ISO-8601 Timestamps
- [x] 7.4.2. Update `LogEntryRow.kt` with line numbering gutter
- [x] 7.4.3. Use `AnnotatedString` to apply syntax highlighting and search term bolding
- [x] 7.4.4. Optimize scrolling performance for large log files (50k+ lines)

### 7.5. Verification & DoD
- [ ] 7.5.1. Perform Visual Audit of "Command-Line Chic" in both modes
- [ ] 7.5.2. Verify Log Level filtering logic
- [ ] 7.5.3. Verify Search highlighting and result accuracy
- [ ] 7.5.4. Verify performance stability with 50,000+ lines
