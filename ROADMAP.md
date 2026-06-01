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
- [ ] Long-running progress notification (operations are fast enough)

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
- [ ] Album art / video thumbnail display in UI

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

### 7.0.7 Log File Analyzer
- [ ] Detect log files by extension and content patterns
- [ ] Show line count, time range, log levels, top repeated errors/warnings
- [ ] Extract stack traces and group similar errors
- [ ] Detect HTTP status codes, slow requests, exception names, package/class names
- [ ] Search and filter by level, time range, keyword, exception

### 7.0.8 Search Inside Structured Files
- [ ] Search inside ZIP/APK/OOXML entries without full extraction
- [ ] Search inside JSON keys/values, XML tags/attributes/text, CSV cells, SQLite schema/table names, PDF text layer, DOCX/PPTX text, code symbols
- [ ] Search result grouping by file, entry path, match type, and risk relevance
- [ ] Support "search selected folder/archive" as a batch operation

### 7.0.9 Batch Privacy Audit
- [ ] Folder-level privacy audit: photos with GPS, Office author metadata, hidden Excel sheets, tracked changes, PDF embedded files, secrets in configs/logs, archives with risky entries
- [ ] Batch Safe Share: create cleaned copies for supported formats and package them into a new ZIP
- [ ] Review screen: safe files / risky files / unsupported files
- [ ] Allow sharing only cleaned files or only files without warnings

### 7.0.10 Inspection Reports
- [ ] Export current file report as Markdown
- [ ] Export machine-readable report as JSON
- [ ] Share report via Android share sheet
- [ ] Report sections: overview, detected type, metadata, structure, risks, trust score, provenance, recommended actions
- [ ] Batch scan report for folders and archives

### 7.0.11 Folder Intelligence Dashboard
- [ ] Folder summary: file count, total size, largest files, newest files, unknown files, risky files, archives, media, documents, code files
- [ ] Privacy summary: files with GPS, author metadata, possible secrets, hidden content
- [ ] Quick filters: risky, unknown, large, recent, duplicate candidates, contains private metadata
- [ ] Entry point from commander panel context menu: "Analyze folder"

### 7.0.12 Deeper APK / Office Intelligence
- [ ] APK: certificate details, signature scheme, native libraries by ABI, exported components, deep links, network security config, cleartext traffic flag, hardcoded URLs from strings
- [ ] APK: tracker/library hints from package/class names with local rules only
- [ ] DOCX: comments, revisions count, embedded image count, external image links, custom properties, template path
- [ ] XLSX: very hidden sheets, formula count, external workbook links, defined names, suspicious formulas
- [ ] PPTX: speaker notes, hidden slides, embedded media, external media links

**Milestone:** User selects a suspicious file or folder and FileLens explains what is inside, why it may matter, where it likely came from, and what safe action is recommended.

---

## Phase 7: Compare Mode (Weeks 18–19)

### 7.1 File Compare (Week 18)
- [ ] Select two files → compare
- [ ] Checksum comparison (MD5, SHA-256)
- [ ] Metadata diff (side by side)
- [ ] Text diff (line-by-line, colored)
- [ ] JSON/XML structural diff
- [ ] Image metadata diff

### 7.2 Folder Compare (Week 19)
- [ ] Compare two folders
- [ ] Files only in left / only in right / in both
- [ ] Changed files (by size, date, checksum)
- [ ] Same content, different names detection
- [ ] Sync actions: copy missing, overwrite changed

---

## Phase 8: Polish & Release (Weeks 20–22)

### 8.1 UX Polish (Week 20)
- [ ] Animations and transitions
- [ ] Empty states with illustrations
- [ ] Error handling with friendly messages
- [ ] Loading skeletons
- [ ] Search within current folder
- [ ] Search within file content (text files)
- [ ] Recent files / frequent folders
- [ ] Bookmarks / favorites

### 8.2 Settings & Preferences (Week 21)
- [ ] Theme: light / dark / system
- [ ] Default panel mode (inspector / dual files)
- [ ] Default sort order
- [ ] Show hidden files toggle
- [ ] File size units (binary / decimal)
- [ ] Keyboard shortcut customization
- [ ] About screen with open source licenses

### 8.3 Testing & Release (Week 22)
- [ ] Unit tests for detectors, parsers, VFS
- [ ] Integration tests for inspectors
- [ ] UI tests for critical flows
- [ ] Performance profiling (large folders, big archives)
- [ ] Memory leak testing
- [ ] Accessibility audit (TalkBack, font scaling)
- [ ] Play Store listing: screenshots, description, feature graphic
- [ ] Beta release

---

## Post-MVP Ideas (Future)

- [ ] Cloud storage browsing (Google Drive, Dropbox via SAF)
- [ ] FTP/SFTP/SMB remote file browsing
- [ ] Batch rename with patterns
- [ ] File hash verification (download integrity)
- [ ] Duplicate file finder
- [ ] Disk usage visualization (treemap)
- [ ] Plugin system for custom inspectors
- [ ] Root mode for system files
- [ ] Backup/restore support
- [ ] RAR, 7Z, TAR.GZ archive navigation
- [ ] EPUB inspector (TOC, chapters, cover)
- [ ] Database browser (SQLite tables, queries)
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
