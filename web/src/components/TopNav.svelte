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

<!-- Top Header Bar -->
<header class="sticky top-0 z-40 h-16 bg-[#0F141C]/95 backdrop-blur-md border-b border-[#2E3D56] px-4 sm:px-6 flex items-center justify-between gap-3">
  <!-- Brand Title -->
  <button
    type="button"
    onclick={() => studio.navigateTo('SCR-HOME')}
    class="flex items-center gap-2.5 text-left group transition-all shrink-0 cursor-pointer"
  >
    <div class="w-9 h-9 rounded-xl bg-[#0D1A2D] border border-[#2E3D56] flex items-center justify-center shadow-md shadow-[#0F52BA]/30 group-hover:scale-105 transition-transform overflow-hidden">
      <img src="/icons/icon-192.png" alt="OpenKitMaker Logo" class="w-full h-full object-cover" />
    </div>
    <div class="flex flex-col">
      <div class="text-sm sm:text-base font-extrabold tracking-wider font-display text-white group-hover:text-[#00C49F] transition-colors leading-tight">
        OPEN<span class="text-[#00C49F]">KIT</span>MAKER
      </div>
      <div class="text-[9px] sm:text-[10px] text-[#64748B] font-mono-tabular tracking-wider uppercase">
        Studio 3D Pro
      </div>
    </div>
  </button>

  <!-- Desktop Navigation Links (hidden on mobile, shown on sm+) -->
  <nav class="hidden sm:flex items-center gap-1.5" aria-label="Navigazione Principale Desktop">
    {#each navItems as item (item.id)}
      <button
        type="button"
        onclick={() => studio.navigateTo(item.id)}
        class="h-9 flex items-center gap-1.5 px-3 rounded-lg text-xs font-semibold transition-all whitespace-nowrap shrink-0 cursor-pointer {studio.currentScreen === item.id
          ? 'bg-[#1A2230] text-[#00C49F] border border-[#2E3D56] shadow-sm font-bold'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1A2230]/50'}"
        title={item.label}
      >
        <span class="text-sm">{item.icon}</span>
        <span>{item.label}</span>
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
        class="h-9 px-3.5 text-xs font-semibold text-[#0F141C] bg-[#00C49F] rounded-xl hover:bg-[#10B981] transition-colors whitespace-nowrap shrink-0 cursor-pointer shadow-sm flex items-center justify-center gap-1.5"
      >
        <span>📱</span>
        <span>Installa</span>
      </button>
    {:else}
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-CATALOG')}
        class="h-9 px-3.5 text-xs font-semibold text-white bg-[#0F52BA] hover:bg-[#0A387E] rounded-xl transition-colors whitespace-nowrap shrink-0 cursor-pointer shadow-md shadow-[#0F52BA]/20 flex items-center justify-center gap-1.5"
      >
        <span>👕</span>
        <span>+ Nuovo Kit</span>
      </button>
    {/if}
  </div>
</header>

<!-- Mobile Bottom Navigation Bar (< 640px) -->
<nav class="sm:hidden fixed bottom-0 left-0 right-0 z-40 h-16 bg-[#0F141C]/95 backdrop-blur-lg border-t border-[#2E3D56] px-1 flex items-center justify-around shadow-2xl" aria-label="Navigazione Mobile Inferiore">
  {#each navItems as item (item.id)}
    <button
      type="button"
      onclick={() => studio.navigateTo(item.id)}
      class="flex flex-col items-center justify-center flex-1 h-full py-1 px-1 rounded-lg transition-all cursor-pointer {studio.currentScreen === item.id
        ? 'text-[#00C49F] font-bold'
        : 'text-[#94A3B8] hover:text-white'}"
      title={item.label}
    >
      <span class="text-lg leading-none mb-1">{item.icon}</span>
      <span class="text-[10px] tracking-tight whitespace-nowrap leading-none">{item.label}</span>
      {#if studio.currentScreen === item.id}
        <span class="w-1 h-1 rounded-full bg-[#00C49F] mt-1"></span>
      {/if}
    </button>
  {/each}
</nav>
