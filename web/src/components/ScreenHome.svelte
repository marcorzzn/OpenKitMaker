// Implements: SCR-HOME / FUN-SELECT-TEMPLATE, FUN-SAVE-LOCAL, FUN-OFFLINE-MODE
<script lang="ts">
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import { BUILTIN_TEMPLATES } from '../data/templates';
  import { importKitFromZip } from '../lib/exporter';
  import { saveKitToDb, loadAllKitsFromDb } from '../lib/db';

  let fileInputRef = $state<HTMLInputElement | null>(null);

  function formatDate(iso: string): string {
    try {
      return new Intl.DateTimeFormat('it-IT', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(iso));
    } catch {
      return iso;
    }
  }

  async function handleZipFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    try {
      const { kit, checksumVerified } = await importKitFromZip(file);
      await saveKitToDb(kit);
      studio.savedKits = await loadAllKitsFromDb();
      studio.openSavedKit(kit, 'SCR-EDITOR');
      studio.notify(
        'success',
        `Kit "${kit.name}" importato da ZIP (${checksumVerified ? 'SHA-256 verificato' : 'manifest v1.0.0'}).`
      );
    } catch (err: any) {
      studio.notify('error', err?.message || i18n.errors.import_failed);
    } finally {
      input.value = '';
    }
  }
</script>

