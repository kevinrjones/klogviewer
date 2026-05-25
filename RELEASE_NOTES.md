# KLogViewer 1.5.0 Release Notes

**Release date:** 2026-05-25  
**Release type:** Customer-facing feature release  
**Scope:** All shipped changes since `1.4.x`

## Highlights

- Added native Amazon S3 support for loading and tailing remote logs.
- Expanded and hardened SFTP and remote directory workflows.
- Improved workspace usability with better tab behavior, path visibility, and filtering controls.
- Increased overall stability and test reliability across desktop UI and connectivity flows.

## New Features

### S3 Connectivity

- Connect to S3 buckets and load remote logs directly.
- Support for multiple authentication methods (default/profile/explicit credentials).
- Added S3 setup guidance for real environments in `docs/S3-SETUP.md`.

### Remote Sources in Recent History

- Recent items now correctly support remote URIs (including `sftp://`) and directory/file classification.

### Line Number Column Resizing

- The `Line #` gutter can now be resized and persisted across sessions.

### Filtering and Selection UX

- Added an **All** option for log-level filters.
- Added multi-selection support and keyboard shortcuts for common actions.

## Improvements

- **Remote connection persistence**: S3 connection details are now automatically saved and restored, aligned with SFTP behavior.
- **Path visibility and context**: Full paths are consistently shown in recent items and tooltips for truncated UI labels.
- **Directory monitoring UX refinement**: Directory tabs now provide clearer visual state handling for missing sub-files vs missing parent directories.
- **Cross-platform delivery**: CI packaging now produces installers for macOS (`.dmg`), Windows (`.msi`), and Linux (`.deb`) plus standalone bundles.

## Fixes and Reliability

- Fixed S3 update behavior and directory loading integration by unifying remote filesystem routing.
- Resolved a critical SFTP cancellation/deadlock path and improved shutdown/cleanup behavior.
- Fixed split-pane column-resize targeting issues in multi-window/tab layouts.
- Removed intrusive missing-file dialogs and replaced them with smoother in-context missing-state handling.
- Hardened UI/integration tests against async race conditions and timing flakiness.

## Security and Credentials

- Added secure credential-storage support for remote connections.
- When secure OS storage is unavailable, the app requests explicit consent before plaintext fallback.

## Upgrade Notes

- No manual migration steps are required for most users.
- Existing local-file workflows remain compatible.
- Teams deploying S3 should review `docs/S3-SETUP.md` for environment and credentials setup.

## Known Issues

- Some version labels in parts of the app/build metadata may still show older values in specific artifacts.
- A small number of UI tests can still be timing-sensitive under heavily loaded CI environments, though stability has improved significantly.