import * as THREE from 'three';
import { GLTFExporter } from 'three/examples/jsm/exporters/GLTFExporter.js';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import { EventEmitter } from 'events';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Polyfill FileReader for Three.js GLTFExporter in Node.js
class FileReaderPolyfill extends EventEmitter {
  constructor() {
    super();
    this.result = null;
    this.readyState = 0;
    this.onload = null;
    this.onloadend = null;
    this.onerror = null;
  }
  addEventListener(type, listener) {
    this.on(type, listener);
  }
  removeEventListener(type, listener) {
    this.off(type, listener);
  }
  readAsArrayBuffer(blob) {
    this.readyState = 1;
    blob.arrayBuffer().then((buf) => {
      this.result = buf;
      this.readyState = 2;
      const ev = { target: this };
      if (typeof this.onload === 'function') this.onload(ev);
      this.emit('load', ev);
      if (typeof this.onloadend === 'function') this.onloadend(ev);
      this.emit('loadend', ev);
    }).catch((err) => {
      if (typeof this.onerror === 'function') this.onerror(err);
      this.emit('error', err);
      if (typeof this.onloadend === 'function') this.onloadend(err);
      this.emit('loadend', err);
    });
  }
}
globalThis.FileReader = FileReaderPolyfill;

/**
 * 1. JERSEY NODE
 * Proportions: Athletic soccer jersey from neck (+1.25) down to waist hem (+0.12).
 * UV Atlas: 2048x2048 standard OpenKitMaker atlas.
 */
function buildJerseyGroup(material) {
  const group = new THREE.Group();
  group.name = 'Jersey';

  // Torso
  const radSegs = 48;
  const heightSegs = 36;
  const torsoHeight = 1.15;
  const torsoGeo = new THREE.CylinderGeometry(0.42, 0.38, torsoHeight, radSegs, heightSegs, true);
  const pos = torsoGeo.attributes.position;
  const uv = torsoGeo.attributes.uv;
  const v = new THREE.Vector3();

  for (let i = 0; i < pos.count; i++) {
    v.fromBufferAttribute(pos, i);
    const normY = (v.y + torsoHeight / 2) / torsoHeight; // 0 (hem) .. 1 (neck/shoulders)
    const angle = Math.atan2(v.x, v.z);

    const latWidth = (0.42 + 0.07 * Math.sin(normY * Math.PI)) * (1.0 + 0.12 * Math.cos(angle * 2));
    const chestDepth = (0.28 + 0.05 * Math.sin(normY * Math.PI)) * (1.0 - 0.08 * Math.cos(angle * 2));

    v.x = Math.sin(angle) * latWidth;
    v.z = Math.cos(angle) * chestDepth;

    if (normY > 0.85) {
      const shoulderFactor = (normY - 0.85) / 0.15;
      const sideDist = Math.abs(Math.sin(angle));
      v.y -= shoulderFactor * sideDist * 0.12;
      v.x *= 1.0 - shoulderFactor * 0.10;
      v.z *= 1.0 - shoulderFactor * 0.14;
    }

    pos.setXYZ(i, v.x, v.y + 0.68, v.z);

    // UV mapping
    const vCoord = 0.34 + normY * 0.64;
    if (Math.cos(angle) >= 0) {
      const uNorm = (Math.sin(angle) + 1) * 0.5;
      uv.setXY(i, 0.02 + uNorm * 0.46, vCoord);
    } else {
      const uNorm = (-Math.sin(angle) + 1) * 0.5;
      uv.setXY(i, 0.52 + uNorm * 0.46, vCoord);
    }
  }

  torsoGeo.computeVertexNormals();
  const torsoMesh = new THREE.Mesh(torsoGeo, material);
  torsoMesh.name = 'jersey_torso';
  group.add(torsoMesh);

  // Left & Right Sleeves
  const createSleeve = (isLeft) => {
    const sleeveLen = 0.45;
    const geo = new THREE.CylinderGeometry(0.18, 0.14, sleeveLen, 24, 12, true);
    const p = geo.attributes.position;
    const uvs = geo.attributes.uv;

    for (let i = 0; i < p.count; i++) {
      const uVal = uvs.getX(i);
      const vVal = uvs.getY(i);
      const uStart = isLeft ? 0.02 : 0.52;
      uvs.setXY(i, uStart + uVal * 0.29, 0.02 + vVal * 0.30);
    }

    geo.rotateZ(isLeft ? -Math.PI * 0.22 : Math.PI * 0.22);
    geo.translate(isLeft ? 0.46 : -0.46, 0.98, 0.0);
    geo.computeVertexNormals();
    return geo;
  };

  const leftSleeveMesh = new THREE.Mesh(createSleeve(true), material);
  leftSleeveMesh.name = 'jersey_sleeve_left';
  group.add(leftSleeveMesh);

  const rightSleeveMesh = new THREE.Mesh(createSleeve(false), material);
  rightSleeveMesh.name = 'jersey_sleeve_right';
  group.add(rightSleeveMesh);

  // Collar Ring
  const collarGeo = new THREE.TorusGeometry(0.20, 0.035, 16, 36);
  collarGeo.scale(1.12, 0.70, 1.0);
  collarGeo.rotateX(Math.PI * 0.46);
  collarGeo.translate(0, 1.22, 0.02);
  const collarUvs = collarGeo.attributes.uv;
  for (let i = 0; i < collarUvs.count; i++) {
    const uVal = collarUvs.getX(i);
    const vVal = collarUvs.getY(i);
    collarUvs.setXY(i, 0.35 + uVal * 0.13, 0.02 + vVal * 0.30);
  }
  collarGeo.computeVertexNormals();
  const collarMesh = new THREE.Mesh(collarGeo, material);
  collarMesh.name = 'jersey_collar';
  group.add(collarMesh);

  return group;
}

