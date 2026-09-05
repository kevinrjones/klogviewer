---
name: KLogViewer
description: A dense, precise incident-response workspace for inspecting live and historical logs.
colors:
  primary-light: "#007ACC"
  primary-dark: "#00A3E0"
  on-primary-light: "#FFFFFF"
  on-primary-dark: "#102027"
  background-light: "#FFFFFF"
  background-dark: "#2B2B2B"
  surface-light: "#F5F5F5"
  surface-dark: "#3C3F41"
  surface-variant-light: "#EBEBEB"
  surface-variant-dark: "#323537"
  tab-background-light: "#E0E0E0"
  tab-background-dark: "#323232"
  text-light: "#121212"
  text-dark: "#E0E0E0"
  status-error: "#B71C1C"
  status-disconnected-light: "#546E7A"
  status-disconnected-dark: "#455A64"
  log-info-light: "#007ACC"
  log-info-dark: "#00A3E0"
  log-warn-light: "#856404"
  log-warn-dark: "#FFC107"
  log-error-light: "#D32F2F"
  log-error-dark: "#FF5252"
  log-debug-light: "#616161"
  log-debug-dark: "#9E9E9E"
  log-trace-light: "#6A1B9A"
  log-trace-dark: "#9575CD"
  log-fatal-light: "#B71C1C"
  log-fatal-dark: "#FF1744"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "20sp"
    fontWeight: 700
    lineHeight: "24sp"
    letterSpacing: "-0.2sp"
  headline:
    fontFamily: "sans-serif"
    fontSize: "18sp"
    fontWeight: 700
    lineHeight: "22sp"
    letterSpacing: "-0.1sp"
  title:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 700
    lineHeight: "20sp"
  body:
    fontFamily: "sans-serif"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
  label:
    fontFamily: "sans-serif"
    fontSize: "10sp"
    fontWeight: 500
    lineHeight: "12sp"
    letterSpacing: "0.5sp"
rounded:
  sm: "4dp"
spacing:
  xxs: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "48dp"
components:
  button-primary-light:
    backgroundColor: "{colors.primary-light}"
    textColor: "{colors.on-primary-light}"
    rounded: "{rounded.sm}"
    padding: "8dp 16dp"
  button-primary-dark:
    backgroundColor: "{colors.primary-dark}"
    textColor: "{colors.on-primary-dark}"
    rounded: "{rounded.sm}"
    padding: "8dp 16dp"
  card:
    backgroundColor: "{colors.surface-light}"
    rounded: "{rounded.sm}"
    padding: "{spacing.lg}"
  filter-bar:
    backgroundColor: "{colors.surface-light}"
    rounded: "{rounded.sm}"
    padding: "{spacing.xs} {spacing.sm}"
---

# Design System: KLogViewer

## Overview

**Creative North Star: "The Incident Control Room"**

KLogViewer is a professional operations console, not a presentation surface. The interface is dense and precise: controls stay close to the data, hierarchy is explicit, and high-signal colors identify state instead of decorating the canvas. The dark theme uses industrial charcoal and steel surfaces; the light theme keeps the same discipline on clean neutral surfaces.

The visual language is built for engineers investigating incidents while attention is divided between streams, filters, timestamps, levels, and structured payloads. Compact typography, labeled toolbar groups, persistent workspace regions, and predictable elevation make the large log surface easy to scan without hiding operational detail.

**Key Characteristics:**
- Dense desktop-first layout with intentional 2–24dp rhythm.
- Signal cyan primary actions and semantic log-level colors.
- Industrial dark mode and clean light mode with equivalent hierarchy.
- Quiet surfaces, compact controls, and explicit state feedback.

## Colors

The palette treats cyan as a diagnostic signal: use it sparingly for actions, selected states, links, and the most important affordances while neutrals carry the workspace.

### Primary
- **Operator Blue** (`#007ACC` light / `#00A3E0` dark): Primary actions, selected controls, links, and the welcome entry point.
- **Primary On-Color** (`#FFFFFF` light / `#102027` dark): Text and icons placed directly on the primary accent.

