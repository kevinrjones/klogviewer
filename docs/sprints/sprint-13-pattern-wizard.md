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
- When heuristic confidence is very high, open as a slim confirmation banner (`Apply / Review / Skip`) instead of the full editor.
- Always offer a **Skip — open as plain text** escape hatch so the wizard never blocks viewing the file.
- Keep structured JSON logs on the existing structured path unless the source is genuinely text-pattern-driven.

#### HITL Review Checkpoint
- Confirm the initial prompt appears at the right moment and does not get in the way of already-correct structured JSON detection.
- Confirm the best-guess draft, 5-zone layout, token pills, and sample lines are understandable before parser internals are extended further.

### 2.2. Editing Experience and Live Preview
- Provide an interactive, visual token bar with clickable pills and delimiter chips alongside direct text import.
- Support importing common pasted pattern formats, starting with Logback/Log4J-style layouts and Serilog-style text patterns where feasible.
- Support point-and-click field extraction from sample line text selections.
- Update a sample-based preview immediately as the draft changes so users can see how fields map onto real lines and table columns.
- Provide bidirectional hover synchronization: hovering a pill highlights matching sample spans and the preview column, and vice versa.
- Support in-wizard undo/redo of every draft mutation, plus keyboard shortcuts (`Esc` cancel, `Cmd/Ctrl+Enter` apply, arrow-key pill navigation).
- Provide a **Resample** control drawing sample lines from the head, middle, and tail of the file, with multiline entries grouped in the inspector.
- Keep preview recompilation debounced and off the UI thread within the performance budget defined in `docs/PATTERN-WIZARD-UI-DESIGN.md` §6.4.
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
- If a saved mapping stops matching a newly opened file (below a match threshold), reopen the wizard preloaded with the saved mapping and a clear diagnostic instead of silently rendering garbage rows.
- Provide a lightweight saved-mapping management surface (list, delete, open-in-wizard) for directory mappings across source types.
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

### 2.6. Multi-Source Mixed-Pattern Windows
A window can load files from multiple locations; some share a pattern, some don't, but interleaving requires a common timestamp. The following decisions are locked:
- **Per-source parsers**: each file/directory source in a window resolves its own pattern (saved mapping → heuristic → wizard); the window merges already-parsed entries by timestamp.
- **Soft timestamp enforcement**: sources whose pattern lacks a parseable timestamp load anyway with a visible "interleaving will be approximate" warning; their entries keep file order anchored to the last timestamped entry.
- **Union-of-columns display**: the table header is the union of all sources' columns (shared core fields first: Timestamp, Level, Message); missing fields render as blank cells.
- **Source identification**: a filterable/sortable `Source` column with short deduped display names (full path in tooltip), plus a per-source colour accent (left-edge stripe/badge) and a source-visibility affordance.
- **Single wizard with source selector**: when at least one source is unresolved the Pattern Wizard opens once, listing all window sources with per-source status (✓ saved / ⚠ needs review), each with its own sample lines, draft, and preview; `Edit Pattern Mapping...` reopens the same dialog.
- **Directory default + file override persistence**: directory mappings remain primary; an optional per-file (or filename-glob) override handles same-directory pattern conflicts; window preferences store a `sourceId → mapping reference` map so session restore is exact.
- **Timestamp-ordered insertion for live tail**: entries arriving out of order across tailed sources are inserted at their timestamp position (near-tail binary search), keeping the interleaved view truthful.

#### HITL Review Checkpoint
- Confirm mixed-pattern windows interleave correctly, the union column layout is readable, and the `Source` column + colour accents make row origin obvious.
- Confirm the multi-source wizard flow (source selector, per-source apply, missing-timestamp warning) feels manageable and per-file overrides persist/restore correctly.

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

### 4.6. Multi-Source Resolution and Merge
- Detection returns `sourceId → ProbeResult`; each source in a window gets its own compiled parser (structured JSON sources continue to route to `JsonLogParser` unchanged).
- `LogWindow` / `WindowPreference` carry a `sourcePatterns: Map<SourceId, SourcePatternRef>` (mapping reference or inline draft); legacy single `parserName`/`patternDraft` fields stay deserializable for backward compatibility.
- `UserPreferences` gains optional `filePatternOverrides` keyed by normalized file identity or glob, layered over `directoryPatternMappings`; lookup order per source: file override → directory mapping → heuristic.
- Parsed streams merge by timestamp; live-tail entries are inserted at their timestamp position via near-tail binary search; timestamp-less entries anchor to the previous timestamped entry in file order.
- Table columns are the union across all source results via the existing column-merge helper; a `Source` column and per-source colour accent identify origin.
- `PatternWizardState` gains per-source entries (status, sample lines, draft, preview) and an active-source selector; the missing-timestamp warning surfaces in `PatternMatchSummary`.

### 4.7. Structured Placeholder Handling
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
- **Professional interaction polish is in scope**: hover sync, undo/redo, keyboard model, resampling, escape hatches, mapping mismatch recovery, and dialog ergonomics are defined in `docs/PATTERN-WIZARD-UI-DESIGN.md` §6 and treated as sprint scope, not stretch goals.
- **Per-source pattern resolution**: multi-source windows resolve one pattern per source and merge parsed entries by timestamp; timestamp enforcement is soft (warn, load anyway) and live-tail arrivals are inserted timestamp-ordered.
- **Directory default + file override persistence**: directory mappings stay primary with optional per-file/glob overrides for same-directory conflicts; window preferences store per-source mapping references for exact session restore.

## 7. Definition of Done
- [ ] Opening an unrecognized or heuristically detected text log shows a best-guess pattern wizard before final mapping is committed.
- [ ] Editing the draft updates preview/mapping output immediately from sampled lines.
- [ ] Applying a pattern reloads the main log table through the existing loading path.
- [ ] Approved mappings are reused for later files in the same normalized directory scope.
- [ ] Structured JSON logs continue to auto-detect and render through the existing structured path.
- [ ] The wizard can be reopened later from the log UI.
- [ ] The wizard never blocks viewing a file: skip/plain-text is always available and high-confidence detection collapses to a confirmation banner.
- [ ] Hover synchronization, undo/redo, and keyboard shortcuts work across the wizard zones.
- [ ] A saved mapping that stops matching triggers wizard re-entry with diagnostics rather than silent mis-parsing.
- [ ] Saved directory mappings can be listed and deleted from a management surface.
- [ ] Deferred items are captured in `docs/deferred_decisions.md` with revisit triggers.
- [ ] Two files with different patterns load into one window, each parsed with its own pattern, interleaved by timestamp with union columns and a `Source` column + colour accent.
- [ ] Same-directory pattern conflicts are handled by file overrides that persist and restore correctly; sources without parseable timestamps show the approximate-interleaving warning.
- [ ] The multi-source wizard shows per-source status and applying updates only the selected source's mapping.
