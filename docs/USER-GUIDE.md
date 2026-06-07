# KLogViewer User Guide

This guide explains how filtering works in KLogViewer and provides practical examples you can paste directly into the filter input.

## Filtering Basics

- The filter bar accepts text queries.
- Each submitted query becomes a chip.
- Multiple chips are combined with logical `AND` (all chips must match).
- You can mix plain-text and structured queries.
- The **Structured** filter UI in the filter bar is a helper that generates normal text queries; text remains the canonical format.

## Quick Start Examples

### Plain-text filters

These work exactly as before and are still fully supported:

- `timeout`
- `error`
- `2026-01-01`

Plain text matches against log content and timestamp text.

### Structured filters

- `level:error`
- `has:trace.id`
- `field:Properties.UserId="u-123"`
- `field:StatusCode >= 500`
- `field:TraceId exists`
- `field:error = null`
- `message contains "timeout"`
- `field:message ~ "timeout|deadline"`

## Structured Query Syntax

You can use either short canonical forms or explicit field predicates.

### Canonical short forms

- `level:error`
- `has:trace.id`

### Explicit field predicates

Supported forms include compact and spaced operators:

- `field:path=value`
- `field:path = value`
- `field:path contains value`
- `field:path ~ value`
- `field:path > value`
- `field:path >= value`
- `field:path < value`
- `field:path <= value`
- `field:path exists`
- `field:path missing`

Canonical-style predicates are also supported where appropriate:

- `message contains "timeout"`
- `level = "error"`
- `trace.id = "abc123"`

## Operators and Meaning

- `=`: exact match
- `contains`: substring match
- `~`: regex match
- `>` / `>=` / `<` / `<=`: numeric comparisons
- `exists`: path is present
- `missing`: path is not present
- `= null`: explicit null comparison

Invalid regex patterns are handled safely and do not crash filtering.

## Values and Types

Unquoted values are parsed as typed literals when unambiguous:

- Numbers: `123`, `-123`, `12.5`
- Booleans: `true`, `false`
- Null: `null`

Quoted values are always strings:

- `"123"`
- `"true"`
- `"null"`

Escaping in quoted strings is supported:

- `field:message contains "failed \"hard\""`
- `field:path = "C:\\logs\\app.log"`

## Canonical Aliases vs Explicit Raw Paths

Canonical queries can fan out to ecosystem aliases (for example different trace-id key spellings):

- `trace.id = "abc"` can match canonical and known alias fields.

Explicit `field:` queries stay path-precise:

- `field:TraceId="abc"` targets `TraceId`.
- `field:@tr="abc"` targets `@tr`.
- `field:traceId="abc"` targets `traceId`.

Use canonical forms when you want broader alias-aware matching; use `field:` when you want exact emitter-specific targeting.

## Escaped Field Paths

Use backticks to treat a segment as a literal key (including dots):

- Root literal key: ``field:`Properties.User.Id`="u-123"``
- Nested literal key: ``field:Properties.`User.Id`="u-123"``

To include a backtick inside a quoted segment, escape it by doubling it:

- ``field:`key``name`="value"``

Normal dotted paths still work unchanged:

- `field:user.id=123`
- `has:trace.id`

## Array Matching

### Default behavior: any-match

For non-indexed array paths, predicates match if **any** element matches:

- `field:items.id="a1"`
- `field:items.status contains "fail"`
- `field:items.durationMs > 100`

`missing` is deterministic for arrays:

- `field:items.id missing` is `true` only when no array element has `id`.

### Index-addressed paths

Use zero-based indexing when you need a specific element:

- `field:items[0].id="a1"`
- `field:items[1].durationMs > 100`
- `field:events[2].type exists`

Out-of-range indexes behave as missing/non-match:

- `exists` -> false
- `missing` -> true
- value predicates -> false

## Boolean Composition in a Single Query

Inside one query string, you can compose predicates with `AND` / `OR` and parentheses:

- `level:error OR level:warn`
- `(level:error OR level:warn) AND message contains "timeout"`

Precedence:

1. Parentheses
2. Predicate parsing
3. `AND`
4. `OR`

## Compatibility and Safety

- Existing plain-text filtering remains supported.
- Existing `@field:key=value` queries remain supported.
- Malformed structured queries are safe and non-blocking; they do not crash filtering.
- Structured UI-generated filters go through the same text-query pipeline as manual input.

## Using the Structured Filter UI

1. Click the structured filter trigger near the filter input.
2. Enter a field/path.
3. Choose an operator.
4. Enter a value (not required for `exists` / `missing`).
5. Click **Apply**.

The UI inserts a normal query expression into the same chip area used by manual typing.

## Current Limits

The current structured UI is intentionally minimal (walking skeleton). The following are planned for later work:

- richer inspector interactions (Sprint `12C`)
- broader ecosystem normalization (Sprint `12D`)
- performance/polish tuning (Sprint `12E`)
- autocomplete, history, presets, and advanced query-builder UX (Sprint `13`)