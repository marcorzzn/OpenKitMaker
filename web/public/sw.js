// Implements: SCR-HOME / FUN-OFFLINE-MODE
/**
 * OpenKitMaker-Web Service Worker (Workbox Architecture)
 * Precache & Runtime Caching per funzionamento 100% Offline (FUN-OFFLINE-MODE / TEST-PAR-07).
 */

const CACHE_VERSION = 'openkitmaker-web-v1.0.0';
const PRECACHE_NAME = `${CACHE_VERSION}-precache`;
const RUNTIME_CACHE_NAME = `${CACHE_VERSION}-runtime`;
const FONTS_CACHE_NAME = `${CACHE_VERSION}-fonts`;
const ASSETS_3D_CACHE_NAME = `${CACHE_VERSION}-3d-ktx2-draco`;

const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/manifest.webmanifest',
  '/icon.svg',
];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(PRECACHE_NAME).then((cache) => {
      return cache.addAll(PRECACHE_URLS).catch(() => {
        // Ignore individual dev server asset miss
      });
    })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) =>
        Promise.all(
          cacheNames
            .filter(
              (name) =>
                name.startsWith('openkitmaker-web-') &&
                name !== PRECACHE_NAME &&
                name !== RUNTIME_CACHE_NAME &&
                name !== FONTS_CACHE_NAME &&
                name !== ASSETS_3D_CACHE_NAME
            )
            .map((name) => caches.delete(name))
        )
      )
      .then(() => self.clients.claim())
  );
});

// Workbox-aligned StaleWhileRevalidate & CacheFirst strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  // Strategy 1: CacheFirst for Google Fonts & Font binaries
  if (
    url.origin === 'https://fonts.googleapis.com' ||
    url.origin === 'https://fonts.gstatic.com'
  ) {
    event.respondWith(
      caches.open(FONTS_CACHE_NAME).then(async (cache) => {
        const cached = await cache.match(request);
        if (cached) return cached;
        try {
          const response = await fetch(request);
          if (response && (response.status === 200 || response.status === 0)) {
            cache.put(request, response.clone());
          }
          return response;
        } catch {
          return cached || new Response('', { status: 408 });
        }
      })
    );
    return;
  }

  // Strategy 2: CacheFirst for KTX2 / Basis Universal / Draco WASM / GLB 3D Assets
  if (
    url.pathname.endsWith('.ktx2') ||
    url.pathname.endsWith('.glb') ||
    url.pathname.endsWith('.wasm')
  ) {
    event.respondWith(
      caches.open(ASSETS_3D_CACHE_NAME).then(async (cache) => {
        const cached = await cache.match(request);
        if (cached) return cached;
        const response = await fetch(request);
        if (response && response.status === 200) {
          cache.put(request, response.clone());
        }
        return response;
      })
    );
    return;
  }

  // Strategy 3: NetworkFirst with Cache Fallback for navigation (SPA shell)
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone();
          caches.open(PRECACHE_NAME).then((cache) => cache.put(request, copy));
          return response;
        })
        .catch(async () => {
          const cache = await caches.open(PRECACHE_NAME);
          return (
            (await cache.match(request)) ||
            (await cache.match('/index.html')) ||
            (await cache.match('/'))
          );
        })
    );
    return;
  }

  // Strategy 4: StaleWhileRevalidate for JS/CSS chunks and same-origin assets
  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.open(RUNTIME_CACHE_NAME).then(async (cache) => {
        const cached = await cache.match(request);
        const networkPromise = fetch(request)
          .then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              cache.put(request, networkResponse.clone());
            }
            return networkResponse;
          })
          .catch(() => cached);

        return cached || networkPromise;
      })
    );
  }
});

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
