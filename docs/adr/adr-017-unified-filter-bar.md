# ADR 017: Unified Filter Bar and Multi-Item Filtering

## Status
Accepted

## Context
As part of the ongoing UI/UX refinement ("Enema"), we aimed to further reduce visual clutter by removing the Microsoft Office-style Ribbon Bar. Additionally, the user requested a "multi item search" capability similar to `Tailviewer`, where multiple terms can be applied simultaneously and managed as chips. Following user feedback, we transitioned the terminology from "Search" to "Filter" to better reflect the application's behavior.

## Decision
We replaced the `RibbonBar` with a unified, high-density `FilterBar` and updated the logic to support multiple filtering terms.

### Key Changes
- **Unified Filter Bar**: Replaced the multi-row `RibbonBar` with a single-row `FilterBar`. This bar now hosts all essential actions as compact icons and a dedicated filtering area.
- **Multi-Item Filtering**: Updated `TabState` to store a list of filter queries instead of a single string. The UI now displays these queries as chips within the filter field.
- **Filtering Logic (AND)**: Changed the log filtering logic to require that an entry matches *all* active filter terms. This allows users to drill down into specific logs (e.g., filtering for both "Error" and "Database").
- **Integrated Highlighting**: Updated `LogHighlighter` to highlight all active filter terms simultaneously in the log list.
- **Cross Icon**: Added a prominent "cross" icon to the filter area to clear all active filters and current input text.
- **Renaming**: Renamed all UI components and internal code elements from "Search" to "Filter" to ensure consistency with the user's preferred terminology.
- **High-Density Optimization**: Switched from standard Material `TextField` to `BasicTextField` and reduced icon sizes (to 28dp) to minimize the bar's vertical footprint, maximizing space for log entries.

## Consequences
- **Pros**:
    - Significantly more vertical space for log content.
    - More powerful filtering capabilities with multiple terms.
    - Modern, IDE-like interface that prioritizes content.
- **Cons**:
    - Icons are less discoverable than labeled buttons (partially mitigated by tooltips/content descriptions).
    - Complex search queries might require more typing than simple one-off searches.
