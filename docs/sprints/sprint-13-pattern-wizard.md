# Sprint 13: Pattern Wizard, Live Preview & Directory Mappings

## 1. Goal
Deliver a UI-first workflow that shows a best-guess log pattern as soon as a log opens, lets the user edit that draft with live preview feedback, and applies approved mappings through the existing parser-loading path without regressing structured JSON support.

## 2. Scope

### 2.1. First-Load Pattern Prompt & 5-Zone UI Layout
- Show a pattern wizard or modal immediately when a text log is opened and the format is guessed or still unresolved.
- Structure the wizard into 5 synchronized zones defined in `docs/PATTERN-WIZARD-UI-DESIGN.md`:
  - **Zone 1 (Header & Importer)**: Presets dropdown, format string importer (`logback.xml`, Serilog patterns), and directory persistence toggle.
  - **Zone 2 (Interactive Token Bar)**: Color-coded field pills (`Timestamp`, `Level`, `Thread`, `Logger`, `Message`, `Custom Property`), delimiter chips, and token configuration flyout.
  - **Zone 3 (Color-Coded Sample Line Inspector)**: Monospace sample line viewer with synchronized background color spans, error underlines, and point-and-click text span extraction.
  - **Zone 4 (Live Table Grid Preview)**: Compact table preview with dynamic columns mapped directly from active tokens.
  - **Zone 5 (Match Health & Action Bar)**: Match confidence badge, error diagnostics drawer, reset, cancel, and apply actions.
- Prefill the editor with the system best guess based on sampled lines.
- Keep structured JSON logs on the existing structured path unless the source is genuinely text-pattern-driven.

#### HITL Review Checkpoint
- Confirm the initial prompt appears at the right moment and does not get in the way of already-correct structured JSON detection.
- Confirm the best-guess draft, 5-zone layout, token pills, and sample lines are understandable before parser internals are extended further.

### 2.2. Editing Experience and Live Preview
- Provide an interactive, visual token bar with clickable pills and delimiter chips alongside direct text import.
- Support importing common pasted pattern formats, starting with Logback/Log4J-style layouts and Serilog-style text patterns where feasible.
- Support point-and-click field extraction from sample line text selections.
- Update a sample-based preview immediately as the draft changes so users can see how fields map onto real lines and table columns.
- Include a visible mapping/field-preview surface in the wizard so the UI is useful before the full table reload path is invoked.

#### HITL Review Checkpoint
- Confirm the editor is easy to understand, token configuration popovers are intuitive, and live preview feedback is fast enough to guide correction.
- Confirm paste/import workflows feel more useful than forcing users to manually recreate patterns from scratch.

### 2.3. Apply and Reload Flow
- Keep editing in a draft overlay rather than mutating the active parser on every keystroke.
- Use `Apply` to compile the approved draft into the runtime template path and reload through the existing `LogLoadingCoordinator` flow.
- Keep the main log table authoritative only after apply so the preview path stays lightweight and safe.
- Ensure reopened wizard edits can be canceled without disturbing the current active parser selection.

#### HITL Review Checkpoint
- Confirm the split between draft preview and applied parser feels predictable.
- Confirm applying a pattern refreshes the main log table in a way that matches what the preview promised.

### 2.4. Directory-Scoped Persistence and Reopenability
- Persist approved pattern mappings by source directory in user preferences stored outside the log directory.
- Reuse persisted mappings automatically when later files are opened from the same directory identity.
- Support local, SFTP, and S3 directory identities using the same normalized source conventions as the current loading flow.
- Add a reopen entry point from the active log UI, centered around the current parser/status controls.

#### HITL Review Checkpoint
- Confirm reused mappings apply to the right directory scope and do not feel surprising across local and remote sources.
- Confirm the reopen entry point is discoverable enough for users who want to refine a mapping later.

### 2.5. Structured-Log Compatibility and Preview-Only Placeholder Support
- Preserve the current `JsonLogParser` / `JsonMapping` structured JSON behavior as the authoritative path for JSON logs.
- Treat Serilog `@mt` placeholders such as `{version}` and `{netversion}` as preview-visible annotations in Sprint 13.
- Do not introduce a second-pass semantic extraction pipeline for structured placeholders in this sprint.
- Keep multiline text-log behavior aligned with the existing template parser path so preview and applied parsing stay consistent.

#### HITL Review Checkpoint
- Confirm structured JSON logs still open correctly without being forced through the text-pattern wizard unnecessarily.
- Confirm preview-only placeholder visibility is sufficient for Sprint 13 and that deeper extraction can remain deferred.

## 3. Dependencies and Ownership Boundaries
- `:domain` owns the canonical persisted pattern definition and directory-scoped preference schema.
- `:core` owns heuristic enrichment, canonical-pattern compilation, importer behavior, preview compilation, and persistence implementation.
- `:ui` owns wizard state, prompt/reopen flows, editor interactions, and preview rendering organized under `ui/src/main/kotlin/com/klogviewer/ui/components/pattern/` (detailed in `docs/PATTERN-WIZARD-UI-DESIGN.md`).
- Existing structured JSON parsing remains owned by the current structured parser/mapping pipeline.
- UI Design Specification: `docs/PATTERN-WIZARD-UI-DESIGN.md`.

