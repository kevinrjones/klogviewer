# Sprint 2: UI/UX Refinement Tasks

## 7. Sprint 2: UI/UX Refinement

### 7.1. Foundation & Design System
- [x] 7.1.1. Initialize Sprint 2 Documentation and Task Tracking
- [ ] 7.1.2. Define "Command-Line Chic" color palettes in `LogViewerColors.kt`
    - [ ] 7.1.2.1. Industrial Dark palette (Background: #121212, Accent: #00A3E0)
    - [ ] 7.1.2.2. Clean Light palette
- [ ] 7.1.3. Implement `LogViewerTheme.kt` with Dark/Light mode support
- [ ] 7.1.4. Configure `JetBrains Mono` as the default monospaced font for log content
- [ ] 7.1.5. Update `LogViewerState` with `isDarkMode` flag
- [ ] 7.1.6. Update `LogViewerViewModel` with `ToggleTheme` intent
- [ ] 7.1.7. Refactor `LogList` and `FileSelector` to use theme tokens

### 7.2. Layout & Components
- [ ] 7.2.1. Implement collapsible `Sidebar.kt` on the left
- [ ] 7.2.2. Implement `StatusBar.kt` for file metadata
    - [ ] 7.2.2.1. Display file path
    - [ ] 7.2.2.2. Display line count
    - [ ] 7.2.2.3. Display encoding
- [ ] 7.2.3. Update `LogViewerScreen.kt` to use `Scaffold` with sidebar and status bar
- [ ] 7.2.4. Refactor Top Bar into a clean, integrated header with Search entry

### 7.3. Interactive Filtering & Search
- [ ] 7.3.1. Extend `LogViewerState` with `searchQuery` and `levelFilters`
- [ ] 7.3.2. Implement `UpdateSearch` and `ToggleLevel` intents in `LogViewerViewModel`
- [ ] 7.3.3. Implement reactive filtering logic in `LogViewerViewModel`
- [ ] 7.3.4. Add `LogLevelFilter` group to the Sidebar
- [ ] 7.3.5. Implement real-time search feedback in the Top Bar (highlight matches count)

### 7.4. Intelligent Highlighting & Line Numbering
- [ ] 7.4.1. Implement regex-based `LogHighlighter.kt` utility
    - [ ] 7.4.1.1. Regex for UUIDs/IDs
    - [ ] 7.4.1.2. Regex for IPv4/IPv6 addresses
    - [ ] 7.4.1.3. Regex for ISO-8601 Timestamps
- [ ] 7.4.2. Update `LogEntryRow.kt` with line numbering gutter
- [ ] 7.4.3. Use `AnnotatedString` to apply syntax highlighting and search term bolding
- [ ] 7.4.4. Optimize scrolling performance for large log files (50k+ lines)

### 7.5. Verification & DoD
- [ ] 7.5.1. Perform Visual Audit of "Command-Line Chic" in both modes
- [ ] 7.5.2. Verify Log Level filtering logic
- [ ] 7.5.3. Verify Search highlighting and result accuracy
- [ ] 7.5.4. Verify performance stability with 50,000+ lines
