# ADR-005: Streaming LogSource Architecture

## Status
Accepted

## Context
The initial implementation of `LogService` loaded the entire log file into memory as a list of entries. This approach is not scalable for large log files and does not easily support real-time monitoring ("Tail -f"). Furthermore, concurrency management was leaking into the UI layer (ViewModel).

## Decision
We have replaced `LogService` with a deeper `LogSource` architecture:
1.  **Repository Interface**: `LogSource` in the `:domain` module defines a streaming interface using Kotlin `Flow`.
2.  **Streaming Data Flow**: Instead of returning a full list, `LogSource` emits `LogUpdate` objects (`Initial`, `Appended`, `Reset`). This supports delta-based updates.
3.  **Internalized Concurrency**: The concrete `FileLogSource` manages its own `CoroutineDispatcher` (defaulting to `Dispatchers.IO`), ensuring that calling code (like the UI) remains responsive without manual thread management.
4.  **Source-side Parsing**: Parsing happens within the stream, allowing the UI to consume typed `LogEntry` objects directly.

## Consequences
- **Pros**:
    - **Scalability**: Large files can be read and emitted in chunks (though the initial implementation still reads lines, the architecture now supports chunking).
    - **Extensibility**: "Tail -f" can be implemented by simply emitting `LogUpdate.Appended` events from the same Flow.
    - **Locality**: Concurrency and IO details are concentrated in the `:core` implementation.
    - **Leverage**: Callers (ViewModels) get a reactive, thread-safe stream of log updates via a single function call.
- **Cons**:
    - Slightly more complex state management in the UI (handling deltas vs full lists).
