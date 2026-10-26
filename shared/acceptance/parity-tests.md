# OpenKitMaker — Test di Accettazione e Parità Funzionale (Specifica di Parità)

Documento di specifica per i test di accettazione automatizzati e manuali per **OpenKitMaker**.
Entrambi i progetti (Web e Native) devono eseguire e superare questi scenari identici. I risultati vengono registrati nel file JSON `parity-report.json` con lo schema:
```json
{
  "testId": "TEST-PAR-01",
  "status": "PASSED",
  "durationMs": 420,
  "notes": "Template caricato correttamente in memoria e texture generata."
}
```

---

## Elenco Scenari di Test di Parità

| ID Test | Nome Test | Descrizione e Condizioni | Criterio di Successo (Soglia) |
|---|---|---|---|
| **`TEST-PAR-01`** | Template Selection & Init | Selezione di un template da `SCR-CATALOG` e caricamento in `SCR-EDITOR` | L'editor si apre entro 1,5s con tutti i layer predefiniti clonati e visibili |
| **`TEST-PAR-02`** | Layer Color Mutex & 2D Update | Modifica colore primario del layer corpo da `#000000` a `#FF0000` tramite color picker | Il canvas 2D riflette il colore entro ≤ 16,6 ms (1 frame) senza glitch |
| **`TEST-PAR-03`** | 3D Viewport Orbit & Framerate | Rotazione continua a 360° per 10 secondi nel viewport 3D | Web: 60 FPS (≥ 55 FPS medi); Android: 60 FPS stabili, frame time medio ≤ 16,6 ms |
| **`TEST-PAR-04`** | Local Persistence & Reload | Salvataggio di una bozza denominata *"TestKit 2026"*, chiusura e riapertura da `SCR-SAVED` | Il kit viene ricaricato esattamente con gli stessi layer, colori e opacità |
| **`TEST-PAR-05`** | Interoperable Export ZIP | Esportazione del kit in formato `kit_<id>.zip` | Lo ZIP contiene `texture.png` (2048×2048), `preview.png` (512×512) e `manifest.json` valido |
| **`TEST-PAR-06`** | Cross-Platform ZIP Import | Importazione nel Ramo Native di uno ZIP generato dal Ramo Web (e viceversa) | La texture ricomposta differisce con DeltaE visivo medio < 1.0; zero errori di schema |
| **`TEST-PAR-07`** | Offline Autonomy | Disconnessione totale dalla rete (Airplane Mode / DevTools Offline) e creazione kit completo | Nessun errore bloccante; salvataggio ed esportazione completati al 100% |

---

## Metodologia di Esecuzione

1. **Ramo Web**:
   - Eseguito tramite suite E2E **Playwright** (`web/tests/e2e/parity.spec.ts`).
   - Monitoraggio performance tramite **Lighthouse CI** e performance observer API.
2. **Ramo Native**:
   - Eseguito tramite test UI **Compose Test** (`native/app/src/androidTest/.../ParityTestSuite.kt`).
   - Profiling frame timing tramite **Jetpack Macrobenchmark** (`dumpsys gfxinfo`).
