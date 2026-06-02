// FileLens kit — components + interactive app
const { useState, useRef, useEffect } = React;
const { CAT, TRUST, FILES, inspectFor } = window.DATA;
const cvar = (v) => "var(" + v + ")";
const tint = (v) => "color-mix(in srgb, var(" + v + ") 14%, transparent)";

// ---------- atoms ----------
function FileGlyph({ cat, size = 36 }) {
  const c = CAT[cat] || CAT.text || { icon: "file", col: "--fl-ink-3" };
  return (
    <span className="fl-ficon" style={{ width: size, height: size, background: tint(c.col) }}>
      <Icon name={c.icon} color={cvar(c.col)} fill={!!c.fill} />
    </span>
  );
}

function TrustChip({ trust }) {
  const t = TRUST[trust.level];
  return (
    <span className="fl-trust" style={{ background: tint(t.v), color: cvar(t.v) }}>
      <Icon name={t.icon} color={cvar(t.v)} /> {t.label} · {trust.score}
    </span>
  );
}

function TrustRing({ trust, size = 60, sw = 5 }) {
  const t = TRUST[trust.level];
  const r = (size - sw) / 2, c = 2 * Math.PI * r;
  return (
    <div style={{ position: "relative", width: size, height: size, flex: "0 0 auto" }}>
      <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="var(--fl-surface-3)" strokeWidth={sw} />
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={cvar(t.v)} strokeWidth={sw}
          strokeDasharray={c} strokeDashoffset={c * (1 - trust.score / 100)} strokeLinecap="round" />
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", fontFamily: "var(--fl-font-mono)", fontWeight: 700, fontSize: size * 0.28 }}>{trust.score}</div>
    </div>
  );
}

function RiskCard({ r }) {
  const ic = r.sev === "danger" ? "shield-alert" : r.sev === "warning" ? "triangle-alert" : "info";
  const col = "var(--fl-" + r.sev + ")";
  return (
    <div className={"fl-risk " + r.sev}>
      <div className="fl-risk-head" style={{ color: col }}><Icon name={ic} color={col} /> {r.sev} · {r.cat}</div>
      <div className="rt">{r.title}</div>
      <div className="rw">{r.why}</div>
      {r.action && r.action !== "—" && <div className="ra"><Icon name="arrow-right" color="var(--fl-ink-3)" /> {r.action}</div>}
    </div>
  );
}

// ---------- file row ----------
function FileRow({ file, selectionMode, checked, isInspected, onTap, onLong }) {
  const timer = useRef(null);
  const c = CAT[file.cat];
  const start = () => { timer.current = setTimeout(() => onLong(), 320); };
  const clear = () => { if (timer.current) clearTimeout(timer.current); };
  return (
    <div className={"fl-row" + (isInspected ? " sel" : "") + (checked ? " checked" : "")}
      onClick={onTap} onMouseDown={start} onMouseUp={clear} onMouseLeave={clear}
      onContextMenu={(e) => { e.preventDefault(); onLong(); }}>
      {selectionMode ? (
        <span className={"fl-check" + (checked ? " on" : "")}>{checked && <Icon name="check" />}</span>
      ) : <FileGlyph cat={file.cat} />}
      <div className="fl-fmeta">
        <div className="fl-fname">{file.name}</div>
        <div className="fl-fsub">{file.sub}</div>
      </div>
      {file.ext && <span className="fl-ext" style={{ color: cvar(c.col), background: tint(c.col) }}>{file.ext}</span>}
      {file.sev && !selectionMode && <span className="fl-dot" style={{ background: "var(--fl-" + file.sev + ")" }} />}
      {file.folder && !selectionMode && <Icon name="chevron-right" cls="fl-chev" />}
    </div>
  );
}

// ---------- inspector tab bodies ----------
function KVList({ rows }) {
  return rows.map(([k, v]) => <div className="fl-kv" key={k}><span className="k">{k}</span><span className="v">{v}</span></div>);
}

