---
sessionId: session-260826-105434-o92n
---

# Requirements

### Overview & Goals

Create Sprint 14 documentation for a new "UI Polish" sprint inserted immediately after Sprint 13, and renumber all downstream sprints by +1.

**Sprint 14 scope**: Four focused desktop UI polish items:
1. **Table text display modes** — Compact vs Full mode with popup cell value reveal
2. **Table grid lines** — Horizontal + vertical grid lines for the main log table
3. **Pattern Wizard color refinement** — Replace purple with blue-gray slate palette
4. **Pattern Wizard shape refinement** — Reduce rounded corners on dialog/buttons

### Product Decisions (User-Confirmed)

| Decision | Choice |
|---|---|
| Display mode names | **Compact** vs **Full** |
| Truncated cell interaction | **Popup reveal** near the cell, with copy button |
| Setting scope | **Per-window** toggle |
| Grid lines | **Both horizontal and vertical** |
| Pattern Wizard palette | **Blue-gray slate** palette, primary blue for buttons |

### Out of Scope
- Unrelated redesigns, new features, parser changes, or broad application-wide theming unless required to implement the requested behavior consistently.
- Changing the shape or color of pattern-description pills (they are explicitly considered acceptable).
- Changing structured JSON behavior, heuristic probing, or any parser/runtime code.

### Renumbering Map

| Old | New | File |
|---|---|---|
| Sprint 14 | Sprint 15 | `docs/sprints/sprint-14-power-user-tools.md` → `sprint-15-power-user-tools.md` |
| Sprint 15 | Sprint 16 | `docs/sprints/sprint-15-clef-viewing.md` → `sprint-16-clef-viewing.md` |
| Sprint 16 | Sprint 17 | `docs/sprints/sprint-16-extensibility-and-release.md` → `sprint-17-extensibility-and-release.md` |
| Sprint 17 | Sprint 18 | `docs/sprints/sprint-17-network-log-adapters.md` → `sprint-18-network-log-adapters.md` |
| Sprint 18 | Sprint 19 | `docs/sprints/sprint-18-open-telemetry.md` → `sprint-19-open-telemetry.md` |
| TASKS-SPRINT-14 | TASKS-SPRINT-15 | `docs/tasks/TASKS-SPRINT-14-POWER-USER-TOOLS.md` → `TASKS-SPRINT-15-POWER-USER-TOOLS.md` |
| TASKS-SPRINT-15 | TASKS-SPRINT-16 | `docs/tasks/TASKS-SPRINT-15-CLEF-VIEWING.md` → `TASKS-SPRINT-16-CLEF-VIEWING.md` |
| TASKS-SPRINT-17 | TASKS-SPRINT-18 | `docs/tasks/TASKS-SPRINT-17-NETWORK-LOG-ADAPTERS.md` → `TASKS-SPRINT-18-NETWORK-LOG-ADAPTERS.md` |
| TASKS-SPRINT-18 | TASKS-SPRINT-19 | `docs/tasks/TASKS-SPRINT-18-OPEN-TELEMETRY.md` → `TASKS-SPRINT-19-OPEN-TELEMETRY.md` |

### Cross-References to Update

- **README.md**: Update the `Sprint 17 Network Adapter Plan` section (lines 128-133) to reference Sprint 18.
- **docs/CONNECTIVITY-DESIGN.md**: Update header reference from "Sprint 16" to "Sprint 17" (or "Sprint 18" depending on context), update task file references, and sprint number references.
- **Renamed sprint docs**: Update headings, cross-references to other sprints, and task file references within each renamed file.
- **Renamed task docs**: Update top-level heading, section headings, task IDs (e.g., 14.x → 15.x), dependency references, and cross-references to other sprint/task docs.

### Historical Records NOT to Change

`docs/project_memory.md` contains historical records that reference original Sprint 14 (Power User Tools) as a past decision. These records describe what was *done* at that time, not active roadmap references. Changing them would be misleading — they accurately describe the numbering as it was when the work was completed. These will be preserved as-is.

### Assumptions

- **Popup interaction**: Clicking a truncated cell opens a lightweight popup anchored near the cell, displaying the full value with a copy button. Clicking outside or pressing Escape dismisses it. Keyboard focus moves to the popup when opened.
- **Grid line styling**: Horizontal lines between every row, vertical lines between every column. Color: `onSurface` at ~10-12% alpha in dark mode, ~15-18% in light mode. Selected rows get a primary-tinted background; hovered rows get a slightly lighter tint. Alternating rows keep subtle shade differences. Grid lines are drawn with `drawLine` in a custom `Canvas` for performance.
- **Shape refinement**: Dialog corners change from `RoundedCornerShape(16.dp)` to `RoundedCornerShape(4.dp)` or `0.dp`. Button corners change from `RoundedCornerShape(8.dp)` to `RoundedCornerShape(4.dp)` or `0.dp`.
- **Palette specifics**: Wizard background uses `surfaceVariant` with a cool-gray/slate shift. Accent colors (selected state, active toggle) use the existing primary blue (`0xFF00A3E0` dark / `0xFF007ACC` light). Button backgrounds use primary blue. Previously purple THREAD role pills use `Color(0xFF607D8B)` (blue-gray) instead of `Color(0xFF9B59B6)`.
- **No new preferences schema change**: The Compact/Full toggle is stored in the existing `WindowPreference` model as a new boolean field, serialized via the existing `JsonPreferencesRepository`.

