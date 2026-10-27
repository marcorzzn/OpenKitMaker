// Implements: SCR-EXPORT, SCR-SAVED / FUN-EXPORT-PNG, FUN-EXPORT-ZIP, FUN-EXPORT-SINGLE
import { zipSync, unzipSync, strToU8, strFromU8 } from 'fflate';
import type { Kit, KitManifestV1, GarmentType } from '../types/kit';
import {
  compositeJersey2048,
  compositeShorts2048,
  compositeSocks2048,
  compositeGarment2048,
  renderFullKitMockup512,
  TEXTURE_SIZE,
} from './compositor2d';
import { generateUuidV4, ensureKitGarments } from '../data/templates';

async function canvasToUint8ArrayPng(canvas: HTMLCanvasElement): Promise<Uint8Array> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      async (blob) => {
        if (!blob) {
          reject(new Error('Impossibile serializzare il canvas PNG.'));
          return;
        }
        const buf = await blob.arrayBuffer();
        resolve(new Uint8Array(buf));
      },
      'image/png',
      1.0
    );
  });
}

export async function computeSha256Hex(bytes: Uint8Array): Promise<string> {
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const hashBuffer = await crypto.subtle.digest(
      'SHA-256',
      bytes as unknown as BufferSource
    );
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
  }
  // Deterministic fallback hash if subtle crypto is unavailable in non-secure context
  let h1 = 0xdeadbeef;
  let h2 = 0x41c6ce57;
  for (let i = 0; i < bytes.length; i++) {
    const ch = bytes[i];
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (
    (h1 >>> 0).toString(16).padStart(8, '0') +
    (h2 >>> 0).toString(16).padStart(8, '0')
  ).repeat(4);
}

export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  setTimeout(() => URL.revokeObjectURL(url), 3000);
}

/**
 * FUN-EXPORT-SINGLE: Generates and downloads lossless 2048x2048 RGBA 32-bit PNG texture for a single garment.
 */
export async function exportGarmentPng2048(
  kit: Kit,
  garmentType: GarmentType,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string; filename: string }> {
  onProgress?.(15);
  const workCanvas = document.createElement('canvas');
  workCanvas.width = TEXTURE_SIZE;
  workCanvas.height = TEXTURE_SIZE;

  compositeGarment2048(workCanvas, kit, garmentType, { showUvGuides: false });
  onProgress?.(60);

  const pngBytes = await canvasToUint8ArrayPng(workCanvas);
  const sha256 = await computeSha256Hex(pngBytes);
  onProgress?.(90);

  const safeSlug = kit.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
  const prefix = garmentType.toLowerCase();
  const filename = `${prefix}_${safeSlug || kit.id.slice(0, 8)}_2048.png`;
  const blob = new Blob([pngBytes as unknown as BlobPart], { type: 'image/png' });
  triggerBlobDownload(blob, filename);
  onProgress?.(100);

  return { bytes: pngBytes, sha256, filename };
}

/**
 * FUN-EXPORT-PNG: Generates and downloads lossless 2048x2048 RGBA 32-bit PNG texture (Jersey default).
 */
export async function exportKitPng2048(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string }> {
  const res = await exportGarmentPng2048(kit, 'JERSEY', onProgress);
  return { bytes: res.bytes, sha256: res.sha256 };
}

/**
 * FUN-EXPORT-ZIP & TEST-PAR-05: Generates interoperable kit_<id>.zip containing:
 * - jersey.png (2048x2048 RGBA lossless)
 * - shorts.png (2048x2048 RGBA lossless)
 * - socks.png (2048x2048 RGBA lossless)
 * - preview.png (512x512 RGBA full kit mockup)
 * - manifest.json (conforming strictly to schema kit-manifest-v1.json)
 * - texture.png (retrocompatibility duplicate of jersey.png)
 */
