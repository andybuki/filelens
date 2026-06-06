// FileLens logo exploration — competitor schematics + logo directions
// Palette (MoneyLupe tokens): navy #0F2A44, blue #2563EB, blue-light #3B82F6,
// blue-dark #1D4ED8, white, slate #94A3B8. Owl eye spokes: orange/magenta/purple/teal/blue.

// ---------- adaptive-icon shell ----------
function Squircle({ size = 96, bg = "#2563EB", children, ring, style }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: size * 0.225, background: bg,
      display: "grid", placeItems: "center", overflow: "hidden", position: "relative",
      boxShadow: ring ? "inset 0 0 0 1px rgba(255,255,255,.10)" : "0 6px 16px -6px rgba(15,42,68,.5)",
      flex: "0 0 auto", ...style,
    }}>
      <svg width={size} height={size} viewBox="0 0 100 100" style={{ display: "block" }}>{children}</svg>
    </div>
  );
}
window.Squircle = Squircle;

// =====================================================================
// COMPETITOR SCHEMATICS  (simplified, for pattern analysis — not exact logos)
// =====================================================================
const Comp = {
  google: (s) => (
    <Squircle size={s} bg="#ffffff">
      <path d="M22 38a4 4 0 0 1 4-4h14l6 7h28a4 4 0 0 1 4 4v23a4 4 0 0 1-4 4H26a4 4 0 0 1-4-4Z" fill="#2684FC"/>
      <rect x="30" y="30" width="34" height="22" rx="3" fill="#34A853" opacity=".0"/>
      <path d="M58 30h-2v40h22a4 4 0 0 0 4-4V45a4 4 0 0 0-4-4H64Z" fill="#1A73E8" opacity=".0"/>
    </Squircle>
  ),
  solid: (s) => (
    <Squircle size={s} bg="#263238">
      <rect x="20" y="28" width="27" height="44" rx="5" fill="#FF7043"/>
      <rect x="53" y="28" width="27" height="44" rx="5" fill="#42A5F5"/>
    </Squircle>
  ),
  total: (s) => (
    <Squircle size={s} bg="#FFC107">
      <path d="M24 40h22l5 6h25v26a3 3 0 0 1-3 3H27a3 3 0 0 1-3-3Z" fill="#00897B"/>
    </Squircle>
  ),
  cx: (s) => (
    <Squircle size={s} bg="#ffffff">
      <path d="M22 40a4 4 0 0 1 4-4h13l6 6h29a4 4 0 0 1 4 4v22a4 4 0 0 1-4 4H26a4 4 0 0 1-4-4Z" fill="#26A69A"/>
    </Squircle>
  ),
  mix: (s) => (
    <Squircle size={s} bg="#1565C0">
      <path d="M26 68V34l12 16 12-16v34" fill="none" stroke="#fff" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round"/>
      <circle cx="66" cy="42" r="6" fill="#90CAF9"/>
    </Squircle>
  ),
  fossify: (s) => (
    <Squircle size={s} bg="#ffffff">
      <path d="M22 40a4 4 0 0 1 4-4h13l6 6h29a4 4 0 0 1 4 4v22a4 4 0 0 1-4 4H26a4 4 0 0 1-4-4Z" fill="#E91E63"/>
      <path d="M22 50h56v18a4 4 0 0 1-4 4H26a4 4 0 0 1-4-4Z" fill="#9C27B0" opacity=".55"/>
    </Squircle>
  ),
};
window.Comp = Comp;

// =====================================================================
// FILELENS LOGO DIRECTIONS
// =====================================================================

// Shared page glyph
function Page({ x = 0, y = 0, fill = "#fff", fold = "#CBD5E1", w = 1 }) {
  return (
    <g transform={`translate(${x} ${y})`}>
      <path d="M28 18h24l16 16v44a4 4 0 0 1-4 4H28a4 4 0 0 1-4-4V22a4 4 0 0 1 4-4Z" fill={fill}/>
      <path d="M52 18v12a4 4 0 0 0 4 4h12Z" fill={fold}/>
    </g>
  );
}

// A — Lens over page (literal: inspect a file)
const DirA = (s, opt = {}) => {
  const bg = opt.bg || "#0F2A44";
  return (
    <Squircle size={s} bg={bg} ring>
      <path d="M26 18h22l15 15v32H26a4 4 0 0 1-4-4V22a4 4 0 0 1 4-4Z" fill="#E8EEF6"/>
      <path d="M48 18v11a4 4 0 0 0 4 4h11Z" fill="#A5B7CC"/>
      <line x1="33" y1="34" x2="50" y2="34" stroke="#9FB1C7" strokeWidth="3.4" strokeLinecap="round"/>
      <line x1="33" y1="43" x2="46" y2="43" stroke="#9FB1C7" strokeWidth="3.4" strokeLinecap="round"/>
      <circle cx="60" cy="60" r="17" fill={bg}/>
      <circle cx="60" cy="60" r="15" fill="none" stroke="#3B82F6" strokeWidth="6"/>
      <circle cx="60" cy="60" r="6.5" fill="#2563EB"/>
      <line x1="72.5" y1="72.5" x2="84" y2="84" stroke="#3B82F6" strokeWidth="7" strokeLinecap="round"/>
    </Squircle>
  );
};

