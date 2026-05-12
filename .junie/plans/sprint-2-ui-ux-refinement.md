---
sessionId: session-260512-121343-ncg9
---

# Requirements

### Overview & Goals
The second sprint focuses on transforming the "Walking Skeleton" into a professional-grade log viewer. As a UI/UX-focused sprint, we will prioritize visual clarity, information density, and interactive analysis tools. We have chosen a **"Command-Line Chic"** aesthetic: an industrial, high-contrast look that honors developer tools while providing modern comforts like dark mode and intelligent highlighting.

### Scope
**In Scope:**
- **Sprint Documentation**: Creation of `docs/sprint-2-ui.md` and updating `TASKS.md`.
- **Custom Visual Identity**: Implementation of a refined "Command-Line Chic" theme with Light/Dark mode support.
- **Enhanced Layout**: Introduction of a left sidebar for filters and a persistent status bar for file metadata.
- **Interactive Filtering**: Real-time log level filtering (INFO, WARN, ERROR, etc.).
- **Advanced Search**: Text search with live result highlighting.
- **Intelligent Highlighting**: Automatic syntax highlighting for IDs, IP addresses, and timestamps using regex.
- **Line Numbering**: Displaying line numbers in a gutter for better reference.

**Out of Scope:**
- Multi-log interleaving (scheduled for Sprint 3).
- Advanced log parsing templates (scheduled for Sprint 3).

# Technical Design

### Current Implementation
The application currently uses default Material colors and a simple vertical column layout. Log levels have basic color coding, but the content is unformatted.

### Key Decisions
- **Theme**: A custom `LogViewerTheme` will be implemented, defaulting to **Dark Mode**. It will use high-contrast accents (#00A3E0 for primary, vibrant colors for log levels) on a near-black background (#121212).
- **Typography**: `JetBrains Mono` will be enforced for all log-related content to ensure tabular alignment and readability.
- **Highlighting Engine**: We will implement a `LogHighlighter` utility that uses regex to find domain-relevant patterns (UUIDs, IPv4/v6, Timestamps) and transforms them into `AnnotatedString` objects.
- **State Management**: The `LogViewerState` will be extended to include search and filter parameters, with the `LogViewerViewModel` performing the filtering on the background thread to maintain UI responsiveness.

### Proposed Changes
- **Theme (`ui/theme`)**:
    - `LogViewerTheme.kt`: The main theme entry point.
    - `LogViewerColors.kt`: Definitions for "Industrial Dark" and "Clean Light" palettes.
- **Components (`ui/components`)**:
    - `Sidebar.kt`: Left-aligned pane for level filters and theme toggle.
    - `StatusBar.kt`: Bottom pane for file information and line counts.
    - `LogHighlighter.kt`: Logic for syntax highlighting using `AnnotatedString`.
    - `LogEntryRow.kt`: Updated to include line numbers and highlighted content.
- **MVI (`ui/mvi`)**:
    - Add `LogViewerIntent.UpdateSearch`, `LogViewerIntent.ToggleLevel`, and `LogViewerIntent.ToggleTheme`.

### Documentation & Tracking
- `docs/sprint-2-ui.md`: A comprehensive sprint document outlining goals, scope, and key decisions for the UI refinement phase.
- `docs/TASKS.md`: Updated with a new section (7. Sprint 2: UI/UX Refinement) containing all subtasks.

### File Structure
```
ui/src/main/kotlin/com/logviewer/ui/
├── theme/
│   ├── LogViewerTheme.kt
│   └── LogViewerColors.kt
├── components/
│   ├── Sidebar.kt
│   ├── StatusBar.kt
│   ├── LogHighlighter.kt
│   └── ...
└── ...
```

# Testing

### Validation Approach
- **Visual Audit**: Verification of the "Command-Line Chic" aesthetic across both Light and Dark modes.
- **Functional Verification**:
    - Ensure searching for a term (e.g., "error") highlights all occurrences in the current view.
    - Verify that unchecking a log level (e.g., "DEBUG") immediately removes those entries from the list.
- **Performance Testing**: Load a 50,000 line log file and verify that scrolling remains fluid with highlighting and line numbers enabled.

### Key Scenarios
- **Scenario: Theme Toggle**: User clicks the theme icon in the sidebar. The UI transitions from Dark to Light mode instantly, preserving all filters and scroll position.
- **Scenario: Multi-term Highlighting**: A log line containing an IP address and a UUID is displayed. Both should be highlighted in their respective colors.
- **Scenario: Search & Filter Interaction**: User filters for ERROR logs only and then searches for "connection". Only ERROR logs containing "connection" should be visible.

# Delivery Steps

### ✓ Step 1: Initialize Sprint 2 Documentation and Task Tracking
Establish the formal tracking and documentation for the sprint.

- Create `docs/sprint-2-ui.md` based on the Sprint 1 template, including Goal, Scope, and Key Decisions.
- Update `docs/TASKS.md` with Section 7: "Sprint 2: UI/UX Refinement", including subtasks for Design, Layout, Filtering, and Highlighting.

### ✓ Step 2: Implement Design System and Theme Infrastructure
Establish the visual foundation for the application.

- Create `LogViewerTheme.kt` with "Command-Line Chic" color palettes for both Light and Dark modes.
- Set up `JetBrains Mono` as the default monospaced font for log content.
- Update `LogViewerState` and `LogViewerViewModel` to handle theme switching.
- Refactor `LogList` and `FileSelector` to utilize the new theme tokens.

### ✓ Step 3: Refine Layout with Sidebar and Status Bar
Organize the UI for better information density and accessibility.

- Implement a collapsible `Sidebar` on the left for navigation and settings.
- Create a `StatusBar` at the bottom to show file path, line count, and encoding.
- Update `LogViewerScreen` to use a modern `Scaffold` with the new layout components.
- Refactor the header into a clean, integrated Top Bar with a Search entry.

### ✓ Step 4: Implement Search and Level Filtering Logic
Enhance the utility of the log viewer with interactive analysis tools.

- Add `searchQuery` and `levelFilters` to the MVI state.
- Implement filtering logic in `LogViewerViewModel` to reactively update the displayed logs.
- Add an interactive `LogLevelFilter` group in the Sidebar.
- Implement real-time search feedback in the Top Bar.

### * Step 5: Add Intelligent Highlighting and Line Numbering
Add professional log viewing features for improved readability.

- Implement a regex-based `LogHighlighter` to identify IDs, IP addresses, and timestamps.
- Update `LogEntryRow` to include a gutter with line numbers.
- Use `AnnotatedString` to apply syntax highlighting and search term bolding in the log content.
- Ensure smooth scrolling performance when highlighting is active.