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

## Phase 1: Foundation (Weeks 1–3)

**Goal:** Browsable file list + basic file info panel. Walking skeleton.

### 1.1 Project Setup (Week 1)
- [ ] Android project with Compose, Material 3, Hilt
- [ ] Package structure:
  ```
  com.filelens.commander
  ├── ui/           # Compose screens, components
  ├── viewmodel/    # ViewModels
  ├── domain/       # Use cases, models
  ├── vfs/          # Virtual file system
  ├── inspector/    # File analysis modules
  ├── detector/     # Type detection (magic bytes)
  └── di/           # Hilt modules
  ```
- [ ] Navigation setup (Compose Navigation)
- [ ] Adaptive layout scaffold: single-pane (phone) / dual-pane (tablet)
- [ ] Theme + typography + color tokens

### 1.2 File Browsing (Week 2)
- [ ] `VirtualFileNode` interface
  ```kotlin
  interface VirtualFileNode {
      val name: String
      val path: String
      val type: NodeType  // FILE, DIRECTORY, ARCHIVE_ENTRY
      val size: Long?
      val lastModified: Long?
      suspend fun children(): List<VirtualFileNode>
      suspend fun openStream(): InputStream?
  }
  ```
- [ ] `LocalFileNode` — real filesystem via SAF / MediaStore
- [ ] Storage permission handling (MANAGE_EXTERNAL_STORAGE or SAF tree picker)
- [ ] File list UI with LazyColumn
- [ ] Folder navigation (back stack)
- [ ] Sort options: name, size, date, type
- [ ] File type icons (by extension, later by magic bytes)
- [ ] Pull-to-refresh

### 1.3 Basic Inspector Panel (Week 3)
- [ ] Inspector scaffold: tabs for Overview / Preview / Structure / Metadata / Risks
- [ ] Overview tab: file name, type, size, dates, path, permissions
- [ ] Phone: bottom sheet on file tap
- [ ] Tablet: right panel always visible
- [ ] "Open with" action via Android Intent

**Milestone:** User can browse folders, tap a file, see basic info.

---

## Phase 2: Type Detection + Core Inspectors (Weeks 4–6)

**Goal:** Magic bytes detection. Inspectors for images, text, JSON, XML.

### 2.1 Magic Bytes Detection (Week 4)
- [ ] `MagicBytesDetector` — read first 16–64 bytes, match signatures
- [ ] Signatures: ZIP, PDF, PNG, JPEG, GIF, BMP, WEBP, SQLite, DEX, ELF, OOXML, OLE2, GZIP, BZIP2, 7Z, RAR, TAR, WASM, MP3, MP4, OGG, FLAC, WAV
- [ ] Mismatch detection: extension says X, magic says Y → risk badge
- [ ] `FileTypeResult`: detected type, confidence, mismatch flag
- [ ] Color-coded type badges in file list

### 2.2 Image Inspector (Week 4–5)
- [ ] Preview tab: image thumbnail via Coil
- [ ] EXIF extraction (ExifInterface from AndroidX)
- [ ] Metadata tab: camera, resolution, GPS, date, orientation
- [ ] Risk: "Contains GPS location" badge
- [ ] Action: "View on map" for GPS coords

### 2.3 Text & Code Inspector (Week 5)
- [ ] Preview tab: syntax-highlighted text (first N lines)
- [ ] Encoding detection (UTF-8, UTF-16, ASCII, etc.)
- [ ] Line/word/char count
- [ ] Code outline for common languages: functions, classes (regex-based, not full AST)
- [ ] Languages: Kotlin, Java, Python, JavaScript, C/C++, Rust, Go, XML, YAML, TOML, INI

### 2.4 JSON & XML Inspector (Week 6)
- [ ] JSON: parse → tree view (expandable nodes)
- [ ] JSON: key count, nesting depth, array sizes, possible secrets detection
- [ ] XML: parse → tree view with namespaces
- [ ] XML: node count, attributes, schema detection
- [ ] Pretty print / raw toggle

**Milestone:** User taps a photo → sees GPS warning. Taps JSON → sees tree. Taps unknown.dat → app detects it's actually a SQLite database.

---

## Phase 3: Archive Explorer (Weeks 7–9)

**Goal:** Navigate ZIP/APK/DOCX as folders without extraction. The hero feature.

### 3.1 ZIP Navigation (Week 7)
- [ ] `ZipEntryNode` implementing `VirtualFileNode`
- [ ] Browse ZIP contents as folder tree
- [ ] Entry metadata: compressed/uncompressed size, method, CRC
- [ ] ZIP overview: file count, folder count, total size, compression ratio
- [ ] Encrypted ZIP detection
- [ ] Nested archive detection + limited preview
- [ ] Preview files inside ZIP (text, images, JSON) via streaming

