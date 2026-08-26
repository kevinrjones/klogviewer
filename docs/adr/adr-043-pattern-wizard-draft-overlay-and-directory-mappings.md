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
- Mapping keys will be normalized by directory identity for local, SFTP, and S3 sources via `DirectoryIdentityNormalizer`:
  - Local: `local:/absolute/dir/path` (e.g. `local:/var/log`)
  - SFTP: `sftp:username@host:port/directory/path` (e.g. `sftp:admin@10.0.0.1:22/var/log`)
  - S3: `s3:bucket/prefix` (e.g. `s3:prod-logs/2026/08`)
- The same directory identity rules are used for lookup, persistence, and reuse.
- Mismatch threshold: when a saved mapping's match confidence on newly opened sample lines falls below 80% (`0.80f`), the wizard automatically reopens with the saved pattern and diagnostics drawer visible rather than silently mis-parsing.

### 4. Per-Source Resolution and File Overrides (Multi-Source Windows)
- Each file/directory source in a window resolves its own pattern independently (file override → directory mapping → heuristic → wizard); the window merges already-parsed entries by timestamp.
- Timestamp enforcement is soft: a source whose pattern lacks a parseable timestamp still loads, with a visible "interleaving will be approximate" warning; its entries keep file order anchored to the last timestamped entry.
- Live-tail entries arriving out of order across sources are inserted at their timestamp position via near-tail binary search.
- Directory mappings remain the primary persistence unit; an optional per-file (or filename-glob) override in `UserPreferences.filePatternOverrides` handles same-directory pattern conflicts.
- Window preferences store a `sourceId → mapping reference` map (`sourcePatterns`) so session restore is exact; legacy single `parserName`/`patternDraft` fields remain deserializable.
- The table shows the union of all sources' columns (core fields first, blank cells for missing fields) plus a filterable `Source` column with a per-source colour accent.
- One Pattern Wizard instance serves all window sources via a source selector with per-source status, sample lines, draft, and preview.

### 5. Structured Placeholder Scope
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
- **Positive**: Multi-source windows can mix patterns truthfully interleaved by timestamp, with exact per-source restore.
- **Negative**: File overrides add a second persistence layer whose precedence over directory mappings must stay well-tested.