# FileLens Commander — Implementation Roadmap

## Vision

Smart File Commander for Android: browse files, peek inside, understand before opening.

Combines file manager + quick preview + metadata inspector + archive explorer + code outline + security/privacy hints.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  Jetpack Compose + Adaptive Layout (phone/tab)   │
├─────────────────────────────────────────────────┤
│                ViewModel Layer                   │
│  FileListVM · InspectorVM · CompareVM            │
├─────────────────────────────────────────────────┤
│               Domain / Use Cases                 │
│  BrowseFiles · InspectFile · DetectType          │
│  ExtractMetadata · AssessRisks · CompareFiles    │
├─────────────────────────────────────────────────┤
│              Virtual File System                 │
│  VirtualFileNode interface                       │
│  ├── LocalFileNode (real FS via SAF)             │
│  ├── ZipEntryNode                                │
│  ├── ApkEntryNode                                │
│  ├── OfficeEntryNode (DOCX/XLSX/PPTX)           │
│  └── NestedArchiveNode                           │
├─────────────────────────────────────────────────┤
│            Analysis / Inspectors                 │
│  MagicBytesDetector · ExifExtractor              │
│  ZipAnalyzer · ApkAnalyzer · JsonTreeParser      │
│  XmlParser · PdfAnalyzer · BinaryInspector       │
│  RiskScanner · CodeOutlineParser                 │
├─────────────────────────────────────────────────┤
│              Platform / Storage                  │
│  Android SAF · MediaStore · ContentResolver      │
└─────────────────────────────────────────────────┘
```

**Tech stack:**
- Kotlin, min SDK 26 (Android 8.0)
- Jetpack Compose + Material 3
- Compose Adaptive Layout (window size classes for phone/tablet/DeX)
- Kotlin Coroutines + Flow
- Hilt for DI
- Coil for image loading
- No Room needed initially (no persistent state beyond preferences)

---

## Phase 1: Foundation (Weeks 1–3) ✅ DONE

**Goal:** Browsable file list + basic file info panel. Walking skeleton.

### 1.1 Project Setup (Week 1) ✅
- [x] Android project with Compose, Material 3, Hilt
- [x] Package structure (ui/viewmodel/domain/vfs/inspector/detector/di)
- [x] Navigation setup (Compose Navigation)
- [x] Adaptive layout scaffold: single-pane (phone) / dual-pane (tablet)
- [x] Theme + typography + color tokens

### 1.2 File Browsing (Week 2) ✅
- [x] `VirtualFileNode` interface
- [x] `LocalFileNode` — real filesystem
- [x] Storage permission handling (MANAGE_EXTERNAL_STORAGE)
- [x] File list UI with LazyColumn
- [x] Folder navigation (back stack)
- [x] Sort options: name, size, date, type
- [x] File type icons (by extension + magic bytes)
- [x] Pull-to-refresh (PullToRefreshBox)

### 1.3 Basic Inspector Panel (Week 3) ✅
- [x] Inspector scaffold: tabs for Overview / Preview / Structure / Metadata / Risks
- [x] Overview tab: file name, type, size, dates, path, permissions
- [x] Phone: bottom sheet on file tap
- [x] Tablet: right panel always visible
- [x] "Open with" action via Android Intent

**Milestone:** User can browse folders, tap a file, see basic info.

---

## Phase 2: Type Detection + Core Inspectors (Weeks 4–6) ✅ DONE

### 2.1 Magic Bytes Detection (Week 4) ✅
- [x] `MagicBytesDetector` — 30+ signatures
- [x] Mismatch detection (extension vs magic)
- [x] `FileTypeResult`: detected type, confidence, mismatch flag
- [x] Color-coded type badges in file list

### 2.2 Image Inspector ✅
- [x] Preview tab: Coil
- [x] EXIF extraction
- [x] Metadata tab: camera, resolution, GPS, date, orientation
- [x] Risk: "Contains GPS location" badge
- [x] Action: "View on map" for GPS coords

### 2.3 Text & Code Inspector ✅
- [x] Preview tab with syntax highlighting (Kotlin/Java/Python/JS/TS, JSON/XML/YAML/Markdown)
- [x] Encoding detection
- [x] Line/word/char count
- [x] Code outline (regex-based, 10+ languages)

### 2.4 JSON & XML Inspector ✅
- [x] JSON tree, stats, secrets detection
- [x] XML tree, DTD/XXE risk
- [x] Pretty print / raw toggle (TextContent.rawContent)
- [x] XML pretty-printer (indent-based)

**Milestone:** User taps a photo → sees GPS warning. Taps JSON → sees tree. Taps unknown.dat → app detects it's actually a SQLite database.

---

## Phase 3: Archive Explorer (Weeks 7–9) ✅ DONE

### 3.1 ZIP Navigation ✅
- [x] `ZipEntryNode` implementing `VirtualFileNode`
- [x] Browse ZIP contents (FileListViewModel navigates into archives)
- [x] Entry metadata, ZIP overview stats
- [x] Zip bomb detection
- [x] Nested archive recursion (`NestedArchiveNode` extracts inner ZIPs to cache)
- [x] Preview files inside ZIP via streaming UI (tap archive → browse contents)

### 3.2 APK Inspector ✅
- [x] PackageManager-based APK analysis
- [x] Package name, version, min/target SDK
- [x] Permission list with risk highlighting
- [x] Debug/backup flag risks
- [x] App icon extraction in UI (PreviewContent.ImageContent cached PNG)
- [x] DEX file stats (method/class counts, multidex warning)

### 3.3 Office Document Explorer ✅
- [x] DOCX/XLSX/PPTX via OOXML ZIP
- [x] Macros, tracked changes, hidden sheets detection
- [x] Embedded objects, external links

**Milestone:** User "enters" a ZIP like a folder. Taps an APK → sees all permissions with risk flags. Opens DOCX → sees hidden tracked changes warning.

---

## Phase 4: Commander Mode (Weeks 10–12) 🟡 PARTIAL

### 4.1 Dual-Panel UI ✅
- [x] Adaptive layout: compact/expanded WindowSizeClass
- [x] Inspector ↔ Files toggle (FilterChip in expanded layout)
- [x] Breadcrumb navigation (compressed path display)
- [x] Active panel indicator (colored divider)
- [x] True dual file panels via `hiltViewModel(key = "rightPanel")`

### 4.2 File Operations ✅
- [x] Multi-select with long-press
- [x] Copy/Move/Delete/Rename/Create folder
- [x] Undo via TrashUseCase (Ctrl+Z restores last delete)
- [x] Soft delete to `.trash/` with JSONL index
- [x] Long-running progress notification

### 4.3 Keyboard Shortcuts ✅
- [x] F2 Rename, F3 View, F5 Copy, F6 Move, F7 New folder, F8 Delete
- [x] Ctrl+A select all, Ctrl+R refresh, Ctrl+Z undo, Backspace go up
- [x] Tab consumed (panel switch)

**Milestone:** User connects keyboard to tablet, navigates files Norton Commander-style with F-keys.

---

## Phase 5: Advanced Inspectors (Weeks 13–15) ✅ DONE

### 5.1 PDF Inspector ✅
- [x] Page count via PdfRenderer
- [x] Encrypted detection
- [x] Author, title, subject, creator, producer, dates
- [x] JavaScript / Launch action / EmbeddedFile / AcroForm detection
- [x] Thumbnail rendering (PdfRenderer + cached PNG, max 1024px)

### 5.2 Binary & Hex Inspector ✅
- [x] Hex view (offset + hex + ASCII)
- [x] String extraction
- [x] Entropy analysis
- [x] ELF header parsing (class, endianness, ABI, type, machine)
- [x] DEX header parsing (version, strings, types, methods)
- [x] SQLite inspector (tables, rows, columns, schema, WAL risk)

### 5.3 Media Inspector ✅
- [x] Audio: format, duration, bitrate via MediaMetadataRetriever
- [x] Audio: ID3-style tags (title, artist, album, etc.)
- [x] Video: resolution, duration, frame count, rotation
- [x] Location/author risk surfacing
- [x] Font inspector (TTF/OTF/WOFF format, name table, sample text)
- [x] Album art / video thumbnail display in UI

**Milestone:** Full inspector coverage for all common file types.

---

## Phase 6: Risk Scanner + Safe Share ✅ DONE

### 6.1 Risk Scanner ✅
- [x] Unified RiskScanner across all inspectors
- [x] Severity levels: info / warning / danger
- [x] Color-coded type badges in file list
- [x] Batch folder scan via `BatchScanUseCase` Flow + dialog progress

### 6.2 Safe Share ✅
- [x] SafeShareSheet UI shows pre-share risk summary
- [x] CleanCopyUseCase — strip EXIF GPS/personal data, strip OOXML metadata
- [x] Share cleaned copy via ACTION_SEND + FileProvider

**Milestone:** User selects photo → sees "Contains GPS" → strips metadata → shares clean copy.

---

## Phase 7.0: File Intelligence & Explainability

**Goal:** Turn FileLens from a set of inspectors into an assistant-like file commander that explains what was found, why it matters, and what the user can safely do next.

### 7.0.1 Explainable Risk Cards ✅
- [x] Risk cards with: what was found / why it matters / recommended action
- [x] Human-readable explanations for extension mismatch, GPS EXIF, PDF JavaScript, PDF launch actions, embedded files, macros, tracked changes, hidden sheets, external links, zip bombs, nested archives, APK debug builds, APK backup flag, suspicious permissions, SQLite WAL files, high entropy binaries
- [x] Risk detail screen with technical evidence: file path, detector name, matched rule, confidence
- [x] "Ignore this risk" action per file/session
- [x] "Learn more" microcopy for common file risks without requiring network access

### 7.0.2 File Trust Score ✅
- [x] Local trust score: trusted / low / medium / high risk
- [x] Score inputs: magic-vs-extension match, known file type, active content, private metadata, archive nesting, encrypted content, suspicious strings, APK permissions, Office macros, PDF actions
- [x] Show top 3 reasons that influenced the score
- [x] Keep score heuristic-only: explicitly not antivirus / not malware detection

### 7.0.3 Open Safely / Open As... ✅
- [x] "Open safely" action group: preview only, inspect metadata, browse archive without extracting, extract selected files only, share cleaned copy
- [x] "Open as..." override: Text, Binary/Hex, Archive, SQLite, JSON, XML, Image, APK, Office ZIP, PDF
- [x] Manual re-run of a selected inspector when automatic detection is wrong or ambiguous
- [x] Preserve original detected type and user-selected type in the report

### 7.0.4 File Provenance & Context ✅
- [x] Detect likely source from path/provider: Downloads, Camera, Screenshots, WhatsApp, Telegram, Signal, browser, Bluetooth, cloud provider, app-specific folders
- [x] Add provenance hints to the Overview tab
- [x] Combine provenance with risk messages, e.g. "APK from Downloads requests SMS permissions"
- [x] Show source confidence and avoid claiming certainty when only path heuristics are available

### 7.0.5 Cross-Format Secret Scanner ✅
- [x] Dedicated secret scan across text, code, JSON, XML, YAML, TOML, INI, `.env`, `.properties`, logs, ZIP entries, APK strings
- [x] Detect API keys, JWTs, private keys, tokens, passwords, database URLs, cloud credentials, Authorization headers
- [x] Redact secret values by default in UI
- [x] Provide "copy key name only" and "open containing file" actions
- [x] Add Safe Share warning when possible secrets are selected for sharing

### 7.0.6 Config File Inspector ✅
- [x] Inspect `.env`, `.properties`, `.ini`, `.yaml`, `.yml`, `.toml`, `.conf`, `.cfg`
- [x] Show key/value count, duplicate keys, empty values, URLs, ports, database settings, environment/profile names
- [x] Detect common frameworks: Spring Boot, Android Gradle, Docker Compose, Kubernetes, GitHub Actions, npm/yarn/pnpm, Python project config
- [x] Risk warnings for secrets, production endpoints, insecure protocols, debug flags

### 7.0.7 Log File Analyzer ✅
- [x] Detect log files by extension and content patterns
- [x] Show line count, time range, log levels, top repeated errors/warnings
- [x] Extract stack traces and group similar errors
- [x] Detect HTTP status codes, slow requests, exception names, package/class names
- [x] Search and filter by level, time range, keyword, exception

### 7.0.8 Search Inside Structured Files ✅
- [x] Search inside ZIP/APK/OOXML entries without full extraction
- [x] Search inside JSON keys/values, XML tags/attributes/text, CSV cells, SQLite schema/table names, PDF text layer, DOCX/PPTX text, code symbols
- [x] Search result grouping by file, entry path, match type, and risk relevance
- [x] Support "search selected folder/archive" as a batch operation

### 7.0.9 Batch Privacy Audit ✅
- [x] Folder-level privacy audit: photos with GPS, Office author metadata, hidden Excel sheets, tracked changes, PDF embedded files, secrets in configs/logs, archives with risky entries
- [x] Batch Safe Share: create cleaned copies for supported formats and package them into a new ZIP
- [x] Review screen: safe files / risky files / unsupported files
- [x] Allow sharing only cleaned files or only files without warnings

### 7.0.10 Inspection Reports ✅
- [x] Export current file report as Markdown
- [x] Export machine-readable report as JSON
- [x] Share report via Android share sheet
- [x] Report sections: overview, detected type, metadata, structure, risks, trust score, provenance, recommended actions
- [x] Batch scan report for folders and archives

### 7.0.11 Folder Intelligence Dashboard ✅
- [x] Folder summary: file count, total size, largest files, newest files, unknown files, risky files, archives, media, documents, code files
- [x] Privacy summary: files with GPS, author metadata, possible secrets, hidden content
- [x] Quick filters: risky, unknown, large, recent, duplicate candidates, contains private metadata
- [x] Entry point from commander panel context menu: "Analyze folder"

### 7.0.12 Deeper APK / Office Intelligence ✅
- [x] APK: certificate details, signature scheme, native libraries by ABI, exported components, deep links, network security config, cleartext traffic flag, hardcoded URLs from strings
- [x] APK: tracker/library hints from package/class names with local rules only
- [x] DOCX: comments, revisions count, embedded image count, external image links, custom properties, template path
- [x] XLSX: very hidden sheets, formula count, external workbook links, defined names, suspicious formulas
- [x] PPTX: speaker notes, hidden slides, embedded media, external media links

**Milestone:** User selects a suspicious file or folder and FileLens explains what is inside, why it may matter, where it likely came from, and what safe action is recommended.

---

## Phase 7: Compare Mode (Weeks 18–19)

### 7.1 File Compare (Week 18)
- [x] Select two files → compare
- [x] Checksum comparison (MD5, SHA-256)
- [x] Metadata diff (side by side)
- [x] Text diff (line-by-line, colored)
- [x] JSON/XML structural diff
- [x] Image metadata diff

### 7.2 Folder Compare (Week 19)
- [x] Compare two folders
- [x] Files only in left / only in right / in both
- [x] Changed files (by size, date, checksum)
- [x] Same content, different names detection
- [x] Sync actions: copy missing, overwrite changed

---

## Phase 8: Polish & Release (Weeks 20–22)

### 8.1 UX Polish (Week 20)
- [x] Animations and transitions
- [x] Empty states with illustrations
- [x] Error handling with friendly messages
- [x] Loading skeletons
- [x] Search within current folder
- [x] Search within file content (text files)
- [x] Recent files / frequent folders
- [x] Bookmarks / favorites

### 8.2 Settings & Preferences (Week 21)
- [x] Theme: light / dark / system
- [x] Default panel mode (inspector / dual files)
- [x] Default sort order
- [x] Show hidden files toggle
- [x] File size units (binary / decimal)
- [x] Keyboard shortcut customization
- [x] About screen with open source licenses

### 8.3 Testing & Release (Week 22)
- [x] Unit tests for detectors, parsers, VFS
- [x] Integration tests for inspectors
- [ ] UI tests for critical flows
- [ ] Performance profiling (large folders, big archives)
- [x] Memory leak testing
- [x] Accessibility audit (TalkBack, font scaling)
- [x] Play Store listing: screenshots, description, feature graphic
- [ ] Beta release

---

## Post-MVP Ideas (Future)

- [ ] Cloud storage browsing (Google Drive, Dropbox via SAF)
- [x] FTP/SFTP/SMB remote file browsing
- [x] Batch rename with patterns
- [x] File hash verification (download integrity)
- [x] Duplicate file finder
- [x] Disk usage visualization (treemap)
- [ ] Plugin system for custom inspectors
- [ ] Root mode for system files
- [x] Backup/restore support
- [x] RAR, 7Z archive navigation (TAR/TAR.GZ done)
- [x] EPUB inspector (TOC, chapters, cover)
- [x] Database browser (SQLite tables, queries)
- [ ] AI-powered file description ("This looks like a bank statement PDF")

---

## Priority Matrix

| Feature | User Value | Effort | Priority |
|---------|-----------|--------|----------|
| File browsing | Must have | Medium | P0 |
| Magic bytes detection | High | Low | P0 |
| Inspector panel | High | Medium | P0 |
| Image + EXIF | High | Low | P0 |
| JSON/XML tree | High | Low | P0 |
| ZIP navigation | Very High | Medium | P0 |
| APK inspector | Very High | Medium | P1 |
| Text/code preview | Medium | Low | P1 |
| Risk badges | High | Low | P1 |
| Office explorer | High | Medium | P1 |
| Dual-panel mode | Medium | High | P2 |
| File operations | Medium | High | P2 |
| Keyboard shortcuts | Niche/High | Medium | P2 |
| PDF inspector | Medium | Medium | P2 |
| Safe Share | High | Medium | P2 |
| Explainable risks | Very High | Medium | P1 |
| Trust score | High | Medium | P1 |
| Open safely / Open as | High | Medium | P1 |
| Cross-format search | Very High | High | P2 |
| Secret scanner | Very High | Medium | P1 |
| Config/log inspectors | High | Medium | P2 |
| Batch privacy audit | Very High | High | P2 |
| Inspection reports | Medium | Low | P2 |
| Folder intelligence dashboard | High | Medium | P2 |
| Deeper APK/Office intelligence | High | Medium | P2 |
| Compare mode | Medium | High | P3 |
| Binary/hex view | Niche | Medium | P3 |

---

## Key Technical Decisions

1. **SAF vs MANAGE_EXTERNAL_STORAGE**: Start with SAF tree picker for broad compatibility. Add MANAGE_EXTERNAL_STORAGE as optional for power users (requires Play Store justification).

2. **Archive parsing**: Use `java.util.zip` for ZIP. For APK manifest, use Android's built-in `XmlPullParser` on binary XML or a lightweight library. Avoid heavy dependencies.

3. **Compose vs Views**: Full Compose. No XML layouts. Compose handles adaptive layouts well with `WindowSizeClass`.

4. **No native code initially**: Pure Kotlin. Add native (Rust/C) only if performance requires it (e.g., hex view for large files, entropy calculation).

5. **Streaming over loading**: Never load full files into memory. Stream and parse incrementally. Critical for large archives and limited mobile RAM.

6. **Offline-first**: No network required. No accounts. No analytics beyond basic crash reporting. Privacy-focused positioning matches the app's ethos.

---

## Phase 9: Pre-Launch — 14-Day Release Plan

**Goal:** ship a polished v1.0 that survives first-impression scrutiny against established competitors. Tasks below are derived from a competitive review of the leading Android file-manager apps, then ordered by impact × effort × token cost so heavy work (localization, theme migration, visuals) starts on Day 1 and small polish lands near the end.

### 9.0 Competitive Reference

Reviewed direct and adjacent competitors to anchor expectations:

| App | What users expect from it | Where FileLens already wins | What we must match |
|-----|---------------------------|------------------------------|--------------------|
| Solid Explorer | Paid, premium polish, dual-pane, cloud, vault | Inspector depth, trust score, risk cards | Visual polish, onboarding, smooth animations |
| X-plore | Dual-pane, hex viewer, SMB/FTP, deep tools | Better inspector tabs, secret scanner, OOXML/PDF intel | App-manager, cloud accounts UI |
| Files by Google | Clean UI, junk cleaner, share nearby, multi-lingual | Privacy depth, archive browsing without extracting | Localization breadth, onboarding clarity, empty-state polish |
| Total Commander / MiXplorer | Power-user features, customization, plugins | Modern Compose UI, explainable risks | Customization surface, plugin-like "Open as…" |
| ZArchiver | Best-in-class archive UX | Inspect-before-extract, nested archives | RAR/7z extraction parity (post-launch) |
| Cx File Explorer | Friendly defaults, recents/favorites | Trust + provenance + safe share | Smooth first-run, "Recent" home tab |
| FX File Explorer | Network features, themes | Inspector and report exports | Theming consistency (light only now) |

### 9.1 Killer Features Users Expect (Gap Analysis)

What every category leader has that FileLens must ship at v1.0:

- **Onboarding flow** — 3–4 slides with value proposition, permission rationale, sample inspection. _(Missing.)_
- **Polished light theme everywhere** — no dark remnants on inspector, dialogs, sheets. _(Partially done; audit needed.)_
- **Localization** — competitors ship 20+ languages. _(English only.)_
- **Home / Quick-access tab** — Recent · Favorites · Categories (Images / Videos / Docs / APKs / Archives / Downloads). _(Recent/Bookmarks exist; surface them as a home screen.)_
- **Storage / category overview** — visual storage usage with category breakdown. _(Treemap exists; needs friendlier entry point.)_
- **Hidden vault / locked folder** — biometric- or PIN-protected area for sensitive files. _(Missing — high-trust feature.)_
- **Search across device** — fast indexed search by name, type, content. _(Within-folder exists; needs global search.)_
- **App manager** — list installed apps, extract APKs, share. _(Partial via APK inspector — add direct app list.)_
- **Per-session bin / recycle UI** — accessible from a top-level entry, not just inspector. _(.trash exists; expose it.)_
- **Trust indicators on the store page** — "No ads · No accounts · No tracking · Open about what we collect" — repeated in app About screen.

**Identified killer differentiators (already in FileLens — must be front-and-centre in store assets and onboarding):**

1. **Inspect before opening** — the unique positioning. Show this in slide 1 of onboarding and screenshot 1.
2. **Trust score with explainable risk cards** — no competitor surfaces this. Lead screenshot.
3. **Safe Share** — strip GPS / author / OOXML metadata. Use as the demo flow in onboarding.
4. **Secret scanner across configs/logs/archives** — developer audience hook.
5. **Browse archives & APKs without extracting** — ZArchiver-grade but with risk context.
6. **Offline · No accounts · No analytics** — repeat in every store paragraph and About screen.

### 9.2 14-Day Plan

Prioritised so heavy / high-token tasks land early and risk is front-loaded; final days are reserved for polish, store assets, and beta verification. Each day lists a primary deliverable; tasks tagged **[parallel]** can run alongside the main thread without blocking.

#### Week 1 — Foundations (Design, Theme, Localization)

- [x] **Day 1 — Design direction lock-in**
  - [x] Delete dark-only assumptions from `README.md` design system; rewrite as light-first.
  - [x] Decide light palette: canvas `#F7F7F8`, surface `#FFFFFF`, surface-2 `#F0F1F4`, ink `#1A1B1F` / `#5A5E66` / `#9098A2`, keep violet accent `#7C3AED` (deeper for light contrast).
  - [x] Strip "theme" selector from settings (preserve preference key for migration only).