// B — Scan reticle (x-ray / understand before opening)
const DirB = (s, opt = {}) => {
  const bg = opt.bg || "#2563EB";
  const c = opt.fg || "#fff";
  const br = (pts) => <path d={pts} fill="none" stroke={c} strokeWidth="6.5" strokeLinecap="round" strokeLinejoin="round"/>;
  return (
    <Squircle size={s} bg={bg}>
      {br("M24 38V30a6 6 0 0 1 6-6h8")}
      {br("M62 24h8a6 6 0 0 1 6 6v8")}
      {br("M76 62v8a6 6 0 0 1-6 6h-8")}
      {br("M38 76h-8a6 6 0 0 1-6-6v-8")}
      <circle cx="50" cy="50" r="13" fill="none" stroke={c} strokeWidth="6"/>
      <circle cx="50" cy="50" r="4.5" fill={c}/>
    </Squircle>
  );
};

// C — Monogram: bold F whose bowl is a lens
const DirC = (s, opt = {}) => {
  const bg = opt.bg || "#0F2A44";
  return (
    <Squircle size={s} bg={bg} ring>
      <path d="M34 24h30v11H46v10h15v11H46v20H34Z" fill="#fff"/>
      <circle cx="64" cy="64" r="14" fill={bg}/>
      <circle cx="64" cy="64" r="12" fill="none" stroke="#3B82F6" strokeWidth="5.5"/>
      <line x1="74" y1="74" x2="83" y2="83" stroke="#3B82F6" strokeWidth="6" strokeLinecap="round"/>
    </Squircle>
  );
};

// D — Lens + iris with owl-eye spokes (MoneyLupe family nod)
const DirD = (s, opt = {}) => {
  const bg = opt.bg || "#0F2A44";
  const spokes = ["#F59E0B", "#EC4899", "#A855F7", "#14B8A6", "#3B82F6", "#F97316"];
  return (
    <Squircle size={s} bg={bg} ring>
      <circle cx="46" cy="46" r="24" fill="none" stroke="#3B82F6" strokeWidth="6.5"/>
      <circle cx="46" cy="46" r="14" fill="#0B1F33"/>
      {spokes.map((col, i) => {
        const a = (i / spokes.length) * Math.PI * 2 - Math.PI / 2;
        const x1 = 46 + Math.cos(a) * 4.5, y1 = 46 + Math.sin(a) * 4.5;
        const x2 = 46 + Math.cos(a) * 13, y2 = 46 + Math.sin(a) * 13;
        return <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke={col} strokeWidth="3.4" strokeLinecap="round"/>;
      })}
      <circle cx="46" cy="46" r="4" fill="#fff"/>
      <line x1="64" y1="64" x2="82" y2="82" stroke="#3B82F6" strokeWidth="8" strokeLinecap="round"/>
    </Squircle>
  );
};

// E — Folder + lens hybrid (keeps category cue, adds the lens)
const DirE = (s, opt = {}) => {
  const bg = opt.bg || "#ffffff";
  return (
    <Squircle size={s} bg={bg}>
      <path d="M20 36a5 5 0 0 1 5-5h15l6 7h29a5 5 0 0 1 5 5v27a5 5 0 0 1-5 5H25a5 5 0 0 1-5-5Z" fill="#2563EB"/>
      <path d="M20 48h60v22a5 5 0 0 1-5 5H25a5 5 0 0 1-5-5Z" fill="#3B82F6"/>
      <circle cx="50" cy="60" r="12.5" fill="#0F2A44"/>
      <circle cx="50" cy="60" r="10.5" fill="none" stroke="#fff" strokeWidth="4.5"/>
      <line x1="59" y1="69" x2="67" y2="77" stroke="#fff" strokeWidth="5" strokeLinecap="round"/>
    </Squircle>
  );
};

// F — Concentric focus + file corner (minimal, iconic)
const DirF = (s, opt = {}) => {
  const bg = opt.bg || "#2563EB";
  return (
    <Squircle size={s} bg={bg}>
      <circle cx="50" cy="50" r="26" fill="none" stroke="#fff" strokeWidth="4" opacity=".35"/>
      <circle cx="50" cy="50" r="17" fill="none" stroke="#fff" strokeWidth="4" opacity=".6"/>
      <circle cx="50" cy="50" r="8" fill="#fff"/>
    </Squircle>
  );
};

window.Dirs = { A: DirA, B: DirB, C: DirC, D: DirD, E: DirE, F: DirF };
