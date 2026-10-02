# 08: Onboarding, permissions, and the "Alarms are off" banner

**What to build:** a new user sees three short onboarding pages (the ring, tap to start and long-press for more, allow alerts) and grants alert permissions once; anyone whose alarms can't ring sees a banner with a Fix button.

**Blocked by:** 06

**Status:** ready-for-agent

- [ ] Three skippable onboarding pages before the first Today; shown once
- [ ] "Allow alerts" requests notification permission and, when full-screen access isn't granted, opens its settings page
- [ ] Permission state (notifications, full-screen intent, exact alarm) read on every foreground
- [ ] Dismissible "Alarms are off" banner on Today with Fix opening the right settings page; editor alarm line says alarms are off and links there
- [ ] Screenshots: the three onboarding pages, Today with the banner
- [ ] Device checks listed for the Android runner
