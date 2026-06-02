// FileLens kit — file system + inspector data
const CAT = {
  dir:   { icon: "folder",    col: "--cat-dir",     fill: true },
  doc:   { icon: "file-text", col: "--cat-doc" },
  archive:{ icon: "archive",  col: "--cat-archive" },
  image: { icon: "image",     col: "--cat-image" },
  json:  { icon: "braces",    col: "--cat-code" },
  pdf:   { icon: "file-text", col: "--cat-pdf" },
  apk:   { icon: "package",   col: "--cat-apk" },
  db:    { icon: "database",  col: "--cat-db" },
  text:  { icon: "file-text", col: "--cat-text" },
  audio: { icon: "music",     col: "--cat-audio" },
};

const TRUST = {
  trusted: { v: "--fl-trust-trusted", label: "Trusted",     icon: "shield-check" },
  low:     { v: "--fl-trust-low",     label: "Low risk",    icon: "shield-check" },
  medium:  { v: "--fl-trust-medium",  label: "Medium risk", icon: "triangle-alert" },
  high:    { v: "--fl-trust-high",    label: "High risk",   icon: "shield-alert" },
};

// Folder tree. key = path; value = array of file entries.
const FILES = {
  "DCIM": [
    { name: "Documents", cat: "dir", sub: "24 items", folder: "DCIM/Documents" },
    { name: "Downloads", cat: "dir", sub: "18 items", folder: "DCIM/Downloads" },
    { name: "report.docx",  cat: "doc",   sub: "1.8 MB · Mar 4",  ext: "DOCX", sev: "warning" },
    { name: "photo.jpg",    cat: "image", sub: "3.2 MB · Feb 28", ext: "JPG",  sev: "danger" },
    { name: "config.json",  cat: "json",  sub: "4 KB · Feb 26",   ext: "JSON", sev: "danger" },
    { name: "archive.zip",  cat: "archive", sub: "12.4 MB · Mar 2", ext: "ZIP", sev: "info", folder: "DCIM/archive.zip" },
    { name: "invoice.pdf",  cat: "pdf",   sub: "240 KB · Mar 1",  ext: "PDF" },
    { name: "app-release.apk", cat: "apk", sub: "18.6 MB · Feb 20", ext: "APK", sev: "danger" },
    { name: "voice-memo.m4a", cat: "audio", sub: "1.1 MB · Feb 19", ext: "M4A" },
  ],
  "DCIM/Documents": [
    { name: "resume.docx", cat: "doc", sub: "88 KB · Jan 12", ext: "DOCX" },
    { name: "budget.json", cat: "json", sub: "6 KB · Jan 30", ext: "JSON" },
    { name: "scan.pdf", cat: "pdf", sub: "1.2 MB · Feb 2", ext: "PDF" },
  ],
  "DCIM/Downloads": [
    { name: "setup.apk", cat: "apk", sub: "44 MB · Feb 22", ext: "APK", sev: "warning" },
    { name: "data.zip", cat: "archive", sub: "8 MB · Feb 21", ext: "ZIP", folder: "DCIM/Downloads/data.zip" },
    { name: "readme.txt", cat: "text", sub: "2 KB · Feb 18", ext: "TXT" },
  ],
  "DCIM/archive.zip": [
    { name: "manifest.json", cat: "json", sub: "1 KB", ext: "JSON" },
    { name: "inner.zip", cat: "archive", sub: "6.2 MB", ext: "ZIP", sev: "info" },
    { name: "logo.png", cat: "image", sub: "120 KB", ext: "PNG" },
  ],
  "DCIM/Downloads/data.zip": [
    { name: "export.csv", cat: "text", sub: "3 MB", ext: "CSV" },
  ],
};

