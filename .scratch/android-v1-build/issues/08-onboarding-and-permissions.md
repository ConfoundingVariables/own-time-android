# 08: Onboarding, permissions, and the "Alarms are off" banner

**What to build:** a new user sees three short onboarding pages (the ring, tap to start and long-press for more, allow alerts) and grants alert permissions once; anyone whose alarms can't ring sees a banner with a Fix button.

**Blocked by:** 06

**Status:** done

- [x] Three skippable onboarding pages before the first Today; shown once
- [x] "Allow alerts" requests notification permission and, when full-screen access isn't granted, opens its settings page
- [x] Permission state (notifications, full-screen intent, exact alarm) read on every foreground
- [x] Dismissible "Alarms are off" banner on Today with Fix opening the right settings page; editor alarm line says alarms are off and links there
- [x] Screenshots: the three onboarding pages, Today with the banner
- [x] Device checks listed for the Android runner

## Verification

- Roborazzi: `onboarding`, `today-alarms-off`.

## Defaults I took

- Onboarding is skipped with the Skip button or by starting Today; both set the same preference.

## On the Android runner

- First run shows onboarding, Allow alerts requests notification permission and points at the full-screen-intent page; skipping then shows the banner with Fix.
