# Play Store assets

This directory holds the Play Store listing content for FileLens. It's the
single source of truth for what the Play Console shows users; the actual
screenshots and feature graphic are produced from the brief in
`listing/screenshots-plan.md`.

## Files

- `listing/title.txt` — app title (30 char limit).
- `listing/short-description.txt` — short description (80 char limit).
- `listing/full-description.txt` — full store description (4000 char limit).
- `listing/feature-graphic.svg` / `feature-graphic.png` — 1024 × 500 hero
  graphic on the light identity (lens mark + tagline + three device mocks).
- `listing/screenshots-plan.md` — capture brief for 8 phone + 4 tablet
  screenshots.
- `listing/i18n/<lang>/` — translated title / short / full description /
  release notes for each launch locale (es, pt-BR, fr, de, it, ru, uk,
  tr, pl, ja, ko, zh-CN, hi, ar).
- `release-notes/v0.1.0-beta.txt` — release notes for the first beta.
- `release-notes/v1.0.0.txt` — release notes for the v1.0 stable launch.

## Generating screenshots

Screenshots are not committed because they need real device or emulator output.
Capture them on a phone (1080×1920) and a tablet (1920×1200) following the
order in `screenshots-plan.md`, then upload via Play Console.

## Pre-flight checklist

- [ ] `app/build.gradle.kts` — versionCode/versionName bumped.
- [ ] Run `./gradlew :app:lintRelease` and resolve any blockers.
- [ ] Run `./gradlew :app:assembleRelease` to produce signed AAB.
- [ ] Confirm `app/src/main/AndroidManifest.xml` permissions list matches the
  data-safety form in Play Console (READ_EXTERNAL_STORAGE,
  MANAGE_EXTERNAL_STORAGE, POST_NOTIFICATIONS).
- [ ] Confirm no network or analytics SDKs were added since last audit.
- [ ] Upload AAB to internal testing track; verify install works.
- [ ] Promote to closed testing → open beta.
