# Sprint: UI Testing Spike

## 1. Goal
Establish a robust and maintainable UI testing infrastructure for KLogViewer, moving from ViewModel-level validation to full end-to-end UI verification using the Compose for Desktop testing framework.

## 2. The Journey to Full UI Testing

### 2.1. Gap Analysis
Currently, KLogViewer uses Cucumber JVM for BDD, but these tests operate at the `ViewModel` level. They verify that intents lead to state changes, but they do not verify:
- Actual rendering of components.
- User interactions like mouse clicks, scrolls, and drag-and-drops.
- Proper integration with AWT-native components (Dialogs, Menus).
- Theme application and visual consistency.

### 2.2. Phase 1: Infrastructure Setup
The first step is to equip the build system with the necessary tools for desktop UI testing.
- **Dependencies**: Add `androidx.compose.ui:ui-test-junit4` to `libs.versions.toml`.
- **Gradle Configuration**: Ensure the `:ui` and `:app` modules are configured to run `desktopTest` tasks.
- **Headless Execution**: Setup a virtual display environment (Xvfb for Linux/CI) to allow UI tests to run without a physical monitor.

### 2.3. Phase 2: The Robot Pattern
To prevent UI tests from becoming brittle, we will adopt the **Robot Pattern**.
- **Robots**: Dedicated classes (e.g., `LogListRobot`, `SidebarRobot`, `TabRobot`) that encapsulate "how" to interact with the UI.
- **DSL**: Tests will use a readable DSL like:
  ```kotlin
  logList {
      verifyEntryCount(5)
      selectEntry(0)
  }
  detailPane {
      verifyLevel("ERROR")
  }
  ```

### 2.4. Phase 3: Handling Side Effects & Fakes
UI tests must be deterministic. We will:
- **Test Dispatchers**: Inject `StandardTestDispatcher` into the UI composition to control coroutine execution.
- **Fakes**: Replace `FileLogSource` and `PreferencesRepository` with Fakes to avoid disk I/O and external side effects.
- **AWT Mocks**: Since `FileDialog` is a blocking AWT call, we will introduce an abstraction (`DialogProvider`) that can be mocked in tests to simulate file selection.

### 2.5. Phase 4: Full Coverage
Once the infrastructure is solid, we will implement tests for:
- **Core Loops**: Open file -> Wait for load -> Verify grid.
- **Filtering**: Click Level "ERROR" -> Verify grid shows only errors.
- **Search**: Type in search bar -> Verify highlights in the grid.
- **Multi-Split**: Open multiple windows -> Verify independent scrolling and selection.
- **Tabs**: Switch tabs -> Verify state persistence.

## 3. Key Decisions
- **Framework**: Use the official `ComposeTestRule` (JUnit 4 based) as it provides the best integration with the Compose runtime.
- **Isolation**: UI tests will run against a "naked" `KLogViewerScreen` where possible, but we will also have full `Main` entry point tests for E2E.
- **Assertions**: Continue using **Strikt** for assertions within the UI tests to maintain consistency with the rest of the test suite.

## 4. Definition of Done
- [ ] UI testing dependencies are present in `libs.versions.toml`.
- [ ] A `BaseRobot` class is established in the `:ui` module.
- [ ] A successful "Smoke Test" that launches the app and verifies the title bar exists.
- [ ] A full E2E test covering the "Load and Filter" workflow.
- [ ] CI/CD pipeline (GitHub Actions) successfully executes UI tests on every PR.
- [ ] Developer documentation added to `docs/TESTING.md` explaining the UI testing strategy.
