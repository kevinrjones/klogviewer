# TASKS: Sprint 14 - UI Polish

## 14. Sprint 14: UI Polish

### 14.0. Goal
Deliver targeted desktop UI polish for the main log table and Pattern Wizard, focusing on Compact vs Full text display modes with popup cell value reveal, table grid lines, blue-gray slate color refinement, and reduced corner-radius shapes — without changing structured JSON, parser, or runtime behavior.

### 14.1. In Scope
- Compact vs Full display mode toggle with popup cell value reveal (per-window).
- Horizontal and vertical grid lines for the main log table.
- Pattern Wizard color refinement: replace purple with a blue-gray slate palette.
- Pattern Wizard shape refinement: reduce rounded corners on dialog and buttons.
- Visual regression, interaction, and usability verification for all four changes.
- Documentation updates for new sprint and task artifacts.

### 14.2. Out of Scope
- Unrelated redesigns, new features, parser changes, or broad application-wide theming unless required to implement the requested behavior consistently.
- Changing the shape or color of pattern-description pills (they are explicitly considered acceptable).
- Changing structured JSON behavior, heuristic probing, or any parser/runtime code.
- Grid-line visibility toggle (grid lines are always-on when implemented).
- Changing the existing `tonalElevation` of the Pattern Wizard.

### 14.3. Dependencies
- Required architectural guidance: `docs/sprints/sprint-14-ui-polish.md`.
- Existing UI component files: `LogList.kt`, `ColumnSizingCalculator.kt`, `PatternWizardDialog.kt`, `PatternTheme.kt`, `PatternTokenBar.kt`.
- Existing preference model: `WindowPreference` in `domain/src/main/kotlin/com/klogviewer/domain/model/UserPreferences.kt`.
- Existing preferences repository: `core/src/main/kotlin/com/klogviewer/core/repository/JsonPreferencesRepository.kt`.

### 14.4. Scope-to-Workstream Mapping
- Table text display modes (`14.5`) -> Compact mode truncation, CellValuePopup composable, toggle wiring in WindowPreference.
- Table grid lines (`14.6`) -> horizontal/vertical line rendering, color/theme integration, row state handling.
- Pattern Wizard color refinement (`14.7`) -> palette audit, roleColor updates, background/button color updates.
- Pattern Wizard shape refinement (`14.8`) -> dialog corner radius, button corner radius, footer shape.
- Visual regression and usability verification (`14.9`) -> comprehensive testing across all four workstreams.
- Documentation updates (`14.10`) -> sprint doc and task doc updates.
- Acceptance criteria (`14.11`) -> verification of all DoD items.

### 14.5. Table Text Display Modes (Compact vs Full)
- [x] 14.5.1. Add `useCompactCellMode: Boolean` field to `WindowPreference`
- [x] 14.5.2. Implement Compact mode text truncation in the log table cell composable using `TextOverflow.Ellipsis` at the column boundary.
- [x] 14.5.3. Create `CellValuePopup.kt` composable: lightweight popup anchored near the truncated cell, displaying the full value with a copy-to-clipboard button.
- [x] 14.5.4. Wire click detection on truncated cells in `LogList.kt` to open the `CellValuePopup`.
- [x] 14.5.5. Add popup dismissal logic: clicking outside or pressing Escape dismisses the popup; keyboard focus moves to the popup when opened.
- [x] 14.5.6. Add a user-visible toggle (e.g., menu item, toolbar button, or context menu action) to switch between Compact and Full display modes per window.
- [x] 14.5.7. Wire the toggle to update `useCompactCellMode` in `WindowPreference` and persist via `JsonPreferencesRepository`.
- [x] 14.5.8. Handle multiline values in the popup (scrollable content area).
- [x] 14.5.9. Handle structured JSON content in the popup (preserved formatting).
- [x] 14.5.10. Handle null/empty cells gracefully (no popup shown for empty cells).
- [x] 14.5.11. Ensure column sizing interactions remain correct when switching between display modes.
- [x] 14.5.12. Add unit tests for the `useCompactCellMode` preference field serialization and default value.
- [x] 14.5.13. Add Compose UI tests for the `CellValuePopup` rendering and dismissal behavior.
- [x] 14.5.14. Add interaction tests for the toggle state persistence across sessions.

