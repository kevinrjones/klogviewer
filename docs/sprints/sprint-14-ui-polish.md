# Sprint 14: UI Polish

## 1. Goal
Deliver targeted desktop UI polish for the main log table and Pattern Wizard, focusing on text display modes, grid lines, color refinement, and shape refinement — without changing structured JSON, parser, or runtime behavior.

## 2. Scope

### 2.1. Table Text Display Modes
- Add a **Compact** display mode (default) that truncates cell content at the column boundary with an ellipsis (`TextOverflow.Ellipsis`), preserving the complete underlying value.
- Add a **Full** display mode that shows cell content without truncation.
- Provide a user-visible toggle between Compact and Full modes as a **per-window** setting.
- Clicking a truncated cell in Compact mode opens a lightweight popup anchored near the cell, displaying the full value with a copy-to-clipboard button.
- Handle multiline values, structured JSON content, null/empty cells, and column-sizing interactions gracefully.
- Ensure keyboard interaction: Escape or clicking outside dismisses the popup; focus moves to the popup when opened.

### 2.2. Table Grid Lines
- Add **both horizontal and vertical** grid lines to the main log table for easier row/column boundary tracking.
- Horizontal lines: full-width, drawn at the bottom of each row.
- Vertical lines: drawn at column boundaries, using column widths from the existing layout system.
- Color: `onSurface` at ~10–12% alpha in dark mode, ~15–18% alpha in light mode.
- Selected rows retain a primary-tinted background; hovered rows get a slightly lighter tint.
- Alternating rows keep their existing subtle shade differences.
- Performance: grid lines are drawn with `drawLine` in a custom `Canvas` within the existing `LogEntryRow` composable.

### 2.3. Pattern Wizard Color Refinement
- Replace the current purple treatment (`0xFF9B59B6`) with a **blue-gray slate** palette (`0xFF607D8B`).
- Keep the existing primary blue (`0xFF00A3E0` dark / `0xFF007ACC` light) for button backgrounds and accent colors.
- Wizard background uses `surfaceVariant` with a cool-gray/slate shift.
- **Do not change** the color or structure of the pattern-description pills — they are explicitly considered acceptable.
- Affected files: `PatternWizardDialog.kt` (Surface tonalElevation, button colors), `PatternTheme.kt` (roleColor for THREAD), `PatternTokenBar.kt` (token pill backgrounds).

### 2.4. Pattern Wizard Shape Refinement
- Reduce the rounded-corner treatment on the Pattern Wizard dialog and buttons to a more conventional desktop appearance.
- Dialog corners: change from `RoundedCornerShape(16.dp)` to `RoundedCornerShape(4.dp)`.
- Button corners: change from `RoundedCornerShape(8.dp)` to `RoundedCornerShape(4.dp)`.
- Footer surface corners: change from `RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)` to `RoundedCornerShape(0.dp)`.
- **Do not change** the existing pill shape used for pattern descriptions — pills retain their current structure and visual treatment.

## 3. Design Contract

### 3.1. Compact vs Full Display Mode
- **Compact mode** (default): Cell text is truncated with ellipsis at the column boundary. A click on a truncated cell opens a lightweight popup (`CellValuePopup.kt`) anchored near the cell, showing the full value with a copy button.
- **Full mode**: Cell text is shown without truncation. Column sizing and horizontal scrolling determine visibility.
- The toggle is stored in `WindowPreference` as `useCompactCellMode: Boolean = true` (default).
- The popup handles multiline values, structured JSON content, and null/empty cells gracefully.
- Clicking outside the popup or pressing Escape dismisses it; keyboard focus moves to the popup when opened.

### 3.2. Grid Line Rendering
- Horizontal lines: drawn full-width at the bottom of each row using `Canvas.drawLine`.
- Vertical lines: drawn at column boundaries, aligned with the existing column layout system.
- Color values:
  - Dark mode: `onSurface` at ~10–12% alpha
  - Light mode: `onSurface` at ~15–18% alpha
- Row state rendering:
  - Selected rows: primary-tinted background, grid lines drawn on top
  - Hovered rows: slightly lighter tint, grid lines drawn on top
  - Alternating rows: keep subtle shade differences, grid lines drawn on top
- Lines are drawn in a custom `Canvas` within the `LogEntryRow` composable for performance.

### 3.3. Pattern Wizard Palette and Shape
- **Palette**:
  - Wizard background: `surfaceVariant` with a cool-gray/slate shift
  - Accent colors (selected state, active toggle): existing primary blue (`0xFF00A3E0` dark / `0xFF007ACC` light)
  - Button backgrounds: primary blue
  - THREAD role pills: `Color(0xFF607D8B)` (blue-gray) instead of `Color(0xFF9B59B6)` (purple)
- **Shape**:
  - Dialog main Surface: `RoundedCornerShape(4.dp)` (was `MaterialTheme.shapes.large`)
  - Apply button: `RoundedCornerShape(4.dp)` (was `RoundedCornerShape(8.dp)`)
  - Footer surface: `RoundedCornerShape(0.dp)` (was `RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)`)

