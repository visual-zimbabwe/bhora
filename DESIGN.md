---
name: Bhora Matchday Canvas
colors:
  surface: '#081423'
  surface-dim: '#081423'
  surface-bright: '#2f3a4b'
  surface-container-lowest: '#040f1e'
  surface-container-low: '#111c2c'
  surface-container: '#152030'
  surface-container-high: '#1f2b3b'
  surface-container-highest: '#2a3546'
  on-surface: '#d7e3f9'
  on-surface-variant: '#c5c6cd'
  inverse-surface: '#d7e3f9'
  inverse-on-surface: '#263141'
  outline: '#8f9097'
  outline-variant: '#44474d'
  surface-tint: '#b9c7e4'
  primary: '#b9c7e4'
  on-primary: '#233148'
  primary-container: '#0a192f'
  on-primary-container: '#74829d'
  inverse-primary: '#515f78'
  secondary: '#e9c349'
  on-secondary: '#3c2f00'
  secondary-container: '#af8d11'
  on-secondary-container: '#342800'
  tertiary: '#bcc6e6'
  on-tertiary: '#263049'
  tertiary-container: '#0d1830'
  on-tertiary-container: '#77819f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d6e3ff'
  primary-fixed-dim: '#b9c7e4'
  on-primary-fixed: '#0d1c32'
  on-primary-fixed-variant: '#39475f'
  secondary-fixed: '#ffe088'
  secondary-fixed-dim: '#e9c349'
  on-secondary-fixed: '#241a00'
  on-secondary-fixed-variant: '#574500'
  tertiary-fixed: '#d9e2ff'
  tertiary-fixed-dim: '#bcc6e6'
  on-tertiary-fixed: '#101b33'
  on-tertiary-fixed-variant: '#3c4661'
  background: '#081423'
  on-background: '#d7e3f9'
  surface-variant: '#2a3546'
typography:
  display-brand:
    fontFamily: EB Garamond
    fontSize: 3.25rem
    fontWeight: '400'
    lineHeight: 3.5rem
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: EB Garamond
    fontSize: 2.25rem
    fontWeight: '400'
    lineHeight: 2.6rem
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: EB Garamond
    fontSize: 1.85rem
    fontWeight: '400'
    lineHeight: 2.2rem
    letterSpacing: -0.01em
  fixture-team:
    fontFamily: Manrope
    fontSize: 1.125rem
    fontWeight: '300'
    lineHeight: 1.6rem
    letterSpacing: 0.08em
  kickoff-time:
    fontFamily: Manrope
    fontSize: 0.875rem
    fontWeight: '500'
    lineHeight: 1.25rem
    letterSpacing: 0.12em
  body-default:
    fontFamily: Manrope
    fontSize: 0.9375rem
    fontWeight: '400'
    lineHeight: 1.5rem
    letterSpacing: 0.01em
  status-editorial:
    fontFamily: Manrope
    fontSize: 0.6875rem
    fontWeight: '600'
    lineHeight: 1rem
    letterSpacing: 0.2em
spacing:
  gutter: 1rem
  margin: 1.5rem
  space-xs: 0.375rem
  space-sm: 0.75rem
  space-md: 1.25rem
  space-lg: 2.25rem
  space-xl: 3.5rem
---

## Brand & Style

The design system establishes an atmospheric, contemplative mobile experience designed exclusively for global Premier League streaming via Prime Video. It avoids standard broadcast noise, live tickers, badges, and decorative embellishment in favor of an editorial matchday programme reduced to its purest typographic essence.

### Personality & Atmosphere
- **Ink-Deep Serenity:** Rooted in an abyssal midnight navy canvas where content emerges organically rather than sitting in layered containers.
- **Literary Sophistication:** Evoking the tradition of classical British sporting chronicles through sloping, high-contrast serif italics set against precise, low-contrast Swiss modernism.
- **Pure Canvas Integration:** Elimination of solid button fills, physical pill tags, icons, and pictorial emojis. Actionable surfaces and structural dividers exist entirely through proportional typographic scale, baseline rhythm, and soft luminous text presence.

### Target Audience
Discerning international football enthusiasts who appreciate tactical stillness, literary poise, and an uninterrupted view of match fixtures, kick-off timings, and broadcast access without gamified telemetry.

## Colors

The palette revolves around deep, light-absorbent oceanic navies paired with aged parchment and gold-leaf typographic accents.

- **Canvas & Abyssal Bases (`#020c1b`, `#0a192f`):** The primary viewports operate on `#0a192f` fading into the absolute depth of `#020c1b`. Surfaces never use opaque elevated card containers; tone shifts are executed exclusively through ambient light graduations.
- **Matchday Illumination (`#f8f9fa`, `#e6f1ff`):** Primary text and headline titling leverage an unblemished, crisp ivory to guarantee legibility across night viewing conditions.
- **Championship Leaf (`#d4af37`):** Reserved for live status indicators ("LIVE", "EXTRA TIME"), selected state underlines, and active pitch telemetry. Used with severe restraint to maintain editorial poise.
- **Editorial Muted Slate (`#8892b0`, `#495670`):** Used for localized kick-off times, broadcast licensing subtexts, and secondary club fixtures, dissolving non-critical data into the background canvas.

