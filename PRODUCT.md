# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive

## Users
Backend engineers, Site Reliability Engineers (SREs), and DevOps professionals who investigate distributed system incidents, debug production failures, and correlate logs across multiple microservices, remote servers, and cloud environments.

## Product Purpose
KLogViewer is a professional-grade, high-density desktop log viewer designed to ingest, parse, filter, and analyze large local and remote log files in real-time. Success means reducing mean-time-to-resolution (MTTR) during outages by providing instantaneous search, structured inspection, and unified cross-stream timelines without requiring heavy server-side log aggregation infrastructure.

## Positioning
A fast, local-first desktop application combining real-time streaming (local files, SFTP, and AWS S3) with multi-stream chronological interleaving, custom interactive pattern matching (Pattern Wizard), and zero-server-dependency operation.

## Operating Context
Used during live incident response, post-mortem root cause investigations, local application development, and remote server monitoring. Operates on developer/operator workstations across macOS, Linux, and Windows, connecting to local disks, SSH/SFTP bastion hosts, and AWS S3 buckets.

## Capabilities and Constraints
- **Streaming Ingestion**: Real-time tailing of local files, remote SFTP logs, and AWS S3 objects/prefixes.
- **Interleaving**: Merges multiple log files into a single chronologically sorted view with source badges and origin visual accents.
- **Parsing Pipeline**: Advanced heuristic detection, Logback/Log4j and Serilog pattern import, multiline stack trace aggregation, and JSON/logfmt structured data extraction.
- **Pattern Wizard**: 5-zone interactive visual modal for drafting, testing, and persisting directory-scoped custom log patterns.
- **Security**: Integration with native OS credential stores (macOS Keychain, Linux Secret Service, Windows Credential Locker) for secure SSH and AWS credential management.
- **Desktop UI**: Resizable columns, horizontal detail split-pane, compact/full cell text toggle with popup reveal, industrial dark and clean light desktop themes.
- **Technical Stack**: Kotlin 2.0.0, Compose for Desktop, Gradle Kotlin DSL, MVI architecture, Arrow functional error handling.

## Brand Commitments
- **Name**: KLogViewer
- **Aesthetic**: Command-Line Chic — high information density, industrial precision, subtle slate/blue-gray tones, crisp typography, and uncluttered desktop toolbars.
- **Voice**: Pragmatic, direct, engineer-centric, and utility-focused.

## Evidence on Hand
- Full four-module codebase (`:domain`, `:core`, `:ui`, `:app`) with >290 automated tests.
- Comprehensive architectural documents (`docs/ARCHITECTURE.md`, `docs/STRUCTURED-DATA-MODEL.md`, `docs/CONNECTIVITY-DESIGN.md`).
- Domain glossary (`docs/UBIQUITOUS_LANGUAGE.md`) and high-level requirements (`docs/HIGH-LEVEL-REQUIREMENTS.md`).
- Sprint specifications and task logs under `docs/sprints/` and `docs/tasks/`.

## Product Principles
- **Local-First & Lightweight**: Zero external server runtime or database required to open and analyze log files.
- **Density Over Decoration**: Maximize visible data lines, structure clarity, and scanability for high-stress debugging sessions.
- **Fast Interactive Feedback**: Instantaneous regex filtering, live pattern drafting, and real-time streaming updates.
- **Rock-Solid Security**: Never persist credentials in plaintext; always delegate to native OS secret storage.
- **Type-Safe Domain Isolation**: Clean separation between domain contracts, core streaming/parsers, and Compose UI.

## Accessibility & Inclusion
- High-contrast Industrial Dark and Clean Light themes with legible typographic hierarchy.
- Full keyboard navigation and shortcut support (Cmd/Ctrl+Enter, Esc dialog dismissal, tab switching).
