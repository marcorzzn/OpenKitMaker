// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-SELECT-TEMPLATE, FUN-MANAGE-LAYERS, FUN-EDIT-COLOR, FUN-SAVE-LOCAL, FUN-OFFLINE-MODE
import type {
  ScreenId,
  Kit,
  Layer,
  LayerType,
  BlendMode,
  Template,
  EditorViewMode,
  LightingPreset,
} from '../types/kit';
import {
  BUILTIN_TEMPLATES,
  cloneTemplateToKit,
  generateUuidV4,
} from '../data/templates';
import {
  saveKitToDb,
  loadAllKitsFromDb,
  deleteKitFromDb,
  saveRecentColorsToDb,
  loadRecentColorsFromDb,
} from '../lib/db';
import {
  compositeKit2048,
  renderMinikitPreview512,
  TEXTURE_SIZE,
} from '../lib/compositor2d';

export interface ToastNotification {
  id: string;
  type: 'success' | 'error' | 'info';
  message: string;
}

class OpenKitMakerStudioStore {
  // Navigation state (SCR-*)
  currentScreen = $state<ScreenId>('SCR-HOME');

  // Active Kit in Editor
  activeKit = $state<Kit>(cloneTemplateToKit(BUILTIN_TEMPLATES[0]));
  selectedLayerId = $state<string>('');
  activeLayerTypeFilter = $state<LayerType | 'ALL'>('ALL');

  // Editor View & 3D state
  editorViewMode = $state<EditorViewMode>('SPLIT');
  lightingPreset = $state<LightingPreset>('studio');
  showUvGuides = $state<boolean>(false);
  textureRevision = $state<number>(0);
  lastCompositeTimeMs = $state<number>(4.2);

  // Recent 8 colors (FUN-EDIT-COLOR)
  recentColors = $state<string[]>([
    '#0F52BA',
    '#00C49F',
    '#EF4444',
    '#F59E0B',
    '#10B981',
    '#FFFFFF',
    '#0F141C',
    '#1E3A8A',
  ]);

  // Saved Kits in IndexedDB (SCR-SAVED / SCR-HOME)
  savedKits = $state<Kit[]>([]);
  isLoadingSaved = $state<boolean>(true);

  // Connectivity & PWA Install State (FUN-OFFLINE-MODE)
  isOnline = $state<boolean>(typeof navigator !== 'undefined' ? navigator.onLine : true);
  canInstallPwa = $state<boolean>(false);
  isStandalonePwa = $state<boolean>(false);
  private deferredInstallPrompt: any = null;

  // Toasts
  toasts = $state<ToastNotification[]>([]);

  // Derived values using Svelte 5 $derived rune
  selectedLayer = $derived<Layer | undefined>(
    this.activeKit.layers.find((l) => l.id === this.selectedLayerId) ||
      this.activeKit.layers[0]
  );

  recentThreeDrafts = $derived<Kit[]>(this.savedKits.slice(0, 3));

  constructor() {
    if (this.activeKit.layers.length > 0) {
      this.selectedLayerId = this.activeKit.layers[0].id;
    }
  }

  async init(): Promise<void> {
    if (typeof window !== 'undefined') {
      this.isOnline = navigator.onLine;
      window.addEventListener('online', () => {
        this.isOnline = true;
        this.notify('info', 'Connessione ristabilita. Sincronizzazione locale attiva.');
      });
      window.addEventListener('offline', () => {
        this.isOnline = false;
        this.notify('info', 'Modalità Offline attiva: tutte le funzioni 2D/3D e salvataggio sono operative al 100%.');
      });

      this.isStandalonePwa =
        window.matchMedia('(display-mode: standalone)').matches ||
        (window.navigator as any).standalone === true;

      window.addEventListener('beforeinstallprompt', (e: Event) => {
        e.preventDefault();
        this.deferredInstallPrompt = e;
        this.canInstallPwa = true;
      });

      window.addEventListener('appinstalled', () => {
        this.canInstallPwa = false;
        this.isStandalonePwa = true;
        this.deferredInstallPrompt = null;
        this.notify('success', 'OpenKitMaker-Web installata correttamente sul dispositivo.');
      });
    }

    try {
      const [kits, colors] = await Promise.all([
        loadAllKitsFromDb(),
        loadRecentColorsFromDb(),
      ]);
      this.recentColors = colors;

      if (kits.length === 0) {
        // Seed initial sample kit so SCR-HOME shows an immediate draft out-of-the-box
        const starterKit = cloneTemplateToKit(BUILTIN_TEMPLATES[0]);
        starterKit.metadata.thumbnailDataUrl = this.generateMinikitDataUrl(starterKit);
        await saveKitToDb(starterKit);
        this.savedKits = [starterKit];
        this.activeKit = starterKit;
        this.selectedLayerId = starterKit.layers[0]?.id || '';
      } else {
        this.savedKits = kits;
        this.activeKit = JSON.parse(JSON.stringify(kits[0]));
        this.selectedLayerId = this.activeKit.layers[0]?.id || '';
      }
    } catch {
      // Fallback if IndexedDB is restricted
    } finally {
      this.isLoadingSaved = false;
    }
  }

