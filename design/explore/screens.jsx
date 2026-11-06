// FileLens exploration — three direction screens
const { StatusBar, AppBar, Crumbs, CAT, FILES, ExtBadge, TrustGauge, TrustRing, RiskCard, TRUST, SEV } = window.FL;

// ============ A · PANEL (clean Darcula) ============
function ScreenA() {
  return (
    <div className="phone">
      <StatusBar />
      <AppBar />
      <Crumbs />
      <div className="filelist">
        {FILES.map((f) => {
          const [ic, col] = CAT[f.cat];
          const sel = f.selected;
          return (
            <div key={f.name} className="frow" style={sel ? { background: "var(--accent-tint-2)", boxShadow: "inset 2px 0 0 var(--accent)" } : null}>
              <span className="ficon" style={{ background: "color-mix(in srgb," + col + " 14%, transparent)" }}><Icon name={ic} size={19} color={col} fill={f.cat === "dir"} /></span>
              <div className="fmeta">
                <div className="fname" style={sel ? { color: "var(--accent-bright)" } : null}>{f.name}</div>
                <div className="fsub">{f.sub}</div>
              </div>
              <ExtBadge ext={f.ext} color={col} />
              {f.sev && <span className="sev-dot" style={{ background: SEV[f.sev] }} />}
            </div>
          );
        })}
      </div>
      <div className="sheet">
        <div className="grab" />
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
          <span className="ficon" style={{ width: 38, height: 38, background: "color-mix(in srgb,var(--cat-doc) 14%, transparent)" }}><Icon name="file-text" size={20} color="var(--cat-doc)" /></span>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 700 }}>report.docx</div>
            <div className="note">Microsoft Word OOXML · 1.8 MB</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 6, padding: "5px 9px", borderRadius: 999, background: "var(--surface-3)" }}>
            <span className="sev-dot" style={{ background: TRUST.low }} />
            <span style={{ fontSize: 11.5, fontWeight: 700, color: TRUST.low }}>Low risk · 74</span>
          </div>
        </div>
        <div className="tabrow">
          {["Overview","Preview","Structure","Metadata","Risks"].map((t,i)=>(<span key={t} className={"tab"+(i===0?" on":"")}>{t}</span>))}
        </div>
        <div className="kv"><span className="k">Modified</span><span className="v">Mar 4, 2025</span></div>
        <div className="kv"><span className="k">Author</span><span className="v">J. Becker</span></div>
        <div style={{ margin: "10px 0" }}><RiskCard /></div>
        <div style={{ display: "flex", gap: 8 }}>
          <span className="btn btn-primary" style={{ flex: 1, justifyContent: "center" }}><Icon name="shield-check" size={16} /> Open safely</span>
          <span className="btn btn-ghost"><Icon name="upload" size={16} /></span>
        </div>
      </div>
    </div>
  );
}

// ============ B · GUTTER (bold, IDE gutter + trust gauge) ============
function ScreenB() {
  return (
    <div className="phone">
      <StatusBar />
      <AppBar accentGlow />
      <Crumbs />
      <div className="filelist" style={{ display: "flex", flexDirection: "column", gap: 7, paddingTop: 4 }}>
        {FILES.map((f) => {
          const [ic, col] = CAT[f.cat];
          const rail = f.sev ? SEV[f.sev] : "var(--border-2)";
          const sel = f.selected;
          return (
            <div key={f.name} style={{ display: "flex", alignItems: "stretch", borderRadius: 12, overflow: "hidden", background: sel ? "var(--surface-2)" : "var(--surface)", border: sel ? "1px solid var(--accent)" : "1px solid var(--border)" }}>
              <div style={{ width: 5, background: rail, flex: "0 0 auto" }} />
              <div className="frow" style={{ flex: 1, padding: "10px 11px" }}>
                <span className="ficon" style={{ background: "color-mix(in srgb," + col + " 16%, transparent)" }}><Icon name={ic} size={19} color={col} fill={f.cat === "dir"} /></span>
                <div className="fmeta">
                  <div className="fname">{f.name}</div>
                  <div className="fsub">{f.sub}</div>
                </div>
                <ExtBadge ext={f.ext} color={col} filled />
              </div>
            </div>
          );
        })}
      </div>
      <div className="sheet" style={{ borderTop: "1px solid var(--accent)", background: "linear-gradient(180deg, color-mix(in srgb,var(--accent) 7%, var(--surface)), var(--surface))" }}>
        <div className="grab" />
        <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 8 }}>
          <TrustGauge score={74} level="low" />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 700 }}>report.docx</div>
            <div className="note" style={{ marginBottom: 6 }}>1.8 MB · DOCX</div>
            <div style={{ display: "inline-flex", alignItems: "center", gap: 6, padding: "4px 10px", borderRadius: 8, background: "color-mix(in srgb,var(--trust-low) 16%, transparent)", color: "var(--trust-low)", fontWeight: 700, fontSize: 12 }}>
              <Icon name="shield-check" size={14} color="var(--trust-low)" /> Low risk
            </div>
          </div>
        </div>
        <div className="tabrow">
          {["Overview","Preview","Risks","Trust"].map((t,i)=>(<span key={t} className={"tab"+(i===2?" on":"")}>{t}</span>))}
        </div>
        <RiskCard variant="strip" />
        <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
          <span className="btn btn-primary" style={{ flex: 1, justifyContent: "center", borderRadius: 12 }}><Icon name="shield-check" size={16} /> Open safely</span>
          <span className="btn btn-ghost" style={{ borderRadius: 12 }}><Icon name="flag" size={16} /></span>
        </div>
      </div>
    </div>
  );
}

