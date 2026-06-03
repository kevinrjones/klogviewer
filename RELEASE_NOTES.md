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