# 09: Export and About

**What to build:** the user exports their complete history as a SQLite file through the share sheet, with readable tables they can query right away, and reads an About sheet explaining privacy, the export, the version, and licences.

**Blocked by:** 02

**Status:** done

- [x] Export from the app bar and the overflow menu: `VACUUM INTO` snapshot, readable `priorities`, `sessions` (split by day), and `daily_totals` (day, priority, budget, spent, remaining, overtime) written from `fold`, schema version set, `PRAGMA integrity_check` passes, shared via `FileProvider`; snapshot deleted on next launch
- [x] Export schema documented in the repository
- [x] About sheet: version, privacy statement (no account, no server, no analytics), what the export contains, open-source licences
- [x] Robolectric test: export of a known event history opens, passes the integrity check, and its `daily_totals` equal `today` for the same events
- [x] Screenshot: About sheet
- [x] Device check listed for the Android runner: share sheet opens with the file

## Verification

- Robolectric: a known history exports a file that passes integrity_check, whose sessions split by day and whose daily_totals match today's derivation.
- `docs/export-schema.md` documents the schema.

## Defaults I took

- Export file name is `day-budget-<epoch>.db`; the whole export cache is cleared on next launch.

## On the Android runner

- Export from the app bar and the overflow menu opens the Android share sheet with the SQLite file; it opens in a SQLite browser.
