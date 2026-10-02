# 09: Export and About

**What to build:** the user exports their complete history as a SQLite file through the share sheet, with readable tables they can query right away, and reads an About sheet explaining privacy, the export, the version, and licences.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Export from the app bar and the overflow menu: `VACUUM INTO` snapshot, readable `priorities`, `sessions` (split by day), and `daily_totals` (day, priority, budget, spent, remaining, overtime) written from `fold`, schema version set, `PRAGMA integrity_check` passes, shared via `FileProvider`; snapshot deleted on next launch
- [ ] Export schema documented in the repository
- [ ] About sheet: version, privacy statement (no account, no server, no analytics), what the export contains, open-source licences
- [ ] Robolectric test: export of a known event history opens, passes the integrity check, and its `daily_totals` equal `today` for the same events
- [ ] Screenshot: About sheet
- [ ] Device check listed for the Android runner: share sheet opens with the file
