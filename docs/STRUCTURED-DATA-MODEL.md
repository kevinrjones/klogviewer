# Structured Data Model (Sprint 12A.5)

This document defines the backward-compatible structured payload contract introduced in Sprint `12A.5`.

## Scope

- Introduces a typed domain contract for structured values.
- Adds deterministic flattening into path-indexed values for filtering/detail features.
- Preserves compatibility for existing `LogEntry.fields: Map<String, String>` consumers.
- Preserves raw structured payload text for inspection/export.

Out of scope for this slice:

- parser-detection confidence hardening (`12A.6`)
- baseline alias normalization (`12A.7`)
- structured filtering grammar (`12B`)
- detail tree inspector UI (`12C`)

## Domain Contract

`domain/src/main/kotlin/com/klogviewer/domain/model/StructuredValue.kt`

- `StructuredValue.StringValue`
- `StructuredValue.NumberValue`
- `StructuredValue.BooleanValue`
- `StructuredValue.NullValue`
- `StructuredValue.ObjectValue`
- `StructuredValue.ArrayValue`

`NumberValue` stores number text to avoid precision loss from eager numeric conversion.

## Structured Container

`domain/src/main/kotlin/com/klogviewer/domain/model/StructuredLogData.kt`

- `root: StructuredValue.ObjectValue`
- `flatPathIndex: Map<String, List<StructuredValue>>`
- `rawPayload: String?`
- `canonicalFields: Map<String, StructuredValue>` (placeholder seam for later canonical rollout)

`LogEntry` now supports nullable `structuredData` while preserving existing constructor usage.

## Flattened Path Invariants

### Object paths

- Dot notation is used for object traversal.
- Example: `user.id`, `http.request.method`.

### Array paths

- Indexed element paths: `items[0].id`, `items[1].id`.
- Any-match aggregate path for element projections: `items[].id`.
- Scalar arrays also populate `items[]` with all scalar values.

### Null handling

- Null is preserved as typed `StructuredValue.NullValue`.
- Compatibility projection renders null deterministically as string `"null"`.

### Path escaping

- Object key path segments escape path syntax characters:
  - `\` -> `\\`
  - `.` -> `\.`
  - `[` -> `\[`
  - `]` -> `\]`

### Deterministic ordering

- Object keys are flattened in sorted-key order.
- Array values preserve source order.
- Compatibility map projection is key-sorted for deterministic tests.

### Empty containers

- Empty objects/arrays do not emit leaf path entries in `flatPathIndex`.
- Presence is preserved in `root` and (when available) `rawPayload`.

## Compatibility Projection Rules

- Compatibility projection is derived from `flatPathIndex` into `Map<String, String>`.
- Single-value path -> direct scalar display string.
- Multi-value path -> comma-joined deterministic display string.
- Collision precedence during merge with explicit `LogEntry.fields`:
  - Explicit `fields` are authoritative.
  - Structured projection fills only missing keys.

`LogEntry.compatibilityFields()` implements:

- `structuredData == null` -> existing `fields`
- `structuredData != null` -> `structuredProjection + explicitFields`

## Canonical vs Raw Precedence

For `12A.5`:

- Raw structured payload (`rawPayload`) is preserved unchanged.
- Canonical structured map exists as a seam (`canonicalFields`) but is not yet normalized in this slice.
- Current compatibility consumers continue to rely on explicit `fields` first.

Canonical alias normalization and canonical-vs-raw mapping expansion are deferred to `12A.7`.