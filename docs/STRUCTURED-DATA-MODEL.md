# Structured Data Model (Sprints 12A + 12D)

This document defines the structured payload contract introduced in Sprint `12A` (`12A.5` + `12A.7`) and extended in Sprint `12D` for ecosystem compatibility normalization.

## Scope

- Typed structured values for parser output.
- Deterministic path flattening for nested objects/arrays.
- Additive canonical projection for baseline fields.
- Raw payload and raw source-field preservation.
- Backward-compatible projection for existing string-field consumers.

Deferred to later sprint slices:

- Structured filtering grammar and advanced query language (`12B`).
- Structured detail-tree inspector UX and context actions (`12C`).
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

## Canonical Alias Mapping (12A + 12D)

Canonical projection keys and alias precedence:

| Canonical key | Alias precedence (left = highest) |
| --- | --- |
| `timestamp` | `timestamp`, `@timestamp`, `time`, `ts`, `@t`, `Timestamp`, `timeMillis`, `timeUnixNano` |
| `level` | `level`, `severity`, `lvl`, `@l`, `LogLevel`, `Level`, `severityText` |
| `message` | `message`, `msg`, `body`, `@m`, `RenderedMessage`, `Message`, `@mt`, `MessageTemplate`, `OriginalFormat` |
| `message.template` | `@mt`, `MessageTemplate`, `OriginalFormat` |
| `logger` | `logger`, `logger_name`, `loggerName`, `SourceContext`, `Category`, `CategoryName`, `LoggerName` |
| `exception` | `exception`, `error`, `stackTrace`, `Exception`, `@x`, `thrown` |
| `trace.id` | `trace.id`, `traceId`, `TraceId`, `@tr`, `trace_id` |
| `span.id` | `span.id`, `spanId`, `SpanId`, `@sp`, `span_id` |
| `correlation.id` | `correlation.id`, `correlationId`, `CorrelationId`, `RequestId`, `requestId` |

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
- Canonical projection is additive and never deletes raw source fields.

## 12D Ecosystem Support Matrix

| Ecosystem | Representative formats | Expected canonical extraction | Preserved raw namespaces |
| --- | --- | --- | --- |
| JVM | LogStash Logback JSON, Logback + MDC, Spring Boot JSON, Log4j2 `JSONLayout`/`JsonTemplateLayout` | `timestamp`, `level`, `message`, `logger`, `exception`, `trace.id`, `span.id` | `mdc.*`, `MDC.*`, nested payload blocks (`payload.*`, `context.*`) |
| .NET | MEL JSON console, Serilog compact/rendered compact/standard, Serilog ASP.NET request logs, NLog JSON, log4net JSON-style | `timestamp`, `level`, `message`, `message.template`, `logger`, `exception`, `trace.id`, `span.id`, `correlation.id` | `EventId.*`, `Scopes[]`, `Properties.*`, request metadata (`RequestPath`, `RequestMethod`, `StatusCode`, `Elapsed`) |
| Containers | Docker JSON wrappers, Kubernetes/CRI wrappers | Canonical fields extracted from nested app payload where available | Envelope metadata (`stream`, wrapper `time`, `kubernetes.*`) + raw nested `log` |
| Cloud envelopes | Provider envelope with nested app event (for example `jsonPayload`) | Canonical fields sourced from nested application event if root aliases are absent | Provider metadata (`insertId`, `resource.*`, provider severity) |
| OTel-like JSON | `timeUnixNano`, `severityText`, `body`, `resource.*`, `attributes.*` | `timestamp`, `level`, `message` from OTel-like fields | `resource.*`, `attributes.*` preserved as raw/filterable paths |

## Nested Wrapper Decoding

- For wrapper/envelope formats, nested JSON payloads are decoded additively under `_decoded.*` for filterability.
- Canonical projection can source values from decoded nested scopes when root aliases are absent.
- Raw wrapper fields (including original string payloads) remain unchanged.

## Fixture Catalog (12D)

Primary fixture constants live in:

- `core/src/test/kotlin/com/klogviewer/core/parser/StructuredEcosystemFixtures.kt`

Referenced fixture groups:

