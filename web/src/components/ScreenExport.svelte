// Implements: SCR-EXPORT / FUN-EXPORT-PNG, FUN-EXPORT-ZIP, FUN-EXPORT-SINGLE, STEP 4
<script lang="ts">
  import { onMount } from 'svelte';
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import {
    compositeGarment2048,
    renderFullKitMockup512,
  } from '../lib/compositor2d';
  import {
    exportGarmentPng,
    exportSocialMockup1080,
    exportKitInteroperableZip,
    exportKitPbrBundleZip,
    exportKitGlb,
    importKitFromZip,
  } from '../lib/exporter';
  import { saveKitToDb, loadAllKitsFromDb } from '../lib/db';
  import type { KitManifestV1, GarmentType, ExportResolution } from '../types/kit';

  let flatTextureCanvas = $state<HTMLCanvasElement | null>(null);
  let fullKitPreviewCanvas = $state<HTMLCanvasElement | null>(null);
  let activePreviewGarment = $state<GarmentType>('JERSEY');
  let selectedResolution = $state<ExportResolution>(2048);
  let exportProgress = $state<number>(0);
  let isExporting = $state<boolean>(false);
  let lastExportSummary = $state<{
    type: 'PNG' | 'ZIP' | 'PBR' | 'GLB';
    filename: string;
    sha256?: string;
    sizeKb?: number;
    manifest?: KitManifestV1;
  } | null>(null);
  let zipFileInput = $state<HTMLInputElement | null>(null);

  function renderExportPreviews() {
    if (!flatTextureCanvas || !fullKitPreviewCanvas) return;
    compositeGarment2048(flatTextureCanvas, studio.activeKit, activePreviewGarment, { showUvGuides: false });
    renderFullKitMockup512(fullKitPreviewCanvas, studio.activeKit);
  }

  $effect(() => {
    if (activePreviewGarment && flatTextureCanvas) {
      compositeGarment2048(flatTextureCanvas, studio.activeKit, activePreviewGarment, { showUvGuides: false });
    }
  });

  onMount(() => {
    renderExportPreviews();
  });

  async function handleExportSingleGarment(garmentType: GarmentType) {
    if (isExporting) return;
    isExporting = true;
    exportProgress = 10;
    try {
      const { bytes, sha256, filename } = await exportGarmentPng(
        studio.activeKit,
        garmentType,
        selectedResolution,
        (pct) => {
          exportProgress = pct;
        }
      );
      lastExportSummary = {
        type: 'PNG',
        filename,
        sha256,
        sizeKb: Math.round(bytes.byteLength / 1024),
      };
      studio.notify(
        'success',
        `Texture PNG ${selectedResolution}×${selectedResolution} (${garmentType}) esportata con successo.`
      );
    } catch {
      studio.notify('error', i18n.errors.export_failed);
    } finally {
      isExporting = false;
    }
  }

  async function handleExportSocial() {
    if (isExporting) return;
    isExporting = true;
    exportProgress = 15;
    try {
      const { bytes, filename } = await exportSocialMockup1080(
        studio.activeKit,
        (pct) => {
          exportProgress = pct;
        }
      );
      lastExportSummary = {
        type: 'PNG',
        filename,
        sizeKb: Math.round(bytes.byteLength / 1024),
      };
      studio.notify(
        'success',
        'Immagine Social 1080×1080 scaricata con successo! Pronta da condividere su WhatsApp e Instagram.'
      );
    } catch {
      studio.notify('error', i18n.errors.export_failed);
    } finally {
      isExporting = false;
    }
  }

  async function handleExportZip() {
    if (isExporting) return;
    isExporting = true;
    exportProgress = 5;
    try {
      const { manifest, zipSizeBytes } = await exportKitInteroperableZip(
        studio.activeKit,
        (pct) => {
          exportProgress = pct;
        }
      );
      lastExportSummary = {
        type: 'ZIP',
        filename: `kit_${studio.activeKit.id}.zip`,
        sha256: manifest.checksums.textureSha256,
        sizeKb: Math.round(zipSizeBytes / 1024),
        manifest,
      };
      studio.notify(
        'success',
        `Archivio Full Kit kit_${studio.activeKit.id.slice(0, 8)}.zip generato (Maglia, Shorts, Calzettoni, Manifest).`
      );
    } catch {
      studio.notify('error', i18n.errors.export_failed);
    } finally {
      isExporting = false;
    }
  }

  async function handleExportPbrBundle() {
    if (isExporting) return;
    isExporting = true;
    exportProgress = 5;
    try {
      const { zipSizeBytes, filename } = await exportKitPbrBundleZip(
        studio.activeKit,
        (pct) => {
          exportProgress = pct;
        }
      );
      lastExportSummary = {
        type: 'PBR',
        filename,
        sizeKb: Math.round(zipSizeBytes / 1024),
      };
      studio.notify(
        'success',
        `Bundle Materiali PBR ${filename} generato (Albedo, Normal, Roughness, Metallic).`
      );
    } catch {
      studio.notify('error', i18n.errors.export_failed);
    } finally {
      isExporting = false;
    }
  }

  async function handleExportGlb() {
    if (isExporting) return;
    isExporting = true;
    exportProgress = 5;
    try {
      const { blob, filename } = await exportKitGlb(
        studio.activeKit,
        (pct) => {
          exportProgress = pct;
        }
      );
      lastExportSummary = {
        type: 'GLB',
        filename,
        sizeKb: Math.round(blob.size / 1024),
      };
      studio.notify(
        'success',
        `Modello 3D ${filename} (.glb) esportato con texture e materiali integrati.`
      );
    } catch {
      studio.notify('error', i18n.errors.export_failed);
    } finally {
      isExporting = false;
    }
  }

  async function handleImportZipVerify(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    try {
      const { kit, manifest, checksumVerified } = await importKitFromZip(file);
      await saveKitToDb(kit);
      studio.savedKits = await loadAllKitsFromDb();
      studio.openSavedKit(kit, 'SCR-EXPORT');
      renderExportPreviews();
      lastExportSummary = {
        type: 'ZIP',
        filename: file.name,
        sha256: manifest.checksums?.textureSha256,
        sizeKb: Math.round(file.size / 1024),
        manifest,
      };
      studio.notify(
        'success',
        `Importazione Full Kit ZIP completata (${checksumVerified ? 'Checksum SHA-256 Verificato' : 'Manifest Valido'}).`
      );
    } catch (err: any) {
      studio.notify('error', err?.message || i18n.errors.import_failed);
    } finally {
      input.value = '';
    }
  }
