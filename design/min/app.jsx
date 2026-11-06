// FileLens (minimal) — flat interactive browser
const { useState, useRef, useEffect } = React;
const { CAT, TRUST, FILES, inspectFor } = window.DATA;

function Glyph({ cat }) {
  const c = CAT[cat] || CAT.text;
  return <span className={"ic" + (c.accent ? " dir" : "")}><Icon name={c.icon} fill={false} /></span>;
}

function Row({ file, selected, onTap }) {
  return (
    <div className={"row" + (selected ? " sel" : "")} onClick={onTap}>
      <Glyph cat={file.cat} />
      <div className="meta">
        <div className="nm">{file.name}</div>
        <div className={"sub" + (file.cat === "dir" ? "" : " mono")}>
          {file.cat === "dir" ? (file.count === 0 ? "Empty" : file.count + " items") : file.sub}
        </div>
      </div>
      <div className="row-right">
        {file.risk && <span className={"riskdot " + file.risk} />}
        {(file.folder) && <Icon name="chevron-right" cls="chev" />}
      </div>
    </div>
  );
}

// ---------- empty states ----------
function EmptyFolder({ onImport }) {
  return (
    <div className="empty">
      <div className="empty-art"><Icon name="folder-open" /></div>
      <div className="empty-title">This folder is empty</div>
      <div className="empty-body">Nothing here yet. Add files, or import a statement to inspect it before opening.</div>
      <button className="empty-btn" onClick={onImport}><Icon name="upload" /> Import a file</button>
    </div>
  );
}

function EmptySearch({ query }) {
  return (
    <div className="empty search-empty">
      <div className="empty-art"><Icon name="search" /></div>
      <div className="empty-title">No matches</div>
      <div className="empty-body">Nothing in this folder matches “{query}”.</div>
    </div>
  );
}

// ---------- inspector ----------
const TABS = ["Overview", "Preview", "Risks"];
function TabBody({ tab, data }) {
  if (tab === "Overview") return (
    <div>
      <div className="kv"><span className="k">Source</span><span className="v" style={{ fontFamily: "var(--ml-font-sans)", color: "var(--ml-text)" }}>{data.where}</span></div>
      {data.overview.map(([k, v]) => <div className="kv" key={k}><span className="k">{k}</span><span className="v">{v}</span></div>)}
    </div>
  );
  if (tab === "Preview") {
    if (data.image) return <div className="imgprev"><Icon name="image" /></div>;
    if (data.preview) return <div className="code">{data.preview}</div>;
    return <div className="clean"><Icon name="eye" color="var(--ml-muted)" style={{ color: "var(--ml-muted)" }} /><div className="ct" style={{ color: "var(--ml-text-2)" }}>No preview available</div></div>;
  }
  if (tab === "Risks") {
    if (!data.risks || data.risks.length === 0)
      return <div className="clean"><Icon name="shield-check" color="var(--ml-income)" /><div className="ct">No issues found</div><div className="cb">This file looks safe to open.</div></div>;
    return (
      <div>
        {data.risks.map((r, i) => (
          <div className={"risk " + r.tone} key={i}>
            <span className="risk-ic"><Icon name={r.tone === "danger" ? "shield-alert" : "triangle-alert"} /></span>
            <div className="risk-txt"><div className="rt">{r.title}</div><div className="rb">{r.body}</div></div>
          </div>
        ))}
      </div>
    );
  }
}

