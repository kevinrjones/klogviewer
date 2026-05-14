# ADR 010: Remote Log Sources

## Status
Proposed

## Context
LogViewer currently only supports loading logs from the local file system. In distributed environments, logs are often stored on remote servers (accessible via SFTP/SSH), in cloud storage (S3), or streamed directly over the network via logging appenders.

## Decision
We will expand the `LogSource` abstraction to support remote and streaming sources.

### 1. Abstract LogSource
The `LogSource` interface will remain the primary abstraction, but we will introduce specific implementations for:
- `SftpLogSource`: Tailing files over SSH.
- `S3LogSource`: Reading from AWS S3 buckets.
- `NetworkLogSource`: Listening on a TCP/UDP port for incoming log streams.

### 2. Connection Management
A new `ConnectionManager` will be introduced to store and manage credentials (encrypted) and connection settings for remote sources.

### 3. Buffering and Performance
Remote streams will implement aggressive local buffering to ensure the UI remains responsive even with high-latency network connections.

## Consequences
- **Positive**: Enables centralized log viewing without manual file downloads.
- **Positive**: Supports real-time monitoring of production environments.
- **Negative**: Adds complexity in managing network connections, timeouts, and security (SSH keys, S3 credentials).
- **Negative**: Requires handling partial file reads and network interruptions gracefully.
