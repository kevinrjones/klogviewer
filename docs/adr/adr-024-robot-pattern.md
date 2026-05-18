# ADR-024: Robot Pattern for UI Testing

## Status
Accepted

## Context
As we introduce UI tests using the Compose Testing library, we face the risk of creating "fragile" tests. Directly embedding UI matchers (like `onNodeWithTag("filter_input")`) and actions within test methods leads to significant code duplication and makes tests hard to read. Furthermore, minor UI changes (e.g., renaming a test tag or restructuring a component) would require updating dozens of test cases.

## Decision
We will implement the **Robot Pattern** to encapsulate UI interactions and assertions.

- **Structure**: Each major UI component or logical area (e.g., `LogList`, `Sidebar`, `FilterBar`, `MainWindow`) will have a corresponding "Robot" class.
- **Responsibilities**:
    - **Actions**: Methods for interacting with the UI (e.g., `typeFilterText(text: String)`, `clickAddTab()`).
    - **Assertions**: Methods for verifying UI state (e.g., `assertLogCount(expected: Int)`, `assertTabIsActive(tabTitle: String)`).
- **Fluent DSL**: Robots will return `this` or the next Robot in the sequence to allow for a fluent, readable DSL in tests.
- **BaseRobot**: A common `BaseRobot` will provide shared utilities for finding nodes, waiting for conditions, and performing common actions.
- **Test Separation**: Test classes will focus on *what* the user is doing (the journey), while Robots handle *how* it is performed in Compose.

## Consequences
- **Pros**:
    - **Readability**: Tests read like user scenarios (e.g., `logList { assertCount(10) }`).
    - **Maintainability**: UI changes are localized to a single Robot class.
    - **Reusability**: Common actions (like opening a file) can be shared across multiple test suites.
- **Cons**:
    - **Initial Overhead**: Requires creating and maintaining an additional layer of Robot classes.
    - **Abstraction Leakage**: If not careful, Robots can become overly complex by trying to handle too many edge cases.
