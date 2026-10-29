// Implements: SCR-EXPORT, SCR-SAVED / FUN-EXPORT-PNG, FUN-EXPORT-ZIP, FUN-EXPORT-SINGLE
import { zipSync, unzipSync, strToU8, strFromU8 } from 'fflate';
import * as THREE from 'three';
import type { Kit, KitManifestV1, GarmentType, FabricType, ExportResolution } from '../types/kit';
import {
  compositeJersey2048,
  compositeShorts2048,
  compositeSocks2048,
  compositeGarment2048,
  renderFullKitMockup512,
  TEXTURE_SIZE,
} from './compositor2d';
import { generateUuidV4, ensureKitGarments } from '../data/templates';

const fabricPbrValues: Record<FabricType, { roughness: number; metalness: number; label: string }> = {
  MESH: { roughness: 0.78, metalness: 0.02, label: 'Mesh Traspirante' },
  POLYESTER: { roughness: 0.22, metalness: 0.12, label: 'Poliestere Lucido' },
  COTTON: { roughness: 0.90, metalness: 0.01, label: 'Cotone Anni 90' },
  JACQUARD: { roughness: 0.48, metalness: 0.06, label: 'Jacquard Nobile' },
};

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
 * Genera un canvas contenente la normal map procedurale del tessuto (tangent space RGB).
 */
export function generateFabricNormalCanvas(fabric: FabricType = 'MESH', size = 1024): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d')!;
  const imgData = ctx.createImageData(size, size);
  const data = imgData.data;

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const idx = (y * size + x) * 4;
      let nx = 128;
      let ny = 128;
      let nz = 240;

      switch (fabric) {
        case 'MESH': {
          const isPore = ((x % 16 === 0 && y % 16 === 0) || (x % 16 === 8 && y % 16 === 8));
          const waveX = Math.sin((x / 8) * Math.PI * 2) * 14;
          const waveY = Math.cos((y / 8) * Math.PI * 2) * 14;
          nx = 128 + waveX;
          ny = 128 + waveY;
          if (isPore) {
            nx = 96;
            ny = 96;
            nz = 180;
          }
          break;
        }
        case 'POLYESTER': {
          const thread = Math.sin((x / 4) * Math.PI * 2) * 8;
          nx = 128 + thread;
          ny = 128;
          nz = 252;
          break;
        }
        case 'COTTON': {
          const coarseX = Math.sin((x / 6) * Math.PI * 2) * 20;
          const coarseY = Math.cos((y / 6) * Math.PI * 2) * 20;
          const fiberNoise = ((x * 17 + y * 31) % 19) - 9;
          nx = 128 + coarseX + fiberNoise;
          ny = 128 + coarseY + fiberNoise;
          nz = 215;
          break;
        }
        case 'JACQUARD': {
          const diag1 = ((x + y) % 32) < 8 ? 32 : -12;
          const diag2 = ((x - y + 1024) % 32) < 8 ? 32 : -12;
          nx = 128 + diag1;
          ny = 128 + diag2;
          nz = 210;
          break;
        }
      }

      data[idx] = Math.min(255, Math.max(0, Math.round(nx)));
      data[idx + 1] = Math.min(255, Math.max(0, Math.round(ny)));
      data[idx + 2] = Math.min(255, Math.max(0, Math.round(nz)));
      data[idx + 3] = 255;
    }
  }
  ctx.putImageData(imgData, 0, 0);
  return canvas;
}

/**
 * Genera una mappa PBR monocromatica a scala di grigi per Roughness o Metalness.
 */
export function generateMonochromeMapCanvas(
  factor: number,
  size = 1024,
  noiseIntensity = 0.03
): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d')!;
  const imgData = ctx.createImageData(size, size);
  const data = imgData.data;

  const baseByte = Math.round(Math.min(1, Math.max(0, factor)) * 255);

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const idx = (y * size + x) * 4;
      const noise = ((x * 19 + y * 37) % 21 - 10) * noiseIntensity * (255 / 10);
      const val = Math.min(255, Math.max(0, Math.round(baseByte + noise)));
      data[idx] = val;
      data[idx + 1] = val;
      data[idx + 2] = val;
      data[idx + 3] = 255;
    }
  }
  ctx.putImageData(imgData, 0, 0);
  return canvas;
}