function Tree({ rows }) {
  return (
    <div className="fl-tree">
      {rows.map(([name, depth, detail], i) => (
        <div key={i} style={{ paddingLeft: depth * 16 }}>
          <Icon name={detail === "dir" ? "folder" : "file-text"} size={14} color={detail === "dir" ? "var(--cat-dir)" : "var(--fl-ink-3)"} fill={detail === "dir"} style={{ verticalAlign: "-2px", marginRight: 6 }} />
          <span className={detail === "dir" ? "tdir" : "tfile"}>{name}</span>
          {detail !== "dir" && <span className="tsz">{detail}</span>}
        </div>
      ))}
    </div>
  );
}

function TabBody({ tab, file, data }) {
  if (tab === "Overview") {
    const t = TRUST[data.trust.level];
    return (
      <div>
        <div className="fl-prov"><Icon name={data.prov.icon} /> {data.prov.label}</div>
        <div style={{ display: "flex", alignItems: "center", gap: 14, padding: "4px 0 14px" }}>
          <TrustRing trust={data.trust} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12, color: "var(--fl-ink-3)", marginBottom: 4 }}>Trust score · heuristic</div>
            <div style={{ fontSize: 15, fontWeight: 700, color: cvar(t.v) }}>{t.label}</div>
            <div style={{ fontSize: 11.5, color: "var(--fl-ink-3)", marginTop: 3 }}>Not antivirus · runs on-device</div>
          </div>
        </div>
        <KVList rows={data.overview} />
      </div>
    );
  }
  if (tab === "Metadata") return <div><KVList rows={data.metadata || [["—", "No metadata"]]} /></div>;
  if (tab === "Structure") {
    if (data.structure) return <Tree rows={data.structure} />;
    return <Empty icon="braces" text="No internal structure to show for this file type." />;
  }
  if (tab === "Preview") {
    const p = data.preview;
    if (!p) return <Empty icon="eye" text="No preview available." />;
    if (p.type === "image") return <div className="fl-img-prev"><div style={{ textAlign: "center" }}><Icon name="image" size={40} color="var(--fl-ink-3)" /><div style={{ fontSize: 12, marginTop: 8 }}>4032 × 3024 · JPEG</div></div></div>;
    if (p.type === "tree") return <Tree rows={data.structure || []} />;
    return <div className="fl-code">{p.body}</div>;
  }
  if (tab === "Risks") {
    if (!data.risks || data.risks.length === 0)
      return <Empty icon="shield-check" text="No risks detected. This file looks clean." safe />;
    return (
      <div>
        {data.secrets ? <div className="fl-prov" style={{ color: "var(--fl-warning)" }}><Icon name="key-round" color="var(--fl-warning)" /> {data.secrets} possible secret found — redacted</div> : null}
        {data.risks.map((r, i) => <RiskCard r={r} key={i} />)}
      </div>
    );
  }
  return null;
}

function Empty({ icon, text, safe }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 12, padding: "36px 30px", textAlign: "center", color: safe ? "var(--fl-safe)" : "var(--fl-ink-3)" }}>
      <Icon name={icon} size={40} color={safe ? "var(--fl-safe)" : "var(--fl-ink-3)"} />
      <div style={{ fontSize: 13.5, lineHeight: 1.5 }}>{text}</div>
    </div>
  );
}

