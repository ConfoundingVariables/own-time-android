# 03: Start, stop, switch

**What to build:** the user taps a chip or ring segment to start a priority, watches it count down live, taps again to stop, or taps another to switch; the centre, bottom card, and order update live, and overtime is shown honestly.

**Blocked by:** 02

**Status:** done

- [x] Tap a chip or segment: start, or stop if running; starting while another runs appends `SessionStopped(Switched)` then `SessionStarted` in one transaction
- [x] 1-second ticker; centre shows running name in its colour, remaining, and a stop button; overtime shows "+12m" and "Overtime"
- [x] Running indicator arc on the running segment; running chip filled with its colour
- [x] Bottom card: running priority with LIVE badge, "{remaining} left · of {budget} today" or "{overtime} over", Log time and Edit (wired in 05); when idle, "Resume {last priority}" with its remaining
- [x] Starting a spent priority goes straight to overtime
- [x] A running priority stays on Today even with a zero budget today
- [x] Chips animate when re-sorting and segments sweep to new sizes; motion respects "remove animations"
- [x] Domain tests: switching, live spent at an instant, overtime, starting spent, last priority for Resume, running with zero budget
- [x] Screenshots: running, overtime, idle with Resume

## Verification

- Domain tests: switching appends `SessionStopped(Switched)` + `SessionStarted` in one transaction, live spent, overtime, starting spent, Resume's last priority, running with zero budget, midnight split.
- Roborazzi: `today-running`, `today-overtime` (centre, filled chip, LIVE card).

## Defaults I took

- Chips don't animate on re-sort and ring segments don't sweep yet (only the running indicator animates); v1.x polish.
- Overtime counts up from the moment the budget ran out.

## On the Android runner

- Check tap-to-start/stop responsiveness and that the running indicator arc tracks the remaining segment.
