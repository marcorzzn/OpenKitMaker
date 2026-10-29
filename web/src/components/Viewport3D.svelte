// Implements: SCR-EDITOR / FUN-PREVIEW-3D
<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { studio } from '../state/studio.svelte';
  import type { Kit3DEngineController } from '../lib/engine3d';
  import type { LightingPreset, ViewportBackground, FabricType } from '../types/kit';

  let {
    sourceCanvas = null,
    jerseyCanvas = null,
    shortsCanvas = null,
    socksCanvas = null,
    textureRevision = 0,
  }: {
    sourceCanvas?: HTMLCanvasElement | null;
    jerseyCanvas?: HTMLCanvasElement | null;
    shortsCanvas?: HTMLCanvasElement | null;
    socksCanvas?: HTMLCanvasElement | null;
    textureRevision: number;
  } = $props();

  let mountContainer = $state<HTMLDivElement | null>(null);
  let engine = $state<Kit3DEngineController | null>(null);
  let isLoadingEngine = $state<boolean>(true);
  let loadError = $state<string | null>(null);
  let autoRotate = $state<boolean>(false);
  let activeCameraPreset = $state<'FRONT' | 'BACK' | 'LEFT' | 'RIGHT' | null>('FRONT');
  let fpsValue = $state<number>(60);
  let fpsTimer: ReturnType<typeof setInterval> | null = null;
  let resizeObserver: ResizeObserver | null = null;
  let isInitializing = false;
  let isDestroyed = false;

  async function initEngine() {
    if (isInitializing || isDestroyed || engine || !mountContainer) return;
    try {
      isInitializing = true;
      isLoadingEngine = true;
      loadError = null;
      const module3d = await import('../lib/engine3d');
      if (isDestroyed || !mountContainer) {
        isInitializing = false;
        return;
      }

      const instance = module3d.createKit3DEngine(
        mountContainer,
        jerseyCanvas || sourceCanvas,
        studio.lightingPreset,
        studio.viewportBackground
      );

      if (isDestroyed) {
        instance.dispose();
        isInitializing = false;
        return;
      }

      engine = instance;
      engine.setFabricType(studio.fabricType);
      engine.setFinishType(studio.activeKit.metadata?.finishType || 'PRO_VAPOR');
      engine.setMannequinVisible(studio.showMannequin);
      engine.setAnatomicalView(studio.showAnatomicalView);
      engine.setGarmentMode(studio.activeGarmentType);
      if (shortsCanvas) engine.updateShortsTexture(shortsCanvas);
      if (socksCanvas) engine.updateSocksTexture(socksCanvas);

      isLoadingEngine = false;
      isInitializing = false;

      resizeObserver = new ResizeObserver(() => {
        engine?.resize();
      });
      resizeObserver.observe(mountContainer);

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
      isInitializing = false;
    }
  }

  onMount(() => {
    initEngine();
  });

  onDestroy(() => {
    isDestroyed = true;
    if (fpsTimer) clearInterval(fpsTimer);
    resizeObserver?.disconnect();
    engine?.dispose();
    engine = null;
  });

  // Reactively push Canvas updates and state changes to 3D PBR engine
  $effect(() => {
    if (!engine && !isInitializing && mountContainer) {
      initEngine();
    }
    const _rev = textureRevision;
    const _mannequin = studio.showMannequin;
    const _anatomy = studio.showAnatomicalView;
    const _garment = studio.activeGarmentType;
    const _lighting = studio.lightingPreset;
    const _bg = studio.viewportBackground;
    const _fabric = studio.fabricType;

    if (engine) {
      engine.setFabricType(_fabric);
      engine.setMannequinVisible(_mannequin);
      engine.setAnatomicalView(_anatomy);
      engine.setGarmentMode(_garment);
      engine.setLightingPreset(_lighting);
      engine.setViewportBackground(_bg);

      if (jerseyCanvas) {
        engine.updateJerseyTexture(jerseyCanvas);
      } else if (sourceCanvas) {
        engine.updateCanvasTexture(sourceCanvas);
      }
      if (shortsCanvas) {
        engine.updateShortsTexture(shortsCanvas);
      }
      if (socksCanvas) {
        engine.updateSocksTexture(socksCanvas);
      }
    }
  });

  function selectCameraPreset(view: 'FRONT' | 'BACK' | 'LEFT' | 'RIGHT') {
    activeCameraPreset = view;
    if (autoRotate) {
      autoRotate = false;
      engine?.setAutoRotate(false);
    }
    engine?.setCameraAngle(view);
  }

  function toggleAutoRotate() {
    autoRotate = !autoRotate;
    if (autoRotate) {
      activeCameraPreset = null;
    }
    engine?.setAutoRotate(autoRotate);
  }

  function switchLighting(preset: LightingPreset) {
    studio.setLightingPreset(preset);
    engine?.setLightingPreset(preset);
  }

  function switchBackground(bg: ViewportBackground) {
    studio.setViewportBackground(bg);
    engine?.setViewportBackground(bg);
  }

  function switchFabric(fabric: FabricType) {
    studio.setFabricType(fabric);
    engine?.setFabricType(fabric);
  }

  function switchFinish(finish: 'MATTE_MESH' | 'PRO_VAPOR' | 'RETRO_SATIN') {
    studio.activeKit.metadata.finishType = finish;
    studio.textureRevision++;
    engine?.setFinishType(finish);
  }
