// Implements: SCR-SAVED / FUN-SAVE-LOCAL, FUN-EXPORT-ZIP
<script lang="ts">
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import { importKitFromZip } from '../lib/exporter';
  import { saveKitToDb, loadAllKitsFromDb } from '../lib/db';
  import type { Kit } from '../types/kit';

  let renamingKitId = $state<string | null>(null);
  let renameDraft = $state<string>('');
  let deletingKit = $state<Kit | null>(null);
  let zipInputRef = $state<HTMLInputElement | null>(null);

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

  function startRename(kit: Kit) {
    renamingKitId = kit.id;
    renameDraft = kit.name;
  }

  async function commitRename(kitId: string) {
    if (renameDraft.trim()) {
      await studio.renameSavedKit(kitId, renameDraft);
    }
    renamingKitId = null;
  }

  async function confirmDelete() {
    if (!deletingKit) return;
    await studio.removeSavedKit(deletingKit.id);
    deletingKit = null;
  }

  async function handleImportZip(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    try {
      const { kit, checksumVerified } = await importKitFromZip(file);
      await saveKitToDb(kit);
      studio.savedKits = await loadAllKitsFromDb();
      studio.notify(
        'success',
        `Archivio "${kit.name}" importato in locale (${checksumVerified ? 'SHA-256 valido' : 'manifest v1.0.0'}).`
      );
    } catch (err: any) {
      studio.notify('error', err?.message || i18n.errors.import_failed);
    } finally {
      input.value = '';
    }
  }
</script>

