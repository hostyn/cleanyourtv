# Design: Halo

The launcher's home screen is a dark room where the light falls only on what you are about to open.
Reference mockup: `design/home.html` (1920x1080 px; on a 1080p TV 1 dp = 2 px, so halve every px value).

## Principles

1. Three to six apps get you watching: they are the favourites row, large and first.
2. Focus is light. The focused tile lifts, grows, casts a long shadow, gets a sheen, and throws a pool of its
   own colour on the ground. Everything else sits in shadow. No borders, no glass highlight.
3. Nothing decorative: no pills with borders, no grey dock band, no kicker labels, no oversized app name.
4. Cheap on a 1 GB TV: gradients, alpha, elevation shadows and scale/translate only. No blur, no video.

## Colour

| Token | Value | Use |
|---|---|---|
| ground | `#0D0F12` → `#08090B` → `#060708` (top to bottom) | Screen background |
| raised | `#16181C` | Menus, input tile base |
| ink | `#F2F2F0` | Clock, focused labels, focused control chip |
| ink-2 | `#9EA3AA` | Date, section heading |
| ink-3 | `#5D6168` | Secondary text in menus |
| shade | black at 42% | Over every unfocused tile; 60% while focus is in the top bar |
| glow | the focused app's sampled colour at 16% | Radial pool centred under the focused tile |

## Type

Mona Sans (OFL), shipped subset and renamed **Halo Sans** (`res/font/halo_sans.ttf`, the OFL Reserved
Font Name forbids "Mona" on a modified copy). Weights 200-600 from one variable file.

| Role | Size | Weight | Tracking |
|---|---|---|---|
| Clock | 74sp | 200 | -0.03 |
| Date | 15sp | 400 | 0 |
| Section heading ("Apps") | 14sp | 500 | 0 |
| Name under focused tile | 13sp (favourites), 12sp (grid) | 500 | 0 |
| Control chip label, menu rows | 13sp | 500 / 400 | 0 |

Latin, Vietnamese and punctuation are in the font; other scripts fall back to the system font.

## Layout (dp, 960x540 screen)

- Side margins 60. Clock top 38, date 7 below it.
- Controls top-right at top 52: Inputs, network, Settings as bare 17dp line icons in 36dp targets.
- Favourites row (`dock_size`, 3-6, default 4) at top 228, full width, gap 24 (20 at 5-6). Tiles 16:9.
- "Apps" heading at 371; grid (`columns`, default 6) from 400, gap 16, rows 32 apart. The second grid row
  peeks at the bottom edge.
- Down into the grid scrolls the page so the heading sits near the top; clock, controls and favourites fade out.

## Components

- **Tile:** 16:9 banner, 8dp corners. Unfocused: shade overlay. Focused: scale 1.075 (favourites) / 1.11
  (grid), translate -3dp, elevation 12dp, sheen (white 12% → 0 at 30% of the height, top to bottom), name fades in 13dp below.
- **Input tile** (`input_tiles`): no banner; surface `#2B2F36` → `#1A1C21` → `#131519` at 150°, 1dp top
  highlight, a line icon (29dp favourites / 20dp grid) centred above the label. The name under it reads
  "Label · Port".
- **Control:** icon only; focused it becomes an ink chip (18dp radius) with the icon and its label in
  `#0B0C0E`, the label sliding open.
- **Inputs menu:** raised `#16181C`, 12dp radius, rows 34dp, focused row is an ink chip; right-aligned under
  the Inputs control.

## Motion

- Ease: exponential out (`cubic-bezier(.16, 1, .3, 1)`); focus 300ms, shade 260ms, glow 520ms, page 560ms.
- One moving thing per press: the focused tile and its light. Reduced motion: no glow travel, 120ms fades.
