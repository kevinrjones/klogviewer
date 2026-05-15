# ADR 016: UI/UX Refinement ("Enema")

## Status
Accepted

## Context
The user requested a cleaner, higher-density UI for better log viewing on desktop. This included reducing visual clutter, increasing information density, and improving dark mode readability.

## Decision
We implemented several UI enhancements to achieve a more professional and efficient log viewing experience.

### Key Changes
- **Log List Density**: Reduced vertical padding between log entries to 0dp and reduced header padding. This allows more logs to be visible on screen simultaneously.
- **Horizontal Scrolling**: Disabled line wrapping for log content and implemented a shared horizontal scroll for the entire log list (including headers). This preserves the structure of log lines which often have fixed-width fields.
- **Filter Consolidation**: Removed log level filters from the toolbar (RibbonBar) and consolidated them into the Sidebar to reduce redundancy and clutter in the main action area.
- **Visual Feedback**: Added log entry counts to the sidebar filters (e.g., "5 DEBUG") to provide immediate insight into the log distribution.
- **Tab Bar Refinement**: Narrowed the tab bar by reducing horizontal and vertical padding, saving vertical space for the log content.
- **Dark Mode Color Palette**: Updated the dark theme from black-centric (`#121212`) to a dark gray palette (`#2B2B2B` background, `#3C3F41` surface) similar to professional IDEs like IntelliJ IDEA (Darcula).
- **Tab Bar Visual Distinction**: Changed the Tab Bar background to a slightly different grey (`#323232` in dark mode, `#E0E0E0` in light mode) to make it stand out from the rest of the UI (Ribbon Bar and Sidebar).
- **Sidebar Filter Styling**: Restyled sidebar filters to match professional hierarchical filter panels. Implemented small square checkboxes, right-aligned entry counts, and indented entries with section headers and expand arrows.
- **Dynamic Details Pane**: Refactored the log entry details pane to show only relevant fields based on the log type. It now hides the "Level" field when it is `UNKNOWN` and automatically iterates through all available structured fields (like `Client IP` or `Status` in Apache/Nginx logs) while ensuring the main content remains highlighted and searchable.

## Consequences
- **Pros**:
    - Increased information density.
    - Improved readability of long log lines.
    - Cleaner interface with less redundant controls.
    - Better dark mode ergonomics.
- **Cons**:
    - Users must now use the sidebar for level filtering (or keyboard shortcuts if implemented later).
    - Horizontal scrolling might require more mouse/trackpad interaction for very long lines.
