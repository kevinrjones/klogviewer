# KLogViewer Release History

## KLogViewer 1.6.0 Release Notes

**Release date:** 2026-05-29  
**Release type:** Minor feature release  
**Scope:** All shipped changes since `1.5.x`

### Highlights

- Added a dedicated **Dashboard workspace** to analyze logs with time-series, frequency, and comparison views.
- Improved **time filtering** with direct `From`/`To` date-time inputs and safer boundary handling.
- Hardened dashboard and chart UX with persisted dashboard ranges, hover date tooltips, and stronger interaction reliability.
- Refined log-level presentation so level analytics appear only when raw level fields are actually present.

### New Features

#### Dashboard Analysis Workspace

- Added per-window `Logs` / `Dashboard` workspace modes.
- Added time-series analytics with interaction-driven filtering from chart selections.
- Added ad-hoc structured-field frequency analysis for top-value exploration.
- Added A/B comparison workflows for baseline vs comparison windows with deterministic ordering.

#### Time-Range Controls

- Added synchronized dashboard and filter-bar time-range controls.
- Persisted dashboard time filters so analysis context is restored between sessions.
- Added chart x-axis hover date tooltips for better point-in-time inspection.

### Improvements

- Simplified chart axis configuration and updated Compose/charting dependencies to keep dashboard rendering behavior consistent.
- Improved log-list and dashboard interaction behavior, including pointer-mapping reliability in level-distribution interactions.
- Expanded user and internal docs for dashboard A/B comparison and frequency-analysis workflows.

### Fixes and Reliability

- Reworked time-filter UI from large dropdown option lists to direct date-time text input controls with clear actions.
- Added explicit ±1 second tolerance for active time-range boundaries to reduce near-edge misses.
- Fixed level presentation mismatches by rendering row levels only from explicit raw `fields["level"]` data.
- Removed misleading dashboard `Level distribution` output when a dataset has no raw level field.
- Added focused UI and ViewModel regression tests for dashboard UX hardening and time-filter behavior.

### Upgrade Notes

- No migration steps are required for this release.
- Existing workspaces and preferences remain compatible.
- If you previously relied on per-entry timestamp dropdowns, use the new direct `From`/`To` date-time inputs (`YYYY-MM-DD HH:mm:ss`).

### Known Issues

- No new customer-facing known issues were identified for this release.

---

## KLogViewer 1.5.0 Release Notes

**Release date:** 2026-05-25  
**Release type:** Customer-facing feature release  
**Scope:** All shipped changes since `1.4.x`

### Highlights

- Added native Amazon S3 support for loading and tailing remote logs.
- Expanded and hardened SFTP and remote directory workflows.
- Improved workspace usability with better tab behavior, path visibility, and filtering controls.
- Increased overall stability and test reliability across desktop UI and connectivity flows.

### New Features

#### Analysis & Visualization

- Added a per-window **Dashboard** workspace for time-series graphing, normalized level distribution, and ad-hoc field frequency analysis.
- Added synchronized date-time controls (`From` / `To` + presets) with chart click/brush interactions to keep log filtering and analytics aligned.
- Added explicit A/B comparison workflows with baseline/comparison windows, deterministic delta ordering, and click-back investigation flow.
- Added deterministic large-dataset sampling, background aggregation (`Dispatchers.Default`), and latency instrumentation to keep dashboard interactions responsive under high-volume append streams.

#### S3 Connectivity

- Connect to S3 buckets and load remote logs directly.
- Support for multiple authentication methods (default/profile/explicit credentials).
- Added S3 setup guidance for real environments in `docs/S3-SETUP.md`.

#### Remote Sources in Recent History

- Recent items now correctly support remote URIs (including `sftp://`) and directory/file classification.

#### Line Number Column Resizing

- The `Line #` gutter can now be resized and persisted across sessions.

#### Filtering and Selection UX

- Added an **All** option for log-level filters.
- Added multi-selection support and keyboard shortcuts for common actions.

### Improvements

- **Remote connection persistence**: S3 connection details are now automatically saved and restored, aligned with SFTP behavior.
- **Path visibility and context**: Full paths are consistently shown in recent items and tooltips for truncated UI labels.
- **Directory monitoring UX refinement**: Directory tabs now provide clearer visual state handling for missing sub-files vs missing parent directories.
- **Cross-platform delivery**: CI packaging now produces installers for macOS (`.dmg`), Windows (`.msi`), and Linux (`.deb`) plus standalone bundles.

### Fixes and Reliability

- Fixed S3 update behavior and directory loading integration by unifying remote filesystem routing.
- Resolved a critical SFTP cancellation/deadlock path and improved shutdown/cleanup behavior.
- Fixed split-pane column-resize targeting issues in multi-window/tab layouts.
- Removed intrusive missing-file dialogs and replaced them with smoother in-context missing-state handling.
- Hardened UI/integration tests against async race conditions and timing flakiness.

### Security and Credentials

- Added secure credential-storage support for remote connections.
- When secure OS storage is unavailable, the app requests explicit consent before plaintext fallback.

### Upgrade Notes

- No manual migration steps are required for most users.
- Existing local-file workflows remain compatible.
- Teams deploying S3 should review `docs/S3-SETUP.md` for environment and credentials setup.

### Known Issues

- Some version labels in parts of the app/build metadata may still show older values in specific artifacts.
- A small number of UI tests can still be timing-sensitive under heavily loaded CI environments, though stability has improved significantly.