## Typography

The typographic hierarchy creates tension between the classical grace of editorial serif script and the hyper-precise geometry of modern sans-serif data.

### EB Garamond (Display & Narrative Titles)
- Set exclusively in its classical italic style for the primary brand mark *Bhora*, featured match marquee heads, and long-form matchday essays.
- Evokes vintage broadsheets, historic match programmes, and archival sporting records.

### Manrope (Fixtures, Metadata, & Action Prompts)
- Set in light (`300`) and medium (`500`) weights with deliberate, generous letter spacing (`0.08em` to `0.2em`) across all caps or uppercase formulations.
- Replaces standard icons and visual badges: a status indicator is never an icon, but a finely spaced textual beacon (e.g., `84' · LIVE` or `20:00 LOCAL`).

## Layout & Spacing

The layout treats the mobile viewport as a continuous, breathable scroll canvas. 

- **Vertical Cadence:** A strict 8px rhythmic interval with expansive negative space. Content blocks are distanced by `space-lg` and `space-xl` to prevent visual compression and permit breathing room between fixtures.
- **Margins & Safe Zones:** A standard `margin` of `1.5rem` holds content away from device bezels. No containers, side rails, or background panels interrupt the fluid transition from the canvas edge.
- **Alignment:** Fixture presentations align along a strict center or left-hand vertical baseline axis. Matchups feature club names separated by breathing space rather than standard "VS" badges or club crests.

## Elevation & Depth

This design system avoids skeuomorphic shadows, physical layers, cards, and artificial elevation tiers. 

- **Zero-Shadow Aesthetic:** Drop shadows, outer blurs, and surface-container elevations are omitted.
- **Atmospheric Depth:** Visual depth is achieved through textual opacity and luminance shifts against the `#0a192f` substrate:
  - **Foreground Focus:** 100% white opacity for active fixture and stream titles.
  - **Secondary Supporting Data:** 60% opacity (`#8892b0`) for venue information and localized time conversions.
  - **Background Elements:** 25% opacity for historical records, upcoming week numbers, and structural text dividing rules.
- **Invisible Architecture:** Boundaries between matches are indicated by vertical space or ultra-fine single-pixel rules rendered at 10% opacity white, maintaining a seamless canvas continuity.

## Shapes

The design system uses a sharp structural framework (`roundedness: 0`). 

- Because cards, pill buttons, floating action buttons, and boxed chip containers are eliminated, rounded corner geometry is obsolete.
- Visual elements, indicators, cursor underscores, and full-screen video viewports terminate at crisp, unrounded 90-degree perimeters.

## Components

Components are stripped of solid background fills, decorative frame borders, icons, and emojis. Every interaction is purely typographic and spatial.

### Buttons & Interactive Triggers
- **Primary Watch Action:** A transparent textual block featuring a generous tap target. The label (e.g., `ENTER STREAM`) is styled in spaced uppercase Manrope (`status-editorial`), accompanied solely by a subtle hairline baseline accent that animates in opacity upon press.
- **Secondary Prompts:** Presented as plain text with 70% opacity that shifts to 100% ivory under direct touch. Never use encapsulated pill shapes or filled button shapes.

### Match Fixture Rows
- **Composition:** Team names appear in `fixture-team` typography, placed on opposing sides of a breathable horizontal axis or vertically stacked with generous line height.
- **Time & Stream Trigger:** The kickoff time appears in `kickoff-time` with localized time conversions (e.g., `17:30 BST / 00:30 CST`) rendered in muted slate below the fixture, dissolving borders entirely.

### Lists & Navigation
- **Navigation:** Bottom navigation bars are replaced by a minimalist typographic header anchor or a discreet bottom text bar featuring single-word text triggers (`FIXTURES`, `ARCHIVE`, `ACCOUNT`) with zero icons.
- **Active Navigation Indicator:** Active tabs are indicated by a change from muted slate to parchment ivory, underlined by a 1px razor-sharp gold hairline rule (`#d4af37`).

### Selection Controls & Checkboxes
- **Selection States:** Standard checkbox squares and radio circles are replaced with typographic state indicators. Inactive states are displayed in low-opacity text (`OFF`), transforming into illuminated gold (`ON`) upon toggle.
- **Timezone Selector:** Interactive horizontal scroll of plain text region abbreviations (`GMT`, `CET`, `EST`, `JST`), where the active selection is signaled by weight change and full luminance.

### Inputs & Search
- **Input Fields:** Unadorned text entry lines marked solely by a subtle bottom hairline (`1px` at 15% opacity). Placeholder text sits in `Manrope` light italic without search glass iconography.