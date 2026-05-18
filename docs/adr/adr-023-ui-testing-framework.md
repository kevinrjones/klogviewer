# ADR-023: UI Testing Framework Selection

## Status
Accepted

## Context
KLogViewer currently relies on unit tests for core logic and BDD tests for the ViewModel layer. While these provide good coverage for business rules, they do not exercise the actual Jetpack Compose UI components, their layout, or their integration with AWT-based platform features (like file dialogs and window management). As we transition to a high-density, desktop-centric UI with complex interactions like split panes and independent window focus, manual regression testing is becoming unsustainable. We need an automated UI testing framework that can verify the end-to-end user journey.

## Decision
We will adopt the official **Compose Test library** (`androidx.compose.ui:ui-test-junit4`) as our primary UI testing framework.

- **Framework**: Use `ComposeTestRule` (JUnit 4 based) for driving the UI.
- **Execution**: Tests will run as `desktopTest` tasks in the `:ui` module.
- **Assertions**: We will continue to use **Strikt** for assertions where applicable, combined with standard Compose `onNode` assertions.
- **Platform Mocking**: We will introduce abstractions (e.g., `DialogProvider`) to mock blocking AWT calls like `FileDialog` during UI tests, allowing them to run in a headless or automated fashion without manual intervention.
- **CI/CD**: Headless execution will be supported via `Xvfb` on Linux-based CI runners.

## Consequences
- **Pros**:
    - **Reliability**: Uses the official synchronization mechanisms to wait for the UI to be idle.
    - **Maintainability**: Rich set of matchers and actions tailored for Compose.
    - **Developer Experience**: Familiar API for anyone who has done Android Compose testing.
- **Cons**:
    - **Mixed Test Runners**: Requires JUnit 4 for UI tests while the rest of the project uses JUnit 5. This is a known limitation of the current Compose Testing library.
    - **Setup Complexity**: Requires specific Gradle configuration for desktop-specific test tasks.
    - **Performance**: UI tests are inherently slower than unit or BDD tests.
