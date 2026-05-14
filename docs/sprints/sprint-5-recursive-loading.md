# Sprint 5: Recursive Directory Loading

## 1. Goal
Extend the log loading capabilities to support scanning and monitoring entire directory trees, automatically discovering and interleaving log files found within.

## 2. Scope

### 2.1. Recursive Scanning
- Implement `DirectoryScanner` to traverse folder structures.
- Support for pattern-based filtering (e.g., `*.log`, `*.txt`).
- Handle deep hierarchies efficiently.

### 2.2. Dynamic Merging
- Extend `MergedLogSource` or create a new `DirectoryLogSource` that dynamically adds/removes files as they are discovered or deleted.
- Ensure chronological interleaving across all discovered files.

### 2.3. UI Integration
- Update the "Open" dialog to support directory selection.
- Display directory-based tabs with appropriate iconography.
- Show the source file name in the log grid (already partially implemented, but need to ensure it works for dynamic sets).

## 3. Key Decisions
- **Discovery Strategy**: Use `java.nio.file.WatchService` or periodic polling to detect new files in selected directories.
- **Interleaving Performance**: Limit the number of concurrently open files in a single directory view if performance degrades.

## 4. Definition of Done
- [ ] Users can select a directory and have all log files within it loaded recursively.
- [ ] New log files added to a watched directory are automatically discovered and their contents interleaved into the view.
- [ ] The UI clearly indicates which file each log entry originated from.
- [ ] Directory loading respects the "Newest First" toggle and other global filters.
