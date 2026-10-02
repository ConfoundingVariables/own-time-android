# 02: Create a priority and see it on the ring

**What to build:** the user taps Add, fills in colour, name, and one daily budget for every day, saves, and sees the priority on Today's ring and chips with the Σ total; the ring shows remaining and spent parts in the right order.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Editor bottom sheet: 8-colour palette harmonized to the dynamic scheme, name, budget slider 15m–8h with presets 30m/1h/2h/4h, alarm info line; Save disabled without a name
- [ ] Saving appends `PriorityCreated`; `fold` + `today` produce the row, filtered and sorted (remaining desc, then budget desc, then name)
- [ ] Ring on a surface-container tone: remaining parts clockwise from 12 in sort order, spent parts after them in reverse order; centre shows total remaining with "Still yours"; Σ total under the ring
- [ ] Chips in ring order with name and remaining; trailing "Add" chip; duration format "45m" / "2.5h", remaining rounded up
- [ ] TalkBack descriptions on chips, segments, and the centre summary
- [ ] Domain tests: sorting and ties, ring order, duration formatting, rounding
- [ ] Screenshots: Today with 4 priorities (light, dark), editor sheet
