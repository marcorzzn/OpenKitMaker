// Implements: SCR-EDITOR / FUN-MANAGE-LAYERS, FUN-EDIT-COLOR, FUN-COMPOSITE-2D, FUN-PREVIEW-3D, FUN-SAVE-LOCAL
<script lang="ts">
  import { onMount } from 'svelte';
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import {
    compositeJersey2048,
    compositeShorts2048,
    compositeSocks2048,
    compositeGarment2048,
    renderJerseyMockup512,
    renderShortsMockup512,
    renderSocksMockup512,
    renderFullKitMockup512,
    TEXTURE_SIZE,
  } from '../lib/compositor2d';
  import ColorPickerHsv from './ColorPickerHsv.svelte';
  import Viewport3D from './Viewport3D.svelte';
  import type {
    LayerType,
    BlendMode,
    PatternStyle,
    CollarStyle,
    SponsorGeometry,
    BadgeShape,
    BadgePosition,
    KitCategory,
    GarmentType,
  } from '../types/kit';

  let canvas2dRef = $state<HTMLCanvasElement | null>(null);
  let mockupCanvasRef = $state<HTMLCanvasElement | null>(null);
  let displayMode2d = $state<'MOCKUP' | 'UV'>('MOCKUP');
  let zoom2d = $state<number>(1.0);

  let jerseyCanvas = $state<HTMLCanvasElement | null>(null);
  let shortsCanvas = $state<HTMLCanvasElement | null>(null);
  let socksCanvas = $state<HTMLCanvasElement | null>(null);

  const allRailItems: { type: LayerType; label: string; shortCode: string }[] = [
    { type: 'BASE', label: 'Base', shortCode: 'BAS' },
    { type: 'PATTERN', label: 'Strisce', shortCode: 'STR' },
    { type: 'COLLAR', label: 'Dettaglio', shortCode: 'DET' },
    { type: 'SLEEVES', label: 'Maniche', shortCode: 'MAN' },
    { type: 'BADGE', label: 'Stemma', shortCode: 'STM' },
    { type: 'SPONSOR', label: 'Sponsor', shortCode: 'SPO' },
    { type: 'TEXT_NAME', label: 'Nome', shortCode: 'NOM' },
    { type: 'NUMBER', label: 'Numero', shortCode: 'NUM' },
  ];

  const garmentAllowedTypes: Record<GarmentType, LayerType[]> = {
    JERSEY: ['BASE', 'PATTERN', 'COLLAR', 'SLEEVES', 'BADGE', 'SPONSOR', 'TEXT_NAME', 'NUMBER'],
    SHORTS: ['BASE', 'PATTERN', 'BADGE', 'NUMBER'],
    SOCKS: ['BASE', 'PATTERN', 'COLLAR'],
  };

  let componentRailItems = $derived(
    studio.activeGarmentType !== 'ALL'
      ? allRailItems.filter((item) => garmentAllowedTypes[studio.activeGarmentType].includes(item.type))
      : allRailItems
  );

  const blendModes: BlendMode[] = ['NORMAL', 'MULTIPLY', 'OVERLAY'];

  const patternStyles: { value: PatternStyle; label: string }[] = [
    { value: 'VERTICAL_STRIPES', label: 'Strisce Verticali' },
    { value: 'HORIZONTAL_HOOPS', label: 'Cerchi Orizzontali' },
    { value: 'DIAGONAL_SASH', label: 'Banda Diagonale' },
    { value: 'CHEVRON_BREAST', label: 'Scaglione Petto (V)' },
    { value: 'HALVES', label: 'Partito 50/50' },
    { value: 'CHECKERED_GEOMETRIC', label: 'Scacchi Geometrici' },
    { value: 'CROSS_GRID', label: 'Croce Sartoriale' },
    { value: 'PINSTRIPES', label: 'Gessato Fine' },
    { value: 'RADIAL_RAYS', label: 'Raggi Solari' },
  ];

  const collarStyles: { value: CollarStyle; label: string }[] = [
    { value: 'V_NECK', label: 'Scollo a V' },
    { value: 'ROUND_CREW', label: 'Girocollo Classico' },
    { value: 'POLO_FOLDED', label: 'Colletto Polo' },
    { value: 'MANDARIN_TRIM', label: 'Coreana Tecnico' },
  ];

  const sponsorGeometries: { value: SponsorGeometry; label: string }[] = [
    { value: 'HEX_TECH_BAR', label: 'Esagono + Barra' },
    { value: 'TRIPLE_DIAMOND', label: 'Triplo Diamante' },
    { value: 'GEOMETRIC_WORDMARK', label: 'Cornice Geometrica' },
    { value: 'ABSTRACT_WAVE', label: 'Onda Dinamica' },
  ];

  const badgeShapes: { value: BadgeShape; label: string }[] = [
    { value: 'HEXAGON_PRO', label: 'Esagono Pro' },
    { value: 'SHIELD_CLASSIC', label: 'Scudo Classico' },
    { value: 'CIRCLE_MONO', label: 'Cerchio Concentrico' },
    { value: 'DIAMOND_CREST', label: 'Rombo Sartoriale' },
  ];

  const badgePositions: { value: BadgePosition; label: string }[] = [
    { value: 'LEFT_CHEST', label: 'Petto SX' },
    { value: 'CENTER_CHEST', label: 'Centro Petto' },
    { value: 'RIGHT_CHEST', label: 'Petto DX' },
  ];

  const kitCategories: KitCategory[] = ['HOME', 'AWAY', 'THIRD', 'GK', 'SPECIAL'];

  function ensureOffscreenCanvases() {
    if (!jerseyCanvas) {
      jerseyCanvas = document.createElement('canvas');
      jerseyCanvas.width = TEXTURE_SIZE;
      jerseyCanvas.height = TEXTURE_SIZE;
    }
    if (!shortsCanvas) {
      shortsCanvas = document.createElement('canvas');
      shortsCanvas.width = TEXTURE_SIZE;
      shortsCanvas.height = TEXTURE_SIZE;
    }
    if (!socksCanvas) {
      socksCanvas = document.createElement('canvas');
      socksCanvas.width = TEXTURE_SIZE;
      socksCanvas.height = TEXTURE_SIZE;
    }
  }

  function redraw2DTexture() {
    ensureOffscreenCanvases();
    const t0 = performance.now();

    // 1. Composite all 3 garments in memory
    if (jerseyCanvas) {
      compositeJersey2048(jerseyCanvas, studio.activeKit, { showUvGuides: studio.showUvGuides });
    }
    if (shortsCanvas) {
      compositeShorts2048(shortsCanvas, studio.activeKit, { showUvGuides: studio.showUvGuides });
    }
    if (socksCanvas) {
      compositeSocks2048(socksCanvas, studio.activeKit, { showUvGuides: studio.showUvGuides });
    }

    // 2. Render visible 2048x2048 UV texture
    if (canvas2dRef) {
      const activeGarment = studio.activeGarmentType === 'ALL' ? 'JERSEY' : studio.activeGarmentType;
      compositeGarment2048(canvas2dRef, studio.activeKit, activeGarment, {
        showUvGuides: studio.showUvGuides,
      });
    }

    // 3. Render 2D Mockup
    if (mockupCanvasRef) {
      if (studio.activeGarmentType === 'JERSEY' && jerseyCanvas) {
        renderJerseyMockup512(mockupCanvasRef, jerseyCanvas, studio.activeKit);
      } else if (studio.activeGarmentType === 'SHORTS' && shortsCanvas) {
        renderShortsMockup512(mockupCanvasRef, shortsCanvas, studio.activeKit);
      } else if (studio.activeGarmentType === 'SOCKS' && socksCanvas) {
        renderSocksMockup512(mockupCanvasRef, socksCanvas, studio.activeKit);
      } else {
        renderFullKitMockup512(mockupCanvasRef, studio.activeKit);
      }
    }

    const elapsed = performance.now() - t0;
    studio.lastCompositeTimeMs = Math.round(elapsed * 10) / 10;
  }

  onMount(() => {
    ensureOffscreenCanvases();
    redraw2DTexture();
  });

  // Re-run 2048x2048 compositor whenever textureRevision, garment type, or UV guide toggle changes
  $effect(() => {
    const _rev = studio.textureRevision;
    const _guides = studio.showUvGuides;
    const _garment = studio.activeGarmentType;
    if (canvas2dRef && _rev >= 0 && typeof _guides === 'boolean' && _garment) {
      redraw2DTexture();
    }
  });
