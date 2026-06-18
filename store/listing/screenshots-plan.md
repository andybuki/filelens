# Screenshots plan — v1.0

Capture on light theme only (FileLens is light-only at v1.0). System bars
should be the system default; status bar tint matches the brand band
(`#2E1065`) on screens that use the home header, otherwise the canvas
`#F7F7F8`.

- **Phone**: 1080 × 1920 portrait (Pixel 7 / 8 emulator is fine).
- **Tablet**: 1920 × 1200 landscape (Pixel Tablet / Tab S9 emulator).
- Use the bundled English locale; show readable file names (no test123.jpg).
- Set font scale to 1.0, display size to default.
- Hide the demo / first-run onboarding before capturing.

## Phone — 8 captures

Order matters — the first three carry the "inspect before opening" pitch.

1. **Home tab (default destination).** Brand header showing the lens
   wordmark, Categories grid (8 tiles: Images / Video / Audio / Docs /
   APKs / Archives / Downloads / Large files), Recent files row with 2–3
   real-looking entries, Favorites row with one bookmarked folder. This
   is the "first impression" screenshot.
2. **Inspector — Overview tab on a real PDF.** Pick something like
   `quarterly-report.pdf`. Must show the file icon tile, file
   name/size/date row, the trust chip (Medium), and the first
   "JavaScript inside PDF" risk card with the violet "Open safely" link.
3. **Inspector — Risks tab on a photo with GPS.** A holiday-style JPG
   from `/Pictures`. Risk cards: "GPS coordinates found" (danger) + "Camera serial"
   (info). Show the "Share cleaned copy" button at the bottom.
4. **Safe Share sheet.** Same photo from screenshot 3 with the Safe
   Share bottom sheet open: checked items "GPS", "Camera serial",
   "Thumbnail", green "Ready to share" footer.
5. **Archive browsing without extracting.** Tap into an `app-release.apk`
   so the file list shows `AndroidManifest.xml`, `classes.dex`,
   `resources.arsc`, `META-INF/`, `lib/arm64-v8a/`, etc. — proves "browse
   without extracting".
6. **Secret scanner result.** A `config.json` or `.env` from a project
   folder, Structure tab open, showing two redacted hits (API key, JWT)
   with the "Copy key name only" action visible.
7. **Find duplicates.** `/Pictures` analysed, "412 MB wasted" summary
   banner, three duplicate groups expanded, "Select old extras" pressed.
8. **Global search.** Top search field with the query `report` typed,
   results grouped by folder (Documents / Downloads), category icons
   visible on each row.

## Tablet — 4 captures (landscape)

9. **Dual-panel Commander.** `/Downloads` on the left, `/Documents` on
   the right, both in detail view, the violet active-panel divider on
   the right panel, F-key toolbar visible at the bottom (F2 Rename, F5
   Copy, F6 Move, F8 Delete).
10. **Inspector with file list (expanded layout).** Left = file list
    inside `/Pictures`; right = full inspector on a selected JPG, Risks
    tab active, trust ring + risk cards visible.
11. **Compare files — `app-release.apk` vs `app-release-v2.apk`.** Top
    banner "Different · 3 changes", left = old metadata, right = new
    metadata, diff highlights on versionCode and permissions.
12. **Folder dashboard.** "Analyze folder" on `/Documents`: stacked
    disk-usage bar by category, largest files panel, "Privacy summary"
    card with "2 files with GPS · 1 with author metadata · 3 possible
    secrets".

## Feature graphic

1024 × 500 — committed as `feature-graphic.svg` (with rendered
`feature-graphic.png`).

- Light canvas `#F7F7F8` with a soft violet wash toward the right.
- Left half: lens-mark badge, FileLens wordmark, tagline
  _"Peek inside before you open. Share without leaking."_, and three
  trust pills (Offline · No accounts · No tracking).
- Right half: three stylised phone mocks — Safe Share (back-left),
  Inspector overview with risk cards (front-centre), Trust score ring
  (back-right).

## Promo video script (30 s)

- 0–4 s: lens mark + tagline animate in on the light canvas.
- 4–10 s: long-press a vacation photo → "Share cleaned copy" → green "3
  risks stripped" banner.
- 10–18 s: open `report.pdf` → Risks tab → "JavaScript inside PDF" card
  with "Open safely" preview-only fallback.
- 18–25 s: Find duplicates on `/Pictures` → "412 MB wasted" → "Select
  old extras" → trash count animates.
- 25–30 s: outro card "Offline · On-device · No accounts" + app icon.

## Capture checklist

- [ ] Sample folder seeded with: 1 holiday photo with GPS, 1 invoice
      PDF (with JS via tooling), 1 `app-release.apk`, 1 `.env` with a
      fake API key, ~80 photos that include obvious duplicates.
- [ ] First-run onboarding completed once so subsequent launches go
      straight to Home.
- [ ] Light theme verified (Settings shows no theme switch — confirm
      no dark surfaces in screenshots).
- [ ] All 8 phone shots cropped to 1080 × 1920.
- [ ] All 4 tablet shots cropped to 1920 × 1200.

## Where to drop the files

Gradle Play Publisher reads numbered PNGs straight from the source set:

- Phone shots → `app/src/main/play/listings/en-US/graphics/phone-screenshots/1.png` … `8.png`
- Tablet shots → `app/src/main/play/listings/en-US/graphics/tablet-screenshots/1.png` … `4.png`

Filename = display order in the Play Console listing. No need to
duplicate per locale unless you want localised text in the shots —
non-en-US locales inherit en-US graphics automatically.

Commit them, push to `main`, and `.github/workflows/play-publish.yml`
runs `publishListing` to upload them to the Console.
