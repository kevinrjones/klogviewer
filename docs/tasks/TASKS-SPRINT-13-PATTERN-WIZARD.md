# TASKS: Sprint 13 - Pattern Wizard, Live Preview & Directory Mappings

## 13. Sprint 13: Pattern Wizard, Live Preview & Directory Mappings

### 13.0. Goal
Deliver the first-load pattern wizard workflow, live sample-based preview, directory-scoped mapping persistence, and reopenable editing flow without regressing structured JSON parsing.

### 13.1. In Scope
- Best-guess prompt when a text log is opened.
- Editable canonical pattern draft with common pattern import support.
- Live preview/mapping feedback from sampled lines.
- Apply/reload flow through the existing loading pipeline.
- Directory-scoped mapping persistence in user preferences.
- Reopenable wizard access from the active log UI.
- Professional interaction polish: hover sync, undo/redo, keyboard model, resampling, escape hatches, and dialog ergonomics (`docs/PATTERN-WIZARD-UI-DESIGN.md` §6).
- Saved-mapping mismatch recovery and a lightweight saved-mapping management surface.
- Structured JSON compatibility and multiline regressions.
- Deferred-decision documentation updates when scope is postponed.

### 13.2. Out of Scope
- Continuous main-table reparsing on each draft edit.
- Full second-pass extraction of structured Serilog `@mt` placeholders.
- Storage of mappings inside log directories.
- Unrelated structured filtering/query-builder redesign.

### 13.3. Dependencies
- Required architectural guidance: `docs/sprints/sprint-13-pattern-wizard.md`.
- Required UI design document: `docs/PATTERN-WIZARD-UI-DESIGN.md`.
- Required decision record: `docs/adr/adr-043-pattern-wizard-draft-overlay-and-directory-mappings.md`.
- Required compatibility context: `docs/adr/adr-014-structured-logging.md`, `docs/adr/adr-019-template-based-log-parsing.md`, `docs/adr/adr-020-multiline-log-aggregation.md`, and `docs/adr/adr-030-extract-log-loading-coordinator.md`.
- Required backlog reference for postponed scope: `docs/deferred_decisions.md`.

### 13.4. Scope-to-Workstream Mapping
- First-load UI + wizard components (`13.5`) -> `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/`, `ui/mvi`, and `ui/viewmodel` prompt flow.
- Preview/edit/apply orchestration (`13.6`) -> sampled-line preview state, apply intents, and reload coordination.
- Canonical model + persistence (`13.7`) -> `domain/model/UserPreferences.kt`, preference schema, and repository persistence.
- Import/compile/runtime parser support (`13.8`) -> `core/parser` heuristic, importer, compiler, and template integration.
- Multi-source mixed-pattern windows (`13.9`) -> per-source resolution, timestamp merge, source-aware display, multi-source wizard, and file-override persistence.
- Regression safety (`13.10`) -> structured JSON, multiline behavior, and reopenability coverage.
- Verification and docs upkeep (`13.11`) -> tests, checks, deferred decisions, and memory/readme follow-through.

