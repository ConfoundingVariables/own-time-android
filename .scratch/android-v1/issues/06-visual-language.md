# Visual language

Type: grilling
Status: resolved
Blocked by: 05

## Question

How does Material 3 apply here? Settle: dynamic (wallpaper) colour vs a fixed brand theme; whether priority colours are a fixed palette independent of the theme and how they get light/dark and remaining/spent tints; the always-dark ring card; typography and shape; motion for re-sorting and segment changes.

## Answer

### Decided by the user

- **Dynamic colour, fully:** the whole app takes its Material 3 colour scheme from the wallpaper.
- **Priority colours:** a fixed curated palette of about 8 colours, each harmonized toward the dynamic scheme so they fit any wallpaper while staying distinct and keeping their identity.
- **Ring background follows the theme:** a tonal surface from the dynamic scheme, light in light mode and dark in dark mode (not the always-dark card of the reference).

### Details (agent-decided, open to veto)

- Light/dark follows the system setting; no in-app theme switch in v1.
- No fallback scheme needed: minSdk is 31, so wallpaper colours are always available (see "Stack and architecture").
- Palette: Terracotta, Mustard, Raspberry, Sage, Slate (the reference five) plus Teal, Plum, Sky. Each is harmonized toward the scheme's primary colour and has light- and dark-theme tones. Spent parts are the priority colour blended toward the ring surface; the blend strength is tuned per theme so remaining vs spent stays obvious.
- Ring surface: a surface-container tone; centre text uses the on-surface colour, the running priority's name uses its own colour. Text on a filled (running) chip is chosen for contrast against that colour.
- Components: stock Material 3 (top app bar, chips, card for the bottom card, modal bottom sheets, dialogs). Material 3 Expressive vs stable is a stack question for "Stack and architecture".
- Typography: the default Material 3 type scale; large countdown numerals use tabular figures so they don't jitter.
- Motion: chips animate when they re-sort and ring segments sweep to their new sizes; all motion respects the system "remove animations" setting.
