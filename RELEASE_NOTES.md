# KLogViewer 1.7.1 Release Notes

**Release date:** 2026-06-03  
**Release type:** Patch release  
**Scope:** Build and packaging updates since `1.7.0`

## Highlights

- Added Linux ARM64 build and packaging support to the GitHub Actions release pipeline.
- Release artifacts now include a dedicated Linux ARM64 executable bundle and DEB package alongside existing macOS, Windows, and Linux x64 outputs.

## Improvements

### CI/CD and Packaging

- Expanded the CI build matrix with a native `ubuntu-24.04-arm` job.
- Added explicit Linux ARM64 artifact naming (`linux-arm64`) to keep release outputs clear and collision-free.
- Generalized Linux workflow conditions so Linux setup and test steps run consistently for both Ubuntu x64 and Ubuntu ARM64 runners.

## User Impact

- **Linux ARM users** can now use official release artifacts built directly for ARM64 environments.
- **Windows/macOS/Linux x64 users** are unaffected; existing installer and executable outputs remain unchanged.
- **Maintainers and release engineers** now get cross-architecture Linux artifacts in one release workflow run.

## Upgrade Notes

- No application data migration is required.
- If you automate artifact downloads, include the new `linux-arm64` artifact variant in your release scripts.

## Known Issues

- No new customer-facing known issues were identified for this release.

## Sprint 12 Structured Data Update (post-1.7.1)

### Structured Performance and Analysis Improvements

- Added bounded, deterministic projection caching for structured payload compatibility/path projections to reduce repeated reparsing across dashboard/filter workflows.
- Added structured flattening guardrails with configurable limits for depth, array breadth, and indexed-path count.
- Added graceful truncation metadata marker (`_meta.limit=(limit-exceeded)`) when structured projection limits are hit.

### Dashboard Structured Field Enhancements

- Dashboard frequency field discovery now includes canonical fields plus bounded discovered structured compatibility paths.
- Frequency analysis now resolves selected fields via compatibility projections, preserving `(missing)` behavior when structured values are absent.
- Added top-N overflow bucketing to `(other)` for high-cardinality frequency summaries.

### Verification Highlights

- Added/extended regression coverage for:
  - structured projection cache reuse and eviction,
  - flattening limits (depth/array breadth) and truncation markers,
  - structured dashboard frequency field discovery and `(other)` bucketing.
- Validated touched-module suites:
  - `:core:test --tests com.klogviewer.core.analysis.InMemoryAnalysisMetricsRepositoryTest`
  - `:domain:test --tests com.klogviewer.domain.model.StructuredLogDataTest`
  - `:ui:test --tests com.klogviewer.ui.viewmodel.DashboardIntentTest`

### Known Limitations / Deferred Follow-ups

- Discovered structured column selection/persistence UI enhancements remain open for a follow-up Sprint 12E completion pass.
- Broader performance/live-tail stress suites and full-sprint quality gates (`detekt`, full `check`) should be rerun before final Sprint 12E closure.