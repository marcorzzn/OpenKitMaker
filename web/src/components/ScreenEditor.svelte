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
    FabricType,
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

  const allRailItems: { type: LayerType; label: string; icon: string; shortCode: string }[] = [
    { type: 'BASE', label: 'Base', icon: '👕', shortCode: 'BAS' },
    { type: 'PATTERN', label: 'Strisce', icon: '⚡', shortCode: 'STR' },
    { type: 'COLLAR', label: 'Colletto', icon: '👔', shortCode: 'COL' },
    { type: 'SLEEVES', label: 'Maniche', icon: '💪', shortCode: 'MAN' },
    { type: 'BADGE', label: 'Stemma', icon: '🛡️', shortCode: 'STM' },
    { type: 'SPONSOR', label: 'Sponsor', icon: '⭐', shortCode: 'SPO' },
    { type: 'TEXT_NAME', label: 'Nome', icon: '🔤', shortCode: 'NOM' },
    { type: 'NUMBER', label: 'Numero', icon: '🔢', shortCode: 'NUM' },
  ];

  function getLayerIcon(type: LayerType): string {
    const item = allRailItems.find((r) => r.type === type);
    return item ? item.icon : '✨';
  }

  const garmentAllowedTypes: Record<GarmentType, LayerType[]> = {
    JERSEY: ['BASE', 'PATTERN', 'COLLAR', 'SLEEVES', 'BADGE', 'SPONSOR', 'TEXT_NAME', 'NUMBER'],
    SHORTS: ['BASE', 'PATTERN', 'BADGE', 'NUMBER'],
    SOCKS: ['BASE', 'PATTERN', 'COLLAR'],
  };

  let componentRailItems = $derived(
    studio.activeGarmentType !== 'ALL'
      ? allRailItems.filter((item) => garmentAllowedTypes[studio.activeGarmentType as GarmentType].includes(item.type))
      : allRailItems
  );

  type EditorTab = 'COLOR_FABRIC' | 'PATTERNS' | 'LOGOS' | 'LAYERS';
  let activeEditorTab = $state<EditorTab>('COLOR_FABRIC');
  let activeLogoSubTab = $state<'BADGE' | 'SPONSOR' | 'NUMBER'>('BADGE');

  let patternLayer = $derived(studio.activeLayers.find((l) => l.type === 'PATTERN'));
  let baseLayer = $derived(studio.activeLayers.find((l) => l.type === 'BASE'));
  let collarLayer = $derived(studio.activeLayers.find((l) => l.type === 'COLLAR'));
  let sleevesLayer = $derived(studio.activeLayers.find((l) => l.type === 'SLEEVES'));
  let badgeLayer = $derived(studio.activeLayers.find((l) => l.type === 'BADGE'));
  let sponsorLayer = $derived(studio.activeLayers.find((l) => l.type === 'SPONSOR'));
  let nameLayer = $derived(studio.activeLayers.find((l) => l.type === 'TEXT_NAME'));
  let numberLayer = $derived(studio.activeLayers.find((l) => l.type === 'NUMBER'));

  function getOrCreateLayer(type: LayerType) {
    const existing = studio.activeLayers.find((l) => l.type === type);
    if (existing) {
      studio.selectedLayerId = existing.id;
      studio.activeLayerTypeFilter = type;
      return existing;
    }
    studio.addNewLayer(type);
    studio.activeLayerTypeFilter = type;
    return studio.selectedLayer;
  }

  const fabricPresets: {
    type: FabricType;
    label: string;
    icon: string;
    desc: string;
    finishLabel: string;
    roughness: number;
    metalness: number;
  }[] = [
    {
      type: 'MESH',
      label: 'Mesh Traspirante',
      icon: '🎽',
      desc: 'Tessuto tecnico con micro-fori traspiranti',
      finishLabel: 'Finitura Opaca',
      roughness: 0.78,
      metalness: 0.02,
    },
    {
      type: 'POLYESTER',
      label: 'Poliestere Lucido',
      icon: '✨',
      desc: 'Effetto sintetico moderno, lucido e brillante',
      finishLabel: 'Finitura Lucida',
      roughness: 0.22,
      metalness: 0.12,
    },
    {
      type: 'COTTON',
      label: 'Cotone Anni \'90',
      icon: '👕',
      desc: 'Tessuto vintage pesante e opaco',
      finishLabel: 'Finitura Vintage',
      roughness: 0.90,
      metalness: 0.01,
    },
    {
      type: 'JACQUARD',
      label: 'Jacquard Nobile',
      icon: '🏛️',
      desc: 'Trama damascata con rilievo geometrico 3D',
      finishLabel: 'Finitura Rilievo 3D',
      roughness: 0.48,
      metalness: 0.06,
    },
  ];

  const blendModes: { value: BlendMode; label: string; desc: string }[] = [
    { value: 'NORMAL', label: 'Normale', desc: 'Copertura standard' },
    { value: 'MULTIPLY', label: 'Moltiplica (Multiply)', desc: 'Scurisce e fonde con le ombre del tessuto' },
    { value: 'OVERLAY', label: 'Sovrapponi (Overlay)', desc: 'Combina moltiplicazione e contrasto' },
    { value: 'SCREEN', label: 'Scherma (Screen)', desc: 'Schiarisce e illumina la trama' },
  ];

  const patternStyles: { value: PatternStyle; label: string; isProcedural?: boolean }[] = [
    { value: 'HEXAGONS', label: '⬡ Esagoni Procedurali', isProcedural: true },
    { value: 'WAVES', label: '〰 Onde Sinusoidali', isProcedural: true },
    { value: 'CAMOUFLAGE', label: '🫧 Mimetico Organico', isProcedural: true },
    { value: 'ABSTRACT_GEOMETRIC', label: '📐 Geometria Astratta', isProcedural: true },
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

  // Magnetic Snapping System State
  let isDraggingLayer = $state(false);
  let dragStartX = $state(0);
  let dragStartY = $state(0);
  let initialOffsetX = $state(0);
  let initialOffsetY = $state(0);
  let activeSnapGuide = $state<string | null>(null);
  let snapTargetType = $state<'CHEST_CENTER' | 'COLLAR_HEIGHT' | 'CREST_LEFT' | 'CREST_RIGHT' | 'SPONSOR_LINE' | null>(null);

  const SNAP_THRESHOLD_PX = 24;

  function snapPosition(rawOffsetX: number, rawOffsetY: number): { x: number; y: number; guide: string | null; target: typeof snapTargetType } {
    let x = rawOffsetX;
    let y = rawOffsetY;
    let guide: string | null = null;
    let target: typeof snapTargetType = null;

    // Horizontal snapping anchors
    if (Math.abs(rawOffsetX) <= SNAP_THRESHOLD_PX) {
      x = 0;
      guide = 'Centro del Petto (X=0)';
      target = 'CHEST_CENTER';
    } else if (Math.abs(rawOffsetX - 163) <= SNAP_THRESHOLD_PX) {
      x = 163;
      guide = 'Allineamento Cuore SX';
      target = 'CREST_LEFT';
    } else if (Math.abs(rawOffsetX + 163) <= SNAP_THRESHOLD_PX) {
      x = -163;
      guide = 'Allineamento Petto DX';
      target = 'CREST_RIGHT';
    }

    // Vertical snapping anchors
    if (Math.abs(rawOffsetY - (-140)) <= SNAP_THRESHOLD_PX) {
      y = -140;
      guide = guide ? `${guide} + Altezza Colletto` : 'Altezza Colletto';
      target = target || 'COLLAR_HEIGHT';
    } else if (Math.abs(rawOffsetY - (-60)) <= SNAP_THRESHOLD_PX) {
      y = -60;
      guide = guide ? `${guide} + Linea Cuore` : 'Linea Cuore / Stemma';
    } else if (Math.abs(rawOffsetY - 20) <= SNAP_THRESHOLD_PX) {
      y = 20;
      guide = guide ? `${guide} + Sponsor Principale` : 'Sponsor Principale';
      target = target || 'SPONSOR_LINE';
    }

    return { x, y, guide, target };
  }

  function handlePointerDown(e: PointerEvent) {
    const layer = studio.selectedLayer;
    if (!layer || (layer.type !== 'SPONSOR' && layer.type !== 'BADGE')) return;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    isDraggingLayer = true;
    dragStartX = e.clientX;
    dragStartY = e.clientY;
    initialOffsetX = Number(layer.properties.offsetX ?? 0);
    initialOffsetY = Number(layer.properties.offsetY ?? (layer.type === 'SPONSOR' ? 20 : 0));
    activeSnapGuide = null;
  }

  function handlePointerMove(e: PointerEvent) {
    if (!isDraggingLayer) return;
    const layer = studio.selectedLayer;
    if (!layer) return;

    const scaleFactor = 1.8;
    const deltaX = (e.clientX - dragStartX) * scaleFactor;
    const deltaY = (e.clientY - dragStartY) * scaleFactor;

    const rawX = initialOffsetX + deltaX;
    const rawY = initialOffsetY + deltaY;

    const snapped = snapPosition(rawX, rawY);
    activeSnapGuide = snapped.guide;
    snapTargetType = snapped.target;

    studio.updateLayerProperty(layer.id, 'offsetX', Math.round(snapped.x));
    studio.updateLayerProperty(layer.id, 'offsetY', Math.round(snapped.y));
    studio.updateLayerProperty(layer.id, 'snappedToGuide', snapped.guide !== null);
  }

  function handlePointerUp(e: PointerEvent) {
    if (!isDraggingLayer) return;
    isDraggingLayer = false;
    setTimeout(() => {
      activeSnapGuide = null;
      snapTargetType = null;
    }, 1400);
  }

  function applyQuickSnap(target: 'CHEST_CENTER' | 'COLLAR' | 'CREST_LEFT' | 'CREST_RIGHT' | 'SPONSOR' | 'RESET') {
    const layer = studio.selectedLayer;
    if (!layer) return;

    switch (target) {
      case 'CHEST_CENTER':
        studio.updateLayerProperty(layer.id, 'offsetX', 0);
        activeSnapGuide = 'Centro del Petto (X=0)';
        snapTargetType = 'CHEST_CENTER';
        break;
      case 'COLLAR':
        studio.updateLayerProperty(layer.id, 'offsetY', -140);
        activeSnapGuide = 'Altezza Colletto (Y=-140)';
        snapTargetType = 'COLLAR_HEIGHT';
        break;
      case 'CREST_LEFT':
        studio.updateLayerProperty(layer.id, 'offsetX', 163);
        studio.updateLayerProperty(layer.id, 'offsetY', -60);
        activeSnapGuide = 'Allineamento Cuore SX';
        snapTargetType = 'CREST_LEFT';
        break;
      case 'CREST_RIGHT':
        studio.updateLayerProperty(layer.id, 'offsetX', -163);
        studio.updateLayerProperty(layer.id, 'offsetY', -60);
        activeSnapGuide = 'Allineamento Petto DX';
        snapTargetType = 'CREST_RIGHT';
        break;
      case 'SPONSOR':
        studio.updateLayerProperty(layer.id, 'offsetX', 0);
        studio.updateLayerProperty(layer.id, 'offsetY', 20);
        activeSnapGuide = 'Sponsor Principale Centrato';
        snapTargetType = 'SPONSOR_LINE';
        break;
      case 'RESET':
        studio.updateLayerProperty(layer.id, 'offsetX', 0);
        studio.updateLayerProperty(layer.id, 'offsetY', layer.type === 'SPONSOR' ? 20 : 0);
        activeSnapGuide = 'Posizione Reimpostata';
        snapTargetType = null;
        break;
    }
    setTimeout(() => {
      activeSnapGuide = null;
      snapTargetType = null;
    }, 1500);
  }

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

      <span class="hidden xl:inline-flex items-center gap-1.5 px-2.5 py-1 text-xs text-[#00C49F] bg-[#00C49F]/10 border border-[#00C49F]/20 rounded-full font-medium whitespace-nowrap">
        <span class="w-1.5 h-1.5 rounded-full bg-[#00C49F] animate-pulse"></span>
        Tessuto: {fabricPresets.find(f => f.type === studio.fabricType)?.label ?? 'Mesh'} · 3D Live
      </span>
    </div>

    <!-- View Mode Selector (Vista 2D | Vista 3D | Vista Doppia) -->
    <div class="inline-flex items-center gap-1 p-1 bg-[#0F141C] border border-[#2E3D56] rounded-xl">
      <button
        type="button"
        onclick={() => (studio.editorViewMode = '2D')}
        class="h-8 px-3 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.editorViewMode === '2D'
          ? 'bg-[#0F52BA] text-white shadow-sm'
          : 'text-[#94A3B8] hover:text-white'}"
      >
        {i18n.editor.view_2d}
      </button>
      <button
        type="button"
        onclick={() => (studio.editorViewMode = '3D')}
        class="h-8 px-3 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.editorViewMode === '3D'
          ? 'bg-[#0F52BA] text-white shadow-sm'
          : 'text-[#94A3B8] hover:text-white'}"
      >
        {i18n.editor.view_3d}
      </button>
      <button
        type="button"
        onclick={() => (studio.editorViewMode = 'SPLIT')}
        class="h-8 px-3 text-xs font-semibold rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer {studio.editorViewMode === 'SPLIT'
          ? 'bg-[#0F52BA] text-white shadow-sm'
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
        class="h-10 px-4 text-xs font-semibold text-white bg-[#253146] hover:bg-[#2E3D56] border border-[#2E3D56] rounded-xl transition-all whitespace-nowrap shrink-0 cursor-pointer flex items-center gap-1.5"
      >
        <span>💾</span>
        <span>{i18n.editor.save_draft}</span>
      </button>
      <button
        type="button"
        onclick={() => studio.navigateTo('SCR-EXPORT')}
        class="h-10 px-5 text-xs font-bold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-xl transition-all whitespace-nowrap shrink-0 cursor-pointer shadow-md shadow-[#00C49F]/20 flex items-center gap-1.5"
      >
        <span>📤</span>
        <span>{i18n.editor.export_btn} →</span>
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
        class="h-10 px-4 text-xs font-bold rounded-xl transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'JERSEY'
          ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-2 ring-[#38BDF8]'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">👕</span>
        <span>Maglia</span>
      </button>

      <button
        type="button"
        onclick={() => studio.setGarmentType('SHORTS')}
        class="h-10 px-4 text-xs font-bold rounded-xl transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'SHORTS'
          ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-2 ring-[#38BDF8]'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">🩳</span>
        <span>Pantaloncini</span>
      </button>

      <button
        type="button"
        onclick={() => studio.setGarmentType('SOCKS')}
        class="h-10 px-4 text-xs font-bold rounded-xl transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'SOCKS'
          ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-2 ring-[#38BDF8]'
          : 'bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#253146]'}"
      >
        <span class="text-base">🧦</span>
        <span>Calzettoni</span>
      </button>

      <button
        type="button"
        onclick={() => studio.setGarmentType('ALL')}
        class="h-10 px-4 text-xs font-bold rounded-xl transition-all flex items-center gap-2 cursor-pointer {studio.activeGarmentType === 'ALL'
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
        class="h-10 px-4 text-xs font-semibold rounded-xl border transition-all flex items-center gap-2 cursor-pointer {studio.showMannequin
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
      <!-- Left Column: 4-Tab Sports Design Station (lg:col-span-4) -->
      <div class="lg:col-span-4 bg-[#1A2230] border border-[#2E3D56] rounded-xl overflow-hidden flex flex-col shadow-xl">
        <!-- 4 Primary Functional Navigation Tabs -->
        <div class="grid grid-cols-4 bg-[#0F141C] border-b border-[#2E3D56] p-1.5 gap-1.5">
          <button
            type="button"
            onclick={() => {
              activeEditorTab = 'COLOR_FABRIC';
              studio.selectLayerByType('BASE');
            }}
            class="h-11 px-1 rounded-xl text-center transition-all cursor-pointer flex flex-col items-center justify-center gap-0.5 {activeEditorTab === 'COLOR_FABRIC'
              ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-1 ring-white/30 font-bold'
              : 'text-[#94A3B8] hover:text-white hover:bg-[#1A2230]'}"
          >
            <span class="text-base">🎨</span>
            <span class="text-[11px] leading-tight font-semibold">Colori</span>
          </button>

          <button
            type="button"
            onclick={() => {
              activeEditorTab = 'PATTERNS';
              const pat = studio.activeLayers.find((l) => l.type === 'PATTERN');
              if (pat) {
                studio.selectedLayerId = pat.id;
              } else {
                studio.addNewLayer('PATTERN');
              }
            }}
            class="h-11 px-1 rounded-xl text-center transition-all cursor-pointer flex flex-col items-center justify-center gap-0.5 {activeEditorTab === 'PATTERNS'
              ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-1 ring-white/30 font-bold'
              : 'text-[#94A3B8] hover:text-white hover:bg-[#1A2230]'}"
          >
            <span class="text-base">⚡</span>
            <span class="text-[11px] leading-tight font-semibold">Pattern</span>
          </button>

          <button
            type="button"
            onclick={() => {
              activeEditorTab = 'LOGOS';
              if (activeLogoSubTab === 'BADGE') getOrCreateLayer('BADGE');
              else if (activeLogoSubTab === 'SPONSOR') getOrCreateLayer('SPONSOR');
              else if (activeLogoSubTab === 'NUMBER') getOrCreateLayer('NUMBER');
            }}
            class="h-11 px-1 rounded-xl text-center transition-all cursor-pointer flex flex-col items-center justify-center gap-0.5 {activeEditorTab === 'LOGOS'
              ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-1 ring-white/30 font-bold'
              : 'text-[#94A3B8] hover:text-white hover:bg-[#1A2230]'}"
          >
            <span class="text-base">🛡️</span>
            <span class="text-[11px] leading-tight font-semibold">Stemmi</span>
          </button>

          <button
            type="button"
            onclick={() => (activeEditorTab = 'LAYERS')}
            class="h-11 px-1 rounded-xl text-center transition-all cursor-pointer flex flex-col items-center justify-center gap-0.5 {activeEditorTab === 'LAYERS'
              ? 'bg-[#0F52BA] text-white shadow-md shadow-[#0F52BA]/30 ring-1 ring-white/30 font-bold'
              : 'text-[#94A3B8] hover:text-white hover:bg-[#1A2230]'}"
          >
            <span class="text-base">📑</span>
            <span class="text-[11px] leading-tight font-semibold">Livelli ({studio.activeLayers.length})</span>
          </button>
        </div>

        <!-- TAB 1: COLORI & TESSUTO -->
        {#if activeEditorTab === 'COLOR_FABRIC'}
          <div class="p-4 space-y-4 max-h-[calc(100vh-210px)] overflow-y-auto">
            <!-- Garment Piece Selector -->
            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold uppercase tracking-wider text-[#94A3B8]">Zona da Colorare:</span>
                <span class="text-[11px] font-bold text-[#00C49F] px-2 py-0.5 rounded bg-[#0F141C] border border-[#2E3D56] flex items-center gap-1">
                  <span>Modificando:</span>
                  <span>{studio.selectedLayer?.name || 'Corpo Maglia'}</span>
                </span>
              </div>
              <div class="grid grid-cols-3 gap-1.5">
                {#if studio.activeGarmentType === 'JERSEY'}
                  <button
                    type="button"
                    id="part-btn-body"
                    onclick={() => {
                      const l = getOrCreateLayer('BASE');
                      if (l) studio.selectedLayerId = l.id;
                    }}
                    class="h-10 px-2.5 text-xs font-bold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 {studio.selectedLayer?.type === 'BASE'
                      ? 'bg-[#0F52BA] text-white border-[#38BDF8] shadow-sm'
                      : 'bg-[#0F141C] text-[#94A3B8] border-[#2E3D56] hover:text-white'}"
                  >
                    <span>👕</span>
                    <span>Corpo</span>
                  </button>
                  <button
                    type="button"
                    id="part-btn-collar"
                    onclick={() => {
                      const l = getOrCreateLayer('COLLAR');
                      if (l) studio.selectedLayerId = l.id;
                    }}
                    class="h-10 px-2.5 text-xs font-bold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 {studio.selectedLayer?.type === 'COLLAR'
                      ? 'bg-[#0F52BA] text-white border-[#38BDF8] shadow-sm'
                      : 'bg-[#0F141C] text-[#94A3B8] border-[#2E3D56] hover:text-white'}"
                  >
                    <span>👔</span>
                    <span>Colletto</span>
                  </button>
                  <button
                    type="button"
                    id="part-btn-sleeves"
                    onclick={() => {
                      const l = getOrCreateLayer('SLEEVES');
                      if (l) studio.selectedLayerId = l.id;
                    }}
                    class="h-10 px-2.5 text-xs font-bold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 {studio.selectedLayer?.type === 'SLEEVES'
                      ? 'bg-[#0F52BA] text-white border-[#38BDF8] shadow-sm'
                      : 'bg-[#0F141C] text-[#94A3B8] border-[#2E3D56] hover:text-white'}"
                  >
                    <span>💪</span>
                    <span>Maniche</span>
                  </button>
                {:else if studio.activeGarmentType === 'SHORTS'}
                  <button
                    type="button"
                    onclick={() => {
                      const l = getOrCreateLayer('BASE');
                      if (l) studio.selectedLayerId = l.id;
                    }}
                    class="col-span-3 h-10 px-2.5 text-xs font-bold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 {studio.selectedLayer?.type === 'BASE'
                      ? 'bg-[#0F52BA] text-white border-[#38BDF8] shadow-sm'
                      : 'bg-[#0F141C] text-[#94A3B8] border-[#2E3D56] hover:text-white'}"
                  >
                    <span>🩳</span>
                    <span>Tessuto Pantaloncini</span>
                  </button>
                {:else}
                  <button
                    type="button"
                    onclick={() => {
                      const l = getOrCreateLayer('BASE');
                      if (l) studio.selectedLayerId = l.id;
                    }}
                    class="h-10 px-2.5 text-xs font-bold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 {studio.selectedLayer?.type === 'BASE'
                      ? 'bg-[#0F52BA] text-white border-[#38BDF8] shadow-sm'
                      : 'bg-[#0F141C] text-[#94A3B8] border-[#2E3D56] hover:text-white'}"
                  >
                    <span>🧦</span>
                    <span>Calza</span>
                  </button>
                  <button
                    type="button"
                    onclick={() => {
                      const l = getOrCreateLayer('COLLAR');
                      if (l) studio.selectedLayerId = l.id;
                    }}
                    class="col-span-2 h-10 px-2.5 text-xs font-bold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 {studio.selectedLayer?.type === 'COLLAR'
                      ? 'bg-[#0F52BA] text-white border-[#38BDF8] shadow-sm'
                      : 'bg-[#0F141C] text-[#94A3B8] border-[#2E3D56] hover:text-white'}"
                  >
                    <span>👔</span>
                    <span>Bordo Elastico</span>
                  </button>
                {/if}
              </div>
            </div>

            <!-- Contextual Collar / Sleeves options if selected -->
            {#if studio.selectedLayer?.type === 'COLLAR' || (collarLayer && studio.selectedLayerId === collarLayer.id)}
              {@const activeCollar = studio.selectedLayer?.type === 'COLLAR' ? studio.selectedLayer : collarLayer}
              {#if activeCollar}
                <div class="grid grid-cols-2 gap-3 p-3 bg-[#0F141C] border border-[#2E3D56] rounded-xl">
                  <div>
                    <label for="collar-style-tab" class="block text-xs text-[#94A3B8] mb-1">Taglio Colletto</label>
                    <select
                      id="collar-style-tab"
                      value={activeCollar.properties.collarStyle || 'V_NECK'}
                      onchange={(e) => studio.updateLayerProperty(activeCollar.id, 'collarStyle', (e.target as HTMLSelectElement).value)}
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
                      <span>{activeCollar.properties.trimWidth ?? 16}px</span>
                    </div>
                    <input
                      type="range"
                      min="8"
                      max="36"
                      step="1"
                      value={activeCollar.properties.trimWidth ?? 16}
                      oninput={(e) => studio.updateLayerProperty(activeCollar.id, 'trimWidth', parseInt((e.target as HTMLInputElement).value, 10))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>
                </div>
              {/if}
            {:else if studio.selectedLayer?.type === 'SLEEVES'}
              {@const activeLayer = studio.selectedLayer}
              <div class="grid grid-cols-2 gap-3 p-3 bg-[#0F141C] border border-[#2E3D56] rounded-xl items-center">
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
                    oninput={(e) => studio.updateLayerProperty(activeLayer.id, 'cuffWidth', parseInt((e.target as HTMLInputElement).value, 10))}
                    class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                  />
                </div>
                <label class="flex items-center gap-2 text-xs text-white cursor-pointer pt-3">
                  <input
                    type="checkbox"
                    checked={Boolean(activeLayer.properties.raglanCut ?? true)}
                    onchange={(e) => studio.updateLayerProperty(activeLayer.id, 'raglanCut', (e.target as HTMLInputElement).checked)}
                    class="accent-[#00C49F]"
                  />
                  Taglio Spalla Raglan
                </label>
              </div>
            {/if}

            <!-- Color Picker for the active part -->
            {#if studio.selectedLayer}
              {@const currentL = studio.selectedLayer}
              <div class="space-y-1.5">
                <span class="text-xs font-bold uppercase tracking-wider text-[#94A3B8]">
                  Tavolozza Colori — {currentL.name}:
                </span>
                <ColorPickerHsv
                  activeHex={currentL.color}
                  secondaryHex={currentL.secondaryColor}
                  hasSecondary={true}
                  onColorChange={(hex, isSec, commit) =>
                    studio.updateLayerColor(currentL.id, hex, isSec, commit)}
                />
              </div>
            {/if}

            <!-- Fabric Presets Card -->
            <div class="p-3.5 rounded-xl border border-[#2E3D56] bg-[#0F141C] space-y-2.5">
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold text-white flex items-center gap-1.5">
                  <span>🎽</span> Tessuto & Materiale PBR
                </span>
                <span class="text-[10px] font-mono-tabular px-2 py-0.5 rounded-full bg-[#1A2230] text-[#00C49F] border border-[#2E3D56]">
                  {fabricPresets.find(f => f.type === studio.fabricType)?.label ?? 'Mesh'}
                </span>
              </div>

              <div class="grid grid-cols-2 gap-2">
                {#each fabricPresets as preset (preset.type)}
                  {@const isSelected = studio.fabricType === preset.type}
                  <button
                    type="button"
                    onclick={() => studio.setFabricType(preset.type)}
                    class="p-2.5 rounded-lg border text-left transition-all cursor-pointer flex flex-col gap-1 {isSelected
                      ? 'bg-[#253146] border-[#00C49F] shadow-md ring-1 ring-[#00C49F]/40'
                      : 'bg-[#1A2230] border-[#2E3D56] hover:border-[#64748B]'}"
                  >
                    <div class="flex items-center justify-between">
                      <span class="text-sm">{preset.icon}</span>
                      <span class="text-[10px] font-bold text-[#00C49F]">
                        {preset.finishLabel}
                      </span>
                    </div>
                    <span class="text-xs font-semibold {isSelected ? 'text-[#00C49F]' : 'text-white'}">
                      {preset.label}
                    </span>
                    <span class="text-[10px] text-[#94A3B8] line-clamp-2">
                      {preset.desc}
                    </span>
                  </button>
                {/each}
              </div>
            </div>
          </div>
        {/if}

        <!-- TAB 2: TRAMA & PATTERN -->
        {#if activeEditorTab === 'PATTERNS'}
          <div class="p-4 space-y-4 max-h-[calc(100vh-210px)] overflow-y-auto">
            {#if !patternLayer}
              <div class="p-6 bg-[#0F141C] border border-[#2E3D56] rounded-xl text-center space-y-3">
                <div class="text-3xl">⚡</div>
                <div class="text-sm font-bold text-white">Nessuna Trama Grafica Attiva</div>
                <p class="text-xs text-[#94A3B8]">
                  Aggiungi un pattern geometrico procedurale o le classiche strisce per personalizzare questo capo.
                </p>
                <button
                  type="button"
                  onclick={() => studio.addNewLayer('PATTERN')}
                  class="px-4 py-2 text-xs font-bold text-[#0F141C] bg-[#00C49F] hover:bg-[#10B981] rounded-lg transition-colors cursor-pointer"
                >
                  + Attiva Grafica Pattern
                </button>
              </div>
            {:else}
              {@const patLayer = patternLayer}
              <!-- Generatore Procedurale Quick Cards -->
              <div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-3.5 space-y-3">
                <div class="flex items-center justify-between">
                  <span class="text-xs font-bold text-white flex items-center gap-1.5">
                    <span>⚡📐</span>
                    <span>Pattern Procedurali 3D</span>
                  </span>
                  <span class="text-[10px] px-2 py-0.5 rounded-full bg-[#00C49F]/20 text-[#00C49F] font-mono-tabular border border-[#00C49F]/30">4 Algoritmi</span>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onclick={() => {
                      studio.selectedLayerId = patLayer.id;
                      studio.updateLayerProperty(patLayer.id, 'patternStyle', 'HEXAGONS');
                    }}
                    class="p-2.5 rounded-lg border text-left transition-all cursor-pointer {patLayer.properties.patternStyle === 'HEXAGONS'
                      ? 'bg-[#0F52BA] border-[#38BDF8] shadow-md shadow-[#0F52BA]/30 text-white'
                      : 'bg-[#1A2230] border-[#2E3D56] text-[#94A3B8] hover:text-white hover:border-[#64748B]'}"
                  >
                    <div class="text-base font-bold">⬡</div>
                    <div class="text-xs font-semibold">Esagoni</div>
                    <div class="text-[10px] opacity-75">Griglia Matematica</div>
                  </button>

                  <button
                    type="button"
                    onclick={() => {
                      studio.selectedLayerId = patLayer.id;
                      studio.updateLayerProperty(patLayer.id, 'patternStyle', 'WAVES');
                    }}
                    class="p-2.5 rounded-lg border text-left transition-all cursor-pointer {patLayer.properties.patternStyle === 'WAVES'
                      ? 'bg-[#0F52BA] border-[#38BDF8] shadow-md shadow-[#0F52BA]/30 text-white'
                      : 'bg-[#1A2230] border-[#2E3D56] text-[#94A3B8] hover:text-white hover:border-[#64748B]'}"
                  >
                    <div class="text-base font-bold">〰</div>
                    <div class="text-xs font-semibold">Onde</div>
                    <div class="text-[10px] opacity-75">Sinusoidale Fluido</div>
                  </button>

                  <button
                    type="button"
                    onclick={() => {
                      studio.selectedLayerId = patLayer.id;
                      studio.updateLayerProperty(patLayer.id, 'patternStyle', 'CAMOUFLAGE');
                    }}
                    class="p-2.5 rounded-lg border text-left transition-all cursor-pointer {patLayer.properties.patternStyle === 'CAMOUFLAGE'
                      ? 'bg-[#0F52BA] border-[#38BDF8] shadow-md shadow-[#0F52BA]/30 text-white'
                      : 'bg-[#1A2230] border-[#2E3D56] text-[#94A3B8] hover:text-white hover:border-[#64748B]'}"
                  >
                    <div class="text-base font-bold">🫧</div>
                    <div class="text-xs font-semibold">Mimetico</div>
                    <div class="text-[10px] opacity-75">Blob Organici (Noise)</div>
                  </button>

                  <button
                    type="button"
                    onclick={() => {
                      studio.selectedLayerId = patLayer.id;
                      studio.updateLayerProperty(patLayer.id, 'patternStyle', 'ABSTRACT_GEOMETRIC');
                    }}
                    class="p-2.5 rounded-lg border text-left transition-all cursor-pointer {patLayer.properties.patternStyle === 'ABSTRACT_GEOMETRIC'
                      ? 'bg-[#0F52BA] border-[#38BDF8] shadow-md shadow-[#0F52BA]/30 text-white'
                      : 'bg-[#1A2230] border-[#2E3D56] text-[#94A3B8] hover:text-white hover:border-[#64748B]'}"
                  >
                    <div class="text-base font-bold">📐</div>
                    <div class="text-xs font-semibold">Geometria</div>
                    <div class="text-[10px] opacity-75">Linee e Triangoli</div>
                  </button>
                </div>

                <div>
                  <label for="pattern-style-tab" class="block text-xs text-[#94A3B8] mb-1">Tutti i Modelli di Pattern</label>
                  <select
                    id="pattern-style-tab"
                    value={patLayer.properties.patternStyle || 'VERTICAL_STRIPES'}
                    onchange={(e) => {
                      studio.selectedLayerId = patLayer.id;
                      studio.updateLayerProperty(patLayer.id, 'patternStyle', (e.target as HTMLSelectElement).value);
                    }}
                    class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                  >
                    {#each patternStyles as ps (ps.value)}
                      <option value={ps.value}>{ps.label}</option>
                    {/each}
                  </select>
                </div>
              </div>

              <!-- Pattern Geometry Sliders -->
              <div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-3.5 space-y-3">
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Scala</span>
                      <span>{(Number(patLayer.properties.patternScale ?? 1.0)).toFixed(1)}x</span>
                    </div>
                    <input
                      type="range"
                      min="0.3"
                      max="3.0"
                      step="0.1"
                      value={patLayer.properties.patternScale ?? 1.0}
                      oninput={(e) => studio.updateLayerProperty(patLayer.id, 'patternScale', parseFloat((e.target as HTMLInputElement).value))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>

                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Rotazione</span>
                      <span>{patLayer.properties.patternRotation ?? 0}°</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="360"
                      step="5"
                      value={patLayer.properties.patternRotation ?? 0}
                      oninput={(e) => studio.updateLayerProperty(patLayer.id, 'patternRotation', parseInt((e.target as HTMLInputElement).value, 10))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Frequenza / Righe</span>
                      <span>{patLayer.properties.stripeCount ?? 5}</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="16"
                      step="1"
                      value={patLayer.properties.stripeCount ?? 5}
                      oninput={(e) => studio.updateLayerProperty(patLayer.id, 'stripeCount', parseInt((e.target as HTMLInputElement).value, 10))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>

                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Spessore / Densitá</span>
                      <span>{Math.round(Number(patLayer.properties.stripeWidth ?? 0.48) * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.10"
                      max="0.90"
                      step="0.02"
                      value={patLayer.properties.stripeWidth ?? 0.48}
                      oninput={(e) => studio.updateLayerProperty(patLayer.id, 'stripeWidth', parseFloat((e.target as HTMLInputElement).value))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>
                </div>

                <!-- Blend Mode & Opacity -->
                <div class="grid grid-cols-2 gap-3 pt-2 border-t border-[#2E3D56]">
                  <div>
                    <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                      <span>Opacità</span>
                      <span>{patLayer.opacity.toFixed(2)}</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="1"
                      step="0.01"
                      value={patLayer.opacity}
                      oninput={(e) => studio.setLayerOpacity(patLayer.id, parseFloat((e.target as HTMLInputElement).value))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>

                  <div>
                    <label for="pattern-blend-tab" class="block text-xs text-[#94A3B8] mb-1">Fusione (Blend)</label>
                    <select
                      id="pattern-blend-tab"
                      value={patLayer.blendMode}
                      onchange={(e) => studio.setLayerBlendMode(patLayer.id, (e.target as HTMLSelectElement).value as BlendMode)}
                      class="w-full px-2.5 py-1.5 text-xs font-semibold bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                    >
                      {#each blendModes as bm (bm.value)}
                        <option value={bm.value}>{bm.label}</option>
                      {/each}
                    </select>
                  </div>
                </div>
              </div>

              <!-- Color Picker for Pattern -->
              <div class="space-y-1.5">
                <span class="text-xs font-bold uppercase tracking-wider text-[#94A3B8]">
                  Colori del Pattern:
                </span>
                <ColorPickerHsv
                  activeHex={patLayer.color}
                  secondaryHex={patLayer.secondaryColor}
                  hasSecondary={true}
                  onColorChange={(hex, isSec, commit) =>
                    studio.updateLayerColor(patLayer.id, hex, isSec, commit)}
                />
              </div>
            {/if}
          </div>
        {/if}

        <!-- TAB 3: STEMMI & SPONSOR -->
        {#if activeEditorTab === 'LOGOS'}
          <div class="p-4 space-y-4 max-h-[calc(100vh-210px)] overflow-y-auto">
            <!-- Sub-navigation for Logos -->
            <div class="grid grid-cols-3 gap-1.5 p-1 bg-[#0F141C] border border-[#2E3D56] rounded-lg">
              <button
                type="button"
                onclick={() => {
                  activeLogoSubTab = 'BADGE';
                  getOrCreateLayer('BADGE');
                }}
                class="py-2 text-xs font-bold rounded-md transition-all cursor-pointer flex items-center justify-center gap-1.5 {activeLogoSubTab === 'BADGE'
                  ? 'bg-[#0F52BA] text-white shadow-sm'
                  : 'text-[#94A3B8] hover:text-white'}"
              >
                <span>🛡️</span>
                <span>Stemma</span>
              </button>

              <button
                type="button"
                onclick={() => {
                  activeLogoSubTab = 'SPONSOR';
                  getOrCreateLayer('SPONSOR');
                }}
                class="py-2 text-xs font-bold rounded-md transition-all cursor-pointer flex items-center justify-center gap-1.5 {activeLogoSubTab === 'SPONSOR'
                  ? 'bg-[#0F52BA] text-white shadow-sm'
                  : 'text-[#94A3B8] hover:text-white'}"
              >
                <span>⭐</span>
                <span>Sponsor</span>
              </button>

              <button
                type="button"
                onclick={() => {
                  activeLogoSubTab = 'NUMBER';
                  getOrCreateLayer('NUMBER');
                }}
                class="py-2 text-xs font-bold rounded-md transition-all cursor-pointer flex items-center justify-center gap-1.5 {activeLogoSubTab === 'NUMBER'
                  ? 'bg-[#0F52BA] text-white shadow-sm'
                  : 'text-[#94A3B8] hover:text-white'}"
              >
                <span>🔢</span>
                <span>Nome & N°</span>
              </button>
            </div>

            <!-- Active Logo Inspector Content -->
            {#if activeLogoSubTab === 'BADGE'}
              {@const activeLayer = badgeLayer || getOrCreateLayer('BADGE')}
              {#if activeLayer}
                <div class="space-y-3">
                  <div class="grid grid-cols-2 gap-3 p-3 bg-[#0F141C] border border-[#2E3D56] rounded-xl">
                    <div>
                      <label for="badge-shape-tab" class="block text-xs text-[#94A3B8] mb-1">Sagoma Stemma</label>
                      <select
                        id="badge-shape-tab"
                        value={activeLayer.properties.badgeShape || 'HEXAGON_PRO'}
                        onchange={(e) => studio.updateLayerProperty(activeLayer.id, 'badgeShape', (e.target as HTMLSelectElement).value)}
                        class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                      >
                        {#each badgeShapes as bs (bs.value)}
                          <option value={bs.value}>{bs.label}</option>
                        {/each}
                      </select>
                    </div>
                    <div>
                      <label for="badge-pos-tab" class="block text-xs text-[#94A3B8] mb-1">Posizione Petto</label>
                      <select
                        id="badge-pos-tab"
                        value={activeLayer.properties.badgePosition || 'LEFT_CHEST'}
                        onchange={(e) => studio.updateLayerProperty(activeLayer.id, 'badgePosition', (e.target as HTMLSelectElement).value)}
                        class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                      >
                        {#each badgePositions as bp (bp.value)}
                          <option value={bp.value}>{bp.label}</option>
                        {/each}
                      </select>
                    </div>
                  </div>

                  <!-- Snapping Pad -->
                  <div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-3 space-y-2">
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-bold text-white flex items-center gap-1.5">
                        <span>🧲</span>
                        <span>Allineamento Magnetico 1-Click</span>
                      </span>
                      <span class="text-[10px] px-2 py-0.5 rounded-full bg-[#38BDF8]/20 text-[#38BDF8] font-mono-tabular border border-[#38BDF8]/30">Guida Attiva</span>
                    </div>

                    <div class="grid grid-cols-3 gap-1.5">
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('CREST_LEFT')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#00C49F] border border-[#2E3D56] cursor-pointer"
                      >
                        🛡️ Cuore SX
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('CHEST_CENTER')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#38BDF8] border border-[#2E3D56] cursor-pointer"
                      >
                        🎯 Centro
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('CREST_RIGHT')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#94A3B8] border border-[#2E3D56] cursor-pointer"
                      >
                        🛡️ Petto DX
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('COLLAR')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#38BDF8] border border-[#2E3D56] cursor-pointer"
                      >
                        👔 Colletto
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('RESET')}
                        class="col-span-2 px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#EF4444] border border-[#2E3D56] cursor-pointer"
                      >
                        ↺ Reset (0, 0)
                      </button>
                    </div>

                    <div class="grid grid-cols-2 gap-3 pt-1">
                      <div>
                        <div class="flex justify-between text-[11px] text-[#94A3B8] font-mono-tabular mb-1">
                          <span>Offset X</span>
                          <span>{activeLayer.properties.offsetX ?? 0}px</span>
                        </div>
                        <input
                          type="range"
                          min="-200"
                          max="200"
                          step="2"
                          value={activeLayer.properties.offsetX ?? 0}
                          oninput={(e) => studio.updateLayerProperty(activeLayer.id, 'offsetX', parseInt((e.target as HTMLInputElement).value, 10))}
                          class="w-full h-1.5 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#38BDF8]"
                        />
                      </div>
                      <div>
                        <div class="flex justify-between text-[11px] text-[#94A3B8] font-mono-tabular mb-1">
                          <span>Offset Y</span>
                          <span>{activeLayer.properties.offsetY ?? 0}px</span>
                        </div>
                        <input
                          type="range"
                          min="-200"
                          max="200"
                          step="2"
                          value={activeLayer.properties.offsetY ?? 0}
                          oninput={(e) => studio.updateLayerProperty(activeLayer.id, 'offsetY', parseInt((e.target as HTMLInputElement).value, 10))}
                          class="w-full h-1.5 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#38BDF8]"
                        />
                      </div>
                    </div>
                  </div>

                  <!-- Color Picker for Badge -->
                  <div class="space-y-1.5">
                    <span class="text-xs font-bold uppercase tracking-wider text-[#94A3B8]">Colori Stemma:</span>
                    <ColorPickerHsv
                      activeHex={activeLayer.color}
                      secondaryHex={activeLayer.secondaryColor}
                      hasSecondary={true}
                      onColorChange={(hex, isSec, commit) => studio.updateLayerColor(activeLayer.id, hex, isSec, commit)}
                    />
                  </div>
                </div>
              {/if}
            {:else if activeLogoSubTab === 'SPONSOR'}
              {@const activeLayer = sponsorLayer || getOrCreateLayer('SPONSOR')}
              {#if activeLayer}
                <div class="space-y-3">
                  <div class="grid grid-cols-2 gap-3 p-3 bg-[#0F141C] border border-[#2E3D56] rounded-xl">
                    <div>
                      <label for="sponsor-label-tab" class="block text-xs text-[#94A3B8] mb-1">Testo Marchio</label>
                      <input
                        id="sponsor-label-tab"
                        type="text"
                        maxlength="14"
                        value={activeLayer.properties.sponsorLabel ?? 'AERO·LAB'}
                        oninput={(e) => studio.updateLayerProperty(activeLayer.id, 'sponsorLabel', (e.target as HTMLInputElement).value)}
                        class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white font-bold"
                      />
                    </div>
                    <div>
                      <label for="sponsor-geom-tab" class="block text-xs text-[#94A3B8] mb-1">Motivo Geometrico</label>
                      <select
                        id="sponsor-geom-tab"
                        value={activeLayer.properties.sponsorGeometry || 'HEX_TECH_BAR'}
                        onchange={(e) => studio.updateLayerProperty(activeLayer.id, 'sponsorGeometry', (e.target as HTMLSelectElement).value)}
                        class="w-full px-2.5 py-1.5 text-xs bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                      >
                        {#each sponsorGeometries as sg (sg.value)}
                          <option value={sg.value}>{sg.label}</option>
                        {/each}
                      </select>
                    </div>
                  </div>

                  <!-- Sponsor Snapping Pad -->
                  <div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-3 space-y-2">
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-bold text-white flex items-center gap-1.5">
                        <span>🧲</span>
                        <span>Allineamento Sponsor 1-Click</span>
                      </span>
                      <span class="text-[10px] px-2 py-0.5 rounded-full bg-[#38BDF8]/20 text-[#38BDF8] font-mono-tabular border border-[#38BDF8]/30">Guida Attiva</span>
                    </div>

                    <div class="grid grid-cols-3 gap-1.5">
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('CHEST_CENTER')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#38BDF8] border border-[#2E3D56] cursor-pointer"
                      >
                        🎯 Centro Petto
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('SPONSOR')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#00C49F] border border-[#2E3D56] cursor-pointer"
                      >
                        ⭐ Standard
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('COLLAR')}
                        class="px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#38BDF8] border border-[#2E3D56] cursor-pointer"
                      >
                        👔 Colletto
                      </button>
                      <button
                        type="button"
                        onclick={() => applyQuickSnap('RESET')}
                        class="col-span-3 px-2 py-1.5 text-[11px] font-semibold rounded-lg bg-[#1A2230] hover:bg-[#253146] text-[#EF4444] border border-[#2E3D56] cursor-pointer"
                      >
                        ↺ Centra Posizione
                      </button>
                    </div>

                    <div class="grid grid-cols-2 gap-3 pt-1">
                      <div>
                        <div class="flex justify-between text-[11px] text-[#94A3B8] font-mono-tabular mb-1">
                          <span>Offset X</span>
                          <span>{activeLayer.properties.offsetX ?? 0}px</span>
                        </div>
                        <input
                          type="range"
                          min="-200"
                          max="200"
                          step="2"
                          value={activeLayer.properties.offsetX ?? 0}
                          oninput={(e) => studio.updateLayerProperty(activeLayer.id, 'offsetX', parseInt((e.target as HTMLInputElement).value, 10))}
                          class="w-full h-1.5 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#38BDF8]"
                        />
                      </div>
                      <div>
                        <div class="flex justify-between text-[11px] text-[#94A3B8] font-mono-tabular mb-1">
                          <span>Offset Y</span>
                          <span>{activeLayer.properties.offsetY ?? 20}px</span>
                        </div>
                        <input
                          type="range"
                          min="-200"
                          max="200"
                          step="2"
                          value={activeLayer.properties.offsetY ?? 20}
                          oninput={(e) => studio.updateLayerProperty(activeLayer.id, 'offsetY', parseInt((e.target as HTMLInputElement).value, 10))}
                          class="w-full h-1.5 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#38BDF8]"
                        />
                      </div>
                    </div>

                    <div class="pt-2 border-t border-[#2E3D56]">
                      <label for="sponsor-blend-tab" class="block text-xs text-[#94A3B8] mb-1">Fusione Sponsor (Blend Mode)</label>
                      <select
                        id="sponsor-blend-tab"
                        value={activeLayer.blendMode}
                        onchange={(e) => studio.setLayerBlendMode(activeLayer.id, (e.target as HTMLSelectElement).value as BlendMode)}
                        class="w-full px-2.5 py-1.5 text-xs font-semibold bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white"
                      >
                        {#each blendModes as bm (bm.value)}
                          <option value={bm.value}>{bm.label}</option>
                        {/each}
                      </select>
                    </div>
                  </div>

                  <!-- Color Picker for Sponsor -->
                  <div class="space-y-1.5">
                    <span class="text-xs font-bold uppercase tracking-wider text-[#94A3B8]">Colori Sponsor:</span>
                    <ColorPickerHsv
                      activeHex={activeLayer.color}
                      secondaryHex={activeLayer.secondaryColor}
                      hasSecondary={true}
                      onColorChange={(hex, isSec, commit) => studio.updateLayerColor(activeLayer.id, hex, isSec, commit)}
                    />
                  </div>
                </div>
              {/if}
            {:else}
              {@const numLayer = numberLayer || getOrCreateLayer('NUMBER')}
              {@const nmLayer = nameLayer}
              <div class="space-y-3">
                <!-- Name & Number Inputs -->
                <div class="p-3 bg-[#0F141C] border border-[#2E3D56] rounded-xl space-y-3">
                  {#if nmLayer}
                    <div class="grid grid-cols-2 gap-3">
                      <div>
                        <label for="p-name-tab" class="block text-xs text-[#94A3B8] mb-1">Nome Dorsale</label>
                        <input
                          id="p-name-tab"
                          type="text"
                          maxlength="16"
                          value={nmLayer.properties.text ?? 'ROSSI'}
                          oninput={(e) => studio.updateLayerProperty(nmLayer.id, 'text', (e.target as HTMLInputElement).value)}
                          class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white font-bold"
                        />
                      </div>
                      <div>
                        <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                          <span>Curvatura Arco</span>
                          <span>{nmLayer.properties.curveRadius ?? 680}px</span>
                        </div>
                        <input
                          type="range"
                          min="0"
                          max="1100"
                          step="50"
                          value={nmLayer.properties.curveRadius ?? 680}
                          oninput={(e) => studio.updateLayerProperty(nmLayer.id, 'curveRadius', parseInt((e.target as HTMLInputElement).value, 10))}
                          class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                        />
                      </div>
                    </div>
                  {/if}

                  {#if numLayer}
                    <div class="grid grid-cols-2 gap-3 items-center pt-2 border-t border-[#2E3D56]">
                      <div>
                        <label for="num-tab" class="block text-xs text-[#94A3B8] mb-1">Numero Gara (1–99)</label>
                        <input
                          id="num-tab"
                          type="text"
                          maxlength="2"
                          value={numLayer.properties.numberText ?? '10'}
                          oninput={(e) => studio.updateLayerProperty(numLayer.id, 'numberText', (e.target as HTMLInputElement).value)}
                          class="w-full px-2.5 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white font-bold"
                        />
                      </div>

                      <label class="flex items-center gap-2 text-xs text-white cursor-pointer pt-4">
                        <input
                          type="checkbox"
                          checked={Boolean(numLayer.properties.showFrontNumber ?? true)}
                          onchange={(e) => studio.updateLayerProperty(numLayer.id, 'showFrontNumber', (e.target as HTMLInputElement).checked)}
                          class="accent-[#00C49F]"
                        />
                        Numero Frontale
                      </label>
                    </div>

                    <div>
                      <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
                        <span>Contorno Vettoriale</span>
                        <span>{numLayer.properties.strokeWidth ?? 12}px</span>
                      </div>
                      <input
                        type="range"
                        min="0"
                        max="24"
                        step="1"
                        value={numLayer.properties.strokeWidth ?? 12}
                        oninput={(e) => studio.updateLayerProperty(numLayer.id, 'strokeWidth', parseInt((e.target as HTMLInputElement).value, 10))}
                        class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                      />
                    </div>
                  {/if}
                </div>

                {#if numLayer}
                  <!-- Color Picker for Number & Name -->
                  <div class="space-y-1.5">
                    <span class="text-xs font-bold uppercase tracking-wider text-[#94A3B8]">Colori Nome & Numero:</span>
                    <ColorPickerHsv
                      activeHex={numLayer.color}
                      secondaryHex={numLayer.secondaryColor}
                      hasSecondary={true}
                      onColorChange={(hex, isSec, commit) => {
                        studio.updateLayerColor(numLayer.id, hex, isSec, commit);
                        if (nmLayer) {
                          studio.updateLayerColor(nmLayer.id, hex, isSec, commit);
                        }
                      }}
                    />
                  </div>
                {/if}
              </div>
            {/if}
          </div>
        {/if}

        <!-- TAB 4: TUTTI I LIVELLI -->
        {#if activeEditorTab === 'LAYERS'}
          <div class="p-4 space-y-4 max-h-[calc(100vh-210px)] overflow-y-auto">
            <!-- Header with Add Layer button -->
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold text-white">
                Pila Livelli ({studio.activeLayers.length})
              </span>
              <button
                type="button"
                onclick={() => studio.addNewLayer(studio.selectedLayer?.type || 'PATTERN')}
                class="px-2.5 py-1 text-xs font-semibold text-[#00C49F] hover:text-white bg-[#0F141C] border border-[#2E3D56] rounded-md cursor-pointer whitespace-nowrap"
              >
                + Nuovo Livello
              </button>
            </div>

            <!-- Draggable / Reorderable Layer List -->
            <div class="space-y-2 max-h-64 overflow-y-auto pr-1">
              {#each [...studio.activeLayers].sort((a, b) => b.zIndex - a.zIndex) as layer (layer.id)}
                {@const isCurrent = studio.selectedLayer?.id === layer.id}
                <div
                  class="flex items-center justify-between gap-2.5 p-3 rounded-xl border shadow-sm transition-all {isCurrent
                    ? 'bg-[#253146] border-[#00C49F] shadow-md ring-1 ring-[#00C49F]/30'
                    : 'bg-[#0F141C] border-[#2E3D56] hover:border-[#64748B]'}"
                >
                  <button
                    type="button"
                    onclick={() => (studio.selectedLayerId = layer.id)}
                    class="flex items-center gap-2.5 min-w-0 flex-1 text-left cursor-pointer"
                  >
                    <span
                      class="w-6 h-6 rounded-md border border-white/30 shrink-0 shadow-sm flex items-center justify-center text-xs"
                      style="background-color: {layer.color};"
                    >
                      {getLayerIcon(layer.type)}
                    </span>
                    <div class="min-w-0 flex-1">
                      <div class="text-xs font-bold text-white truncate flex items-center gap-1.5">
                        <span>{layer.name}</span>
                        <span class="text-[9px] px-1 py-0.2 rounded bg-[#0F141C] text-[#00C49F] font-mono-tabular border border-[#2E3D56]">z:{layer.zIndex}</span>
                      </div>
                      <div class="text-[10px] text-[#94A3B8] font-mono-tabular">
                        {layer.type} · {Math.round(layer.opacity * 100)}%
                      </div>
                    </div>
                  </button>

                  <div class="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onclick={() => studio.toggleLayerVisibility(layer.id)}
                      class="px-2 py-0.5 text-[10px] font-mono-tabular font-semibold rounded transition-colors {layer.visible
                        ? 'bg-[#10B981]/20 text-[#10B981] border border-[#10B981]/40'
                        : 'bg-[#EF4444]/20 text-[#EF4444] border border-[#EF4444]/40'} cursor-pointer"
                    >
                      {layer.visible ? 'ON' : 'OFF'}
                    </button>
                    <button
                      type="button"
                      onclick={() => studio.moveLayerZIndex(layer.id, 'UP')}
                      class="w-6 h-6 flex items-center justify-center rounded bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#2E3D56] cursor-pointer text-xs font-bold"
                      title="Sposta su"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      onclick={() => studio.moveLayerZIndex(layer.id, 'DOWN')}
                      class="w-6 h-6 flex items-center justify-center rounded bg-[#1A2230] text-[#94A3B8] hover:text-white hover:bg-[#2E3D56] cursor-pointer text-xs font-bold"
                      title="Sposta giù"
                    >
                      ↓
                    </button>
                    {#if studio.activeLayers.length > 1}
                      <button
                        type="button"
                        onclick={() => studio.deleteLayer(layer.id)}
                        class="w-6 h-6 flex items-center justify-center rounded bg-[#EF4444]/20 text-[#EF4444] hover:bg-[#EF4444]/40 cursor-pointer text-xs"
                        title="Elimina livello"
                      >
                        ✕
                      </button>
                    {/if}
                  </div>
                </div>
              {/each}
            </div>

            <!-- Inspector for Selected Layer -->
            {#if studio.selectedLayer}
              {@const activeLayer = studio.selectedLayer}
              <div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-3.5 space-y-3">
                <div class="flex items-center justify-between gap-2 border-b border-[#2E3D56] pb-2">
                  <input
                    type="text"
                    value={activeLayer.name}
                    oninput={(e) => {
                      activeLayer.name = (e.target as HTMLInputElement).value;
                      studio.textureRevision++;
                    }}
                    class="text-xs font-semibold text-white bg-transparent border-b border-transparent focus:border-[#00C49F] focus:outline-none flex-1"
                  />
                </div>

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
                      oninput={(e) => studio.setLayerOpacity(activeLayer.id, parseFloat((e.target as HTMLInputElement).value))}
                      class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
                    />
                  </div>

                  <div>
                    <label for="layer-blend-select" class="block text-xs text-[#94A3B8] mb-1">Metodo Fusione</label>
                    <select
                      id="layer-blend-select"
                      value={activeLayer.blendMode}
                      onchange={(e) => studio.setLayerBlendMode(activeLayer.id, (e.target as HTMLSelectElement).value as BlendMode)}
                      class="w-full px-2.5 py-1.5 text-xs font-semibold bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white focus:outline-none focus:border-[#00C49F]"
                    >
                      {#each blendModes as bm (bm.value)}
                        <option value={bm.value}>{bm.label}</option>
                      {/each}
                    </select>
                  </div>
                </div>

                <ColorPickerHsv
                  activeHex={activeLayer.color}
                  secondaryHex={activeLayer.secondaryColor}
                  hasSecondary={true}
                  onColorChange={(hex, isSec, commit) => studio.updateLayerColor(activeLayer.id, hex, isSec, commit)}
                />
              </div>
            {/if}
          </div>
        {/if}
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

        <div
          role="region"
          aria-label="Area di editing e posizionamento magnetico"
          class="relative bg-[#0F141C] border border-[#2E3D56] rounded-lg overflow-hidden flex items-center justify-center aspect-square max-h-[600px] select-none touch-none {studio.selectedLayer && (studio.selectedLayer.type === 'SPONSOR' || studio.selectedLayer.type === 'BADGE') ? 'cursor-grab active:cursor-grabbing ring-1 ring-[#00C49F]/30' : ''}"
          onpointerdown={handlePointerDown}
          onpointermove={handlePointerMove}
          onpointerup={handlePointerUp}
          onpointercancel={handlePointerUp}
        >
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

          <!-- Magnetic Snapping Visual Guidelines Overlay -->
          {#if isDraggingLayer || activeSnapGuide}
            <svg class="absolute inset-0 w-full h-full pointer-events-none z-20" viewBox="0 0 512 512">
              {#if snapTargetType === 'CHEST_CENTER' || activeSnapGuide?.includes('Centro')}
                <line x1="256" y1="50" x2="256" y2="440" stroke="#00C49F" stroke-width="2.5" stroke-dasharray="6,4" />
                <circle cx="256" cy="256" r="6" fill="#00C49F" />
                <circle cx="256" cy="256" r="14" fill="none" stroke="#00C49F" stroke-width="1.5" opacity="0.75" />
              {/if}

              {#if snapTargetType === 'COLLAR_HEIGHT' || activeSnapGuide?.includes('Colletto')}
                <line x1="100" y1="160" x2="412" y2="160" stroke="#38BDF8" stroke-width="2.5" stroke-dasharray="6,4" />
                <circle cx="256" cy="160" r="6" fill="#38BDF8" />
              {/if}

              {#if snapTargetType === 'CREST_LEFT' || activeSnapGuide?.includes('Cuore SX')}
                <line x1="337" y1="110" x2="337" y2="300" stroke="#F59E0B" stroke-width="2" stroke-dasharray="6,4" />
                <line x1="220" y1="210" x2="410" y2="210" stroke="#F59E0B" stroke-width="2" stroke-dasharray="6,4" />
                <circle cx="337" cy="210" r="7" fill="none" stroke="#F59E0B" stroke-width="2" />
              {/if}

              {#if snapTargetType === 'CREST_RIGHT' || activeSnapGuide?.includes('Petto DX')}
                <line x1="175" y1="110" x2="175" y2="300" stroke="#F59E0B" stroke-width="2" stroke-dasharray="6,4" />
                <line x1="100" y1="210" x2="260" y2="210" stroke="#F59E0B" stroke-width="2" stroke-dasharray="6,4" />
                <circle cx="175" cy="210" r="7" fill="none" stroke="#F59E0B" stroke-width="2" />
              {/if}

              {#if snapTargetType === 'SPONSOR_LINE' || activeSnapGuide?.includes('Sponsor')}
                <line x1="100" y1="290" x2="412" y2="290" stroke="#A855F7" stroke-width="2" stroke-dasharray="6,4" />
                <circle cx="256" cy="290" r="6" fill="#A855F7" />
              {/if}
            </svg>
          {/if}

          <!-- Floating Snapping HUD Banner -->
          {#if activeSnapGuide}
            <div class="absolute top-4 left-1/2 -translate-x-1/2 px-3.5 py-1.5 rounded-full bg-[#0F141C]/90 border border-[#00C49F] shadow-xl shadow-[#00C49F]/20 text-xs font-bold text-white flex items-center gap-2 z-30 pointer-events-none transition-all">
              <span class="text-sm">🧲</span>
              <span class="text-[#00C49F]">{activeSnapGuide}</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-[#00C49F]/20 text-[#00C49F] font-mono-tabular">SNAP</span>
            </div>
          {/if}

          <!-- Dragging Helper Badge -->
          {#if studio.selectedLayer && (studio.selectedLayer.type === 'SPONSOR' || studio.selectedLayer.type === 'BADGE')}
            <div class="absolute bottom-2 left-2 px-2.5 py-1 rounded-md bg-[#0F141C]/85 border border-[#2E3D56] text-[10px] text-[#00C49F] flex items-center gap-1.5 pointer-events-none backdrop-blur-sm z-20">
              <span>🧲 Snapping Magnetico</span>
              <span class="text-[#64748B]">· Trascina per agganciare</span>
            </div>
          {/if}
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