export async function exportKitInteroperableZip(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ manifest: KitManifestV1; zipSizeBytes: number }> {
  onProgress?.(5);

  const garments = ensureKitGarments(kit);

  // 1. Jersey 2048
  const jerseyCanvas = document.createElement('canvas');
  jerseyCanvas.width = TEXTURE_SIZE;
  jerseyCanvas.height = TEXTURE_SIZE;
  compositeJersey2048(jerseyCanvas, kit, { showUvGuides: false });
  onProgress?.(20);

  // 2. Shorts 2048
  const shortsCanvas = document.createElement('canvas');
  shortsCanvas.width = TEXTURE_SIZE;
  shortsCanvas.height = TEXTURE_SIZE;
  compositeShorts2048(shortsCanvas, kit, { showUvGuides: false });
  onProgress?.(35);

  // 3. Socks 2048
  const socksCanvas = document.createElement('canvas');
  socksCanvas.width = TEXTURE_SIZE;
  socksCanvas.height = TEXTURE_SIZE;
  compositeSocks2048(socksCanvas, kit, { showUvGuides: false });
  onProgress?.(50);

  // 4. Preview 512 (Full Kit Mockup)
  const prevCanvas = document.createElement('canvas');
  prevCanvas.width = 512;
  prevCanvas.height = 512;
  renderFullKitMockup512(prevCanvas, kit);
  onProgress?.(65);

  // Convert canvases to PNG bytes
  const [jerseyBytes, shortsBytes, socksBytes, previewBytes] = await Promise.all([
    canvasToUint8ArrayPng(jerseyCanvas),
    canvasToUint8ArrayPng(shortsCanvas),
    canvasToUint8ArrayPng(socksCanvas),
    canvasToUint8ArrayPng(prevCanvas),
  ]);
  onProgress?.(80);

  // Compute checksums
  const [jerseySha256, shortsSha256, socksSha256, previewSha256] = await Promise.all([
    computeSha256Hex(jerseyBytes),
    computeSha256Hex(shortsBytes),
    computeSha256Hex(socksBytes),
    computeSha256Hex(previewBytes),
  ]);

  const cleanLayers = JSON.parse(JSON.stringify(kit.layers));
  const cleanGarments = JSON.parse(JSON.stringify(garments));
  const cleanMetadata = JSON.parse(JSON.stringify(kit.metadata));

  const manifest: KitManifestV1 = {
    $schema: 'https://openkitmaker.dev/schemas/kit-manifest-v1.json',
    version: '1.0.0',
    kitId: kit.id,
    name: kit.name,
    category: kit.category,
    exportTimestamp: new Date().toISOString(),
    generator: {
      platform: 'OpenKitMaker-Web',
      version: '1.1.0',
      buildNumber: '110',
    },
    dimensions: {
      width: 2048,
      height: 2048,
    },
    checksums: {
      textureSha256: jerseySha256,
      previewSha256,
      jerseySha256,
      shortsSha256,
      socksSha256,
    },
    kitData: {
      templateId: kit.templateId,
      layers: cleanLayers,
      garments: cleanGarments,
      metadata: cleanMetadata,
    },
  };

  const manifestJsonString = JSON.stringify(manifest, null, 2);
  const manifestBytes = strToU8(manifestJsonString);

  onProgress?.(90);
  const zipped = zipSync(
    {
      'jersey.png': jerseyBytes,
      'shorts.png': shortsBytes,
      'socks.png': socksBytes,
      'preview.png': previewBytes,
      'manifest.json': manifestBytes,
      'texture.png': jerseyBytes,
    },
    { level: 6 }
  );

  const zipBlob = new Blob([zipped as unknown as BlobPart], { type: 'application/zip' });
  triggerBlobDownload(zipBlob, `kit_${kit.id}.zip`);
  onProgress?.(100);

  return {
    manifest,
    zipSizeBytes: zipped.byteLength,
  };
}

/**
 * TEST-PAR-06: Cross-Platform ZIP Import (Web <-> Android Native).
 * Validates manifest.json schema and SHA-256 checksums, returning a hydrated Kit entity.
 */
export async function importKitFromZip(file: File): Promise<{
  kit: Kit;
  manifest: KitManifestV1;
  checksumVerified: boolean;
}> {
  const arrayBuffer = await file.arrayBuffer();
  const unzipped = unzipSync(new Uint8Array(arrayBuffer));

  const manifestEntry = unzipped['manifest.json'];
  if (!manifestEntry) {
    throw new Error('File manifest.json mancante nell\'archivio ZIP.');
  }

  const manifestText = strFromU8(manifestEntry);
  const manifest = JSON.parse(manifestText) as KitManifestV1;

  if (
    !manifest.kitId ||
    !manifest.kitData ||
    (!Array.isArray(manifest.kitData.layers) && !manifest.kitData.garments) ||
    manifest.dimensions?.width !== 2048 ||
    manifest.dimensions?.height !== 2048
  ) {
    throw new Error('Schema manifest.json non valido o dimensioni non conformi a 2048x2048.');
  }

  let checksumVerified = false;
  const jerseyEntry = unzipped['jersey.png'] || unzipped['texture.png'];
  if (jerseyEntry && (manifest.checksums?.jerseySha256 || manifest.checksums?.textureSha256)) {
    const targetSha = manifest.checksums.jerseySha256 || manifest.checksums.textureSha256;
    const computedTexSha = await computeSha256Hex(jerseyEntry);
    checksumVerified = computedTexSha.toLowerCase() === targetSha.toLowerCase();
  }

  const now = new Date().toISOString();
  const layers = manifest.kitData.layers || manifest.kitData.garments?.jersey.layers || [];
  const importedKit: Kit = {
    id: manifest.kitId || generateUuidV4(),
    name: manifest.name || 'Kit Importato',
    category: manifest.category || 'HOME',
    createdAt: manifest.exportTimestamp || now,
    updatedAt: now,
    templateId: manifest.kitData.templateId || 'tpl-home-vertical-classic',
    garments: manifest.kitData.garments || undefined,
    layers,
    metadata: manifest.kitData.metadata || {
      author: manifest.generator?.platform || 'OpenKitMaker-Native',
      version: manifest.version || '1.1.0',
      tags: [manifest.category || 'HOME', 'Importato ZIP'],
      finishType: 'PRO_VAPOR',
      lightingPreset: 'studio',
    },
  };

  ensureKitGarments(importedKit);

  // Generate 512x512 Full Kit thumbnail
  const prevCanvas = document.createElement('canvas');
  prevCanvas.width = 512;
  prevCanvas.height = 512;
  renderFullKitMockup512(prevCanvas, importedKit);
  importedKit.metadata.thumbnailDataUrl = prevCanvas.toDataURL('image/png');

  return {
    kit: importedKit,
    manifest,
    checksumVerified,
  };
}