// ---------- inspector sheet ----------
const TABS = ["Overview", "Preview", "Structure", "Metadata", "Risks"];
function InspectorSheet({ file, show, onClose, onAction }) {
  const [tab, setTab] = useState("Overview");
  useEffect(() => { if (show) setTab("Overview"); }, [file, show]);
  const data = file ? inspectFor(file) : null;
  const c = file ? CAT[file.cat] : null;
  return (
    <>
      <div className={"fl-scrim" + (show ? " show" : "")} onClick={onClose} />
      <div className={"fl-sheet" + (show ? " show" : "")}>
        <div className="fl-grab" />
        {file && data && (
          <>
            <div className="fl-sheet-head">
              <FileGlyph cat={file.cat} size={42} />
              <div className="fl-sheet-title">
                <div className="nm">{file.name}</div>
                <div className="ty">{data.type} · {file.sub.split(" · ")[0]}</div>
              </div>
              <TrustChip trust={data.trust} />
            </div>
            <div className="fl-tabs">
              {TABS.map((t) => (
                <span key={t} className={"fl-tab" + (t === tab ? " on" : "")} onClick={() => setTab(t)}>
                  {t}{t === "Risks" && data.risks && data.risks.length > 0 && <span className="tcount">{data.risks.length}</span>}
                </span>
              ))}
            </div>
            <div className="fl-tabbody"><TabBody tab={tab} file={file} data={data} /></div>
            <div className="fl-actions">
              <button className="fl-btn fl-btn-primary" onClick={() => onAction("open", file)}><Icon name="shield-check" /> Open safely</button>
              <button className="fl-btn fl-btn-ghost" onClick={() => onAction("share", file, data)}><Icon name="share-2" /></button>
              <button className="fl-btn fl-btn-ghost" onClick={() => onAction("openwith", file)}><Icon name="external-link" /></button>
            </div>
          </>
        )}
      </div>
    </>
  );
}

// ---------- commander bar ----------
const CMDS = [
  { id: "view", icon: "eye", lab: "View" },
  { id: "copy", icon: "copy", lab: "Copy" },
  { id: "move", icon: "folder-input", lab: "Move" },
  { id: "rename", icon: "pencil", lab: "Rename" },
  { id: "newfolder", icon: "folder-plus", lab: "Folder" },
  { id: "delete", icon: "trash-2", lab: "Delete", danger: true },
];
function CommanderBar({ count, onCmd }) {
  return (
    <div className="fl-commander">
      {CMDS.map((c) => (
        <div key={c.id} className={"fl-cmd" + (c.danger ? " danger" : "")} onClick={() => onCmd(c.id)}>
          <Icon name={c.icon} /><span className="lab">{c.lab}</span>
        </div>
      ))}
    </div>
  );
}

