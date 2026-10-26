// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-OFFLINE-MODE
import type { KitCategory, LayerType } from '../types/kit';

export const i18n = {
  app: {
    name: 'OpenKitMaker-Web',
    shortName: 'OpenKitMaker',
    offline_badge: 'Modalità Offline',
    online_badge: ' Connesso · Pronto Offline',
    subtitle: 'Studio Sartoriale 2D/3D per Divise Sportive · Contratto di Parità v1.0',
  },
  nav: {
    home: 'Home',
    catalog: 'Catalogo',
    editor: 'Editor Kit',
    saved: 'I Miei Kit',
    export: 'Esporta',
  },
  home: {
    cta_new_kit: 'Nuovo Kit da Template',
    cta_all_kits: 'Tutti i miei kit',
    cta_import_zip: 'Importa Archivio .ZIP',
    recent_drafts_title: 'Ultime Bozze Modificate',
    empty_drafts: 'Nessuna bozza salvata in locale. Inizia scegliendo un modello dal Catalogo Template.',
  },
  catalog: {
    title: 'Catalogo Template',
    search_placeholder: 'Cerca template per nome, stile geometrico o categoria...',
    categories: {
      ALL: 'Tutti',
      HOME: 'Casa',
      AWAY: 'Trasferta',
      THIRD: 'Terza',
      GK: 'Portiere',
      SPECIAL: 'Speciali',
    } as Record<'ALL' | KitCategory, string>,
  },
  editor: {
    view_2d: 'Vista 2D',
    view_3d: 'Vista 3D',
    view_split: 'Vista Doppia',
    save_draft: 'Salva Bozza',
    export_btn: 'Esporta',
    lighting_studio: 'Luce Studio (6500K)',
    lighting_stadium: 'Luce Stadio (5400K)',
    component_rail: {
      BASE: 'Base',
      PATTERN: 'Strisce',
      COLLAR: 'Colletto',
      SLEEVES: 'Maniche',
      SPONSOR: 'Sponsor',
      BADGE: 'Stemma',
      TEXT_NAME: 'Nome',
      NUMBER: 'Numero',
    } as Record<LayerType, string>,
  },
  saved: {
    title: 'I Miei Kit',
    subtitle: 'Archivio locale persistito in IndexedDB (Dexie.js) con minikit 512×512.',
    open_editor: "Apri nell'Editor",
    rename: 'Rinomina',
    duplicate: 'Duplica',
    delete: 'Elimina',
    confirm_delete_title: 'Conferma Eliminazione',
    confirm_delete_msg: 'Sei sicuro di voler eliminare definitivamente questo kit dallo storage locale?',
  },
  export: {
    title: 'Centro Esportazione Interoperabile',
    option_png_title: 'Texture PNG 2048×2048',
    option_png_desc: 'Salvataggio texture UV distesa ad alta definizione RGBA a 32-bit lossless.',
    option_zip_title: 'Archivio Completo (.ZIP)',
    option_zip_desc: 'Pacchetto interoperabile Web/Android con texture.png, preview.png e manifest.json firmato SHA-256.',
  },
  errors: {
    storage_full: "Spazio di archiviazione locale insufficiente per completare l'operazione.",
    export_failed: "Si è verificato un errore durante l'esportazione del file. Riprova.",
    import_failed: 'Archivio ZIP non valido o manifest.json non conforme allo schema v1.0.0.',
  },
} as const;
