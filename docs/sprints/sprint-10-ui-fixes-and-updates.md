# Sprint 10: UI Fixes & Updates

## 1. Goal
Improve day-to-day log analysis workflows with focused usability upgrades for log management, display control, and interaction ergonomics.

## 2. Scope

### 2.1. In-Tab Log File Drop-down Management
- Convert the list of files in the current tab into a drop-down list.
- Add a checked (`[x]`) control next to each item to remove that log from the current tab.

### 2.2. Font Selection for Log View
- Provide a `Font` dialog to change font family and font size for the log view.
- Restrict selectable fonts to fixed-width fonts only.
- Use the operating system/system font dialog where feasible.

### 2.3. Line Selection and Clipboard Copy
- Support selecting one or more log lines in the log view.
- Allow copying selected lines to the system clipboard.

### 2.4. Toolbar Refresh Action
- Add a `Refresh` button to the toolbar.
- Refresh reconnects and disconnects as needed when the viewer is currently disconnected.

### 2.5. Time Filter Clear Option
- Add a `Clear` option to the time filter menu (alongside options like "Last 5 minutes").
- Implement clear as a logical reset by setting filter time to "now" so only subsequent log lines are shown.

### 2.6. Context Menu Support
- Add right-click context menu support in the log UI.
- Provide `Copy`, `Refresh`, and `Clear` context actions.
- Ensure `Refresh` and `Clear` match toolbar/filter behavior.

### 2.7. Drag-and-Drop File Import
- Support dragging files into the current window.
- Dropping on the log view adds the log to the current tab.
- Dropping on the tab bar creates a new tab and adds the log there.

### 2.8. Source Badge Hover Tooltip
- For multi-log tabs, show a colored source button per log line.
- On hover, display the source log filename in a tooltip.

### 2.9. Per-Source Row Background Differentiation
- For multi-log tabs, add subtle gray background shade variants by source to improve visual separation.

## 3. Key Decisions
- **Interaction Consistency**: `Refresh` and `Clear` actions must behave identically from toolbar and context menu.
- **Readability First**: Limit font choices to fixed-width families to preserve alignment and scanning.
- **Non-Destructive Clear**: `Clear` is logical (time-reset), not destructive deletion of loaded source data.

## 4. Definition of Done
- [ ] Users can manage logs in a tab from a drop-down list and remove individual logs.
- [ ] Users can open a `Font` dialog and apply fixed-width font family and size to the log view.
- [ ] Users can select and copy one or more log lines to the clipboard.
- [ ] A toolbar `Refresh` action is available and works for connected/disconnected states.
- [ ] Time filter includes a `Clear` option that resets to "now".
- [ ] Context menu supports `Copy`, `Refresh`, and `Clear` with matching behavior.
- [ ] Drag-and-drop supports drop-to-current-tab and drop-to-new-tab interactions.
- [ ] Source buttons show source filename tooltips on hover.
- [ ] Multi-source rows use subtle gray background differentiation by log source.
