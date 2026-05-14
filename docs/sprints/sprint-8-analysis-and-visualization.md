# Sprint 8: Analysis & Visualization

## 1. Goal
Transform raw log data into visual insights by providing dashboards, time-series charts, and frequency analysis tools.

## 2. Scope

### 2.1. Metrics Dashboard
- Create a dedicated "Dashboard" view.
- Implement time-series charts for log frequency (events per second/minute).
- Implement distribution charts for log levels.

### 2.2. Ad-hoc Analysis
- Enable "Frequency Analysis" on any selected field (e.g., "Show count of unique IP addresses").
- Implement log diffing to compare patterns between two time periods or two different logs.

### 2.3. Charting Engine
- Integrate a Compose-native charting library.
- Support for interactive charts (zoom, pan, click-to-filter).

## 3. Key Decisions
- **Sampling for Performance**: Use data sampling for extremely large log files to ensure charts remain responsive.
- **Background Aggregation**: Perform metric calculations on background threads (`Dispatchers.Default`) to avoid UI stutters.

## 4. Definition of Done
- [ ] A dashboard view shows real-time log frequency and level distribution.
- [ ] Users can generate a frequency report for any structured field.
- [ ] Charts are interactive and integrated into the desktop UI theme.
- [ ] Log diffing provides a clear visual comparison of two log streams.