/**
 * FUN-EXPORT-SINGLE & STEP 4: Generates and downloads lossless 2048x2048 (2K) or 4096x4096 (4K) PNG texture.
 */
export async function exportGarmentPng(
  kit: Kit,
  garmentType: GarmentType,
  resolution: ExportResolution = 2048,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string; filename: string }> {
  onProgress?.(15);
  const baseCanvas = document.createElement('canvas');
  baseCanvas.width = TEXTURE_SIZE;
  baseCanvas.height = TEXTURE_SIZE;
  compositeGarment2048(baseCanvas, kit, garmentType, { showUvGuides: false });
  onProgress?.(50);

  let outputCanvas = baseCanvas;
  if (resolution === 4096) {
    const canvas4k = document.createElement('canvas');
    canvas4k.width = 4096;
    canvas4k.height = 4096;
    const ctx4k = canvas4k.getContext('2d')!;
    ctx4k.imageSmoothingEnabled = true;
    ctx4k.imageSmoothingQuality = 'high';
    ctx4k.drawImage(baseCanvas, 0, 0, 4096, 4096);
    outputCanvas = canvas4k;
  }
  onProgress?.(70);

  const pngBytes = await canvasToUint8ArrayPng(outputCanvas);
  const sha256 = await computeSha256Hex(pngBytes);
  onProgress?.(90);

  const safeSlug = kit.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
  const prefix = garmentType.toLowerCase();
  const filename = `${prefix}_${safeSlug || kit.id.slice(0, 8)}_${resolution}.png`;
  const blob = new Blob([pngBytes as unknown as BlobPart], { type: 'image/png' });
  triggerBlobDownload(blob, filename);
  onProgress?.(100);

  return { bytes: pngBytes, sha256, filename };
}

export async function exportGarmentPng2048(
  kit: Kit,
  garmentType: GarmentType,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string; filename: string }> {
  return exportGarmentPng(kit, garmentType, 2048, onProgress);
}

export async function exportGarmentPng4096(
  kit: Kit,
  garmentType: GarmentType,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string; filename: string }> {
  return exportGarmentPng(kit, garmentType, 4096, onProgress);
}

/**
 * FUN-EXPORT-PNG: Generates and downloads lossless 2048x2048 RGBA 32-bit PNG texture (Jersey default).
 */