- [x] **Day 2 — New logo + brand mark**
  - [x] Redraw `assets/lens_mark.svg` and `assets/app_icon.svg` against the new light identity.
  - [x] Adaptive launcher icon (foreground + monochrome).
  - [x] Replace in-app brand chip and splash. _(No image-based brand chip or splash drawable existed; app uses `app_name` string + Material icons. Verified no `lens_mark`/`app_icon` references in Kotlin sources.)_
- [x] **Day 3 — Light-theme migration pass 1**
  - [x] Audit every Compose screen for hardcoded dark surfaces / colors. _(Only `FlNavy` deep-violet brand header band remains as an intentional surface; all other hardcoded `Color(0x…)` calls are category seeds, syntax-highlight tones, or ink/stroke — appropriate for light surfaces.)_
  - [x] Rebuild `ui/theme/Color.kt` and `Type.kt` against the new light tokens. _(Done in Day 1 commit `80344f8`; canvas `#F7F7F8`, surface `#FFFFFF`, surface-2 `#F0F1F4`, ink ramp `#1A1B1F / #5A5E66 / #9098A2 / #7B8089`, violet accent `#7C3AED`.)_
  - [x] Remove dark/system theme branches from `MainScreen.kt`, `SettingsScreen.kt`, bottom sheets, dialogs. _(Verified: zero `isSystemInDarkTheme` / `darkColorScheme` references in the codebase; `KEY_THEME` preference removed in v1.0 — see `SettingsRepository.kt:64`.)_
