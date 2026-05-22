# ADR-028: Refactoring SFTP Log Sources

## Context
The `SftpLogSource` and `SftpDirectoryLogSource` were becoming large and difficult to maintain due to multiple responsibilities being handled within the same class, including:
- SSH connection and authentication logic.
- SSH client pooling and session management.
- Remote `tail -f` command execution and streaming.
- Aggregation of initial log loads from multiple remote files.
- Exception-to-`LogFailure` mapping.

This led to code duplication between `SftpLogSource`, `SftpDirectoryLogSource`, and `SftpFileSystem`.

## Decision
We decided to decompose these responsibilities into dedicated, focused components:

1.  **SshService**: A centralized service for creating and authenticating `SSHClient` instances. It includes retry logic (`withRetry`) and supports password and key-pair authentication.
2.  **SshClientPool**: Manages a pool of authenticated `SSHClient` instances, allowing multiple sessions to share a single connection while respecting session limits.
3.  **RemoteLogTailer**: Encapsulates the logic for executing a remote `tail -f` command and streaming the output as lines. It handles command/session lifecycle and cancellation.
4.  **LogInitialLoadCoordinator**: Manages the aggregation of initial log loads from multiple files in a directory, ensuring a consistent `Initial` update is emitted only when all files have been processed.
5.  **LogFailure Extensions**: Small helper functions to map `Throwable` and `String` to `LogFailure`.

## Consequences
- **Improved Maintainability**: Each component has a single responsibility and can be tested or modified independently.
- **Reduced Duplication**: Connection and authentication logic is now shared across all SFTP-related components.
- **Enhanced Testability**: We can now mock individual components like `SshService` or `RemoteLogTailer` more easily in unit tests.
- **Cleaner Code**: `observeLogs` functions in the main log sources are now much shorter and focus purely on coordinating the flow.