/**
 * 2. SHORTS NODE
 * Proportions: Athletic soccer shorts from waist (+0.18) down to just above knees (-0.38).
 * UV Atlas: 2048x2048:
 * - Front Left:  u in [0.02..0.48], v in [0.52..0.98]
 * - Front Right: u in [0.52..0.98], v in [0.52..0.98]
 * - Back Left:   u in [0.02..0.48], v in [0.02..0.48]
 * - Back Right:  u in [0.52..0.98], v in [0.02..0.48]
 */
function buildShortsGroup(material) {
  const group = new THREE.Group();
  group.name = 'Shorts';

  // Waistband & Pelvis Shell
  const pelvisHeight = 0.22;
  const pelvisGeo = new THREE.CylinderGeometry(0.38, 0.39, pelvisHeight, 36, 12, true);
  const pPos = pelvisGeo.attributes.position;
  const pUv = pelvisGeo.attributes.uv;
  const pv = new THREE.Vector3();

  for (let i = 0; i < pPos.count; i++) {
    pv.fromBufferAttribute(pPos, i);
    const normY = (pv.y + pelvisHeight / 2) / pelvisHeight;
    const angle = Math.atan2(pv.x, pv.z);

    pv.x = Math.sin(angle) * (0.38 + 0.03 * Math.cos(angle * 2));
    pv.z = Math.cos(angle) * (0.25 - 0.02 * Math.cos(angle * 2));
    pPos.setXYZ(i, pv.x, pv.y + 0.06, pv.z);

    const isBack = Math.cos(angle) < 0;
    const isLeft = pv.x >= 0;
    const uBase = isLeft ? 0.02 : 0.52;
    const vBase = isBack ? 0.02 : 0.52;
    const uSpan = 0.44;
    const vSpan = 0.44;

    const angleNorm = (Math.sin(angle) + 1) * 0.5;
    pUv.setXY(i, uBase + angleNorm * uSpan * 0.5, vBase + (0.6 + normY * 0.38) * vSpan);
  }
  pelvisGeo.computeVertexNormals();
  const pelvisMesh = new THREE.Mesh(pelvisGeo, material);
  pelvisMesh.name = 'shorts_pelvis';
  group.add(pelvisMesh);

  // Left & Right Legs
  const createLeg = (isLeft) => {
    const legHeight = 0.42;
    const legGeo = new THREE.CylinderGeometry(0.22, 0.205, legHeight, 28, 16, true);
    const lPos = legGeo.attributes.position;
    const lUv = legGeo.attributes.uv;
    const lv = new THREE.Vector3();

    for (let i = 0; i < lPos.count; i++) {
      lv.fromBufferAttribute(lPos, i);
      const normY = (lv.y + legHeight / 2) / legHeight; // 0 (bottom hem) .. 1 (upper thigh)
      const angle = Math.atan2(lv.x, lv.z);

      // Flared athletic cut
      lv.x = Math.sin(angle) * (0.21 + 0.015 * Math.sin(normY * Math.PI));
      lv.z = Math.cos(angle) * (0.20 + 0.012 * Math.sin(normY * Math.PI));
      lPos.setXYZ(i, lv.x + (isLeft ? 0.18 : -0.18), lv.y - 0.18, lv.z);

      const isBack = Math.cos(angle) < 0;
      const uBase = isLeft ? 0.02 : 0.52;
      const vBase = isBack ? 0.02 : 0.52;
      const uRel = (Math.sin(angle) + 1) * 0.5;

      lUv.setXY(i, uBase + uRel * 0.44, vBase + normY * 0.44);
    }

    legGeo.computeVertexNormals();
    return legGeo;
  };

  const leftLegMesh = new THREE.Mesh(createLeg(true), material);
  leftLegMesh.name = 'shorts_leg_left';
  group.add(leftLegMesh);

  const rightLegMesh = new THREE.Mesh(createLeg(false), material);
  rightLegMesh.name = 'shorts_leg_right';
  group.add(rightLegMesh);

  return group;
}

