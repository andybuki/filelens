// FileLens — File details screen (flat MoneyLupe). Single self-contained component.
const { useState } = React;

const FILE = {
  name: "SortedIntList.java",
  type: "Source code",
  trust: "ok", score: 100,
  glyph: "braces",
  reasons: ["No risks detected."],
  source: "Downloads",
  sourceNote: "File lives in the device Downloads folder.",
  overview: [
    ["File", "SortedIntList.java"], ["Type", "Source code"], ["Extension", ".java"],
    ["MIME", "text/plain"], ["Size", "491 B"], ["Modified", "Jun 1, 2026"],
    ["Detected type", "Text"], ["Encoding", "UTF-8"], ["Lines", "23"],
    ["Words", "48"], ["Characters", "491"], ["Language", "Java"],
  ],
  metadata: [
    ["Permissions", "rw-r--r--"], ["Owner", "user"], ["Path", "/Download/SortedIntList.java"],
    ["Created", "Jun 1, 2026"], ["Accessed", "Jun 7, 2026"], ["SHA-256", "9f2c1a7e…b4d0"],
    ["Read-only", "No"], ["Hidden", "No"],
  ],
  structure: [
    ["package com.example", "dir"], ["class SortedIntList", "leaf", "23 ln"],
    ["  add(int)", "leaf", "void"], ["  remove(int)", "leaf", "boolean"],
    ["  get(int)", "leaf", "int"], ["  size()", "leaf", "int"],
  ],
};

const TRUSTMETA = {
  ok: { label: "Trusted", icon: "shield-check" },
  review: { label: "Worth a look", icon: "triangle-alert" },
  caution: { label: "Review before opening", icon: "shield-alert" },
};

// Each line is an array of [className, text] segments. "" = default ink.
const CODE = [
  [["cm", "// SortedIntList.java"]],
  [],
  [["tk", "package"], ["", " com.example;"]],
  [],
  [["tk", "public class"], ["", " "], ["ty2", "SortedIntList"], ["", " {"]],
  [["", "    private int[] data;"]],
  [["", "    private int size;"]],
  [],
  [["", "    "], ["tk", "public void"], ["", " "], ["ty2", "add"], ["", "(int v) {"]],
  [["cm", "        // insert in order"]],
  [["", "    }"]],
  [["", "}"]],
];

function Toast({ msg }) {
  return msg ? <div className="toast show"><Icon name="check" /> {msg}</div> : null;
}

function TabOverview({ onAct }) {
  const t = TRUSTMETA[FILE.trust];
  return (
    <>
      <div className={"trust " + FILE.trust}>
        <div className="top">
          <span className="ic"><Icon name={t.icon} /></span>
          <span className="lab">{t.label}</span>
          <span className="num">{FILE.score}<span className="max"> / 100</span></span>
        </div>
        <div className="meter"><div className="fill" style={{ width: FILE.score + "%" }} /></div>
        <div className="hint">Heuristic trust score · runs on-device. Not antivirus.</div>
        <div className="sep" />
        <div className="reasons-eyebrow">Top reasons</div>
        {FILE.reasons.map((r) => (
          <div className="reason" key={r}><Icon name="check" color="var(--ml-income)" /> {r}</div>
        ))}
      </div>

      <div className="source">
        <div className="line1"><Icon name="map-pin" /><span className="k">Probable source</span><span className="v">{FILE.source}</span></div>
        <div className="line2">{FILE.sourceNote}</div>
      </div>

      <div className="kvlist">
        {FILE.overview.map(([k, v]) => (
          <div className="kv" key={k}><span className="k">{k}</span><span className="v">{v}</span></div>
        ))}
      </div>

      <div className="section-label">Safe actions</div>
      <button className="abtn" onClick={() => onAct("Opened in preview-only mode")}><Icon name="eye" /> Preview only</button>
      <button className="abtn" onClick={() => onAct("Inspecting structure…")}><Icon name="braces" /> Inspect structure</button>
      <button className="abtn" onClick={() => onAct("Search inside this file")}><Icon name="search" /> Search inside…</button>
      <div className="arow">
        <button className="abtn two" onClick={() => onAct("Exported as Markdown")}><Icon name="file-text" /> Markdown</button>
        <button className="abtn two" onClick={() => onAct("Exported as JSON")}><Icon name="braces" /> JSON</button>
      </div>
      <button className="abtn" onClick={() => onAct("Shared a cleaned copy — metadata removed")}><Icon name="shield-check" /> Share cleaned copy</button>
      <button className="abtn" onClick={() => onAct("Hash verified · matches")}><Icon name="key-round" /> Verify hash</button>

      <div className="section-label">External</div>
      <button className="abtn primary" onClick={() => onAct("Open with… (system chooser)")}><Icon name="external-link" /> Open with…</button>
    </>
  );
}

