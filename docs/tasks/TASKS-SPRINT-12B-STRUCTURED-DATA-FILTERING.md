# TASKS: Sprint 12B - Structured Field Filtering

## 12B. Structured Field Filtering

### 12B.0. Goal
Make structured logs operationally useful in the log list by adding path-aware, alias-aware filtering with predictable semantics for typed values and arrays.

### 12B.1. In Scope
- Structured filter syntax and grammar for canonical and explicit field predicates.
- Flattened path lookup integration for structured fields from 12A.
- Predicate support: exact, contains, regex, numeric comparisons, boolean/null, exists/missing.
- Alias-aware canonical filtering (for example `trace.id`, `level`, `message`).
- Array matching semantics for any-match and index-addressed paths.
- Escaping and quoting rules for dotted names and quoted values.
- Filter query/chip display updates if needed for clarity.
- Unit + integration tests for structured filter behavior and regressions.
- User-facing docs for new filter syntax.

### 12B.2. Out of Scope
- Structured detail-tree inspector interactions and copy/filter-from-node UX (12C).
- Dashboard/frequency redesign for structured fields (12E).
- Broad ecosystem normalization expansion beyond 12A baseline aliases (12D).
- Heavy performance optimization beyond preventing obvious filter regressions (12E).

### 12B.3. Dependencies
- Depends on `TASKS-SPRINT-12A-STRUCTURED-DATA-FOUNDATION.md`.
- Optional coupling with 12C for “Filter by this field/value” UX entry points.

### 12B.4. Scope-to-Workstream Mapping
- Grammar and parsing (`5`, `6`) -> `ui/viewmodel/LogFilterService.kt`, filter intent handling/parsing helpers.
- Structured lookup + alias resolution (`7`, `8`) -> flattened path index lookup adapters and canonical alias resolver.
- UI query presentation (`9`) -> filter bar/chip/query rendering state.
- Verification and regressions (`10`) -> filter unit tests + integration flows + plain-text regression checks.
- Documentation (`11`) -> user docs and sprint progress notes.

### 12B.5. Filter Syntax and Query Grammar
- [ ] 12B.5.1. Define canonical short forms (for example `level:error`, `has:trace.id`) and explicit predicates (`field:path op value`).
- [ ] 12B.5.2. Support operators for exact (`=`), contains, regex (`~`), numeric comparisons (`>`, `>=`, `<`, `<=`), exists, missing, and null checks.
- [ ] 12B.5.3. Define precedence/composition rules (AND/OR and grouping) consistent with existing query behavior.
- [ ] 12B.5.4. Preserve backward compatibility for existing free-text and `@field:key=value` queries via translation or dual parsing.

### 12B.6. Escaping, Quoting, and Typed Value Parsing
- [ ] 12B.6.1. Implement quoting rules for string literals with escaped quotes.
- [ ] 12B.6.2. Implement field-path escaping for literal dotted names (for example backtick path quoting).
- [ ] 12B.6.3. Parse unquoted numeric, boolean, and `null` tokens as typed comparisons where unambiguous.
- [ ] 12B.6.4. Define deterministic behavior when parsing is ambiguous (fallback to string comparison).

### 12B.7. Structured Path Lookup and Canonical Alias Resolution
- [ ] 12B.7.1. Resolve explicit `field:<path>` queries against the flattened path index produced in 12A.
- [ ] 12B.7.2. Resolve canonical terms to ecosystem aliases (for example `trace.id` -> `trace.id`, `traceId`, `TraceId`, `@tr`).
- [ ] 12B.7.3. Ensure canonical alias fan-out can match both canonical projection and raw-source keys.
- [ ] 12B.7.4. Keep raw exact-path queries precise when users explicitly target emitter-specific keys.

### 12B.8. Array Semantics and Missing-Field Behavior
- [ ] 12B.8.1. Implement `exists`/`missing` semantics for nested object and array paths.
- [ ] 12B.8.2. Implement any-match behavior for array predicate evaluation by default.
- [ ] 12B.8.3. Support deterministic index-addressed paths (for example `items[0].id`) where provided.
- [ ] 12B.8.4. Define null-vs-missing behavior explicitly and validate with tests.

### 12B.9. Filter Bar / Query Display Updates
- [ ] 12B.9.1. Update filter chip/query rendering so structured predicates are readable and reversible.
- [ ] 12B.9.2. Ensure parser/validator feedback is non-blocking for malformed user queries.
- [ ] 12B.9.3. Preserve existing keyboard and workflow behavior for legacy filter input patterns.

### 12B.10. Verification & Quality Gates
- [ ] 12B.10.1. Add/extend unit tests for grammar parsing, escaping/quoting, typed comparisons, and alias fan-out.
- [ ] 12B.10.2. Add integration tests validating structured query behavior in mixed structured/plain datasets.
- [ ] 12B.10.3. Add regression tests proving free-text and `@field:key=value` behavior remains stable.
- [ ] 12B.10.4. Run static analysis with `./gradlew detekt`.
- [ ] 12B.10.5. Run relevant module tests for touched modules.
- [ ] 12B.10.6. Run broader `./gradlew check` before closing 12B when feasible.

### 12B.11. Documentation and Rollout Notes
- [ ] 12B.11.1. Add user-facing filter syntax documentation with concrete examples:
  - `field:Properties.UserId="u-123"`
  - `field:StatusCode >= 500`
  - `field:TraceId exists`
  - `has:trace.id`
  - `message contains "timeout"`
- [ ] 12B.11.2. Update sprint epic progress markers for filtering slice completion.
- [ ] 12B.11.3. Document known limitations deferred to 12C/12D/12E.

### 12B.12. Acceptance Criteria
- [ ] Users can filter by flattened nested paths (for example `field:user.id=123`) with expected matches.
- [ ] Users can run exact, contains, regex, numeric, boolean/null, exists, and missing structured predicates.
- [ ] Filtering by canonical aliases (for example `trace.id`) matches ecosystem-specific fields (for example `TraceId`, `traceId`, `@tr`).
- [ ] Array predicates use documented any-match semantics and support index-addressed lookups.
- [ ] Invalid structured filter expressions produce safe feedback and do not break list rendering.
- [ ] Existing plain-text filtering behavior remains unchanged for non-structured logs.
