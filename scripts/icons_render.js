// 知牛 · TDesign SVG → PNG（20px 设计格，DPR4 高清渲染，透明底）
// 用法：scripts/icons_build.sh 下载 SVG 到 /tmp/tdesign_svg 后执行本脚本。
// Playwright 解析顺序：仓库 node_modules → 本机 npx 缓存（约定回退）。
let chromium;
try {
  ({ chromium } = require('playwright'));
} catch {
  ({ chromium } = require('/Users/c14h14n3/.npm/_npx/31e32ef8478fbf80/node_modules/playwright'));
}
const fs = require('fs');
const path = require('path');

const SRC = process.env.TDESIGN_SVG_DIR || '/tmp/tdesign_svg';
const OUT = path.resolve(__dirname, '../kuikly-shell/shared/src/commonMain/assets/common/icons');
const CHROME = process.env.CHROME_PATH ||
  '/Users/c14h14n3/Library/Caches/ms-playwright/chromium-1234/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing';

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: CHROME, args: ['--no-sandbox', '--disable-gpu'] });
  const ctx = await browser.newContext({ viewport: { width: 120, height: 120 }, deviceScaleFactor: 4 });
  const page = await ctx.newPage();
  const files = fs.readdirSync(SRC).filter(f => f.endsWith('.svg'));
  for (const f of files) {
    const svg = fs.readFileSync(path.join(SRC, f), 'utf8');
    const html = `<!DOCTYPE html><html><head><style>
      html,body{margin:0;padding:0;background:transparent}
      svg{display:block;width:20px;height:20px;filter:brightness(0) saturate(100%) invert(45%) sepia(8%) saturate(430%) hue-rotate(174deg) brightness(91%) contrast(88%)}
    </style></head><body>${svg}</body></html>`;
    await page.setContent(html, { waitUntil: 'load' });
    await page.waitForTimeout(60);
    const el = await page.$('svg');
    const name = f.replace('.svg', '');
    await el.screenshot({
      path: path.join(OUT, `${name}.png`),
      omitBackground: true,
    });
    console.log('rendered', name + '.png');
  }
  await browser.close();
  console.log('icons done ->', OUT);
})().catch(e => { console.error('FATAL', e); process.exit(1); });
