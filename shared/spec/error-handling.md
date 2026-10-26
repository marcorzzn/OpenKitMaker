# OpenKitMaker — Gestione degli Errori e Resilienza (Specifica di Parità)

Documento di specifica per la gestione unificata delle eccezioni e dei messaggi utente in **OpenKitMaker**.
Nessun messaggio deve essere hardcoded: tutti i messaggi mostrati all'utente devono essere referenziati tramite le chiavi del dizionario i18n (`shared/i18n/it.json`).

---

## Tabella dei Codici di Errore Standard

| Codice Errore | Significato Tecnico | Chiave i18n Messaggio | Comportamento UX Richiesto |
|---|---|---|---|
| **`ERR_STORAGE_FULL`** | Spazio su disco o quota IndexedDB/Room esaurita | `errors.storage_full` | Mostra dialog modale con invito a liberare spazio o cancellare bozze vecchie |
| **`ERR_EXPORT_FAILED`** | Fallimento nella generazione del file PNG o dello ZIP | `errors.export_failed` | Mostra snackbar di errore con pulsante "Riprova" |
| **`ERR_TEXTURE_INVALID`** | Layer corrotto o formato immagine non supportato | `errors.texture_invalid` | Disattiva il layer problematico e avvisa l'utente senza interrompere l'editor |
| **`ERR_TEMPLATE_LOAD_FAILED`** | Impossibile leggere il file JSON/risorsa del template | `errors.template_load_failed` | Ritorna al catalogo e mostra notifica di template non disponibile offline |
| **`ERR_3D_CONTEXT_LOST`** | Perdita del contesto WebGL o crash del driver GPU mobile | `errors.gpu_context_lost` | Ricarica automatica del contesto grafico o fallback su visualizzazione 2D |
| **`ERR_PERMISSION_DENIED`** | Negato permesso di scrittura su storage esterno (Android) | `errors.permission_denied` | Mostra dialog esplicativo con link alle impostazioni di sistema |

---

## Linee Guida per il Trattamento degli Errori

1. **Non Bloccante per Default**: Se un componente secondario fallisce (es. il Viewport 3D perde il contesto grafico), l'editor 2D deve continuare a funzionare regolarmente permettendo all'utente di non perdere il lavoro svolto.
2. **Auto-Salvataggio di Sicurezza**: Prima di qualsiasi operazione rischiosa (come l'esportazione ad alta risoluzione o il caricamento di texture esterne pesanti), lo stato corrente viene salvato in una bozza temporanea di emergenza (`kit_autosave_recovery`).
3. **Log Strutturati**: Entrambe le piattaforme devono emettere log nel formato standard:
   ```text
   [OpenKitMaker][ERROR][CodiceErrore] Descrizione contestuale dell'errore
   ```
