# Remaining v1 defaults

Type: grilling
Status: resolved
Blocked by: 05, 06, 07, 08

## Question

Which leftover fog items must be settled before the spec, and what are their v1 defaults?

## Answer

Agent-proposed defaults; the user accepted these and every other "agent-decided, open to veto" section across the map's tickets as the v1 baseline.

- **Empty state:** with no priorities, the ring shows an empty track with "Nothing planned today" and a prominent "Add your first priority" button. With priorities but none budgeted today, the same message plus a link to Manage priorities.
- **Onboarding** (from "Alerts and permissions UX"): 3 short screens before the first Today screen: what the ring means, tap to start / long-press for more, then "Allow alerts". Skippable; the "Alarms are off" banner covers anyone who skips.
- **Accessibility:** colour is never the only signal (chips and the bottom card always show names); every chip and ring segment has a TalkBack description ("Deep work, running, 2.5 hours left of 4 hours"); the ring's centre is announced as a summary; text scales with the system font size without clipping; the 8 palette colours are picked to differ in lightness, not just hue; motion respects "remove animations".
- **About sheet** (overflow › About): app version, a short privacy statement (no account, no server, no analytics, data stays on the device), what the export contains, and open-source licences.
- **Export:** overflow › Export history and the app bar's export button both produce the SQLite snapshot and open the Android share sheet; the temporary file is deleted on next launch.
- **Working name and application id (user-chosen):** "Day Budget", `com.addedomarus.daybudget`. Placeholder until naming; final before the first Play upload.
- **Verification:** the orb can build the app and run JVM unit tests (JDK 17 + Android SDK command-line tools, no emulator). On-device checks (alarms, notifications, full-screen intent, export) wait for an Android runner or device.
