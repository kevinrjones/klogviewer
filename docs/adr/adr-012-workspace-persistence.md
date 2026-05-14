# ADR 012: Workspace Persistence

## Status
Proposed

## Context
Currently, when the application is closed, all state (open files, filters, search queries) is lost. Users often work with the same sets of log files repeatedly and would benefit from being able to save and restore their workspace.

## Decision
We will implement a workspace persistence mechanism that allows users to save their environment state to a project file (`.lvp`).

### 1. Workspace Configuration Model
Create a serializable `Workspace` model that includes:
- List of open tabs and their configuration (files, filters, source colors).
- Remote connection settings (referencing `ConnectionManager`).
- UI settings (sidebar state, dark mode, window size).

### 2. File Format
Use JSON or TOML for the `.lvp` (LogViewer Project) file format for human readability and ease of versioning.

### 3. Recent Workspaces
Implement a "Recent Workspaces" list in the "File" menu and on the startup screen for quick access.

## Consequences
- **Positive**: Significantly improves productivity for recurring analysis tasks.
- **Positive**: Enables sharing analysis environments between team members.
- **Negative**: Requires careful management of sensitive information (e.g., remote passwords should be stored in an OS-level keychain, not the project file).
- **Negative**: Schema versioning is required to handle upgrades to the workspace format.
