# ADR 043: Pattern Wizard Draft Overlay and Directory-Scoped Mappings

## Status
Proposed

## Context
KLogViewer can already auto-detect several log formats and parse structured JSON logs well, but plain-text pattern guessing is imperfect. Users need a UI-first way to inspect the system's best guess, edit it against sample lines, and save the approved mapping for reuse without storing metadata beside the log files. At the same time, existing structured JSON parsing and the current parser-loading flow must remain stable.

## Decision
We will implement Sprint 13 around a draft-overlay pattern wizard backed by a canonical persisted pattern model and directory-scoped preference storage.

### 1. Draft Overlay Workflow
- Heuristic detection produces a draft pattern and sample lines.
- The wizard edits that draft and shows a sample-based live preview.
- The active parser does not change until the user selects `Apply`.
- `Apply` compiles the draft into the existing runtime template/parser flow and triggers a normal reload.

### 2. Canonical Pattern Model
- We will persist one KLogViewer-owned pattern representation instead of raw framework-specific syntax.
- Importers translate common pasted formats, initially including Logback/Log4J-style layouts and Serilog-style text layouts where feasible.
- The canonical model carries enough metadata to support preview, persistence, and compilation back into the runtime template path.

### 3. Directory-Scoped Storage
- Approved mappings will be stored in user preferences, outside the source log directory.
- Mapping keys will be normalized by directory identity for local, SFTP, and S3 sources.
- The same directory identity rules must be used for lookup, persistence, and reuse.

### 4. Structured Placeholder Scope
- Structured JSON detection remains authoritative.
- Serilog `@mt` placeholders are visible in preview only during Sprint 13.
- Sprint 13 does not add a second-pass structured placeholder extraction pipeline.

## Consequences
- **Positive**: Users can correct the guessed pattern before the main table is committed to it.
- **Positive**: Persisted mappings are reusable without writing files into log directories.
- **Positive**: One canonical model reduces long-term coupling to a single external pattern syntax.
- **Positive**: Structured JSON behavior remains isolated from text-pattern editing risks.
- **Negative**: Preview and main-table output can temporarily differ until the user applies the draft.
- **Negative**: Importers add conversion complexity and will need coverage across common pasted formats.
- **Negative**: Shared project-file portability for mappings remains future work.