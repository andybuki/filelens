# FileLens Design System

FileLens is a **smart file commander for Android**. You browse files, **peek inside before opening**, inspect metadata, explore archives, and — the heart of the product — **understand the risk and trustworthiness of a file** before you act on it. It is privacy-first, offline, on-device. No accounts, no network, no analytics beyond crash reporting.

> Vision (from the repo): _"Browse files, peek inside, understand before opening."_ It combines a file manager + quick preview + metadata inspector + archive explorer + code outline + security/privacy hints.

The product flow is: **file list → tabbed inspector**. Tap a file and the inspector opens with tabs for **Overview · Preview · Structure · Metadata · Risks**, plus a local **Trust Score**, explainable **Risk Cards** (what was found / why it matters / recommended action), **Provenance** (where the file likely came from), and **Safe Share** (strip GPS/author metadata before sharing).

This design system is a **fresh visual language** for FileLens — **light-only**, near-white canvas, with a single confident violet accent and a **risk-&-trust visualization layer** as its signature. It deliberately departs from the stock Material 3 / Google-blue look the codebase shipped, to feel like a focused, IDE-grade tool rather than a generic file manager. The previous dark variant was removed in v1.0 after light tested better in usability sessions and produced a cleaner first impression on the Play Store screenshots.

## Sources

- **Codebase:** `andybuki/filelens` on GitHub (private) — Kotlin + Jetpack Compose + Material 3, min SDK 26. Files read for domain + behavior:
  - `ROADMAP.md` — vision, architecture, the full inspector / risk / trust / provenance feature set (Phases 1–8 + 7.0 "File Intelligence").
  - `domain/model/InspectorResult.kt` — the data model: `TrustScore` (levels Trusted/Low/Medium/High + 0–100 score + top reasons), `RiskItem` (severity info/warning/danger × category privacy/security/active-content/integrity/metadata/encryption), `Provenance`, `SecretMatch`, `PreviewContent` (Text/Image/Hex/Tree).
  - `domain/usecase/TrustScoreUseCase.kt` — scoring rules (start 100; danger −25, warning −10, info −3; tiers ≥90 / ≥70 / ≥40 / else).
  - `ui/screens/MainScreen.kt` — adaptive layout: compact (phone: list + bottom-sheet inspector + commander toolbar) vs expanded (tablet: dual file panels or list + side inspector).
  - `ui/components/FileListItem.kt`, `ui/theme/{Color,Type}.kt` — original Material palette + type (replaced here).
- **Original theme (replaced):** Google-blue `#1A73E8` on white, Material 3 defaults, Roboto inherited. We keep Roboto, drop the blue, swap to violet on a near-white canvas.
- **Chosen direction:** Light-first FileLens identity — violet accent on warm-grey canvas, IDE-grade information density without the dark surface. The earlier Darcula exploration is preserved in `FileLens Directions.html` for reference only.

## Products represented

- **FileLens Android app** — the only surface. Phone-first (single-pane: file list + bottom-sheet inspector + commander action bar). Tablet/landscape uses the same components in a two-pane layout.

## Index

- `README.md` — this file. Read top-to-bottom.
- `SKILL.md` — Agent-Skill stub; invoke to generate FileLens-branded artifacts.
- `colors_and_type.css` — all design tokens (CSS variables): neutrals, accent, risk, trust ramp, category seeds, type scale, spacing, radii, shadows, motion. Light is `:root`; there is no dark variant.
- `assets/` — `lens_mark.svg` (monochrome brand glyph, `currentColor`), `app_icon.svg` (full-color rounded app icon).
- `fonts/` — _empty._ The app uses **Roboto** (UI) + **Roboto Mono** (data/code), loaded from Google Fonts; system fallbacks cover Roboto on Android natively. See VISUAL FOUNDATIONS.
- `preview/` — one HTML card per concept; registered to the Design System tab.
- `ui_kits/android/` — hi-fi recreation of the app: components + `index.html` click-through.
- `FileLens Directions.html` — the original 3-direction exploration (kept for reference).

---

## CONTENT FUNDAMENTALS

**Voice:** Calm, precise, protective. FileLens is the friend who reads the fine print for you. It states what it found plainly, explains *why it matters* in one sentence, and tells you the *safe next step*. It never alarms for effect and never claims certainty it doesn't have.

**Casing:** Sentence case everywhere — titles, dialogs, body. Title Case only for multi-word primary buttons / tab labels where it reads as a proper control ("Open safely", "Safe Share", "Open with"). Tabs are single words in sentence case ("Overview", "Preview", "Structure", "Metadata", "Risks").

**Person:** Second person for the user's things ("your photo still has GPS location", "where this file came from"). First-person plural when the app acts on the user's behalf ("we'll strip the location before sharing"). The app is the subject only in the lens metaphor copy ("What the lens found").

**Tone markers:**
- **Explainable, three-beat risk copy:** _what was found_ → _why it matters_ → _what you can do_. E.g. "2 tracked changes present" / "Edits and reviewer comments are still stored in this document." / "Share a cleaned copy to remove them."
- **Honest about confidence:** provenance and detection are heuristic — say "likely from Downloads", never "from Downloads, guaranteed". Trust score is explicitly **"not antivirus, not malware detection."**
- **Concrete over abstract:** "Contains GPS location (48.85, 2.35)" beats "privacy concern".
- **Short:** one short sentence per idea. Metadata is key/value, not prose.

