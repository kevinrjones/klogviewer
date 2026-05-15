# Ubiquitous Language

This document defines the core terminology used across the LogViewer project to ensure a shared understanding between developers, stakeholders, and the codebase.

## Core Domain Concepts

### Log Entry
A single record extracted from a log source. It fundamentally consists of a **Timestamp**, a **Level**, and **Content**.
*   **Timestamp**: The date and time when the log event occurred.
*   **Level**: The designated severity of the entry (e.g., DEBUG, INFO, WARN, ERROR, FATAL).
*   **Content**: The main message or payload of the log entry.

### Log Level
The severity classification of a **Log Entry**. The system normalizes various external representations into a standard internal set:
*   `DEBUG`: Fine-grained informational events that are most useful to debug an application.
*   `INFO`: Informational messages that highlight the progress of the application at coarse-grained level.
*   `WARN`: Potentially harmful situations.
*   `ERROR`: Error events that might still allow the application to continue running.
*   `FATAL`: Very severe error events that will presumably lead the application to abort.

### Log Source
An abstraction representing a stream of **Log Entries**. Common implementations include local files, remote streams, or merged collections.

### Log Parser
The component responsible for converting raw data (e.g., lines of text) from a **Log Source** into structured **Log Entry** objects.

### Log Template
A configuration (typically regex-based) that defines how a **Log Parser** should identify and extract core fields (timestamp, level, content) from a specific log format.

### Level Mapper
A translation mechanism that maps diverse string representations of log levels (e.g., "WRN", "SEVERE", "ERR") found in raw logs to the internal **Log Level** enum.

## Advanced Features

### Structured Data
Metadata associated with a **Log Entry**, represented as key-value pairs (fields) extracted during parsing. These provide context beyond the core fields.

### Interleaving
The process of merging **Log Entries** from multiple **Log Sources** into a single, unified chronological view, allowing for correlation of events across different systems or files.

### Merged Log Source
A specialized **Log Source** that performs **Interleaving** of multiple underlying streams into one.

### Heuristic Probe
An automated mechanism that attempts to match the first few lines of a log file against registered **Log Templates** to automatically detect the file format.

## Technical Architecture

### Tiny Type
A type-safe wrapper around a primitive (e.g., `LogFilePath`, `LogContent`, `LogTimestamp`) used to avoid "primitive obsession" and centralize validation logic.

### Log Update
A discrete packet of data emitted by a **Log Source**, representing a state change in the log stream:
*   `Initial`: The full set of entries loaded when a source is first opened.
*   `Appended`: New entries that have been added to the stream (e.g., as a file grows).
*   `Reset`: A command to clear all currently loaded entries for a source.

### Workspace
A persistent state of the application, including open files, active tabs, and UI layout configurations.

## UI Concepts

### Tab
A specific view within the UI that can contain one or more **Log Windows**.

### Log Window
A UI component that encapsulates a single log view, including its own filters, sorting state, and displayed **Log Entries**.

### Split View
A layout feature that allows multiple **Log Windows** to be displayed simultaneously within a single **Tab**, typically arranged horizontally.

### Filter
A criteria or search query used to include or exclude **Log Entries** from a specific **Log Window**.

### Gutter
The visual area on the left of the log view typically used for displaying line numbers and entry-specific indicators.
