# TASKS: Sprint 6 - UI Redesign

## 11. Sprint 6: UI Redesign ("Enema")

### 11.1. Log List Enhancements
- [x] 11.1.1. Disable line wrapping for log lines and implement horizontal scrolling
- [x] 11.1.2. Reduce gap between log lines for higher density

### 11.2. Toolbar & Sidebar Cleanup
- [x] 11.2.1. Remove filters from the toolbar (TopBar)
- [x] 11.2.2. Update sidebar filters to show counts for each category (e.g., '5 DEBUG')

### 11.3. Layout & Theme Refinement
- [x] 11.3.1. Narrow the tab bar (RibbonBar)
- [x] 11.3.2. Update dark mode theme to use dark grays instead of blacks

### 11.4. UI Polish & Refinements
- [x] 11.4.1. Add vertical and horizontal scroll bars to the log list
- [x] 11.4.2. Narrow the tab bar further (reduce depth)
- [x] 11.4.3. Eliminate gap between log lines (reduce row height to minimum)
- [x] 11.4.4. Change tab bar background to a grey color (Material Surface)

### 11.5. Search Bar & Ribbon Removal
- [x] 11.5.1. Replace RibbonBar with a unified SearchBar
- [x] 11.5.2. Implement multi-item search with chips
- [x] 11.5.3. Integrate action icons (Open, Add, Clear, Theme, Sidebar, Sort) into SearchBar
- [x] 11.5.4. Update LogHighlighter and LogList to support multiple search terms

### 11.6. Further UI Simplification
- [x] 11.6.1. Remove "File Open" and "Clear Logs" icons from SearchBar
- [x] 11.6.2. Remove redundant "Dark Mode" and "Sidebar" toggles from Sidebar

### 11.7. Sidebar Restyling
- [x] 11.7.1. Restyle sidebar filters to match professional hierarchical panel (square checkboxes, right-aligned counts)
- [x] 11.7.2. Implement hierarchical layout with section headers and indentation
- [x] 11.7.3. Change sidebar filter entry names to sentence case (e.g., "Debug" instead of "DEBUG")

### 11.8. Filter Bar Renaming & Cleanup
- [x] 11.8.1. Rename "Search" to "Filter" in UI (placeholder, tooltips)
- [x] 11.8.2. Add "cross" icon to clear all active filters
- [x] 11.8.3. Rename internal code (intents, state, components) from Search to Filter

### 11.9. Verification
- [x] 11.9.1. Manual verification of the "Clean" UI
- [x] 11.9.2. Verify accessibility of all functions after consolidation

### 11.10. Density Improvements
- [x] 11.10.1. Reduce height of Filter Bar (use BasicTextField and smaller icons)
- [x] 11.10.2. Tighten vertical padding in Sidebar filters

### 11.11. Split View Support
- [x] 11.11.1. Refactor `TabState` to support multiple `LogWindow` instances
- [x] 11.11.2. Add "Split Horizontal" button to `FilterBar`
- [x] 11.11.3. Implement window focus and independent filtering/sorting per split
- [x] 11.11.4. Update `KLogViewerScreen` to render vertical stacks of splits
- [x] 11.11.5. Display fully qualified file path in split window header

### 11.12. UI Interactivity & Polish
- [x] 11.12.1. Add tooltips to all icons in the navbar (FilterBar and TabRow)

### 11.13. Workspace Persistence
- [x] 11.13.1. Extend `UserPreferences` to include tab and split window configuration
- [x] 11.13.2. Implement UI state restoration in `KLogViewerViewModel` on startup
- [x] 11.13.3. Implement automatic log reloading for all open windows
- [x] 11.13.4. Ensure real-time persistence of layout and filter changes
- [x] 11.14. Resizable Columns
- [x] 11.14.1. Implement resizable columns in the log list
- [x] 11.14.2. Persist column widths in user preferences

### 11.15. UI Bug Fixes
- [x] 11.15.1. Fix column resizing 'snap back' regression and optimize save performance
- [x] 11.15.2. Configure details pane to show dynamic fields based on log type