### Open Questions

- Whether the grid-line visibility should be independently toggleable or always-on when grid lines are implemented.
- Whether the Pattern Wizard's `tonalElevation` (currently 8.dp) should be reduced alongside the corner-radius change.
- Whether the existing `PatternTheme.roleColor` for THREAD (currently purple) should change to blue-gray, or if only the wizard background/buttons need palette changes.

# Technical Design

### Sprint Doc Structure

Follow the existing convention from `docs/sprints/sprint-13-pattern-wizard.md`:

```markdown

# Sprint 14: UI Polish

## 1. Goal
## 2. Scope
### 2.1. Table Text Display Modes
### 2.2. Table Grid Lines
### 2.3. Pattern Wizard Color Refinement
### 2.4. Pattern Wizard Shape Refinement
## 3. Design Contract
### 3.1. Compact vs Full Display Mode
### 3.2. Grid Line Rendering
### 3.3. Pattern Wizard Palette and Shape
## 4. Out of Scope
## 5. Key Decisions
## 6. Definition of Done
```

### Task Doc Structure

Follow the existing convention from `docs/tasks/TASKS-SPRINT-13-PATTERN-WIZARD.md`:

```markdown

# TASKS: Sprint 14 - UI Polish

## 14. Sprint 14: UI Polish
### 14.0. Goal
### 14.1. In Scope
### 14.2. Out of Scope
### 14.3. Dependencies
### 14.4. Scope-to-Workstream Mapping
### 14.5. Table Text Display Modes (Compact vs Full)
### 14.6. Table Grid Lines
### 14.7. Pattern Wizard Color Refinement
### 14.8. Pattern Wizard Shape Refinement
### 14.9. Visual Regression and Usability Verification
### 14.10. Documentation Updates
### 14.11. Acceptance Criteria
```

### Key Design Details

**Table Text Display Modes**:
- `LogList.kt` already has `calculateColumnWidthToContent` in `ColumnSizingCalculator.kt`. The Compact mode will truncate cell text at the column boundary with ellipsis, using `TextOverflow.Ellipsis`.
- A popup composable (e.g., `CellValuePopup.kt`) will show the full value on click, with a copy-to-clipboard button.
- The toggle is stored in `WindowPreference` as `useCompactCellMode: Boolean = true`.
- The popup must handle multiline values, structured JSON content, and null/empty cells gracefully.

**Table Grid Lines**:
- Render using `Canvas` `drawLine` in the existing `LogEntryRow` composable in `LogList.kt`.
- Horizontal lines: full-width, drawn at the bottom of each row.
- Vertical lines: drawn at column boundaries, using column widths from the existing layout system.
- Colors: `onSurface` at ~10-12% alpha (dark), ~15-18% alpha (light).

**Pattern Wizard Colors**:
- Affected files: `PatternWizardDialog.kt` (Surface tonalElevation, button colors), `PatternTheme.kt` (roleColor for THREAD), `PatternTokenBar.kt` (token pill backgrounds).
- New palette: Remove purple `Color(0xFF9B59B6)`, replace with blue-gray `Color(0xFF607D8B)` for THREAD role. Wizard background uses `surfaceVariant` with cool-gray shift.

**Pattern Wizard Shapes**:
- `PatternWizardDialog.kt`: Change `MaterialTheme.shapes.large` → `RoundedCornerShape(4.dp)` on the main Surface.
- Change `RoundedCornerShape(8.dp)` on the Apply button → `RoundedCornerShape(4.dp)`.
- Change `RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)` on the footer Surface → `RoundedCornerShape(0.dp)`.

### Renumbering Details

Each renamed sprint doc needs:
1. Heading change (e.g., `# Sprint 14: Power User...` → `# Sprint 15: Power User...`)
2. All internal sprint number references updated
3. Task file references updated (e.g., `TASKS-SPRINT-14-...` → `TASKS-SPRINT-15-...`)
4. Dependency references to other sprints updated

Each renamed task doc needs:
1. Top heading and section heading updated
2. All task IDs updated (e.g., `14.5.1` → `15.5.1`)
3. Dependency references to other sprint/task docs updated
4. All cross-references within the document updated

**Note**: `TASKS-SPRINT-17-NETWORK-LOG-ADAPTERS.md` uses task IDs like `13.3.1` (preserved from the original Sprint 8 connectivity plan). These preserved IDs should NOT be changed — only the sprint number in the file name, heading, and cross-references should be updated.

# Delivery Plan

The delivery plan is structured as sequential stages. Each stage represents a concrete deliverable.