function Inspector({ file, show, onClose, onAction }) {
  const [tab, setTab] = useState("Overview");
  useEffect(() => { if (show) setTab("Overview"); }, [file, show]);
  const data = file ? inspectFor(file) : null;
  const t = data ? TRUST[data.trust] : null;
  return (
    <>
      <div className={"scrim" + (show ? " show" : "")} onClick={onClose} />
      <div className={"sheet" + (show ? " show" : "")}>
        <div className="grab" />
        {file && data && (
          <>
            <div className="sheet-head">
              <Glyph cat={file.cat} />
              <div className="sheet-name">
                <div className="nm">{file.name}</div>
                <div className="ty">{data.type} · {(file.sub || "").split(" · ")[0] || ""}</div>
              </div>
            </div>
            <div className={"trust " + data.trust}>
              <Icon name={t.icon} /> {t.label}
            </div>
            <div className="tabs">
              {TABS.map((x) => <span key={x} className={"tab" + (x === tab ? " on" : "")} onClick={() => setTab(x)}>{x}</span>)}
            </div>
            <div className="tabbody"><TabBody tab={tab} data={data} /></div>
            <div className="actions">
              <button className="btn btn-primary" onClick={() => onAction("open", file, data)}><Icon name="eye" /> Open</button>
              <button className="btn btn-ghost" onClick={() => onAction("share", file, data)}><Icon name="share-2" /></button>
            </div>
          </>
        )}
      </div>
    </>
  );
}

// ---------- app ----------
function App() {
  const [path, setPath] = useState("Internal storage");
  const [inspected, setInspected] = useState(null);
  const [open, setOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [toast, setToast] = useState(null);
  const timer = useRef(null);

  const segs = path.split("/");
  const showToast = (m) => { setToast(m); if (timer.current) clearTimeout(timer.current); timer.current = setTimeout(() => setToast(null), 2600); };
  const navTo = (p) => { setPath(p); setSearchOpen(false); setQuery(""); };

  let list = (FILES[path] || []).slice();
  if (query) list = list.filter((f) => f.name.toLowerCase().includes(query.toLowerCase()));
  list.sort((a, b) => (a.cat === "dir") === (b.cat === "dir") ? a.name.localeCompare(b.name) : (a.cat === "dir" ? -1 : 1));

  const onTap = (f) => { if (f.folder) navTo(f.folder); else { setInspected(f); setOpen(true); } };
  const onAction = (kind, file, data) => {
    if (kind === "open") showToast("Opened " + file.name + " in preview");
    else {
      const risky = data.risks.some((r) => r.title.includes("GPS") || r.title.includes("metadata"));
      showToast(risky ? "Shared a cleaned copy — location removed" : "Shared " + file.name);
    }
  };

  const isEmpty = list.length === 0;

  return (
    <div className="app">
      {/* header */}
      <div className="hdr">
        <div className="hdr-top">
          <span className="hdr-mark"><Icon name="scan-search" color="#fff" /></span>
          <span className="hdr-title">FileLens</span>
          <span className="hdr-spacer" />
          <span className="hdr-icon" onClick={() => { setSearchOpen(!searchOpen); setQuery(""); }}><Icon name={searchOpen ? "x" : "search"} /></span>
        </div>
        <div className="hdr-path">
          {segs.length > 1 && <span className="hdr-up" onClick={() => navTo(segs.slice(0, -1).join("/"))}><Icon name="corner-left-up" size={15} /></span>}
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
      </div>

      {searchOpen && (
        <div className="search">
          <Icon name="search" />
          <input autoFocus placeholder="Search this folder…" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
      )}

      {/* body */}
      {isEmpty ? (
        query ? <EmptySearch query={query} /> : <EmptyFolder onImport={() => showToast("Pick a file to import and inspect")} />
      ) : (
        <div className="list">
          {list.map((f) => (
            <Row key={f.name} file={f} selected={open && inspected && inspected.name === f.name && !f.folder} onTap={() => onTap(f)} />
          ))}
        </div>
      )}

      {/* FAB only when there are files (empty state has its own import CTA) */}
      {!isEmpty && <div className="fab" onClick={() => showToast("Pick a file to import and inspect")}><Icon name="upload" color="#fff" /></div>}

      <Inspector file={inspected} show={open} onClose={() => setOpen(false)} onAction={onAction} />

      {toast && <div className="toast show"><Icon name="check" /> {toast}</div>}
    </div>
  );
}

window.FileLensMin = App;
