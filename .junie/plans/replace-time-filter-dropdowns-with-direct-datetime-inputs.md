---
sessionId: session-260527-213324-nkxd
---

# Requirements

### Overview & Goals
Replace the current `From`/`To` timestamp dropdown UX with practical date/time input controls in the filter bar, while preserving existing filter architecture and preset workflows.

### Current Behavior Confirmed
- `FilterBar` currently renders `From`/`To` via `DateTimeFilterDropdown` in `ui/src/main/kotlin/com/klogviewer/ui/components/FilterBar.kt`.
- The dropdown options are built from every parsed timestamp in the active window (`availableTimeFilterInstants`) in `KLogViewerScreen.LogTopBar` (`ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`).
- Time filter state currently lives in `LogWindow` (`timeFilterFrom`, `timeFilterTo`, `timeFilterFromInstant`, `timeFilterToInstant`, `timeFilterPreset`, `timeFilterValidationMessage`) in `ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerState.kt`.

### Scope
#### In Scope
- Replace dropdown-based timestamp selection with compact direct date/time input controls.
- Keep second-level precision (`yyyy-MM-dd HH:mm:ss` and existing ISO-compatible parsing behavior).
- Apply explicit ±1 second boundary tolerance to filtering.
- Preserve existing preset behavior (`Last N`, `Visible Window`, `Full Loaded Range`, `Custom`) and custom-mode transitions.
- Add clear actions for `From`, `To`, and full time filter.
- Add/update tests for parsing, validation, tolerance, filtering behavior, and UI regression away from per-entry timestamp dropdowns.

#### Out of Scope
- Re-architecting ViewModel/MVI flow.
- Changing unrelated dashboard, text, or level filter behavior.
- Introducing heavy date-picker dependencies.

### Clarification Captured
- Tolerance scope confirmed: **apply ±1 second to all range sources** (manual input, presets, and chart/range selections).

# Technical Design

### Current Implementation (inspected)
- UI source of problematic behavior:
  - `FilterBar.TimeFilterControls` builds `dateTimeOptions` from `availableEntryInstants`.
  - `DateTimeFilterDropdown` renders a clickable box and menu entries for `Any time` + all timestamps.
  - Files: `ui/src/main/kotlin/com/klogviewer/ui/components/FilterBar.kt`, `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`.
- State + intent flow:
  - `KLogViewerIntent.SetTimeFilterFrom`, `SetTimeFilterTo`, `ApplyTimeFilterPreset`, `ClearTimeFilter`.
  - `FilterIntentHandler` parses input with `TimeRangeFilterSupport.parseInstantOrNull`, validates, updates `LogWindow`, and sets preset to `CUSTOM` on manual edits.
  - Files: `ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerIntent.kt`, `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/FilterIntentHandler.kt`.
- Filter evaluation:
  - `LogFilterService.filter` combines level + text + time range.
  - Entries without parsable timestamp already fail the time-range predicate when range is active.
  - File: `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/LogFilterService.kt`.

### Key Decisions
1. **Use direct text date/time fields (no new picker dependency).**
   - Replace dropdown UI with compact text inputs using existing Compose primitives.
   - Keep parsing centralized in `TimeRangeFilterSupport`.
2. **Apply tolerance in filtering layer, not state layer.**
   - Keep stored `from/to` instants exact.
   - Expand boundary comparison at evaluation time (`from - 1s`, `to + 1s`) in `LogFilterService`.
3. **Preserve current state model and preset semantics.**
   - Reuse existing `LogWindow` fields and intent handlers.
   - Keep manual edits switching preset to `CUSTOM`.

### Proposed Changes
- `ui/src/main/kotlin/com/klogviewer/ui/components/FilterBar.kt`
  - Remove `DateTimeFilterDropdown` usage for `From`/`To`.
  - Replace with compact direct input controls (labeled `From`/`To`) with placeholder example `YYYY-MM-DD HH:mm:ss`.
  - Add per-field clear actions (`Clear From`, `Clear To`) plus existing full clear (`Clear time filter`).
  - Keep validation feedback visible in filter bar (icon + text/tooltip refinement).
  - Remove timestamp-options model (`TimeFilterDateTimeOption`) and option list generation.
- `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
  - Remove `availableTimeFilterInstants` derivation from `activeWindow.logs` and corresponding prop wiring into `FilterBar`.
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/LogFilterService.kt`
  - Apply one-second tolerance for active time ranges:
    - lower bound comparison against `from.minusSeconds(1)`
    - upper bound comparison against `to.plusSeconds(1)`
  - Keep behavior that entries without timestamps are excluded when time filter is active.
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/TimeRangeFilterSupport.kt`
  - Keep parser/validation as canonical input handling.
  - Add small helper(s) only if needed for safe bound expansion/readability.
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/FilterIntentHandler.kt`
  - Keep current parsing/validation and preset-to-`CUSTOM` behavior; adjust only if needed for new per-field clear button wiring.

