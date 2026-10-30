// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-OFFLINE-MODE
<script lang="ts">
  import { onMount } from 'svelte';
  import { studio } from './state/studio.svelte';
  import TopNav from './components/TopNav.svelte';
  import ScreenHome from './components/ScreenHome.svelte';
  import ScreenCatalog from './components/ScreenCatalog.svelte';
  import ScreenEditor from './components/ScreenEditor.svelte';
  import ScreenSaved from './components/ScreenSaved.svelte';
  import ScreenExport from './components/ScreenExport.svelte';

  onMount(() => {
    studio.init();
  });
</script>

<div class="min-h-screen flex flex-col bg-[#0F141C] text-white">
  <TopNav />

  <main class="flex-1 pb-20 sm:pb-4">
    {#if studio.currentScreen === 'SCR-HOME'}
      <ScreenHome />
    {:else if studio.currentScreen === 'SCR-CATALOG'}
      <ScreenCatalog />
    {:else if studio.currentScreen === 'SCR-EDITOR'}
      <ScreenEditor />
    {:else if studio.currentScreen === 'SCR-SAVED'}
      <ScreenSaved />
    {:else if studio.currentScreen === 'SCR-EXPORT'}
      <ScreenExport />
    {/if}
  </main>

  <!-- Toast Notifications -->
  {#if studio.toasts.length > 0}
    <div class="fixed bottom-20 sm:bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      {#each studio.toasts as t (t.id)}
        <div
          class="pointer-events-auto px-4 py-3 rounded-xl border shadow-xl flex items-center justify-between gap-3 text-xs font-medium {t.type ===
          'error'
            ? 'bg-[#1A2230] border-[#EF4444] text-[#EF4444]'
            : t.type === 'success'
              ? 'bg-[#1A2230] border-[#10B981] text-[#10B981]'
              : 'bg-[#1A2230] border-[#0F52BA] text-white'}"
        >
          <span>{t.message}</span>
          <button
            type="button"
            onclick={() => studio.dismissToast(t.id)}
            class="text-[#94A3B8] hover:text-white cursor-pointer"
          >
            ✕
          </button>
        </div>
      {/each}
    </div>
  {/if}

  <footer class="border-t border-[#2E3D56] py-5 px-6 text-xs text-[#64748B] flex flex-wrap items-center justify-between gap-4">
    <div>OpenKitMaker-Web · Studio Sartoriale 2D/3D (Contratto di Parità v1.0.0)</div>
    <div class="flex items-center gap-4">
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="hover:text-white cursor-pointer"
      >
        Catalogo Template
      </button>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-SAVED')}
        class="hover:text-white cursor-pointer"
      >
        I Miei Kit
      </button>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-EXPORT')}
        class="hover:text-white cursor-pointer"
      >
        Centro Esportazione
      </button>
    </div>
  </footer>
</div>
