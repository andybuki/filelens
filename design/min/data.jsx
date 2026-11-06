// FileLens (minimal) — file system + inspector data
// Risk model kept minimal: ok / info / warning / danger. Color only when it matters.

const CAT = {
  dir:     { icon: "folder",    accent: true },
  doc:     { icon: "file-text" },
  pdf:     { icon: "file-text" },
  image:   { icon: "image" },
  json:    { icon: "braces" },
  archive: { icon: "archive" },
  apk:     { icon: "package" },
  audio:   { icon: "music" },
  video:   { icon: "video" },
  text:    { icon: "file-text" },
};

// Trust tiers — minimal: ok (safe), review (warning), caution (danger)
const TRUST = {
  ok:      { label: "Looks safe",          tone: "income",  icon: "shield-check" },
  review:  { label: "Worth a look",        tone: "warning", icon: "triangle-alert" },
  caution: { label: "Review before opening", tone: "expense", icon: "shield-alert" },
};

// Folder tree. Every folder key exists; empty folders map to [].
const FILES = {
  "Internal storage": [
    { name: "Documents", cat: "dir", count: 6, folder: "Internal storage/Documents" },
    { name: "Downloads", cat: "dir", count: 4, folder: "Internal storage/Downloads" },
    { name: "Pictures",  cat: "dir", count: 0, folder: "Internal storage/Pictures" },
    { name: "Backups",   cat: "dir", count: 0, folder: "Internal storage/Backups" },
    { name: "report.pdf",   cat: "pdf",   sub: "1.8 MB · Mar 4",  ext: "PDF" },
    { name: "config.json",  cat: "json",  sub: "4 KB · Feb 26",   ext: "JSON", risk: "caution" },
    { name: "voice-memo.m4a", cat: "audio", sub: "1.1 MB · Feb 19", ext: "M4A" },
  ],
  "Internal storage/Documents": [
    { name: "Contracts", cat: "dir", count: 0, folder: "Internal storage/Documents/Contracts" },
    { name: "resume.docx",  cat: "doc",  sub: "88 KB · Jan 12",  ext: "DOCX" },
    { name: "report.pdf",   cat: "pdf",  sub: "1.8 MB · Mar 4",  ext: "PDF", risk: "review" },
    { name: "budget.json",  cat: "json", sub: "6 KB · Jan 30",   ext: "JSON" },
    { name: "scan.pdf",     cat: "pdf",  sub: "1.2 MB · Feb 2",  ext: "PDF" },
    { name: "notes.txt",    cat: "text", sub: "2 KB · Feb 18",   ext: "TXT" },
    { name: "photo.jpg",    cat: "image", sub: "3.2 MB · Feb 28", ext: "JPG", risk: "caution" },
  ],
  "Internal storage/Downloads": [
    { name: "setup.apk",  cat: "apk",     sub: "44 MB · Feb 22", ext: "APK", risk: "review" },
    { name: "archive.zip", cat: "archive", sub: "12.4 MB · Mar 2", ext: "ZIP", folder: "Internal storage/Downloads/archive.zip" },
    { name: "invoice.pdf", cat: "pdf",    sub: "240 KB · Mar 1", ext: "PDF" },
    { name: "clip.mp4",   cat: "video",   sub: "22 MB · Feb 24", ext: "MP4" },
  ],
  "Internal storage/Pictures": [],
  "Internal storage/Backups": [],
  "Internal storage/Documents/Contracts": [],
  "Internal storage/Downloads/archive.zip": [
    { name: "manifest.json", cat: "json", sub: "1 KB", ext: "JSON" },
    { name: "logo.png", cat: "image", sub: "120 KB", ext: "PNG" },
  ],
};

// Per-file inspector data. Generic fallback for the rest.
const INSPECT = {
  "config.json": {
    type: "JSON · config", trust: "caution",
    where: "Likely from Downloads",
    overview: [["Type", "JSON"], ["Size", "4 KB"], ["Keys", "14"], ["Encoding", "UTF-8"], ["Lines", "28"]],
    preview: '{\n  "service": "billing-api",\n  "db": "postgres://prod-db:5432/app",\n  "apiKey": "sk_live_••••••••",\n  "region": "eu-central-1"\n}',
    risks: [
      { tone: "danger", title: "Possible live API key", body: "An \u201capiKey\u201d value matches a live secret token. Don\u2019t share this file." },
      { tone: "warning", title: "Production database URL", body: "A prod database connection string is stored in plain text." },
    ],
  },
  "photo.jpg": {
    type: "JPEG image", trust: "caution",
    where: "From Camera",
    overview: [["Type", "JPEG"], ["Size", "3.2 MB"], ["Resolution", "4032 × 3024"], ["Taken", "Feb 28, 14:22"]],
    preview: null, image: true,
    risks: [
      { tone: "danger", title: "Contains GPS location", body: "This photo embeds where it was taken (48.86, 2.35). Share a cleaned copy to remove it." },
    ],
  },
  "report.pdf": {
    type: "PDF document", trust: "ok",
    where: "On this device",
    overview: [["Type", "PDF 1.7"], ["Size", "1.8 MB"], ["Pages", "12"], ["Encrypted", "No"]],
    preview: "Q1 Performance Review\n\nRevenue grew 14% QoQ, driven by the\nEMEA segment…",
    risks: [],
  },
  "setup.apk": {
    type: "Android package", trust: "review",
    where: "From Downloads",
    overview: [["Package", "com.example.app"], ["Version", "2.4.1 (241)"], ["Min SDK", "26"], ["Size", "44 MB"]],
    preview: null,
    risks: [
      { tone: "warning", title: "Installs an app", body: "APKs from Downloads can install software. Verify the source before opening." },
    ],
  },
};

function inspectFor(file) {
  if (INSPECT[file.name]) return INSPECT[file.name];
  const size = (file.sub || "").split(" · ")[0] || "—";
  return {
    type: (file.ext || "File"), trust: file.risk || "ok",
    where: "On this device",
    overview: [["Type", file.ext || "Unknown"], ["Size", size], ["Path", "/" + file.name]],
    preview: file.cat === "image" ? null : "No issues found.\nThis file looks clean.",
    image: file.cat === "image",
    risks: [],
  };
}

window.DATA = { CAT, TRUST, FILES, INSPECT, inspectFor };
