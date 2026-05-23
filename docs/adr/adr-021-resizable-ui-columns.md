# ADR 021: Resizable UI Columns

## Status
Proposed

## Context
As the application moved towards supporting diverse log formats with dynamic columns (ADR 019), it became necessary for users to adjust the widths of these columns to better view specific information (e.g., long requests in Apache logs or deep hostnames in Syslog). Fixed-width columns were too restrictive for the high-density desktop experience.

## Decision
We decided to implement interactive, resizable columns in the `LogList` grid.

1.  **State Management**: Added a `columnWidths: Map<String, Int>` property to `LogWindow` and persisted it in `UserPreferences`. Includes a special `"Line #"` key for the line number gutter.
2.  **Interaction Pattern**: Users can drag the right edge of any column header, including the line number gutter, to resize it.
3.  **Visual Feedback**: 
    - The mouse cursor changes to a horizontal resize icon (`E_RESIZE_CURSOR`) when hovering over the draggable areas between columns.
    - Draggable handles are now visually marked with a subtle vertical bar to improve discoverability.
4.  **MVI Integration**: A new `UpdateColumnWidth` intent was added to handle the state updates and trigger persistence.
5.  **Dynamic Layout**: Both the header and log rows now share a common `getColumnWidth` logic that prioritizes user-defined widths over default presets.

## Consequences
- **Positive**: significantly improved usability for structured log files.
- **Positive**: Users can now optimize their screen real estate based on their specific log data.
- **Neutral**: Added slight complexity to the `LogList` layout logic to ensure synchronization between header and rows.
- **Neutral**: Column widths are now part of the persisted workspace state, increasing the preference file size slightly.