# Delivery Steps

### ✓ Step 1: Create Sprint 14 sprint document
`docs/sprints/sprint-14-ui-polish.md` is created following the existing sprint doc conventions.

- Write the full sprint document with sections: Goal, Scope (4 subsections), Design Contract, Out of Scope, Key Decisions, Definition of Done.
- Document the Compact vs Full display mode with popup reveal, per-window toggle, and interaction design.
- Document the grid line specification (horizontal + vertical, colors, row states).
- Document the blue-gray slate palette and shape refinement for the Pattern Wizard.
- Include the blue-gray slate palette color values and their usage mapping.
- Include the shape spec (corner radii changes) with the specific files and values to change.

### ✓ Step 2: Create Sprint 14 task document
`docs/tasks/TASKS-SPRINT-14-UI-POLISH.md` is created following the existing task doc conventions.

- Write hierarchical checkbox task IDs (14.0 through 14.11).
- Include 14.5 for Table Text Display Modes with sub-tasks (Compact mode implementation, popup component, toggle wiring, edge cases).
- Include 14.6 for Table Grid Lines with sub-tasks (horizontal lines, vertical lines, row state rendering, theme integration).
- Include 14.7 for Pattern Wizard Color Refinement with sub-tasks (palette audit, roleColor updates, background/button color updates).
- Include 14.8 for Pattern Wizard Shape Refinement with sub-tasks (dialog corner radius, button corner radius, footer shape).
- Include 14.9 for Visual Regression and Usability Verification.
- Include 14.10 for Documentation Updates.
- Include 14.11 for Acceptance Criteria.
- Use the confirmed product decisions (Compact/Full, popup reveal, per-window, both grid lines, blue-gray slate palette).

### ✓ Step 3: Rename and renumber downstream sprint documents
All downstream sprint doc files are renamed and their content updated for the +1 renumbering.

- Rename `docs/sprints/sprint-14-power-user-tools.md` → `docs/sprints/sprint-15-power-user-tools.md` and update heading, all sprint number references, and task file references.
- Rename `docs/sprints/sprint-15-clef-viewing.md` → `docs/sprints/sprint-16-clef-viewing.md` and update heading, all sprint number references, task file references, dependency references, and verification commands.
- Rename `docs/sprints/sprint-16-extensibility-and-release.md` → `docs/sprints/sprint-17-extensibility-and-release.md` and update heading and references.
- Rename `docs/sprints/sprint-17-network-log-adapters.md` → `docs/sprints/sprint-18-network-log-adapters.md` and update heading and references.
- Rename `docs/sprints/sprint-18-open-telemetry.md` → `docs/sprints/sprint-19-open-telemetry.md` and update heading, all sprint number references, task file references, and dependency references.
- Preserve the Sprint 18 OpenTelemetry task IDs (18.x) — they just become 19.x.

### ✓ Step 4: Rename and renumber downstream task documents
All downstream task doc files are renamed and their content updated for the +1 renumbering.

- Rename `docs/tasks/TASKS-SPRINT-14-POWER-USER-TOOLS.md` → `docs/tasks/TASKS-SPRINT-15-POWER-USER-TOOLS.md` and update all headings (14.x → 15.x), task IDs, and dependency references.
- Rename `docs/tasks/TASKS-SPRINT-15-CLEF-VIEWING.md` → `docs/tasks/TASKS-SPRINT-16-CLEF-VIEWING.md` and update all headings (15.x → 16.x), task IDs, dependency references, and verification commands.
- Rename `docs/tasks/TASKS-SPRINT-17-NETWORK-LOG-ADAPTERS.md` → `docs/tasks/TASKS-SPRINT-18-NETWORK-LOG-ADAPTERS.md` and update the heading. Preserve the legacy `13.3.x` and `13.5.x` task IDs (they are intentionally preserved from the original Sprint 8 connectivity plan).
- Rename `docs/tasks/TASKS-SPRINT-18-OPEN-TELEMETRY.md` → `docs/tasks/TASKS-SPRINT-19-OPEN-TELEMETRY.md` and update all headings (18.x → 19.x), task IDs, and dependency references.

### ✓ Step 5: Update active roadmap cross-references
All active roadmap references in README.md and CONNECTIVITY-DESIGN.md are updated to reflect the new numbering.

- Update `README.md` lines 128-133: Change "Sprint 17 Network Adapter Plan" to "Sprint 18 Network Adapter Plan", update sprint doc and task doc references to sprint-18 and TASKS-SPRINT-18.
- Update `docs/CONNECTIVITY-DESIGN.md`: Change line 5 header from "Sprint 16" to "Sprint 17" (or appropriate target), update all sprint number references to match the new numbering, update task file references on lines 207-224.
- Do NOT update `docs/project_memory.md` — historical records about original Sprint 14 (Power User Tools) are preserved as-is to avoid historical ambiguity.
- Verify all cross-references are consistent by searching for any remaining old sprint/filename references.