<section class="max-w-7xl mx-auto px-6 py-10 space-y-12">
  <!-- Hero Dashboard Banner (SCR-HOME) -->
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch bg-[#1A2230] border border-[#2E3D56] rounded-2xl p-8">
    <div class="lg:col-span-7 flex flex-col justify-between space-y-6">
      <div class="space-y-3">
        <div class="flex items-center gap-2 text-xs text-[#94A3B8] font-mono-tabular">
          <span>OpenKitMaker-Web v1.0.0</span>
          <span aria-hidden="true">·</span>
          <span>Target A+B (Svelte 5 + Three.js + PWA)</span>
          <span aria-hidden="true">·</span>
          <span class={studio.isOnline ? 'text-[#00C49F]' : 'text-[#F59E0B]'}>
            {studio.isOnline ? 'Stato: Connesso (Cache Offline Attiva)' : i18n.app.offline_badge}
          </span>
        </div>

        <h1 class="text-4xl sm:text-5xl font-bold font-display tracking-wide text-white leading-tight" style="text-wrap: balance;">
          Studio Sartoriale 2D & 3D per Divise Sportive Interoperabili
        </h1>

        <p class="text-base text-[#94A3B8] max-w-2xl leading-relaxed">
          Progetta texture tecniche a risoluzione nativa <span class="text-white font-mono-tabular">2048×2048 px</span>,
          ispeziona il modello 3D con illuminazione PBR Studio/Stadio a 60 FPS e condividi pacchetti
          <span class="text-white font-mono-tabular">kit_&lt;id&gt;.zip</span> compatibili al 100% con OpenKitMaker-Native Android.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-4 pt-2">
        <!-- Main CTA required by SCR-HOME -->
        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-CATALOG')}
          class="px-6 py-3 text-sm font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer shadow-sm"
        >
          {i18n.home.cta_new_kit}
        </button>

        <!-- Quick Access required by SCR-HOME -->
        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-SAVED')}
          class="px-5 py-3 text-sm font-semibold text-white bg-[#253146] hover:bg-[#2E3D56] border border-[#2E3D56] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
        >
          {i18n.home.cta_all_kits} ({studio.savedKits.length})
        </button>

        <!-- Cross-platform ZIP import (TEST-PAR-06) -->
        <button
          type="button"
          onclick={() => fileInputRef?.click()}
          class="px-5 py-3 text-sm font-medium text-[#94A3B8] hover:text-white bg-transparent hover:bg-[#253146] border border-[#2E3D56] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
        >
          {i18n.home.cta_import_zip}
        </button>
        <input
          bind:this={fileInputRef}
          type="file"
          accept=".zip,application/zip"
          onchange={handleZipFileChange}
          class="hidden"
        />
      </div>
    </div>

    <!-- Geometric Live Preview Showcase Card -->
    <div class="lg:col-span-5 bg-[#0F141C] border border-[#2E3D56] rounded-xl p-6 flex flex-col justify-between">
      <div class="flex items-center justify-between text-xs text-[#94A3B8]">
        <span>Bozza Attiva nello Studio</span>
        <span class="font-mono-tabular text-[#00C49F]">{studio.activeKit.category} · 2048×2048 RGBA</span>
      </div>

      <div class="my-4 flex items-center justify-center">
        {#if studio.activeKit.metadata?.thumbnailDataUrl}
          <img
            src={studio.activeKit.metadata.thumbnailDataUrl}
            alt="Anteprima minikit {studio.activeKit.name}"
            class="w-56 h-56 object-contain rounded-lg"
          />
        {:else}
          <svg viewBox="0 0 240 240" class="w-52 h-52">
            <path
              d="M72 42 L100 42 C108 56 132 56 140 42 L168 42 L208 76 L190 110 L166 94 L166 198 L74 198 L74 94 L50 110 L32 76 Z"
              fill="#0F52BA"
              stroke="#FFFFFF"
              stroke-width="4"
            />
            <polygon points="88,62 112,62 158,190 134,190" fill="#00C49F" />
          </svg>
        {/if}
      </div>

      <div class="flex items-center justify-between pt-3 border-t border-[#2E3D56]">
        <div class="truncate pr-3">
          <div class="text-sm font-semibold text-white truncate">{studio.activeKit.name}</div>
          <div class="text-xs text-[#64748B] font-mono-tabular">
            {studio.activeKit.layers.length} livelli attivi · ID {studio.activeKit.id.slice(0, 8)}
          </div>
        </div>
        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-EDITOR')}
          class="px-4 py-2 text-xs font-semibold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
        >
          Continua Editing
        </button>
      </div>
    </div>
  </div>

  <!-- Recent 3 Modified Drafts Grid (Required by SCR-HOME) -->
  <div class="space-y-5">
    <div class="flex items-center justify-between border-b border-[#2E3D56] pb-4">
      <div>
        <h2 class="text-2xl font-bold font-display tracking-wide text-white">
          {i18n.home.recent_drafts_title}
        </h2>
        <p class="text-sm text-[#94A3B8]">
          Le ultime 3 divise salvate in IndexedDB locale con minikit 2D a 512×512 px.
        </p>
      </div>

      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-SAVED')}
        class="text-sm font-medium text-[#00C49F] hover:underline whitespace-nowrap shrink-0 cursor-pointer"
      >
        {i18n.home.cta_all_kits} →
      </button>
    </div>

    {#if studio.recentThreeDrafts.length === 0}
      <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-10 text-center space-y-4">
        <p class="text-sm text-[#94A3B8]">{i18n.home.empty_drafts}</p>
        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-CATALOG')}
          class="px-5 py-2.5 text-xs font-semibold text-white bg-[#0F52BA] rounded-lg hover:bg-[#0A387E] transition-colors cursor-pointer"
        >
          Esplora Catalogo Template
        </button>
      </div>
    {:else}
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        {#each studio.recentThreeDrafts as draft (draft.id)}
          <article class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-col justify-between hover:border-[#0F52BA] transition-colors">
            <div>
              <!-- Unboxed quiet metadata line per Zero-Pill rule -->
              <div class="flex items-center justify-between text-xs text-[#94A3B8] mb-3 font-mono-tabular">
                <span>{i18n.catalog.categories[draft.category]} · {draft.layers.length} livelli</span>
                <span>{formatDate(draft.updatedAt)}</span>
              </div>

              <div class="bg-[#0F141C] rounded-lg p-3 flex items-center justify-center aspect-square mb-4">
                {#if draft.metadata?.thumbnailDataUrl}
                  <img
                    src={draft.metadata.thumbnailDataUrl}
                    alt="Minikit 2D {draft.name}"
                    class="w-full h-full object-contain rounded"
                  />
                {:else}
                  <div class="text-xs text-[#64748B]">Minikit 2D</div>
                {/if}
              </div>

              <h3 class="text-lg font-semibold text-white truncate">{draft.name}</h3>
              <p class="text-xs text-[#64748B] font-mono-tabular mt-1">
                UUID: {draft.id.slice(0, 13)}…
              </p>
            </div>

            <div class="flex items-center gap-3 mt-5 pt-4 border-t border-[#2E3D56]">
              <button
                type="button"
                onclick={() => studio.openSavedKit(draft, 'SCR-EDITOR')}
                class="flex-1 py-2 px-3 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-lg transition-colors whitespace-nowrap cursor-pointer"
              >
                Apri nell'Editor
              </button>
              <button
                type="button"
                onclick={() => studio.openSavedKit(draft, 'SCR-EXPORT')}
                class="py-2 px-3 text-xs font-medium text-[#94A3B8] hover:text-white bg-[#253146] rounded-lg transition-colors whitespace-nowrap cursor-pointer"
              >
                Esporta
              </button>
            </div>
          </article>
        {/each}
      </div>
    {/if}
  </div>

  <!-- Quick Template Starter Strip -->
  <div class="space-y-4 pt-2">
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-bold font-display tracking-wide text-white">
        Avvio Rapido da Modello Geometrico
      </h2>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="text-xs font-medium text-[#94A3B8] hover:text-white cursor-pointer"
      >
        Vedi tutti i {BUILTIN_TEMPLATES.length} template →
      </button>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each BUILTIN_TEMPLATES.slice(0, 3) as tpl (tpl.id)}
        <button
          type="button"
          onclick={() => studio.selectTemplate(tpl)}
          class="text-left bg-[#1A2230] hover:bg-[#253146] border border-[#2E3D56] rounded-xl p-4 flex items-center gap-4 transition-colors cursor-pointer"
        >
          <div
            class="w-12 h-12 rounded-lg shrink-0 border border-white/15 flex items-center justify-center"
            style="background: linear-gradient(135deg, {tpl.basePreviewColor} 50%, {tpl.accentPreviewColor} 50%);"
          >
            <span class="w-3 h-3 rounded-full" style="background-color: {tpl.secondaryPreviewColor};"></span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="text-sm font-semibold text-white truncate">{tpl.name}</div>
            <div class="text-xs text-[#94A3B8] truncate">
              {i18n.catalog.categories[tpl.category]} · {tpl.patternSummary}
            </div>
          </div>
        </button>
      {/each}
    </div>
  </div>
</section>
