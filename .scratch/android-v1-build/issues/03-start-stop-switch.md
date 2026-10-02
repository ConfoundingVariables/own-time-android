# 03: Start, stop, switch

**What to build:** the user taps a chip or ring segment to start a priority, watches it count down live, taps again to stop, or taps another to switch; the centre, bottom card, and order update live, and overtime is shown honestly.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Tap a chip or segment: start, or stop if running; starting while another runs appends `SessionStopped(Switched)` then `SessionStarted` in one transaction
- [ ] 1-second ticker; centre shows running name in its colour, remaining, and a stop button; overtime shows "+12m" and "Overtime"
- [ ] Running indicator arc on the running segment; running chip filled with its colour
- [ ] Bottom card: running priority with LIVE badge, "{remaining} left · of {budget} today" or "{overtime} over", Log time and Edit (wired in 05); when idle, "Resume {last priority}" with its remaining
- [ ] Starting a spent priority goes straight to overtime
- [ ] A running priority stays on Today even with a zero budget today
- [ ] Chips animate when re-sorting and segments sweep to new sizes; motion respects "remove animations"
- [ ] Domain tests: switching, live spent at an instant, overtime, starting spent, last priority for Resume, running with zero budget
- [ ] Screenshots: running, overtime, idle with Resume
