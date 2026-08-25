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
- Regression safety (`13.9`) -> structured JSON, multiline behavior, and reopenability coverage.
- Verification and docs upkeep (`13.10`) -> tests, checks, deferred decisions, and memory/readme follow-through.

### 13.5. First-Load Wizard UI and Reopenable Editing
- [ ] 13.5.1. Add wizard visibility, draft pattern representation, and sample lines to `ui/src/main/kotlin/com/klogviewer/ui/mvi/KLogViewerState.kt` and define corresponding intents/events in `KLogViewerIntent.kt`.
- [ ] 13.5.2. Implement top-level modal container `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternWizardDialog.kt` managing 5-zone layout, scrolling, and action buttons.
- [ ] 13.5.3. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternImporterBar.kt` with preset selection dropdown, raw format string paste input field (`logback.xml`, Serilog), import trigger, and directory persistence toggle.
- [ ] 13.5.4. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternTokenBar.kt` rendering interactive field pills (`Timestamp`, `Level`, `Thread`, `Logger`, `Message`, `Custom Property`), delimiter chips, reordering controls, and `+ Add Field` buttons.
- [ ] 13.5.5. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternTokenConfigPopover.kt` flyout editor to configure token column role, custom property names, date/time format patterns, and matching flags.
- [ ] 13.5.6. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/SampleLineInspector.kt` with monospace font, color-coded background spans matching active token pills, line carousel navigation (`Line X of Y`), and parse failure annotations.
- [ ] 13.5.7. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/SampleLineSelectionPopup.kt` allowing point-and-click text span extraction to create new tokens from unmapped sample segments.
- [ ] 13.5.8. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternTablePreview.kt` compact log grid rendering parsed sample rows into dynamic columns matching mapped tokens.
- [ ] 13.5.9. Implement `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/PatternMatchSummary.kt` displaying match health confidence badge (e.g. `10/10 matched`) and error diagnostics drawer.
- [ ] 13.5.10. Wire automatic first-load prompt for heuristically detected text logs in `WorkspaceLogLoader.kt` / `LogLoadingCoordinator.kt` and add reopen action from `StatusBar.kt`.
- [ ] 13.5.11. Ensure cancel and close actions remain completely non-destructive to the current active parser state.
- [ ] 13.5.12. Human in the Loop review: validate the 5-zone UI layout, interactive token pills, color-coded sample spans, live table preview, and reopenability against real sample files before proceeding to deeper parser integration.

### 13.6. Live Preview and Apply/Reload Flow
- [ ] 13.6.1. Introduce a sample-based preview state/service boundary that updates as the draft changes without reloading the full table.
- [ ] 13.6.2. Wire preview refresh into the draft editor flow in `ui` so field mappings and unmatched fragments are visible immediately.
- [ ] 13.6.3. Add `Apply` handling in `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/LogLoadingCoordinator.kt` so approved drafts compile and reload through the existing window-loading path.
- [ ] 13.6.4. Ensure preview results and applied parser results stay aligned for the same sample lines.
- [ ] 13.6.5. Human in the Loop review: validate draft-preview responsiveness and the apply/reload experience against real sample logs.

### 13.7. Canonical Pattern Model and Directory-Scoped Persistence
- [ ] 13.7.1. Extend `domain/src/main/kotlin/com/klogviewer/domain/model/UserPreferences.kt` with directory-scoped mapping persistence types for canonical pattern definitions.
- [ ] 13.7.2. Define the canonical pattern segment/token model in `:domain`, including imported-origin metadata and preview-only structured placeholder annotations.
- [ ] 13.7.3. Implement persistence support in `core/src/main/kotlin/com/klogviewer/core/repository/JsonPreferencesRepository.kt`.
- [ ] 13.7.4. Define and reuse normalized directory identity rules for local, SFTP, and S3 sources across lookup and save flows.
- [ ] 13.7.5. Ensure saved mappings are reused automatically when later files open from the same directory identity.
- [ ] 13.7.6. Human in the Loop review: validate the saved-mapping behavior and confirm the directory scope feels correct across source types.

### 13.8. Importers, Heuristic Drafting, and Runtime Compilation
- [ ] 13.8.1. Extend `core/src/main/kotlin/com/klogviewer/core/parser/HeuristicProbe.kt` so detection returns enough metadata to seed the canonical draft.
- [ ] 13.8.2. Add importer support for common pasted Logback/Log4J-style and Serilog-style text patterns into the canonical model.
- [ ] 13.8.3. Add compiler support from canonical pattern definitions to runtime `LogTemplate` / `TemplateLogParser` behavior.
- [ ] 13.8.4. Keep multiline compatibility aligned with `core/src/main/kotlin/com/klogviewer/core/source/MultilineProcessor.kt` and the existing template parser path.
- [ ] 13.8.5. Surface Serilog `@mt` placeholders in preview only, without changing the structured JSON runtime semantics.
- [ ] 13.8.6. Human in the Loop review: validate imported-pattern usability and confirm the canonical model feels flexible enough before follow-on polish.

### 13.9. Regression Protection for Structured and Existing Behavior
- [ ] 13.9.1. Add or extend tests proving `JsonLogParser` / `JsonMapping` remain the authoritative path for structured JSON logs.
- [ ] 13.9.2. Add or extend tests for multiline text-log behavior so preview/apply results remain consistent.
- [ ] 13.9.3. Add or extend tests for reopening the wizard after apply, cancel, and directory-mapping reuse flows.
- [ ] 13.9.4. Add or extend tests for SFTP/S3/local directory identity normalization and persisted mapping reuse.
- [ ] 13.9.5. Human in the Loop review: validate that structured-log compatibility and regression scope look sufficient before sprint closure.

### 13.10. Verification, Deferred Decisions, and Closeout Docs
- [ ] 13.10.1. Add/extend unit tests in `core` for canonical compilation, importers, heuristic draft creation, and directory-key persistence.
- [ ] 13.10.2. Add/extend UI/viewmodel tests in `ui` for first-open prompt state, live preview updates, apply flow, reopenability, and persisted reuse.
- [ ] 13.10.3. Run relevant touched-module tests.
- [ ] 13.10.4. Run `./gradlew check` before closing the sprint work.
- [ ] 13.10.5. Run the required cyclomatic complexity review and decide whether follow-on complexity reduction tasks are needed.
- [ ] 13.10.6. Keep `docs/deferred_decisions.md` updated whenever work is consciously postponed.
- [ ] 13.10.7. Update `README.md` to reflect the shipped workflow when the sprint implementation is complete.
- [ ] 13.10.8. Update `docs/project_memory.md` with shipped scope, key decisions, gotchas, and test coverage areas when the sprint implementation is complete.

### 13.11. Acceptance Criteria
- [ ] Opening an unrecognized or heuristically detected text log shows the best-guess pattern wizard before final parser commitment.
- [ ] Editing the draft updates preview/mapping output immediately from sampled lines.
- [ ] Applying a pattern reloads the main log table using the existing loading path.
- [ ] Approved mappings are reused for later files in the same normalized directory scope.
- [ ] Structured JSON logs continue to auto-detect and render correctly.
- [ ] Serilog `@mt` placeholders are visible in preview without changing structured runtime parsing semantics.
- [ ] The wizard can be reopened later from the active log UI.
- [ ] Deferred items remain tracked in `docs/deferred_decisions.md`.