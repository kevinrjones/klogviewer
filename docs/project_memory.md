# Project Memory

## Overall
**What was shipped**
- Initial project structure defined and documented.
- Sprint 1: Walking Skeleton plan finalized.
- Architectural pillars established (MVI, Either, Multi-Module, Tiny Types).

**Key decisions**
- Adopted MVI for UI architecture to align with functional and immutable principles.
- Chose Arrow's `Either` for error handling to support typed domain failures.
- Selected a Layered Multi-Module structure (`domain`, `core`, `ui`, `app`) for better separation of concerns.
- Committed to using Tiny Types for core domain concepts to enhance type safety.

**Gotchas**
- Initial discussion on `Result` vs `Either` highlighted the importance of typed errors in functional design.

## Sprint: Walking Skeleton Implementation
**Title**: Sprint 1 Completion
**Date/time completed**: 2026-05-12 11:30
**What was shipped**
- Layered multi-module project structure (`domain`, `core`, `ui`, `app`).
- Core domain models and Tiny Types for type safety.
- `LogParser` and `LogService` for parsing and loading log files.
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