  generateMinikitDataUrl(kit: Kit): string {
    if (typeof document === 'undefined') return '';
    const texCanvas = document.createElement('canvas');
    texCanvas.width = TEXTURE_SIZE;
    texCanvas.height = TEXTURE_SIZE;
    compositeKit2048(texCanvas, kit);

    const prevCanvas = document.createElement('canvas');
    prevCanvas.width = 512;
    prevCanvas.height = 512;
    return renderMinikitPreview512(prevCanvas, texCanvas, kit);
  }

  navigateTo(screen: ScreenId): void {
    this.currentScreen = screen;
  }

  /**
   * FUN-SELECT-TEMPLATE: Initializes a new Kit by cloning a template with fresh UUIDv4.
   */
  selectTemplate(template: Template): void {
    const newKit = cloneTemplateToKit(template);
    this.activeKit = newKit;
    this.selectedLayerId = newKit.layers[0]?.id || '';
    this.activeLayerTypeFilter = 'ALL';
    this.textureRevision++;
    this.currentScreen = 'SCR-EDITOR';
    this.notify('info', `Template "${template.name}" caricato nello Studio.`);
  }

  openSavedKit(kit: Kit, targetScreen: ScreenId = 'SCR-EDITOR'): void {
    const cloned: Kit = JSON.parse(JSON.stringify(kit));
    this.activeKit = cloned;
    this.selectedLayerId = cloned.layers[0]?.id || '';
    this.lightingPreset = cloned.metadata?.lightingPreset || 'studio';
    this.textureRevision++;
    this.currentScreen = targetScreen;
  }

  selectLayerByType(type: LayerType): void {
    this.activeLayerTypeFilter = type;
    const match = this.activeKit.layers.find((l) => l.type === type);
    if (match) {
      this.selectedLayerId = match.id;
    }
  }

  /**
   * FUN-EDIT-COLOR: Immediate reactive color update + recent 8 colors history.
   */
  updateLayerColor(
    layerId: string,
    hexColor: string,
    isSecondary = false,
    commitRecent = false
  ): void {
    const layer = this.activeKit.layers.find((l) => l.id === layerId);
    if (!layer) return;

    const normalized = hexColor.startsWith('#') ? hexColor.toUpperCase() : `#${hexColor.toUpperCase()}`;
    if (isSecondary) {
      layer.secondaryColor = normalized;
    } else {
      layer.color = normalized;
    }
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;

    if (commitRecent && /^#[0-9A-F]{6}([0-9A-F]{2})?$/i.test(normalized)) {
      const filtered = this.recentColors.filter(
        (c) => c.toUpperCase() !== normalized.slice(0, 7)
      );
      this.recentColors = [normalized.slice(0, 7), ...filtered].slice(0, 8);
      saveRecentColorsToDb(this.recentColors).catch(() => {});
    }
  }

  /**
   * FUN-MANAGE-LAYERS: Layer visibility, opacity, blend mode, z-index, and CRUD.
   */
  toggleLayerVisibility(layerId: string): void {
    const layer = this.activeKit.layers.find((l) => l.id === layerId);
    if (!layer) return;
    layer.visible = !layer.visible;
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
  }

  setLayerOpacity(layerId: string, opacity: number): void {
    const layer = this.activeKit.layers.find((l) => l.id === layerId);
    if (!layer) return;
    layer.opacity = Math.max(0, Math.min(1, Number(opacity)));
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
  }

  setLayerBlendMode(layerId: string, blendMode: BlendMode): void {
    const layer = this.activeKit.layers.find((l) => l.id === layerId);
    if (!layer) return;
    layer.blendMode = blendMode;
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
  }

  updateLayerProperty(layerId: string, key: string, value: unknown): void {
    const layer = this.activeKit.layers.find((l) => l.id === layerId);
    if (!layer) return;
    layer.properties = {
      ...layer.properties,
      [key]: value,
    };
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
  }

  moveLayerZIndex(layerId: string, direction: 'UP' | 'DOWN'): void {
    const sorted = [...this.activeKit.layers].sort((a, b) => a.zIndex - b.zIndex);
    const idx = sorted.findIndex((l) => l.id === layerId);
    if (idx === -1) return;
    const swapIdx = direction === 'UP' ? idx + 1 : idx - 1;
    if (swapIdx < 0 || swapIdx >= sorted.length) return;

    const temp = sorted[idx];
    sorted[idx] = sorted[swapIdx];
    sorted[swapIdx] = temp;

    sorted.forEach((l, i) => {
      l.zIndex = i;
    });
    this.activeKit.layers = sorted;
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
  }

