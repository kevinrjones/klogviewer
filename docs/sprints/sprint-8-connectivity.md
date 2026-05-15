# Sprint 8: Connectivity & Remote Sources

## 1. Goal
Enable KLogViewer to ingest logs from distributed environments by supporting SFTP/SSH, Cloud Storage, and direct network streams.

## 2. Scope

### 2.1. SFTP/SSH Support
- Implement `SftpLogSource` for tailing remote files.
- Support for SSH key-based and password authentication.

### 2.2. Cloud Integration
- Implement `S3LogSource` for reading logs from AWS S3 buckets.
- Support for common cloud authentication patterns (AWS profiles, environment variables).

### 2.3. Network Appenders
- Implement a TCP/UDP listener in `:core` to receive logs from remote appenders.
- Define a lightweight protocol for streaming logs directly to KLogViewer.

### 2.4. Connection Manager
- Build a UI for managing saved connections and credentials.
- Implement secure credential storage using OS-level keychains (via a library like `secret-service` or `keychain`).

## 3. Key Decisions
- **Polling vs. Streaming**: Use SSH tailing (`tail -f`) for real-time remote updates where possible.
- **Credential Security**: Never store raw passwords in plain text; use native OS integration for secrets.

## 4. Definition of Done
- [ ] Users can open and tail a log file from a remote server via SFTP.
- [ ] Logs can be read from an AWS S3 bucket.
- [ ] KLogViewer can receive and display logs sent over the network (TCP/UDP).
- [ ] Connections can be saved and reused securely.
