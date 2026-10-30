const fs = require('fs');
const path = require('path');
const { chromium } = require('../web/node_modules/playwright');

const UPLOADED_SOURCE = 'C:/Users/marco/.gemini/antigravity/brain/abf69a1a-21a8-4fdd-830c-a5c76145f9cd/.user_uploaded/media_1793357975168_90244c7c.png';

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

// Function to build a multi-resolution .ico file containing PNG frames
function createIcoFromPngBuffers(pngBuffers) {
  // pngBuffers: array of { width, height, buffer }
  const count = pngBuffers.length;
  const headerSize = 6;
  const entrySize = 16;
  let offset = headerSize + count * entrySize;

  const header = Buffer.alloc(headerSize);
  header.writeUInt16LE(0, 0); // Reserved
  header.writeUInt16LE(1, 2); // Type 1 = ICO
  header.writeUInt16LE(count, 4); // Count

  const entries = [];
  for (const item of pngBuffers) {
    const entry = Buffer.alloc(entrySize);
    entry.writeUInt8(item.width >= 256 ? 0 : item.width, 0);
    entry.writeUInt8(item.height >= 256 ? 0 : item.height, 1);
    entry.writeUInt8(0, 2); // Colors in palette
    entry.writeUInt8(0, 3); // Reserved
    entry.writeUInt16LE(1, 4); // Color planes
    entry.writeUInt16LE(32, 6); // Bits per pixel
    entry.writeUInt32LE(item.buffer.length, 8); // Image data size
    entry.writeUInt32LE(offset, 12); // Image data offset
    entries.push(entry);
    offset += item.buffer.length;
  }

  return Buffer.concat([header, ...entries, ...pngBuffers.map(p => p.buffer)]);
}