// Inspector results keyed by file name (rich for hero files, generic otherwise).
const INSPECT = {
  "report.docx": {
    type: "Microsoft Word OOXML", trust: { score: 74, level: "low" },
    prov: { icon: "map-pin", label: "Likely from Downloads" },
    overview: [["Type", "DOCX · OOXML"], ["Size", "1.8 MB"], ["Modified", "Mar 4, 2025"], ["Detected", "DOCX (magic match)"], ["Path", "/DCIM/report.docx"]],
    metadata: [["Author", "J. Becker"], ["Last saved by", "M. Roth"], ["Company", "Acme GmbH"], ["Created", "2025-02-18"], ["Revisions", "7"], ["Words", "3,412"], ["Template", "Normal.dotm"]],
    structure: [["word/", 0, "dir"], ["document.xml", 1, "412 KB"], ["comments.xml", 1, "8 KB"], ["settings.xml", 1, "3 KB"], ["docProps/", 0, "dir"], ["core.xml", 1, "1 KB"], ["app.xml", 1, "1 KB"]],
    preview: { type: "text", body: "Q1 Performance Review — Confidential\n\nRevenue grew 14% QoQ, driven by the EMEA\nsegment. Tracked edits below reflect legal's\nredlines (still embedded)…" },
    risks: [
      { sev: "warning", cat: "Metadata", title: "2 tracked changes present", why: "Edits and reviewer comments are still stored in this document.", action: "Open safely to preview without exposing them, or share a cleaned copy." },
      { sev: "info", cat: "Metadata", title: "Author metadata present", why: "Author, company and editor names are embedded in document properties.", action: "Strip metadata before sharing externally." },
    ],
  },
  "photo.jpg": {
    type: "JPEG image", trust: { score: 58, level: "medium" },
    prov: { icon: "map-pin", label: "From Camera" },
    overview: [["Type", "JPEG"], ["Size", "3.2 MB"], ["Resolution", "4032 × 3024"], ["Taken", "Feb 28, 2025 14:22"], ["Path", "/DCIM/photo.jpg"]],
    metadata: [["Camera", "Apple iPhone 13"], ["Lens", "26 mm ƒ/1.6"], ["ISO", "64"], ["GPS", "48.8566, 2.3522"], ["Orientation", "6 (rotate 90°)"]],
    preview: { type: "image" },
    risks: [
      { sev: "danger", cat: "Privacy", title: "Contains GPS location", why: "This photo embeds where it was taken (48.86, 2.35) — about 30 m precision.", action: "Share a cleaned copy to remove the location before sending." },
      { sev: "info", cat: "Metadata", title: "Device & capture details", why: "Camera model, lens and timestamp are embedded in EXIF.", action: "Safe Share strips these automatically." },
    ],
  },
  "config.json": {
    type: "JSON · config", trust: { score: 52, level: "medium" }, secrets: 1,
    prov: { icon: "map-pin", label: "Likely from Downloads" },
    overview: [["Type", "JSON"], ["Size", "4 KB"], ["Keys", "14"], ["Max depth", "3"], ["Encoding", "UTF-8"], ["Lines", "28"]],
    metadata: [["Framework", "Spring Boot (guess)"], ["URLs", "2"], ["Ports", "8443"], ["Empty values", "0"], ["Duplicate keys", "0"]],
    preview: { type: "code", lang: "json", body: '{\n  "service": "billing-api",\n  "db": "postgres://prod-db:5432/app",\n  "apiKey": "sk_live_••••••••••••",\n  "debug": false,\n  "region": "eu-central-1"\n}' },
    risks: [
      { sev: "danger", cat: "Security", title: "Possible live API key", why: "An \"apiKey\" value matches the pattern of a live secret token.", action: "Don't share this file. Rotate the key if it has leaked." },
      { sev: "warning", cat: "Security", title: "Production database URL", why: "A production Postgres connection string is stored in plain text.", action: "Move secrets to a vault or environment variable." },
    ],
  },
  "archive.zip": {
    type: "ZIP archive", trust: { score: 81, level: "low" },
    prov: { icon: "map-pin", label: "Likely from Downloads" },
    overview: [["Type", "ZIP"], ["Entries", "142"], ["Compressed", "12.4 MB"], ["Uncompressed", "38.1 MB"], ["Ratio", "3.1×"]],
    metadata: [["Method", "Deflate"], ["Comment", "—"], ["Encrypted entries", "0"], ["Nested archives", "1"]],
    structure: [["assets/", 0, "dir"], ["logo.png", 1, "120 KB"], ["data/", 0, "dir"], ["export.csv", 1, "3 MB"], ["inner.zip", 0, "6.2 MB"], ["manifest.json", 0, "1 KB"]],
    preview: { type: "tree" },
    risks: [
      { sev: "info", cat: "Integrity", title: "Nested archive inside", why: "Contains another .zip (inner.zip) — its contents aren't scanned until opened.", action: "Browse without extracting to inspect it safely." },
    ],
  },
  "invoice.pdf": {
    type: "PDF document", trust: { score: 93, level: "trusted" },
    prov: { icon: "map-pin", label: "Likely from Downloads" },
    overview: [["Type", "PDF 1.7"], ["Size", "240 KB"], ["Pages", "3"], ["Encrypted", "No"], ["Path", "/DCIM/invoice.pdf"]],
    metadata: [["Title", "Invoice #2025-0412"], ["Author", "Billing"], ["Producer", "LibreOffice 7.6"], ["Created", "2025-03-01"], ["Forms", "None"]],
    preview: { type: "text", body: "INVOICE  #2025-0412\n\nBill to: Acme GmbH\nAmount due: € 4,200.00\nDue date: 2025-03-31\n\nThank you for your business." },
    risks: [],
  },
  "app-release.apk": {
    type: "Android package", trust: { score: 41, level: "medium" },
    prov: { icon: "map-pin", label: "From Downloads" },
    overview: [["Package", "com.example.app"], ["Version", "2.4.1 (241)"], ["Min SDK", "26"], ["Target SDK", "34"], ["Size", "18.6 MB"]],
    metadata: [["Signature", "v2 + v3"], ["Native libs", "arm64-v8a, x86_64"], ["DEX files", "2 (multidex)"], ["Cleartext traffic", "Allowed"], ["Trackers (heuristic)", "2 likely"]],
    risks: [
      { sev: "danger", cat: "Security", title: "Requests SMS permissions", why: "An app from Downloads requests READ_SMS and SEND_SMS — uncommon for this category.", action: "Verify the source before installing." },
      { sev: "warning", cat: "Security", title: "Debuggable build", why: "android:debuggable is true — not a production release flag.", action: "Treat as a test build; avoid installing on a primary device." },
      { sev: "info", cat: "Security", title: "Cleartext traffic allowed", why: "The network config permits unencrypted HTTP.", action: "—" },
    ],
  },
};

// Generic fallback for files without bespoke data
function inspectFor(file) {
  if (INSPECT[file.name]) return INSPECT[file.name];
  const sizeMatch = (file.sub || "").split(" · ")[0] || "—";
  return {
    type: (file.ext || "File") + (file.cat === "text" ? " · text" : ""),
    trust: { score: 96, level: "trusted" },
    prov: { icon: "map-pin", label: "On this device" },
    overview: [["Type", file.ext || "Unknown"], ["Size", sizeMatch], ["Detected", (file.ext || "—") + " (magic match)"], ["Path", "/" + file.name]],
    metadata: [["Encoding", "UTF-8"], ["Read-only", "No"]],
    preview: file.cat === "image" ? { type: "image" } : { type: "text", body: "No preview risks found.\nThis file looks clean." },
    risks: [],
  };
}

window.DATA = { CAT, TRUST, FILES, INSPECT, inspectFor };