### 14.6. Table Grid Lines
- [x] 14.6.1. Add horizontal grid line rendering in `LogEntryRow` composable in `LogList.kt` using `Canvas.drawLine` at the bottom of each row.
- [x] 14.6.2. Add vertical grid line rendering at column boundaries, using column widths from the existing layout system.
- [x] 14.6.3. Define grid line color constants: `onSurface` at ~10–12% alpha (dark mode), ~15–18% alpha (light mode), in the theme.
- [x] 14.6.4. Ensure selected rows draw grid lines on top of the primary-tinted background.
- [x] 14.6.5. Ensure hovered rows draw grid lines on top of the slightly lighter tinted background.
- [x] 14.6.6. Ensure alternating rows keep their subtle shade differences with grid lines on top.
- [x] 14.6.7. Verify grid lines are consistent with the existing visual theme (no visual conflicts).
- [x] 14.6.8. Verify grid line rendering performance is acceptable for large tables (use `drawLine` in `Canvas`).
- [x] 14.6.9. Add Compose UI tests verifying grid line visibility and color in dark/light theme.
- [x] 14.6.10. Add snapshot or visual tests for grid lines across selected, hovered, and alternating row states.

### 14.7. Pattern Wizard Color Refinement
- [x] 14.7.1. Audit the Pattern Wizard palette for all purple color references (`0xFF9B59B6` and similar).
- [x] 14.7.2. Update `PatternTheme.kt` `roleColor` for THREAD: replace `Color(0xFF9B59B6)` with `Color(0xFF607D8B)` (blue-gray).
- [x] 14.7.3. Update `PatternWizardDialog.kt` background: use `surfaceVariant` with a cool-gray/slate shift.
- [x] 14.7.4. Update `PatternWizardDialog.kt` button colors: use primary blue (`0xFF00A3E0` dark / `0xFF007ACC` light) for button backgrounds.
- [x] 14.7.5. Verify `PatternTokenBar.kt` token pill backgrounds are consistent with the new palette.
- [x] 14.7.6. Verify pattern-description pills are NOT changed (they keep their current color and structure).
- [x] 14.7.7. Add Compose UI tests verifying the new palette renders correctly in the wizard.
- [x] 14.7.8. Add visual regression tests for the wizard with the new palette (light and dark mode).

### 14.8. Pattern Wizard Shape Refinement
- [x] 14.8.1. Update `PatternWizardDialog.kt` main Surface: change `MaterialTheme.shapes.large` to `RoundedCornerShape(4.dp)`.
- [x] 14.8.2. Update `PatternWizardDialog.kt` Apply button: change `RoundedCornerShape(8.dp)` to `RoundedCornerShape(4.dp)`.
- [x] 14.8.3. Update `PatternWizardDialog.kt` footer Surface: change `RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)` to `RoundedCornerShape(0.dp)`.
- [x] 14.8.4. Verify pattern-description pills are NOT changed (they keep their current shape and structure).
- [x] 14.8.5. Add Compose UI tests verifying the new corner radii render correctly.
- [x] 14.8.6. Add visual regression tests for the wizard with the new shapes.

### 14.9. Visual Regression and Usability Verification
- [x] 14.9.1. Run `./gradlew :ui:test` to verify all UI tests pass.
- [x] 14.9.2. Run `./gradlew check` for all affected modules (`:domain`, `:core`, `:ui`, `:app`).
- [x] 14.9.3. Run `./gradlew detekt` to verify static analysis passes.
- [x] 14.9.4. Run the cyclomatic complexity review and decide whether follow-up reduction tasks are needed.
- [x] 14.9.5. Verify structured JSON logs, heuristic probing, and parser/runtime code are unchanged.
- [x] 14.9.6. Verify pattern-description pills are unchanged in color and shape.
- [x] 14.9.7. Verify the Compact/Full toggle persists correctly across app restarts.
- [x] 14.9.8. Verify grid lines render correctly with dark/light theme switching.
- [x] 14.9.9. Verify the Pattern Wizard dialog and buttons have the correct corner radii.
- [x] 14.9.10. Verify the Pattern Wizard uses the blue-gray slate palette (no purple remaining).

### 14.10. Documentation Updates
- [x] 14.10.1. Keep sprint doc (`docs/sprints/sprint-14-ui-polish.md`) and task doc aligned with final implementation.
- [x] 14.10.2. Update `docs/project_memory.md` at sprint completion with shipped scope, key decisions, gotchas, and test coverage areas.

### 14.11. Acceptance Criteria
- [x] Compact mode truncates cell content with ellipsis at column boundaries; Full mode shows content without 
  truncation.
- [x] Clicking a truncated cell in Compact mode opens a popup with the full value and a copy button.
- [x] The Compact/Full toggle is wired per-window and persists across sessions.
- [x] Horizontal and vertical grid lines are visible in the main log table.
- [x] Grid line colors respect dark/light theme and row states (selected, hovered, alternating).
- [x] Pattern Wizard purple treatment is replaced with the blue-gray slate palette.
- [x] Pattern Wizard dialog and button corners are reduced to 4.dp (footer to 0.dp).
- [x] Pattern-description pills are unchanged in color and shape.
- [x] Structured JSON logs, heuristic probing, and parser/runtime code are unchanged.
- [x] Visual regression and usability verification is complete.
- [x] Sprint and task documentation is updated.