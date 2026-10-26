# OpenKitMaker — Guida Ufficiale alla Pubblicazione e Deployment

> **Prodotto:** OpenKitMaker  
> **Autore & Proprietario:** [Marco Razzano](https://github.com/marcorzzn) (`@marcorzzn`)  
> **Target:** Web (PWA su Vercel/Netlify) & Mobile (Google Play Store)

---

Questa guida documenta la procedura completa per distribuire e monetizzare in autonomia **OpenKitMaker** su scala globale.

---

## PARTE 1: Pubblicazione Web (PWA su Vercel / Netlify / Cloudflare Pages)

La directory [web/](web/) contiene un'applicazione Svelte 5 / Vite 6 pura, senza server backend, pronta per essere distribuita su qualsiasi CDN globale a costo zero o scalabile.

### 1.1 Deployment su Vercel (Consigliato)

1. **Tramite GitHub (Automatico)**:
   - Accedi a [vercel.com](https://vercel.com/) e collega il tuo account GitHub (`marcorzzn`).
   - Clicca **"Add New Project"** e seleziona il repository `marcorzzn/OpenKitMaker`.
   - Nelle impostazioni del progetto:
     - **Root Directory**: `web`
     - **Framework Preset**: `Vite`
     - **Build Command**: `npm run build`
     - **Output Directory**: `dist`
   - Clicca **"Deploy"**. Vercel assegnerà un URL HTTPS pubblico (es. `openkitmaker.vercel.app`) con certificato SSL automatico (necessario per il Service Worker PWA).

2. **Tramite Vercel CLI**:
   ```bash
   cd web
   npm install -g vercel
   vercel --prod
   ```

### 1.2 Deployment su Netlify

Crea o verifica il file `web/netlify.toml`:
```toml
[build]
  base = "web"
  publish = "dist"
  command = "npm run build"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```
- Collega il repository a [Netlify](https://www.netlify.com/) e avvia il deploy continuo su branch `main`.

### 1.3 Verifica PWA e Installazione
Una volta online con protocollo `https://`:
1. Apri Chrome o Edge su Desktop o Smartphone.
2. Verifica che nella barra degli indirizzi compaia l'icona **"Installa OpenKitMaker"**.
3. Clicca Installa: l'app si aprirà in una finestra nativa standalone senza barra del browser.
4. Vai in modalità aereo: l'app continuerà a funzionare al 100% grazie al Service Worker [public/sw.js](web/public/sw.js).

---

## PARTE 2: Pubblicazione Android su Google Play Store

L'applicazione nativa Android ha identificativo ufficiale:
- **Application ID**: `com.marcorazzano.openkitmaker`
- **Versione**: 1.0.0 (versionCode 100)

### 2.1 Generazione del Keystore di Firma Ufficiale

Per caricare l'app sul Google Play Store è necessario un certificato crittografico privato. Genera il tuo Keystore personale con `keytool`:

```bash
keytool -genkeypair -v \
  -keystore openkitmaker-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias openkitmaker \
  -dname "CN=Marco Razzano, OU=OpenKitMaker, O=Marco Razzano, L=Milano, ST=Lombardia, C=IT"
```

> ⚠️ **IMPORTANTE**: Conserva il file `openkitmaker-release-key.jks` e le relative password in un luogo sicuro (es. password manager). Non committare MAI il file `.jks` su GitHub!

### 2.2 Configurazione della Firma in Gradle

In `native/app/build.gradle.kts` o tramite variabili d'ambiente (per CI):
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../openkitmaker-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "TuaPasswordStore"
            keyAlias = System.getenv("KEY_ALIAS") ?: "openkitmaker"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "TuaPasswordKey"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### 2.3 Compilazione dell'Android App Bundle (AAB)

Google Play richiede il formato `.aab` (Android App Bundle) per la pubblicazione sullo store:

```bash
cd native
./gradlew :app:bundleRelease
```
L'artefatto finale verrà generato in:
`native/app/build/outputs/bundle/release/app-release.aab`

### 2.4 Caricamento su Google Play Console

1. Accedi a [Google Play Console](https://play.google.com/console).
2. Crea una nuova applicazione:
   - **Nome**: `OpenKitMaker`
   - **Lingua predefinita**: Italiano (it-IT)
   - **Tipo**: App
   - **Prezzo**: Gratuita (o a pagamento)
3. **Scheda dello Store**:
   - Icona app: 512×512 PNG 32-bit.
   - Grafica delle funzioni: 1024×500 PNG/JPEG.
   - Screenshot telefono: almeno 4 screenshot 1080×1920 (catturati dall'Editor, Viewport 3D, Catalogo e Centro Esportazione).
4. **Traccia di Test Interno**:
   - Crea una nuova release e carica il file `app-release.aab`.
   - Aggiungi la tua email come tester e installa l'app direttamente dal Play Store.

---

## PARTE 3: Alternativa Rapida TWA (Trusted Web Activity)

Se desideri pubblicare sul Play Store la versione Web Svelte 5 con il minimo overhead di manutenzione, puoi utilizzare il tool ufficiale di Google **Bubblewrap**:

```bash
# Installa Bubblewrap CLI
npm install -g @bubblewrap/cli

# Inizializza il progetto TWA dal tuo URL di produzione
bubblewrap init --manifest=https://openkitmaker.vercel.app/manifest.webmanifest

# Compila l'APK / AAB per il Play Store
bubblewrap build
```
Questo genererà un AAB pronto per il Play Store che carica la tua PWA a schermo intero con prestazioni native!