### 3.2 APK Inspector (Week 8)
- [ ] APK = ZIP, reuse ZIP navigation
- [ ] Parse AndroidManifest.xml (binary XML → readable)
- [ ] Extract: package name, version, min/target SDK
- [ ] Permission list with risk highlighting (camera, location, SMS, etc.)
- [ ] Activity/service/receiver/provider counts
- [ ] App icon extraction
- [ ] Signature info (V1/V2/V3)
- [ ] DEX file stats: class count, method count
- [ ] Risk badges: dangerous permissions, debug flag, backup flag

### 3.3 Office Document Explorer (Week 9)
- [ ] DOCX/XLSX/PPTX = ZIP with OOXML
- [ ] Navigate internal structure as folder tree
- [ ] DOCX: extract text preview, page/word count, author, tracked changes detection
- [ ] XLSX: sheet names, row/column counts, formula detection
- [ ] PPTX: slide count, embedded media count
- [ ] Risk: macros detection, embedded OLE objects, external links
- [ ] Hidden content detection (hidden sheets, tracked changes, comments)

**Milestone:** User "enters" a ZIP like a folder. Taps an APK → sees all permissions with risk flags. Opens DOCX → sees hidden tracked changes warning.

---

## Phase 4: Commander Mode (Weeks 10–12)

**Goal:** Dual-panel file management. Copy/move/delete. Keyboard support.

### 4.1 Dual-Panel UI (Week 10)
- [ ] Left panel: source folder
- [ ] Right panel: switchable between second folder / inspector
- [ ] Panel mode toggle: Inspector ↔ Files ↔ Preview ↔ Compare
- [ ] Active panel indicator
- [ ] Breadcrumb navigation per panel
- [ ] Tab to switch panels

### 4.2 File Operations (Week 11)
- [ ] Multi-select with checkboxes or long-press
- [ ] Copy files (with progress)
- [ ] Move files
- [ ] Delete (with confirmation)
- [ ] Rename
- [ ] Create folder
- [ ] File operation queue with progress notification
- [ ] Undo for delete (move to app trash first)

### 4.3 Keyboard Shortcuts — DeX / Chromebook (Week 12)
- [ ] F3 View, F4 Edit (external), F5 Copy, F6 Move, F7 New folder, F8 Delete
- [ ] F10 Menu
- [ ] Space to select, Tab to switch panel
- [ ] Ctrl+A select all, Ctrl+C/V copy-paste
- [ ] Arrow keys navigation
- [ ] Enter to open, Backspace to go up
- [ ] Keyboard shortcut overlay (Ctrl+K or ?)
- [ ] Hardware keyboard detection → show shortcut hints

**Milestone:** User connects keyboard to tablet, navigates files Norton Commander-style with F-keys.

---

## Phase 5: Advanced Inspectors (Weeks 13–15)

**Goal:** PDF, binary, SQLite, media inspectors.

### 5.1 PDF Inspector (Week 13)
- [ ] Page count, author, title, creation date
- [ ] Encrypted/password-protected detection
- [ ] Embedded files/attachments listing
- [ ] JavaScript detection (risk)
- [ ] Form fields detection
- [ ] Text extraction preview (first page)
- [ ] Thumbnail of first page

### 5.2 Binary & Hex Inspector (Week 14)
- [ ] Hex view: offset + hex + ASCII
- [ ] String extraction (printable sequences)
- [ ] Entropy analysis (detect encryption/compression)
- [ ] ELF/DEX header parsing
- [ ] SQLite detection → show tables, row counts, schema

### 5.3 Media Inspector (Week 15)
- [ ] Audio: format, duration, bitrate, channels, sample rate
- [ ] Audio: ID3 tags, album art
- [ ] Video: format, resolution, duration, codec, frame rate
- [ ] Video: thumbnail extraction
- [ ] Font files: preview with sample text, glyph count

**Milestone:** Full inspector coverage for all common file types.

---

## Phase 6: Risk Scanner + Safe Share (Weeks 16–17)

**Goal:** Privacy-focused features that differentiate from competitors.

### 6.1 Risk Scanner (Week 16)
- [ ] Unified risk engine scanning for:
  - GPS/location in EXIF
  - Author/company in Office metadata
  - Macros in Office documents
  - Hidden sheets/tracked changes
  - Executable content in archives
  - Suspicious permissions in APK
  - Extension/content mismatch
  - Possible secrets/API keys in text/JSON
  - Embedded scripts in PDF
- [ ] Risk severity levels: info / warning / danger
- [ ] Risk badges in file list (colored dots)
- [ ] Batch scan: scan entire folder

### 6.2 Safe Share (Week 17)
- [ ] "Check before sharing" action
- [ ] Summary of what the file may reveal
- [ ] "Create cleaned copy" for images (strip EXIF GPS, author)
- [ ] "Create cleaned copy" for Office (strip author, comments, tracked changes)
- [ ] Share cleaned copy via Android share sheet

**Milestone:** User selects photo → sees "Contains GPS" → strips metadata → shares clean copy.

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
