import * as THREE from '../web/node_modules/three/build/three.module.js';
import { GLTFExporter } from '../web/node_modules/three/examples/jsm/exporters/GLTFExporter.js';
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
 * Athletic soccer jersey from neck (+1.25) down to waist hem (+0.12).
 * Smooth shading (per-vertex normals), rounded athletic contours (curved chest, lat taper).
 * UV Atlas: 2048x2048:
 * - Front: u in [0.02..0.48], v in [0.34..0.98]
 * - Back:  u in [0.52..0.98], v in [0.34..0.98]
 * - Sleeves: left in [0.02..0.31], right in [0.52..0.81], v in [0.02..0.32]
 * - Collar:  u in [0.34..0.48], v in [0.02..0.32]
 */
function buildJerseyGroup(material) {
  const group = new THREE.Group();
  group.name = 'Jersey';

  // Torso - High tessellation for smooth curvature
  const radSegs = 64;
  const heightSegs = 48;
  const torsoHeight = 1.15;
  const torsoGeo = new THREE.CylinderGeometry(0.40, 0.38, torsoHeight, radSegs, heightSegs, true);
  const pos = torsoGeo.attributes.position;
  const uv = torsoGeo.attributes.uv;
  const v = new THREE.Vector3();

  for (let i = 0; i < pos.count; i++) {
    v.fromBufferAttribute(pos, i);
    const normY = (v.y + torsoHeight / 2) / torsoHeight; // 0 (bottom hem) .. 1 (neck/shoulders)
    const angle = Math.atan2(v.x, v.z);

    // Anatomical athletic silhouette:
    // - Broader at chest (normY ~ 0.70)
    // - Sleek taper at waist (normY ~ 0.25)
    // - Elliptical cross section: X (width) > Z (depth)
    const waistTaper = 1.0 - 0.10 * Math.sin(normY * Math.PI * 0.85);
    const chestExpand = 1.0 + 0.16 * Math.pow(normY, 1.4);
    
    // Front chest volume bulge vs flat spine
    const isFront = Math.cos(angle) > 0;
    const frontBulge = isFront ? 0.04 * Math.sin(normY * Math.PI) * Math.cos(angle) : 0.0;

    const latRadiusX = 0.44 * waistTaper * chestExpand;
    const chestRadiusZ = (0.28 * waistTaper * chestExpand) + frontBulge;

    v.x = Math.sin(angle) * latRadiusX;
    v.z = Math.cos(angle) * chestRadiusZ;

    // Smooth shoulder slope near neck
    if (normY > 0.82) {
      const shoulderFactor = (normY - 0.82) / 0.18;
      const lateralDist = Math.abs(Math.sin(angle));
      v.y -= shoulderFactor * lateralDist * 0.10;
      v.x *= 1.0 - shoulderFactor * 0.08;
      v.z *= 1.0 - shoulderFactor * 0.12;
    }

    pos.setXYZ(i, v.x, v.y + 0.68, v.z);

    // Continuous UV mapping onto Front and Back zones
    const vCoord = 0.35 + normY * 0.62;
    if (Math.cos(angle) >= 0) {
      const uNorm = (Math.sin(angle) + 1) * 0.5; // 0 (right) .. 1 (left)
      uv.setXY(i, 0.03 + uNorm * 0.44, vCoord);
    } else {
      const uNorm = (-Math.sin(angle) + 1) * 0.5;
      uv.setXY(i, 0.53 + uNorm * 0.44, vCoord);
    }
  }

  torsoGeo.computeVertexNormals();
  const torsoMesh = new THREE.Mesh(torsoGeo, material);
  torsoMesh.name = 'jersey_torso';
  group.add(torsoMesh);

  // Left & Right Sleeves - Smooth anatomical raglan cut
  const createSleeve = (isLeft) => {
    const sleeveLen = 0.44;
    const sleeveGeo = new THREE.CylinderGeometry(0.175, 0.145, sleeveLen, 32, 20, true);
    const p = sleeveGeo.attributes.position;
    const uvs = sleeveGeo.attributes.uv;
    const sv = new THREE.Vector3();

    for (let i = 0; i < p.count; i++) {
      sv.fromBufferAttribute(p, i);
      const uVal = uvs.getX(i);
      const vVal = uvs.getY(i);
      const angle = Math.atan2(sv.x, sv.z);

      // Subtle bicep roundness
      const normY = (sv.y + sleeveLen / 2) / sleeveLen;
      const bicepBulge = 1.0 + 0.08 * Math.sin(normY * Math.PI);
      sv.x *= bicepBulge;
      sv.z *= bicepBulge;
      p.setXYZ(i, sv.x, sv.y, sv.z);

      const uStart = isLeft ? 0.03 : 0.53;
      uvs.setXY(i, uStart + uVal * 0.28, 0.03 + vVal * 0.28);
    }

    sleeveGeo.rotateZ(isLeft ? -Math.PI * 0.20 : Math.PI * 0.20);
    sleeveGeo.rotateX(Math.PI * 0.04);
    sleeveGeo.translate(isLeft ? 0.47 : -0.47, 0.98, 0.02);
    sleeveGeo.computeVertexNormals();
    return sleeveGeo;
  };

  const leftSleeveMesh = new THREE.Mesh(createSleeve(true), material);
  leftSleeveMesh.name = 'jersey_sleeve_left';
  group.add(leftSleeveMesh);

  const rightSleeveMesh = new THREE.Mesh(createSleeve(false), material);
  rightSleeveMesh.name = 'jersey_sleeve_right';
  group.add(rightSleeveMesh);

  // Ribbed Collar Ring - Smooth Torus
  const collarGeo = new THREE.TorusGeometry(0.19, 0.032, 20, 48);
  collarGeo.scale(1.10, 0.72, 1.0);
  collarGeo.rotateX(Math.PI * 0.46);
  collarGeo.translate(0, 1.22, 0.02);
  const collarUvs = collarGeo.attributes.uv;
  for (let i = 0; i < collarUvs.count; i++) {
    const uVal = collarUvs.getX(i);
    const vVal = collarUvs.getY(i);
    collarUvs.setXY(i, 0.35 + uVal * 0.12, 0.03 + vVal * 0.28);
  }
  collarGeo.computeVertexNormals();
  const collarMesh = new THREE.Mesh(collarGeo, material);
  collarMesh.name = 'jersey_collar';
  group.add(collarMesh);

  return group;
}

