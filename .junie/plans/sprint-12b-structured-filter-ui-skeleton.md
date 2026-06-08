---
sessionId: session-260607-095219-12l0
---

# Requirements

### Overview & Goals
Add a **walking-skeleton structured field filter UI** to Sprint 12B so users can build one structured predicate without typing full syntax, while keeping text query input as the canonical source of truth.

### In Scope
- Add a small structured-filter entry affordance near the existing filter input in `FilterBar`.
- Open a compact panel/popover/dialog with:
  - field/path text input,
  - operator selector (`=`, `contains`, `~`, `>`, `>=`, `<`, `<=`, `exists`, `missing`),
  - value input shown only when required,
  - `Apply` + `Cancel` actions.
- `Apply` generates a **text query expression** compatible with Sprint 12B grammar and submits via the existing filter query path.
- Generated query is rendered in the same filter chip/query area used by manual text input.
- Lightweight UI validation:
  - disable `Apply` for blank field/path,
  - disable `Apply` when value is required but blank,
  - keep parser/validator feedback non-blocking and shared with manual queries.
- Update Sprint 12B task/sprint docs to explicitly include this walking skeleton and its acceptance criteria.

### Out of Scope (Deferred UI Work)
- Field autocomplete
- Schema/field discovery UI
- Detail-tree click-to-filter actions
- Saved filters/query history/presets
- Visual boolean/group builder
- Full query-builder UX
- Dashboard query-builder integration

### Acceptance Criteria Additions
- Users can add a basic structured field predicate through a minimal UI without manually typing full expression.
- UI-generated predicates flow through the same grammar + filtering pipeline as manual text input.
- Text input remains fully supported and canonical.
- Structured UI remains intentionally minimal and does not replace power-user text workflows.

# Technical Design

### Current Implementation (from codebase)
- `ui/src/main/kotlin/com/klogviewer/ui/components/FilterBar.kt`
  - Current filter UX is a single `BasicTextField` (`testTag = "filter_input"`) and chips rendered from `filterQueries`.
  - Submitting text calls `onAddQuery`, clearing uses `onClearQueries`, and chips remove via `onRemoveQuery`.