- [x] **Day 4 — Light-theme migration pass 2 (deep screens)**
  - [x] Inspector tabs (Overview / Preview / Structure / Metadata / Risks).
  - [x] Risk cards, trust chip, severity dots, category badges — verify contrast on light.
  - [x] Compare mode, batch scan dialog, safe-share sheet, batch privacy audit review.
  - [x] Hex view, code view, tree view — re-tune syntax colors for light background.
- [x] **Day 5 — Localization framework**
  - [x] Migrate all hardcoded UI strings to `strings.xml`.
  - [x] Extract risk-card copy, provenance hints, trust tier labels, error messages.
  - [x] Set up `values-<lang>/` structure and lint rule for hardcoded strings.
  - [x] Plural / quantity strings via `<plurals>` for counts ("3 files", "%d risks").
- [x] **Day 6 — Translate to 12 launch languages**
  - [x] Languages: **en, es, pt-BR, fr, de, it, ru, uk, tr, pl, ja, ko, zh-CN, hi, ar (RTL)**.
  - [x] Use a single translation pass per language; review locale-sensitive items (units, dates, RTL layouts).
  - [x] Verify RTL mirroring on the inspector and commander panels.
- [x] **Day 7 — Onboarding flow** (new)
  - [x] 4-slide welcome: _Inspect before opening · Trust score & risk cards · Safe Share · Privacy promise_.
  - [x] Permission rationale screens for storage; defer optional permissions until used.
  - [ ] "Try it" demo: bundled sample photo with GPS → run Safe Share live.
  - [x] Show only on first launch + reachable from About.

