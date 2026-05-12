# Project Memory

## Overall
**What was shipped**
- Initial project structure defined and documented.
- Sprint 1: Walking Skeleton completed (Multi-module, MVI, Log Loading).
- Deepened architecture with reactive `LogSource` streaming.
- Native UI enhancements for file browsing.

**Key decisions**
- Adopted MVI for UI architecture to align with functional and immutable principles.
- Chose Arrow's `Either` for error handling to support typed domain failures.
- Selected a Layered Multi-Module structure (`domain`, `core`, `ui`, `app`) for better separation of concerns.
- Committed to using Tiny Types for core domain concepts to enhance type safety.
- Transitioned to a streaming Flow-based `LogSource` to support scalability and real-time monitoring.

**Gotchas**
- Initial discussion on `Result` vs `Either` highlighted the importance of typed errors in functional design.
- `FileDialog` via `AwtWindow` requires manual state reset on close to avoid dialog re-triggering.

## Sprint: Walking Skeleton Implementation
**Title**: Sprint 1 Completion
**Date/time completed**: 2026-05-12 11:30
**What was shipped**
- Layered multi-module project structure (`domain`, `core`, `ui`, `app`).
- Core domain models and Tiny Types for type safety.
- `LogParser` and initial `LogService` for parsing and loading log files.
- MVI-based UI layer with `LogViewerViewModel` and Compose components.
- BDD (Cucumber) and TDD (JUnit/Strikt) test suites.

**Key decisions**
- Used `value class` for Tiny Types to ensure performance with safety.
- Implemented unidirectional data flow (MVI) in the UI layer.
- Offloaded file I/O to `Dispatchers.IO` in the ViewModel.

**Gotchas**
- Gradle 9.3 requires `junit-platform-launcher` to be explicitly added to the test runtime classpath.
- Cucumber docstrings can introduce leading whitespace that may cause regex parsing to fail if not handled (fixed by adding `.trim()` in the parser).

**Test coverage areas**
- `SimpleLogParser`: Unit tests for various log levels and invalid lines.
- `LogViewerViewModel`: BDD test covering the end-to-end flow of loading a log file and updating the state.

## Task: Architectural Deepening - Streaming LogSource
**Title**: LogSource Implementation
**Date/time completed**: 2026-05-12 11:50
**What was shipped**
- `LogSource` repository interface in `:domain`.
- `FileLogSource` implementation in `:core` using Kotlin Flow.
- Delta-based `LogUpdate` models for efficient UI updates.
- Refactored `LogViewerViewModel` to consume streams instead of full lists.

**Key decisions**
- Internalized concurrency (`Dispatchers.IO`) within the `LogSource` implementation.
- Used `LogUpdate` sealed interface to distinguish between initial load and subsequent appends.

**Gotchas**
- Asynchronous flow collection in BDD tests required using `flow.first()` with timeout to avoid race conditions.

**Test coverage areas**
- `FileLogSource`: Integration via BDD tests.
- `LogViewerViewModel`: Updated BDD steps to handle Flow collection.

## Task: UI Enhancements - File Browsing
**Title**: File Browsing Implementation
**Date/time completed**: 2026-05-12 12:05
**What was shipped**
- Integrated native `FileDialog` into the Compose for Desktop UI.
- Enhanced `FileSelector` component with a "Browse" button.
- Updated MVI model with `SelectPath` intent for decoupled path selection.

**Key decisions**
- Used `java.awt.FileDialog` via `AwtWindow` to provide a native OS feel for file selection.
- Maintained MVI purity by routing file selection through the ViewModel's state.

**Gotchas**
- `AwtWindow` requires careful handling of the `onCloseRequest` to ensure the dialog state is reset in the Composable.

**Test coverage areas**
- UI components: `FileSelector` and `LogViewerScreen` (verified via manual run and build).