**Numbers, sizes, data:** Always **Roboto Mono** — file sizes ("1.8 MB"), hashes, line/byte counts, hex, IBAN/coords, trust score ("74 / 100"). Sizes use locale formatting; the app supports binary/decimal units.

**Risk severity language:** three words only — **Info** · **Warning** · **Danger**. Trust tiers: **Trusted · Low risk · Medium risk · High risk**.

**Emoji usage:** **None.** No emoji as icons or decoration. (The original repo had none meaningful either.) Use Lucide icons.

**Vibe in one line:** _A quiet, sharp instrument — it looks closely so you can decide._

---

## VISUAL FOUNDATIONS

**Palette DNA:** A **warm near-white canvas** layered with slightly cooler **panel** and **card** surfaces, hairline borders, and a single **violet** accent used only for interaction (primary actions, active tab/row, links, the lens mark). Color otherwise belongs to **meaning**: the risk semantics (danger/warning/info/safe), the trust ramp, and per-file-type category seeds. Decoration is never colored for its own sake.

- **Canvas `#F7F7F8`** — the app background.
- **Surface `#FFFFFF`** — cards, the inspector sheet, list background.
- **Surface-2 `#F0F1F4`** — raised rows, input fills, hovered surfaces.
- **Surface-3 `#E8EAEE`** — pressed wash, secondary chips.
- **Borders `#E3E5EA`** (hairline) / `#CBD0D8` (stronger) — 1px only.
- **Ink `#1A1B1F` / `#5A5E66` / `#9098A2`** — primary / secondary / tertiary text + icons.
- **Accent violet `#7C3AED`** — the only interaction color. `#8B5CF6` for active text; `#6D28D9` for pressed; `#EDE4FF` for selected-row wash.
- **Risk:** danger `#FF5C6C`, warning `#F5B642`, info `#4C8DFF`, safe `#36D399` — each with a ~14% tint for fills.
- **Trust ramp:** Trusted `#36D399` (≥90) · Low `#8BD64F` (≥70) · Medium `#F5B642` (≥40) · High `#FF5C6C` (<40).
- **Category seeds (16):** folder `#E3B341` · image `#4CC38A` · video `#FF6B6B` · audio `#FF8A3D` · document `#4C8DFF` · spreadsheet `#2EB872` · presentation `#FF7A45` · archive `#B07CF6` · code `#2DD4D4` · text `#8B98A8` · pdf `#F0616D` · apk `#3DDC84` · database `#C08457` · font `#EC6FA8` · executable `#9AA0AC` · unknown `#6E727C`. Used for the file-type icon, its tinted icon-tile, and the small uppercase ext badge.

**Type:** **Roboto** (400/500/700) for all UI; **Roboto Mono** (400/500/700) for data — sizes, hashes, hex, code, scores, coordinates. The original app inherited Roboto on Android; we keep it and add Roboto Mono for the inspector's data density. _Substitution flag: no font files ship in `fonts/` — both load from Google Fonts. If the team adopts bundled `.woff2`, drop them in `fonts/` and update `--fl-font-sans` / `--fl-font-mono`._

**Type scale** (see `colors_and_type.css`): Display 30/700 · H1 24/700 (screen title) · H2 19/700 (card/sheet title) · H3 16/600 (section header) · Body 15/400 · Small 13/500 (labels, buttons, metadata) · Caption 11/700 uppercase tracked (eyebrows) · Mono 13/500 (data) · Mono-sm 11.5.

**Spacing:** 4-based — 4, 8, 12, 16, 20, 24, 32, 40, 48, 64. Screen horizontal padding 16. Card padding 14–16. Row vertical padding 11–12. Gap between cards 12.

**Corner radii:** 6 (small badges), 8 (chips/tabs/ext badges), 11 (buttons, inputs, file rows), 14 (cards), 20 (bottom sheets, dialogs), 999 (pills, FAB). Comfortable but not pill-soft — it should read like a precise tool.

**Backgrounds:** Flat light. **No gradients** on content, **no patterns, no grain, no full-bleed photography.** The only gradient in the system is inside the app-icon asset. Illustrations appear only in empty/onboarding states — flat, duotone, violet+ink on white; use a sized placeholder rectangle if absent.

**Borders:** Hairlines only — `1px solid var(--fl-border)`. A card has **either** a border **or** a shadow, rarely both. Selection is shown with an `inset 2px` accent bar + a faint accent-tint wash, not a heavy outline.

**Shadows:** Minimal — elevation reads through hairline borders and a soft, low-contrast drop. `--sh-xs` resting, `--sh-sm` sheets/menus, `--sh-md` dialogs. The one colored shadow is `--sh-accent` under the primary FAB / primary button. No inner shadows.