## 4. Out of Scope
- Unrelated redesigns, new features, parser changes, or broad application-wide theming unless required to implement the requested behavior consistently.
- Changing the shape or color of pattern-description pills (they are explicitly considered acceptable).
- Changing structured JSON behavior, heuristic probing, or any parser/runtime code.
- Changing the existing `tonalElevation` of the Pattern Wizard (deferred — open question).
- Grid-line visibility toggle (grid lines are always-on when implemented).

## 5. Key Decisions
- **Display mode names**: **Compact** (default) and **Full**.
- **Truncated cell interaction**: A lightweight popup anchored near the cell reveals the full value with a copy button.
- **Setting scope**: Per-window toggle, stored in the existing `WindowPreference` model as `useCompactCellMode: Boolean = true`.
- **Grid lines**: Both horizontal and vertical lines are shown. Always-on (no toggle) for this sprint.
- **Pattern Wizard palette**: Blue-gray slate palette (`0xFF607D8B`) replaces purple (`0xFF9B59B6`). Primary blue retained for buttons and accents.
- **Pattern Wizard shapes**: Dialog corners reduced to 4.dp, button corners to 4.dp, footer corners to 0.dp.
- **No new preferences schema**: Compact/Full toggle is a new boolean field in the existing `WindowPreference` model, serialized via the existing `JsonPreferencesRepository`.
- **Grid lines drawn with `drawLine`**: Using `Canvas` in `LogEntryRow` for performance.

## 6. Definition of Done
- [x] Compact mode truncates cell content with ellipsis at column boundaries; Full mode shows content without truncation.
- [x] Clicking a truncated cell in Compact mode opens a popup with the full value and a copy button.
- [x] The Compact/Full toggle is wired per-window and persists across sessions.
- [x] Horizontal and vertical grid lines are visible in the main log table.
- [x] Grid line colors respect dark/light theme and row states (selected, hovered, alternating).
- [x] Pattern Wizard purple treatment is replaced with the blue-gray slate palette.
- [x] Pattern Wizard dialog and button corners are reduced to 4.dp (footer to 0.dp).
- [x] Pattern-description pills are unchanged in color and shape.
- [x] Structured JSON logs, heuristic probing, and parser/runtime code are unchanged.
- [ ] Visual regression and usability verification is complete.
- [x] Sprint and task documentation is updated.

## 7. Implementation Notes

### 7.1. Compact vs Full Display Mode
- **Preference field**: `useCompactCellMode: Boolean = true` added to `WindowPreference` in `UserPreferences.kt`.
- **Toggle location**: Toolbar "More" menu item labeled "Cell View: Compact" / "Cell View: Full" with test tag `toolbar_toggle_compact_mode` in `FilterBar.kt`.
- **Cell truncation**: `LogEntryCell.kt` uses `maxLines = 1` and `TextOverflow.Ellipsis` in Compact mode; `maxLines = Int.MAX_VALUE` and `TextOverflow.Visible` in Full mode.
- **Popup**: `CellValuePopup.kt` provides a scrollable popup (200–600dp width, 60–400dp height) with monospace body text, copy-to-clipboard button, and Escape/click-outside dismissal. Test tag: `cell_value_popup`.
- **Intent**: `KLogViewerIntent.ToggleCompactCellMode` handled in the ViewModel to toggle and persist the preference.

### 7.2. Grid Lines
- **Rendering**: `drawBehind` Canvas modifier in `LogEntryRow` within `LogList.kt`.
- **Colors**: Dark mode `onSurface` at `0.12f` alpha; Light mode `onSurface` at `0.18f` alpha.
- **Lines**: Horizontal line at row bottom, vertical line after the gutter column, vertical lines at each column boundary.
- **Stroke**: 1px width.

### 7.3. Pattern Wizard Colors
- **THREAD role**: Dark `0xFF607D8B`, Light `0xFF455A64` (replaced purple `0xFF9B59B6`).
- **Wizard background**: `MaterialTheme.colorScheme.surfaceVariant`.
- **Button colors**: Primary blue from `MaterialTheme.colorScheme.primary` (dark `0xFF00A3E0` / light `0xFF007ACC`).
- **Pills**: Pattern-description pills retain `RoundedCornerShape(16.dp)` and existing color treatment.

### 7.4. Pattern Wizard Shapes
- **Dialog Surface**: `RoundedCornerShape(4.dp)`.
- **Apply/Action buttons**: `RoundedCornerShape(4.dp)`.
- **Footer Surface**: `RoundedCornerShape(0.dp)`.

### 7.5. Test Files
- `CompactCellModePersistenceTest.kt` — preference serialization and default value.
- `CellValuePopupTest.kt` — popup rendering and dismissal behavior.
- `LogListGridLineTest.kt` — grid line visibility and color in dark/light theme.
- `PatternWizardThemeAndShapeUiTest.kt` — wizard palette and corner radii verification.