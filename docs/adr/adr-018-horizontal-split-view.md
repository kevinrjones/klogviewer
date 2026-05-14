# ADR 018: Horizontal Split View Support

## Status
Proposed

## Context
Users need to view multiple log files simultaneously within a single tab to compare or correlate events. Previously, each tab supported only a single log view (which could be a merge of multiple files). A "horizontal split" allows dividing a tab's content area into multiple independent windows.

## Decision
We have refactored the UI state and components to support a flexible split view architecture:

1.  **State Refactoring**:
    *   Introduced `LogWindow` to encapsulate all state related to a single log view (logs, filters, file path, sorting, etc.).
    *   Updated `TabState` to hold a list of `LogWindow` instances and an `activeWindowId`.
    *   Intents and ViewModel logic now target the "active window" of the active tab by default.

2.  **UI Updates**:
    *   Added a "Split Horizontal" button to the `FilterBar`.
    *   Updated `LogViewerScreen` to render multiple `LogWindow` instances in a vertical stack (creating horizontal splits).
    *   Implemented window focus logic: clicking a window makes it active, and the `FilterBar`/`Sidebar` controls are then tied to that window.
    *   Added a close button to individual split windows to allow collapsing them.

3.  **Terminology**:
    *   The "horizontal split" refers to the orientation of the divider line, resulting in top and bottom windows.

## Consequences
- **Pros**:
    *   Enables multi-file comparison within a single tab.
    *   Maintains independent filtering and sorting per split.
    *   Scalable to more than two splits.
- **Cons**:
    *   Increased complexity in state management and event routing.
    *   Higher memory usage when multiple large logs are loaded in separate splits.
