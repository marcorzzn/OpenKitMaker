// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-OFFLINE-MODE
<script lang="ts">
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import type { ScreenId } from '../types/kit';

  const navItems: { id: ScreenId; label: string }[] = [
    { id: 'SCR-HOME', label: i18n.nav.home },
    { id: 'SCR-CATALOG', label: i18n.nav.catalog },
    { id: 'SCR-EDITOR', label: i18n.nav.editor },
    { id: 'SCR-SAVED', label: i18n.nav.saved },
    { id: 'SCR-EXPORT', label: i18n.nav.export },
  ];
</script>

<header class="sticky top-0 z-40 h-16 bg-[#0F141C]/95 border-b border-[#2E3D56] px-6 flex items-center justify-between gap-8">
  <!-- Zone 1: Brand Title (Single element wordmark per Top Bar Contract) -->
  <button
    type="button"
    onclick={() => studio.navigateTo('SCR-HOME')}
    class="text-xl font-bold tracking-wide font-display text-white hover:text-[#00C49F] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
  >
    OpenKitMaker-Web
  </button>

  <!-- Zone 2: 5 Concise Single-Line Navigation Links -->
  <nav class="flex items-center gap-6 overflow-x-auto" aria-label="Navigazione Principale">
    {#each navItems as item (item.id)}
      <button
        type="button"
        onclick={() => studio.navigateTo(item.id)}
        class="relative py-1.5 text-sm font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.currentScreen === item.id
          ? 'text-white after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-[#00C49F]'
          : 'text-[#94A3B8] hover:text-white'}"
      >
        {item.label}
      </button>
    {/each}
  </nav>

  <!-- Zone 3: Connectivity Indicator & Primary Action -->
  <div class="flex items-center gap-4 shrink-0">
    <span
      class="text-xs font-mono-tabular whitespace-nowrap shrink-0 {studio.isOnline ? 'text-[#94A3B8]' : 'text-[#F59E0B] font-semibold'}"
      title="Stato di rete e autonomia PWA"
    >
      {studio.isOnline ? 'Online · PWA Ready' : i18n.app.offline_badge}
    </span>

    {#if studio.canInstallPwa && !studio.isStandalonePwa}
      <button
        type="button"
        onclick={() => studio.triggerPwaInstall()}
        class="px-4 py-2 text-xs font-semibold text-[#0F141C] bg-[#00C49F] rounded-lg hover:bg-[#10B981] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        Installa App
      </button>
    {:else}
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="px-4 py-2 text-xs font-semibold text-white bg-[#0F52BA] rounded-lg hover:bg-[#0A387E] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        Nuovo Kit
      </button>
    {/if}
  </div>
</header>
