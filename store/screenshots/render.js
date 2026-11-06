// Renders captioned Play Store phone screenshots (1080x1920) from real app
// captures in raw/ and the captions in captions.json.
//
//   npm i -g playwright   (once; uses its bundled Chromium)
//   node store/screenshots/render.js
//
// Output: store/screenshots/out/1.png, 2.png, ... in captions.json order.
// Shots whose raw capture is missing are skipped and listed at the end.

const fs = require('fs');
const path = require('path');

function loadPlaywright() {
  try {
    return require('playwright');
  } catch (_) {
    const root = require('child_process').execSync('npm root -g').toString().trim();
    return require(path.join(root, 'playwright'));
  }
}

const dir = __dirname;
const outDir = path.join(dir, 'out');
const { shots } = JSON.parse(fs.readFileSync(path.join(dir, 'captions.json'), 'utf8'));

function escapeHtml(s) {
  return s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
}

function page(shot, dataUri) {
  return `<!doctype html><html><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700;900&display=block" rel="stylesheet">
<style>
  * { margin: 0; box-sizing: border-box; }
  body { width: 1080px; height: 1920px; overflow: hidden; font-family: Roboto, sans-serif;
         background: radial-gradient(120% 70% at 85% 0%, #6D28D9 0%, #3B0F8C 45%, #1E0A4A 100%); }
  .text { position: absolute; top: 120px; left: 80px; right: 80px; text-align: center; color: #fff; }
  h1 { font-size: 84px; line-height: 1.08; font-weight: 900; letter-spacing: -1.5px; }
  p { margin-top: 28px; font-size: 42px; line-height: 1.3; font-weight: 400; color: #DDD6FE; }
  .phone { position: absolute; left: 50%; top: 470px; width: 840px; height: 1560px; transform: translateX(-50%);
           border-radius: 64px; background: #0B0618; padding: 22px;
           box-shadow: 0 40px 120px rgba(0,0,0,.55), 0 0 0 2px rgba(255,255,255,.08); }
  .screen { width: 100%; height: 100%; border-radius: 44px; overflow: hidden; background: #fff;
            background-image: url(${dataUri}); background-size: 100% auto;
            background-position: 50% ${Math.round((shot.offset || 0) * 100)}%; background-repeat: no-repeat; }
</style></head><body>
  <div class="text"><h1>${escapeHtml(shot.title)}</h1><p>${escapeHtml(shot.subtitle)}</p></div>
  <div class="phone"><div class="screen"></div></div>
</body></html>`;
}

(async () => {
  fs.mkdirSync(outDir, { recursive: true });
  for (const f of fs.readdirSync(outDir)) if (f.endsWith('.png')) fs.unlinkSync(path.join(outDir, f));

  const { chromium } = loadPlaywright();
  const proxy = process.env.HTTPS_PROXY ? { server: process.env.HTTPS_PROXY } : undefined;
  const browser = await chromium.launch({ proxy });
  const tab = await browser.newPage({ viewport: { width: 1080, height: 1920 }, ignoreHTTPSErrors: true });

  const missing = [];
  let n = 0;
  for (const shot of shots) {
    const raw = path.join(dir, 'raw', shot.raw);
    if (!fs.existsSync(raw)) { missing.push(shot); continue; }
    const dataUri = 'data:image/png;base64,' + fs.readFileSync(raw).toString('base64');
    await tab.setContent(page(shot, dataUri), { waitUntil: 'networkidle' });
    await tab.evaluate(() => document.fonts.ready);
    n += 1;
    await tab.screenshot({ path: path.join(outDir, `${n}.png`) });
    console.log(`${n}.png  ${shot.title}`);
  }
  await browser.close();

  if (missing.length) {
    console.log('\nStill to capture (save into store/screenshots/raw/):');
    for (const s of missing) console.log(`  ${s.raw}: ${s.todo || ''}`);
  }
})().catch((e) => { console.error(e); process.exit(1); });
