# TASKS: Sprint 8 - Connectivity & Remote Sources

## 13. Sprint 8: Connectivity & Remote Sources

### 13.1. SFTP/SSH Support (ADR 010)
- [x] 13.1.1. Implement `SftpLogSource` for tailing remote files
- [x] 13.1.2. Support for SSH key-based authentication
- [x] 13.1.3. Support for password authentication
- [x] 13.1.4. Use SSH tailing (`tail -f`) for real-time remote updates

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

### 13.5. Verification & Testing
- [x] 13.5.1. Unit tests for `SftpLogSource` using a mock SSH server
- [ ] 13.5.2. Unit tests for `S3LogSource` with S3 mocks
- [ ] 13.5.3. Integration tests for network log reception
- [ ] 13.5.4. Security audit of credential storage implementation
