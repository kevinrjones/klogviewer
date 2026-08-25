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