### 13.5. First-Load Wizard UI and Reopenable Editing
- [x] 13.5.1. Add wizard visibility, draft pattern representation, and sample lines to `ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerState.kt` and define corresponding intents/events in `KLogViewerIntent.kt`.
- [x] 13.5.2. Implement top-level modal container `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternWizardDialog.kt` managing 5-zone layout, scrolling, and action buttons.
- [x] 13.5.3. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternImporterBar.kt` with preset selection dropdown, raw format string paste input field (`logback.xml`, Serilog), import trigger, and directory persistence toggle.
- [x] 13.5.4. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternTokenBar.kt` rendering interactive field pills (`Timestamp`, `Level`, `Thread`, `Logger`, `Message`, `Custom Property`), delimiter chips, reordering controls, and `+ Add Field` buttons.
- [x] 13.5.5. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternTokenConfigPopover.kt` flyout editor to configure token column role, custom property names, date/time format patterns, and matching flags.
- [x] 13.5.6. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/SampleLineInspector.kt` with monospace font, color-coded background spans matching active token pills, line carousel navigation (`Line X of Y`), and parse failure annotations.
- [x] 13.5.7. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/SampleLineSelectionPopup.kt` allowing point-and-click text span extraction to create new tokens from unmapped sample segments.
- [x] 13.5.8. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternTablePreview.kt` compact log grid rendering parsed sample rows into dynamic columns matching mapped tokens.
- [x] 13.5.9. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternMatchSummary.kt` displaying match health confidence badge (e.g. `10/10 matched`) and error diagnostics drawer.
- [x] 13.5.10. Wire automatic first-load prompt for heuristically detected text logs in `WorkspaceLogLoader.kt` / `LogLoadingCoordinator.kt` and add reopen action from `StatusBar.kt`.
- [x] 13.5.11. Ensure cancel and close actions remain completely non-destructive to the current active parser state.
- [x] 13.5.12. Add the always-available `Skip — open as plain text` escape hatch and the collapsed high-confidence confirmation banner variant (`Apply / Review / Skip`).
- [x] 13.5.13. Implement bidirectional hover synchronization between token pills, sample line spans, and preview table columns.
- [x] 13.5.14. Implement in-wizard undo/redo for all draft mutations and the keyboard/focus model (`Esc`, `Cmd/Ctrl+Enter`, arrow-key pill navigation, `Delete`).
- [x] 13.5.15. Implement dialog ergonomics: resizable wizard with remembered size, Zone 3/4 splitter, subtle pill animations, and preview level badges matching the main table styling.
- [x] 13.5.16. Add `@Preview` composables (light/dark, matched/error states) for every component under `ui/components/pattern/` so HITL reviews can render the UI headlessly.
- [x] 13.5.17. Human in the Loop review: validate the 5-zone UI layout, interactive token pills, color-coded sample 
  spans, hover sync, undo/redo, keyboard model, live table preview, and reopenability against real sample files before proceeding to deeper parser integration.

### 13.6. Live Preview and Apply/Reload Flow
- [x] 13.6.1. Introduce a sample-based preview state/service boundary that updates as the draft changes without reloading the full table.
- [x] 13.6.2. Wire preview refresh into the draft editor flow in `ui` so field mappings and unmatched fragments are visible immediately, with ~150 ms debounce, off-UI-thread parsing, stale-result discard, and a <250 ms visible-update budget.
- [x] 13.6.3. Add the `Resample` control drawing sample lines from head, middle, and tail of the file, with multiline entries grouped and long lines soft-wrapped with expand toggles.
- [x] 13.6.4. Add `Apply` handling in `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/LogLoadingCoordinator.kt` so approved drafts compile and reload through the existing window-loading path.
- [x] 13.6.5. Ensure preview results and applied parser results stay aligned for the same sample lines.
- [x] 13.6.6. Human in the Loop review: validate draft-preview responsiveness, resampling, and the apply/reload 
  experience against real sample logs.

### 13.7. Canonical Pattern Model and Directory-Scoped Persistence
- [x] 13.7.1. Extend `domain/src/main/kotlin/com/klogviewer/domain/model/UserPreferences.kt` with directory-scoped mapping persistence types for canonical pattern definitions.
- [x] 13.7.2. Define the canonical pattern segment/token model in `:domain`, including imported-origin metadata and preview-only structured placeholder annotations.
- [x] 13.7.3. Implement persistence support in `core/src/main/kotlin/com/klogviewer/core/repository/JsonPreferencesRepository.kt`.
- [x] 13.7.4. Define and reuse normalized directory identity rules for local, SFTP, and S3 sources across lookup and save flows.
- [x] 13.7.5. Ensure saved mappings are reused automatically when later files open from the same directory identity.
- [x] 13.7.6. Detect when a saved mapping falls below a match threshold on a newly opened file and reopen the wizard preloaded with the saved mapping plus diagnostics instead of silently mis-parsing.
- [x] 13.7.7. Add a lightweight saved-mapping management surface listing directory key, source type, pattern summary, and last-used time with delete and open-in-wizard actions.
- [x] 13.7.8. Human in the Loop review: validate the saved-mapping behavior, mismatch recovery, management surface, 
  and confirm the directory scope feels correct across source types.

### 13.8. Importers, Heuristic Drafting, and Runtime Compilation
- [x] 13.8.1. Extend `core/src/main/kotlin/com/klogviewer/core/parser/HeuristicProbe.kt` so detection returns enough metadata to seed the canonical draft.
- [x] 13.8.2. Add importer support for common pasted Logback/Log4J-style and Serilog-style text patterns into the canonical model.
- [x] 13.8.3. Add compiler support from canonical pattern definitions to runtime `LogTemplate` / `TemplateLogParser` behavior.
- [x] 13.8.4. Keep multiline compatibility aligned with `core/src/main/kotlin/com/klogviewer/core/source/MultilineProcessor.kt` and the existing template parser path.
- [x] 13.8.5. Surface Serilog `@mt` placeholders in preview only, without changing the structured JSON runtime semantics.
- [ ] 13.8.6. Human in the Loop review: validate imported-pattern usability and confirm the canonical model feels 
  flexible enough before follow-on polish.

### 13.9. Multi-Source Mixed-Pattern Windows
Design context: sprint doc §2.6 / §4.6 and `docs/adr/adr-043-pattern-wizard-draft-overlay-and-directory-mappings.md` (per-source resolution and file overrides).
- [x] 13.9.1. Add `sourcePatterns: Map<SourceId, SourcePatternRef>` to `LogWindow` (`ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerState.kt`) and `WindowPreference` (`domain/src/main/kotlin/com/klogviewer/domain/model/UserPreferences.kt`), keeping legacy `parserName`/`patternDraft` fields deserializable.
- [x] 13.9.2. Add `filePatternOverrides` to `UserPreferences` keyed by normalized file identity or glob; lookup order per source: file override → directory mapping → heuristic.
- [x] 13.9.3. Update `PreferencesStateMapper`, `JsonPreferencesRepository` round-trip, and session restore for the new schema, with serialization and backward-compat tests.
- [x] 13.9.4. Change detection in `WorkspaceLogLoader.kt` to resolve `sourceId → ProbeResult` per source and `createLogFlows` to attach per-source parsers (structured JSON path untouched).
- [x] 13.9.5. Merge parsed streams by timestamp; implement near-tail binary-search insertion for live-tail out-of-order arrivals and anchor timestamp-less entries to file order.
- [x] 13.9.6. Union columns across source results via `mergeColumnsWithDiscoveredStatic`; add unit tests for merge ordering and anchoring.
- [x] 13.9.7. Add a filterable `Source` column with short deduped names and full-path tooltip plus per-source colour accent (left-edge stripe/badge) and source-visibility toggle in `ui/src/main/kotlin/com/klogviewer/ui/components/LogList.kt`.
- [x] 13.9.8. Extend `PatternWizardState` with per-source entries (status, sample lines, draft, preview) and an active-source selector in `PatternWizardDialog.kt`; auto-open only when ≥1 source is unresolved.
- [x] 13.9.9. Per-source Apply saves to directory mapping or file override per the persistence toggle; surface the missing-timestamp warning in `PatternMatchSummary.kt`.
- [x] 13.9.10. Extend `PatternWizardIntentHandler` and tests for multi-source intents; UI tests for `Source` column rendering, blank cells, and source filtering.
- [x] 13.9.11. Human in the Loop review: validate mixed-pattern interleaving, union columns, source identification, 
  multi-source wizard flow, and file-override persistence against real files.

### 13.10. Regression Protection for Structured and Existing Behavior
- [x] 13.10.1. Add or extend tests proving `JsonLogParser` / `JsonMapping` remain the authoritative path for structured JSON logs.
- [x] 13.10.2. Add or extend tests for multiline text-log behavior so preview/apply results remain consistent.
- [ ] 13.10.3. Add or extend tests for reopening the wizard after apply, cancel, and directory-mapping reuse flows.
- [ ] 13.10.4. Add or extend tests for SFTP/S3/local directory identity normalization and persisted mapping reuse.
- [ ] 13.10.5. Add or extend tests proving single-source windows behave exactly as before the multi-source changes.
- [x] 13.10.6. Human in the Loop review: validate that structured-log compatibility and regression scope look sufficient before sprint closure.

### 13.11. Verification, Deferred Decisions, and Closeout Docs
- [x] 13.11.1. Add/extend unit tests in `core` for canonical compilation, importers, heuristic draft creation, and directory-key persistence.
- [ ] 13.11.2. Add/extend UI/viewmodel tests in `ui` for first-open prompt state, live preview updates, apply flow, reopenability, and persisted reuse.
- [x] 13.11.3. Run relevant touched-module tests.
- [x] 13.11.4. Run `./gradlew check` before closing the sprint work.
- [x] 13.11.5. Run the required cyclomatic complexity review and decide whether follow-on complexity reduction tasks are needed.
- [ ] 13.11.6. Keep `docs/deferred_decisions.md` updated whenever work is consciously postponed.
- [ ] 13.11.7. Update `README.md` to reflect the shipped workflow when the sprint implementation is complete.
- [ ] 13.11.8. Update `docs/project_memory.md` with shipped scope, key decisions, gotchas, and test coverage areas when the sprint implementation is complete.

### 13.12. Acceptance Criteria
- [ ] Opening an unrecognized or heuristically detected text log shows the best-guess pattern wizard before final parser commitment.
- [ ] Editing the draft updates preview/mapping output immediately from sampled lines.
- [ ] Applying a pattern reloads the main log table using the existing loading path.
- [ ] Approved mappings are reused for later files in the same normalized directory scope.
- [ ] Structured JSON logs continue to auto-detect and render correctly.
- [ ] Serilog `@mt` placeholders are visible in preview without changing structured runtime parsing semantics.
- [ ] The wizard can be reopened later from the active log UI.
- [ ] The wizard never blocks viewing a file: skip/plain-text is always available and high-confidence detection collapses to a confirmation banner.
- [ ] Hover synchronization, undo/redo, and keyboard shortcuts work across all wizard zones.
- [ ] A saved mapping that stops matching triggers wizard re-entry with diagnostics rather than silent mis-parsing.
- [ ] Saved directory mappings can be listed and deleted from the management surface.
- [ ] Deferred items remain tracked in `docs/deferred_decisions.md`.
- [ ] Two files with different patterns load into one window, each parsed with its own pattern and interleaved by timestamp.
- [ ] Union columns render with blank cells where a source lacks a field; the `Source` column and colour accent identify origins.
- [ ] File overrides win over directory mappings for same-directory conflicts and persist/restore correctly.
- [ ] Sources without a parseable timestamp load with the approximate-interleaving warning and keep anchored file order.
- [ ] Live tail across sources inserts out-of-order arrivals at their timestamp position.
- [ ] The multi-source wizard shows per-source status and applying updates only the selected source's mapping.