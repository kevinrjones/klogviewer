# Deferred Decisions

This file tracks design and implementation choices that were intentionally postponed so they can be revisited in a later sprint without losing context.

## Deferred Item Template
- **Title**:
- **Current choice**:
- **Why deferred**:
- **Impact / risk**:
- **Revisit trigger**:

## 1. Full Serilog Message-Template Extraction
- **Title**: Structured placeholder extraction from Serilog `@mt`
- **Current choice**: Sprint 13 shows placeholders like `{version}` in preview/mapping UI only.
- **Why deferred**: Adding a second semantic extraction pass would broaden Sprint 13 from a UI-first pattern workflow into a deeper structured-runtime parsing change.
- **Impact / risk**: Structured logs will not expose nested message-template placeholders as first-class parsed columns yet.
- **Revisit trigger**: Revisit when the preview-only UX proves insufficient or when a later sprint focuses on structured semantic enrichment.

## 2. Full Visual Pattern Builder
- **Title**: Advanced graphical pattern-builder workflow
- **Current choice**: Sprint 13 leads with an editable canonical text pattern plus live preview, with visual mapping kept lightweight.
- **Why deferred**: The first sprint needs a fast UI-first tracer bullet that supports paste/import workflows before investing in a richer builder surface.
- **Impact / risk**: Some users may still prefer a more guided no-syntax builder for complex patterns.
- **Revisit trigger**: Revisit after initial HITL feedback on editor usability and after paste/import coverage has been validated.

## 3. Continuous Main-Table Reparse While Editing
- **Title**: Parser-first always-live editing
- **Current choice**: Sprint 13 uses a draft overlay and only reloads the main table when the user applies the draft.
- **Why deferred**: Continuous reparsing would tightly couple the editor to the active load path and raises regression risk for structured and multiline behavior.
- **Impact / risk**: The preview and main table can differ temporarily until the user applies the draft.
- **Revisit trigger**: Revisit if users find the apply boundary confusing or if preview fidelity proves insufficient for real workflows.

## 4. Project-File Pattern Sharing
- **Title**: Shared project-scoped pattern mappings
- **Current choice**: Sprint 13 stores approved mappings in user settings by directory rather than in shareable project files.
- **Why deferred**: The immediate requirement is personal reuse without writing metadata into log directories, and project-file portability needs a separate design.
- **Impact / risk**: Mappings are convenient for one user on one machine but are not yet portable as explicit project artifacts.
- **Revisit trigger**: Revisit when project/workspace file persistence is extended to carry reusable mapping definitions.

## 5. User-Defined Preset Library
- **Title**: Saving custom patterns as named, reusable presets in the wizard preset dropdown
- **Current choice**: Sprint 13 presets are the built-in/detected templates only; reuse happens implicitly via directory-scoped mappings.
- **Why deferred**: A named preset library adds a second persistence and management concept on top of directory mappings before we know how users actually reuse patterns.
- **Impact / risk**: Users with many similar-but-differently-located logs must re-import or re-apply patterns per directory.
- **Revisit trigger**: Revisit if HITL feedback shows users repeatedly importing the same pattern across unrelated directories.

## 6. Drag-and-Drop Token Reordering
- **Title**: Full drag-and-drop pill reordering with drop indicators
- **Current choice**: Sprint 13 ships keyboard/button-based left-right reordering; drag-and-drop is a polish follow-up.
- **Why deferred**: Compose Desktop drag-and-drop within a flow-row needs careful gesture work and should not block the core wizard workflow.
- **Impact / risk**: Reordering is slightly less fluid than a best-in-class editor until added.
- **Revisit trigger**: Revisit during post-sprint polish once the token bar interaction model is validated by HITL review.

## 7. Raw Regex Power-User Mode
- **Title**: Direct raw-regex editing view alongside the token bar
- **Current choice**: Sprint 13 exposes the canonical token model and framework-pattern import only; the compiled regex is internal.
- **Why deferred**: Exposing raw regex creates a two-way sync problem between free-form regex and the canonical token model.
- **Impact / risk**: Power users with exotic formats may hit limits of the token model; custom-span extraction is the workaround.
- **Revisit trigger**: Revisit if HITL testing surfaces real log formats the token model plus span extraction cannot express.

## 8. Windowed Re-Sort Buffer for Live Tail
- **Title**: Buffered N-second re-sort batches for multi-source live tail ordering
- **Current choice**: Sprint 13 inserts arriving entries at their timestamp position via near-tail binary search instead of buffering and emitting sorted batches.
- **Why deferred**: A re-sort buffer adds display latency and tuning knobs while still leaving edge cases; direct timestamp-ordered insertion keeps the interleaved view truthful with a small implementation cost.
- **Impact / risk**: Extremely late arrivals far from the tail may cause an insertion deep in the list; if that proves expensive it can be revisited.
- **Revisit trigger**: Revisit if profiling shows near-tail insertion causing UI jank on high-volume multi-source tails.

## 9. Per-Source Column Sets
- **Title**: Table layouts that swap columns per row's source instead of union-of-columns
- **Current choice**: Sprint 13 shows the union of all sources' columns with blank cells where a source lacks a field, core fields first.
- **Why deferred**: Per-source column layouts are maximally faithful but visually chaotic and hard to scan/sort; union with blanks is predictable and reuses existing column machinery.
- **Impact / risk**: Windows mixing many disparate sources may show wide, sparse tables; users can hide columns via existing controls.
- **Revisit trigger**: Revisit if HITL feedback shows union tables become unusable for windows with many heterogeneous sources.

## 10. Colour-Only Source Identification
- **Title**: Identifying row origin with only a colour stripe/legend and no `Source` column
- **Current choice**: Sprint 13 ships a filterable `Source` column plus a per-source colour accent.
- **Why deferred**: Colour-only identification keeps the table clean but removes text filtering/sorting on origin, which existing table machinery gives for free with a column.
- **Impact / risk**: The extra column consumes horizontal space in single-source windows; it can be hidden there.
- **Revisit trigger**: Revisit if users ask for a denser layout once source filtering is available through other affordances.