/**
 * 3. SOCKS NODE
 * Proportions: Athletic soccer socks from below knee (-0.62) down to ankle & foot (-1.45).
 * UV Atlas: 2048x2048:
 * - Left Sock:  u in [0.02..0.48], v in [0.02..0.98]
 * - Right Sock: u in [0.52..0.98], v in [0.02..0.98]
 */
function buildSocksGroup(material) {
  const group = new THREE.Group();
  group.name = 'Socks';

  const createSock = (isLeft) => {
    const sockHeight = 0.76;
    const sockGeo = new THREE.CylinderGeometry(0.155, 0.115, sockHeight, 28, 24, true);
    const sPos = sockGeo.attributes.position;
    const sUv = sockGeo.attributes.uv;
    const sv = new THREE.Vector3();

    for (let i = 0; i < sPos.count; i++) {
      sv.fromBufferAttribute(sPos, i);
      const normY = (sv.y + sockHeight / 2) / sockHeight; // 0 (ankle) .. 1 (knee cuff)
      const angle = Math.atan2(sv.x, sv.z);

      // Anatomical calf bulge
      const calfBulge = 0.035 * Math.sin(Math.pow(normY, 1.2) * Math.PI);
      const rX = 0.115 + (0.155 - 0.115) * normY + calfBulge;
      const rZ = 0.120 + (0.160 - 0.120) * normY + calfBulge * 1.25;

      sv.x = Math.sin(angle) * rX + (isLeft ? 0.18 : -0.18);
      sv.z = Math.cos(angle) * rZ;
      sPos.setXYZ(i, sv.x, sv.y - 0.98, sv.z);

      const uBase = isLeft ? 0.02 : 0.52;
      const uRel = (angle / (Math.PI * 2) + 0.5);
      sUv.setXY(i, uBase + uRel * 0.44, 0.02 + normY * 0.94);
    }

    sockGeo.computeVertexNormals();
    return sockGeo;
  };

  const leftSockMesh = new THREE.Mesh(createSock(true), material);
  leftSockMesh.name = 'socks_left';
  group.add(leftSockMesh);

  const rightSockMesh = new THREE.Mesh(createSock(false), material);
  rightSockMesh.name = 'socks_right';
  group.add(rightSockMesh);

  return group;
}

/**
 * 4. MANNEQUIN NODE
 * Stylized athletic mannequin:
 * - Minimalist Head & Neck (y = +1.28 to +1.65)
 * - Exposed Arms & Hands (extending from sleeves)
 * - Exposed Knees (gap between shorts -0.38 and socks -0.62)
 * - Sleek Soccer Boots / Cleats at feet (-1.34 to -1.52)
 * - Minimalist Studio Pedestal Base Disc
 */
