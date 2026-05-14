# Tasks: Logging Integration

## Infrastructure [x]
- [x] Add logging dependencies to `libs.versions.toml` (SLF4J, Logback, Logstash, kotlin-logging)
- [x] Configure module dependencies in `build.gradle.kts` files
- [x] Create `adr-014-structured-logging.md`
- [x] Create `logback.xml` with console and JSON appenders

## Implementation [x]
- [x] Add logging to `FileLogSource` (info/debug/error)
- [x] Add logging to `SimpleLogParser` (debug)
- [x] Add logging to `MergedLogSource` (info/error)
- [x] Add logging to `LogViewerViewModel` (intents/errors)
- [x] Add startup log to `Main.kt`

## Verification [x]
- [x] Ensure project builds and tests pass
- [x] Verify error events are explicitly logged
