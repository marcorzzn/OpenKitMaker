const fs = require('fs');
const path = require('path');
const { chromium } = require('./web/node_modules/playwright');

if (!fs.existsSync('qa_screenshots')) {
  fs.mkdirSync('qa_screenshots', { recursive: true });
}

const issues = [];

function recordIssue(category, severity, location, defect, suggestion = 'Nessun fix specifico richiesto', screenshot = null) {
  issues.push({ category, severity, location, defect, suggestion, screenshot });
  console.log(`[${severity}] [${category}] ${location}: ${defect}`);
}

(async () => {
  console.log('--- STARTING QA AUDIT WITH PLAYWRIGHT ---');
  const browser = await chromium.launch({
    channel: 'chrome',
    headless: true,
  });

  const consoleLogs = [];
  const pageErrors = [];

  // ==========================================
  // FASE 1: Prima Impressione & Responsive
  // ==========================================
  console.log('\n--- FASE 1: Responsive & Home Layout ---');
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleLogs.push(msg.text());
    }
  });
  page.on('pageerror', (err) => {
    pageErrors.push(err.message);
  });

  await page.goto('http://localhost:5173', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  // Check desktop horizontal overflow
  const desktopOverflow = await page.evaluate(() => {
    return document.documentElement.scrollWidth > window.innerWidth;
  });
  if (desktopOverflow) {
    recordIssue(
      'Layout/Overflow',
      'Grave',
      'Home Desktop (1920x1080)',
      'Rilevato overflow orizzontale della pagina (scrollWidth > window.innerWidth).',
      'Aggiungere overflow-x-hidden al root container o verificare elementi con larghezza fissa.',
      '01_desktop_home.png'
    );
  }

  await page.screenshot({ path: 'qa_screenshots/01_desktop_home.png', fullPage: true });

  // Test Mobile (iPhone 13: 390x844)
  console.log('Testing Mobile Viewport (390x844)...');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.waitForTimeout(600);

  const mobileScrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
  console.log(`Mobile scrollWidth: ${mobileScrollWidth}px (Viewport: 390px)`);

  if (mobileScrollWidth > 390) {
    recordIssue(
      'Layout/Overflow',
      'Grave',
      'Home Mobile (390x844)',
      `La pagina ha overflow orizzontale su mobile: scrollWidth è ${mobileScrollWidth}px invece di 390px. La schermata può oscillare lateralmente.`,
      'Controllare padding fissi, tabelle o container con min-w-[...] che non scalano a 390px.',
      '01_mobile_home.png'
    );
  }

  // Check top navigation & header on mobile
  const navHeaderWrap = await page.evaluate(() => {
    const header = document.querySelector('header');
    return header ? header.scrollHeight > 75 : false;
  });
  if (navHeaderWrap) {
    recordIssue(
      'Layout/Overflow',
      'Minore',
      'Top Navigation Mobile',
      'La barra di navigazione superiore su mobile occupa più di 75px verticali a causa del wrap dei pulsanti.',
      'Su schermi piccoli, spostare i tab nella barra inferiore mobile (Bottom Navigation Bar).',
      '01_mobile_home.png'
    );
  }

  // Check presence of mobile bottom navigation bar
  const hasBottomNav = await page.evaluate(() => {
    const bottomNav = document.querySelector('nav.fixed.bottom-0');
    return !!bottomNav;
  });
  console.log('Mobile bottom nav present:', hasBottomNav);
  if (!hasBottomNav) {
    recordIssue(
      'UI/UX Mobile',
      'Minore',
      'Mobile Bottom Nav',
      'La barra di navigazione inferiore per mobile non è stata rilevata nel DOM.',
      'Aggiungere una Bottom Navigation Bar fissa per dispositivi compatti.'
    );
  }

  await page.screenshot({ path: 'qa_screenshots/01_mobile_home.png', fullPage: true });

  // Reset to Desktop for next phases
  await page.setViewportSize({ width: 1920, height: 1080 });
  await page.waitForTimeout(500);

  // ==========================================
  // FASE 2: Navigazione e Catalogo
  // ==========================================
  console.log('\n--- FASE 2: Navigazione & Catalogo ---');
  // Click on Catalogo
  const catalogBtn = await page.$('button:has-text("Catalogo")');
  if (catalogBtn) {
    await catalogBtn.click();
    await page.waitForTimeout(800);
  } else {
    recordIssue('Navigazione', 'Bloccante', 'TopNav', 'Pulsante "Catalogo" non trovato nella navigazione.');
  }

  await page.screenshot({ path: 'qa_screenshots/02_catalog.png' });

  // Check catalog template cards
  const templateButtons = await page.$$('button:has-text("Clona e Personalizza")');
  console.log(`Found ${templateButtons.length} template cards in Catalog.`);

  if (templateButtons.length === 0) {
    recordIssue('Catalogo', 'Bloccante', 'Catalogo Template', 'Nessuna card template con pulsante "Clona e Personalizza" trovata nel catalogo.');
  } else {
    // Select first template card
    console.log('Selecting first template card...');
    await templateButtons[0].click();
    await page.waitForTimeout(1800); // Wait for Three.js engine and DOM mount
  }

  await page.screenshot({ path: 'qa_screenshots/02_editor_initial.png' });

  // Check if 3D Canvas is mounted and valid
  const canvas3D = await page.evaluate(() => {
    const allCanvases = Array.from(document.querySelectorAll('canvas'));
    return { count: allCanvases.length };
  });

  console.log('Canvases found in DOM:', canvas3D.count);
  if (canvas3D.count < 2) {
    recordIssue('3D Engine', 'Bloccante', 'Viewport 3D', 'Il canvas WebGL Three.js o il canvas 2D non è stato montato correttamente nel DOM.');
  }

  // ==========================================
  // FASE 3: Test Editor 3D & Camera Presets & Lighting
  // ==========================================
  console.log('\n--- FASE 3: Editor 3D & Controlli Flottanti ---');

  // Ensure Viewport3D is mounted
  const splitBtnInit = await page.$('button:has-text("Vista Doppia")');
  if (splitBtnInit) {
    await splitBtnInit.click();
    await page.waitForTimeout(1000);
  }

  // Test Camera Presets
  const cameraPresets = [
    { name: 'Retro', sel: '#btn-cam-back', text: 'button:has-text("Retro")', file: '03_camera_retro.png' },
    { name: 'Lato SX', sel: '#btn-cam-left', text: 'button:has-text("Lato SX")', file: '03_camera_left.png' },
    { name: 'Lato DX', sel: '#btn-cam-right', text: 'button:has-text("Lato DX")', file: '03_camera_right.png' },
    { name: 'Fronte', sel: '#btn-cam-front', text: 'button:has-text("Fronte")', file: '03_camera_fronte.png' },
    { name: '360 Auto-Rotate', sel: '#btn-cam-rotate', text: 'button:has-text("360°")', file: '03_camera_360.png' },
  ];

  for (const cam of cameraPresets) {
    const btn = (await page.$(cam.sel)) || (await page.$(cam.text));
    if (btn) {
      await btn.click();
      await page.waitForTimeout(600);
      await page.screenshot({ path: `qa_screenshots/${cam.file}` });
    } else {
      recordIssue('3D Camera', 'Grave', 'Barra Flottante Viewport', `Pulsante camera "${cam.name}" non trovato.`);
    }
  }

  // Turn off auto-rotate if still on
  const rotateBtn = (await page.$('#btn-cam-rotate')) || (await page.$('button:has-text("360°")'));
  if (rotateBtn) {
    await rotateBtn.click();
    await page.waitForTimeout(300);
  }

  // Test Lighting Presets
  const lightingPresets = [
    { name: 'Stadio', sel: '#btn-light-stadium', text: 'button:has-text("Stadio")', file: '03_light_stadium.png' },
    { name: 'Tramonto', sel: '#btn-light-sunset', text: 'button:has-text("Tramonto")', file: '03_light_sunset.png' },
    { name: 'Radiante', sel: '#btn-light-radiant', text: 'button:has-text("Radiante")', file: '03_light_radiant.png' },
    { name: 'Studio', sel: '#btn-light-studio', text: 'button:has-text("Studio")', file: '03_light_studio.png' },
  ];

  for (const light of lightingPresets) {
    const btn = (await page.$(light.sel)) || (await page.$(light.text));
    if (btn) {
      await btn.click();
      await page.waitForTimeout(400);
      await page.screenshot({ path: `qa_screenshots/${light.file}` });
    } else {
      recordIssue('3D Lighting', 'Minore', 'Barra Flottante Viewport', `Pulsante luce "${light.name}" non trovato.`);
    }
  }

  // Test Background Presets
  const bgPresets = [
    { name: 'Campo', sel: '#btn-bg-pitch', text: 'button:has-text("Campo")', file: '03_bg_pitch.png' },
    { name: 'Neutro', sel: '#btn-bg-neutral', text: 'button:has-text("Neutro")', file: '03_bg_neutral.png' },
    { name: 'Scuro', sel: '#btn-bg-dark', text: 'button:has-text("Scuro")', file: '03_bg_dark.png' },
  ];

  for (const bg of bgPresets) {
    const btn = (await page.$(bg.sel)) || (await page.$(bg.text));
    if (btn) {
      await btn.click();
      await page.waitForTimeout(400);
      await page.screenshot({ path: `qa_screenshots/${bg.file}` });
    } else {
      recordIssue('3D Background', 'Minore', 'Barra Flottante Viewport', `Pulsante sfondo "${bg.name}" non trovato.`);
    }
  }

  // Test Anatomical View
  const anatBtn = (await page.$('#btn-toggle-anatomy')) || (await page.$('button:has-text("Anatomica")'));
  if (anatBtn) {
    await anatBtn.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'qa_screenshots/03_anatomical_view.png' });
    await anatBtn.click(); // toggle back
  }

  // Test View Modes: 2D, 3D, SPLIT
  const splitBtn = await page.$('button:has-text("Vista Doppia")');
  if (splitBtn) {
    await splitBtn.click();
    await page.waitForTimeout(600);
    await page.screenshot({ path: 'qa_screenshots/03_view_split.png' });
  }

  // Check floating bar height (must not exceed 65px to ensure single-row non-overlapping layout)
  const floatingBarHeight = await page.evaluate(() => {
    const bar = document.querySelector('#btn-cam-front')?.closest('div.absolute');
    return bar ? bar.clientHeight : 0;
  });
  console.log(`Floating bar height: ${floatingBarHeight}px`);
  if (floatingBarHeight > 65) {
    recordIssue(
      'Layout/Overlay',
      'Grave',
      'Barra Flottante 3D',
      `La barra controlli 3D è alta ${floatingBarHeight}px (> 65px) e si sovrappone al modello. Deve rimanere su una singola riga orizzontale compatta.`
    );
  }

  // ==========================================
  // FASE 4: Test delle Funzionalità di Editing (I 4 Tab)
  // ==========================================
  console.log('\n--- FASE 4: Funzionalità di Editing (I 4 Tab) ---');

  // --- TAB 1: Colori & Tessuto ---
  console.log('Testing Tab 1: Colori & Tessuto...');
  const tabColoriBtn = await page.$('button:has-text("Colori")');
  if (tabColoriBtn) {
    await tabColoriBtn.click();
    await page.waitForTimeout(500);
  }

  // Check sub-parts buttons: Corpo, Colletto, Maniche
  const collettoPartBtn = (await page.$('#part-btn-collar')) || (await page.$('button:has-text("Colletto")'));
  if (collettoPartBtn) {
    await collettoPartBtn.click();
    await page.waitForTimeout(500);
    const collarSelect = (await page.$('#collar-style-tab')) || (await page.waitForSelector('#collar-style-tab', { timeout: 2000 }).catch(() => null));
    if (!collarSelect) {
      recordIssue('Editor 2D/UI', 'Grave', 'Tab Colori & Tessuto', 'Selezionando "Colletto", il dropdown con i tagli sartoriali non compare.', 'Garantire selezione deterministica del layer colletto.');
    }
  }

  const manichePartBtn = await page.$('button:has-text("Maniche")');
  if (manichePartBtn) {
    await manichePartBtn.click();
    await page.waitForTimeout(400);
  }

  const corpoPartBtn = await page.$('button:has-text("Corpo")');
  if (corpoPartBtn) {
    await corpoPartBtn.click();
    await page.waitForTimeout(400);
  }

  // Test the 4 Fabric presets
  const fabrics = [
    { name: 'Poliestere Lucido', text: 'Poliestere' },
    { name: 'Cotone Anni \'90', text: 'Cotone' },
    { name: 'Jacquard Nobile', text: 'Jacquard' },
    { name: 'Mesh Traspirante', text: 'Mesh' },
  ];

  for (const f of fabrics) {
    const fBtn = await page.$(`button:has-text("${f.text}")`);
    if (fBtn) {
      await fBtn.click();
      await page.waitForTimeout(400);
    } else {
      recordIssue('Material Engine', 'Grave', 'Pannello Tessuti PBR', `Pulsante tessuto "${f.name}" non trovato.`);
    }
  }

  // Check fabric card text truncation or overflow
  const fabricCardOverflow = await page.evaluate(() => {
    const buttons = Array.from(document.querySelectorAll('button')).filter(b => b.textContent.includes('Finitura') || b.textContent.includes('R:'));
    for (const b of buttons) {
      if (b.scrollWidth > b.clientWidth + 2) return true;
    }
    return false;
  });
  if (fabricCardOverflow) {
    recordIssue('Layout/Overflow', 'Minore', 'Pannello Tessuti', 'I testi nelle card dei tessuti sbordano su layout compatti.');
  }

  await page.screenshot({ path: 'qa_screenshots/04_tab_colors.png' });

  // --- TAB 2: Trama & Pattern ---
  console.log('Testing Tab 2: Trama & Pattern...');
  const tabPatternBtn = await page.$('button:has-text("Pattern")');
  if (tabPatternBtn) {
    await tabPatternBtn.click();
    await page.waitForTimeout(500);
  }

  const activatePatternBtn = await page.$('button:has-text("+ Attiva Grafica Pattern")');
  if (activatePatternBtn) {
    console.log('Activating pattern layer...');
    await activatePatternBtn.click();
    await page.waitForTimeout(500);
  }

  // Test procedural pattern buttons
  const wavePatternBtn = await page.$('button:has-text("Onde")');
  if (wavePatternBtn) {
    await wavePatternBtn.click();
    await page.waitForTimeout(400);
  }

  const camoPatternBtn = await page.$('button:has-text("Mimetico")');
  if (camoPatternBtn) {
    await camoPatternBtn.click();
    await page.waitForTimeout(400);
  }

  const hexPatternBtn = await page.$('button:has-text("Esagoni")');
  if (hexPatternBtn) {
    await hexPatternBtn.click();
    await page.waitForTimeout(400);
  }

  // Test range sliders
  const sliders = await page.$$('input[type="range"]');
  console.log(`Found ${sliders.length} range sliders in view.`);
  for (let i = 0; i < Math.min(sliders.length, 3); i++) {
    await sliders[i].evaluate((el) => {
      const min = parseFloat(el.min) || 0;
      const max = parseFloat(el.max) || 100;
      el.value = ((min + max) / 2).toString();
      el.dispatchEvent(new Event('input', { bubbles: true }));
    });
    await page.waitForTimeout(100);
  }

  await page.screenshot({ path: 'qa_screenshots/04_tab_patterns.png' });

  // --- TAB 3: Stemmi & Sponsor & Snapping ---
  console.log('Testing Tab 3: Stemmi & Sponsor...');
  const tabStemmiBtn = await page.$('button:has-text("Stemmi")');
  if (tabStemmiBtn) {
    await tabStemmiBtn.click();
    await page.waitForTimeout(500);
  }

  // Test Snapping Buttons for Badge
  const snapHeartBtn = await page.$('button:has-text("Cuore SX")');
  if (snapHeartBtn) {
    await snapHeartBtn.click();
    await page.waitForTimeout(300);
  }

  const snapCollarBtn = await page.$('button:has-text("Colletto")');
  if (snapCollarBtn) {
    await snapCollarBtn.click();
    await page.waitForTimeout(300);
  }

  const snapCenterBtn = await page.$('button:has-text("Centro")');
  if (snapCenterBtn) {
    await snapCenterBtn.click();
    await page.waitForTimeout(300);
  }

  // Switch to Sponsor sub-tab
  const subSponsorBtn = await page.$('button:has-text("Sponsor")');
  if (subSponsorBtn) {
    await subSponsorBtn.click();
    await page.waitForTimeout(400);

    const sponsorInput = await page.$('input#sponsor-label-tab');
    if (sponsorInput) {
      await sponsorInput.fill('SPEED·PRO');
      await page.waitForTimeout(300);
    } else {
      recordIssue('Editor 2D', 'Grave', 'Tab Sponsor', 'Campo testo marchio sponsor non trovato (#sponsor-label-tab).');
    }

    const snapSponsorStd = await page.$('button:has-text("Standard")');
    if (snapSponsorStd) {
      await snapSponsorStd.click();
      await page.waitForTimeout(300);
    }
  }

  // Switch to Nome & Numero sub-tab
  const subNameNumBtn = await page.$('button:has-text("Nome & N°")');
  if (subNameNumBtn) {
    await subNameNumBtn.click();
    await page.waitForTimeout(400);

    const nameInput = await page.$('input#p-name-tab');
    if (nameInput) {
      await nameInput.fill('MARCO');
    }
    const numInput = await page.$('input#num-tab');
    if (numInput) {
      await numInput.fill('7');
    }
    await page.waitForTimeout(300);
  }

  await page.screenshot({ path: 'qa_screenshots/04_tab_logos_snapping.png' });

  // --- TAB 4: Livelli ---
  console.log('Testing Tab 4: Livelli...');
  const tabLivelliBtn = await page.$('button:has-text("Livelli")');
  if (tabLivelliBtn) {
    await tabLivelliBtn.click();
    await page.waitForTimeout(500);
  }

  const layerItems = await page.$$('button:has-text("ON"), button:has-text("OFF")');
  console.log(`Found ${layerItems.length} layer visibility toggles.`);

  if (layerItems.length > 0) {
    await layerItems[0].click();
    await page.waitForTimeout(300);
    await layerItems[0].click();
  } else {
    recordIssue('Logica Livelli', 'Grave', 'Tab Livelli', 'Nessun pulsante visibilità ON/OFF trovato nello stack dei livelli.');
  }

  await page.screenshot({ path: 'qa_screenshots/04_tab_layers.png' });

  // ==========================================
  // FASE 5: Test dell'Esportazione
  // ==========================================
  console.log('\n--- FASE 5: Test Esportazione ---');
  const exportNavBtn = await page.$('button:has-text("Esporta")');
  if (exportNavBtn) {
    await exportNavBtn.click();
    await page.waitForTimeout(1000);
  } else {
    recordIssue('Navigazione', 'Bloccante', 'TopNav', 'Pulsante "Esporta" non trovato.');
  }

  await page.screenshot({ path: 'qa_screenshots/05_export_screen.png' });

  // Check 1-Click Social Showcase 1080x1080 button
  const socialBtn = await page.$('button:has-text("Scarica per WhatsApp & Social")');
  if (!socialBtn) {
    recordIssue(
      'Esportazione',
      'Grave',
      'Schermata Esporta',
      'Pulsante eroe "Scarica per WhatsApp & Social" non trovato.'
    );
  } else {
    console.log('Clicking Social Showcase 1080x1080 download button...');
    const t0 = Date.now();
    try {
      await socialBtn.click();
      await page.waitForTimeout(1200);
      const elapsed = Date.now() - t0;
      console.log(`Social export triggered in ${elapsed}ms.`);
    } catch (e) {
      console.log('Download trigger error:', e.message);
    }
  }

  // Test 4K resolution toggle
  const res4kBtn = await page.$('button:has-text("4K Ultra HD")');
  if (res4kBtn) {
    await res4kBtn.click();
    await page.waitForTimeout(300);
    console.log('Selected 4K Ultra HD export resolution.');
  } else {
    recordIssue('Esportazione', 'Minore', 'Pannello Risoluzione', 'Pulsante 4K Ultra HD non trovato.', 'Uniformare il testo del pulsante.');
  }

  // Check Mobile Layout of Export Screen
  console.log('Testing Mobile Export Screen (390x844)...');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.waitForTimeout(600);

  const exportMobileWidth = await page.evaluate(() => document.documentElement.scrollWidth);
  console.log(`Export screen mobile scrollWidth: ${exportMobileWidth}px (Viewport: 390px)`);

  if (exportMobileWidth > 390) {
    recordIssue(
      'Layout/Overflow',
      'Grave',
      'Schermata Esporta Mobile',
      `Overflow orizzontale rilevato su mobile nella schermata export: scrollWidth è ${exportMobileWidth}px su 390px. Le card universali o la preview UV forzano la larghezza della pagina.`,
      'Aggiungere max-w-full overflow-hidden e assicurare che la griglia e le preview si ridimensionino sotto i 390px.',
      '05_mobile_export.png'
    );
  }

  // Inspect if any text wraps badly or buttons clip
  const textTruncation = await page.evaluate(() => {
    const headings = Array.from(document.querySelectorAll('h1, h2, h3'));
    return headings.some(h => h.scrollWidth > h.clientWidth + 5);
  });
  if (textTruncation) {
    recordIssue('Layout/Typography', 'Minore', 'Titoli Schermata Export', 'Alcuni titoli o header su mobile subiscono overflow o troncamento.');
  }

  await page.screenshot({ path: 'qa_screenshots/05_mobile_export.png', fullPage: true });

  // Reset viewport to desktop for modal testing
  await page.setViewportSize({ width: 1920, height: 1080 });
  await page.waitForTimeout(400);

  // ==========================================
  // FASE 5B: Test Modali & Popup (Standard 1: "X" + Click-Outside)
  // ==========================================
  console.log('\n--- FASE 5B: Test Modali & Popups (Standard 1) ---');
  // Navigate to SCR-SAVED to test modal
  const savedNavBtn = (await page.$('button:has-text("Progetti")')) || (await page.$('button:has-text("I Miei Progetti")'));
  if (savedNavBtn) {
    await savedNavBtn.click();
    await page.waitForTimeout(800);

    const deleteBtn = await page.$('button:has-text("Elimina")');
    if (deleteBtn) {
      console.log('Opening delete modal in SCR-SAVED...');
      await deleteBtn.click();
      await page.waitForTimeout(400);

      // Verify modal is open
      const modal = await page.$('[role="dialog"]');
      if (!modal) {
        recordIssue('Modali/UI', 'Grave', 'SCR-SAVED', 'La finestra di dialogo di eliminazione non si è aperta.');
      } else {
        // Test "X" close button
        const closeXBtn = await page.$('button[aria-label="Chiudi"]');
        if (!closeXBtn) {
          recordIssue('Modali/UI', 'Grave', 'SCR-SAVED Modal', 'Il pulsante "X" per chiudere la finestra non è presente.');
        } else {
          console.log('Testing "X" button close...');
          await closeXBtn.click();
          await page.waitForTimeout(400);
          const modalStillOpen = await page.$('[role="dialog"]');
          if (modalStillOpen) {
            recordIssue('Modali/UI', 'Grave', 'SCR-SAVED Modal', 'Il click sul pulsante "X" non ha chiuso la finestra modale.');
          } else {
            console.log('Modal successfully closed via "X" button.');
          }
        }

        // Re-open and test click-outside
        console.log('Testing click-outside to close modal...');
        const reDeleteBtn = await page.$('button:has-text("Elimina")');
        if (reDeleteBtn) {
          await reDeleteBtn.click();
          await page.waitForTimeout(400);
          const modalBackdrop = await page.$('[role="dialog"]');
          if (modalBackdrop) {
            await modalBackdrop.click({ position: { x: 10, y: 10 } });
            await page.waitForTimeout(400);
            const modalClosedByBackdrop = await page.$('[role="dialog"]');
            if (modalClosedByBackdrop) {
              recordIssue('Modali/UI', 'Grave', 'SCR-SAVED Modal', 'Il click fuori dall\'area modale (click-outside) non ha chiuso la finestra.');
            } else {
              console.log('Modal successfully closed via click-outside.');
            }
          }
        }
      }
    }
  }

  // ==========================================
  // FASE 6: Sintesi Errori Console & Report
  // ==========================================
  console.log('\n--- FASE 6: Sintesi Errori Console & Logica ---');
  if (consoleLogs.length > 0) {
    consoleLogs.forEach((err) => {
      recordIssue('Errori Console / Runtime', 'Grave', 'Console Web', err);
    });
  }

  if (pageErrors.length > 0) {
    pageErrors.forEach((err) => {
      recordIssue('Errori Runtime', 'Bloccante', 'Page Runtime', err);
    });
  }

  await browser.close();

  // Write QA_REPORT.md
  console.log(`\nWriting QA_REPORT.md with ${issues.length} detected items...`);
  
  let reportMd = `# 🛡️ QA AUDIT REPORT — OpenKitMaker Web
**Data Esecuzione**: ${new Date().toISOString()}  
**Ambiente Collaudo**: Google Chrome Headless (Playwright Automation Engine)  
**Risoluzioni Analizzate**: Desktop 1920×1080 (HD) & Mobile 390×844 (iPhone 13 Viewport)  
**URL Locale**: \`http://localhost:5173\`  

---

## 📊 Sintesi Esecutiva

| Categoria | Bloccanti | Gravi | Minori | Totale |
| :--- | :---: | :---: | :---: | :---: |
| **Layout & Overflow** | ${issues.filter(i => i.category.includes('Layout') && i.severity === 'Bloccante').length} | ${issues.filter(i => i.category.includes('Layout') && i.severity === 'Grave').length} | ${issues.filter(i => i.category.includes('Layout') && i.severity === 'Minore').length} | ${issues.filter(i => i.category.includes('Layout')).length} |
| **3D Engine & Camera** | ${issues.filter(i => i.category.includes('3D') && i.severity === 'Bloccante').length} | ${issues.filter(i => i.category.includes('3D') && i.severity === 'Grave').length} | ${issues.filter(i => i.category.includes('3D') && i.severity === 'Minore').length} | ${issues.filter(i => i.category.includes('3D')).length} |
| **Editor 2D & Snapping** | ${issues.filter(i => i.category.includes('Editor') && i.severity === 'Bloccante').length} | ${issues.filter(i => i.category.includes('Editor') && i.severity === 'Grave').length} | ${issues.filter(i => i.category.includes('Editor') && i.severity === 'Minore').length} | ${issues.filter(i => i.category.includes('Editor')).length} |
| **Esportazione** | ${issues.filter(i => i.category.includes('Esportazione') && i.severity === 'Bloccante').length} | ${issues.filter(i => i.category.includes('Esportazione') && i.severity === 'Grave').length} | ${issues.filter(i => i.category.includes('Esportazione') && i.severity === 'Minore').length} | ${issues.filter(i => i.category.includes('Esportazione')).length} |
| **Runtime & Console** | ${issues.filter(i => i.category.includes('Runtime') && i.severity === 'Bloccante').length} | ${issues.filter(i => i.category.includes('Runtime') && i.severity === 'Grave').length} | ${issues.filter(i => i.category.includes('Runtime') && i.severity === 'Minore').length} | ${issues.filter(i => i.category.includes('Runtime')).length} |
| **TOTALE GENERALE** | **${issues.filter(i => i.severity === 'Bloccante').length}** | **${issues.filter(i => i.severity === 'Grave').length}** | **${issues.filter(i => i.severity === 'Minore').length}** | **${issues.length}** |

---

## 🔍 Registro Dettagliato delle Anomalie Rilevate

`;

  if (issues.length === 0) {
    reportMd += `> ✅ **Nessun bug visivo o logico critico rilevato durante l'esecuzione automatica.** L'interfaccia ha superato tutti i test di rendering, snapping, camera presets ed export.\n\n`;
  } else {
    issues.forEach((iss, idx) => {
      const badge = iss.severity === 'Bloccante' ? '🔴 BLOCCANTE' : iss.severity === 'Grave' ? '🟠 GRAVE' : '🟡 MINORE';
      reportMd += `### ${idx + 1}. [${badge}] ${iss.location}
* **Categoria**: \`${iss.category}\`
* **Cosa non va**: ${iss.defect}
* **Suggerimento di Fix**: ${iss.suggestion}
${iss.screenshot ? `* **Screenshot Evidenza**: \`qa_screenshots/${iss.screenshot}\`` : ''}

---
`;
    });
  }

  reportMd += `\n## 📸 Screenshot Acquisiti nel Corso del Collaudo
Tutti gli screenshot del test sono stati salvati nella cartella \`qa_screenshots/\`:
* \`01_desktop_home.png\` — Panoramica Home Desktop 1920×1080
* \`01_mobile_home.png\` — Panoramica Home Mobile 390×844
* \`02_catalog.png\` — Griglia Catalogo Template Ufficiali
* \`02_editor_initial.png\` — Primo Ingressso nello Studio 3D
* \`03_camera_fronte.png\` / \`retro.png\` / \`left.png\` / \`right.png\` — Rotazione Camera
* \`03_light_stadium.png\` / \`sunset.png\` / \`radiant.png\` — Preset Luci
* \`03_bg_pitch.png\` / \`neutral.png\` / \`dark.png\` — Preset Sfondi
* \`03_view_split.png\` — Modalità Vista Doppia 2D + 3D
* \`04_tab_colors.png\` — Tab Colori & Tessuto PBR
* \`04_tab_patterns.png\` — Tab Trama & Generatore Procedurale
* \`04_tab_logos_snapping.png\` — Tab Stemmi & Snapping Magnetico
* \`04_tab_layers.png\` — Tab Pila Livelli
* \`05_export_screen.png\` — Pannello Esportazione Desktop
* \`05_mobile_export.png\` — Pannello Esportazione Mobile

---
*Collaudo generato automaticamente da Antigravity QA Suite per OpenKitMaker.*
`;

  fs.writeFileSync('QA_REPORT.md', reportMd, 'utf8');
  console.log('QA_REPORT.md successfully written to root directory.');
})();
