# TASKS: Sprint 14 - Network Log Adapters

## 14. Sprint 14: Network Log Adapters

> Task IDs from the original connectivity plan are intentionally preserved to keep cross-document references stable.

### 13.3. Network Appenders
- [ ] 13.3.1. Implement a TCP listener in `:core` to receive logs
- [ ] 13.3.2. Implement a UDP listener in `:core` to receive logs
- [ ] 13.3.3. Define a lightweight protocol for streaming logs directly to KLogViewer
- [ ] 13.3.4. Support multiple concurrent network log streams
- [ ] 13.3.5. Implement buffer management and overflow handling for high-volume streams
- [ ] 13.3.6. Add source identification for network streams (sender IP/hostname)
- [ ] 13.3.7. Implement UI toggle to start/stop network listeners in the toolbar
- [ ] 13.3.8. Persist listener configuration and auto-start state in user preferences
- [ ] 13.3.9. Support secure TLS-encrypted TCP listeners
- [ ] 13.3.10. Display network listener status and connection count in the status bar
- [ ] 13.3.11. Add Java Logback `SocketAppender` compatibility profile and fixture validation
- [ ] 13.3.12. Add Log4j `SocketAppender` compatibility profile and fixture validation
- [ ] 13.3.13. Add NLog network target compatibility profile and fixture validation
- [ ] 13.3.14. Add Serilog network sink compatibility profile and fixture validation
- [ ] 13.3.15. Add Python logging socket handler compatibility profile and fixture validation
- [ ] 13.3.16. Add Logstash protocol ingestion profile and parser mapping
- [ ] 13.3.17. Add OpenTelemetry logs ingestion profile and `LogUpdate` mapping

### 13.5. Verification & Testing
- [ ] 13.5.4. Integration tests for network log reception and multi-stream interleaving
- [ ] 13.5.5. Performance testing for high-volume network log streams and buffer limits
- [ ] 13.5.6. Compatibility integration suite for Logback, Log4j, NLog, Serilog, and Python socket emitters
- [ ] 13.5.7. Protocol integration suite for Logstash and OpenTelemetry ingestion paths

### 13.6. Future Protocol Candidates (Later Versions)
- [ ] 13.6.1. Evaluate GELF ingestion profile
- [ ] 13.6.2. Evaluate Fluentd Forward ingestion profile
- [ ] 13.6.3. Evaluate Loki Push API ingestion profile
- [ ] 13.6.4. Evaluate Splunk HEC ingestion profile
- [ ] 13.6.5. Evaluate Kafka ingestion bridge