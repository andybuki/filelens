// FileLens exploration — shared UI pieces + brand lens mark
const { useState } = React;

// ---- Lens mark (brand glyph): violet ring + reticle + handle ----
function LensMark({ size = 20, stroke = "#fff", glow = false }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      style={glow ? { filter: "drop-shadow(0 0 5px rgba(192,132,252,.7))" } : null}>
      <circle cx="10.5" cy="10.5" r="6.5" stroke={stroke} strokeWidth="2" />
      <circle cx="10.5" cy="10.5" r="2.4" fill={stroke} />
      <path d="M15.4 15.4 L21 21" stroke={stroke} strokeWidth="2.4" strokeLinecap="round" />
      <path d="M10.5 1.6V4 M10.5 17v2.4 M1.6 10.5H4 M17 10.5h2.4" stroke={stroke} strokeWidth="1.4" strokeLinecap="round" opacity=".9" />
    </svg>
  );
}
window.LensMark = LensMark;

const TRUST = { trusted: "var(--trust-trusted)", low: "var(--trust-low)", medium: "var(--trust-medium)", high: "var(--trust-high)" };
const SEV = { danger: "var(--danger)", warning: "var(--warning)", info: "var(--info)", safe: "var(--safe)" };

function StatusBar() {
  return (
    <div className="statusbar">
      <span>9:41</span>
      <span className="dots">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M2 17h3v4H2zM8 13h3v8H8zM14 9h3v12h-3zM20 5h2v16h-2z"/></svg>
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 4C7 4 3.3 6 1 9l11 13L23 9c-2.3-3-6-5-11-5z"/></svg>
        <svg viewBox="0 0 26 24" fill="none"><rect x="1" y="6" width="20" height="12" rx="3" stroke="currentColor" strokeWidth="1.6"/><rect x="3" y="8" width="14" height="8" rx="1.5" fill="currentColor"/><rect x="22" y="9.5" width="2.5" height="5" rx="1.2" fill="currentColor"/></svg>
      </span>
    </div>
  );
}

function AppBar({ accentGlow }) {
  return (
    <div className="appbar">
      <div className="brandmark" style={{ background: "var(--accent)", boxShadow: accentGlow ? "0 0 16px -2px rgba(168,85,247,.7)" : "none" }}>
        <LensMark size={18} stroke="#fff" />
      </div>
      <span className="title">FileLens</span>
      <span className="spacer" />
      <span className="iconbtn"><Icon name="search" size={19} /></span>
      <span className="iconbtn"><Icon name="sliders-horizontal" size={19} /></span>
    </div>
  );
}

function Crumbs() {
  return (
    <div className="crumbs">
      <span><Icon name="corner-left-up" size={13} /></span>
      <span>Internal</span><Icon name="chevron-right" size={12} />
      <span>storage</span><Icon name="chevron-right" size={12} />
      <span className="here">DCIM</span>
    </div>
  );
}

const CAT = {
  dir: ["folder", "var(--cat-dir)"], doc: ["file-text", "var(--cat-doc)"],
  archive: ["archive", "var(--cat-archive)"], image: ["image", "var(--cat-image)"],
  json: ["braces", "var(--cat-code)"], db: ["database", "var(--cat-db)"],
};

// File dataset shared across directions
const FILES = [
  { name: "Documents", cat: "dir", sub: "24 items", ext: "", sev: null, trust: null },
  { name: "Downloads", cat: "dir", sub: "18 items", ext: "", sev: null, trust: null },
  { name: "report.docx", cat: "doc", sub: "1.8 MB · Mar 4", ext: "DOCX", sev: "warning", trust: "low", score: 74, selected: true },
  { name: "archive.zip", cat: "archive", sub: "12.4 MB · Mar 2", ext: "ZIP", sev: "info", trust: "low", score: 81 },
  { name: "photo.jpg", cat: "image", sub: "3.2 MB · Feb 28", ext: "JPG", sev: "danger", trust: "medium", score: 58 },
  { name: "config.json", cat: "json", sub: "4 KB · Feb 26", ext: "JSON", sev: "danger", trust: "medium", score: 52 },
];

