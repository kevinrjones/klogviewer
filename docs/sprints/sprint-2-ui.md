# Sprint 2: UI/UX Refinement (In Progress)

## 1. Goal
Transform the "Walking Skeleton" into a professional-grade log viewer. Prioritize visual clarity, information density, and interactive analysis tools using a **"Command-Line Chic"** aesthetic.

## 2. Scope

### 2.1. Visual Identity & Theme
- Implement "Command-Line Chic" theme with Light/Dark mode support.
- Use high-contrast accents (#00A3E0) on near-black background (#121212) for Dark Mode.
- Enforce `JetBrains Mono` for all log content.

### 2.2. Layout & Components
- Add a left sidebar for filters and settings.
- Add a bottom status bar for file metadata (path, line count, encoding).
- Refactor top bar to include a search entry.

### 2.3. Analysis Tools
- Real-time log level filtering (INFO, WARN, ERROR, etc.).
- Text search with live result highlighting.
- Regex-based intelligent highlighting (IDs, IP addresses, timestamps).
- Line numbering in a gutter.

## 3. Key Decisions
- **Theme**: Custom `LogViewerTheme` defaulting to Dark Mode.
- **Typography**: `JetBrains Mono` for tabular alignment.
- **Highlighting Engine**: Regex-based `LogHighlighter` using `AnnotatedString`.
- **State Management**: Extend `LogViewerState` for search/filter; background processing in `ViewModel`.

## 4. Definition of Done
- [ ] "Command-Line Chic" aesthetic verified in Light and Dark modes.
- [ ] Log levels can be toggled on/off with immediate UI update.
- [ ] Search terms are highlighted and filtered correctly.
- [ ] IDs, IPs, and timestamps are automatically highlighted.
- [ ] Line numbers are visible and accurate.
- [ ] Scrolling remains fluid with 50,000+ lines.