#### Week 2 — Differentiators, Polish, Store

- [x] **Day 8 — Home / Quick-access tab**
  - [x] New top-level "Home" destination: Recent · Favorites · Categories grid (Images / Video / Audio / Docs / APKs / Archives / Downloads / Large files).
  - [x] Wire to existing Recent and Bookmarks state.
- [x] **Day 9 — Hidden vault (locked folder)**
  - [x] Biometric / device-credential gated vault directory under app-private storage.
  - [x] Move-in / move-out flow; vault items excluded from search & MediaStore.
  - [x] Surface as a Settings entry and on the Home tab.
- [ ] **Day 10 — Global search + App manager**
  - [ ] Global search across indexed folders (name + extension + type), cancellable.
  - [ ] App manager screen: list user/system apps, extract APK, share, jump to APK inspector.
  - [ ] Surface recycle bin (`.trash/`) as a top-level destination with restore-all.
- [ ] **Day 11 — Settings overhaul**
  - [ ] Compare with Solid Explorer / Files by Google / X-plore settings.
  - [ ] Remove: theme switcher.
  - [ ] Add: language override, default home tab, "ask before delete" toggle, vault PIN/biometric setup, search-included folders, telemetry opt-out (default off), root storage warning.
  - [ ] Reorder into sections: General · Files · Inspector · Safety · Keyboard · Backup · About.
  - [ ] About screen: privacy promise, no-ads/no-accounts/no-tracking statement, open licenses, version.
