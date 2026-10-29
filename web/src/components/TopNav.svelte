// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-OFFLINE-MODE
<script lang="ts">
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import type { ScreenId } from '../types/kit';

  const navItems: { id: ScreenId; label: string; icon: string }[] = [
    { id: 'SCR-HOME', label: i18n.nav.home, icon: '🏠' },
    { id: 'SCR-CATALOG', label: i18n.nav.catalog, icon: '📋' },
    { id: 'SCR-EDITOR', label: i18n.nav.editor, icon: '👕' },
    { id: 'SCR-SAVED', label: i18n.nav.saved, icon: '📁' },
    { id: 'SCR-EXPORT', label: i18n.nav.export, icon: '📤' },
  ];
</script>

<header class="sticky top-0 z-40 h-16 bg-[#0F141C]/95 backdrop-blur border-b border-[#2E3D56] px-3 sm:px-6 flex items-center justify-between gap-2 sm:gap-6">
  <!-- Brand Title -->
  <button
    type="button"
    onclick={() => studio.navigateTo('SCR-HOME')}
    class="flex items-center gap-2 sm:gap-2.5 text-left group transition-all shrink-0 cursor-pointer"
  >
    <div class="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-gradient-to-br from-[#0F52BA] to-[#00C49F] flex items-center justify-center text-white font-black text-base sm:text-lg shadow-md shadow-[#0F52BA]/30 group-hover:scale-105 transition-transform">
      ⚡
    </div>
    <div class="hidden sm:flex flex-col">
      <div class="text-sm sm:text-base font-extrabold tracking-wider font-display text-white group-hover:text-[#00C49F] transition-colors leading-tight">
        OPEN<span class="text-[#00C49F]">KIT</span>MAKER
      </div>
      <div class="text-[9px] sm:text-[10px] text-[#64748B] font-mono-tabular tracking-wider uppercase hidden xs:block">
        Studio 3D Pro
      </div>
    </div>
  </button>

  <!-- Navigation Links -->
  <nav class="flex items-center gap-1 sm:gap-2" aria-label="Navigazione Principale">
    {#each navItems as item (item.id)}
      <button
        type="button"
        onclick={() => studio.navigateTo(item.id)}
        class="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all whitespace-nowrap shrink-0 cursor-pointer {studio.currentScreen === item.id
          ? 'bg-[#1A2230] text-[#00C49F] border border-[#2E3D56] shadow-sm'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1A2230]/50'}"
        title={item.label}
      >
        <span class="text-sm">{item.icon}</span>
        <span class="hidden sm:inline">{item.label}</span>
      </button>
    {/each}
  </nav>

  <!-- Quick Actions -->
  <div class="flex items-center gap-2 sm:gap-3 shrink-0">
    <div
      class="hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#1A2230] border border-[#2E3D56] text-[11px] font-mono-tabular {studio.isOnline ? 'text-[#00C49F]' : 'text-[#F59E0B]'}"
    >
      <span class="w-1.5 h-1.5 rounded-full {studio.isOnline ? 'bg-[#00C49F]' : 'bg-[#F59E0B]'} animate-pulse"></span>
      <span>{studio.isOnline ? '3D Online' : 'Offline'}</span>
    </div>

    {#if studio.canInstallPwa && !studio.isStandalonePwa}
      <button
        type="button"
        onclick={() => studio.triggerPwaInstall()}
        class="px-2.5 sm:px-3.5 py-1.5 text-xs font-semibold text-[#0F141C] bg-[#00C49F] rounded-lg hover:bg-[#10B981] transition-colors whitespace-nowrap shrink-0 cursor-pointer shadow-sm"
      >
        <span class="hidden sm:inline">Installa App</span>
        <span class="sm:hidden">Installa</span>
      </button>
    {:else}
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="px-2.5 sm:px-3.5 py-1.5 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer shadow-md shadow-[#0F52BA]/20"
      >
        <span class="hidden sm:inline">+ Nuovo Kit</span>
        <span class="sm:hidden">+ Kit</span>
      </button>
    {/if}
  </div>
</header>
