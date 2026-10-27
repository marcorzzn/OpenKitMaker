# OpenKitMaker — Formato di Esportazione e Interoperabilità (Specifica di Parità)

Documento di specifica per il formato di esportazione di **OpenKitMaker**.
L'obiettivo è la **perfetta interoperabilità bidirezionale**: qualsiasi kit esportato dall'applicazione Web deve essere apribile, modificabile e ri-esportabile dall'applicazione nativa Android, e viceversa, senza alcuna perdita di informazione.

---

## 1. Struttura dell'Archivio ZIP (`kit_<id>.zip`)

Quando l'utente richiede l'esportazione completa del kit (`FUN-EXPORT-ZIP`), viene generato un archivio ZIP standard privo di compressione protetta da password, strutturato come segue:

```text
kit_<id>.zip
│
├── jersey.png             # Texture 2048×2048 ad alta definizione per la Maglia
├── shorts.png             # Texture 2048×2048 ad alta definizione per i Pantaloncini
├── socks.png              # Texture 2048×2048 ad alta definizione per i Calzettoni
├── preview.png            # Anteprima 2D Full Kit / Minikit (512×512)
├── manifest.json          # Metadati, configurazione completa dei 3 capi e checksum SHA-256
└── texture.png            # (Retrocompatibilità con v1.0: duplicato di jersey.png)
```

### Esportazione Singolo Capo (`FUN-EXPORT-SINGLE`)
L'utente può esportare individualmente la texture del capo attivo correntemente visualizzato nell'editor:
- `jersey_<id>_2048.png`: esportazione texture della sola maglia (2048×2048 RGBA).
- `shorts_<id>_2048.png`: esportazione texture dei soli pantaloncini (2048×2048 RGBA).
- `socks_<id>_2048.png`: esportazione texture dei soli calzettoni (2048×2048 RGBA).

---

## 2. Requisiti delle Immagini

### `jersey.png`, `shorts.png`, `socks.png`
- **Risoluzione**: Esattamente **2048 × 2048 pixel**.
- **Canali**: 32-bit RGBA (8 bit per canale).
- **Spazio colore**: sRGB standard.
- **Formato di compressione**: PNG lossless, compressione Deflate.

### `preview.png`
- **Risoluzione**: Esattamente **512 × 512 pixel**.
- **Canali**: 32-bit RGBA con sfondo neutro studio card.
- **Contenuto**: Composizione visiva del Full Kit (Maglia + Pantaloncini + Calzettoni).

---

## 3. Schema JSON Rigido di `manifest.json`

Il file `manifest.json` contiene la definizione del kit, i parametri di rigenerazione grafica e i checksum crittografici dei file inclusi.

```json
{
  "$schema": "https://openkitmaker.dev/schemas/kit-manifest-v1.json",
  "version": "1.0.0",
  "kitId": "a2c5b364-7f12-401d-91b6-681b4df12a9e",
  "name": "Milano Home 2026",
  "category": "HOME",
  "exportTimestamp": "2026-10-26T20:00:00Z",
  "generator": {
    "platform": "OpenKitMaker-Web",
    "version": "1.0.0",
    "buildNumber": "104"
  },
  "dimensions": {
    "width": 2048,
    "height": 2048
  },
  "checksums": {
    "textureSha256": "3B84...A1",
    "previewSha256": "C120...5F"
  },
  "kitData": {
    "templateId": "tpl_classic_stripes_01",
    "layers": [
      {
        "id": "lay_base_01",
        "name": "Colore Base",
        "type": "BASE",
        "visible": true,
        "opacity": 1.0,
        "zIndex": 0,
        "color": "#001F3F",
        "blendMode": "NORMAL",
        "properties": {}
      },
      {
        "id": "lay_stripes_02",
        "name": "Strisce Verticali",
        "type": "PATTERN",
        "visible": true,
        "opacity": 1.0,
        "zIndex": 1,
        "color": "#000000",
        "blendMode": "NORMAL",
        "properties": {
          "stripeCount": 7,
          "widthRatio": 0.5
        }
      }
    ]
  }
}
```

---

## 4. Test di Conformità e Interoperabilità

Durante i test di parità (`TEST-PAR-05`), uno script di test automatico deve verificare:
1. Validità dello ZIP e presenza di tutti e tre i file alla radice dell'archivio.
2. Dimensione esatta di `texture.png` (2048×2048) e `preview.png` (512×512).
3. Corrispondenza degli hash SHA-256 calcolati sui file estratti con quelli indicati in `manifest.json`.
4. Ricaricamento dei layer in `kitData` da parte dell'applicazione complementare (Web legge ZIP di Android, Android legge ZIP di Web) con rigenerazione della texture identica al pixel entro un delta visivo DeltaE < 1.0.
