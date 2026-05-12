# Sprint 1: Walking Skeleton

## 1. Goal
Establish a minimal end-to-end "walking skeleton" that loads a log file, parses it into domain objects, and displays it in a Compose for Desktop UI with appropriate color coding for log levels.

## 2. Scope

### 2.1. Foundation
- [ ] Set up layered multi-module structure (`domain`, `core`, `ui`, `app`).
- [ ] Initialize project with Gradle Kotlin DSL and Version Catalog (`libs.versions.toml`).
- [ ] Create initial ADRs (Architectural Decision Records) for core pillars.
- [ ] Configure Compose for Desktop.
- [ ] Implement MVI (Model-View-Intent) architecture for UI state management.
- [ ] Add core dependencies: Arrow, JUnit 5, Strikt, Kluent.

### 2.2. Domain & Logic
- [ ] Define Tiny Types for core concepts: `LogFilePath`, `LogLevel`, `LogTimestamp`, `LogContent`.
- [ ] Implement a sealed interface for Log Entry parsing errors.
- [ ] Create a `LogParser` service that handles simple Log4j-like text formats.
- [ ] Use `Either` for error handling in the parsing pipeline.

### 2.3. User Interface
- [ ] Create a basic Window with a file path input field and a "Load" button.
- [ ] Implement a scrollable list to display log entries.
- [ ] Apply color coding based on `LogLevel` (e.g., Error = Red, Warn = Yellow, Info = Green).

### 2.4. Testing
- [ ] TDD: Unit tests for `LogParser` using Strikt/JUnit.
- [ ] BDD: A simple Cucumber feature for "Opening a log file".

## 3. Key Decisions (Confirmed)
- **Structure**: Layered Multi-Module (`domain`, `core`, `ui`, `app`).
- **Architecture**: MVI (Model-View-Intent) for the UI layer.
- **Format**: Initial support limited to simple text logs to prove the pipeline.
- **Monitoring**: Static file loading only; "Tail -f" deferred to Sprint 2.
- **Error Handling**: Arrow `Either` for all domain operations.
- **Testing**: Mix of unit tests (TDD) and high-level behavioral specs (Cucumber).

## 4. Definition of Done
- Application builds and runs on at least one desktop platform.
- A sample log file can be loaded and viewed.
- At least 80% code coverage on the parser logic.
- All guidelines (Tiny Types, Functional Style, Naming) are followed.
