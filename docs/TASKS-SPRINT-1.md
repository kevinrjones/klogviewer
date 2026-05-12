# Tasks

## 1. Project Initialization
- [x] 1.1. Create layered multi-module structure (`domain`, `core`, `ui`, `app`) `[x]`
- [x] 1.2. Initialize Gradle with Kotlin DSL and `libs.versions.toml` `[x]`
- [x] 1.3. Configure `.gitignore` for Kotlin and Gradle projects `[x]`
- [x] 1.4. Set up Compose for Desktop in the `ui` and `app` modules `[x]`

## 2. Architectural Documentation
- [x] 2.1. Create ADR-001: Multi-Module Layered Architecture `[x]`
- [x] 2.2. Create ADR-002: Functional Error Handling with Arrow Either `[x]`
- [x] 2.3. Create ADR-003: UI Architecture with MVI `[x]`
- [x] 2.4. Create ADR-004: Type Safety with Tiny Types `[x]`

## 3. Core Domain & Logic
- [x] 3.1. Implement Tiny Types for `LogFilePath`, `LogLevel`, `LogTimestamp`, `LogContent` in `domain` module `[x]`
- [x] 3.2. Implement `LogEntry` data class and `LogParsingError` sealed interface in `domain` module `[x]`
- [x] 3.3. Implement `LogParser` service in `core` module `[x]`
    - [x] 3.3.1. Create unit tests for `LogParser` using TDD (JUnit 5 + Strikt) `[x]`
    - [x] 3.3.2. Implement parsing logic for simple text logs `[x]`

## 4. Presentation Layer (MVI)
- [x] 4.1. Define `LogViewerState`, `LogViewerIntent`, and `LogViewerEvent` in `ui` module `[x]`
- [x] 4.2. Implement `LogViewerViewModel` handling log loading intents `[x]`
- [x] 4.3. Create Compose UI components:
    - [x] 4.3.1. File selector header `[x]`
    - [x] 4.3.2. Log list with color coding `[x]`

## 5. Integration & Verification
- [x] 5.1. Implement a Cucumber BDD feature for loading a log file `[x]`
- [x] 5.2. Run the application and verify end-to-end "walking skeleton" flow `[x]`

## 6. UI Enhancements
- [x] 6.1. Add file browsing capability to the UI `[x]`
    - [x] 6.1.1. Add "Browse" button to `FileSelector` `[x]`
    - [x] 6.1.2. Implement native file picker integration `[x]`
    - [x] 6.1.3. Ensure selected file path is loaded into the viewer `[x]`
