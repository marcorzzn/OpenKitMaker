// Implements: SCR-EDITOR / FUN-PREVIEW-3D
<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { studio } from '../state/studio.svelte';
  import type { Kit3DEngineController } from '../lib/engine3d';
  import type { LightingPreset } from '../types/kit';

  let {
    sourceCanvas,
    textureRevision = 0,
  }: {
    sourceCanvas: HTMLCanvasElement | null;
    textureRevision: number;
  } = $props();

  let mountContainer = $state<HTMLDivElement | null>(null);
  let engine = $state<Kit3DEngineController | null>(null);
  let isLoadingEngine = $state<boolean>(true);
  let loadError = $state<string | null>(null);
  let dockCollapsed = $state<boolean>(false);
  let autoRotate = $state<boolean>(false);
  let fpsValue = $state<number>(60);
  let fpsTimer: ReturnType<typeof setInterval> | null = null;
  let resizeObserver: ResizeObserver | null = null;

  async function initEngine() {
    if (engine || !mountContainer) return;
    try {
      isLoadingEngine = true;
      loadError = null;
      const module3d = await import('../lib/engine3d');
      if (!mountContainer) return;

      engine = module3d.createKit3DEngine(
        mountContainer,
        sourceCanvas,
        studio.lightingPreset
      );
      engine.setFinishType(studio.activeKit.metadata?.finishType || 'PRO_VAPOR');
      isLoadingEngine = false;

      resizeObserver = new ResizeObserver(() => {
        engine?.resize();
      });
      resizeObserver.observe(mountContainer);

      // Force an initial resize once DOM layout finishes
      requestAnimationFrame(() => {
        engine?.resize();
      });

      fpsTimer = setInterval(() => {
        if (engine) {
          fpsValue = engine.getFps();
        }
      }, 600);
    } catch (err: any) {
      loadError = err?.message || 'Errore inizializzazione WebGL2.';
      isLoadingEngine = false;
    }
  }

  onMount(() => {
    initEngine();
  });

  onDestroy(() => {
    if (fpsTimer) clearInterval(fpsTimer);
    resizeObserver?.disconnect();
    engine?.dispose();
    engine = null;
  });

  // Reactively push 2048x2048 Canvas updates to 3D PBR texture
  $effect(() => {
    if (!engine && mountContainer) {
      initEngine();
    }
    const _rev = textureRevision;
    if (engine && sourceCanvas && _rev >= 0) {
      engine.updateCanvasTexture(sourceCanvas);
    }
  });


  function switchLighting(preset: LightingPreset) {
    studio.lightingPreset = preset;
    studio.activeKit.metadata.lightingPreset = preset;
    engine?.setLightingPreset(preset);
  }

  function switchFinish(finish: 'MATTE_MESH' | 'PRO_VAPOR' | 'RETRO_SATIN') {
    studio.activeKit.metadata.finishType = finish;
    studio.textureRevision++;
    engine?.setFinishType(finish);
  }

  function toggleAutoRotate() {
    autoRotate = !autoRotate;
    engine?.setAutoRotate(autoRotate);
  }
</script>