### State Fields Affected
- **Retained (no model redesign):**
  - `LogWindow.timeFilterFrom`
  - `LogWindow.timeFilterTo`
  - `LogWindow.timeFilterFromInstant`
  - `LogWindow.timeFilterToInstant`
  - `LogWindow.timeFilterPreset`
  - `LogWindow.timeFilterValidationMessage`
- **Behavioral adjustment:** filtering comparisons become tolerance-aware while state values remain exact.

### Compatibility Notes
- Presets (`VISIBLE_WINDOW`, `FULL_LOADED_RANGE`, `LAST_*`, `CUSTOM`) continue to update fields and drive filtering exactly as today.
- Manual field edits continue to mark preset as `CUSTOM` via existing handler logic.
- No changes to persistence schema (`WindowPreference.timeFilterFrom/timeFilterTo/timeFilterPresetMinutes`).

# Testing

### Validation Approach
Update focused unit/UI tests around time-filter parsing + filtering and add UI regression coverage for control shape (text input vs timestamp dropdown).

### Tests to Change
- `ui/src/test/kotlin/com/klogviewer/ui/viewmodel/LogFilterServiceTimeRangeTest.kt`
  - Update/add cases for:
    1. `From`-only includes entries at/after `from - 1s`.
    2. `To`-only includes entries at/before `to + 1s`.
    3. `From` + `To` includes tolerated closed interval.
    4. Entries without timestamps are excluded when time range active.
- `ui/src/test/kotlin/com/klogviewer/ui/viewmodel/TimeRangeFilterSupportTest.kt`
  - Keep/extend validation coverage:
    - invalid `From`
    - invalid `To`
    - `From > To`
    - empty `From`/`To` means no active explicit range.
- `ui/src/test/kotlin/com/klogviewer/ui/viewmodel/DashboardIntentTest.kt`
  - Keep preset/range synchronization assertions green after UI/control refactor.
  - Verify manual edits still imply `CUSTOM` behavior through existing intent flow.
- New UI-level regression test (components or screen-level under `ui/src/test/kotlin/com/klogviewer/ui/components/` or `.../ui/test/`)
  - Assert time filter controls are direct text inputs with placeholder/example text.
  - Assert UI no longer exposes per-log-entry timestamp dropdown list behavior.
  - Assert clear-from, clear-to, and clear-all time filter actions work.

### Acceptance Mapping
- Covers required scenarios 1–10 from the issue, with core logic validated in unit tests and control behavior validated in Compose UI tests.

# Delivery Steps

### ✓ Step 1: Refactor filter bar time controls to direct date-time inputs
The filter bar exposes compact `From`/`To` direct date-time fields instead of timestamp dropdown menus.
- Update `FilterBar.kt` to replace `DateTimeFilterDropdown` with input controls that support typed values and placeholders (`YYYY-MM-DD HH:mm:ss`).
- Add per-field clear actions (`From`, `To`) and keep full time-filter clear action.
- Remove option-list plumbing (`availableEntryInstants`/`TimeFilterDateTimeOption`) from `FilterBar`.
- Update `KLogViewerScreen.LogTopBar` to stop deriving/passing `availableTimeFilterInstants` from log entries.

### ✓ Step 2: Implement tolerance-aware time-range evaluation while preserving preset/state flow
Time filtering applies ±1 second tolerance at boundaries for all range sources without changing the state model.
- Update `LogFilterService.filter` comparisons to evaluate against tolerated bounds (`from - 1s`, `to + 1s`).
- Preserve exclusion of entries without timestamp when a time range is active.
- Keep `FilterIntentHandler` parse/validation/preset-custom behavior intact, adjusting only wiring needed for new clear-from/clear-to UI actions.
- Keep preset synchronization (`ApplyTimeFilterPreset`, dashboard-selected ranges, manual edits -> `CUSTOM`) unchanged in behavior.

### ✓ Step 3: Expand regression coverage for parsing, validation, tolerance, and UI behavior
Automated tests verify second-precision input handling, tolerant filtering semantics, and removal of per-entry timestamp dropdown UX.
- Extend `LogFilterServiceTimeRangeTest.kt` with from-only, to-only, combined-range tolerance, and missing-timestamp exclusion scenarios.
- Extend `TimeRangeFilterSupportTest.kt` for invalid/empty/inverted input validation outcomes.
- Keep/adjust `DashboardIntentTest.kt` assertions for preset/range synchronization after UI refactor.
- Add a Compose UI regression test ensuring time controls are direct inputs (not entry-populated dropdowns) and clear actions behave as expected.