# RunescapeClaude - HDOS Render Engine Plugin

A RuneLite plugin that integrates the HDOS render engine for styled text overlays, animated progress tracking, and custom UI rendering.

## Features

- **Styled Text Rendering** - Rich text with inline color codes (`#RRGGBB`), shadow, glow, outline, and underline effects
- **Progress Bar Overlay** - Clip-based progress bar with configurable colors, size, and rounded corners
- **Animation System** - Frame sequencing with progress thresholds and timed delays
- **XP Tracking** - Track skill XP progress with ETA calculations
- **Custom Layout Engine** - Weight-based `LayoutManager2` for plugin panel UI
- **Golden-Ratio Text Wrapping** - Multi-line text auto-wraps targeting phi (1.618) width:height ratio

## Architecture

```
com.hdos.render
  ├── GraphicsUtils          - Anti-aliasing, window centering
  ├── AnimationController    - Frame sequencing with progress thresholds
  ├── text/
  │   └── StyledText         - Rich text: color markup, shadow, glow, multi-line wrap
  ├── layout/
  │   ├── BaseLayout         - Abstract LayoutManager2 with weight constraints
  │   └── VerticalLayout     - Vertical stacking layout
  └── overlay/
      ├── HdosRenderPlugin   - RuneLite plugin entry point
      ├── HdosRenderConfig   - Plugin configuration
      ├── HdosRenderOverlay  - Styled text overlay (status, %, ETA)
      ├── HdosProgressOverlay- Progress bar overlay
      └── HdosRenderPanel    - Sidebar plugin panel
```

## Building

Requires Java 11+.

```bash
./gradlew build
```

## Configuration

All settings are configurable in the RuneLite plugin panel:

- **Text Rendering**: font size, shadow/glow effects, colors
- **Progress Bar**: width, height, fill color, background color
- **Animation**: enable/disable, speed (ms per frame)