- [ ] **Day 12 — UX polish sweep**
  - [ ] Empty states with illustrations (no files, no risks, no results).
  - [ ] Loading skeletons for inspector and folder scan.
  - [ ] Friendly error toasts (permission denied, file gone, unsupported).
  - [ ] Smooth animations: sheet open 200ms, tab switch 120ms, list crossfade.
  - [ ] Accessibility pass: TalkBack labels on icons, 48dp touch targets, font scaling, contrast.
- [ ] **Day 13 — Store assets + marketing copy**
  - [ ] Feature graphic **1024 × 500** built from the new light identity (lens mark + tagline _"Peek inside before you open."_ + 3 device mocks of inspector / trust score / safe share).
  - [ ] 8 phone screenshots, 4 tablet screenshots (Home · Inspector overview · Risk cards · Trust score · Safe Share · Commander dual-pane · Compare · Search).
  - [ ] Rewrite store copy in English: **title** (≤30 chars), **short description** (≤80 chars), **full description** (≤4000 chars) — lead with "Inspect before opening", end with privacy promise.
  - [ ] Translate store copy into the 12 launch languages.
  - [ ] Release notes for v1.0.0.
- [ ] **Day 14 — Beta, fixes, ship**
  - [ ] Closed beta to 10–20 testers across phone / tablet / DeX.
  - [ ] Triage P0/P1 bugs only; defer everything else.
  - [ ] Verify install size, cold-start time, large-folder scroll, big-archive open, RTL layout, font scaling, TalkBack.
  - [ ] Tag `v1.0.0`, upload AAB, submit for review.