### Neutral
- **Clean White** (`#FFFFFF`): Light theme application background.
- **Industrial Charcoal** (`#2B2B2B`): Dark theme application background.
- **Light Surface** (`#F5F5F5`): Light theme cards, panels, toolbar, and log table surfaces.
- **Dark Surface** (`#3C3F41`): Dark theme cards, panels, toolbar, and log table surfaces.
- **Light Surface Variant** (`#EBEBEB`): Light theme secondary surfaces and tonal separation.
- **Dark Surface Variant** (`#323537`): Dark theme secondary surfaces and tonal separation.
- **Light Tab Gray** (`#E0E0E0`): Light tab strip background.
- **Dark Tab Gray** (`#323232`): Dark tab strip background.
- **Light Ink** (`#121212`): Primary light-theme text.
- **Dark Ink** (`#E0E0E0`): Primary dark-theme text.

### Status and log levels
- **Error Red** (`#B71C1C` light and dark): Connection/error status surfaces and fatal light-theme emphasis.
- **Disconnected Slate** (`#546E7A` light / `#455A64` dark): Disconnected status surfaces.
- **Info Cyan** (`#007ACC` light / `#00A3E0` dark): Informational log entries.
- **Warning Amber** (`#856404` light / `#FFC107` dark): Warnings; maintain readable contrast in both themes.
- **Error Coral** (`#D32F2F` light / `#FF5252` dark): Error log entries.
- **Debug Gray** (`#616161` light / `#9E9E9E` dark): Debug-level entries and secondary diagnostics.
- **Trace Violet** (`#6A1B9A` light / `#9575CD` dark): Trace-level entries.
- **Fatal Crimson** (`#B71C1C` light / `#FF1744` dark): Fatal-level entries.

## Typography

**Display Font:** System sans-serif.
**Body Font:** System sans-serif.
**Label/Mono Font:** Log content may use a user-selected monospaced font; controls remain system sans-serif.

**Character:** The type system is compact, neutral, and highly legible. Bold display and section styles establish workspace hierarchy, while 13sp body text and 11sp metadata preserve density for large log volumes.

### Hierarchy
- **Display** (700, 20sp, 24sp): Main workspace and prominent welcome headings.
- **Headline** (700, 18sp, 22sp): Secondary workspace headings and high-level panel titles.
- **Title** (700, 16sp, 20sp): Section titles such as filter groups and dialog headings.
- **Body** (400, 13sp, 18sp): Control labels, descriptions, and general application copy.
- **Metadata** (400, 11sp, 15sp): Timestamps, counts, captions, and secondary status text.
- **Label** (500, 10sp, 12sp, 0.5sp tracking): Overlines, compact section labels, and toolbar group headings.

### Named Rules
**The Signal Before Ornament Rule.** Use weight, spacing, and semantic color to clarify operational state; never enlarge type or add decoration merely to fill space.

## Layout

The application uses a desktop-first workspace with persistent regions: menu bar, compact grouped filter bar, tab strip, optional sidebar, resizable log table, and optional detail/dashboard panes. The main content should receive the largest share of the viewport; supporting regions stay compact and collapse when not needed.

Use a tight rhythm built from 2dp row adjustments, 4dp control gaps and corners, 8dp panel padding, 12dp internal grouping, 16dp card padding, and 24dp between welcome actions. The expanded sidebar is 200dp wide and the collapsed sidebar is 56dp. The filter bar uses 8dp horizontal padding and 2dp vertical padding. Preserve resizable columns and allow long log values to ellipsize in compact mode rather than forcing the workspace wider.

Light and dark themes share the same geometry and density. Responsive or narrow-window adaptations should reduce optional panels before reducing the legibility of the log table.

## Elevation & Depth

The system is layered and lifted. Tonal surface changes establish the persistent workspace hierarchy, while small Compose elevations separate bars and headers from the log surface. Elevation is stronger for persistent side panels and tab bars, and strongest for transient popups; do not use shadows as decoration.

### Shadow Vocabulary
- **Table header** (`elevation: 1dp`): Separates column labels from scrolling log rows.
- **Filter bar** (`elevation: 2dp`): Keeps grouped controls legible above the workspace.
- **Sidebar and tab strip** (`elevation: 4dp`): Marks persistent navigation and workspace boundaries.
- **Popup** (`elevation: 8dp`): Places transient cell-value and tooltip surfaces above all content.

