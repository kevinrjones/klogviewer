---
sessionId: session-260518-211542-1i8w
---

# Requirements

### Overview & Goals
The goal of this task is to extend the existing UI testing framework to cover complex UI behaviors, such as independent column resizing in split panes and multi-selection of log entries. This will ensure that regressions in these areas are caught early and that the application remains robust as new features are added.

### Scope
- **In Scope**:
    - Adding `testTag` decorations to windows, resize handles, and headers.
    - Extending the Robot Pattern with support for mouse dragging and keyboard modifiers.
    - Implementing UI tests for split pane column resizing and multi-selection.
    - Providing guidance on screenshot testing vs. functional UI testing.
- **Out of Scope**:
    - Implementing a full-blown visual regression library (unless specifically requested after initial feedback).
    - Testing platform-specific file dialogs (already covered by `DialogProvider` mocks).

# Technical Design

### Current Implementation
- **UI Framework**: Jetpack Compose for Desktop.
- **Testing Framework**: `androidx.compose.ui:ui-test-junit4`.
- **Patterns**: Robot Pattern for DSL-based interactions.
- **Resizing**: Handled via `detectDragGestures` in `LogListHeader`.
- **Splits**: Implemented by rendering multiple `LogList` components in a `Column` or `Row`.

### Proposed Changes
1.  **UI Tagging**:
    - `KLogViewerScreen.kt`: Add `Modifier.testTag("window_${window.id}")` to the window container `Column` (around line 144).
    - `LogList.kt`: Add `Modifier.testTag("resize_handle_$column")` to the resize handle `Box` in `LogListHeader` (around line 183).
2.  **Robot Extensions**:
    - `BaseRobot`: Add `performMouseDrag(tag, startOffset, endOffset)` and `clickWithModifiers(tag, shift, meta)`.
    - `LogListRobot`: Add `resizeColumn(column, offset)` using `performMouseDrag` and `assertRowSelected(index)`.
    - `MainRobot`: Add `splitHorizontal()` (clicks `split_horizontal` tag) and `switchWindow(windowId)` (clicks `window_$windowId` tag).
3.  **New Test Suite**: `KLogViewerComplexUiTest.kt` to verify that resizing a column in one split does not affect the other.

### Screenshot Testing Analysis
For the specific issue of "wrong pane's columns were resized", functional UI tests are more appropriate because:
-   They directly verify the state change in the model (column widths).
-   They are less flaky and easier to maintain than screenshot baselines.
-   They run faster in headless CI environments.

Screenshot testing should be reserved for:
-   Verifying ANSI color rendering.
-   Checking complex layout alignment and spacing.
-   Verifying theme transitions.

We can implement a lightweight screenshot check using `onNodeWithTag(...).captureToImage()` if visual consistency is a high priority.

# Delivery Steps

### ✓ Step 1: Enhance UI tagging for complex behaviors
Add `testTag` decorations to enable targeted UI testing in split scenarios.

- Add `testTag("window_${window.id}")` to the window container in `KLogViewerScreen.kt`.
- Add `testTag("resize_handle_$column")` to the resize handle in `LogListHeader` within `LogList.kt`.

### ✓ Step 2: Extend Robot Pattern for Resizing and Selection
Update robots to support advanced mouse and keyboard interactions.

- Add `performMouseDrag` and `clickWithModifiers` to `BaseRobot.kt` using `performMouseInput`.
- Implement `splitHorizontal()` and `switchWindow(windowId)` in `MainRobot.kt`.
- Implement `resizeColumn(column, offset)` and `assertRowSelected(index)` in `LogListRobot.kt`.

### ✓ Step 3: Implement Complex Behavior UI Tests
Create `KLogViewerComplexUiTest.kt` to cover regression scenarios for splits and selection.

- **Independent Column Resizing**: Split the window, resize a column in one pane, and verify the other pane's column width remains unchanged.
- **Multi-selection**: Load logs and verify that Shift+Click selects a range and Meta/Ctrl+Click toggles individual selection.
- **Tab & Window Management**: Verify that opening and closing splits/tabs updates the UI state correctly.

### ✓ Step 4: Documentation on Testing Strategies
Provide a brief guide or ADR on when to use Visual Regression (screenshots) vs. Functional UI tests.
- Explain how to use `captureToImage()` for basic visual checks if needed.