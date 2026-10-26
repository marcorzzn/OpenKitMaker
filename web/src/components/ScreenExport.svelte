// Implements: SCR-EXPORT / FUN-EXPORT-PNG, FUN-EXPORT-ZIP
<script lang="ts">
  import { onMount } from 'svelte';
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import {
    compositeKit2048,
    renderMinikitPreview512,
  } from '../lib/compositor2d';
  import {
    exportKitPng2048,
    exportKitInteroperableZip,
    importKitFromZip,
  } from '../lib/exporter';
  import { saveKitToDb, loadAllKitsFromDb } from '../lib/db';
  import type { KitManifestV1 } from '../types/kit';

  let flatTextureCanvas = $state<HTMLCanvasElement | null>(null);
  let minikitPreviewCanvas = $state<HTMLCanvasElement | null>(null);
  let exportProgress = $state<number>(0);
  let isExporting = $state<boolean>(false);
  let lastExportSummary = $state<{
    type: 'PNG' | 'ZIP';
    filename: string;
    sha256?: string;
    sizeKb?: number;
    manifest?: KitManifestV1;
  } | null>(null);
  let zipFileInput = $state<HTMLInputElement | null>(null);

  function renderExportPreviews() {
    if (!flatTextureCanvas || !minikitPreviewCanvas) return;
    compositeKit2048(flatTextureCanvas, studio.activeKit, { showUvGuides: false });
    renderMinikitPreview512(minikitPreviewCanvas, flatTextureCanvas, studio.activeKit);
  }

  onMount(() => {
    renderExportPreviews();
  });

  async function handleExportPng() {
    if (isExporting) return;
    isExporting = true;
    exportProgress = 5;
    try {
      const { bytes, sha256 } = await exportKitPng2048(studio.activeKit, (pct) => {
        exportProgress = pct;
      });
      lastExportSummary = {
        type: 'PNG',
        filename: `texture_${studio.activeKit.id.slice(0, 8)}_2048.png`,
        sha256,
        sizeKb: Math.round(bytes.byteLength / 1024),
      };
      studio.notify('success', 'Texture PNG 2048×2048 lossless esportata con successo.');
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
        `Archivio interoperabile kit_${studio.activeKit.id.slice(0, 8)}.zip generato.`
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
        `Importazione ZIP completata (${checksumVerified ? 'Checksum SHA-256 Verificato' : 'Manifest Valido'}).`
      );
    } catch (err: any) {
      studio.notify('error', err?.message || i18n.errors.import_failed);
    } finally {
      input.value = '';
    }
  }
</script>

<section class="max-w-7xl mx-auto px-6 py-10 space-y-8">
  <!-- Header (SCR-EXPORT) -->
  <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#2E3D56] pb-6">
    <div class="space-y-1.5">
      <div class="text-xs text-[#94A3B8] font-mono-tabular">
        SCR-EXPORT · FUN-EXPORT-PNG · FUN-EXPORT-ZIP · Interoperabilità Web ↔ Android
      </div>
      <h1 class="text-3xl sm:text-4xl font-bold font-display tracking-wide text-white">
        {i18n.export.title}
      </h1>
      <p class="text-sm text-[#94A3B8]">
        Kit attivo: <span class="text-white font-semibold">{studio.activeKit.name}</span>
        <span class="font-mono-tabular text-xs text-[#64748B]">({studio.activeKit.id})</span>
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
        Verifica / Importa ZIP (TEST-PAR-06)
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

  <!-- Dual Previews required by SCR-EXPORT: Texture Distesa 2048x2048 + Minikit 512x512 -->
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
    <div class="lg:col-span-7 bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-col justify-between">
      <div class="flex items-center justify-between text-xs text-[#94A3B8] font-mono-tabular mb-3">
        <span class="text-white font-semibold">Anteprima Texture Distesa (`texture.png`)</span>
        <span class="text-[#00C49F]">2048×2048 px · RGBA 32-bit Lossless</span>
      </div>

      <div class="bg-[#0F141C] border border-[#2E3D56] rounded-lg p-3 flex items-center justify-center aspect-square max-h-[440px]">
        <canvas
          bind:this={flatTextureCanvas}
          width="2048"
          height="2048"
          class="w-full h-full object-contain rounded"
        ></canvas>
      </div>
    </div>

    <div class="lg:col-span-5 bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-col justify-between">
      <div class="flex items-center justify-between text-xs text-[#94A3B8] font-mono-tabular mb-3">
        <span class="text-white font-semibold">Anteprima Minikit (`preview.png`)</span>
        <span class="text-[#00C49F]">512×512 px · RGBA</span>
      </div>

      <div class="bg-[#0F141C] border border-[#2E3D56] rounded-lg p-3 flex items-center justify-center aspect-square max-h-[440px]">
        <canvas
          bind:this={minikitPreviewCanvas}
          width="512"
          height="512"
          class="w-full h-full object-contain rounded"
        ></canvas>
      </div>
    </div>
  </div>

  <!-- Progress Bar (Required by SCR-EXPORT) -->
  {#if isExporting || exportProgress > 0}
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-4 space-y-2">
      <div class="flex items-center justify-between text-xs font-mono-tabular">
        <span class="text-white">
          {isExporting
            ? 'Compositing 2048×2048, calcolo SHA-256 e compressione in corso…'
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

  <!-- Output Options: PNG 2048x2048 vs Interoperable ZIP -->
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <!-- Option 1: PNG 2048x2048 (FUN-EXPORT-PNG) -->
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-6 flex flex-col justify-between space-y-5">
      <div class="space-y-2">
        <div class="text-xs text-[#94A3B8] font-mono-tabular">FUN-EXPORT-PNG · Output Diretto</div>
        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          {i18n.export.option_png_title}
        </h2>
        <p class="text-sm text-[#94A3B8] leading-relaxed">
          {i18n.export.option_png_desc}
        </p>
      </div>

      <button
        type="button"
        disabled={isExporting}
        onclick={handleExportPng}
        class="w-full py-3 px-5 text-sm font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] disabled:opacity-50 rounded-lg transition-colors cursor-pointer"
      >
        Scarica Texture PNG (2048×2048)
      </button>
    </div>

    <!-- Option 2: Interoperable ZIP Archive (FUN-EXPORT-ZIP) -->
    <div class="bg-[#1A2230] border border-[#00C49F]/50 rounded-xl p-6 flex flex-col justify-between space-y-5">
      <div class="space-y-2">
        <div class="text-xs text-[#00C49F] font-mono-tabular">
          FUN-EXPORT-ZIP · Contratto di Parità Web ↔ Android
        </div>
        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          {i18n.export.option_zip_title}
        </h2>
        <p class="text-sm text-[#94A3B8] leading-relaxed">
          {i18n.export.option_zip_desc}
        </p>
        <div class="text-xs text-[#64748B] font-mono-tabular pt-1">
          Contiene: texture.png (2048×2048) · preview.png (512×512) · manifest.json
        </div>
      </div>

      <button
        type="button"
        disabled={isExporting}
        onclick={handleExportZip}
        class="w-full py-3 px-5 text-sm font-semibold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] disabled:opacity-50 rounded-lg transition-colors cursor-pointer"
      >
        Esporta Archivio kit_{studio.activeKit.id.slice(0, 8)}.zip
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
            SHA-256 Texture: {lastExportSummary.sha256.slice(0, 24)}…
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