<section class="max-w-7xl mx-auto px-6 py-10 space-y-8">
  <!-- Header (SCR-SAVED) -->
  <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#2E3D56] pb-6">
    <div class="space-y-1.5">
      <div class="text-xs text-[#94A3B8] font-mono-tabular">
        📁 I TUOI PROGETTI · Sempre disponibili offline
      </div>
      <h1 class="text-3xl sm:text-4xl font-bold font-display tracking-wide text-white">
        {i18n.saved.title} ({studio.savedKits.length})
      </h1>
      <p class="text-sm text-[#94A3B8]">
        {i18n.saved.subtitle}
      </p>
    </div>

    <div class="flex items-center gap-3">
      <button
        type="button"
        onclick={() => zipInputRef?.click()}
        class="px-4 py-2.5 text-xs font-semibold text-white bg-[#253146] hover:bg-[#2E3D56] border border-[#2E3D56] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        Importa ZIP (.zip)
      </button>
      <input
        bind:this={zipInputRef}
        type="file"
        accept=".zip,application/zip"
        onchange={handleImportZip}
        class="hidden"
      />

      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="px-4 py-2.5 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        + Nuovo da Template
      </button>
    </div>
  </div>

  {#if studio.savedKits.length === 0}
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-12 text-center space-y-4">
      <p class="text-base text-white font-medium">Nessun kit salvato nello storage locale.</p>
      <p class="text-sm text-[#94A3B8]">
        Crea un kit dal Catalogo Template o importa un archivio interoperabile <span class="font-mono-tabular">kit_&lt;id&gt;.zip</span>.
      </p>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="px-5 py-2.5 text-xs font-semibold text-white bg-[#0F52BA] rounded-lg cursor-pointer"
      >
        Apri Catalogo Template
      </button>
    </div>
  {:else}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {#each studio.savedKits as kit (kit.id)}
        <article class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-col justify-between">
          <div>
            <!-- Unboxed metadata line -->
            <div class="flex items-center justify-between text-xs text-[#94A3B8] mb-3 font-mono-tabular">
              <span>{i18n.catalog.categories[kit.category]} · {kit.layers.length} livelli</span>
              <span>{formatDate(kit.updatedAt)}</span>
            </div>

            <!-- Minikit 512x512 Preview -->
            <div class="bg-[#0F141C] rounded-lg p-3 aspect-square flex items-center justify-center mb-4">
              {#if kit.metadata?.thumbnailDataUrl}
                <img
                  src={kit.metadata.thumbnailDataUrl}
                  alt="Thumbnail minikit {kit.name}"
                  class="w-full h-full object-contain rounded"
                />
              {:else}
                <span class="text-xs text-[#64748B] font-mono-tabular">512×512 Minikit</span>
              {/if}
            </div>

            {#if renamingKitId === kit.id}
              <div class="flex items-center gap-2 mb-2">
                <input
                  type="text"
                  bind:value={renameDraft}
                  onkeydown={(e) => e.key === 'Enter' && commitRename(kit.id)}
                  class="flex-1 px-3 py-1.5 text-sm bg-[#0F141C] border border-[#00C49F] rounded-lg text-white focus:outline-none"
                />
                <button
                  type="button"
                  onclick={() => commitRename(kit.id)}
                  class="px-3 py-1.5 text-xs font-semibold text-[#0F141C] bg-[#00C49F] rounded-lg cursor-pointer"
                >
                  Salva
                </button>
              </div>
            {:else}
              <h2 class="text-xl font-bold font-display tracking-wide text-white truncate">
                {kit.name}
              </h2>
            {/if}

            <div class="text-xs text-[#64748B] font-mono-tabular mt-1">
              ID: {kit.id}
            </div>
          </div>

          <!-- Actions required by SCR-SAVED: Apri nell'Editor, Rinomina, Duplica, Elimina -->
          <div class="space-y-2 mt-5 pt-4 border-t border-[#2E3D56]">
            <div class="flex items-center gap-2">
              <button
                type="button"
                onclick={() => studio.openSavedKit(kit, 'SCR-EDITOR')}
                class="flex-1 py-2 px-3 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-lg transition-colors whitespace-nowrap cursor-pointer"
              >
                {i18n.saved.open_editor}
              </button>
              <button
                type="button"
                onclick={() => studio.openSavedKit(kit, 'SCR-EXPORT')}
                class="py-2 px-3 text-xs font-semibold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-lg transition-colors whitespace-nowrap cursor-pointer"
              >
                Esporta
              </button>
            </div>

            <div class="grid grid-cols-3 gap-2">
              <button
                type="button"
                onclick={() => startRename(kit)}
                class="py-1.5 px-2 text-xs font-medium text-[#94A3B8] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-md transition-colors whitespace-nowrap cursor-pointer"
              >
                {i18n.saved.rename}
              </button>
              <button
                type="button"
                onclick={() => studio.duplicateSavedKit(kit)}
                class="py-1.5 px-2 text-xs font-medium text-[#94A3B8] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-md transition-colors whitespace-nowrap cursor-pointer"
              >
                {i18n.saved.duplicate}
              </button>
              <button
                type="button"
                onclick={() => (deletingKit = kit)}
                class="py-1.5 px-2 text-xs font-medium text-[#EF4444] hover:bg-[#EF4444]/10 bg-[#0F141C] border border-[#2E3D56] rounded-md transition-colors whitespace-nowrap cursor-pointer"
              >
                {i18n.saved.delete}
              </button>
            </div>
          </div>
        </article>
      {/each}
    </div>
  {/if}

  <!-- Confirmation Dialog for Delete (Required by SCR-SAVED) -->
  {#if deletingKit}
    <div
      class="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-modal-title"
    >
      <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
        <h3 id="delete-modal-title" class="text-xl font-bold font-display text-white">
          {i18n.saved.confirm_delete_title}
        </h3>
        <p class="text-sm text-[#94A3B8]">
          {i18n.saved.confirm_delete_msg}
          <span class="block mt-2 text-white font-semibold">"{deletingKit.name}"</span>
        </p>

        <div class="flex items-center justify-end gap-3 pt-3 border-t border-[#2E3D56]">
          <button
            type="button"
            onclick={() => (deletingKit = null)}
            class="px-4 py-2 text-xs font-semibold text-[#94A3B8] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-lg cursor-pointer"
          >
            Annulla
          </button>
          <button
            type="button"
            onclick={confirmDelete}
            class="px-4 py-2 text-xs font-semibold text-white bg-[#EF4444] hover:bg-red-600 rounded-lg cursor-pointer"
          >
            Elimina Definitivamente
          </button>
        </div>
      </div>
    </div>
  {/if}
</section>
