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

## Advanced Format Support [ ]
- [ ] Implement `LevelMapper` for flexible level names (INF, ERR, etc.)
- [ ] Support optional levels (default to UNKNOWN)
- [ ] Add `LogTemplate` system for custom regex and date patterns
- [ ] Implement multiline support for stack traces
- [ ] Add `JsonLogParser` for structured logs
- [ ] Implement format auto-detection heuristic
