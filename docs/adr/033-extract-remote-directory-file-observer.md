# ADR 033: Extract RemoteDirectoryFileObserver

## Status
Proposed

## Context
The `SftpDirectoryLogSource.observeLogs()` method had high cyclomatic complexity (estimated ~25-27). It was responsible for:
1. Rescanning the remote directory.
2. Managing the lifecycle of individual file observers (launching, cancelling).
3. Coordinating initial load across multiple files.
4. Managing SSH client pooling.
5. Handling and routing log updates.

This multi-responsibility approach made the code hard to read and maintain.

## Decision
Extract the per-file observation orchestration into a dedicated `RemoteDirectoryFileObserver` class.

`RemoteDirectoryFileObserver` now handles:
- Tracking active file jobs.
- Launching new jobs for newly discovered files.
- Cancelling jobs for removed files.
- Managing individual file observation flows.
- Interacting with `LogInitialLoadCoordinator` and `SshClientPool`.

`SftpDirectoryLogSource` now focuses on:
- The high-level directory scanning loop.
- Directory-level error handling.
- Coordinating the transition from "initial load" to "incremental updates" at the directory level.

## Consequences
- **Improved Maintainability**: `SftpDirectoryLogSource` reduced from ~140 lines to ~85 lines.
- **Better Separation of Concerns**: Directory-level logic is separated from file-level logic.
- **Easier Testing**: `RemoteDirectoryFileObserver` can theoretically be tested in isolation (though currently tested via `SftpDirectoryLogSourceTest`).
- **Reduced Complexity**: The cyclomatic complexity of `observeLogs` is significantly reduced.