### 9.3 Effort × Impact × Token-Cost Matrix

| Task | Effort | Impact | Token-cost (Claude-assisted dev) | Day |
|------|--------|--------|----------------------------------|-----|
| Localization framework + 12 langs | High | Very High | High (many files) | 5–6 |
| Light-theme migration | High | Very High | Medium | 3–4 |
| New logo + brand | Low | High | Low | 2 |
| Onboarding flow | Medium | Very High | Medium | 7 |
| Hidden vault | Medium | High | Medium | 9 |
| Home / Quick-access tab | Medium | High | Low | 8 |
| Global search + App manager | Medium | High | Medium | 10 |
| Settings overhaul | Low | Medium | Low | 11 |
| UX polish sweep | Medium | High | Medium | 12 |
| Store assets + copy + i18n copy | Medium | Very High | Low | 13 |
| Beta + fixes | Medium | Critical | Medium | 14 |

### 9.4 Out-of-Scope for v1.0 (post-launch)

- RAR / 7z **extraction** (browsing already covered).
- Cloud accounts (Drive, Dropbox, OneDrive) — SAF gives us reach without account UI.
- AI file description ("This looks like a bank statement") — keep as differentiated v1.1 feature.
- Plugin system for custom inspectors.
- Root-mode browsing.

### 9.5 Definition of Done for v1.0

- Light theme only; no dark surfaces visible on any screen, dialog, sheet, or empty state.
- All UI strings localized to 12+ languages; RTL verified in Arabic.
- First launch shows onboarding; permission rationale is inline and skippable.
- Trust score, risk cards, and Safe Share are reachable within 2 taps from the file list.
- Home tab is the default destination; Recent / Favorites / Categories populated.
- Store listing: new logo, 1024×500 feature graphic, 8+ screenshots, copy translated to the launch languages.
- Closed-beta sign-off on at least 5 device profiles (phone, large phone, tablet, foldable, DeX).
- Crash-free sessions ≥ 99% on beta build.

