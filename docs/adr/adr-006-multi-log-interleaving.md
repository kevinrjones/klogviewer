# ADR 006: Multi-Log Interleaving Strategy

## Status
Proposed

## Context
Users need to correlate events across multiple log files (e.g., application logs and database logs). A unified chronological view is required to see the flow of events across these different sources.

## Decision
We will implement a chronological merging strategy for multiple log streams.

### 1. Source Identification
Each `LogEntry` will be updated to include an optional `sourceId` (typically the filename). This allows the UI to identify the origin of each entry and apply appropriate visual indicators (badges, colors).

### 2. MergedLogSource
A new `MergedLogSource` will be created in the `:core` module. It will:
- Accept a list of `LogSource` instances.
- Observe all streams concurrently.
- Maintain a buffer of entries from each source.
- Emit a merged stream where entries are ordered by their `LogTimestamp`.

### 3. Merging Algorithm
The merging will happen in-memory. Since log files are generally produced in chronological order, we can use a "k-way merge" style approach if we were streaming from disk, but for the initial implementation, we will merge the `Initial` updates and then handle `Appended` updates by inserting them into the sorted list or appending if they are newer than the last entry.

## Consequences
- **Positive**: Enables powerful cross-service debugging and event correlation.
- **Positive**: Reuses existing `LogSource` infrastructure.
- **Negative**: Increased memory usage when loading many large files simultaneously.
- **Negative**: Complexity in handling out-of-order logs if the timestamps between machines are not synchronized (will assume synchronized time for now).
