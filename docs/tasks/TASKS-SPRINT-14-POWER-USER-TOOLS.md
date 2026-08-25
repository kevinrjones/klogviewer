# TASKS: Sprint 14 - Power User Workflows, Query UX & Workspace Persistence

## 14. Sprint 14: Power User Workflows, Query UX & Workspace Persistence

### 14.0. Goal
Deliver power-user query workflows and persistence capabilities that build on Sprint 12B structured filtering semantics without introducing a second query language.

### 14.1. In Scope
- Query-builder UX that composes predicates using Sprint 12B grammar/semantics.
- Autocomplete for structured field paths and canonical aliases.
- Query history and saved query presets.
- Workspace/project `.lvp` persistence for power-user state.
- Context-menu productivity actions including filter injection from selected field/value.
- External-tool integration entry points from log/context actions.
- Regression checks proving Sprint 12B filtering semantics remain stable.

### 14.2. Out of Scope
- Defining a second SQL-like query grammar outside Sprint 12B.
- Redefining predicate semantics, typed comparisons, null/missing behavior, or array behavior owned by 12B.
- Broad structured-data normalization expansion (Sprint 12D).
- Heavy filtering performance tuning outside UX-critical needs (Sprint 12E).
- Deep parser/runtime semantics changes unrelated to power-user UX/workflow/persistence.

### 14.3. Dependencies
- Required: `TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md`.
- Required semantics from 12B: grammar, path-aware lookup, alias-aware canonical filtering, typed comparisons, array behavior, and backward compatibility.
- Recommended integration dependency: `TASKS-SPRINT-12C-STRUCTURED-DATA-INSPECTOR.md` for inspector/context-action entry points.

### 14.4. Scope-to-Workstream Mapping
- Query builder + composition UX (`14.5`) -> query bar/viewmodel intent flows and filter-state rendering.
- Structured autocomplete (`14.6`) -> alias/path suggestion providers and query-edit UX hints.
- Presets/history (`14.7`) -> preference/workspace state models and recall flows.
- Workspace `.lvp` persistence (`14.8`) -> workspace serialization/deserialization and recent-items integration.
- Context actions + integrations (`14.9`, `14.10`) -> log row context menu actions and external-action adapters.
- Verification/regressions (`14.11`) -> unit/integration/UI flows validating semantic stability and UX behavior.
- Documentation and rollout notes (`14.12`) -> sprint docs + user workflow docs + project memory updates.

### 14.5. Query Builder UX Over 12B Grammar
- [ ] 14.5.1. Build visual predicate composition controls that emit valid 12B-compatible filter expressions.
- [ ] 14.5.2. Support grouping/composition UX that preserves 12B operator precedence and semantics.
- [ ] 14.5.3. Ensure round-trip editing between visual builder state and text filter input.
- [ ] 14.5.4. Keep backward compatibility for existing free-text and `@field:key=value` workflows.

### 14.6. Autocomplete for Structured Paths and Canonical Aliases
- [ ] 14.6.1. Provide path suggestions from structured flattened path indexes where available.
- [ ] 14.6.2. Provide canonical alias suggestions (for example `trace.id`, `span.id`, `message`, `level`).
- [ ] 14.6.3. Provide operator/value hints aligned with 12B typed and exists/missing/null semantics.
- [ ] 14.6.4. Keep autocomplete responsive and non-blocking during large/mixed dataset sessions.

### 14.7. Query History and Saved Presets
- [ ] 14.7.1. Persist query history entries with deterministic ordering and recall behavior.
- [ ] 14.7.2. Add named saved query presets with create/edit/delete/apply workflows.
- [ ] 14.7.3. Support preset application to active log window context without losing current tab/workspace state.
- [ ] 14.7.4. Preserve compatibility for users who only use direct text filters.

### 14.8. Workspace `.lvp` Persistence
- [ ] 14.8.1. Persist and restore query-builder-visible state via `.lvp` workspace files.
- [ ] 14.8.2. Persist tabs, filters, query history references, preset references, and remote-source references.
- [ ] 14.8.3. Keep project portability with relative-path handling where feasible.
- [ ] 14.8.4. Ensure "Open Recent" behavior supports `.lvp` project/workspace workflows.

### 14.9. Context-Menu Productivity Actions
- [ ] 14.9.1. Add "Filter by this field" action that injects valid 12B-compatible predicates.
- [ ] 14.9.2. Add "Filter by this value" action with correct quoting/escaping behavior.
- [ ] 14.9.3. Keep context-injected filters editable in query text and reflected in builder state.
- [ ] 14.9.4. Preserve existing context-menu action behavior and enablement states.

### 14.10. External-Tool Integrations
- [ ] 14.10.1. Define external-tool action entry points from log/context menus.
- [ ] 14.10.2. Add safe validation/fallback handling when an external target is unavailable.
- [ ] 14.10.3. Keep integration actions non-blocking and workflow-friendly for investigations.

### 14.11. Verification & Quality Gates
- [ ] 14.11.1. Add/extend tests for query-builder composition and text round-trip behavior.
- [ ] 14.11.2. Add/extend tests for autocomplete path/alias suggestions and typed operator hints.
- [ ] 14.11.3. Add/extend tests for query history and saved preset persistence behavior.
- [ ] 14.11.4. Add/extend workspace persistence tests for `.lvp` power-user state restore.
- [ ] 14.11.5. Add/extend tests for context-menu filter injection correctness.
- [ ] 14.11.6. Add regression tests proving Sprint 12B filtering semantics remain unchanged.
- [ ] 14.11.7. Run static analysis with `./gradlew detekt`.
- [ ] 14.11.8. Run relevant module tests for touched modules.
- [ ] 14.11.9. Run broader `./gradlew check` before closing Sprint 14 when feasible.

### 14.12. Documentation Updates
- [ ] 14.12.1. Update user documentation for query-builder workflows on top of 12B syntax.
- [ ] 14.12.2. Document autocomplete behavior for field paths and canonical aliases.
- [ ] 14.12.3. Document query history/preset behavior and workspace persistence expectations.
- [ ] 14.12.4. Update sprint/project-memory tracking with shipped scope and known limitations.

### 14.13. Acceptance Criteria
- [ ] Query-builder workflows produce valid Sprint 12B-compatible filter expressions.
- [ ] Sprint 13 does not introduce a competing query grammar.
- [ ] Autocomplete supports structured field paths and canonical aliases from 12B semantics.
- [ ] Query history and named presets persist and restore across sessions.
- [ ] Workspace `.lvp` load/save restores expected power-user query/workspace state.
- [ ] Context-menu actions can inject field/value predicates with correct 12B behavior.
- [ ] External-tool actions are available at defined entry points with graceful fallback handling.
- [ ] Regression checks confirm existing filtering behavior remains stable.