// ---------- app ----------
function App() {
  const [fs, setFs] = useState(() => JSON.parse(JSON.stringify(FILES)));
  const [path, setPath] = useState("DCIM");
  const [inspected, setInspected] = useState(null);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [selMode, setSelMode] = useState(false);
  const [sel, setSel] = useState(new Set());
  const [sort, setSort] = useState("Name");
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [toast, setToast] = useState(null);
  const toastTimer = useRef(null);

  const segs = path.split("/");
  const showToast = (msg) => {
    setToast(msg);
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 2600);
  };

  let list = (fs[path] || []).slice();
  if (query) list = list.filter((f) => f.name.toLowerCase().includes(query.toLowerCase()));
  if (sort === "Name") list.sort((a, b) => (a.cat === "dir") === (b.cat === "dir") ? a.name.localeCompare(b.name) : (a.cat === "dir" ? -1 : 1));
  else if (sort === "Type") list.sort((a, b) => a.cat.localeCompare(b.cat));

  const navTo = (p) => { setPath(p); setSelMode(false); setSel(new Set()); setSearchOpen(false); setQuery(""); };

  const onTap = (file) => {
    if (selMode) {
      const n = new Set(sel);
      n.has(file.name) ? n.delete(file.name) : n.add(file.name);
      setSel(n);
      if (n.size === 0) setSelMode(false);
      return;
    }
    if (file.folder) { navTo(file.folder); return; }
    setInspected(file); setSheetOpen(true);
  };
  const onLong = (file) => {
    if (file.folder) return;
    setSelMode(true);
    setSel(new Set([file.name]));
  };

  const onCmd = (id) => {
    if (id === "delete") {
      const next = JSON.parse(JSON.stringify(fs));
      next[path] = (next[path] || []).filter((f) => !sel.has(f.name));
      setFs(next); showToast(sel.size + " item" + (sel.size > 1 ? "s" : "") + " moved to .trash · Ctrl+Z to undo");
      setSel(new Set()); setSelMode(false);
      return;
    }
    if (id === "view") {
      const first = list.find((f) => sel.has(f.name));
      if (first) { setInspected(first); setSheetOpen(true); }
      return;
    }
    const labels = { copy: "Copy", move: "Move", rename: "Rename", newfolder: "New folder" };
    showToast(labels[id] + " — " + sel.size + " selected");
  };

  const onAction = (kind, file, data) => {
    if (kind === "open") showToast("Opened " + file.name + " in preview-only mode");
    else if (kind === "share") {
      const risky = data && data.risks && data.risks.some((r) => r.cat === "Privacy" || r.cat === "Metadata");
      showToast(risky ? "Shared a cleaned copy — GPS & author metadata removed" : "Shared " + file.name);
    } else if (kind === "openwith") showToast("Open with… (system chooser)");
  };

  return (
    <div className="fl-app">
      {/* app bar */}
      <div className="fl-appbar">
        <span className="fl-brand"><Icon name="scan-search" color="#fff" /></span>
        <span className="fl-title">FileLens</span>
        <span className="fl-spacer" />
        <span className={"fl-iconbtn" + (searchOpen ? " on" : "")} onClick={() => { setSearchOpen(!searchOpen); setQuery(""); }}><Icon name="search" /></span>
        <span className="fl-iconbtn" onClick={() => showToast("Scan this folder for risks")}><Icon name="sliders-horizontal" /></span>
      </div>

      {/* breadcrumb */}
      <div className="fl-crumbs">
        {segs.length > 1 && <span className="fl-up" onClick={() => navTo(segs.slice(0, -1).join("/"))}><Icon name="corner-left-up" size={15} /></span>}
        {segs.map((s, i) => {
          const p = segs.slice(0, i + 1).join("/");
          const last = i === segs.length - 1;
          return (
            <React.Fragment key={p}>
              <span className={last ? "here" : "seg"} onClick={() => !last && navTo(p)}>{s}</span>
              {!last && <Icon name="chevron-right" size={13} />}
            </React.Fragment>
          );
        })}
      </div>

      {searchOpen && (
        <div className="fl-search">
          <Icon name="search" />
          <input autoFocus placeholder="Search this folder…" value={query} onChange={(e) => setQuery(e.target.value)} />
          {query && <Icon name="x" size={16} color="var(--fl-ink-3)" style={{ cursor: "pointer" }} onClick={() => setQuery("")} />}
        </div>
      )}

      {!searchOpen && (
        <div className="fl-seg">
          {["Name", "Size", "Date", "Type"].map((s) => (
            <span key={s} className={s === sort ? "on" : ""} onClick={() => setSort(s)}>{s}</span>
          ))}
        </div>
      )}

      {/* list */}
      <div className="fl-list">
        {list.length === 0 ? (
          <div className="fl-empty"><Icon name="search" /><div>No files match “{query}”.</div></div>
        ) : list.map((f) => (
          <FileRow key={f.name} file={f} selectionMode={selMode} checked={sel.has(f.name)}
            isInspected={!selMode && inspected && inspected.name === f.name && sheetOpen}
            onTap={() => onTap(f)} onLong={() => onLong(f)} />
        ))}
      </div>

      {/* FAB */}
      {!selMode && <div className="fl-fab" onClick={() => showToast("Pick files or a folder to scan")}><Icon name="upload" color="#fff" /></div>}

      {/* commander */}
      {selMode && <CommanderBar count={sel.size} onCmd={onCmd} />}

      {/* inspector */}
      <InspectorSheet file={inspected} show={sheetOpen} onClose={() => setSheetOpen(false)} onAction={onAction} />

      {/* toast */}
      {toast && <div className={"fl-toast show"}><Icon name="check" /> {toast}</div>}
    </div>
  );
}

window.FileLensApp = App;
