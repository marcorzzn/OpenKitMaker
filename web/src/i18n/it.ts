// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-OFFLINE-MODE
import type { KitCategory, LayerType } from '../types/kit';

export const i18n = {
  app: {
    name: 'OpenKitMaker',
    shortName: 'OpenKitMaker',
    offline_badge: 'Modalità Offline',
    online_badge: 'Connesso · Pronto all\'Uso',
    subtitle: 'Crea e personalizza divise da calcio uniche in 3D fotorealistico',
  },
  nav: {
    home: 'Home',
    catalog: 'Catalogo Template',
    editor: 'Editor 3D',
    saved: 'I Miei Kit',
    export: 'Esporta',
  },
  home: {
    cta_new_kit: 'Crea Nuova Divisa',
    cta_all_kits: 'I Miei Progetti',
    cta_import_zip: 'Importa Kit (.ZIP)',
    recent_drafts_title: 'I Tuoi Ultimi Progetti',
    empty_drafts: 'Nessun kit salvato al momento. Scegli un modello dal catalogo per iniziare subito a creare.',
  },
  catalog: {
    title: 'Catalogo Modelli & Template',
    search_placeholder: 'Cerca per nome, stile geometrico o tipologia di divisa...',
    categories: {
      ALL: 'Tutti i Modelli',
      HOME: 'Prima Maglia (Casa)',
      AWAY: 'Seconda Maglia (Trasferta)',
      THIRD: 'Terza Divisa',
      GK: 'Portiere',
      SPECIAL: 'Edizioni Speciali',
    } as Record<'ALL' | KitCategory, string>,
  },
  editor: {
    view_2d: 'Vista 2D',
    view_3d: 'Vista 3D',
    view_split: 'Vista Doppia (2D + 3D)',
    save_draft: 'Salva Kit',
    export_btn: 'Esporta',
    lighting_studio: 'Luce Studio (Neutro)',
    lighting_stadium: 'Luce Stadio (Riflettori)',
    component_rail: {
      BASE: 'Base',
      PATTERN: 'Trama',
      COLLAR: 'Colletto',
      SLEEVES: 'Maniche',
      SPONSOR: 'Sponsor',
      BADGE: 'Stemma',
      TEXT_NAME: 'Nome',
      NUMBER: 'Numero',
    } as Record<LayerType, string>,
  },
  saved: {
    title: 'I Tuoi Kit Salvati',
    subtitle: 'Tutti i tuoi progetti salvati in memoria locale, accessibili sempre anche senza connessione internet.',
    open_editor: "Apri nell'Editor",
    rename: 'Rinomina',
    duplicate: 'Duplica',
    delete: 'Elimina',
    confirm_delete_title: 'Conferma Eliminazione',
    confirm_delete_msg: 'Sei sicuro di voler eliminare definitivamente questa divisa?',
  },
  export: {
    title: 'Esporta e Condividi il Kit',
    option_png_title: 'Texture 2D Grafica (2K o 4K)',
    option_png_desc: 'Texture grafica ad altissima fedeltà lossless pronta per modding, videogiochi o stampa su tessuto.',
    option_zip_title: 'Archivio Completo Divisa (.ZIP)',
    option_zip_desc: 'Pacchetto completo con texture di Maglia, Pantaloncini, Calzettoni, anteprima e configurazione riapribile ovunque.',
  },
  errors: {
    storage_full: "Spazio di archiviazione insufficiente per completare l'operazione.",
    export_failed: "Si è verificato un errore durante l'esportazione del file. Riprova.",
    import_failed: 'File non valido o non riconosciuto. Assicurati che sia un archivio esportato da OpenKitMaker.',
  },
} as const;
