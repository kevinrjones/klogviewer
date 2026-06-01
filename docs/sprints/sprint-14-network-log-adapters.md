# Sprint 14: Network Log Adapters

## 1. Goal
Enable KLogViewer to ingest live logs directly over the network using TCP/UDP listeners, with clear operational controls and secure transport options.

## 2. Scope

### 2.1. Network Appenders
- Implement TCP and UDP listeners in `:core` to receive logs from remote appenders.
- Define a lightweight ingestion protocol for streaming logs directly to KLogViewer.
- Support sender identity and multi-stream interleaving.

### 2.2. Compatibility Targets (Sprint 14)
- Support Java Logback `SocketAppender` senders.
- Support Log4j `SocketAppender` senders.
- Support NLog network targets.
- Support Serilog network sinks.
- Support Python logging socket handlers.
- Support Logstash protocol ingestion profile.
- Support OpenTelemetry logs ingestion profile.

### 2.3. Listener Controls & Persistence
- Add toolbar controls to start and stop listeners.
- Persist listener configuration and auto-start preferences.
- Surface listener health, throughput, and connection information in the status bar.

### 2.4. Security
- Support TLS-encrypted TCP listeners.
- Provide safe defaults for bind addresses and warnings for insecure exposure.

## 3. Key Decisions
- **Protocol Baseline**: Start with newline-framed `plain-line` ingestion for fast delivery and compatibility with simple emitters.
- **Compatibility Profiles**: Add parser/codec profiles so major logging ecosystems can emit directly to Sprint 14 listeners.
- **Operational Safety**: Use bounded buffering and explicit overflow policies to preserve UI responsiveness under load.

## 4. Definition of Done
- [ ] KLogViewer can receive and display logs over TCP and UDP.
- [ ] Multiple concurrent network streams are ingested with source identification.
- [ ] Java Logback, Log4j, NLog, Serilog, and Python socket-based emitters are verified against compatibility fixtures.
- [ ] Logstash protocol ingestion is available and validated.
- [ ] OpenTelemetry logs ingestion is available and validated.
- [ ] Listener controls and configuration persistence are available in the UI.
- [ ] TLS mode is available for TCP listeners.
- [ ] Integration and performance validation for network ingestion is complete.

## 5. Future Protocol Candidates (Later Versions)
- GELF (Graylog Extended Log Format)
- Fluentd Forward protocol
- Loki Push API
- Vector socket/native protocol
- Splunk HEC (HTTP Event Collector)
- Windows Event Forwarding bridge
- Kafka ingestion bridge