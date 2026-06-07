# Structured Data Model (Sprint 12A Foundation)

This document defines the structured payload contract for Sprint `12A` (`12A.5` + `12A.7`) including baseline canonical normalization and backward-compatible projection rules.

## Scope

- Typed structured values for parser output.
- Deterministic path flattening for nested objects/arrays.
- Additive canonical projection for baseline fields.
- Raw payload and raw source-field preservation.
- Backward-compatible projection for existing string-field consumers.

Deferred to later sprint slices:

- Structured filtering grammar and advanced query language (`12B`).
- Structured detail-tree inspector UX and context actions (`12C`).
- Ecosystem-wide normalization pack beyond baseline aliases (`12D`).
- Performance/polish deep tuning and dashboard redesign (`12E`).

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

- `root: StructuredValue.ObjectValue` (raw typed tree)
- `flatPathIndex: Map<String, List<StructuredValue>>` (deterministic flattened index)
- `rawPayload: String?` (original record text)
- `canonicalFields: Map<String, StructuredValue>` (additive canonical projection)

`LogEntry` keeps legacy fields and now supports nullable `structuredData`.

## Flattened Path Invariants

### Object paths

- Dot notation is used for object traversal (`user.id`, `http.request.method`).

### Array paths

- Indexed element paths: `items[0].id`, `items[1].id`.
- Any-match aggregate path: `items[].id`.
- Scalar arrays also populate `items[]`.

### Null handling

- Null remains typed as `StructuredValue.NullValue`.
- Compatibility projection renders null as string `"null"`.

### Path escaping

- Object key path segments escape path syntax characters:
  - `\` -> `\\`
  - `.` -> `\.`
  - `[` -> `\[`
  - `]` -> `\]`

### Deterministic ordering

- Object keys flatten in sorted-key order.
- Array values preserve source order.
- Compatibility map output is key-sorted.

### Empty containers

- Empty objects/arrays do not emit leaf entries in `flatPathIndex`.
- Presence is still represented in `root`/`rawPayload`.

## Baseline Canonical Alias Mapping (12A.7)

Canonical projection keys and alias precedence:

| Canonical key | Alias precedence (left = highest) |
| --- | --- |
| `timestamp` | `timestamp`, `@timestamp`, `time`, `ts`, `@t`, `Timestamp` |
| `level` | `level`, `severity`, `lvl`, `@l`, `LogLevel`, `Level` |
| `message` | `message`, `msg`, `body`, `@m`, `Message`, `@mt` |
| `logger` | `logger`, `logger_name`, `SourceContext`, `Category`, `CategoryName` |
| `exception` | `exception`, `error`, `stackTrace`, `Exception`, `@x` |
| `trace.id` | `traceId`, `TraceId`, `@tr` |
| `span.id` | `spanId`, `SpanId`, `@sp` |

Canonical selection rules:

- Canonical projection is additive; source keys are never renamed/deleted.
- If canonical and alias keys both exist, canonical key value wins.
- If canonical key is absent, first non-null alias wins by listed order.
- Null alias values do not override an already available non-null candidate.
- Non-scalar values remain typed in canonical projection and use standard structured display conversion in compatibility projection.

## Rendered vs Template Message Rule

- Canonical `message` prefers rendered/user-facing fields (`message`, `msg`, `body`, `@m`, `Message`).
- `@mt` (message template) is retained as raw structured/flattened/compatibility data.
- If no rendered candidate exists, `@mt` is used as fallback canonical `message`.

## Canonical vs Raw Coexistence

- Raw namespaces (`Properties.*`, `attributes.*`, wrapper metadata, and unknown fields) are preserved in `root` and `flatPathIndex`.
- Baseline canonical projection only applies declared 12A aliases; unsupported ecosystem-specific fields remain raw.
- Compatibility projection combines flattened raw paths and canonical keys.

## Compatibility Projection Rules

- Structured projection is derived from `flatPathIndex` and `canonicalFields`.
- Single-value paths map to one display string.
- Multi-value paths map to comma-joined display strings.
- Canonical keys are additive and may overlap raw path names; canonical projection is merged deterministically.

`LogEntry.compatibilityFields()` behavior:

- `structuredData == null` -> existing `fields`
- `structuredData != null` -> `(structured projection incl. canonical) + explicit fields`
- Explicit `LogEntry.fields` remain authoritative on collisions for backward compatibility.