- `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt`
  - `LogTopBar` wires `FilterBar` callbacks to `KLogViewerIntent.AddFilterQuery`, `RemoveFilterQuery`, and `ClearFilterQueries`.
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/FilterIntentHandler.kt`
  - `AddFilterQuery` appends non-blank unique queries into active window state and triggers filtering.
- `ui/src/main/kotlin/com/klogviewer/ui/viewmodel/LogFilterService.kt`
  - Filtering consumes `window.filterQueries` as the canonical query list.
- Existing UI tests:
  - `ui/src/test/kotlin/com/klogviewer/ui/components/FilterBarTimeFilterControlsTest.kt`
  - `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerUiTest.kt`

### Key Decisions
1. **Single canonical representation**: structured UI emits plain text query expressions only; no secondary AST/view-model query format.
2. **Reuse existing filter pipeline**: structured `Apply` invokes existing `onAddQuery` flow (`AddFilterQuery` intent path).
3. **Localized UI state**: panel open/close + temporary field/operator/value state stays in `FilterBar` (`remember` state), matching existing toolbar menu patterns.
4. **Minimal operator model**: use an internal operator mapping (label/token/requiresValue) to drive UI and string generation deterministically.
5. **Validation parity**: malformed generated expressions use the same non-blocking parser/validator feedback channel as manual input (no separate error model).

### Proposed Changes
- **`FilterBar.kt`**
  - Add a compact structured-filter trigger button near the query input (with stable `testTag`).
  - Add a compact structured filter panel/popover/dialog with:
    - field/path input (`Properties.UserId`, `trace.id`, `StatusCode`, `items[0].id` placeholders),
    - operator selector,
    - value input conditionally visible/enabled,
    - `Apply` / `Cancel` actions.
  - Add a small query-construction helper that builds Sprint 12B-compatible text expressions.
  - Keep `Apply` disabled unless required inputs are valid.
  - On `Apply`: call `onAddQuery(generatedQuery)`, then close/reset panel.

- **Query generation contract (UI side)**
  - Prefer explicit structured predicate form (`field:<path> <operator> <value>` and `field:<path> exists|missing`) so output stays parser-oriented.
  - Quote/escape string literals; pass through obvious typed tokens where grammar supports them.
  - Keep canonical alias shortcuts (`has:trace.id`) optional and parser-driven, not a separate UI-only branch.

- **`KLogViewerScreen.kt` / ViewModel wiring**
  - Continue using existing `AddFilterQuery` intent pipeline.
  - If Sprint 12B parser validation state is surfaced in `LogWindow`, pass it into `FilterBar` using the existing warning/tooltip presentation style already used for time-filter validation.

- **Sprint documentation updates**
  - Update `docs/tasks/TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md` to add explicit walking-skeleton structured UI scope, deferred UI work list, and acceptance criteria additions.
  - Update `docs/sprints/sprint-12-structured-data.md` implementation approach to note this minimal structured entry UX in 12B while deferring full query-builder UX to Sprint 13.

### File Structure (planned touches)
- `docs/tasks/TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md`
- `docs/sprints/sprint-12-structured-data.md`
- `ui/src/main/kotlin/com/klogviewer/ui/components/FilterBar.kt`
- `ui/src/main/kotlin/com/klogviewer/ui/components/KLogViewerScreen.kt` (if additional props are required)
- `ui/src/test/kotlin/com/klogviewer/ui/components/FilterBarTimeFilterControlsTest.kt` and/or new `FilterBarStructuredFilterTest.kt`
- `ui/src/test/kotlin/com/klogviewer/ui/test/KLogViewerUiTest.kt`

# Testing

### Validation Approach
- Use Compose UI tests for structured panel behavior and input validation states.
- Use existing UI integration tests to confirm generated query entry and manual text-filter regression behavior.
- Ensure checks remain non-blocking for invalid/malformed query text.

### Key Scenarios
- Structured filter trigger is visible near `filter_input`.
- Clicking trigger opens panel/dialog.
- Field + operator + value + `Apply` adds a filter query chip/display entry.
- `exists` and `missing` do not require value input.
- `Apply` is disabled for missing required field/value.
- `Cancel` dismisses without adding query.
- Existing manual text filter flow (`typeFilter`, chip rendering, clear behavior) remains unchanged.

### Edge Cases
- Blank field/path never applies.
- Value with quotes/special characters is escaped in generated query string.
- Invalid generated expression (when parser rejects) surfaces existing non-blocking validator feedback and does not crash/filter bar UI.
- Mixed manual + structured queries remain removable/clearable through the same chip controls.

### Test Changes
- Extend/add `FilterBar` component tests for panel open/close, operator/value visibility, and `Apply` enablement.
- Extend/add `KLogViewerUiTest` scenario asserting structured-generated query appears in chip/query area and coexists with manual query behavior.

# Delivery Steps

### ✓ Step 1: Update Sprint 12B planning artifacts to include the walking skeleton UI scope
Sprint 12B docs explicitly define the minimal structured filter UI as part of this sprint, with clear deferrals.
- Update `docs/tasks/TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md` scope/workstream/acceptance sections to include the structured filter UI skeleton.
- Add a short **Deferred UI Work** list (autocomplete, discovery, node-based filtering, presets/history, visual boolean builder).
- Update `docs/sprints/sprint-12-structured-data.md` to reflect that 12B includes discoverable minimal structured entry while full builder UX remains in Sprint 13.
- Keep wording explicit that text grammar remains canonical and generated UI predicates are grammar-compatible text.

### ✓ Step 2: Implement the structured filter entry skeleton in the filter bar using the existing query pipeline
Users can open a compact structured panel, build one predicate, and inject it into the existing filter query list.
- Add a small structured trigger control in `FilterBar.kt` adjacent to current query input/chips.
- Implement panel state and controls (field/path input, operator selector, conditional value input, Apply/Cancel).
- Add deterministic operator mapping and text-expression builder for Sprint 12B-compatible output.
- Enforce lightweight UI validation by disabling `Apply` when required inputs are incomplete.
- Reuse existing `onAddQuery` callback so generated expressions follow the same `AddFilterQuery` → `FilterIntentHandler` → `LogFilterService` flow.

### ✓ Step 3: Integrate validation parity and extend UI coverage for structured and manual query flows
Structured and manual query entry paths are both covered by tests and share the same non-blocking feedback behavior.
- Surface/propagate shared filter validation messaging to `FilterBar` (if 12B parser validation state is present), using existing warning/tooltip patterns.
- Add/extend Compose tests around `FilterBar` for panel visibility, operator-specific value handling, and apply/cancel behavior.
- Add/extend UI integration tests in `KLogViewerUiTest.kt` to verify generated query chip/display behavior and unchanged manual text filtering.
- Cover failure-safe behavior: incomplete inputs do not apply queries, and invalid generated queries do not crash or break free-text filtering.

### ✓ Step 4: Implement 12B.5 query AST and grammar parser with safe fallback behavior
Add deterministic parsing for canonical short forms and explicit/canonical field predicates while preserving legacy free-text behavior.
- Introduce a small internal query-expression model (text, field predicate, and boolean expression extension point) in the filtering layer.
- Implement parsing for `level:error`, `has:path`, `field:path op value`, and canonical `path op value` forms with typed literal parsing.
- Support escaped quoted strings, numeric/boolean/null literal coercion for unquoted tokens, and safe fallback to legacy text queries for malformed/unsupported structured syntax.
- Keep parser extensible for AND/OR/parentheses precedence even if chip-level implicit AND remains the active composition mechanism.

### ✓ Step 5: Integrate structured predicate evaluation into LogFilterService without regressing existing filters
Route each filter chip query through parser + evaluator so structured predicates and legacy text queries share one safe pipeline.
- Evaluate operators (`=`, `contains`, `~`, `>`, `>=`, `<`, `<=`, `exists`, `missing`, `= null`) against structured paths/compatibility fields.
- Preserve existing plain-text and `@field:key=value` behavior (directly or via translation) with non-crashing malformed-query handling.
- Add safe regex handling for invalid patterns and document null-vs-missing limitation if the current model cannot fully distinguish it.

### ✓ Step 6: Add/extend tests and run quality gates for 12B.5 grammar + compatibility
Verify parser/evaluator behavior and backward compatibility, then run required checks.
- Add unit tests for syntax parsing, typed literals, escaping, operator behavior, and safe fallbacks.
- Add backward-compatibility tests for plain text, timestamp matching, multi-chip AND behavior, and `@field:key=value` support.
- Run targeted UI/domain tests impacted by filtering changes, then run `./gradlew detekt` and broader `./gradlew check` if feasible.

### ✓ Step 7: Complete remaining 12B parser/evaluator behavior (escaping, raw precision, arrays, indexing)
Implement the remaining unchecked filtering semantics without regressing existing behavior.
- Add field-path escaping with backtick-quoted literal segments and safe malformed-path fallback.
- Keep explicit `field:` predicates raw-path precise while preserving canonical alias fan-out for non-explicit canonical forms.
- Implement array any-match semantics for predicates and deterministic indexed path behavior (`items[0].id` style).
- Add/extend unit tests for escaping, raw precision, any-match, indexed paths, invalid index/path safety, and regressions.

### ✓ Step 8: Update Sprint 12B documentation and progress markers
Document user-facing syntax/rollout notes and update task acceptance checkboxes for completed items.
- Add concrete syntax examples and behavior notes (operators, aliases, escaped paths, arrays, indexed paths, fallback safety).
- Document deferred limitations for 12C/12D/12E and compatibility with plain-text / `@field:key=value` filtering.
- Mark completed task entries in `TASKS-SPRINT-12B-STRUCTURED-DATA-FILTERING.md` after verification evidence exists.

### ✓ Step 9: Run final quality gates and close Sprint 12B remaining work
Run required commands and finalize quality-gate-related checklist items based on actual outcomes.
- Run targeted tests for touched modules.
- Run `./gradlew detekt` and, when feasible, `./gradlew check`.
- Update task checklist statuses for 12B.10.4 and 12B.10.6 strictly according to command results.

### ✓ Step 10: Extract parser internals into dedicated classes without changing behavior
Refactor `LogQueryParser` internals into smaller focused parser collaborators while preserving all accepted grammar forms and fallback behavior.
- Keep public parser entrypoint contract unchanged for callers/tests.
- Extract tokenization/boolean-expression parsing/predicate parsing/literal parsing concerns into dedicated classes or helpers.
- Maintain deterministic parsing and safe fallback to legacy text query for malformed/unsupported structured syntax.

### ✓ Step 11: Extract evaluator operator strategies and alias resolver dependency
Refactor `LogFilterService` to isolate operator evaluation and alias resolution concerns into focused collaborators.
- Move field-operator evaluation logic behind explicit strategy helpers.
- Introduce alias resolver dependency instead of hardcoded alias map placement in evaluator flow.
- Preserve explicit `field:` path precision and canonical alias fan-out behavior.

### ✓ Step 12: Decompose FilterBar into focused composables and isolate structured draft state
Reduce `FilterBar.kt` complexity by splitting UI responsibilities into smaller composables and explicit structured-filter draft state.
- Extract clear UI sections (query input/chips/actions/dialog content).
- Keep test tags and user-visible behavior stable.
- Preserve structured filter apply/cancel/validation semantics.

### ✓ Step 13: Split PreferencesStateMapper.toState into policy and mapping helpers
Refactor `PreferencesStateMapper` to separate policy/default reconciliation from state shape mapping.
- Keep state output and existing behavior unchanged.
- Use small pure helpers to improve readability and testability.
- Add or update tests for touched behavior where coverage is missing.