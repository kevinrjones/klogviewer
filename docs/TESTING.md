# Testing Strategy

This document outlines the testing strategy, frameworks, and patterns used in KLogViewer.

## 1. Overview

KLogViewer uses a multi-layered testing approach to ensure reliability and maintainability:

- **Unit Tests**: Focus on individual components, parsers, and logic.
- **BDD (Behavior-Driven Development)**: Focus on user stories and high-level requirements.
- **UI Tests**: Focus on the visual components and end-to-end user workflows using the Robot Pattern.

## 2. Frameworks & Tools

- **JUnit 5**: The primary test runner for unit and BDD tests.
- **Strikt**: An assertion library for Kotlin that provides a fluent and type-safe DSL.
- **MockK**: A powerful mocking library for Kotlin.
- **Cucumber JVM**: For executing Gherkin-based BDD specifications.
- **Compose UI Testing**: `org.jetbrains.compose.ui:ui-test-junit4` for testing Compose Desktop components.
- **JUnit 4**: Used specifically for UI tests (required by the current Compose Testing framework).

## 3. Unit Testing

Unit tests are located in the `src/test/kotlin` directory of each module. We follow TDD principles where possible.

### Example (Strikt)
```kotlin
expectThat(result)
    .isA<LogUpdate.Initial>()
    .get { entries }
    .hasSize(3)
```

## 4. BDD Testing

BDD specifications are located in the `app/src/test/resources/features` directory. Step definitions are in `app/src/test/kotlin/com/klogviewer/bdd`.

## 5. UI Testing (Robot Pattern)

UI tests are located in the `:ui` module under `src/test/kotlin/com/klogviewer/ui/test`. They utilize the **Robot Pattern** to separate test intent from implementation details.

### The Robot Pattern

Robots provide a domain-specific language (DSL) for interacting with the UI:

- `MainRobot`: Handles global actions like adding tabs or typing into the filter bar.
- `SidebarRobot`: Handles filter interactions in the sidebar.
- `LogListRobot`: Handles assertions and interactions with the log grid.

### Example UI Test
```kotlin
@Test
fun givenLogsLoaded_whenSearchTermEntered_thenLogsAreFiltered() {
    setupApp()
    
    composeTestRule.mainRobot {
        clickAddFile()
        typeFilter("error")
    }

    composeTestRule.logList {
        assertLogCount(1)
        assertTextExists("Second log message (error)")
    }
}
```

### Headless Execution & Mocking

To support CI and headless environments, we mock AWT-based dialogs using the `DialogProvider` interface. When writing UI tests that involve file selection, ensure you inject a mocked `DialogProvider` into the `KLogViewerScreen`.

## 6. Running Tests

### All Tests
```bash
./gradlew test
```

### UI Tests Only
```bash
./gradlew :ui:desktopTest
```

### BDD Tests Only
```bash
./gradlew :app:test --tests "com.klogviewer.bdd.*"
```

## 7. Best Practices

- **Tiny Types**: Use Tiny Types (e.g., `LogFilePath`, `LogContent`) in tests to ensure type safety.
- **Test Tags**: Use `Modifier.testTag("tag_name")` sparingly but consistently for stable UI element matching.
- **Isolated State**: Ensure each test starts with a fresh `ViewModel` and mocked repositories.
- **Wait for Idle**: Use `waitUntilExists` or `composeTestRule.waitForIdle()` when dealing with asynchronous updates in UI tests.