**Animation:** Material-standard. 120ms micro (press), 200ms state, 300ms screen, 400ms for chart/gauge sweep. Easing `--ease-std` `cubic-bezier(0.4,0,0.2,1)`. Fades + small 8px vertical slides. **No bounces, no parallax.** The trust gauge / progress sweeps in once on appear (`--ease-out`).

**Hover / press states (mobile + preview):**
- Primary button **press:** fill → `--fl-accent-press`, scale(0.98).
- Row **press:** `--fl-surface-2` wash, no scale. Selected row: accent-tint wash + inset accent bar.
- Icon **press:** opacity 0.6.
- Preview/web row **hover:** `--fl-surface-2` tint.

**Transparency & blur:** Sparingly. Bottom-sheet scrim is 40% black. The app bar may use `backdrop-filter: blur(12px)` when content scrolls under it. Never translucent violet over content; tints are flat rgba.

**Imagery vibe:** Calm, bright, precise. The brand image is the **lens mark** (a violet magnifier ring + center dot + reticle ticks). No photography.

**Layout rules:**
- **App bar** (transparent over canvas): lens mark + "FileLens" wordmark + search + sort/filter icons.
- **Breadcrumb** row under the app bar (tertiary ink, chevron separators, current segment in secondary ink).
- **File list** fills the middle. Comfortable rows: tinted icon tile + name (Roboto) + mono subtitle (size · date) + ext badge + a small severity dot when the file carries risk.
- **Inspector** = bottom sheet on phone (grab handle, file header with trust chip, tab row, tab content), side panel on tablet.
- **Commander action bar** pinned at the very bottom when files are selected (View / Copy / Move / New folder / Rename / Delete), Norton-Commander-style; F-key hints on tablet.
- One **FAB**: Import / scan, violet, bottom-right, `--sh-accent`.

**Iconography philosophy:** See ICONOGRAPHY.

**Signature element — the Risk & Trust layer:**
- **Trust chip:** a pill with a colored dot + "Low risk · 74", colored by trust tier.
- **Severity dot:** an 8px dot on a file row when it carries a risk (danger/warning/info).
- **Risk card:** surface-2 card with a 3px severity-colored left border, a severity+category header ("Warning · Metadata"), a one-line finding, and "why it matters" subtext; expandable to evidence + recommended action.
- **Trust score:** "74 / 100" in mono + tier label; an optional thin linear or ring meter colored by tier. (Direction A keeps it as a compact chip + optional ring, not the big gauge.)
- These are the colorful moments in an otherwise restrained light UI.

---

## ICONOGRAPHY

The Compose codebase uses **Material Symbols** (`androidx.compose.material.icons`, filled set for file types). For HTML recreations we substitute **[Lucide](https://lucide.dev/)** (outline, 1.75px stroke, MIT, CDN). The clean outline weight suits the IDE-grade feel better than filled glyphs.

> ⚠️ **Substitution:** Material Symbols fonts are not shipped here; Lucide is the CDN stand-in. Folder/category icons in the live app are *filled* Material icons — in the kit we use Lucide outline and reserve `fill` only for the folder glyph so directories read as solid. Flag and swap if an official icon kit is adopted.

**Usage rules:**
- Stroke 1.75 (Lucide default). Don't mix filled + outline in the same context (folder is the sanctioned exception).
- Sizes: 28 (file-row icon tile glyph ~19 inside a 34 tile), 24 (app-bar + tab-bar), 20 (row-leading / inline), 16 (inside buttons + risk cards).
- Color inherits from text/category. Active = `--fl-accent-bright`; inactive = `--fl-ink-3`; category icons use their seed color.
- **No emoji. No unicode glyphs** (`✓`, `✕`) as icons — always a Lucide SVG.

**Brand glyph:** `assets/lens_mark.svg` — monochrome, uses `currentColor`; sits in a violet rounded tile in the app bar (18px glyph in a 30px tile). `assets/app_icon.svg` is the full-color launcher icon (gradient violet lens on a soft-white rounded-square — rebuilt on Day 2 of the v1.0 release plan). Use the lens mark at 16–48px; the app icon at 48px+.

**Category icons (Lucide):** folder→`folder` (filled) · image→`image` · video→`file-video`/`video` · audio→`file-audio`/`music` · document→`file-text` · spreadsheet→`sheet`/`table` · presentation→`presentation` · archive→`file-archive`/`archive` · code→`file-code`/`braces` · text→`file-text` · pdf→`file-text` (pdf-tinted) · apk→`package`/`smartphone` · database→`database` · font→`type` · executable→`terminal` · unknown→`file`.

**Action icons (Lucide):** search→`search` · lens/scan→`scan-search` · sort/filter→`sliders-horizontal` · import/upload→`upload` · share→`share-2` · clean-share→`shield-check` · delete→`trash-2` · rename→`pencil` · new-folder→`folder-plus` · copy→`copy` · move→`folder-input` · back/up→`corner-left-up` · more→`more-vertical` · ignore-risk→`eye-off` · flag→`flag` · open-with→`external-link` · map→`map-pin` · key/secret→`key-round`.

**Risk / trust icons:** danger→`shield-alert` · warning→`triangle-alert` · info→`info` · safe/trusted→`shield-check` · trust score→`gauge` · provenance→`map-pin` / source-specific.
