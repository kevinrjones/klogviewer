# KLogViewer 1.6.0 Release Notes

**Release date:** 2026-05-29  
**Release type:** Minor feature release  
**Scope:** All shipped changes since `1.5.x`

## Highlights

- Added a dedicated **Dashboard workspace** to analyze logs with time-series, frequency, and comparison views.
- Improved **time filtering** with direct `From`/`To` date-time inputs and safer boundary handling.
- Hardened dashboard and chart UX with persisted dashboard ranges, hover date tooltips, and stronger interaction reliability.
- Refined log-level presentation so level analytics appear only when raw level fields are actually present.

## New Features

### Dashboard Analysis Workspace

- Added per-window `Logs` / `Dashboard` workspace modes.
- Added time-series analytics with interaction-driven filtering from chart selections.
- Added ad-hoc structured-field frequency analysis for top-value exploration.
- Added A/B comparison workflows for baseline vs comparison windows with deterministic ordering.

### Time-Range Controls

- Added synchronized dashboard and filter-bar time-range controls.
- Persisted dashboard time filters so analysis context is restored between sessions.
- Added chart x-axis hover date tooltips for better point-in-time inspection.

## Improvements

- Simplified chart axis configuration and updated Compose/charting dependencies to keep dashboard rendering behavior consistent.
- Improved log-list and dashboard interaction behavior, including pointer-mapping reliability in level-distribution interactions.
- Expanded user and internal docs for dashboard A/B comparison and frequency-analysis workflows.

## Fixes and Reliability

- Reworked time-filter UI from large dropdown option lists to direct date-time text input controls with clear actions.
- Added explicit ±1 second tolerance for active time-range boundaries to reduce near-edge misses.
- Fixed level presentation mismatches by rendering row levels only from explicit raw `fields["level"]` data.
- Removed misleading dashboard `Level distribution` output when a dataset has no raw level field.
- Added focused UI and ViewModel regression tests for dashboard UX hardening and time-filter behavior.

## Upgrade Notes

- No migration steps are required for this release.
- Existing workspaces and preferences remain compatible.
- If you previously relied on per-entry timestamp dropdowns, use the new direct `From`/`To` date-time inputs (`YYYY-MM-DD HH:mm:ss`).

## Known Issues

- No new customer-facing known issues were identified for this release.