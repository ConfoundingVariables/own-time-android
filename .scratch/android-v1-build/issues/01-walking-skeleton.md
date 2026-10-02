# 01: Walking skeleton

**What to build:** the app installs and opens to an empty Today screen ("Nothing planned today" with "Add your first priority") in the wallpaper's colours, backed by an empty events database, with the build, unit tests, and screenshot tests running in the orb.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Two modules, `:core` and `:app`, on the stack in the spec (Kotlin 2.4.20, AGP 9.4.0, SDK 31–36, Compose BOM 2026.09.00, Material 3 1.4.0, Room + KSP, Hilt, Navigation Compose); application id `com.addedomarus.daybudget`, name "Day Budget"
- [ ] The orb can build: JDK 17 and Android SDK command-line tools installed by a committed setup script
- [ ] One command builds the debug APK and runs all JVM tests and screenshot tests
- [ ] Room database with the append-only `events` table and `meta` table (device id generated once); a trigger rejects updates and deletes
- [ ] Today shows the empty state with a dynamic-colour Material 3 theme in light and dark
- [ ] Roborazzi screenshot of the empty Today in light and dark, inspected
- [ ] A Robolectric test appends an event and reads it back, and proves an update is rejected
