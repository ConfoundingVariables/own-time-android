# Today screen and ring prototype

Type: prototype
Status: resolved
Blocked by: 03

## Question

How should the Today screen look and behave on Android? Build a throwaway Compose prototype (or a web stand-in if no device is available) of the ring and list with fake time: segment ordering, remaining vs spent tints, centre states (idle, running, overtime), the running indicator, live re-sorting, and the start/stop/log affordances, overtime display ("12m over"), the weekly budget schedule editor, and where 0-budget priorities (hidden from Today) are managed. React to it and lock the behaviour.

## Answer

Prototype (three variants, fake clock): branch `prototype/today-screen`, file `.scratch/android-v1/prototypes/today-screen.html`. Open it in a browser; `?variant=B` is the winner.

### Decided by the user

- **Primary layout: variant B, "ring hero".** The big ring is the screen; chips below it start and stop priorities.
- **Variant C (list-first cards with progress bars, mini ring, Stop FAB) is a post-v1 alternate view** the user can choose. Not built in v1.
- **Long-press** a chip or ring colour opens a bottom sheet with Log time, Edit, Delete. Onboarding must teach long-press.
- **Bottom card when nothing is running:** "Resume {last priority}" with its remaining time, plus Log time and Edit for it.

### Today screen behaviour (validated in the prototype)

- App bar: "Today" with the date; Export and an overflow menu (Manage priorities, Export history, About).
- Ring on a dark circle. Full circle = total budget for today. Remaining parts clockwise from 12 o'clock in sort order (bright), then spent parts in reverse order (dark tint), so each priority's spent part mirrors its remaining part. Overtime is not drawn in the ring.
- Centre: idle shows total remaining with "Still yours" (or "Nothing planned today"); running shows the priority name in its colour, its remaining time, and a stop button; in overtime it shows "+12m" and "Overtime".
- Running indicator: a white inner arc spanning the running priority's remaining segment, with a tick where it ends.
- Caption under the ring: "Tap a colour to start it · Σ {total}".
- Chips in the same order as the ring: colour dot, name, remaining (or "+overtime"); the running chip is filled with its colour. A trailing "+ Add" chip.
- Tap a chip or ring segment: start it, or stop it if it is running. Starting switches from the running priority.
- Bottom card while running: name, LIVE badge, "{remaining} left · of {budget} today" (or "{overtime} over"), Log time and Edit.
- Sort order (ring and chips): remaining, largest first; ties by budget, then name. Re-sorts live.
- 0-budget priorities are not on Today; they live in overflow › Manage priorities, which lists every priority with its weekly budget schedule and a "Not today" badge.
- Durations: under an hour as minutes ("45m"); an hour or more as decimal hours ("2.5h"); remaining rounds up so a countdown never shows 0 early.

### Editor and log sheet (validated)

- Editor: colour (5 swatches), name, daily budget with a "Same every day" (slider 15m to 8h, presets 30m/1h/2h/4h) or "Per weekday" (seven rows, ±15m steps, "Off") switch, alarm info line, wrap-up notification (Off/5/10/15 min), hide from lock screen, and Delete (dialog: "Delete, keep history" / "Erase everything" / Cancel).
- Log time: Today / Yesterday switch, signed slider −4h to +4h in 5-minute steps, presets ±15m/30m/1h, help text explaining that negatives can't take the day below zero.
