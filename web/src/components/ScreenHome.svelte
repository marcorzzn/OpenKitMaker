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
        `Kit "${kit.name}" caricato con successo!`
      );
    } catch (err: any) {
      studio.notify('error', err?.message || i18n.errors.import_failed);
    } finally {
      input.value = '';
    }
  }
</script>

<section class="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-10 space-y-12 overflow-hidden">
  <!-- Hero Banner -->
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch bg-gradient-to-br from-[#1A2230] to-[#121824] border border-[#2E3D56] rounded-3xl p-8 sm:p-10 shadow-2xl relative overflow-hidden">
    <!-- Background Glow Effect -->
    <div class="absolute -top-32 -right-32 w-96 h-96 bg-[#0F52BA]/15 rounded-full blur-3xl pointer-events-none"></div>
    <div class="absolute -bottom-32 -left-32 w-96 h-96 bg-[#00C49F]/10 rounded-full blur-3xl pointer-events-none"></div>

    <div class="lg:col-span-7 flex flex-col justify-between space-y-6 relative z-10">
      <div class="space-y-4">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#0F141C] border border-[#2E3D56] text-xs font-mono-tabular">
          <span class="text-[#00C49F] font-semibold">⚡ STUDIO 3D SPORTSWEAR</span>
          <span class="text-[#64748B]">·</span>
          <span class="text-[#94A3B8]">100% Gratuito & Senza Limiti</span>
        </div>

        <h1 class="text-4xl sm:text-5xl font-black font-display tracking-tight text-white leading-tight" style="text-wrap: balance;">
          Progetta la Tua Divisa Perfetta in 3D Fotorealistico
        </h1>

        <p class="text-base text-[#94A3B8] max-w-2xl leading-relaxed">
          Crea divise complete (maglia, pantaloncini, calzettoni) con pattern geometrici dinamici, stemmi magnetici e 4 tessuti tecnici reali: <strong class="text-white">Mesh traspirante</strong>, <strong class="text-white">Poliestere lucido</strong>, <strong class="text-white">Cotone '90</strong> e <strong class="text-white">Jacquard</strong>.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-3 pt-2">
        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-CATALOG')}
          class="px-6 py-3.5 text-sm font-bold text-white bg-gradient-to-r from-[#0F52BA] to-[#0D47A1] hover:from-[#1565C0] hover:to-[#0F52BA] rounded-xl transition-all shadow-lg shadow-[#0F52BA]/25 cursor-pointer flex items-center gap-2"
        >
          <span>👕</span>
          <span>Inizia da un Modello</span>
        </button>

        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-SAVED')}
          class="px-5 py-3.5 text-sm font-semibold text-white bg-[#253146] hover:bg-[#2E3D56] border border-[#2E3D56] rounded-xl transition-colors cursor-pointer flex items-center gap-2"
        >
          <span>📁</span>
          <span>I Miei Progetti ({studio.savedKits.length})</span>
        </button>

        <button
          type="button"
          onclick={() => fileInputRef?.click()}
          class="px-5 py-3.5 text-sm font-medium text-[#94A3B8] hover:text-white bg-[#1A2230]/70 hover:bg-[#1A2230] border border-[#2E3D56] rounded-xl transition-colors cursor-pointer flex items-center gap-2"
        >
          <span>📥</span>
          <span>Importa .ZIP</span>
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

    <!-- Active Draft Showcase Card -->
    <div class="lg:col-span-5 bg-[#0F141C]/90 backdrop-blur border border-[#2E3D56] rounded-2xl p-6 flex flex-col justify-between relative z-10 shadow-xl">
      <div class="flex items-center justify-between text-xs text-[#94A3B8]">
        <span class="flex items-center gap-1.5 font-semibold text-white">
          <span class="w-2 h-2 rounded-full bg-[#00C49F]"></span> Progetto Attivo
        </span>
        <span class="px-2 py-0.5 rounded-full bg-[#1A2230] text-[#00C49F] font-mono-tabular border border-[#2E3D56]">
          {studio.fabricType}
        </span>
      </div>

      <div class="my-5 flex flex-col items-center justify-center">
        <div class="w-full aspect-[4/3] max-h-56 bg-[#141B26] border border-[#2E3D56] rounded-xl flex items-center justify-center p-3 overflow-hidden group">
          {#if studio.activeKit.metadata?.thumbnailDataUrl}
            <img
              src={studio.activeKit.metadata.thumbnailDataUrl}
              alt="Anteprima {studio.activeKit.name}"
              class="w-full h-full object-contain filter drop-shadow-md group-hover:scale-105 transition-transform"
            />
          {:else}
            <div class="text-4xl">👕</div>
          {/if}
        </div>
        <div class="mt-3 text-center">
          <div class="text-lg font-bold text-white tracking-wide">{studio.activeKit.name}</div>
          <div class="text-xs text-[#64748B] mt-0.5">
            {i18n.catalog.categories[studio.activeKit.category]} · {studio.activeKit.layers.length} livelli grafici
          </div>
        </div>
      </div>

      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-EDITOR')}
        class="w-full py-3 px-4 text-xs font-bold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-xl transition-all shadow-md shadow-[#00C49F]/20 cursor-pointer flex items-center justify-center gap-1.5"
      >
        <span>Apri nell'Editor 3D</span>
        <span>→</span>
      </button>
    </div>
  </div>

  <!-- Recent Projects Section -->
  <div class="space-y-5">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-bold font-display tracking-tight text-white">
          I Tuoi Ultimi Progetti
        </h2>
        <p class="text-xs text-[#94A3B8] mt-1">
          Modifica le tue divise, esporta nuove texture o condividile con gli amici.
        </p>
      </div>

      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-SAVED')}
        class="text-xs font-semibold text-[#00C49F] hover:underline cursor-pointer"
      >
        Vedi tutti ({studio.savedKits.length}) →
      </button>
    </div>

    {#if studio.recentThreeDrafts.length === 0}
      <div class="bg-[#1A2230] border border-[#2E3D56] rounded-2xl p-10 text-center space-y-4">
        <div class="text-4xl">⚽</div>
        <div class="text-sm font-medium text-white">Nessuna divisa creata finora</div>
        <p class="text-xs text-[#94A3B8] max-w-md mx-auto">
          Inizia scegliendo uno dei nostri modelli geometrici ispirati ai più grandi club del mondo.
        </p>
        <button
          type="button"
          onclick={() => studio.navigateTo('SCR-CATALOG')}
          class="px-5 py-2.5 text-xs font-semibold text-white bg-[#0F52BA] rounded-xl hover:bg-[#0A387E] transition-colors cursor-pointer"
        >
          Esplora Modelli
        </button>
      </div>
    {:else}
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        {#each studio.recentThreeDrafts as draft (draft.id)}
          <article class="bg-[#1A2230] border border-[#2E3D56] hover:border-[#0F52BA]/60 rounded-2xl p-5 flex flex-col justify-between transition-all hover:shadow-xl hover:shadow-[#0F52BA]/10 group">
            <div>
              <div class="flex items-center justify-between text-xs text-[#94A3B8] mb-3 font-mono-tabular">
                <span class="px-2 py-0.5 rounded-full bg-[#0F141C] border border-[#2E3D56] text-[11px] text-[#00C49F]">
                  {i18n.catalog.categories[draft.category]}
                </span>
                <span class="text-[11px] text-[#64748B]">{formatDate(draft.updatedAt)}</span>
              </div>

              <div class="bg-[#0F141C] rounded-xl p-3 flex items-center justify-center aspect-square mb-4 border border-[#2E3D56]/60">
                {#if draft.metadata?.thumbnailDataUrl}
                  <img
                    src={draft.metadata.thumbnailDataUrl}
                    alt="Kit {draft.name}"
                    class="w-full h-full object-contain filter drop-shadow-sm group-hover:scale-105 transition-transform"
                  />
                {:else}
                  <div class="text-3xl">👕</div>
                {/if}
              </div>

              <h3 class="text-base font-bold text-white truncate">{draft.name}</h3>
              <p class="text-xs text-[#64748B] mt-0.5">
                {draft.layers.length} elementi · Tessuto {draft.metadata?.fabricType || 'Mesh'}
              </p>
            </div>

            <div class="flex items-center gap-2 mt-5 pt-4 border-t border-[#2E3D56]">
              <button
                type="button"
                onclick={() => studio.openSavedKit(draft, 'SCR-EDITOR')}
                class="flex-1 py-2 px-3 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-lg transition-colors cursor-pointer text-center"
              >
                Modifica
              </button>
              <button
                type="button"
                onclick={() => studio.openSavedKit(draft, 'SCR-EXPORT')}
                class="py-2 px-3 text-xs font-medium text-[#94A3B8] hover:text-white bg-[#253146] hover:bg-[#2E3D56] rounded-lg transition-colors cursor-pointer"
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
  <div class="space-y-5 pt-2">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-bold font-display tracking-tight text-white">
          Modelli di Partenza Rapida
        </h2>
        <p class="text-xs text-[#94A3B8] mt-1">
          Scegli un'impostazione geometrica classica per creare la tua divisa in pochi secondi.
        </p>
      </div>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="text-xs font-semibold text-[#00C49F] hover:underline cursor-pointer"
      >
        Vedi tutti i {BUILTIN_TEMPLATES.length} modelli →
      </button>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each BUILTIN_TEMPLATES.slice(0, 3) as tpl (tpl.id)}
        <button
          type="button"
          onclick={() => studio.selectTemplate(tpl)}
          class="text-left bg-[#1A2230] hover:bg-[#253146] border border-[#2E3D56] hover:border-[#00C49F]/50 rounded-2xl p-4 flex items-center gap-4 transition-all cursor-pointer shadow-sm group"
        >
          <div
            class="w-14 h-14 rounded-xl shrink-0 border border-white/15 flex items-center justify-center shadow-inner group-hover:scale-105 transition-transform"
            style="background: linear-gradient(135deg, {tpl.basePreviewColor} 50%, {tpl.accentPreviewColor} 50%);"
          >
            <span class="w-3.5 h-3.5 rounded-full shadow-sm" style="background-color: {tpl.secondaryPreviewColor};"></span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="text-sm font-bold text-white truncate group-hover:text-[#00C49F] transition-colors">{tpl.name}</div>
            <div class="text-xs text-[#94A3B8] truncate mt-0.5">
              {i18n.catalog.categories[tpl.category]} · {tpl.patternSummary}
            </div>
          </div>
        </button>
      {/each}
    </div>
  </div>
</section>
