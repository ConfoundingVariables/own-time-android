# 02: Create a priority and see it on the ring

**What to build:** the user taps Add, fills in colour, name, and one daily budget for every day, saves, and sees the priority on Today's ring and chips with the Σ total; the ring shows remaining and spent parts in the right order.

**Blocked by:** 01

**Status:** done

- [x] Editor bottom sheet: 8-colour palette harmonized to the dynamic scheme, name, budget slider 15m–8h with presets 30m/1h/2h/4h, alarm info line; Save disabled without a name
- [x] Saving appends `PriorityCreated`; `fold` + `today` produce the row, filtered and sorted (remaining desc, then budget desc, then name)
- [x] Ring on a surface-container tone: remaining parts clockwise from 12 in sort order, spent parts after them in reverse order; centre shows total remaining with "Still yours"; Σ total under the ring
- [x] Chips in ring order with name and remaining; trailing "Add" chip; duration format "45m" / "2.5h", remaining rounded up
- [x] TalkBack descriptions on chips, segments, and the centre summary
- [x] Domain tests: sorting and ties, ring order, duration formatting, rounding
- [x] Screenshots: Today with 4 priorities (light, dark), editor sheet

## Verification

- Domain tests cover sorting and ties, ring order, and compact duration formatting and rounding.
- Roborazzi: `today-four-light` / `today-four-dark` (4 priorities, live sort), `editor`.

## Defaults I took

- The editor creates a priority with a same-every-day budget; `PriorityCreated` is always followed by `ScheduleSet` from today, so budgets have one lookup path.
- Spent parts sink into the ring surface at a fixed blend; segment gaps are 1.2 degrees.

## On the Android runner

- Install and check the palette blends with your wallpaper; the reference app's five colours are kept plus Teal, Plum, Sky.
