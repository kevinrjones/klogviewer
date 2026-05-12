# ADR-001: Multi-Module Layered Architecture

## Status
Accepted

## Context
To ensure a clean separation of concerns and maintainability as the LogViewer project grows, we need a robust architectural structure. A single-module project can lead to tight coupling and leaked implementation details between the domain logic and the UI framework (Compose for Desktop).

## Decision
We will adopt a layered multi-module architecture consisting of four primary modules:
1.  `:domain`: Contains core business entities (LogEntry), domain-specific types (Tiny Types), and repository interfaces. No external dependencies except Arrow.
2.  `:core`: Implements the business logic, services (LogParser), and use cases. Depends on `:domain`.
3.  `:ui`: Contains the presentation logic (ViewModels) and Compose UI components. Depends on `:core` and `:domain`.
4.  `:app`: The entry point of the application. Responsible for wiring dependencies and launching the UI.

## Consequences
- **Pros**:
    - Clear boundaries between layers.
    - Improved testability (unit tests for domain/core without UI).
    - Faster build times due to incremental compilation of modules.
- **Cons**:
    - Slightly higher initial setup complexity.
    - Need to manage cross-module dependencies.
