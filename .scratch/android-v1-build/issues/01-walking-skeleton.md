# 01: Walking skeleton

**What to build:** the app installs and opens to an empty Today screen ("Nothing planned today" with "Add your first priority") in the wallpaper's colours, backed by an empty events database, with the build, unit tests, and screenshot tests running in the orb.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Two modules, `:core` and `:app`, on the stack in the spec (Kotlin 2.4.20, AGP 9.4.0, min SDK 31, target SDK 36, compile SDK 37, Compose BOM 2026.09.00, Material 3 1.4.0, Room + KSP, Hilt, Navigation Compose); application id `com.addedomarus.daybudget`, name "Day Budget"
- [x] The orb can build: JDK 17 and Android SDK command-line tools installed by a committed setup script
- [x] One command builds the debug APK and runs all JVM tests and screenshot tests
- [x] Room database with the append-only `events` table and `meta` table (device id generated once); a trigger rejects updates and deletes
- [x] Today shows the empty state with a dynamic-colour Material 3 theme in light and dark
- [x] Roborazzi screenshot of the empty Today in light and dark, inspected
- [x] A Robolectric test appends an event and reads it back, and proves an update is rejected

## Verification

- `./gradlew assembleDebug testDebugUnitTest` builds `app-debug.apk` (`com.addedomarus.daybudget`, label "Day Budget", minSdk 31, targetSdk 36) and runs `EventLogTest` (3 tests) and `TodayScreenshotTest` (2 tests); screenshots land in `app/build/screenshots/`.
- Mutation check: dropping the update trigger fails `updatesAndDeletesAreRejected`.

## On the Android runner

- Install the APK, launch it: Today shows "Nothing planned today" in the wallpaper's colours, light and dark.
- Relaunch: the database is created once (`meta.device_id` unchanged, via `adb shell run-as com.addedomarus.daybudget`).
