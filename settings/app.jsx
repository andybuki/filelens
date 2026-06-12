// FileLens — Settings screen (flat MoneyLupe). Faithful to SettingsScreen.kt.
const { useState } = React;

function Toggle({ on, onClick }) {
  return <div className={"toggle" + (on ? " on" : "")} onClick={onClick}><div className="knob" /></div>;
}

function SwitchRow({ label, desc, on, onToggle }) {
  return (
    <div className="row" onClick={onToggle}>
      <div className="rtext">
        <div className="rlabel">{label}</div>
        <div className="rdesc">{desc}</div>
      </div>
      <Toggle on={on} onClick={onToggle} />
    </div>
  );
}

function Segment({ options, value, onSelect }) {
  return (
    <div className="seg-wrap">
      <div className="seg">
        {options.map((o) => (
          <div key={o} className={"opt" + (o === value ? " on" : "")} onClick={() => onSelect(o)}>
            {o === value && <Icon name="check" color="#fff" />}{o}
          </div>
        ))}
      </div>
    </div>
  );
}

function ChoiceRow({ label, value }) {
  return (
    <div className="row static">
      <div className="rtext">
        <div className="rlabel">{label}</div>
        <div className="rvalue">{value}</div>
      </div>
    </div>
  );
}

function DisclosureRow({ icon, label, desc, onClick }) {
  return (
    <div className="row" onClick={onClick}>
      {icon && <span className="rlead"><Icon name={icon} /></span>}
      <div className="rtext">
        <div className="rlabel">{label}</div>
        <div className="rdesc">{desc}</div>
      </div>
      <Icon name="chevron-right" cls="chev" />
    </div>
  );
}

function Settings() {
  const [showHidden, setShowHidden] = useState(false);
  const [hideEmpty, setHideEmpty] = useState(true);
  const [units, setUnits] = useState("Auto");
  const [panel, setPanel] = useState("Single");
  const [toast, setToast] = useState(null);
  const ping = (m) => { setToast(m); clearTimeout(window.__st); window.__st = setTimeout(() => setToast(null), 2400); };

  return (
    <div className="screen">
      <div className="headzone">
        <div className="statusbar">
          <span>14:02</span>
          <span className="right">
            <svg width="17" height="11" viewBox="0 0 17 11" fill="#fff"><rect x="0" y="7" width="3" height="4" rx=".5"/><rect x="5" y="5" width="3" height="6" rx=".5"/><rect x="10" y="2" width="3" height="9" rx=".5"/><rect x="15" y="0" width="3" height="11" rx=".5"/></svg>
            <svg width="16" height="11" viewBox="0 0 16 11" fill="#fff"><path d="M8 2a10 10 0 0 1 7 2.8l-1.4 1.4A8 8 0 0 0 8 4 8 8 0 0 0 2.4 6.2L1 4.8A10 10 0 0 1 8 2Zm0 4a6 6 0 0 1 4.2 1.7L11 9A4 4 0 0 0 8 8a4 4 0 0 0-3 1L3.8 7.7A6 6 0 0 1 8 6Z"/></svg>
            <svg width="25" height="11" viewBox="0 0 25 11"><rect x=".5" y=".5" width="22" height="10" rx="2.3" fill="none" stroke="#fff" strokeWidth="1" opacity=".5"/><rect x="23" y="3.5" width="1.5" height="4" rx=".5" fill="#fff" opacity=".5"/><rect x="2" y="2" width="18" height="7" rx="1.3" fill="#fff"/></svg>
          </span>
        </div>
        <div className="appbar">
          <span className="back" onClick={() => ping("Back to files")}><Icon name="chevron-left" color="#fff" /></span>
          <span className="title">Settings</span>
        </div>
      </div>

      <div className="body">
        {/* BROWSING */}
        <div className="sec-label">Browsing</div>
        <div className="card">
          <SwitchRow label="Show hidden files" desc="Display dotfiles and folders that start with a period." on={showHidden} onToggle={() => setShowHidden(!showHidden)} />
          <div className="divider" />
          <SwitchRow label="Hide empty folders" desc="Don’t show folders that contain no visible files." on={hideEmpty} onToggle={() => setHideEmpty(!hideEmpty)} />
          <div className="divider" />
          <ChoiceRow label="File size units" value={units} />
          <Segment options={["Auto", "Binary", "Decimal"]} value={units} onSelect={setUnits} />
          <div className="divider" />
          <ChoiceRow label="Default panel (tablets)" value={panel} />
          <Segment options={["Single", "Dual"]} value={panel} onSelect={setPanel} />
        </div>

        {/* REMOTE */}
        <div className="sec-label">Remote</div>
        <div className="card">
          <DisclosureRow icon="server" label="Remote servers" desc="Browse FTP, SFTP, and SMB shares like local folders." onClick={() => ping("Open remote servers")} />
        </div>

        {/* BACKUP */}
        <div className="sec-label">Backup &amp; restore</div>
        <div className="card">
          <div className="backup">
            <div className="bdesc">Save your settings, bookmarks, recent files, and frequent folders to a JSON file you can restore later.</div>
            <div className="brow">
              <button className="btn primary" onClick={() => ping("Backup saved")}><Icon name="download" color="#fff" /> Export</button>
              <button className="btn" onClick={() => ping("Choose a backup to import")}><Icon name="upload" /> Import</button>
            </div>
          </div>
        </div>

        {/* ABOUT */}
        <div className="sec-label">About</div>
        <div className="card">
          <div className="row static">
            <div className="rtext">
              <div className="rlabel">FileLens Commander</div>
              <div className="rdesc">Version 0.5.0</div>
            </div>
          </div>
          <div className="divider" />
          <div className="vision">
            <div className="vlabel">Vision</div>
            <div className="vtext">Browse files, peek inside, understand before opening. Offline, on-device, no accounts.</div>
          </div>
          <div className="divider" />
          <DisclosureRow icon="scroll-text" label="Open source licenses" desc="FileLens is built on open source libraries." onClick={() => ping("Show licenses")} />
        </div>

        <div className="foot">
          <span className="mark"><Icon name="scan-search" color="#fff" size={24} /></span>
          <span className="ftext"><b>FileLens</b> · on-device · no tracking</span>
        </div>
      </div>

      {toast && <div className="toast show"><Icon name="check" /> {toast}</div>}
    </div>
  );
}

window.Settings = Settings;
