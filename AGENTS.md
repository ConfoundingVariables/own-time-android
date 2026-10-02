# AGENTS.md

Native Android app: personal daily time budgets ("a chess clock for your day"). Kotlin, Jetpack Compose, Material Design 3.

## Rules

- Never name, reference, or link the iOS app this product's UX is modelled on, in code, docs, commits, or store copy.

## Development

- Build from `.scratch/android-v1-build/spec.md`; work its tickets in `issues/` in dependency order. One ticket lands as small Conventional Commits (`feat(core): …`) pushed to `main` only after its checks pass.
- Follow pstack's development guidance (`poteto-mode` and the `principle-*` skills) to reach v1 with the least code: name the data shape first, keep rules pure in `:core`, tests assert observable behaviour against literal values, run `deslop` and `no-comments` before committing, prove it works on the real artifact.
- Check command: `./gradlew assembleDebug testDebugUnitTest` builds the debug APK and runs every JVM, Robolectric, and Roborazzi test. Screenshots are recorded to `app/build/screenshots/` for review, not compared. `./gradlew assembleRelease` (debug-signed) produces the installable APK.
- Device-only behaviour (alarms, notifications, permissions, share sheet) can't run in an orb; list each such check in its ticket for the Android runner.

## Agent skills

Skills in `.agents/skills/` are not committed. Reinstall from `mattpocock/skills`, `cursor/plugins` (`pstack` and `cursor-team-kit/deslop`) when missing.

### Issue tracker

Issues, specs, and wayfinding maps live as local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
