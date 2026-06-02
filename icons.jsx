// FileLens exploration — inline Lucide-style icon set (stroke 1.75)
const ICON_PATHS = {
  folder: ["M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"],
  "file-text": ["M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z","M14 2v5h5","M8 13h8","M8 17h8","M8 9h2"],
  archive: ["M4 3h16a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z","M4 8v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V8","M10 12h4"],
  image: ["M3 5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z","M9 10a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z","M21 15l-5-5L5 21"],
  braces: ["M8 3H7a2 2 0 0 0-2 2v5a2 2 0 0 1-2 2 2 2 0 0 1 2 2v5a2 2 0 0 0 2 2h1","M16 3h1a2 2 0 0 1 2 2v5a2 2 0 0 0 2 2 2 2 0 0 0-2 2v5a2 2 0 0 1-2 2h-1"],
  "shield-alert": ["M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1Z","M12 8v4","M12 16h.01"],
  "shield-check": ["M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1Z","M9 12l2 2 4-4"],
  "map-pin": ["M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z","M12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z"],
  key: ["M15.5 7.5a4 4 0 1 1-4.6 4.6L4 19v2h3v-2h2v-2h2l1.9-1.9a4 4 0 0 1 2.6-6.6Z","M16.5 7.5h.01"],
  search: ["M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z","M21 21l-4.3-4.3"],
  "scan-search": ["M3 7V5a2 2 0 0 1 2-2h2","M17 3h2a2 2 0 0 1 2 2v2","M21 17v2a2 2 0 0 1-2 2h-2","M7 21H5a2 2 0 0 1-2-2v-2","M11 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z","M16 16l-2.5-2.5"],
  "chevron-right": ["M9 18l6-6-6-6"],
  "chevron-down": ["M6 9l6 6 6-6"],
  "more-vertical": ["M12 5h.01","M12 12h.01","M12 19h.01"],
  "corner-left-up": ["M14 9l-5-5-5 5","M9 4v12a4 4 0 0 0 4 4h7"],
  upload: ["M12 15V3","M7 8l5-5 5 5","M5 21h14"],
  "sliders-horizontal": ["M4 8h10","M18 8h2","M4 16h2","M10 16h10","M14 6v4","M6 14v4"],
  gauge: ["M12 21a9 9 0 1 0-9-9","M12 14l4-4","M3 12h2","M19 12h2","M12 3v2"],
  flag: ["M4 22V4a1 1 0 0 1 1-1h13l-3 5 3 5H5"],
  database: ["M12 8c4.4 0 8-1.3 8-3s-3.6-3-8-3-8 1.3-8 3 3.6 3 8 3Z","M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5","M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"],
};

function Icon({ name, size = 20, color = "currentColor", fill = false, sw = 1.75, style }) {
  const paths = ICON_PATHS[name] || [];
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke={color} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round"
      style={style}>
      {paths.map((d, i) => <path key={i} d={d} fill={fill ? color : "none"} stroke={fill ? "none" : color} />)}
    </svg>
  );
}
window.Icon = Icon;