function TabPreview() {
  return (
    <div className="codewrap">
      <div className="codebar">
        <span className="dotrow"><i style={{ background: "#F87171" }} /><i style={{ background: "#FBBF24" }} /><i style={{ background: "#34D399" }} /></span>
        SortedIntList.java · 23 lines
      </div>
      <div className="code">
        {CODE.map((segs, i) => (
          <span className="ln" key={i}>
            {segs.length === 0
              ? "\u00a0"
              : segs.map(([cls, txt], j) => <span key={j} className={cls}>{txt}</span>)}
          </span>
        ))}
      </div>
    </div>
  );
}

function TabStructure() {
  return (
    <div className="kvlist" style={{ padding: "12px 16px" }}>
      <div className="tree">
        {FILE.structure.map(([n, kind, sz], i) => (
          <div className="node" key={i} style={{ paddingLeft: n.startsWith("  ") ? 18 : 0 }}>
            <Icon name={kind === "dir" ? "folder" : "file-text"} color={kind === "dir" ? "var(--ml-blue)" : "var(--ml-text-2)"} fill={kind === "dir"} />
            <span className={kind === "dir" ? "dir" : "leaf"}>{n.trim()}</span>
            {sz && <span className="sz">{sz}</span>}
          </div>
        ))}
      </div>
    </div>
  );
}

function TabMetadata() {
  return (
    <div className="kvlist">
      {FILE.metadata.map(([k, v]) => (
        <div className="kv" key={k}><span className="k">{k}</span><span className="v">{v}</span></div>
      ))}
    </div>
  );
}

function TabRisks() {
  return (
    <div className="clean">
      <span className="ci"><Icon name="shield-check" /></span>
      <div className="ct">No issues found</div>
      <div className="cb">This file looks safe to open. We checked privacy, secrets, active content and integrity.</div>
    </div>
  );
}

const TABS = ["Overview", "Preview", "Structure", "Metadata", "Risks"];

function Details() {
  const [tab, setTab] = useState("Overview");
  const [toast, setToast] = useState(null);
  const t = TRUSTMETA[FILE.trust];
  const onAct = (m) => { setToast(m); clearTimeout(window.__t); window.__t = setTimeout(() => setToast(null), 2400); };

  return (
    <div className="screen">
      <div className="statusbar">
        <span>14:02</span>
        <span className="right">
          <svg width="17" height="11" viewBox="0 0 17 11" fill="currentColor"><rect x="0" y="7" width="3" height="4" rx=".5"/><rect x="5" y="5" width="3" height="6" rx=".5"/><rect x="10" y="2" width="3" height="9" rx=".5"/><rect x="15" y="0" width="3" height="11" rx=".5"/></svg>
          <svg width="16" height="11" viewBox="0 0 16 11" fill="currentColor"><path d="M8 2a10 10 0 0 1 7 2.8l-1.4 1.4A8 8 0 0 0 8 4 8 8 0 0 0 2.4 6.2L1 4.8A10 10 0 0 1 8 2Zm0 4a6 6 0 0 1 4.2 1.7L11 9A4 4 0 0 0 8 8a4 4 0 0 0-3 1L3.8 7.7A6 6 0 0 1 8 6Z"/></svg>
          <svg width="25" height="11" viewBox="0 0 25 11"><rect x=".5" y=".5" width="22" height="10" rx="2.3" fill="none" stroke="currentColor" strokeWidth="1" opacity=".4"/><rect x="23" y="3.5" width="1.5" height="4" rx=".5" fill="currentColor" opacity=".4"/><rect x="2" y="2" width="18" height="7" rx="1.3" fill="currentColor"/></svg>
        </span>
      </div>
      <div className="grab" />

      <div className="shead">
        <span className="glyph"><Icon name={FILE.glyph} /></span>
        <div className="info">
          <div className="nm">{FILE.name}</div>
          <div className="sub">
            <span className="ty">{FILE.type}</span>
            <span className={"tpill " + FILE.trust}><span className="dot" /> {t.label} · {FILE.score}</span>
          </div>
        </div>
        <div className="acts">
          <span className="ibtn" onClick={() => onAct("More options")}><Icon name="more-vertical" /></span>
          <span className="ibtn" onClick={() => onAct("Closed")}><Icon name="x" /></span>
        </div>
      </div>

      <div className="tabs">
        {TABS.map((x) => <span key={x} className={"tab" + (x === tab ? " on" : "")} onClick={() => setTab(x)}>{x}</span>)}
      </div>

      <div className="body">
        {tab === "Overview" && <TabOverview onAct={onAct} />}
        {tab === "Preview" && <TabPreview />}
        {tab === "Structure" && <TabStructure />}
        {tab === "Metadata" && <TabMetadata />}
        {tab === "Risks" && <TabRisks />}
      </div>

      <Toast msg={toast} />
    </div>
  );
}

window.Details = Details;