function buildMannequinGroup(material) {
  const group = new THREE.Group();
  group.name = 'Mannequin';

  // 4a. Stylized Minimalist Head
  const headGeo = new THREE.SphereGeometry(0.16, 28, 22);
  headGeo.scale(1.0, 1.25, 1.12);
  headGeo.translate(0, 1.48, 0.02);
  const headMesh = new THREE.Mesh(headGeo, material);
  headMesh.name = 'mannequin_head';
  group.add(headMesh);

  // 4b. Neck
  const neckGeo = new THREE.CylinderGeometry(0.10, 0.115, 0.16, 20);
  neckGeo.translate(0, 1.28, 0.01);
  const neckMesh = new THREE.Mesh(neckGeo, material);
  neckMesh.name = 'mannequin_neck';
  group.add(neckMesh);

  // 4c. Exposed Arms & Hands
  const createArm = (isLeft) => {
    const armGroup = new THREE.Group();
    // Bicep/Forearm
    const armGeo = new THREE.CylinderGeometry(0.11, 0.085, 0.55, 16);
    armGeo.rotateZ(isLeft ? -Math.PI * 0.10 : Math.PI * 0.10);
    armGeo.translate(isLeft ? 0.54 : -0.54, 0.55, 0.04);
    const armMesh = new THREE.Mesh(armGeo, material);
    armGroup.add(armMesh);

    // Stylized Hand
    const handGeo = new THREE.BoxGeometry(0.06, 0.14, 0.09);
    handGeo.translate(isLeft ? 0.58 : -0.58, 0.22, 0.06);
    const handMesh = new THREE.Mesh(handGeo, material);
    armGroup.add(handMesh);
    return armGroup;
  };

  const leftArm = createArm(true);
  leftArm.name = 'mannequin_arm_left';
  group.add(leftArm);

  const rightArm = createArm(false);
  rightArm.name = 'mannequin_arm_right';
  group.add(rightArm);

  // 4d. Exposed Knees (gap between shorts hem -0.38 and socks cuff -0.62)
  const createKnee = (isLeft) => {
    const kneeGeo = new THREE.CylinderGeometry(0.15, 0.14, 0.26, 20);
    kneeGeo.translate(isLeft ? 0.18 : -0.18, -0.49, 0.0);
    const kneeMesh = new THREE.Mesh(kneeGeo, material);
    kneeMesh.name = isLeft ? 'mannequin_knee_left' : 'mannequin_knee_right';
    return kneeMesh;
  };
  group.add(createKnee(true));
  group.add(createKnee(false));

  // 4e. Modern Soccer Cleats / Boots
  const createBoot = (isLeft) => {
    const bootGeo = new THREE.BoxGeometry(0.16, 0.12, 0.38);
    bootGeo.translate(isLeft ? 0.18 : -0.18, -1.42, 0.07);
    const bootMesh = new THREE.Mesh(bootGeo, material);
    bootMesh.name = isLeft ? 'mannequin_boot_left' : 'mannequin_boot_right';
    return bootMesh;
  };
  group.add(createBoot(true));
  group.add(createBoot(false));

  // 4f. Minimalist Pedestal Stand
  const baseGeo = new THREE.CylinderGeometry(0.72, 0.78, 0.04, 36);
  baseGeo.translate(0, -1.50, 0.0);
  const baseMesh = new THREE.Mesh(baseGeo, material);
  baseMesh.name = 'mannequin_pedestal';
  group.add(baseMesh);

  return group;
}