export async function exportKitPng2048(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ bytes: Uint8Array; sha256: string }> {
  const res = await exportGarmentPng(kit, 'JERSEY', 2048, onProgress);
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
 * STEP 4: Exports complete PBR Material Bundle (.ZIP) for 3D production (Blender / Unity / Unreal):
 * - albedo.png (2048x2048)
 * - albedo_jersey.png (2048x2048)
 * - albedo_shorts.png (2048x2048)
 * - albedo_socks.png (2048x2048)
 * - normal.png (1024x1024 procedural fabric normal map)
 * - roughness.png (1024x1024 greyscale roughness map)
 * - metallic.png (1024x1024 greyscale metallic map)
 * - preview.png (512x512 kit mockup)
 * - pbr_manifest.json (PBR texture map definitions)
 */
export async function exportKitPbrBundleZip(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ zipSizeBytes: number; filename: string }> {
  onProgress?.(5);

  const fabric = kit.metadata?.fabricType || 'MESH';
  const pbrInfo = fabricPbrValues[fabric] || fabricPbrValues.MESH;

  // 1. Albedo Canvases
  const jerseyCanvas = document.createElement('canvas');
  jerseyCanvas.width = TEXTURE_SIZE;
  jerseyCanvas.height = TEXTURE_SIZE;
  compositeJersey2048(jerseyCanvas, kit, { showUvGuides: false });
  onProgress?.(20);

  const shortsCanvas = document.createElement('canvas');
  shortsCanvas.width = TEXTURE_SIZE;
  shortsCanvas.height = TEXTURE_SIZE;
  compositeShorts2048(shortsCanvas, kit, { showUvGuides: false });
  onProgress?.(35);

  const socksCanvas = document.createElement('canvas');
  socksCanvas.width = TEXTURE_SIZE;
  socksCanvas.height = TEXTURE_SIZE;
  compositeSocks2048(socksCanvas, kit, { showUvGuides: false });
  onProgress?.(45);

  const prevCanvas = document.createElement('canvas');
  prevCanvas.width = 512;
  prevCanvas.height = 512;
  renderFullKitMockup512(prevCanvas, kit);
  onProgress?.(55);

  // 2. Procedural Normal, Roughness, Metallic Canvases
  const normalCanvas = generateFabricNormalCanvas(fabric, 1024);
  const roughnessCanvas = generateMonochromeMapCanvas(pbrInfo.roughness, 1024, 0.03);
  const metallicCanvas = generateMonochromeMapCanvas(pbrInfo.metalness, 1024, 0.01);
  onProgress?.(65);

  // 3. Convert all canvases to PNG Uint8Arrays
  const [
    jerseyBytes,
    shortsBytes,
    socksBytes,
    previewBytes,
    normalBytes,
    roughnessBytes,
    metallicBytes,
  ] = await Promise.all([
    canvasToUint8ArrayPng(jerseyCanvas),
    canvasToUint8ArrayPng(shortsCanvas),
    canvasToUint8ArrayPng(socksCanvas),
    canvasToUint8ArrayPng(prevCanvas),
    canvasToUint8ArrayPng(normalCanvas),
    canvasToUint8ArrayPng(roughnessCanvas),
    canvasToUint8ArrayPng(metallicCanvas),
  ]);
  onProgress?.(80);

  const pbrManifest = {
    schemaVersion: '1.0.0',
    kitId: kit.id,
    kitName: kit.name,
    category: kit.category,
    fabricType: fabric,
    fabricDescription: pbrInfo.label,
    pbrParameters: {
      roughnessFactor: pbrInfo.roughness,
      metallicFactor: pbrInfo.metalness,
      normalMapFormat: 'Tangent Space OpenGL (+Y)',
    },
    textureMaps: {
      albedo: 'albedo.png (Primary Jersey)',
      albedoJersey: 'albedo_jersey.png (2048x2048)',
      albedoShorts: 'albedo_shorts.png (2048x2048)',
      albedoSocks: 'albedo_socks.png (2048x2048)',
      normal: 'normal.png (1024x1024 Micro-Weave)',
      roughness: 'roughness.png (1024x1024 Greyscale)',
      metallic: 'metallic.png (1024x1024 Greyscale)',
      preview: 'preview.png (512x512 Mockup)',
    },
    compatibleEngines: ['Blender 4+', 'Unity URP/HDRP', 'Unreal Engine 5', 'Godot 4'],
    exportedAt: new Date().toISOString(),
  };

  const pbrManifestBytes = strToU8(JSON.stringify(pbrManifest, null, 2));
  onProgress?.(90);

  const zipped = zipSync(
    {
      'albedo.png': jerseyBytes,
      'albedo_jersey.png': jerseyBytes,
      'albedo_shorts.png': shortsBytes,
      'albedo_socks.png': socksBytes,
      'normal.png': normalBytes,
      'roughness.png': roughnessBytes,
      'metallic.png': metallicBytes,
      'preview.png': previewBytes,
      'pbr_manifest.json': pbrManifestBytes,
    },
    { level: 6 }
  );

  const safeSlug = kit.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
  const filename = `pbr_bundle_${safeSlug || kit.id.slice(0, 8)}.zip`;
  const zipBlob = new Blob([zipped as unknown as BlobPart], { type: 'application/zip' });
  triggerBlobDownload(zipBlob, filename);
  onProgress?.(100);

  return {
    zipSizeBytes: zipped.byteLength,
    filename,
  };
}

/**
 * STEP 4: Exports standalone self-contained 3D Model (.GLB) with full outfit and baked textures.
 * Ready for drag-and-drop into Blender, Unity, or Unreal Engine.
 */
export async function exportKitGlb(
  kit: Kit,
  onProgress?: (pct: number) => void
): Promise<{ blob: Blob; filename: string }> {
  onProgress?.(10);

  const [THREE, { GLTFLoader }, { GLTFExporter }] = await Promise.all([
    import('three'),
    import('three/examples/jsm/loaders/GLTFLoader.js'),
    import('three/examples/jsm/exporters/GLTFExporter.js'),
  ]);
  onProgress?.(25);

  const fabric = kit.metadata?.fabricType || 'MESH';
  const pbrInfo = fabricPbrValues[fabric] || fabricPbrValues.MESH;

  // 1. Composite Canvases
  const jerseyCanvas = document.createElement('canvas');
  jerseyCanvas.width = TEXTURE_SIZE;
  jerseyCanvas.height = TEXTURE_SIZE;
  compositeJersey2048(jerseyCanvas, kit, { showUvGuides: false });

  const shortsCanvas = document.createElement('canvas');
  shortsCanvas.width = TEXTURE_SIZE;
  shortsCanvas.height = TEXTURE_SIZE;
  compositeShorts2048(shortsCanvas, kit, { showUvGuides: false });

  const socksCanvas = document.createElement('canvas');
  socksCanvas.width = TEXTURE_SIZE;
  socksCanvas.height = TEXTURE_SIZE;
  compositeSocks2048(socksCanvas, kit, { showUvGuides: false });

  const normalCanvas = generateFabricNormalCanvas(fabric, 512);
  onProgress?.(45);

  // 2. Load Base Clean GLB Model
  const loader = new GLTFLoader();
  const gltf = await loader.loadAsync('/models/kit_full_outfit_clean.glb');
  onProgress?.(65);

  // 3. Create textures & Materials
  const jerseyTex = new THREE.CanvasTexture(jerseyCanvas);
  jerseyTex.colorSpace = THREE.SRGBColorSpace;
  jerseyTex.flipY = false;

  const shortsTex = new THREE.CanvasTexture(shortsCanvas);
  shortsTex.colorSpace = THREE.SRGBColorSpace;
  shortsTex.flipY = false;

  const socksTex = new THREE.CanvasTexture(socksCanvas);
  socksTex.colorSpace = THREE.SRGBColorSpace;
  socksTex.flipY = false;

  const normalTex = new THREE.CanvasTexture(normalCanvas);
  normalTex.wrapS = THREE.RepeatWrapping;
  normalTex.wrapT = THREE.RepeatWrapping;
  normalTex.repeat.set(24, 24);

  const jerseyMat = new THREE.MeshStandardMaterial({
    name: 'JerseyMaterial',
    map: jerseyTex,
    normalMap: normalTex,
    roughness: pbrInfo.roughness,
    metalness: pbrInfo.metalness,
  });

  const shortsMat = new THREE.MeshStandardMaterial({
    name: 'ShortsMaterial',
    map: shortsTex,
    normalMap: normalTex,
    roughness: pbrInfo.roughness,
    metalness: pbrInfo.metalness,
  });

  const socksMat = new THREE.MeshStandardMaterial({
    name: 'SocksMaterial',
    map: socksTex,
    normalMap: normalTex,
    roughness: pbrInfo.roughness,
    metalness: pbrInfo.metalness,
  });

  const mannequinMat = new THREE.MeshStandardMaterial({
    name: 'MannequinMaterial',
    color: 0x1e2530,
    roughness: 0.85,
    metalness: 0.05,
  });

  // Assign materials to meshes
  gltf.scene.traverse((child) => {
    if ((child as THREE.Mesh).isMesh) {
      const mesh = child as THREE.Mesh;
      const name = (mesh.name || mesh.parent?.name || '').toLowerCase();
      if (name.includes('jersey')) {
        mesh.material = jerseyMat;
      } else if (name.includes('shorts')) {
        mesh.material = shortsMat;
      } else if (name.includes('socks')) {
        mesh.material = socksMat;
      } else if (name.includes('mannequin')) {
        mesh.material = mannequinMat;
      }
    }
  });
  onProgress?.(80);

  // 4. Export as binary GLB
  const exporter = new GLTFExporter();
  const glbBuffer = (await exporter.parseAsync(gltf.scene, {
    binary: true,
  })) as ArrayBuffer;
  onProgress?.(95);

  const safeSlug = kit.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
  const filename = `kit_${safeSlug || kit.id.slice(0, 8)}.glb`;
  const blob = new Blob([glbBuffer], { type: 'model/gltf-binary' });
  triggerBlobDownload(blob, filename);
  onProgress?.(100);

  return { blob, filename };
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
