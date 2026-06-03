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
- [x] 13.1.14. Detect and visually mark deleted files in monitored remote directories
- [x] 13.1.15. Suppress global error state for individual file deletions in monitored directories
- [x] 13.1.16. Refresh window by removing logs and source IDs when a file is deleted from a monitored directory

### 13.2. Cloud Integration (ADR 035)
- [x] 13.2.1. Implement `S3LogSource` for reading logs from AWS S3 buckets
- [x] 13.2.2. Support for AWS profiles authentication
- [x] 13.2.3. Support for environment variable based authentication
- [x] 13.2.4. Implement polling-based tailing for S3 objects
- [x] 13.2.5. Implement S3 bucket/prefix browsing and multi-object selection
- [x] 13.2.6. Support loading all logs from an S3 "directory" (prefix)
- [x] 13.2.7. Implement connection retry and staggered loading for robustness
- [x] 13.2.8. Automatically detect and add new objects in monitored S3 prefixes
- [x] 13.2.9. Detect and visually mark deleted objects in monitored S3 prefixes
- [x] 13.2.10. Refresh window by removing logs and source IDs when an object is deleted from a monitored prefix
- [x] 13.2.11. Restore S3 log sources and connection state on application startup
- [x] 13.2.12. Support adding S3 logs to existing workspace via "+" button dropdown

### 13.3. Network Appenders (Moved to Sprint 15)
- [ ] The full 13.3 scope is now tracked in `docs/tasks/TASKS-SPRINT-15-NETWORK-LOG-ADAPTERS.md`

### 13.4. Connection Manager
- [x] 13.4.1. Build a UI for managing saved connections and credentials
- [x] 13.4.2. Implement secure credential storage using OS-level keychains
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
- [x] 13.4.13. Support adding remote SFTP logs to existing workspace via "+" button dropdown
- [x] 13.4.14. Add tooltips to tabs and status bar to show fully qualified file names
- [x] 13.4.15. Prompt for explicit user consent before plaintext fallback when secure storage is unavailable

### 13.5. Verification & Testing
- [x] 13.5.1. Unit tests for `SftpLogSource` using a mock SSH server
- [x] 13.5.2. Unit tests for `S3LogSource` with S3 mocks
- [ ] 13.5.3. Integration tests for S3 directory monitoring and deletion detection
- [ ] 13.5.4. (Moved to Sprint 15) Integration tests for network log reception and multi-stream interleaving
- [ ] 13.5.5. (Moved to Sprint 15) Performance testing for high-volume network log streams and buffer limits
- [ ] 13.5.6. Security audit of credential storage implementation