- JVM: `LOGSTASH_LOGBACK_JSON`, `LOGBACK_JSON_WITH_MDC`, `SPRING_BOOT_STRUCTURED_JSON`, `LOG4J2_JSON_LAYOUT`, `LOG4J2_JSON_TEMPLATE_LAYOUT`
- .NET: `MEL_JSON_CONSOLE`, `SERILOG_COMPACT_JSON`, `SERILOG_RENDERED_COMPACT_JSON`, `SERILOG_STANDARD_JSON`, `SERILOG_ASPNET_REQUEST_JSON`, `NLOG_JSON_LAYOUT`, `LOG4NET_JSON_STYLE`
- Container/cloud/OTel: `DOCKER_JSON_WRAPPER`, `KUBERNETES_CRI_WRAPPER`, `CLOUD_PROVIDER_ENVELOPE`, `OTEL_LIKE_JSON`

## Known Limitations and Partial Support

- Unsupported variants are ingested as raw structured fields with existing fallback behavior (no destructive transformations).
- Deep ecosystem-specific semantics (for example, provider-specific severity translation rules) are not normalized beyond alias-driven canonical extraction.
- Some alias-aware filtering behavior is validated primarily through canonical and preserved raw paths; unsupported short forms continue to use existing grammar fallbacks.
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

## Sprint 12B Structured Filter Syntax

Structured filtering is text-first: the parser consumes query text, including text emitted by the minimal structured
filter UI in `FilterBar`.

### Core examples

- `field:Properties.UserId="u-123"`
- `field:StatusCode >= 500`
- `field:TraceId exists`
- `has:trace.id`
- `message contains "timeout"`

### Predicate forms and operators

- Exact: `=`
  - `field:service = "auth"`
- Contains: `contains`
  - `field:message contains "timeout"`
- Regex: `~`
  - `field:message ~ "timeout|deadline"`
- Numeric comparisons: `>`, `>=`, `<`, `<=`
  - `field:durationMs > 250`
- Boolean values:
  - `field:isRetry = true`
- Null checks:
  - `field:error = null`
  - `field:error is null`
- Existence:
  - `field:TraceId exists`
  - `has:trace.id`
- Missing:
  - `field:TraceId missing`

### Canonical aliases vs explicit field paths

- Canonical forms (for example `trace.id = "abc"`, `level:error`) may fan out through alias mapping.
- Explicit `field:<path>` predicates are path-precise and do not fan out to alias siblings.
- Existing compatibility query form `@field:key=value` remains supported.
- Existing plain-text filtering remains supported and still matches log content/timestamp.

### Escaped path segments

Use backticks for literal segment names that contain path-significant characters such as dots:

- Root literal dotted key:
  - ``field:`Properties.User.Id`="u-123"``
- Nested literal dotted key:
  - ``field:Properties.`User.Id`="u-123"``
- Backtick in segment name is escaped by doubling:
  - ``field:`User``Id`="u-123"``

Malformed escaped paths are non-blocking: the query safely falls back to legacy text behavior.

### Array semantics

- Default (non-indexed) path semantics are any-match across array elements.
  - `field:items.id="a1"` matches when any `items[]` element has `id == "a1"`.
- `exists` on an array path is true when at least one value exists for that path.
- `missing` on an array path is true only when no values exist at that path.

### Indexed array paths

Indexed paths are zero-based and target exactly one element:

- `field:items[0].id="a1"`
- `field:items[1].durationMs > 100`
- `field:events[2].type exists`

Out-of-range indexed paths behave as missing/non-match:

- `exists` => `false`
- `missing` => `true`
- value predicates => `false`

Indexed paths compose with escaped segments:

- ``field:`items.with.dot`[0].id="a1"``
- ``field:items[0].`id.with.dot`="a1"``

### Deferred limitations and follow-up slices

- `12C`: richer structured inspector interactions (`filter by this field/value` from detail tree).
- `12D`: broader ecosystem normalization beyond baseline alias pack.
- `12E`: performance/polish and dashboard redesign work.
- Sprint `13`: autocomplete, query history, presets, and fuller query-builder UX.