function ExtBadge({ ext, color, filled }) {
  if (!ext) return null;
  return <span className="ext" style={filled
    ? { background: color, color: "#15161a" }
    : { color, background: "color-mix(in srgb, " + color + " 16%, transparent)" }}>{ext}</span>;
}

// Semicircular trust gauge (Direction B)
function TrustGauge({ score, level }) {
  const r = 46, cx = 56, cy = 56;
  const a0 = Math.PI, a1 = Math.PI * (1 - score / 100);
  const pt = (a) => [cx + r * Math.cos(a), cy - r * Math.sin(a)];
  const [sx, sy] = pt(a0), [ex, ey] = pt(a1);
  const large = (a0 - a1) > Math.PI ? 1 : 0;
  return (
    <svg width="112" height="68" viewBox="0 0 112 68">
      <path d={`M ${pt(Math.PI)[0]} ${pt(Math.PI)[1]} A ${r} ${r} 0 0 1 ${pt(0)[0]} ${pt(0)[1]}`} fill="none" stroke="var(--surface-3)" strokeWidth="9" strokeLinecap="round" />
      <path d={`M ${sx} ${sy} A ${r} ${r} 0 ${large} 1 ${ex} ${ey}`} fill="none" stroke={TRUST[level]} strokeWidth="9" strokeLinecap="round" />
      <text x="56" y="50" textAnchor="middle" fontFamily="var(--mono)" fontSize="24" fontWeight="700" fill="var(--ink)">{score}</text>
    </svg>
  );
}

// Circular trust ring (Direction C)
function TrustRing({ score, level, size = 38, sw = 3 }) {
  const r = (size - sw) / 2, c = 2 * Math.PI * r;
  return (
    <svg width={size} height={size} style={{ position: "absolute", inset: 0, transform: "rotate(-90deg)" }}>
      <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="rgba(255,255,255,.10)" strokeWidth={sw} />
      <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={TRUST[level]} strokeWidth={sw}
        strokeDasharray={c} strokeDashoffset={c * (1 - score / 100)} strokeLinecap="round" />
    </svg>
  );
}

function RiskCard({ variant }) {
  // tracked-changes warning for report.docx
  if (variant === "strip") {
    return (
      <div style={{ borderRadius: 12, overflow: "hidden", background: "var(--surface-2)", border: "1px solid var(--border)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 12px", background: "var(--warning-tint)", color: "var(--warning)", fontWeight: 700, fontSize: 12 }}>
          <Icon name="shield-alert" size={16} color="var(--warning)" /> Warning · Metadata
        </div>
        <div style={{ padding: "10px 12px" }}>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 3 }}>2 tracked changes present</div>
          <div style={{ fontSize: 12, color: "var(--ink-2)", lineHeight: 1.4 }}>Edits and reviewer comments are still stored in this document.</div>
        </div>
      </div>
    );
  }
  // bordered (Direction A)
  return (
    <div style={{ display: "flex", gap: 10, padding: "11px 12px", borderRadius: 12, background: "var(--surface-2)", borderLeft: "3px solid var(--warning)" }}>
      <Icon name="shield-alert" size={18} color="var(--warning)" style={{ flex: "0 0 auto", marginTop: 1 }} />
      <div>
        <div style={{ fontSize: 13, fontWeight: 600 }}>2 tracked changes present</div>
        <div style={{ fontSize: 11.5, color: "var(--ink-2)", marginTop: 2, lineHeight: 1.4 }}>Edits and reviewer comments still stored.</div>
      </div>
    </div>
  );
}

window.FL = { StatusBar, AppBar, Crumbs, CAT, FILES, ExtBadge, TrustGauge, TrustRing, RiskCard, TRUST, SEV };
