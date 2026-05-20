# TASKS: Sprint 8 - Connectivity & Remote Sources

## 13. Sprint 8: Connectivity & Remote Sources

### 13.1. SFTP/SSH Support (ADR 010)
- [x] 13.1.1. Implement `SftpLogSource` for tailing remote files
- [x] 13.1.2. Support for SSH key-based authentication
- [x] 13.1.3. Support for password authentication
- [x] 13.1.4. Use SSH tailing (`tail -f`) for real-time remote updates
- [x] 13.1.5. Implement remote directory browsing and multi-file selection
- [x] 13.1.6. Support loading all logs from a remote subdirectory
- [x] 13.1.7. Implement connection retry and staggered loading for robustness
- [x] 13.1.8. Automatically detect and add new files in monitored remote directories
- [x] 13.1.9. Fix tailing loop premature exit in `SftpLogSource`
- [x] 13.1.10. Improve robustness and responsiveness of `SftpDirectoryLogSource`
- [x] 13.1.11. Refine directory-level error handling and UI indicators
- [x] 13.1.12. Fix data loss during remote directory initialization
- [x] 13.1.13. Implement SSH connection sharing for remote directories

### 13.2. Cloud Integration (ADR 010)
- [ ] 13.2.1. Implement `S3LogSource` for reading logs from AWS S3 buckets
- [ ] 13.2.2. Support for AWS profiles authentication
- [ ] 13.2.3. Support for environment variable based authentication

### 13.3. Network Appenders
- [ ] 13.3.1. Implement a TCP listener in `:core` to receive logs
- [ ] 13.3.2. Implement a UDP listener in `:core` to receive logs
- [ ] 13.3.3. Define a lightweight protocol for streaming logs directly to KLogViewer

### 13.4. Connection Manager
- [x] 13.4.1. Build a UI for managing saved connections and credentials
- [ ] 13.4.2. Implement secure credential storage using OS-level keychains
- [x] 13.4.3. Integrate connection manager with the main log loading flow
- [x] 13.4.4. Implement Tab navigation in all application dialogs (UX refinement)
- [x] 13.4.5. Update tab title when connecting to remote log sources
- [x] 13.4.6. Automatically save SFTP connection details upon connecting
- [x] 13.4.7. Restore SFTP log sources on application startup
- [x] 13.4.8. Mark failed/missing remote sources in UI (red bar, strike-through)
- [x] 13.4.9. Implement Disconnect/Reconnect button in toolbar
- [x] 13.4.10. Persist connection state in user preferences
- [x] 13.4.11. Fix SFTP directory restoration and parser name persistence on startup
- [x] 13.4.12. Ensure all remote connections are auto-saved and persisted when first opened

### 13.5. Verification & Testing
- [x] 13.5.1. Unit tests for `SftpLogSource` using a mock SSH server
- [ ] 13.5.2. Unit tests for `S3LogSource` with S3 mocks
- [ ] 13.5.3. Integration tests for network log reception
- [ ] 13.5.4. Security audit of credential storage implementation
