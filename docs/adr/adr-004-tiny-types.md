# ADR-004: Type Safety with Tiny Types

## Status
Accepted

## Context
Primitive types like `String` and `Int` are often used to represent domain-specific concepts (e.g., file paths, log levels). This can lead to "primitive obsession," where different concepts are accidentally mixed up, and validation logic is scattered.

## Decision
We will use Tiny Types (also known as Value Objects or Inline Classes in Kotlin) to represent core domain concepts.
- `LogFilePath` (String)
- `LogLevel` (Enum/Sealed)
- `LogContent` (String)
- `LogTimestamp` (String/Instant)

We will use Kotlin's `value class` to ensure type safety with zero runtime overhead where possible.

## Consequences
- **Pros**:
    - Stronger type safety at compile time.
    - Improved readability (e.g., `fun load(path: LogFilePath)` instead of `fun load(path: String)`).
    - Validation can be centralized in the type's init block or factory method.
- **Cons**:
    - Slight increase in the number of classes.
    - Potential friction when interfacing with libraries that expect primitives.