</script>

<div class="relative w-full h-full min-h-[500px] bg-[#0F141C] rounded-2xl border border-[#2E3D56] overflow-hidden flex flex-col shadow-2xl">
  <!-- Minimal Specs Pill (Top-Left) -->
  <div class="absolute top-3 left-3 z-30 pointer-events-none hidden xl:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-[#0F141C]/80 backdrop-blur-md border border-[#2E3D56]/60 text-xs font-mono-tabular text-[#94A3B8] shadow-lg">
    <span class="w-2 h-2 rounded-full bg-[#00C49F] animate-pulse"></span>
    <span class="text-white font-semibold">3D PBR Studio</span>
    <span aria-hidden="true" class="text-[#64748B]">·</span>
    <span class="text-[#00C49F] font-bold">{fpsValue} FPS</span>
  </div>

  <!-- FLOATING CONTROL BAR OVER 3D VIEWPORT (Material Design 3 Glassmorphism) -->
  <div class="absolute top-3 left-1/2 -translate-x-1/2 z-20 w-[96%] max-w-4xl bg-[#141B26]/85 backdrop-blur-md border border-[#2E3D56]/70 rounded-2xl p-2 shadow-2xl flex flex-wrap items-center justify-between gap-2.5 transition-all">
    <!-- 1. Viste Rapide (Camera Presets) -->
    <div class="flex items-center gap-1 bg-[#0F141C]/80 border border-[#2E3D56]/50 rounded-xl p-1">
      <span class="text-[10px] uppercase font-bold text-[#64748B] px-1.5 hidden md:inline">Vista:</span>
      <button
        type="button"
        id="btn-cam-front"
        onclick={() => selectCameraPreset('FRONT')}
        class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer {activeCameraPreset === 'FRONT' && !autoRotate
          ? 'bg-[#0F52BA] text-white shadow-sm shadow-[#0F52BA]/30'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
        title="Vista Frontale"
      >
        Fronte
      </button>
      <button
        type="button"
        id="btn-cam-back"
        onclick={() => selectCameraPreset('BACK')}
        class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer {activeCameraPreset === 'BACK' && !autoRotate
          ? 'bg-[#0F52BA] text-white shadow-sm shadow-[#0F52BA]/30'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
        title="Vista Posteriore"
      >
        Retro
      </button>
      <button
        type="button"
        id="btn-cam-left"
        onclick={() => selectCameraPreset('LEFT')}
        class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer {activeCameraPreset === 'LEFT' && !autoRotate
          ? 'bg-[#0F52BA] text-white shadow-sm shadow-[#0F52BA]/30'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
        title="Vista Fianco Sinistro"
      >
        Lato SX
      </button>
      <button
        type="button"
        id="btn-cam-right"
        onclick={() => selectCameraPreset('RIGHT')}
        class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer {activeCameraPreset === 'RIGHT' && !autoRotate
          ? 'bg-[#0F52BA] text-white shadow-sm shadow-[#0F52BA]/30'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
        title="Vista Fianco Destro"
      >
        Lato DX
      </button>
      <button
        type="button"
        id="btn-cam-rotate"
        onclick={toggleAutoRotate}
        class="px-2.5 py-1 text-xs font-bold rounded-lg transition-all cursor-pointer flex items-center gap-1 {autoRotate
          ? 'bg-[#00C49F] text-[#0F141C] shadow-md shadow-[#00C49F]/30'
          : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
        title="Rotazione Continua Orbitale 360°"
      >
        <span>🔄</span>
        <span>360°</span>
      </button>
    </div>

    <!-- 2. Controlli Luce (Lighting Presets) -->
    <div class="flex items-center gap-1 bg-[#0F141C]/80 border border-[#2E3D56]/50 rounded-xl p-1">
      <span class="text-[10px] uppercase font-bold text-[#64748B] px-1.5 hidden md:inline">Luce:</span>
      {#each [
        { id: 'studio', label: 'Studio', emoji: '💡' },
        { id: 'stadium', label: 'Stadio', emoji: '🏟️' },
        { id: 'sunset', label: 'Tramonto', emoji: '🌅' },
        { id: 'radiant', label: 'Radiante', emoji: '✨' }
      ] as item (item.id)}
        <button
          type="button"
          id="btn-light-{item.id}"
          onclick={() => switchLighting(item.id as LightingPreset)}
          class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer flex items-center gap-1 {studio.lightingPreset === item.id
            ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30'
            : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
          title="Luce {item.label}"
        >
          <span>{item.emoji}</span>
          <span class="hidden sm:inline">{item.label}</span>
        </button>
      {/each}
    </div>

    <!-- 3. Toggle Manichino & Vista Anatomica -->
    <div class="flex items-center gap-1 bg-[#0F141C]/80 border border-[#2E3D56]/50 rounded-xl p-1">
      <button
        type="button"
        id="btn-toggle-mannequin"
        onclick={() => studio.toggleMannequin()}
        class="px-2.5 py-1 text-xs font-semibold rounded-lg border transition-all cursor-pointer flex items-center gap-1.5 {studio.showMannequin
          ? 'bg-[#253146] border-[#00C49F]/50 text-[#00C49F]'
          : 'border-transparent text-[#64748B] hover:text-[#94A3B8] hover:bg-[#1E293B]'}"
        title="Mostra / Nascondi Manichino Sartoriale"
      >
        <span>👤</span>
        <span class="hidden sm:inline">Manichino</span>
      </button>
      <button
        type="button"
        id="btn-toggle-anatomy"
        onclick={() => studio.toggleAnatomicalView()}
        class="px-2.5 py-1 text-xs font-semibold rounded-lg border transition-all cursor-pointer flex items-center gap-1.5 {studio.showAnatomicalView
          ? 'bg-[#00E5FF]/20 border-[#00E5FF]/70 text-[#00E5FF] shadow-sm shadow-[#00E5FF]/20 font-bold'
          : 'border-transparent text-[#64748B] hover:text-[#94A3B8] hover:bg-[#1E293B]'}"
        title="Vista Anatomica: Visualizza cuciture, taglio raglan e linee sartoriali 3D"
      >
        <span>✂️</span>
        <span class="hidden sm:inline">Anatomica</span>
      </button>
    </div>

    <!-- 4. Sfondo (Background Presets) -->
    <div class="flex items-center gap-1 bg-[#0F141C]/80 border border-[#2E3D56]/50 rounded-xl p-1">
      <span class="text-[10px] uppercase font-bold text-[#64748B] px-1.5 hidden md:inline">Sfondo:</span>
      {#each [
        { id: 'dark', label: 'Scuro', emoji: '⬛' },
        { id: 'neutral', label: 'Neutro', emoji: '🏢' },
        { id: 'pitch', label: 'Campo', emoji: '⚽' }
      ] as bg (bg.id)}
        <button
          type="button"
          id="btn-bg-{bg.id}"
          onclick={() => switchBackground(bg.id as ViewportBackground)}
          class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer flex items-center gap-1 {studio.viewportBackground === bg.id
            ? 'bg-[#253146] text-white border border-[#38BDF8] shadow-sm'
            : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
          title="Sfondo 3D: {bg.label}"
        >
          <span>{bg.emoji}</span>
          <span>{bg.label}</span>
        </button>
      {/each}
    </div>

    <!-- 5. Tessuto & Materiale (Fabric Engine) -->
    <div class="flex items-center gap-1 bg-[#0F141C]/80 border border-[#2E3D56]/50 rounded-xl p-1">
      <span class="text-[10px] uppercase font-bold text-[#64748B] px-1.5 hidden md:inline">Tessuto:</span>
      {#each [
        { id: 'MESH', label: 'Mesh', emoji: '🎽', title: 'Mesh Traspirante (Micro-fori opachi)' },
        { id: 'POLYESTER', label: 'Poliestere', emoji: '✨', title: 'Poliestere Lucido (Riflessi plastici)' },
        { id: 'COTTON', label: 'Cotone', emoji: '👕', title: "Cotone Anni '90 (Fibre ruvide opache)" },
        { id: 'JACQUARD', label: 'Jacquard', emoji: '🏛️', title: 'Jacquard (Trama damascata in rilievo)' }
      ] as f (f.id)}
        <button
          type="button"
          id="btn-fabric-{f.id.toLowerCase()}"
          onclick={() => switchFabric(f.id as FabricType)}
          class="px-2.5 py-1 text-xs font-semibold rounded-lg transition-all cursor-pointer flex items-center gap-1 {studio.fabricType === f.id
            ? 'bg-[#00C49F] text-[#0F141C] font-extrabold shadow-md shadow-[#00C49F]/30 ring-1 ring-white/30'
            : 'text-[#94A3B8] hover:text-white hover:bg-[#1E293B]'}"
          title={f.title}
        >
          <span>{f.emoji}</span>
          <span class="hidden sm:inline">{f.label}</span>
        </button>
      {/each}
    </div>
  </div>

  <!-- Isolated WebGL Mount Container -->
  <div
    bind:this={mountContainer}
    class="w-full h-full flex-1 cursor-grab active:cursor-grabbing"
    aria-label="Modello 3D interattivo della divisa sportiva"
  ></div>

  {#if isLoadingEngine}
    <div class="absolute inset-0 z-40 bg-[#0F141C]/90 flex flex-col items-center justify-center gap-3 p-6 text-center">
      <div class="w-10 h-10 rounded-full border-2 border-[#0F52BA] border-t-[#00C49F] animate-spin"></div>
      <div class="text-sm font-semibold text-white">Caricamento Lazy Motore 3D (Three.js + KTX2 + Draco)…</div>
      <div class="text-xs text-[#94A3B8] font-mono-tabular">Proiezione UV 2048×2048 su mesh sartoriale PBR</div>
    </div>
  {:else if loadError}
    <div class="absolute inset-0 z-40 bg-[#0F141C]/95 flex flex-col items-center justify-center gap-2 p-6 text-center">
      <div class="text-sm font-semibold text-[#EF4444]">{loadError}</div>
      <div class="text-xs text-[#94A3B8]">Utilizza la Vista 2D ad alta definizione 2048×2048.</div>
    </div>
  {/if}

  <!-- Bottom Floating Fabric Finish Pill (Discreet, Zero Viewport Occlusion) -->
  <div class="absolute bottom-3 right-3 z-10 hidden sm:flex items-center gap-1.5 p-1 bg-[#141B26]/85 backdrop-blur-md border border-[#2E3D56]/70 rounded-xl text-xs font-mono-tabular text-[#94A3B8] shadow-xl">
    <span class="text-[10px] uppercase font-bold text-[#64748B] px-1">Tessuto:</span>
    <button
      type="button"
      onclick={() => switchFinish('PRO_VAPOR')}
      class="px-2 py-0.5 rounded-lg transition-colors cursor-pointer {studio.activeKit.metadata?.finishType === 'PRO_VAPOR'
        ? 'bg-[#253146] text-[#00C49F] font-semibold'
        : 'hover:text-white'}"
    >
      Vapor
    </button>
    <button
      type="button"
      onclick={() => switchFinish('MATTE_MESH')}
      class="px-2 py-0.5 rounded-lg transition-colors cursor-pointer {studio.activeKit.metadata?.finishType === 'MATTE_MESH'
        ? 'bg-[#253146] text-[#00C49F] font-semibold'
        : 'hover:text-white'}"
    >
      Matte
    </button>
    <button
      type="button"
      onclick={() => switchFinish('RETRO_SATIN')}
      class="px-2 py-0.5 rounded-lg transition-colors cursor-pointer {studio.activeKit.metadata?.finishType === 'RETRO_SATIN'
        ? 'bg-[#253146] text-[#00C49F] font-semibold'
        : 'hover:text-white'}"
    >
      Satin
    </button>
  </div>
</div>
