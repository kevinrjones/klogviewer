# ADR 013: Plugin Architecture

## Status
Proposed

## Context
As KLogViewer grows, it is impossible to support every proprietary log format, specialized data source, or custom analysis visualization natively in the core product. A plugin system is needed to allow third-party developers and users to extend the application's capabilities.

## Decision
We will implement a modular plugin architecture based on a Service Provider Interface (SPI) pattern.

### 1. Extension Points
Define clear interfaces for the following extension points:
- `LogParserProvider`: Custom regex or structured parsers.
- `LogSourceProvider`: Support for new remote protocols or database engines.
- `LogAnalysisProvider`: Custom charts, dashboards, or data processors.
- `LogActionProvider`: Custom context menu actions (e.g., "Open in Jira").

### 2. Loading Mechanism
Use a dynamic loading mechanism (e.g., JAR loading via `ServiceLoader` or a custom classloader) to discover and load plugins from a specific directory.

### 3. Plugin API
Provide a stable `:plugin-api` module that developers can depend on without pulling in the entire application's dependencies.

## Consequences
- **Positive**: Enables a community-driven ecosystem of parsers and tools.
- **Positive**: Keeps the core application lightweight and focused.
- **Negative**: Stability becomes a concern; plugin failures must not crash the main application.
- **Negative**: Security risks from executing third-party code; requires a clear security model or sandbox.
