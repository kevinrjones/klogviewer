# TASKS: Sprint 10 - UI Fixes & Updates

## 15. Sprint 10: UI Fixes & Updates

### 15.0. Scope-to-Workstream Mapping
- Tab/File management (`2.1`, `2.7`) -> `KLogViewerScreen`, tab/window workspace wiring, drag-and-drop entry points.
- Log window interaction (`2.2`, `2.3`, `2.6`) -> `LogList`, selection/copy flows, context actions.
- Toolbar/filter behavior (`2.4`, `2.5`) -> `FilterBar`, `FilterIntentHandler`, `UiToggleIntentHandler`, `TimeRangeFilterSupport`.
- Multi-source readability (`2.8`, `2.9`) -> source badge rendering, tooltip behavior, per-source row styling in `LogList`.
- Menu/desktop parity (`2.3`, `2.4`, `2.6`) -> `Main.kt` + shared ViewModel intent handlers.

### 15.1. In-Tab Log File Drop-down Management (`2.1`)
- [x] 15.1.1. Replace the current per-tab file list with a drop-down control scoped to the active tab/window.
- [x] 15.1.2. Render one entry per loaded source with a checked (`[x]`) remove control beside each item.
- [x] 15.1.3. Remove only the selected source from the active tab when its checked control is triggered.
- [x] 15.1.4. Keep tab/workspace state, visible rows, and source metadata consistent after source removal.

### 15.2. Font Selection for Log View (`2.2`)
- [x] 15.2.1. Add a `Font` action that opens a font configuration dialog for the log view.
- [x] 15.2.2. Restrict selectable font families to fixed-width/monospaced fonts.
- [x] 15.2.3. Use the operating-system/system font dialog where feasible and fall back gracefully where not available.
- [x] 15.2.4. Apply selected font family and size to rendered log rows in the active UI.
- [x] 15.2.5. Persist selected font settings so they are restored on restart/workspace reload.

### 15.3. Line Selection and Clipboard Copy (`2.3`)
- [x] 15.3.1. Support selecting one log line and extending selection to multiple lines in the log view.
- [x] 15.3.2. Implement copy action that writes selected lines to system clipboard in visible order.
- [x] 15.3.3. Expose copy through keyboard/menu/context entry points with shared behavior.
- [x] 15.3.4. Disable copy action when no lines are selected.

### 15.4. Toolbar Refresh Action (`2.4`)
- [x] 15.4.1. Add `Refresh` button/action to toolbar with discoverable label/tooltip.
- [x] 15.4.2. Implement refresh behavior to reconnect/disconnect as needed when current viewer is disconnected.
- [x] 15.4.3. Reuse the same intent/handler flow as existing connect-state toggles to avoid divergent behavior.
- [x] 15.4.4. Ensure refresh updates active sources without requiring tab/workspace recreation.

### 15.5. Time Filter Reset Option (`2.5`)
- [x] 15.5.1. Add `Reset` option to the time filter menu alongside preset ranges.
- [x] 15.5.2. Implement reset as clearing time bounds so all loaded log lines are shown.
- [x] 15.5.3. Keep non-time filters and current workspace context unchanged when reset is applied.
- [x] 15.5.4. Route reset through shared filter intent handling so behavior is deterministic.

### 15.6. Context Menu Support (`2.6`)
- [x] 15.6.1. Add right-click context menu support in the log UI.
- [x] 15.6.2. Provide `Copy` context action bound to the same selection/clipboard behavior as primary copy flows.
- [x] 15.6.3. Provide `Refresh` context action bound to the same behavior as toolbar refresh.
- [x] 15.6.4. Provide `Clear` context action bound to the same behavior as time filter clear.
- [x] 15.6.5. Reflect enabled/disabled menu states based on current selection and connection/filter context.

### 15.7. Drag-and-Drop File Import (`2.7`)
- [x] 15.7.1. Accept file drag-and-drop into the current application window.
- [x] 15.7.2. Dropping files on the log view adds sources into the current tab.
- [x] 15.7.3. Dropping files on the tab bar creates a new tab and loads dropped files there.
- [x] 15.7.4. Support multi-file drops and handle invalid/unsupported drops with non-blocking feedback.

### 15.8. Source Badge Hover Tooltip (`2.8`)
- [x] 15.8.1. For multi-source tabs, render a colored source badge/button per log line.
- [x] 15.8.2. Show source filename tooltip on hover for each source badge.
- [x] 15.8.3. Keep tooltip/source mapping accurate after source add/remove and live updates.

### 15.9. Per-Source Row Background Differentiation (`2.9`)
- [x] 15.9.1. Apply subtle gray background shade variants per source in multi-source tabs.
- [x] 15.9.2. Use deterministic source-to-shade mapping so rows remain visually stable across refreshes.
- [x] 15.9.3. Preserve readability/contrast with existing severity coloring, selection styling, and theme modes.

### 15.10. Verification & Testing
- [x] 15.10.1. Add/extend UI tests for drop-down source management and per-source removal behavior.
- [x] 15.10.2. Add/extend UI/state tests for fixed-width font selection, application, and persistence.
- [x] 15.10.3. Add/extend UI tests for multi-line selection and clipboard copy behavior.
- [x] 15.10.4. Add/extend ViewModel tests for toolbar/context `Refresh` parity in connected/disconnected states.
- [x] 15.10.5. Add/extend filter tests for dropdown `Reset` clear-time-bounds behavior and menu `Clear` reset-to-now parity.
- [x] 15.10.6. Add/extend UI tests for context menu actions (`Copy`, `Refresh`, `Clear`) and enablement states.
- [x] 15.10.7. Add/extend interaction tests for drag-drop to current tab vs tab bar new tab creation.
- [x] 15.10.8. Add/extend UI tests for source badge filename tooltips and per-source row background differentiation.
- [ ] 15.10.9. Run regression checks for workspace persistence and live-update consistency with Sprint 10 changes.