(async () => {
  console.log('--- GENERATING BRAND ICONS & FAVICONS ---');

  if (!fs.existsSync(UPLOADED_SOURCE)) {
    throw new Error('Uploaded image not found at ' + UPLOADED_SOURCE);
  }

  const rawBytes = fs.readFileSync(UPLOADED_SOURCE);
  const imgB64 = rawBytes.toString('base64');

  // Copy master to shared/assets/
  ensureDir(path.resolve(__dirname, '../shared/assets'));
  fs.writeFileSync(path.resolve(__dirname, '../shared/assets/brand_logo_source.png'), rawBytes);
  console.log('Saved source image to shared/assets/brand_logo_source.png');

  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage();

  await page.setContent(`
    <!DOCTYPE html>
    <html>
      <head><style>body { margin: 0; background: transparent; }</style></head>
      <body>
        <img id="src" src="data:image/png;base64,${imgB64}" />
      </body>
    </html>
  `);

  await page.waitForSelector('#src');

  // Helper to render on page canvas and export PNG buffer
  async function renderIcon(options) {
    const {
      size,
      paddingPercent = 0.08,
      isRound = false,
      isForegroundOnly = false,
      bgHex = '#0d1a2d',
      cornerRadius = 0,
    } = options;

    const base64Png = await page.evaluate((opts) => {
      const img = document.getElementById('src');
      const cv = document.createElement('canvas');
      cv.width = opts.size;
      cv.height = opts.size;
      const ctx = cv.getContext('2d');
      ctx.imageSmoothingEnabled = true;
      ctx.imageSmoothingQuality = 'high';

      const s = opts.size;

      if (opts.isRound) {
        ctx.beginPath();
        ctx.arc(s / 2, s / 2, s / 2, 0, Math.PI * 2);
        ctx.closePath();
        ctx.clip();
      } else if (opts.cornerRadius > 0) {
        const r = opts.cornerRadius;
        ctx.beginPath();
        ctx.moveTo(r, 0);
        ctx.lineTo(s - r, 0);
        ctx.quadraticCurveTo(s, 0, s, r);
        ctx.lineTo(s, s - r);
        ctx.quadraticCurveTo(s, s, s - r, s);
        ctx.lineTo(r, s);
        ctx.quadraticCurveTo(0, s, 0, s - r);
        ctx.lineTo(0, r);
        ctx.quadraticCurveTo(0, 0, r, 0);
        ctx.closePath();
        ctx.clip();
      }

      if (!opts.isForegroundOnly) {
        ctx.fillStyle = opts.bgHex;
        ctx.fillRect(0, 0, s, s);
      }

      // Compute bounding draw rect
      const maxDim = Math.max(img.naturalWidth, img.naturalHeight);
      const targetContentSize = s * (1 - opts.paddingPercent * 2);
      const scale = targetContentSize / maxDim;
      const drawW = img.naturalWidth * scale;
      const drawH = img.naturalHeight * scale;
      const dx = (s - drawW) / 2;
      const dy = (s - drawH) / 2;

      if (!opts.isForegroundOnly) {
        ctx.drawImage(img, dx, dy, drawW, drawH);
      } else {
        // Render to temp canvas to remove background #0d1a2d
        const temp = document.createElement('canvas');
        temp.width = img.naturalWidth;
        temp.height = img.naturalHeight;
        const tctx = temp.getContext('2d');
        tctx.drawImage(img, 0, 0);
        const imgData = tctx.getImageData(0, 0, temp.width, temp.height);
        const data = imgData.data;

        // Background color is #0d1a2d (R:13, G:26, B:45)
        const bgR = 13, bgG = 26, bgB = 45;
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];
          const diff = Math.sqrt((r - bgR) ** 2 + (g - bgG) ** 2 + (b - bgB) ** 2);
          if (diff < 22) {
            data[i + 3] = 0;
          } else if (diff < 45) {
            // Smooth edge alpha
            data[i + 3] = Math.round(((diff - 22) / 23) * 255);
          }
        }
        tctx.putImageData(imgData, 0, 0);
        ctx.drawImage(temp, dx, dy, drawW, drawH);
      }

      return cv.toDataURL('image/png').split(',')[1];
    }, { size, paddingPercent, isRound, isForegroundOnly, bgHex, cornerRadius });

    return Buffer.from(base64Png, 'base64');
  }

  // 1. Generate Web Icons
  console.log('\nGenerating Web icons in web/public/...');
  ensureDir(path.resolve(__dirname, '../web/public/icons'));
  ensureDir(path.resolve(__dirname, '../web/public/assets'));

  const pwa512 = await renderIcon({ size: 512, paddingPercent: 0.08 });
  const pwa192 = await renderIcon({ size: 192, paddingPercent: 0.08 });
  const pwaMaskable512 = await renderIcon({ size: 512, paddingPercent: 0.16 });
  const pwaMaskable192 = await renderIcon({ size: 192, paddingPercent: 0.16 });
  const appleTouchIcon = await renderIcon({ size: 180, paddingPercent: 0.08, cornerRadius: 28 });
  const favicon48 = await renderIcon({ size: 48, paddingPercent: 0.05 });
  const favicon32 = await renderIcon({ size: 32, paddingPercent: 0.05 });
  const favicon16 = await renderIcon({ size: 16, paddingPercent: 0.05 });
  const logoTransparent512 = await renderIcon({ size: 512, paddingPercent: 0.08, isForegroundOnly: true });

  fs.writeFileSync(path.resolve(__dirname, '../web/public/icons/icon-512.png'), pwa512);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/icons/icon-192.png'), pwa192);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/icons/icon-maskable-512.png'), pwaMaskable512);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/icons/icon-maskable-192.png'), pwaMaskable192);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/apple-touch-icon.png'), appleTouchIcon);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/favicon.png'), favicon32);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/assets/logo.png'), pwa512);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/assets/logo-transparent.png'), logoTransparent512);

  // Build favicon.ico
  const icoBuffer = createIcoFromPngBuffers([
    { width: 16, height: 16, buffer: favicon16 },
    { width: 32, height: 32, buffer: favicon32 },
    { width: 48, height: 48, buffer: favicon48 },
  ]);
  fs.writeFileSync(path.resolve(__dirname, '../web/public/favicon.ico'), icoBuffer);
  console.log('Generated favicon.ico with 16x16, 32x32, 48x48 mipmaps.');

  // Create an SVG wrapper for high quality scalable icon
  const pwa512B64 = pwa512.toString('base64');
  const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <rect width="512" height="512" rx="96" fill="#0D1A2D"/>
  <image href="data:image/png;base64,${pwa512B64}" width="512" height="512"/>