</script>

<section class="max-w-[1600px] mx-auto px-4 sm:px-6 py-4 space-y-4">
  <!-- Top Studio Editor Sub-Navigation Bar (SCR-EDITOR) -->
  <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl px-4 py-3 flex flex-wrap items-center justify-between gap-4">
    <!-- Editable Kit Title & Category -->
    <div class="flex items-center gap-3 min-w-0 flex-1">
      <input
        type="text"
        value={studio.activeKit.name}
        oninput={(e) => {
          studio.activeKit.name = (e.target as HTMLInputElement).value;
          studio.activeKit.updatedAt = new Date().toISOString();
          studio.textureRevision++;
        }}
        aria-label="Titolo del Kit"
        class="px-3 py-1.5 text-base font-bold font-display tracking-wide bg-[#0F141C] border border-[#2E3D56] rounded-lg text-white focus:outline-none focus:border-[#00C49F] w-52 sm:w-64"
      />

      <select
        value={studio.activeKit.category}
        onchange={(e) => {
          studio.activeKit.category = (e.target as HTMLSelectElement).value as KitCategory;
          studio.activeKit.updatedAt = new Date().toISOString();
          studio.textureRevision++;
        }}
        aria-label="Categoria Kit"
        class="px-2.5 py-1.5 text-xs font-semibold bg-[#0F141C] border border-[#2E3D56] rounded-lg text-[#94A3B8] focus:outline-none focus:border-[#00C49F]"
      >
        {#each kitCategories as cat (cat)}
          <option value={cat}>{i18n.catalog.categories[cat]}</option>
        {/each}
      </select>

      <span class="hidden xl:inline-block text-xs text-[#64748B] font-mono-tabular whitespace-nowrap">
        2048×2048 RGBA · Render: {studio.lastCompositeTimeMs} ms
      </span>
    </div>

    <!-- View Mode Selector (Vista 2D | Vista 3D | Vista Doppia) -->
    <div class="inline-flex items-center gap-1 p-1 bg-[#0F141C] border border-[#2E3D56] rounded-lg">
      <button
        type="button"
        onclick={() => (studio.editorViewMode = '2D')}
        class="px-3 py-1.5 text-xs font-semibold rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.editorViewMode === '2D'
          ? 'bg-[#0F52BA] text-white'
          : 'text-[#94A3B8] hover:text-white'}"
      >
        {i18n.editor.view_2d}
      </button>
      <button
        type="button"
        onclick={() => (studio.editorViewMode = '3D')}
        class="px-3 py-1.5 text-xs font-semibold rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.editorViewMode === '3D'
          ? 'bg-[#0F52BA] text-white'
          : 'text-[#94A3B8] hover:text-white'}"
      >
        {i18n.editor.view_3d}
      </button>
      <button
        type="button"
        onclick={() => (studio.editorViewMode = 'SPLIT')}
        class="px-3 py-1.5 text-xs font-semibold rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.editorViewMode === 'SPLIT'
          ? 'bg-[#0F52BA] text-white'
          : 'text-[#94A3B8] hover:text-white'}"
      >
        {i18n.editor.view_split}
      </button>
    </div>

    <!-- Save Draft & Export Actions -->
    <div class="flex items-center gap-2.5 shrink-0">
      <button
        type="button"
        onclick={() => studio.saveActiveKitToLocal()}
        class="px-4 py-2 text-xs font-semibold text-white bg-[#253146] hover:bg-[#2E3D56] border border-[#2E3D56] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        {i18n.editor.save_draft}
      </button>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-EXPORT')}
        class="px-4 py-2 text-xs font-semibold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        {i18n.editor.export_btn} →
      </button>
    </div>
  </div>

  <!-- Horizontal Garment Selector Bar & 3D Mannequin Switcher (FASE 3 / FUN-EDIT-SHORTS / FUN-EDIT-SOCKS / FUN-MANNEQUIN-TOGGLE) -->
  <div class="bg-[#141B26] border border-[#2E3D56] rounded-xl px-4 py-2.5 flex flex-wrap items-center justify-between gap-3 shadow-md">
    <div class="flex items-center gap-2 overflow-x-auto">
      <span class="text-xs font-bold uppercase tracking-wider text-[#64748B] mr-1 hidden sm:inline">Capo da Modificare:</span>

      <button
        type="button"
        onclick={() => studio.setGarmentType('JERSEY')}
        class="px-3.5 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'JERSEY'
          ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-2 ring-[#38BDF8]'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">👕</span>
        <span>Maglia</span>
      </button>

      <button
        type="button"
        onclick={() => studio.setGarmentType('SHORTS')}
        class="px-3.5 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'SHORTS'
          ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-2 ring-[#38BDF8]'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">🩳</span>
        <span>Pantaloncini</span>
      </button>

      <button
        type="button"
        onclick={() => studio.setGarmentType('SOCKS')}
        class="px-3.5 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'SOCKS'
          ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-2 ring-[#38BDF8]'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">🧦</span>
        <span>Calzettoni</span>
      </button>

      <button
        type="button"
        onclick={() => studio.setGarmentType('ALL')}
        class="px-3.5 py-2 text-xs font-bold rounded-lg transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'ALL'
          ? 'bg-[#00C49F] text-[#0F141C] font-extrabold shadow-md shadow-[#00C49F]/30 ring-2 ring-white'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">👤</span>
        <span>Vista Completo</span>
      </button>
    </div>

    <!-- Mannequin Toggle Switch (FUN-MANNEQUIN-TOGGLE) -->
    <div class="flex items-center gap-2">
      <button
        type="button"
        onclick={() => studio.toggleMannequin()}
        class="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all flex items-center gap-2 cursor-pointer {studio.showMannequin
          ? 'bg-[#253146] border-[#00C49F] text-[#00C49F]'
          : 'bg-[#1A2230] border-[#2E3D56] text-[#64748B] hover:text-[#94A3B8]'}"
      >
        <span>👤</span>
        <span>Mostra Manichino</span>
        <span class="w-2.5 h-2.5 rounded-full {studio.showMannequin ? 'bg-[#00C49F] shadow-[0_0_8px_#00C49F]' : 'bg-[#64748B]'}"></span>
      </button>
    </div>
  </div>

  <!-- Main Editor Studio Grid -->
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
    {#if studio.activeGarmentType !== 'ALL'}
      <!-- Left Column: Component Icon Rail + Layer Stack + Inspector + HSV Picker (lg:col-span-4) -->
      <div class="lg:col-span-4 bg-[#1A2230] border border-[#2E3D56] rounded-xl overflow-hidden flex flex-col">
        <!-- Component Icon Rail -->
        <div class="p-2 bg-[#0F141C] border-b border-[#2E3D56] grid grid-cols-4 sm:grid-cols-8 lg:grid-cols-4 gap-1.5">
          {#each componentRailItems as item (item.type)}
            {@const isSelectedType = studio.selectedLayer?.type === item.type}
            <button
              type="button"
              onclick={() => studio.selectLayerByType(item.type)}
              class="px-2 py-2 rounded-lg text-center transition-colors cursor-pointer flex flex-col items-center justify-center gap-0.5 {isSelectedType
                ? 'bg-[#0F52BA] text-white'
                : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
            >
              <span class="text-[10px] font-mono-tabular opacity-75">{item.shortCode}</span>
              <span class="text-xs font-semibold whitespace-nowrap">{item.label}</span>
            </button>
          {/each}
        </div>

        <div class="p-4 space-y-5 max-h-[calc(100vh-180px)] overflow-y-auto">
          <!-- Layer Stack Manager (FUN-MANAGE-LAYERS) -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-white">
                Pila Livelli {studio.activeGarmentType === 'JERSEY' ? 'Maglia' : studio.activeGarmentType === 'SHORTS' ? 'Pantaloncini' : 'Calzettoni'} ({studio.activeLayers.length})
              </span>
              <button
                type="button"
                onclick={() =>
                  studio.addNewLayer(studio.selectedLayer?.type || 'PATTERN')}
                class="px-2.5 py-1 text-xs font-semibold text-[#00C49F] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-md cursor-pointer whitespace-nowrap"
              >
                + Nuovo Livello
              </button>
            </div>

            <div class="space-y-3 max-h-64 overflow-y-auto pr-1">
              {#each [...studio.activeLayers].sort((a, b) => b.zIndex - a.zIndex) as layer (layer.id)}
                {@const isCurrent = studio.selectedLayer?.id === layer.id}
                <div
                  class="flex flex-col gap-2.5 p-4 rounded-xl border shadow-sm transition-all {isCurrent
                    ? 'bg-[#253146] border-[#00C49F] shadow-md ring-1 ring-[#00C49F]/30'
                    : 'bg-[#0F141C] border-[#2E3D56] hover:border-[#64748B]'}"
                >
                  <div class="flex items-center justify-between gap-3">
                  <button
                    type="button"
                    onclick={() => (studio.selectedLayerId = layer.id)}
                    class="flex items-center gap-3 min-w-0 flex-1 text-left cursor-pointer"
                  >
                    <span
                      class="w-4 h-4 rounded-full border border-white/40 shrink-0 shadow-inner"
                      style="background-color: {layer.color};"
                    ></span>
                    <div class="min-w-0 flex-1">
                      <div class="text-sm font-semibold text-white truncate">{layer.name}</div>
                      <div class="text-[11px] text-[#94A3B8] font-mono-tabular">
                        {i18n.editor.component_rail[layer.type]} · z:{layer.zIndex} · {Math.round(layer.opacity * 100)}%
                      </div>
                    </div>
                  </button>

                  <div class="flex items-center gap-1.5 shrink-0">
                    <button
                      type="button"
                      onclick={() => studio.toggleLayerVisibility(layer.id)}
                      class="px-2 py-1 text-[11px] font-mono-tabular font-semibold rounded-md transition-colors {layer.visible
                        ? 'bg-[#10B981]/20 text-[#10B981] border border-[#10B981]/40'
                        : 'bg-[#EF4444]/20 text-[#EF4444] border border-[#EF4444]/40'} cursor-pointer"
                      title={layer.visible ? 'Nascondi livello' : 'Mostra livello'}
                    >
                      {layer.visible ? 'ON' : 'OFF'}
                    </button>
                    <button
                      type="button"
                      onclick={() => studio.moveLayerZIndex(layer.id, 'UP')}
                      class="w-7 h-7 flex items-center justify-center rounded-md bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#2E3D56] cursor-pointer text-xs font-bold"
                      title="Sposta su (z-index +)"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      onclick={() => studio.moveLayerZIndex(layer.id, 'DOWN')}
                      class="w-7 h-7 flex items-center justify-center rounded-md bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#2E3D56] cursor-pointer text-xs font-bold"
                      title="Sposta giù (z-index -)"
                    >
                      ↓
                    </button>
                  </div>
                </div>
              </div>
            {/each}
          </div>
        </div>

        <!-- Layer Inspector (Opacity, Blend Mode, Component-Specific Properties) -->
        {#if studio.selectedLayer}
          {@const activeLayer = studio.selectedLayer}
          <div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-4 space-y-4">
            <div class="flex items-center justify-between gap-2 border-b border-[#2E3D56] pb-2.5">
              <input
                type="text"
                value={activeLayer.name}
                oninput={(e) => {
                  activeLayer.name = (e.target as HTMLInputElement).value;
                  studio.textureRevision++;
                }}
                class="text-xs font-semibold text-white bg-transparent border-b border-transparent focus:border-[#00C49F] focus:outline-none flex-1"
              />
              <button
                type="button"
                onclick={() => studio.deleteLayer(activeLayer.id)}
                class="text-xs text-[#EF4444] hover:underline cursor-pointer whitespace-nowrap"
              >
                Elimina
              </button>
            </div>

            <!-- Opacity Slider (0.0 .. 1.0) & Blend Mode (NORMAL, MULTIPLY, OVERLAY) -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                  <span>Opacità</span>
                  <span>{activeLayer.opacity.toFixed(2)}</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  value={activeLayer.opacity}
                  oninput={(e) =>
                    studio.setLayerOpacity(
                      activeLayer.id,
                      parseFloat((e.target as HTMLInputElement).value)
                    )}
                  class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                />
              </div>

              <div>
                <label for="blend-mode-select" class="block text-xs text-[#94A3B8] mb-1">
                  Blend Mode
                </label>
                <select
                  id="blend-mode-select"
                  value={activeLayer.blendMode}
                  onchange={(e) =>
                    studio.setLayerBlendMode(
                      activeLayer.id,
                      (e.target as HTMLSelectElement).value as BlendMode
                    )}
                  class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white focus:outline-none focus:border-[#00C49F]"
                >
                  {#each blendModes as bm (bm)}
                    <option value={bm}>{bm}</option>
                  {/each}
                </select>
              </div>
            </div>

            <!-- Contextual Properties Inspector by LayerType -->
            {#if activeLayer.type === 'PATTERN'}
              <div class="space-y-3 pt-2 border-t border-[#2E3D56]">
                <div>
                  <label for="pattern-style" class="block text-xs text-[#94A3B8] mb-1">
                    Geometria Pattern / Strisce
                  </label>
                  <select
                    id="pattern-style"
                    value={activeLayer.properties.patternStyle || 'VERTICAL_STRIPES'}
                    onchange={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'patternStyle',
                        (e.target as HTMLSelectElement).value
                      )}
                    class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                  >
                    {#each patternStyles as ps (ps.value)}
                      <option value={ps.value}>{ps.label}</option>
                    {/each}
                  </select>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Ripetizioni</span>
                      <span>{activeLayer.properties.stripeCount ?? 5}</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="14"
                      step="1"
                      value={activeLayer.properties.stripeCount ?? 5}
                      oninput={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'stripeCount',
                          parseInt((e.target as HTMLInputElement).value, 10)
                        )}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>

                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Spessore</span>
                      <span>{Math.round(Number(activeLayer.properties.stripeWidth ?? 0.48) * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.15"
                      max="0.85"
                      step="0.02"
                      value={activeLayer.properties.stripeWidth ?? 0.48}
                      oninput={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'stripeWidth',
                          parseFloat((e.target as HTMLInputElement).value)
                        )}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>
                </div>
              </div>
            {:else if activeLayer.type === 'COLLAR'}
              <div class="grid grid-cols-2 gap-3 pt-2 border-t border-[#2E3D56]">
                <div>
                  <label for="collar-style" class="block text-xs text-[#94A3B8] mb-1">
                    Taglio Colletto
                  </label>
                  <select
                    id="collar-style"
                    value={activeLayer.properties.collarStyle || 'V_NECK'}
                    onchange={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'collarStyle',
                        (e.target as HTMLSelectElement).value
                      )}
                    class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                  >
                    {#each collarStyles as cs (cs.value)}
                      <option value={cs.value}>{cs.label}</option>
                    {/each}
                  </select>
                </div>

                <div>
                  <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                    <span>Bordo Trim</span>
                    <span>{activeLayer.properties.trimWidth ?? 16}px</span>
                  </div>
                  <input
                    type="range"
                    min="8"
                    max="36"
                    step="1"
                    value={activeLayer.properties.trimWidth ?? 16}
                    oninput={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'trimWidth',
                        parseInt((e.target as HTMLInputElement).value, 10)
                      )}
                    class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                  />
                </div>
              </div>
            {:else if activeLayer.type === 'SLEEVES'}
              <div class="grid grid-cols-2 gap-3 pt-2 border-t border-[#2E3D56] items-center">
                <div>
                  <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                    <span>Polsino</span>
                    <span>{activeLayer.properties.cuffWidth ?? 34}px</span>
                  </div>
                  <input
                    type="range"
                    min="12"
                    max="60"
                    step="2"
                    value={activeLayer.properties.cuffWidth ?? 34}
                    oninput={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'cuffWidth',
                        parseInt((e.target as HTMLInputElement).value, 10)
                      )}
                    class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                  />
                </div>

                <label class="flex items-center gap-2 text-xs text-white cursor-pointer pt-3">
                  <input
                    type="checkbox"
                    checked={Boolean(activeLayer.properties.raglanCut ?? true)}
                    onchange={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'raglanCut',
                        (e.target as HTMLInputElement).checked
                      )}
                    class="accent-[#00C49F]"
                  />
                  Taglio Spalla Raglan
                </label>
              </div>
            {:else if activeLayer.type === 'BADGE'}
              <div class="grid grid-cols-2 gap-3 pt-2 border-t border-[#2E3D56]">
                <div>
                  <label for="badge-shape" class="block text-xs text-[#94A3B8] mb-1">
                    Sagoma Stemma
                  </label>
                  <select
                    id="badge-shape"
                    value={activeLayer.properties.badgeShape || 'HEXAGON_PRO'}
                    onchange={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'badgeShape',
                        (e.target as HTMLSelectElement).value
                      )}
                    class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                  >
                    {#each badgeShapes as bs (bs.value)}
                      <option value={bs.value}>{bs.label}</option>
                    {/each}
                  </select>
                </div>

                <div>
                  <label for="badge-pos" class="block text-xs text-[#94A3B8] mb-1">
                    Posizione Petto
                  </label>
                  <select
                    id="badge-pos"
                    value={activeLayer.properties.badgePosition || 'LEFT_CHEST'}
                    onchange={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'badgePosition',
                        (e.target as HTMLSelectElement).value
                      )}
                    class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                  >
                    {#each badgePositions as bp (bp.value)}
                      <option value={bp.value}>{bp.label}</option>
                    {/each}
                  </select>
                </div>
              </div>
            {:else if activeLayer.type === 'SPONSOR'}
              <div class="space-y-3 pt-2 border-t border-[#2E3D56]">
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label for="sponsor-geom" class="block text-xs text-[#94A3B8] mb-1">
                      Motivo Geometrico
                    </label>
                    <select
                      id="sponsor-geom"
                      value={activeLayer.properties.sponsorGeometry || 'HEX_TECH_BAR'}
                      onchange={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'sponsorGeometry',
                          (e.target as HTMLSelectElement).value
                        )}
                      class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                    >
                      {#each sponsorGeometries as sg (sg.value)}
                        <option value={sg.value}>{sg.label}</option>
                      {/each}
                    </select>
                  </div>

                  <div>
                    <label for="sponsor-label" class="block text-xs text-[#94A3B8] mb-1">
                      Testo Placeholder
                    </label>
                    <input
                      id="sponsor-label"
                      type="text"
                      maxlength="14"
                      value={activeLayer.properties.sponsorLabel ?? 'AERO·LAB'}
                      oninput={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'sponsorLabel',
                          (e.target as HTMLInputElement).value
                        )}
                      class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                    />
                  </div>
                </div>
              </div>
            {:else if activeLayer.type === 'TEXT_NAME'}
              <div class="space-y-3 pt-2 border-t border-[#2E3D56]">
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label for="player-name-input" class="block text-xs text-[#94A3B8] mb-1">
                      Nome Dorsale
                    </label>
                    <input
                      id="player-name-input"
                      type="text"
                      maxlength="16"
                      value={activeLayer.properties.text ?? 'ROSSI'}
                      oninput={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'text',
                          (e.target as HTMLInputElement).value
                        )}
                      class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                    />
                  </div>

                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Arco Curvatura</span>
                      <span>
                        {Number(activeLayer.properties.curveRadius ?? 680) === 0
                          ? 'Dritto'
                          : `${activeLayer.properties.curveRadius ?? 680}px`}
                      </span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="1100"
                      step="50"
                      value={activeLayer.properties.curveRadius ?? 680}
                      oninput={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'curveRadius',
                          parseInt((e.target as HTMLInputElement).value, 10)
                        )}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>
                </div>
              </div>
            {:else if activeLayer.type === 'NUMBER'}
              <div class="space-y-3 pt-2 border-t border-[#2E3D56]">
                <div class="grid grid-cols-2 gap-3 items-center">
                  <div>
                    <label for="kit-number-input" class="block text-xs text-[#94A3B8] mb-1">
                      Numero Gara (1–99)
                    </label>
                    <input
                      id="kit-number-input"
                      type="text"
                      maxlength="2"
                      value={activeLayer.properties.numberText ?? '10'}
                      oninput={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'numberText',
                          (e.target as HTMLInputElement).value
                        )}
                      class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                    />
                  </div>

                  <label class="flex items-center gap-2 text-xs text-white cursor-pointer pt-4">
                    <input
                      type="checkbox"
                      checked={Boolean(activeLayer.properties.showFrontNumber ?? true)}
                      onchange={(e) =>
                        studio.updateLayerProperty(
                          activeLayer.id,
                          'showFrontNumber',
                          (e.target as HTMLInputElement).checked
                        )}
                      class="accent-[#00C49F]"
                    />
                    Numero anche Frontale
                  </label>
                </div>

                <div>
                  <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                    <span>Contorno Vettoriale</span>
                    <span>{activeLayer.properties.strokeWidth ?? 12}px</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="24"
                    step="1"
                    value={activeLayer.properties.strokeWidth ?? 12}
                    oninput={(e) =>
                      studio.updateLayerProperty(
                        activeLayer.id,
                        'strokeWidth',
                        parseInt((e.target as HTMLInputElement).value, 10)
                      )}
                    class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                  />
                </div>
              </div>
            {/if}
          </div>

          <!-- HSV + HEX Color Picker with Recent 8 Colors -->
          <ColorPickerHsv
            activeHex={activeLayer.color}
            secondaryHex={activeLayer.secondaryColor}
            hasSecondary={true}
            onColorChange={(hex, isSec, commit) =>
              studio.updateLayerColor(activeLayer.id, hex, isSec, commit)}
          />
        {/if}
      </div>
    </div>
    {/if}

    <!-- Right Column: 2D Texture Canvas (2048x2048) & 3D PBR Viewport (lg:col-span-8 or col-span-12) -->
    <div
      class="{studio.activeGarmentType === 'ALL' ? 'lg:col-span-12' : 'lg:col-span-8'} grid gap-4 {studio.editorViewMode === 'SPLIT'
        ? 'grid-cols-1 xl:grid-cols-2'
        : 'grid-cols-1'}"
    >
      <!-- Texture2DView / Mockup Maglia 2D (Always mounted in DOM so 2048x2048 feeds 3D CanvasTexture) -->
      <div
        class="{studio.editorViewMode === '3D'
          ? 'sr-only pointer-events-none'
          : 'flex flex-col bg-[#1A2230] border border-[#2E3D56] rounded-xl p-4 space-y-3'}"
      >
        <div class="flex items-center justify-between gap-2 flex-wrap">
          <!-- Toggle: Mockup 2D vs Foglio UV -->
          <div class="inline-flex items-center p-0.5 bg-[#0F141C] border border-[#2E3D56] rounded-lg">
            <button
              type="button"
              onclick={() => (displayMode2d = 'MOCKUP')}
              class="px-2.5 py-1 text-xs font-semibold rounded-md transition-colors cursor-pointer {displayMode2d === 'MOCKUP'
                ? 'bg-[#0F52BA] text-white shadow-sm'
                : 'text-[#94A3B8] hover:text-white'}"
            >
              Mockup 2D
            </button>
            <button
              type="button"
              onclick={() => (displayMode2d = 'UV')}
              class="px-2.5 py-1 text-xs font-semibold rounded-md transition-colors cursor-pointer {displayMode2d === 'UV'
                ? 'bg-[#0F52BA] text-white shadow-sm'
                : 'text-[#94A3B8] hover:text-white'}"
            >
              Foglio UV (2048px)
            </button>
          </div>

          <div class="flex items-center gap-2">
            {#if displayMode2d === 'UV'}
              <label class="flex items-center gap-1.5 text-xs text-[#94A3B8] hover:text-white cursor-pointer">
                <input
                  type="checkbox"
                  bind:checked={studio.showUvGuides}
                  class="accent-[#00C49F]"
                />
                Guide UV
              </label>
            {/if}

            <button
              type="button"
              onclick={() => (zoom2d = zoom2d === 1.0 ? 1.45 : 1.0)}
              class="px-2.5 py-1 text-xs font-mono-tabular bg-[#0F141C] border border-[#2E3D56] rounded-md text-[#94A3B8] hover:text-white cursor-pointer"
            >
              Zoom {Math.round(zoom2d * 100)}%
            </button>
          </div>
        </div>

        <div class="relative bg-[#0F141C] border border-[#2E3D56] rounded-lg overflow-auto flex items-center justify-center aspect-square max-h-[600px]">
          <!-- 2D Photorealistic Jersey Mockup Canvas (Default user view) -->
          <canvas
            bind:this={mockupCanvasRef}
            width="512"
            height="512"
            class="w-full h-full object-contain transition-transform duration-150 {displayMode2d === 'MOCKUP' ? 'block' : 'hidden'}"
            style="transform: scale({zoom2d}); transform-origin: center center;"
          ></canvas>

          <!-- 2048x2048 Full Texture Canvas (Active in DOM for 3D PBR and print UV unwrap inspection) -->
          <canvas
            bind:this={canvas2dRef}
            width="2048"
            height="2048"
            class="w-full h-full object-contain transition-transform duration-150 {displayMode2d === 'UV' ? 'block' : 'sr-only pointer-events-none'}"
            style="transform: scale({zoom2d}); transform-origin: center center;"
          ></canvas>
        </div>

        <div class="flex items-center justify-between text-[11px] text-[#64748B] font-mono-tabular">
          <span>{displayMode2d === 'MOCKUP' ? 'Sagome con ombreggiatura e mockup realistico' : 'Mappa Texture 2048×2048 RGBA'}</span>
          <span>{displayMode2d === 'MOCKUP' ? 'Mockup 512×512' : '2048×2048 RGBA'}</span>
        </div>
      </div>

      <!-- Viewport3D (Lazy Loaded when editorViewMode is '3D' or 'SPLIT') -->
      {#if studio.editorViewMode === '3D' || studio.editorViewMode === 'SPLIT'}
        <div class="flex flex-col bg-[#1A2230] border border-[#2E3D56] rounded-xl p-4 space-y-3 h-full min-h-[520px]">
          <div class="flex items-center justify-between text-xs text-[#94A3B8] font-mono-tabular">
            <span class="text-white font-semibold">Viewport3D (WebGL2 PBR)</span>
            <span>Rotazione Orbitale 360° · Pitch Vincolato</span>
          </div>

          <div class="flex-1 min-h-[460px]">
            <Viewport3D
              sourceCanvas={canvas2dRef}
              jerseyCanvas={jerseyCanvas}
              shortsCanvas={shortsCanvas}
              socksCanvas={socksCanvas}
              textureRevision={studio.textureRevision}
            />
          </div>
        </div>
      {/if}
    </div>

    {#if studio.activeGarmentType === 'ALL'}
      <!-- Full Kit Overview & Export CTA Card when in Vista Completo -->
      <div class="col-span-12 bg-[#1A2230] border border-[#2E3D56] rounded-xl p-5 flex flex-wrap items-center justify-between gap-4 shadow-xl">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-xl bg-[#00C49F]/10 border border-[#00C49F]/30 flex items-center justify-center text-2xl">
            👤
          </div>
          <div>
            <div class="text-sm font-bold text-white flex items-center gap-2">
              <span>Panoramica Kit Completo Ufficiale</span>
              <span class="px-2 py-0.5 text-[10px] font-semibold bg-[#00C49F]/20 text-[#00C49F] rounded-full border border-[#00C49F]/40">Pronto per Esportazione</span>
            </div>
            <div class="text-xs text-[#94A3B8] mt-0.5">
              Maglia ({studio.activeKit.garments?.jersey.layers.length ?? 0} liv.) · Pantaloncini ({studio.activeKit.garments?.shorts.layers.length ?? 0} liv.) · Calzettoni ({studio.activeKit.garments?.socks.layers.length ?? 0} liv.)
            </div>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <button
            type="button"
            onclick={() => studio.setGarmentType('JERSEY')}
            class="px-4 py-2.5 text-xs font-semibold text-[#94A3B8] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-lg transition-colors cursor-pointer"
          >
            ← Modifica Singoli Capi
          </button>
          <button
            type="button"
            onclick={() => studio.navigateTo('SCR-EXPORT')}
            class="px-5 py-2.5 text-xs font-bold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-lg shadow-md shadow-[#00C49F]/20 transition-all cursor-pointer flex items-center gap-2"
          >
            <span>Esporta Kit Completo (ZIP)</span>
            <span>→</span>
          </button>
        </div>
      </div>
    {/if}
  </div>
</section>
