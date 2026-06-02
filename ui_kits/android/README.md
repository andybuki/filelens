# FileLens — Android UI Kit

A hi-fi, interactive recreation of the FileLens Android app in the "Panel" direction (dark-first Darcula, violet accent, risk-&-trust signature). Open `index.html`.

## What it demonstrates

A real click-through, not a storybook:

- **File list** — comfortable rows: category icon tile, name, mono metadata, uppercase ext badge, a **severity dot** when a file carries risk, chevron for folders/archives.
- **Browse** — tap folders (Documents / Downloads) and *into archives* (archive.zip) like folders; breadcrumb + up button navigate back.
- **Search** (top-right) filters the current folder; **sort** segmented control (Name / Size / Date / Type).
- **Inspector bottom sheet** — tap a file → sheet rises with a **trust chip**, then tabs: **Overview** (provenance + trust ring + key/value), **Preview** (text / code with redacted secrets / image / tree), **Structure** (archive & OOXML trees), **Metadata** (EXIF, OOXML props, APK manifest…), **Risks** (explainable cards: *what → why → what to do*, with severity & category). Clean files show a "no risks" safe state.
- **Trust score** — circular ring + tier label, score 0–100 (Trusted / Low / Medium / High). Heuristic, on-device.
- **Selection / Commander mode** — long-press a file (click-and-hold in preview) to enter multi-select; the bottom **commander bar** appears (View / Copy / Move / Rename / Folder / Delete). Delete soft-removes with an undo toast.
- **Actions** — "Open safely", "Safe Share" (strips GPS/author metadata on risky files), "Open with".
- **FAB** — import / scan.

Try: `report.docx` (tracked changes), `photo.jpg` (GPS danger), `config.json` (live API key + prod DB url, Preview tab), `archive.zip` (browse into it), `invoice.pdf` (clean / Trusted).

## Files

- `index.html` — entry; loads fonts, tokens, kit styles, then the scripts below. Auto-scales the device to fit the viewport.
- `android-frame.jsx` — device bezel + status/nav bar (starter component; used in `dark` mode, no Material app bar — FileLens draws its own chrome).
- `icons.jsx` — inline Lucide-style icon set (stroke 1.75; folder is the one filled glyph). Exposes `window.Icon`.
- `data.jsx` — `window.DATA`: category map, trust tiers, the folder tree (`FILES`), and per-file inspector results (`INSPECT`) + `inspectFor()` fallback.
- `app.jsx` — components (`FileGlyph`, `TrustChip`, `TrustRing`, `RiskCard`, `FileRow`, `InspectorSheet`, `CommanderBar`) + the stateful `App`. Exposes `window.FileLensApp`.
- `kit.css` — all component styles, built on `../../colors_and_type.css` tokens.

## Notes / fidelity

- Visuals are a fresh design (the live app ships Material 3 / Google-blue); structure, tabs, trust/risk model, provenance and copy follow the real codebase (`InspectorResult.kt`, `TrustScoreUseCase.kt`, `MainScreen.kt`, `ROADMAP.md`).
- Icons are Lucide-style stand-ins for Material Symbols (see root README → ICONOGRAPHY).
- Fonts (Roboto / Roboto Mono) load from Google Fonts.
- This is a cosmetic recreation — file operations are simulated (toasts), not real.
