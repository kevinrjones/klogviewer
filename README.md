# KLogViewer

[![Build Status](https://github.com/kevinrjones/klogviewer/actions/workflows/build.yml/badge.svg)](https://github.com/kevinrjones/klogviewer/actions)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose for Desktop](https://img.shields.io/badge/Compose%20for%20Desktop-1.6.11-orange.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Platforms](https://img.shields.io/badge/platform-macOS%20%7C%20Windows%20%7C%20Linux-lightgrey)

KLogViewer is a professional-grade, high-density desktop log viewer built with Kotlin and Compose for Desktop. It is designed to handle large log files and complex distributed system events with ease, providing a reactive and highly customizable viewing experience.

## Key Features

### Multiple Tabs & Workspaces
Work on multiple log files or different views simultaneously using a native tabbed interface. Each tab maintains its own filters, search queries, and scroll position.

### Horizontal Split Panes
Inspect log entries in detail without losing context. Selecting a log entry opens a horizontal detail pane at the bottom, displaying full content, stack traces, and structured metadata.

### Interleaved Log Streams
Merge multiple log files into a single, chronologically sorted view. Visual source badges and subtle background shading help you distinguish between different log sources at a glance.

### Real-time Log Tailing
Monitor your logs as they grow. KLogViewer uses an efficient polling mechanism to detect file appends and automatically scrolls to the latest entries.

### Remote Log Streams (SFTP)
Connect to remote servers and tail logs in real-time over SSH. Supports both password and public-key authentication (RSA, Ed25519, etc.) with optional passphrase support for encrypted keys.

### Advanced Heuristic Parsing
Supports a wide variety of log formats out-of-the-box, including:
- **Standard Text**: ISO8601, Apache, Syslog, CSV.
- **Structured Data**: JSON and Logfmt (key-value) formats.
- **Multiline Support**: Automatically aggregates stack traces and indented content.
- **Auto-Detection**: Heuristically probes file content to select the best parser template.

### Professional Grid UI
- **Resizable Columns**: Interactively adjust column widths with persistence across sessions.
- **Command-Line Chic Theme**: Custom Industrial Dark and Clean Light palettes.
- **Regex Filtering & Search**: Real-time filtering with support for complex regular expressions.
- **Smart Highlighting**: Automatically highlights IDs, IP addresses, and timestamps.

## Screenshots

*(Screenshots will be added soon)*

### Main Interface
![KLogViewer Main Interface](docs/images/screenshots/main_interface.png)
*The high-density grid showing interleaved logs with source badges and level filtering.*

### Split View & Details
![KLogViewer Split View](docs/images/screenshots/split_view.png)
*The horizontal detail pane displaying a full stack trace for a selected ERROR entry.*

### Dark Mode
![KLogViewer Dark Mode](docs/images/screenshots/dark_mode.png)
*Industrial Dark theme designed for long-running analysis sessions.*

## Technology Stack

- **Language**: Kotlin 2.0.0
- **UI Framework**: Compose for Desktop (JetBrains)
- **Architecture**: MVI (Model-View-Intent) for predictable state management
- **Logic**: Arrow for functional error handling and data flow
- **Testing**: JUnit 5, Strikt (assertions), and Cucumber JVM (BDD)
- **Build System**: Gradle Kotlin DSL with Version Catalog

## Getting Started

### Prerequisites
- JDK 17 or higher

### Running the Application
To run the application directly from source:
```bash
./gradlew :app:run
```

### Building the Native Distribution
To create a native distribution for your operating system (DMG, MSI, or DEB):
```bash
./gradlew :app:package
```
Distributions will be available in `app/build/compose/binaries`.

## Usage

### Connecting to SFTP Log Sources
KLogViewer allows you to tail logs from remote servers via SFTP.

1. Go to **File > Connect to SFTP...**
2. Enter the **Host**, **Port** (default 22), and **Username**.
3. Choose your **Authentication** method:
   - **Password**: Enter your SFTP password.
   - **Key Pair**: 
     - Click **Browse** to select your **Private Key file**. You should select your **private** key (e.g., `id_rsa`, `id_ed25519`), not the public `.pub` file.
     - If your private key is protected by a password, enter it in the **Passphrase (Optional)** field.
4. Enter the **Log File Path** on the remote server (e.g., `/var/log/syslog`).
5. Click **Connect**.

KLogViewer uses `tail -f` over SSH to provide efficient, real-time updates for remote files.

### Continuous Integration & Deployment
KLogViewer uses GitHub Actions for automated building and packaging. For every push to the `main` branch, the following are automatically generated:
- **Installers**: DMG (macOS), MSI (Windows), and DEB (Linux).
- **Standalone Executables**: Platform-specific zipped bundles for all three major operating systems.

You can find these deployable units in the [GitHub Actions artifacts](https://github.com/kevinrjones/klogviewer/actions).

## Architecture

KLogViewer follows a clean, multi-module architecture:
- `:domain`: Core business models and interfaces.
- `:core`: Business logic, parsers, and repository implementations.
- `:ui`: Composable components, themes, and MVI state management.
- `:app`: Application entry point and configuration.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
