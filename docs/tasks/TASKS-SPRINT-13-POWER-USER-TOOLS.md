# TASKS: Sprint 13 - Power User Workflows, Query UX & Workspace Persistence

## 13. Sprint 13: Power User Workflows, Query UX & Workspace Persistence

### 13.0. Goal
Deliver power-user query workflows and persistence capabilities that build on Sprint 12B structured filtering semantics without introducing a second query language.

### 13.1. In Scope
- Query-builder UX that composes predicates using Sprint 12B grammar/semantics.
- Autocomplete for structured field paths and canonical aliases.
- Query history and saved query presets.
- Workspace/project `.lvp` persistence for power-user state.
- Context-menu productivity actions including filter injection from selected field/value.
- External-tool integration entry points from log/context actions.
- Regression checks proving Sprint 12B filtering semantics remain stable.

### 13.2. Out of Scope
- Defining a second SQL-like query grammar outside Sprint 12B.
- Redefining predicate semantics, typed comparisons, null/missing behavior, or array behavior owned by 12B.
- Broad structured-data normalization expansion (Sprint 12D).
- Heavy filtering performance tuning outside UX-critical needs (Sprint 12E).
- Deep parser/runtime semantics changes unrelated to power-user UX/workflow/persistence.

### 13.3. Dependencies
- Required: `TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md`.
- Required semantics from 12B: grammar, path-aware lookup, alias-aware canonical filtering, typed comparisons, array behavior, and backward compatibility.
- Recommended integration dependency: `TASKS-SPRINT-12C-STRUCTURED-DATA-INSPECTOR.md` for inspector/context-action entry points.

### 13.4. Scope-to-Workstream Mapping
- Query builder + composition UX (`13.5`) -> query bar/viewmodel intent flows and filter-state rendering.
- Structured autocomplete (`13.6`) -> alias/path suggestion providers and query-edit UX hints.
- Presets/history (`13.7`) -> preference/workspace state models and recall flows.
- Workspace `.lvp` persistence (`13.8`) -> workspace serialization/deserialization and recent-items integration.
- Context actions + integrations (`13.9`, `13.10`) -> log row context menu actions and external-action adapters.
- Verification/regressions (`13.11`) -> unit/integration/UI flows validating semantic stability and UX behavior.
- Documentation and rollout notes (`13.12`) -> sprint docs + user workflow docs + project memory updates.

### 13.5. Query Builder UX Over 12B Grammar
- [ ] 13.5.1. Build visual predicate composition controls that emit valid 12B-compatible filter expressions.
- [ ] 13.5.2. Support grouping/composition UX that preserves 12B operator precedence and semantics.
- [ ] 13.5.3. Ensure round-trip editing between visual builder state and text filter input.
- [ ] 13.5.4. Keep backward compatibility for existing free-text and `@field:key=value` workflows.

### 13.6. Autocomplete for Structured Paths and Canonical Aliases
- [ ] 13.6.1. Provide path suggestions from structured flattened path indexes where available.
- [ ] 13.6.2. Provide canonical alias suggestions (for example `trace.id`, `span.id`, `message`, `level`).
- [ ] 13.6.3. Provide operator/value hints aligned with 12B typed and exists/missing/null semantics.
- [ ] 13.6.4. Keep autocomplete responsive and non-blocking during large/mixed dataset sessions.

### 13.7. Query History and Saved Presets
- [ ] 13.7.1. Persist query history entries with deterministic ordering and recall behavior.
- [ ] 13.7.2. Add named saved query presets with create/edit/delete/apply workflows.
- [ ] 13.7.3. Support preset application to active log window context without losing current tab/workspace state.
- [ ] 13.7.4. Preserve compatibility for users who only use direct text filters.

### 13.8. Workspace `.lvp` Persistence
- [ ] 13.8.1. Persist and restore query-builder-visible state via `.lvp` workspace files.
- [ ] 13.8.2. Persist tabs, filters, query history references, preset references, and remote-source references.
- [ ] 13.8.3. Keep project portability with relative-path handling where feasible.
- [ ] 13.8.4. Ensure "Open Recent" behavior supports `.lvp` project/workspace workflows.

### 13.9. Context-Menu Productivity Actions
- [ ] 13.9.1. Add "Filter by this field" action that injects valid 12B-compatible predicates.
- [ ] 13.9.2. Add "Filter by this value" action with correct quoting/escaping behavior.
- [ ] 13.9.3. Keep context-injected filters editable in query text and reflected in builder state.
- [ ] 13.9.4. Preserve existing context-menu action behavior and enablement states.

### 13.10. External-Tool Integrations
- [ ] 13.10.1. Define external-tool action entry points from log/context menus.
- [ ] 13.10.2. Add safe validation/fallback handling when an external target is unavailable.
- [ ] 13.10.3. Keep integration actions non-blocking and workflow-friendly for investigations.

### 13.11. Verification & Quality Gates
- [ ] 13.11.1. Add/extend tests for query-builder composition and text round-trip behavior.
- [ ] 13.11.2. Add/extend tests for autocomplete path/alias suggestions and typed operator hints.
- [ ] 13.11.3. Add/extend tests for query history and saved preset persistence behavior.
- [ ] 13.11.4. Add/extend workspace persistence tests for `.lvp` power-user state restore.
- [ ] 13.11.5. Add/extend tests for context-menu filter injection correctness.
- [ ] 13.11.6. Add regression tests proving Sprint 12B filtering semantics remain unchanged.
- [ ] 13.11.7. Run static analysis with `./gradlew detekt`.
- [ ] 13.11.8. Run relevant module tests for touched modules.
- [ ] 13.11.9. Run broader `./gradlew check` before closing Sprint 13 when feasible.

### 13.12. Documentation Updates
- [ ] 13.12.1. Update user documentation for query-builder workflows on top of 12B syntax.
- [ ] 13.12.2. Document autocomplete behavior for field paths and canonical aliases.
- [ ] 13.12.3. Document query history/preset behavior and workspace persistence expectations.
- [ ] 13.12.4. Update sprint/project-memory tracking with shipped scope and known limitations.

### 13.13. Acceptance Criteria
- [ ] Query-builder workflows produce valid Sprint 12B-compatible filter expressions.
- [ ] Sprint 13 does not introduce a competing query grammar.
- [ ] Autocomplete supports structured field paths and canonical aliases from 12B semantics.
- [ ] Query history and named presets persist and restore across sessions.
- [ ] Workspace `.lvp` load/save restores expected power-user query/workspace state.
- [ ] Context-menu actions can inject field/value predicates with correct 12B behavior.
- [ ] External-tool actions are available at defined entry points with graceful fallback handling.
- [ ] Regression checks confirm existing filtering behavior remains stable.