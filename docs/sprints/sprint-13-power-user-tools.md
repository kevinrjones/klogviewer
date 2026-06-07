# Sprint 13: Power User Workflows, Query UX & Workspace Persistence

## 1. Goal
Deliver power-user workflows that make structured filtering faster to use and easier to persist, while keeping Sprint 12B as the semantic source of truth for query behavior.

## 2. Scope

### 2.1. Query Builder UX on Top of Sprint 12B
- Build a visual query-builder experience that composes predicates using the Sprint 12B structured filtering grammar and semantics.
- Ensure query-builder output round-trips to editable filter text without changing predicate meaning.
- Keep generated query behavior consistent with existing free-text and field-filter compatibility rules delivered in 12B.

### 2.2. Structured Autocomplete and Guidance
- Add autocomplete for structured field paths and canonical aliases exposed by Sprint 12B (for example `trace.id`, `http.request.path`).
- Provide operator/value hints aligned to 12B typed comparisons, null checks, exists/missing, and array semantics.

### 2.3. Query History and Saved Presets
- Persist query history across sessions with predictable ordering and recall behavior.
- Add saved query presets that users can name, reapply, and edit.

### 2.4. Workspace and Project Persistence
- Implement saving/loading of `.lvp` (KLogViewer Project) files for power-user workflow continuity.
- Persist tabs, filters, query history references, and remote connection references.
- Provide discoverable "Open Recent" access for workspace/project files.

### 2.5. Context Menu Productivity and External Tools
- Add context actions such as "Filter by this field/value" that inject valid 12B-compatible predicates.
- Add productivity actions (`Copy as JSON`, targeted web search, and related convenience actions) where appropriate.
- Support external-tool integration entry points (for example "Open in IDE at this line").

## 3. Dependencies and Ownership Boundaries
- Sprint 13 query-builder UX depends on `docs/tasks/TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md`.
- Sprint 12B owns filtering parser/query semantics (grammar, typed comparisons, null/missing/exists, array behavior, alias-aware canonical filtering, and backward compatibility).
- Sprint 13 must not duplicate parser or filtering semantics from 12B; it provides UX/workflow entry points, composition, persistence, and productivity features on top.

## 4. Out of Scope
- Creating a second or competing query language independent from Sprint 12B.
- Redefining structured predicate semantics already owned by Sprint 12B.
- Changing typed comparison behavior, null/missing behavior, or array predicate semantics defined in 12B.
- Broad structured-data normalization expansion (owned by Sprint 12D).
- Heavy filtering performance tuning unless directly required for Sprint 13 UX outcomes (otherwise owned by Sprint 12E).

## 5. Key Decisions
- **Semantic source of truth**: Sprint 12B structured filtering engine remains authoritative for query meaning and evaluation.
- **Sprint 13 focus**: Deliver visual composition, autocomplete, history, presets, workspace persistence, and context-driven entry points.
- **Project portability**: Use relative paths in `.lvp` files where possible to support sharing projects across machines.

## 6. Definition of Done
- [ ] Query-builder UX composes and edits valid Sprint 12B-compatible filters without semantic drift.
- [ ] Structured autocomplete supports field paths and canonical aliases aligned with Sprint 12B.
- [ ] Query history and saved presets are persisted and restorable across sessions.
- [ ] Workspace `.lvp` persistence restores tabs, filters, and relevant power-user state.
- [ ] Context-menu actions (including filter-by-field/value) produce valid Sprint 12B-compatible queries.
- [ ] External-tool integration actions are available at defined context entry points.