async function main() {
  console.log('Generating procedural Clean Room Full Kit outfit...');

  // Materials with distinct names
  const jerseyMaterial = new THREE.MeshStandardMaterial({
    name: 'jersey_material',
    roughness: 0.8,
    metalness: 0.0
  });

  const shortsMaterial = new THREE.MeshStandardMaterial({
    name: 'shorts_material',
    roughness: 0.8,
    metalness: 0.0
  });

  const socksMaterial = new THREE.MeshStandardMaterial({
    name: 'socks_material',
    roughness: 0.8,
    metalness: 0.0
  });

  const mannequinMaterial = new THREE.MeshStandardMaterial({
    name: 'mannequin_material',
    color: 0x1E293B,
    roughness: 0.35,
    metalness: 0.75
  });

  const root = new THREE.Group();
  root.name = 'FullKit';

  const jerseyNode = buildJerseyGroup(jerseyMaterial);
  const shortsNode = buildShortsGroup(shortsMaterial);
  const socksNode = buildSocksGroup(socksMaterial);
  const mannequinNode = buildMannequinGroup(mannequinMaterial);

  root.add(jerseyNode);
  root.add(shortsNode);
  root.add(socksNode);
  root.add(mannequinNode);

  const exporter = new GLTFExporter();
  const rawGltf = await exporter.parseAsync(root, { binary: true });
  const rawBuffer = Buffer.from(rawGltf);

  // Filament gltfio baseColorTexture injection
  const png1x1 = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'base64'
  );
  const padPng = (4 - (png1x1.length % 4)) % 4;
  const pngBuf = Buffer.concat([png1x1, Buffer.alloc(padPng)]);

  const jsonLen = rawBuffer.readUInt32LE(12);
  const gltfObj = JSON.parse(rawBuffer.subarray(20, 20 + jsonLen).toString('utf8'));

  const binHeaderOffset = 20 + jsonLen;
  const binLen = rawBuffer.readUInt32LE(binHeaderOffset);
  const origBin = rawBuffer.subarray(binHeaderOffset + 8, binHeaderOffset + 8 + binLen);

  const newBin = Buffer.concat([origBin, pngBuf]);
  gltfObj.buffers[0].byteLength = newBin.length;

  const imgViewIdx = gltfObj.bufferViews.length;
  gltfObj.bufferViews.push({
    buffer: 0,
    byteOffset: binLen,
    byteLength: png1x1.length,
  });

  gltfObj.images = [{ bufferView: imgViewIdx, mimeType: 'image/png' }];
  gltfObj.samplers = [{ magFilter: 9729, minFilter: 9729, wrapS: 10497, wrapT: 10497 }];
  gltfObj.textures = [{ sampler: 0, source: 0 }];

  // Assign baseColorTexture to the 3 garment materials so Filament compiles baseColorMap
  for (const mat of gltfObj.materials) {
    if (mat.name !== 'mannequin_material') {
      if (!mat.pbrMetallicRoughness) mat.pbrMetallicRoughness = {};
      mat.pbrMetallicRoughness.baseColorTexture = { index: 0 };
    }
  }

  let jsonStr = JSON.stringify(gltfObj);
  const padJson = (4 - (Buffer.byteLength(jsonStr) % 4)) % 4;
  jsonStr += ' '.repeat(padJson);
  const newJsonBuf = Buffer.from(jsonStr, 'utf8');

  const totalLen = 12 + 8 + newJsonBuf.length + 8 + newBin.length;
  const buffer = Buffer.alloc(totalLen);
  buffer.write('glTF', 0);
  buffer.writeUInt32LE(2, 4);
  buffer.writeUInt32LE(totalLen, 8);

  // Chunk 0 (JSON)
  buffer.writeUInt32LE(newJsonBuf.length, 12);
  buffer.writeUInt32LE(0x4e4f534a, 16);
  newJsonBuf.copy(buffer, 20);

  // Chunk 1 (BIN)
  const chunk1Offset = 20 + newJsonBuf.length;
  buffer.writeUInt32LE(newBin.length, chunk1Offset);
  buffer.writeUInt32LE(0x004e4942, chunk1Offset + 4);
  newBin.copy(buffer, chunk1Offset + 8);

  // Save to Web public models
  const webModelsDir = path.resolve(__dirname, '../public/models');
  fs.mkdirSync(webModelsDir, { recursive: true });
  const webGlbPath = path.join(webModelsDir, 'kit_full_outfit_clean.glb');
  fs.writeFileSync(webGlbPath, buffer);
  console.log(`Saved Web model: ${webGlbPath} (${buffer.length} bytes)`);

  // Save to Android assets models
  const androidAssetsDir = path.resolve(__dirname, '../../native/app/src/main/assets/models');
  fs.mkdirSync(androidAssetsDir, { recursive: true });
  const androidGlbPath = path.join(androidAssetsDir, 'kit_full_outfit_clean.glb');
  fs.writeFileSync(androidGlbPath, buffer);
  console.log(`Saved Android model: ${androidGlbPath} (${buffer.length} bytes)`);

  // Also verify root scripts/ location compatibility
  const rootScriptsDir = path.resolve(__dirname, '../../scripts');
  fs.mkdirSync(rootScriptsDir, { recursive: true });
  fs.copyFileSync(__filename, path.join(rootScriptsDir, 'generate-procedural-kit.mjs'));
  console.log(`Mirrored script to root: scripts/generate-procedural-kit.mjs`);
}

main().catch((err) => {
  console.error('Failed to generate full kit GLB:', err);
  process.exit(1);
});