## 4. Design Contract

### 4.1. Pattern Wizard UI Architecture
- The wizard UI follows the 5-zone interactive model detailed in `docs/PATTERN-WIZARD-UI-DESIGN.md`:
  - `PatternWizardDialog.kt`: Modal container and flow coordinator.
  - `PatternImporterBar.kt`: Preset selector and format string paste importer.
  - `PatternTokenBar.kt` & `PatternTokenConfigPopover.kt`: Interactive token bar with field pills, delimiter chips, reordering, and field configuration popover.
  - `SampleLineInspector.kt` & `SampleLineSelectionPopup.kt`: Color-coded sample lines with synchronized span backgrounds, error diagnostics, and point-and-click span extraction.
  - `PatternTablePreview.kt`: Live preview table with dynamic columns derived from token mappings.
  - `PatternMatchSummary.kt`: Match health badge and error diagnostics drawer.

### 4.2. Canonical Pattern Model
- Persist one internal pattern definition rather than raw Logback or Serilog text.
- The canonical model should capture:
  - normalized directory key
  - ordered pattern segments/tokens
  - field-role metadata for timestamp, level, thread, logger, message, exception, and custom properties
  - compile-time regex/timestamp metadata needed to build a `LogTemplate`
  - imported-original pattern text when the draft came from paste/import
  - preview-only placeholder annotations for structured message-template tokens such as Serilog `@mt`
- The same canonical model is used by three flows: heuristic draft creation, live preview compilation, and persisted directory mapping reuse.

### 4.3. Import and Compilation Pipeline
- Common pasted patterns are converted through `import -> canonical draft -> compile/runtime template`.
- Initial import support targets Logback/Log4J-style `%d`, `%thread`, `%level`, `%logger`, `%msg`-style layouts and Serilog-style text patterns where feasible.
- Importers should preserve the original pasted text in metadata so the wizard can explain where the draft came from.
- Compilation must continue to feed the existing template parser path rather than introducing a parallel parser framework.

### 4.4. Preview Versus Apply
- Preview reads sampled lines only and returns mapped fields, unmatched fragments, and placeholder annotations quickly enough for interactive editing.
- Preview must not mutate active window parser state.
- `Apply` is the boundary where the current draft is validated, compiled, persisted by directory, and handed to `LogLoadingCoordinator` for the authoritative reload.
- Canceling or closing the wizard leaves the currently applied parser untouched.

### 4.5. Directory Identity Rules
- Local mappings key by normalized parent-directory path.
- SFTP mappings key by connection identity plus normalized remote directory path.
- S3 mappings key by bucket identity plus normalized prefix/directory path.
- The same directory key rules must be used in first-open lookup, apply-time persistence, and later reuse.

### 4.6. Structured Placeholder Handling
- Structured JSON detection remains authoritative and happens before any text-pattern wizard path is forced onto a file.
- When a structured entry exposes a message template such as `"@mt":"Starting ... {version}"`, placeholder tokens are surfaced in preview only.
- Sprint 13 does not reinterpret those placeholders into a second set of parsed columns during normal log loading.

## 5. Out of Scope
- Replacing the active parser continuously on every draft edit.
- Full semantic extraction of structured message-template placeholders from Serilog `@mt` values.
- Storing mappings inside log directories.
- Reworking unrelated structured filtering, query-builder, or inspector semantics.

## 6. Key Decisions
- **Draft overlay architecture**: detection produces a draft, preview stays sample-based, and `Apply` alone updates the active parser path.
- **5-zone interactive UI model**: structured around importer bar, interactive token pills, color-coded sample lines, live table preview, and match health (`docs/PATTERN-WIZARD-UI-DESIGN.md`).
- **Canonical internal pattern model**: the editor and persistence format use one KLogViewer-owned representation with importers for common pasted syntaxes.
- **UI-first sequencing**: the first deliverable is the visible pattern prompt/editor workflow before deeper runtime refinements.
- **Directory-scoped persistence**: approved mappings live in user settings and are reused by normalized directory identity.
- **Structured compatibility first**: JSON parsing remains authoritative and nested placeholder support is preview-only in this sprint.
- **Deferred decisions tracked explicitly**: postponed design or implementation branches must be recorded in `docs/deferred_decisions.md`.

## 7. Definition of Done
- [ ] Opening an unrecognized or heuristically detected text log shows a best-guess pattern wizard before final mapping is committed.
- [ ] Editing the draft updates preview/mapping output immediately from sampled lines.
- [ ] Applying a pattern reloads the main log table through the existing loading path.
- [ ] Approved mappings are reused for later files in the same normalized directory scope.
- [ ] Structured JSON logs continue to auto-detect and render through the existing structured path.
- [ ] The wizard can be reopened later from the log UI.
- [ ] Deferred items are captured in `docs/deferred_decisions.md` with revisit triggers.