</svg>`;
  fs.writeFileSync(path.resolve(__dirname, '../web/public/icon.svg'), svgContent, 'utf8');
  fs.writeFileSync(path.resolve(__dirname, '../web/public/favicon.svg'), svgContent, 'utf8');
  console.log('Generated web/public/icon.svg and favicon.svg.');

  // 2. Generate Android Launcher Icons
  console.log('\nGenerating Android APK Launcher icons in native/app/src/main/res/...');
  const androidDensities = [
    { name: 'mipmap-mdpi', size: 48, round: 48 },
    { name: 'mipmap-hdpi', size: 72, round: 72 },
    { name: 'mipmap-xhdpi', size: 96, round: 96 },
    { name: 'mipmap-xxhdpi', size: 144, round: 144 },
    { name: 'mipmap-xxxhdpi', size: 192, round: 192 },
  ];

  for (const d of androidDensities) {
    const dir = path.resolve(__dirname, '../native/app/src/main/res', d.name);
    ensureDir(dir);

    // Standard square / legacy icon
    const squarePng = await renderIcon({ size: d.size, paddingPercent: 0.08, cornerRadius: Math.round(d.size * 0.18) });
    fs.writeFileSync(path.join(dir, 'ic_launcher.png'), squarePng);

    // Round icon
    const roundPng = await renderIcon({ size: d.round, paddingPercent: 0.08, isRound: true });
    fs.writeFileSync(path.join(dir, 'ic_launcher_round.png'), roundPng);
  }

  // Adaptive Icon Foreground (432x432: safe zone 264x264 in center -> padding 0.18)
  const drawableDir = path.resolve(__dirname, '../native/app/src/main/res/drawable');
  ensureDir(drawableDir);

  const adaptiveFg = await renderIcon({
    size: 432,
    paddingPercent: 0.18,
    isForegroundOnly: true,
  });
  fs.writeFileSync(path.join(drawableDir, 'ic_launcher_foreground.png'), adaptiveFg);
  fs.writeFileSync(path.join(drawableDir, 'app_logo.png'), pwa512);

  // Background color XML
  const valuesDir = path.resolve(__dirname, '../native/app/src/main/res/values');
  ensureDir(valuesDir);

  const colorsXmlPath = path.join(valuesDir, 'colors.xml');
  let colorsXml = `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#0D1A2D</color>\n    <color name="openkit_primary">#0F52BA</color>\n    <color name="openkit_secondary">#00C49F</color>\n    <color name="openkit_bg">#0F141C</color>\n</resources>\n`;
  fs.writeFileSync(colorsXmlPath, colorsXml, 'utf8');

  // Adaptive icon XMLs (anydpi-v26)
  const anydpiDir = path.resolve(__dirname, '../native/app/src/main/res/mipmap-anydpi-v26');
  ensureDir(anydpiDir);

  const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:color="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
`;
  fs.writeFileSync(path.join(anydpiDir, 'ic_launcher.xml'), adaptiveXml, 'utf8');
  fs.writeFileSync(path.join(anydpiDir, 'ic_launcher_round.xml'), adaptiveXml, 'utf8');

  // Google Play store 512x512 icon
  const playstoreIcon = await renderIcon({ size: 512, paddingPercent: 0.08 });
  fs.writeFileSync(path.resolve(__dirname, '../native/app/src/main/ic_launcher-playstore.png'), playstoreIcon);

  await browser.close();
  console.log('✅ ALL ICONS SUCCESSFULLY GENERATED!');
})();