</script>

<section class="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-10 space-y-8 overflow-hidden">
  <!-- Header (SCR-EXPORT) -->
  <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#2E3D56] pb-6">
    <div class="space-y-1.5">
      <div class="text-xs text-[#00C49F] font-mono-tabular">
        ⚡ ESPORTAZIONE UNIVERSALE & CONDIVISIONE
      </div>
      <h1 class="text-3xl sm:text-4xl font-bold font-display tracking-wide text-white">
        {i18n.export.title}
      </h1>
      <p class="text-sm text-[#94A3B8]">
        Kit attivo: <span class="text-white font-semibold">{studio.activeKit.name}</span>
        <span class="ml-2 px-2 py-0.5 rounded-full bg-[#1A2230] text-[#00C49F] text-xs font-mono-tabular border border-[#2E3D56]">
          Tessuto: {studio.fabricType}
        </span>
      </p>
    </div>

    <div class="flex items-center gap-3">
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-EDITOR')}
        class="px-4 py-2 text-xs font-semibold text-[#94A3B8] hover:text-white bg-[#1A2230] border border-[#2E3D56] rounded-lg cursor-pointer whitespace-nowrap"
      >
        ← Torna all'Editor
      </button>
      <button
        type="button"
        onclick={() => zipFileInput?.click()}
        class="px-4 py-2 text-xs font-semibold text-white bg-[#253146] hover:bg-[#2E3D56] border border-[#2E3D56] rounded-lg cursor-pointer whitespace-nowrap"
      >
        📥 Importa File .ZIP
      </button>
      <input
        bind:this={zipFileInput}
        type="file"
        accept=".zip,application/zip"
        onchange={handleImportZipVerify}
        class="hidden"
      />
    </div>
  </div>

  <!-- Dual Previews: Texture Distesa 2048x2048 (con selettore capo) + Mockup Full Kit 512x512 -->
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
    <!-- Texture Distesa 2048x2048 -->
    <div class="lg:col-span-7 bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-col justify-between">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-3">
        <div class="flex items-center gap-2">
          <span class="text-white text-xs font-semibold">Texture UV Distesa</span>
          <span class="text-[#00C49F] text-xs font-mono-tabular">2048×2048 px · RGBA</span>
        </div>

        <!-- Garment Texture Tabs -->
        <div class="flex items-center gap-1 bg-[#0F141C] p-1 rounded-lg border border-[#2E3D56]">
          <button
            type="button"
            onclick={() => activePreviewGarment = 'JERSEY'}
            class="px-2.5 py-1 text-xs rounded font-medium transition-colors cursor-pointer {activePreviewGarment === 'JERSEY' ? 'bg-[#0F52BA] text-white font-semibold' : 'text-[#94A3B8] hover:text-white'}"
          >
            👕 Maglia
          </button>
          <button
            type="button"
            onclick={() => activePreviewGarment = 'SHORTS'}
            class="px-2.5 py-1 text-xs rounded font-medium transition-colors cursor-pointer {activePreviewGarment === 'SHORTS' ? 'bg-[#0F52BA] text-white font-semibold' : 'text-[#94A3B8] hover:text-white'}"
          >
            🩳 Shorts
          </button>
          <button
            type="button"
            onclick={() => activePreviewGarment = 'SOCKS'}
            class="px-2.5 py-1 text-xs rounded font-medium transition-colors cursor-pointer {activePreviewGarment === 'SOCKS' ? 'bg-[#0F52BA] text-white font-semibold' : 'text-[#94A3B8] hover:text-white'}"
          >
            🧦 Calzettoni
          </button>
        </div>
      </div>

      <div class="bg-[#0F141C] border border-[#2E3D56] rounded-lg p-3 flex items-center justify-center aspect-square max-h-[440px]">
        <canvas
          bind:this={flatTextureCanvas}
          width="2048"
          height="2048"
          class="w-full h-full object-contain rounded"
        ></canvas>
      </div>
      <div class="text-[11px] text-[#64748B] font-mono-tabular mt-2 text-center">
        Visualizzazione texture attiva: <span class="text-white">{activePreviewGarment.toLowerCase()}.png</span> (layout UV clean-room)
      </div>
    </div>

    <!-- Mockup Full Kit 512x512 -->
    <div class="lg:col-span-5 bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-col justify-between">
      <div class="flex items-center justify-between text-xs text-[#94A3B8] font-mono-tabular mb-3">
        <span class="text-white font-semibold">Mockup Full Kit (`preview.png`)</span>
        <span class="text-[#00C49F]">512×512 px · RGBA</span>
      </div>

      <div class="bg-[#0F141C] border border-[#2E3D56] rounded-lg p-3 flex items-center justify-center aspect-square max-h-[440px]">
        <canvas
          bind:this={fullKitPreviewCanvas}
          width="512"
          height="512"
          class="w-full h-full object-contain rounded"
        ></canvas>
      </div>
      <div class="text-[11px] text-[#64748B] font-mono-tabular mt-2 text-center">
        Composizione fotorealistica 2D di Maglia + Shorts + Calzettoni
      </div>
    </div>
  </div>

  <!-- Progress Bar (Required by SCR-EXPORT) -->
  {#if isExporting || exportProgress > 0}
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-4 space-y-2">
      <div class="flex items-center justify-between text-xs font-mono-tabular">
        <span class="text-white">
          {isExporting
            ? 'Elaborazione in corso: upscaling vettoriale, bundling PBR o compilazione GLB 3D…'
            : 'Operazione di esportazione completata al 100%'}
        </span>
        <span class="text-[#00C49F] font-semibold">{exportProgress}%</span>
      </div>
      <div class="w-full h-2 bg-[#0F141C] rounded-full overflow-hidden">
        <div
          class="h-full bg-[#00C49F] transition-all duration-200"
          style="width: {exportProgress}%;"
        ></div>
      </div>
    </div>
  {/if}

  <!-- 1-Click Social & WhatsApp Showcase Banner (Hero Action) -->
  <div class="bg-gradient-to-r from-[#0F52BA]/25 via-[#1A2230] to-[#00C49F]/20 border-2 border-[#00C49F]/50 rounded-2xl p-5 sm:p-7 flex flex-col sm:flex-row sm:items-center justify-between gap-6 shadow-xl shadow-[#00C49F]/10">
    <div class="space-y-2">
      <div class="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-[#0F141C] text-[#00C49F] text-xs font-mono-tabular border border-[#00C49F]/40 font-semibold">
        <span>📱</span> CONDIVISIONE ISTANTANEA
      </div>
      <h2 class="text-xl sm:text-2xl font-bold font-display text-white">
        Scarica Immagine per WhatsApp & Social (1080×1080 HD)
      </h2>
      <p class="text-sm text-[#94A3B8] max-w-2xl leading-relaxed">
        Genera in un istante una grafica quadrata ad alta risoluzione con la divisa completa (Maglia + Pantaloncini + Calzettoni), sfondo da stadio e dettagli del kit. Perfetta da inviare nelle chat di squadra o pubblicare su Instagram e TikTok.
      </p>
    </div>

    <button
      type="button"
      disabled={isExporting}
      onclick={handleExportSocial}
      class="px-5 sm:px-6 py-3.5 sm:py-4 rounded-xl bg-gradient-to-r from-[#00C49F] to-[#10B981] hover:from-[#10B981] hover:to-[#059669] text-[#0F141C] font-extrabold text-sm sm:text-base transition-all shadow-lg shadow-[#00C49F]/30 hover:scale-102 cursor-pointer whitespace-nowrap shrink-0 flex items-center justify-center gap-2"
    >
      <span>📲</span>
      <span>Scarica per WhatsApp & Social</span>
    </button>
  </div>

  <!-- Universal Export Grid: 4 Formati Professionali (STEP 4) -->
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <!-- Format 1: Texture 2D ad Altissima Risoluzione (2K / 4K) -->
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-6 flex flex-col justify-between space-y-5">
      <div class="space-y-3">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div class="text-xs text-[#94A3B8] font-mono-tabular">EXPORT 2D · PNG LOSSLESS</div>
          <!-- Resolution Selector -->
          <div class="flex items-center gap-1 bg-[#0F141C] p-1 rounded-lg border border-[#2E3D56]">
            <button
              type="button"
              onclick={() => selectedResolution = 2048}
              class="px-2.5 py-1 text-xs rounded font-medium transition-colors cursor-pointer {selectedResolution === 2048 ? 'bg-[#0F52BA] text-white font-semibold' : 'text-[#94A3B8] hover:text-white'}"
            >
              2K (2048×2048)
            </button>
            <button
              type="button"
              onclick={() => selectedResolution = 4096}
              class="px-2.5 py-1 text-xs rounded font-medium transition-colors cursor-pointer {selectedResolution === 4096 ? 'bg-[#00C49F] text-[#0F141C] font-bold' : 'text-[#94A3B8] hover:text-white'}"
            >
              ✨ 4K Ultra HD (4096×4096)
            </button>
          </div>
        </div>

        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          Esporta Texture 2D ({selectedResolution}×{selectedResolution})
        </h2>
        <p class="text-sm text-[#94A3B8] leading-relaxed">
          Scarica la texture grafica lossless in formato <strong>{selectedResolution === 4096 ? '4K Ultra HD (4096×4096)' : '2K Standard (2048×2048)'}</strong> con campionamento ad alta fedeltà, ideale per videogame, stampa o mockup.
        </p>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
        <button
          type="button"
          disabled={isExporting}
          onclick={() => handleExportSingleGarment('JERSEY')}
          class="py-2.5 px-3 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] disabled:opacity-50 rounded-lg transition-colors cursor-pointer text-center"
        >
          👕 Maglia {selectedResolution === 4096 ? '4K' : '2K'}
        </button>
        <button
          type="button"
          disabled={isExporting}
          onclick={() => handleExportSingleGarment('SHORTS')}
          class="py-2.5 px-3 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] disabled:opacity-50 rounded-lg transition-colors cursor-pointer text-center"
        >
          🩳 Shorts {selectedResolution === 4096 ? '4K' : '2K'}
        </button>
        <button
          type="button"
          disabled={isExporting}
          onclick={() => handleExportSingleGarment('SOCKS')}
          class="py-2.5 px-3 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] disabled:opacity-50 rounded-lg transition-colors cursor-pointer text-center"
        >
          🧦 Calze {selectedResolution === 4096 ? '4K' : '2K'}
        </button>
      </div>
    </div>

    <!-- Format 2: Interoperable Full Kit ZIP Archive (FUN-EXPORT-ZIP) -->
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-6 flex flex-col justify-between space-y-5">
      <div class="space-y-2">
        <div class="text-xs text-[#00C49F] font-mono-tabular">
          INTEROPERABILE · CROSS-PLATFORM WEB ↔ ANDROID
        </div>
        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          {i18n.export.option_zip_title}
        </h2>
        <p class="text-sm text-[#94A3B8] leading-relaxed">
          {i18n.export.option_zip_desc}
        </p>
        <div class="text-xs text-[#64748B] font-mono-tabular pt-1">
          Contiene: jersey.png · shorts.png · socks.png · preview.png · manifest.json
        </div>
      </div>

      <button
        type="button"
        disabled={isExporting}
        onclick={handleExportZip}
        class="w-full py-3 px-5 text-sm font-semibold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] disabled:opacity-50 rounded-lg transition-colors cursor-pointer"
      >
        📦 Esporta Archivio Full Kit (.ZIP)
      </button>
    </div>

    <!-- Format 3: PBR Materials Bundle (STEP 4) -->
    <div class="bg-[#1A2230] border border-[#8B5CF6]/50 rounded-xl p-6 flex flex-col justify-between space-y-5">
      <div class="space-y-2">
        <div class="text-xs text-[#A78BFA] font-mono-tabular">
          MATERIAL ENGINE · TESSUTO PBR WORKFLOW
        </div>
        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          Bundle Materiali PBR (.ZIP)
        </h2>
        <p class="text-sm text-[#94A3B8] leading-relaxed">
          Genera un pacchetto professionale con mappe di rilievo e riflessione per <strong>Blender, Unity, Unreal Engine o Maya</strong>:
        </p>
        <div class="text-xs text-[#A78BFA] font-mono-tabular bg-[#0F141C] p-2.5 rounded-lg border border-[#2E3D56] space-y-1">
          <div>• <strong>albedo.png</strong>: Texture colore 2048x2048 (Maglia, Shorts, Calze)</div>
          <div>• <strong>normal.png</strong>: Normal map micro-trama {studio.fabricType} (1024x1024)</div>
          <div>• <strong>roughness.png</strong>: Mappa ruvidità calibrata</div>
          <div>• <strong>metallic.png</strong>: Mappa speculare metallica</div>
        </div>
      </div>

      <button
        type="button"
        disabled={isExporting}
        onclick={handleExportPbrBundle}
        class="w-full py-3 px-5 text-sm font-semibold text-white bg-[#7C3AED] hover:bg-[#6D28D9] disabled:opacity-50 rounded-lg transition-colors cursor-pointer"
      >
        🎨 Esporta Bundle Materiali PBR (.ZIP)
      </button>
    </div>

    <!-- Format 4: Standalone 3D Model (.GLB) (STEP 4) -->
    <div class="bg-[#1A2230] border border-[#38BDF8]/50 rounded-xl p-6 flex flex-col justify-between space-y-5">
      <div class="space-y-2">
        <div class="text-xs text-[#38BDF8] font-mono-tabular">
          UNIVERSAL 3D ASSET · GLTF BINARY CONTAINER
        </div>
        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          Modello 3D Kit (.GLB)
        </h2>
        <p class="text-sm text-[#94A3B8] leading-relaxed">
          Scarica il modello tridimensionale completo del kit in formato <strong>glTF Binary (.glb)</strong> con geometria, coordinate UV, normal map e texture già collegate e pronte all'uso.
        </p>
        <div class="text-xs text-[#64748B] font-mono-tabular pt-1">
          Compatibile al 100% con Blender, Three.js, Godot, Windows 3D Viewer e motori di gioco.
        </div>
      </div>

      <button
        type="button"
        disabled={isExporting}
        onclick={handleExportGlb}
        class="w-full py-3 px-5 text-sm font-semibold text-[#0F141C] bg-[#38BDF8] hover:bg-[#0284C7] disabled:opacity-50 rounded-lg transition-colors cursor-pointer"
      >
        🧊 Scarica Modello 3D Kit (.GLB)
      </button>
    </div>
  </div>

  <!-- Completion & Manifest Verification Panel -->
  {#if lastExportSummary}
    <div class="bg-[#1A2230] border border-[#10B981]/50 rounded-xl p-6 space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div class="text-sm font-semibold text-[#10B981]">
          Esportazione Completata: {lastExportSummary.filename} ({lastExportSummary.sizeKb} KB)
        </div>
        {#if lastExportSummary.sha256}
          <div class="text-xs text-[#94A3B8] font-mono-tabular">
            SHA-256: {lastExportSummary.sha256.slice(0, 24)}…
          </div>
        {/if}
      </div>

      {#if lastExportSummary.manifest}
        <details class="bg-[#0F141C] border border-[#2E3D56] rounded-lg p-3">
          <summary class="text-xs font-mono-tabular text-[#00C49F] cursor-pointer">
            Ispeziona manifest.json generato (Schema kit-manifest-v1.json)
          </summary>
          <pre class="mt-3 text-xs font-mono-tabular text-[#94A3B8] overflow-x-auto max-h-64">{JSON.stringify(
              lastExportSummary.manifest,
              null,
              2
            )}</pre>
        </details>
      {/if}
    </div>
  {/if}
</section>