### Named Rules
**The Layered Workspace Rule.** Use tonal surfaces first and elevation second; every lifted surface must clarify a persistent region, active state, or transient interaction.

## Shapes

The form language is compact and nearly square: all Material and Material 3 theme shapes resolve to a 4dp rounded corner, including extra-small through extra-large shapes. Cards, filter chips, popups, tooltips, and state surfaces use the same radius so the product feels like one instrument.

Borders are functional rather than ornamental: use them for compact checkboxes, resize handles, chart selections, and clear input boundaries. Avoid pills and large organic silhouettes. Keep clipping limited to small surfaces and preserve the rectangular geometry of tables and toolbars.

## Components

### Buttons
- **Shape:** Compact 4dp corners with system-sans label typography.
- **Primary:** Signal cyan background, contrasting on-primary text, and compact 8dp vertical / 16dp horizontal padding.
- **Hover / Focus:** Increase contrast or surface emphasis without shifting layout; retain a visible keyboard focus treatment.
- **Secondary / Ghost / Tertiary:** Prefer neutral surface controls and icon actions; reserve cyan for the action or selected state that matters most.

### Chips
- **Style:** Filter chips use a 4dp radius, a low-alpha primary surface, and primary text; removable query chips include a clear affordance.
- **State:** Selected and active filters should be immediately distinguishable from available filter actions while remaining compact.

### Cards / Containers
- **Corner Style:** 4dp everywhere.
- **Background:** Theme surface for cards and panels; surface variant or tab gray for tonal separation.
- **Shadow Strategy:** 2dp for toolbar-like surfaces, 4dp for persistent sidebar/tab regions, and 8dp for transient popups.
- **Border:** Add only where a control or selection needs a crisp boundary.
- **Internal Padding:** 8dp for compact controls, 16dp for welcome cards and dialog content.

### Inputs / Fields
- **Style:** Compact neutral fields with 4dp corners and clear grouping inside the filter bar; use primary color for active or applied filter state.
- **Focus:** Provide a visible primary-colored focus boundary or equivalent high-contrast state without adding a glow.
- **Error / Disabled:** Use error red for validation and status errors; reduce opacity for unavailable actions while preserving readable labels.

### Navigation
- **Style:** Menu bar, tab strip, and sidebar are persistent, compact workspace chrome. Use bold or overline labels for sections, primary color for active state, and neutral tonal contrast for inactive regions.
- **States:** The sidebar expands from 56dp to 200dp; tab and toolbar states should be explicit through color, weight, and elevation rather than animation-heavy transitions.

### Log Table
- **Signature:** The log table is the product's central instrument: column headers sit on a 1dp raised surface, rows stay tight, columns remain resizable, and timestamps/metadata use subdued text.
- **Levels:** Use the semantic log-level palette only for parsed level values. Keep message content readable and allow compact mode to ellipsize overflow.
- **Sources:** Use subtle source badges and restrained per-source differentiation; source color must never compete with level severity.

## Do's and Don'ts

### Do:
- **Do** use the same 4dp corner language across cards, inputs, chips, popups, and theme shapes.
- **Do** preserve the dense 2–24dp spacing rhythm and keep the log table visually dominant.
- **Do** support equivalent light and dark hierarchy using the defined theme roles rather than inverting colors ad hoc.
- **Do** use signal cyan and log-level colors as semantic feedback with sufficient contrast.
- **Do** use elevation to explain workspace layers and transient interactions.
- **Do** keep toolbar actions organized into labeled groups such as Sources, Stream, View, More, and Filters.

### Don't:
- **Don't** introduce decorative dashboard styling, oversized hero typography, or marketing-style whitespace into the operational workspace.
- **Don't** use gradients, glassmorphism, or ornamental shadows that reduce log contrast.
- **Don't** turn every status color into a decorative accent; color should communicate a state or action.
- **Don't** replace the rectangular, compact geometry with pills or large rounded containers.
- **Don't** hide critical filtering or stream controls behind ambiguous unlabeled icon clusters.