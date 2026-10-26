# OpenKitMaker — Checklist di Valutazione e Parità dei Prototipi

Questa checklist costituisce lo strumento oggettivo a disposizione dei committenti e degli sviluppatori per confrontare i due prototipi (**OpenKitMaker-Web** e **OpenKitMaker-Native**) e decidere quali componenti integrare nel progetto definitivo.

---

## Matrice Comparativa Oggettiva

| # | Dimensione di Confronto | Metrica Oggettiva | Soglia di Accettazione | Risultato OpenKitMaker-Web (A+B) | Risultato OpenKitMaker-Native (C) | Verdetto / Note Tecniche |
|---|---|---|---|---|---|---|
| **1** | **Copertura Schermate** | `ID implementati / ID in screens.md` | **100%** (5 su 5: Home, Catalog, Editor, Saved, Export) | [ ] In corso | [ ] In corso | |
| **2** | **Copertura Funzioni** | `ID implementati / ID in features.md` | **100%** (9 su 9: template, layer, color, composite, 3D, save, PNG, ZIP, offline) | [ ] In corso | [ ] In corso | |
| **3** | **Parità Test di Accettazione** | `Test superati / Totale parity-tests.md` | **≥ 95%** (almeno 6 su 7 scenari conformi) | [ ] In corso | [ ] In corso | |
| **4** | **Interoperabilità Export** | Importazione incrociata dello ZIP `kit_<id>.zip` | **100%** (Web apre ZIP Android e viceversa) | [ ] In corso | [ ] In corso | |
| **5** | **Fluidità Rendering 3D** | Framerate medio durante rotazione continua | **60 FPS stabili** (frame time ≤ 16,6 ms) | [ ] Three.js WebGL2 | [ ] Filament/SceneView GPU | |
| **6** | **Dimensioni Pacchetto** | Dimensione del deliverable di distribuzione | Web: **≤ 80 KB gzip JS**; Native: **APK ≤ 25 MB** | [ ] Misurato con Vite | [ ] Misurato con APK Analyzer | |
| **7** | **Startup & Reattività** | Tempo al primo render interattivo (LCP / Startup) | Web: **LCP ≤ 2,0 s**; Native: **Cold start ≤ 2,5 s** | [ ] Lighthouse CI | [ ] Macrobenchmark | |
| **8** | **Conformità Design Tokens** | Nessun valore di colore/spacing hardcoded | **100%** conforme a `tokens.json` | [ ] Audit codice | [ ] Audit codice | |
| **9** | **Localizzazione (i18n)** | Nessuna stringa testuale hardcoded nel codice | **Zero** stringhe non mappate su `it.json` | [ ] Grep audit | [ ] Grep audit | |
| **10**| **Autonomia Offline** | Funzionalità complete senza connettività | **100%** delle funzioni locali operative | [ ] Service Worker cache | [ ] Room SQLite locale | |

---

## Procedura di Valutazione per gli Sviluppatori

1. **Verifica Interoperabilità File**:
   - Creare un kit con 3 layer su **OpenKitMaker-Web** ed esportare `kit_web_test.zip`.
   - Caricare `kit_web_test.zip` su **OpenKitMaker-Native**: verificare che tutti i layer e colori siano riprodotti esattamente.
   - Creare un kit su **OpenKitMaker-Native**, esportare `kit_native_test.zip` e ricaricarlo su **OpenKitMaker-Web**.
2. **Ispezione Architetturale**:
   - Verificare che il codice sorgente segua la modularizzazione 1:1 con il Contratto di Parità.
   - Ispezionare la sostituzione del vecchio renderer software in CPU con Google Filament in `native/`.
