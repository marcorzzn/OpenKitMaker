// Implements: SCR-EXPORT, SCR-SAVED / FUN-EXPORT-PNG, FUN-EXPORT-ZIP
import { zipSync, unzipSync, strToU8, strFromU8 } from 'fflate';
import type { Kit, KitManifestV1 } from '../types/kit';
import {
  compositeKit2048,
  renderMinikitPreview512,
  TEXTURE_SIZE,
} from './compositor2d';
import { generateUuidV4 } from '../data/templates';

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
 * FUN-EXPORT-PNG: Generates and downloads lossless 2048x2048 RGBA 32-bit PNG texture.
 */
export async function exportKitPng2048(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string }> {
  onProgress?.(15);
  const workCanvas = document.createElement('canvas');
  workCanvas.width = TEXTURE_SIZE;
  workCanvas.height = TEXTURE_SIZE;

  compositeKit2048(workCanvas, kit, { showUvGuides: false });
  onProgress?.(60);

  const pngBytes = await canvasToUint8ArrayPng(workCanvas);
  const sha256 = await computeSha256Hex(pngBytes);
  onProgress?.(90);

  const safeSlug = kit.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
  const blob = new Blob([pngBytes as unknown as BlobPart], { type: 'image/png' });
  triggerBlobDownload(blob, `texture_${safeSlug || kit.id.slice(0, 8)}_2048.png`);
  onProgress?.(100);

  return { bytes: pngBytes, sha256 };
}

/**
 * FUN-EXPORT-ZIP & TEST-PAR-05: Generates interoperable kit_<id>.zip containing:
 * - texture.png (2048x2048 RGBA lossless)
 * - preview.png (512x512 RGBA minikit)
 * - manifest.json (conforming strictly to schema kit-manifest-v1.json)
 */
export async function exportKitInteroperableZip(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ manifest: KitManifestV1; zipSizeBytes: number }> {
  onProgress?.(10);
  const texCanvas = document.createElement('canvas');
  texCanvas.width = TEXTURE_SIZE;
  texCanvas.height = TEXTURE_SIZE;
  compositeKit2048(texCanvas, kit, { showUvGuides: false });

  onProgress?.(35);
  const prevCanvas = document.createElement('canvas');
  prevCanvas.width = 512;
  prevCanvas.height = 512;
  renderMinikitPreview512(prevCanvas, texCanvas, kit);

  onProgress?.(55);
  const [textureBytes, previewBytes] = await Promise.all([
    canvasToUint8ArrayPng(texCanvas),
    canvasToUint8ArrayPng(prevCanvas),
  ]);

  onProgress?.(75);
  const [textureSha256, previewSha256] = await Promise.all([
    computeSha256Hex(textureBytes),
    computeSha256Hex(previewBytes),
  ]);

  const cleanLayers = JSON.parse(JSON.stringify(kit.layers));
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
      version: '1.0.0',
      buildNumber: '100',
    },
    dimensions: {
      width: 2048,
      height: 2048,
    },
    checksums: {
      textureSha256,
      previewSha256,
    },
    kitData: {
      templateId: kit.templateId,
      layers: cleanLayers,
      metadata: cleanMetadata,
    },
  };

  const manifestJsonString = JSON.stringify(manifest, null, 2);
  const manifestBytes = strToU8(manifestJsonString);

  onProgress?.(90);
  const zipped = zipSync(
    {
      'texture.png': textureBytes,
      'preview.png': previewBytes,
      'manifest.json': manifestBytes,
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
    !Array.isArray(manifest.kitData.layers) ||
    manifest.dimensions?.width !== 2048 ||
    manifest.dimensions?.height !== 2048
  ) {
    throw new Error('Schema manifest.json non valido o dimensioni non conformi a 2048x2048.');
  }

  let checksumVerified = false;
  const textureEntry = unzipped['texture.png'];
  if (textureEntry && manifest.checksums?.textureSha256) {
    const computedTexSha = await computeSha256Hex(textureEntry);
    checksumVerified =
      computedTexSha.toLowerCase() === manifest.checksums.textureSha256.toLowerCase();
  }

  const now = new Date().toISOString();
  const importedKit: Kit = {
    id: manifest.kitId || generateUuidV4(),
    name: manifest.name || 'Kit Importato',
    category: manifest.category || 'HOME',
    createdAt: manifest.exportTimestamp || now,
    updatedAt: now,
    templateId: manifest.kitData.templateId || 'tpl-home-vertical-classic',
    layers: manifest.kitData.layers,
    metadata: manifest.kitData.metadata || {
      author: manifest.generator?.platform || 'OpenKitMaker-Native',
      version: manifest.version || '1.0.0',
      tags: [manifest.category || 'HOME', 'Importato ZIP'],
      finishType: 'PRO_VAPOR',
      lightingPreset: 'studio',
    },
  };

  // Re-generate 512x512 thumbnail so SCR-HOME and SCR-SAVED have an immediate preview
  const texCanvas = document.createElement('canvas');
  texCanvas.width = TEXTURE_SIZE;
  texCanvas.height = TEXTURE_SIZE;
  compositeKit2048(texCanvas, importedKit);

  const prevCanvas = document.createElement('canvas');
  prevCanvas.width = 512;
  prevCanvas.height = 512;
  importedKit.metadata.thumbnailDataUrl = renderMinikitPreview512(
    prevCanvas,
    texCanvas,
    importedKit
  );

  return {
    kit: importedKit,
    manifest,
    checksumVerified,
  };
}