// ============ C · LENS (focus reticle + trust ring) ============
function ScreenC() {
  return (
    <div className="phone" style={{ background: "radial-gradient(120% 80% at 50% 8%, #1c1d22 0%, var(--bg) 60%)" }}>
      <StatusBar />
      <AppBar />
      <Crumbs />
      <div className="filelist">
        {FILES.map((f) => {
          const [ic, col] = CAT[f.cat];
          const sel = f.selected;
          return (
            <div key={f.name} className="frow" style={{ position: "relative", padding: "11px 12px", ...(sel ? { background: "var(--surface)" } : {}) }}>
              {sel && <Reticle />}
              <div style={{ position: "relative", width: 38, height: 38, flex: "0 0 auto" }}>
                <span style={{ position: "absolute", inset: 4, borderRadius: 9, display: "grid", placeItems: "center", background: "color-mix(in srgb," + col + " 16%, transparent)" }}><Icon name={ic} size={18} color={col} fill={f.cat === "dir"} /></span>
                {f.trust && <TrustRing score={f.score} level={f.trust} size={38} sw={2.5} />}
              </div>
              <div className="fmeta">
                <div className="fname">{f.name}</div>
                <div className="fsub">{f.sub}</div>
              </div>
              {f.sev && <Icon name={f.sev === "danger" ? "shield-alert" : f.sev === "warning" ? "shield-alert" : "shield-check"} size={16} color={SEV[f.sev]} />}
            </div>
          );
        })}
      </div>
      <div className="sheet" style={{ background: "var(--surface)" }}>
        <div className="grab" />
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 12 }}>
          <div style={{ position: "relative", width: 56, height: 56, flex: "0 0 auto" }}>
            <span style={{ position: "absolute", inset: 7, borderRadius: 12, display: "grid", placeItems: "center", background: "color-mix(in srgb,var(--cat-doc) 16%, transparent)" }}><Icon name="file-text" size={22} color="var(--cat-doc)" /></span>
            <TrustRing score={74} level="low" size={56} sw={3.5} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 700 }}>report.docx</div>
            <div className="note">Microsoft Word OOXML</div>
            <div style={{ fontSize: 12, fontWeight: 700, color: TRUST.low, marginTop: 4 }}>Low risk · 74 / 100</div>
          </div>
          <span className="iconbtn" style={{ color: "var(--accent-bright)" }}><Icon name="scan-search" size={22} color="var(--accent-bright)" /></span>
        </div>
        <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: ".06em", color: "var(--ink-3)", marginBottom: 8 }}>WHAT THE LENS FOUND</div>
        {[["shield-alert","warning","Tracked changes","2 edits still stored"],["flag","info","Author metadata","J. Becker"]].map(([ic2,sv,t,d])=>(
          <div key={t} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 0" }}>
            <span style={{ width: 4, height: 28, borderRadius: 2, background: SEV[sv] }} />
            <Icon name={ic2} size={17} color={SEV[sv]} />
            <div style={{ flex: 1 }}><div style={{ fontSize: 13, fontWeight: 600 }}>{t}</div><div style={{ fontSize: 11.5, color: "var(--ink-3)" }}>{d}</div></div>
          </div>
        ))}
        <span className="btn btn-primary" style={{ width: "100%", justifyContent: "center", marginTop: 10, background: "var(--accent)", boxShadow: "0 6px 18px -6px rgba(168,85,247,.8)" }}><Icon name="scan-search" size={16} /> Open safely</span>
      </div>
    </div>
  );
}

function Reticle() {
  const c = "var(--accent)";
  const corner = (pos) => ({ position: "absolute", width: 12, height: 12, border: "2px solid " + c, ...pos });
  return (
    <div style={{ position: "absolute", inset: 4, borderRadius: 12, boxShadow: "0 0 0 1px color-mix(in srgb,var(--accent) 35%, transparent)", pointerEvents: "none" }}>
      <span style={corner({ top: -1, left: -1, borderRight: "none", borderBottom: "none", borderRadius: "5px 0 0 0" })} />
      <span style={corner({ top: -1, right: -1, borderLeft: "none", borderBottom: "none", borderRadius: "0 5px 0 0" })} />
      <span style={corner({ bottom: -1, left: -1, borderRight: "none", borderTop: "none", borderRadius: "0 0 0 5px" })} />
      <span style={corner({ bottom: -1, right: -1, borderLeft: "none", borderTop: "none", borderRadius: "0 0 5px 0" })} />
    </div>
  );
}

window.Screens = { ScreenA, ScreenB, ScreenC };
