# ADR 008: Transition to Desktop-Centric UI Pattern

## Status
Proposed

## Context
The current implementation of LogViewer uses a "Mobile Chic" aesthetic, heavily influenced by Material Design patterns common in mobile and web applications (e.g., `Scaffold`, top bars with limited actions, and simplified layouts). While functional, this approach does not align with the power-user expectations of a professional desktop log analysis tool.

Desktop users typically require:
1.  **High Information Density**: Maximizing the number of log entries visible on screen.
2.  **Discoverability**: Surfacing frequent actions (filtering, searching, file management) without hiding them behind menus.
3.  **Standard OS Integration**: Familiar patterns like native menu bars and tabbed workspaces.

## Decision
We will transition the UI architecture from a mobile-style Material layout to a desktop-centric pattern characterized by:
1.  **Ribbon/Tool Bar**: A horizontal area at the top grouping related actions (File, Analysis, View).
2.  **Native Menu Bar**: Leveraging `WindowScope` to provide standard `File`, `Edit`, `View` menus.
3.  **Multi-Pane Layout**: Using a workspace-style layout with resizable sidebars and a bottom detail pane for log entry inspection.
4.  **Tabbed Interface**: Allowing multiple logs or interleaved views to be open simultaneously (supporting the goals of Sprint 3).

## Consequences

### Positive
*   **Improved Productivity**: Surfacing actions in a ribbon bar reduces clicks for power users.
*   **Professional Feel**: The app will feel like a native desktop utility rather than a ported mobile app.
*   **Scalability**: The menu bar provides a logical place for advanced features as the project grows.

### Negative
*   **Implementation Effort**: Transitioning away from standard Material `Scaffold` components requires custom layout management in Compose for Desktop.
*   **UI Complexity**: Managing a higher density of information requires careful design to avoid clutter.

### Neutral
*   **Component Selection**: We may need to move away from some `androidx.compose.material` components in favor of custom or `desktop`-specific implementations to achieve the desired "desktop" look.