  addNewLayer(type: LayerType): void {
    const maxZ = this.activeKit.layers.reduce((max, l) => Math.max(max, l.zIndex), 0);
    const defaultPropsByType: Record<LayerType, Record<string, unknown>> = {
      BASE: {},
      PATTERN: { patternStyle: 'VERTICAL_STRIPES', stripeCount: 5, stripeWidth: 0.45, angle: 0 },
      COLLAR: { collarStyle: 'V_NECK', trimWidth: 16 },
      SLEEVES: { cuffWidth: 34, raglanCut: true },
      SPONSOR: { sponsorGeometry: 'HEX_TECH_BAR', sponsorLabel: 'OPEN·KIT', scale: 1.0, offsetY: 20 },
      BADGE: { badgeShape: 'HEXAGON_PRO', badgePosition: 'LEFT_CHEST', scale: 1.0, innerRing: true },
      TEXT_NAME: { text: 'ATLETA', curveRadius: 680, fontSize: 84, strokeWidth: 8, offsetY: 0 },
      NUMBER: { numberText: '9', fontSize: 360, strokeWidth: 12, showFrontNumber: true, innerLineOffset: 6 },
    };

    const newLayer: Layer = {
      id: generateUuidV4(),
      name: `Livello ${type} #${this.activeKit.layers.length + 1}`,
      type,
      visible: true,
      opacity: 1.0,
      zIndex: maxZ + 1,
      color: '#00C49F',
      secondaryColor: '#FFFFFF',
      blendMode: 'NORMAL',
      properties: defaultPropsByType[type],
    };

    this.activeKit.layers = [...this.activeKit.layers, newLayer];
    this.selectedLayerId = newLayer.id;
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
    this.notify('success', `Nuovo livello "${newLayer.name}" aggiunto.`);
  }

  deleteLayer(layerId: string): void {
    if (this.activeKit.layers.length <= 1) {
      this.notify('error', 'Il kit deve contenere almeno un livello attivo.');
      return;
    }
    this.activeKit.layers = this.activeKit.layers
      .filter((l) => l.id !== layerId)
      .map((l, idx) => ({ ...l, zIndex: idx }));

    if (this.selectedLayerId === layerId) {
      this.selectedLayerId = this.activeKit.layers[0]?.id || '';
    }
    this.activeKit.updatedAt = new Date().toISOString();
    this.textureRevision++;
  }

  /**
   * FUN-SAVE-LOCAL: Persists current kit to IndexedDB with 512x512 minikit thumbnail.
   */
  async saveActiveKitToLocal(): Promise<void> {
    try {
      this.activeKit.updatedAt = new Date().toISOString();
      this.activeKit.metadata.lightingPreset = this.lightingPreset;
      this.activeKit.metadata.thumbnailDataUrl = this.generateMinikitDataUrl(this.activeKit);

      await saveKitToDb(this.activeKit);
      this.savedKits = await loadAllKitsFromDb();
      this.notify('success', `Kit "${this.activeKit.name}" salvato in IndexedDB locale.`);
    } catch {
      this.notify(
        'error',
        "Spazio di archiviazione locale insufficiente per completare l'operazione."
      );
    }
  }

  async renameSavedKit(kitId: string, newName: string): Promise<void> {
    const trimmed = newName.trim();
    if (!trimmed) return;
    const target = this.savedKits.find((k) => k.id === kitId);
    if (!target) return;

    target.name = trimmed;
    target.updatedAt = new Date().toISOString();
    target.metadata.thumbnailDataUrl = this.generateMinikitDataUrl(target);
    await saveKitToDb(target);

    if (this.activeKit.id === kitId) {
      this.activeKit.name = trimmed;
    }
    this.savedKits = await loadAllKitsFromDb();
    this.notify('success', `Kit rinominato in "${trimmed}".`);
  }

  async duplicateSavedKit(kit: Kit): Promise<void> {
    const copy: Kit = JSON.parse(JSON.stringify(kit));
    const now = new Date().toISOString();
    copy.id = generateUuidV4();
    copy.name = `${kit.name} (Copia)`;
    copy.createdAt = now;
    copy.updatedAt = now;
    copy.layers = copy.layers.map((l) => ({ ...l, id: generateUuidV4() }));
    copy.metadata.thumbnailDataUrl = this.generateMinikitDataUrl(copy);

    await saveKitToDb(copy);
    this.savedKits = await loadAllKitsFromDb();
    this.notify('success', `Kit duplicato come "${copy.name}".`);
  }

  async removeSavedKit(kitId: string): Promise<void> {
    await deleteKitFromDb(kitId);
    this.savedKits = await loadAllKitsFromDb();
    this.notify('info', 'Kit eliminato dallo storage locale.');
  }

  async triggerPwaInstall(): Promise<void> {
    if (!this.deferredInstallPrompt) return;
    await this.deferredInstallPrompt.prompt();
    const { outcome } = await this.deferredInstallPrompt.userChoice;
    if (outcome === 'accepted') {
      this.canInstallPwa = false;
      this.deferredInstallPrompt = null;
    }
  }

  notify(type: 'success' | 'error' | 'info', message: string): void {
    const id = generateUuidV4();
    this.toasts = [...this.toasts, { id, type, message }];
    setTimeout(() => {
      this.toasts = this.toasts.filter((t) => t.id !== id);
    }, 4200);
  }

  dismissToast(id: string): void {
    this.toasts = this.toasts.filter((t) => t.id !== id);
  }
}

export const studio = new OpenKitMakerStudioStore();