/**
 * 2. SHORTS NODE
 * Smooth soccer shorts from waist (+0.18) down to thighs (-0.38).
 * Anatomical pelvic curvature with smooth flared leg cuts.
 */
function buildShortsGroup(material) {
  const group = new THREE.Group();
  group.name = 'Shorts';

  // Waistband & Pelvis Shell
  const pelvisHeight = 0.24;
  const pelvisGeo = new THREE.CylinderGeometry(0.39, 0.41, pelvisHeight, 48, 20, true);
  const pPos = pelvisGeo.attributes.position;
  const pUv = pelvisGeo.attributes.uv;
  const pv = new THREE.Vector3();

  for (let i = 0; i < pPos.count; i++) {
    pv.fromBufferAttribute(pPos, i);
    const normY = (pv.y + pelvisHeight / 2) / pelvisHeight;
    const angle = Math.atan2(pv.x, pv.z);

    // Oval pelvis cross-section
    const isBack = Math.cos(angle) < 0;
    const gluteBulge = isBack ? 0.04 * (1.0 - normY) : 0.0;

    pv.x = Math.sin(angle) * (0.39 + 0.02 * Math.cos(angle * 2));
    pv.z = Math.cos(angle) * (0.26 - 0.01 * Math.cos(angle * 2)) + gluteBulge;
    pPos.setXYZ(i, pv.x, pv.y + 0.06, pv.z);

    const isLeft = pv.x >= 0;
    const uBase = isLeft ? 0.03 : 0.53;
    const vBase = isBack ? 0.03 : 0.53;
    const angleNorm = (Math.sin(angle) + 1) * 0.5;
    pUv.setXY(i, uBase + angleNorm * 0.44 * 0.5, vBase + (0.55 + normY * 0.42) * 0.44);
  }
  pelvisGeo.computeVertexNormals();
  const pelvisMesh = new THREE.Mesh(pelvisGeo, material);
  pelvisMesh.name = 'shorts_pelvis';
  group.add(pelvisMesh);

  // Left & Right Legs - Smooth flared athletic cuts
  const createLeg = (isLeft) => {
    const legHeight = 0.44;
    const legGeo = new THREE.CylinderGeometry(0.23, 0.21, legHeight, 36, 24, true);
    const lPos = legGeo.attributes.position;
    const lUv = legGeo.attributes.uv;
    const lv = new THREE.Vector3();

    for (let i = 0; i < lPos.count; i++) {
      lv.fromBufferAttribute(lPos, i);
      const normY = (lv.y + legHeight / 2) / legHeight; // 0 (bottom hem) .. 1 (upper thigh)
      const angle = Math.atan2(lv.x, lv.z);

      // Flared athletic cut
      const thighFlare = 1.0 + 0.06 * Math.sin(normY * Math.PI);
      lv.x = Math.sin(angle) * 0.215 * thighFlare;
      lv.z = Math.cos(angle) * 0.205 * thighFlare;
      lPos.setXYZ(i, lv.x + (isLeft ? 0.18 : -0.18), lv.y - 0.18, lv.z);

      const isBack = Math.cos(angle) < 0;
      const uBase = isLeft ? 0.03 : 0.53;
      const vBase = isBack ? 0.03 : 0.53;
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
 * Anatomical soccer socks from knee (-0.62) down to ankle (-1.35).
 * Smooth calf bulge and tapered ankle.
 */
function buildSocksGroup(material) {
  const group = new THREE.Group();
  group.name = 'Socks';

  const createSock = (isLeft) => {
    const sockHeight = 0.76;
    const sockGeo = new THREE.CylinderGeometry(0.155, 0.115, sockHeight, 36, 32, true);
    const sPos = sockGeo.attributes.position;
    const sUv = sockGeo.attributes.uv;
    const sv = new THREE.Vector3();

    for (let i = 0; i < sPos.count; i++) {
      sv.fromBufferAttribute(sPos, i);
      const normY = (sv.y + sockHeight / 2) / sockHeight; // 0 (ankle) .. 1 (knee cuff)
      const angle = Math.atan2(sv.x, sv.z);

      // Anatomical calf bulge (gastrocnemius profile)
      const isBack = Math.cos(angle) < 0;
      const calfBulge = isBack ? 0.040 * Math.sin(Math.pow(normY, 1.1) * Math.PI) : 0.015 * Math.sin(normY * Math.PI);
      const rX = 0.115 + (0.155 - 0.115) * normY + (calfBulge * 0.6);
      const rZ = 0.118 + (0.158 - 0.118) * normY + calfBulge;

      sv.x = Math.sin(angle) * rX + (isLeft ? 0.18 : -0.18);
      sv.z = Math.cos(angle) * rZ;
      sPos.setXYZ(i, sv.x, sv.y - 0.98, sv.z);

      const uBase = isLeft ? 0.03 : 0.53;
      const uRel = (angle / (Math.PI * 2) + 0.5);
      sUv.setXY(i, uBase + uRel * 0.44, 0.03 + normY * 0.94);
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
 * Stylized athletic mannequin (Smooth, elegant low-poly with rounded geometry):
 * - Stylized rounded head (SphereGeometry with 36x28 segments)
 * - Anatomical neck
 * - Smooth arms & streamlined hands (no boxy meshes!)
 * - Smooth contoured knees
 * - Aerodynamic modern soccer cleats (smoothed aerodynamic contour, no flat boxes!)
 * - Sleek circular studio pedestal
 */
function buildMannequinGroup(material) {
  const group = new THREE.Group();
  group.name = 'Mannequin';

  // 4a. Smooth Minimalist Head
  const headGeo = new THREE.SphereGeometry(0.165, 36, 28);
  headGeo.scale(0.98, 1.25, 1.10);
  headGeo.translate(0, 1.48, 0.02);
  headGeo.computeVertexNormals();
  const headMesh = new THREE.Mesh(headGeo, material);
  headMesh.name = 'mannequin_head';
  group.add(headMesh);

  // 4b. Smooth Neck
  const neckGeo = new THREE.CylinderGeometry(0.10, 0.115, 0.16, 28);
  neckGeo.translate(0, 1.28, 0.01);
  neckGeo.computeVertexNormals();
  const neckMesh = new THREE.Mesh(neckGeo, material);
  neckMesh.name = 'mannequin_neck';
  group.add(neckMesh);

  // 4c. Smooth Exposed Arms & Streamlined Hands
  const createArm = (isLeft) => {
    const armGroup = new THREE.Group();

    // Forearm
    const armGeo = new THREE.CylinderGeometry(0.105, 0.082, 0.55, 24);
    armGeo.rotateZ(isLeft ? -Math.PI * 0.10 : Math.PI * 0.10);
    armGeo.translate(isLeft ? 0.54 : -0.54, 0.55, 0.04);
    armGeo.computeVertexNormals();
    const armMesh = new THREE.Mesh(armGeo, material);
    armGroup.add(armMesh);

    // Streamlined Hand (Smooth tapered cylinder rather than sharp box)
    const handGeo = new THREE.CylinderGeometry(0.065, 0.045, 0.15, 20);
    handGeo.scale(1.2, 1.0, 0.7); // Flattened hand profile
    handGeo.rotateZ(isLeft ? -Math.PI * 0.08 : Math.PI * 0.08);
    handGeo.translate(isLeft ? 0.58 : -0.58, 0.22, 0.06);
    handGeo.computeVertexNormals();
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

  // 4d. Smooth Exposed Knees
  const createKnee = (isLeft) => {
    const kneeGeo = new THREE.CylinderGeometry(0.148, 0.138, 0.26, 28);
    kneeGeo.translate(isLeft ? 0.18 : -0.18, -0.49, 0.0);
    kneeGeo.computeVertexNormals();
    const kneeMesh = new THREE.Mesh(kneeGeo, material);
    kneeMesh.name = isLeft ? 'mannequin_knee_left' : 'mannequin_knee_right';
    return kneeMesh;
  };
  group.add(createKnee(true));
  group.add(createKnee(false));

  // 4e. Modern Aerodynamic Soccer Cleats (Smooth aerodynamic shape, rounded toe)
  const createBoot = (isLeft) => {
    // Morphed smooth cylinder: length 0.38, height 0.13, width 0.16
    const bootGeo = new THREE.CylinderGeometry(0.09, 0.075, 0.38, 28, 16);
    bootGeo.rotateX(Math.PI / 2); // Lay horizontally along Z axis
    bootGeo.scale(0.9, 0.75, 1.0); // Athletic low-profile cleat silhouette
    const bPos = bootGeo.attributes.position;
    const bv = new THREE.Vector3();
    for (let i = 0; i < bPos.count; i++) {
      bv.fromBufferAttribute(bPos, i);
      // Taper front toe
      if (bv.z > 0.08) {
        const toeTaper = (bv.z - 0.08) / 0.11;
        bv.y *= 1.0 - toeTaper * 0.35;
        bv.x *= 1.0 - toeTaper * 0.25;
      }
      bPos.setXYZ(i, bv.x, bv.y, bv.z);
    }
    bootGeo.translate(isLeft ? 0.18 : -0.18, -1.41, 0.08);
    bootGeo.computeVertexNormals();
    const bootMesh = new THREE.Mesh(bootGeo, material);
    bootMesh.name = isLeft ? 'mannequin_boot_left' : 'mannequin_boot_right';
    return bootMesh;
  };
  group.add(createBoot(true));
  group.add(createBoot(false));

  // 4f. Smooth Minimalist Pedestal Stand
  const baseGeo = new THREE.CylinderGeometry(0.72, 0.78, 0.04, 48);
  baseGeo.translate(0, -1.50, 0.0);
  baseGeo.computeVertexNormals();
  const baseMesh = new THREE.Mesh(baseGeo, material);
  baseMesh.name = 'mannequin_pedestal';
  group.add(baseMesh);

  return group;
}

async function main() {
  console.log('Generating procedural Clean Room Full Kit outfit (Smooth Shading v1.1.1)...');

  // Materials with flatShading = false (Smooth Shading by vertex normal interpolation)
  const jerseyMaterial = new THREE.MeshStandardMaterial({
    name: 'jersey_material',
    roughness: 0.8,
    metalness: 0.0,
    flatShading: false,
  });

  const shortsMaterial = new THREE.MeshStandardMaterial({
    name: 'shorts_material',
    roughness: 0.8,
    metalness: 0.0,
    flatShading: false,
  });

  const socksMaterial = new THREE.MeshStandardMaterial({
    name: 'socks_material',
    roughness: 0.8,
    metalness: 0.0,
    flatShading: false,
  });

  const mannequinMaterial = new THREE.MeshStandardMaterial({
    name: 'mannequin_material',
    color: 0x1E293B,
    roughness: 0.35,
    metalness: 0.75,
    flatShading: false,
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
  const webModelsDir = path.resolve(__dirname, '../web/public/models');
  fs.mkdirSync(webModelsDir, { recursive: true });
  const webGlbPath = path.join(webModelsDir, 'kit_full_outfit_clean.glb');
  fs.writeFileSync(webGlbPath, buffer);
  console.log(`Saved Web model: ${webGlbPath} (${buffer.length} bytes)`);

  // Save to Android assets models
  const androidAssetsDir = path.resolve(__dirname, '../native/app/src/main/assets/models');
  fs.mkdirSync(androidAssetsDir, { recursive: true });
  const androidGlbPath = path.join(androidAssetsDir, 'kit_full_outfit_clean.glb');
  fs.writeFileSync(androidGlbPath, buffer);
  console.log(`Saved Android model: ${androidGlbPath} (${buffer.length} bytes)`);
}

main().catch((err) => {
  console.error('Failed to generate full kit GLB:', err);
  process.exit(1);
});