<div class="relative w-full h-full min-h-[460px] bg-[#0F141C] rounded-xl border border-[#2E3D56] overflow-hidden flex flex-col">
  <!-- Minimal Top-Left Viewport Spec Badge (Unobstructed 3D Center) -->
  <div class="absolute top-3 left-3 z-10 pointer-events-none flex items-center gap-2 px-3 py-1.5 rounded-lg bg-[#0F141C]/85 border border-[#2E3D56] text-xs font-mono-tabular text-[#94A3B8]">
    <span class="text-white font-semibold">Viewport 3D PBR</span>
    <span aria-hidden="true">·</span>
    <span>KTX2 / Draco Ready</span>
    <span aria-hidden="true">·</span>
    <span class="text-[#00C49F]">{fpsValue} FPS</span>
  </div>

  <!-- Isolated WebGL Mount Container -->
  <div
    bind:this={mountContainer}
    class="w-full h-full flex-1 cursor-grab active:cursor-grabbing"
    aria-label="Modello 3D interattivo della divisa sportiva"
  ></div>

  {#if isLoadingEngine}
    <div class="absolute inset-0 z-20 bg-[#0F141C]/90 flex flex-col items-center justify-center gap-3 p-6 text-center">
      <div class="w-10 h-10 rounded-full border-2 border-[#0F52BA] border-t-[#00C49F] animate-spin"></div>
      <div class="text-sm font-semibold text-white">Caricamento Lazy Motore 3D (Three.js + KTX2 + Draco)…</div>
      <div class="text-xs text-[#94A3B8] font-mono-tabular">Proiezione UV 2048×2048 su mesh sartoriale PBR</div>
    </div>
  {:else if loadError}
    <div class="absolute inset-0 z-20 bg-[#0F141C]/95 flex flex-col items-center justify-center gap-2 p-6 text-center">
      <div class="text-sm font-semibold text-[#EF4444]">{loadError}</div>
      <div class="text-xs text-[#94A3B8]">Utilizza la Vista 2D ad alta definizione 2048×2048.</div>
    </div>
  {/if}

  <!-- Collapsible Bottom Control Dock (Zero Side-Panel Occlusion per Three.js Guidelines) -->
  <div class="absolute bottom-3 left-1/2 -translate-x-1/2 z-10 w-[94%] max-w-2xl flex flex-col items-center">
    <button
      type="button"
      onclick={() => (dockCollapsed = !dockCollapsed)}
      class="px-3 py-1 text-[11px] font-medium text-[#94A3B8] hover:text-white bg-[#1A2230] border border-b-0 border-[#2E3D56] rounded-t-lg cursor-pointer whitespace-nowrap"
    >
      {dockCollapsed ? '▴ Mostra Controlli 3D' : '▾ Nascondi Controlli 3D'}
    </button>

    {#if !dockCollapsed}
      <div class="w-full bg-[#1A2230]/95 border border-[#2E3D56] rounded-xl p-2.5 shadow-xl flex flex-wrap items-center justify-between gap-2">
        <!-- Lighting Preset Selector (Studio 6500K vs Stadium 5400K) -->
        <div class="inline-flex items-center gap-1 p-0.5 bg-[#0F141C] border border-[#2E3D56] rounded-lg">
          <button
            type="button"
            onclick={() => switchLighting('studio')}
            class="px-2.5 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap cursor-pointer {studio.lightingPreset === 'studio'
              ? 'bg-[#0F52BA] text-white'
              : 'text-[#94A3B8] hover:text-white'}"
          >
            Studio 6500K
          </button>
          <button
            type="button"
            onclick={() => switchLighting('stadium')}
            class="px-2.5 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap cursor-pointer {studio.lightingPreset === 'stadium'
              ? 'bg-[#0F52BA] text-white'
              : 'text-[#94A3B8] hover:text-white'}"
          >
            Stadio 5400K
          </button>
        </div>

        <!-- Fabric Finish Selector -->
        <div class="inline-flex items-center gap-1 p-0.5 bg-[#0F141C] border border-[#2E3D56] rounded-lg">
          <button
            type="button"
            onclick={() => switchFinish('PRO_VAPOR')}
            class="px-2 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap cursor-pointer {studio.activeKit.metadata?.finishType === 'PRO_VAPOR'
              ? 'bg-[#253146] text-[#00C49F]'
              : 'text-[#94A3B8] hover:text-white'}"
          >
            Vapor
          </button>
          <button
            type="button"
            onclick={() => switchFinish('MATTE_MESH')}
            class="px-2 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap cursor-pointer {studio.activeKit.metadata?.finishType === 'MATTE_MESH'
              ? 'bg-[#253146] text-[#00C49F]'
              : 'text-[#94A3B8] hover:text-white'}"
          >
            Matte
          </button>
          <button
            type="button"
            onclick={() => switchFinish('RETRO_SATIN')}
            class="px-2 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap cursor-pointer {studio.activeKit.metadata?.finishType === 'RETRO_SATIN'
              ? 'bg-[#253146] text-[#00C49F]'
              : 'text-[#94A3B8] hover:text-white'}"
          >
            Satin
          </button>
        </div>

        <!-- Camera Orbit Quick Presets -->
        <div class="inline-flex items-center gap-1">
          <button
            type="button"
            onclick={() => engine?.setCameraAngle('FRONT')}
            class="px-2.5 py-1 text-xs font-medium text-[#94A3B8] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-md cursor-pointer whitespace-nowrap"
          >
            Fronte
          </button>
          <button
            type="button"
            onclick={() => engine?.setCameraAngle('BACK')}
            class="px-2.5 py-1 text-xs font-medium text-[#94A3B8] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-md cursor-pointer whitespace-nowrap"
          >
            Retro
          </button>
          <button
            type="button"
            onclick={toggleAutoRotate}
            class="px-2.5 py-1 text-xs font-medium rounded-md border border-[#2E3D56] cursor-pointer whitespace-nowrap {autoRotate
              ? 'bg-[#00C49F] text-[#0F141C] font-semibold'
              : 'bg-[#0F141C] text-[#94A3B8] hover:text-white'}"
          >
            360°
          </button>
        </div>
      </div>
    {/if}
  </div>
</div>
