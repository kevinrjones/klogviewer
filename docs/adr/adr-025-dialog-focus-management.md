# ADR 025: Dialog Focus Management and Tab Navigation

## Status
Proposed

## Context
The application uses various custom dialogs implemented with Jetpack Compose `AlertDialog`. Users expect standard desktop behavior where the `Tab` key moves focus between input fields, buttons, and other interactive elements. By default, Compose Desktop might not handle Tab navigation within dialogs as expected, especially with complex layouts.

## Decision
We will implement explicit focus management in all custom dialogs to ensure consistent and reliable Tab/Shift+Tab navigation.

### Implementation Details
1. **Initial Focus**: Use `FocusRequester` and `LaunchedEffect(Unit)` to request focus on the primary input field or button when the dialog is opened.
2. **Context-Aware Focus Manager**: Fetch `LocalFocusManager.current` INSIDE the `AlertDialog` content slots (text, confirmButton, etc.) to ensure access to the dialog-specific focus manager. Fetching it outside the dialog scope may return the main window's focus manager, which will not work for dialog navigation.
3. **Explicit Tab Handling**: Add `onPreviewKeyEvent` to the root layout of the dialog content AND to action buttons (confirm/dismiss) to intercept `Tab` and `Shift+Tab` keys and manually call `focusManager.moveFocus()`.
4. **Keyboard Actions**: For `TextField` components, set `KeyboardOptions(imeAction = ImeAction.Next)` and `KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })`.

## Consequences
- **Pros**: 
    - Consistent and predictable user experience on desktop.
    - Improved accessibility.
    - Clearer focus states when dialogs open.
- **Cons**: 
    - Slightly more boilerplate in dialog implementations.
    - Need to remember to apply this pattern to new dialogs.
