import * as THREE from 'three';
import { GLTFExporter } from 'three/examples/jsm/exporters/GLTFExporter.js';
import * as fs from 'fs';
import * as path from 'path';
import { EventEmitter } from 'events';

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



function buildJersey() {
  const group = new THREE.Group();
  group.name = 'kit_jersey';

  const material = new THREE.MeshStandardMaterial({
    roughness: 0.5,
    metalness: 0.05,
    name: 'jersey_material'
  });

  // 1. Torso
  const radSegs = 48;
  const heightSegs = 36;
  const torsoHeight = 2.4;
  const torsoGeo = new THREE.CylinderGeometry(0.74, 0.70, torsoHeight, radSegs, heightSegs, true);

  const posAttr = torsoGeo.attributes.position;
  const uvAttr = torsoGeo.attributes.uv;
  const vertex = new THREE.Vector3();

  for (let i = 0; i < posAttr.count; i++) {
    vertex.fromBufferAttribute(posAttr, i);
    const normY = (vertex.y + torsoHeight / 2) / torsoHeight; // 0 (bottom) .. 1 (top)
    const angle = Math.atan2(vertex.x, vertex.z);

    // Anatomical chest shape: wider chest, slight waist taper
    const latWidth = (0.74 + 0.12 * Math.sin(normY * Math.PI)) * (1.0 + 0.12 * Math.cos(angle * 2));
    const chestDepth = (0.50 + 0.08 * Math.sin(normY * Math.PI)) * (1.0 - 0.08 * Math.cos(angle * 2));

    vertex.x = Math.sin(angle) * latWidth;
    vertex.z = Math.cos(angle) * chestDepth;

    if (normY > 0.85) {
      const shoulderFactor = (normY - 0.85) / 0.15;
      const sideDist = Math.abs(Math.sin(angle));
      vertex.y -= shoulderFactor * sideDist * 0.20;
      vertex.x *= 1.0 - shoulderFactor * 0.10;
      vertex.z *= 1.0 - shoulderFactor * 0.15;
    }

    posAttr.setXYZ(i, vertex.x, vertex.y, vertex.z);

    // UV Atlas mapping: Front torso u in [0.01..0.49], Back torso u in [0.51..0.99]
    // Torso y in canvas: 0..1360 out of 2048 -> v in [0.336..1.0]
    const vCoord = 0.34 + normY * 0.64;
    if (Math.cos(angle) >= 0) {
      const uNorm = (Math.sin(angle) + 1) * 0.5;
      uvAttr.setXY(i, 0.02 + uNorm * 0.46, vCoord);
    } else {
      const uNorm = (-Math.sin(angle) + 1) * 0.5;
      uvAttr.setXY(i, 0.52 + uNorm * 0.46, vCoord);
    }
  }

  torsoGeo.computeVertexNormals();
  const torsoMesh = new THREE.Mesh(torsoGeo, material);
  torsoMesh.name = 'torso';
  group.add(torsoMesh);

  // 2. Sleeves
  const createSleeveGeometry = (isLeft) => {
    const sleeveLen = 0.85;
    const geo = new THREE.CylinderGeometry(0.32, 0.26, sleeveLen, 24, 12, true);
    const p = geo.attributes.position;
    const uvs = geo.attributes.uv;

    for (let i = 0; i < p.count; i++) {
      const u = uvs.getX(i);
      const v = uvs.getY(i);
      // Sleeve UV island: Left u in [0.01..0.32], Right u in [0.51..0.82], v in [0.02..0.32]
      const uStart = isLeft ? 0.02 : 0.52;
      const uSpan = 0.29;
      uvs.setXY(i, uStart + u * uSpan, 0.02 + v * 0.30);
    }

    geo.rotateZ(isLeft ? -Math.PI * 0.32 : Math.PI * 0.32);
    geo.translate(isLeft ? 0.98 : -0.98, 0.70, 0.0);
    geo.computeVertexNormals();
    return geo;
  };

  const leftSleeve = new THREE.Mesh(createSleeveGeometry(true), material);
  leftSleeve.name = 'sleeve_left';
  group.add(leftSleeve);

  const rightSleeve = new THREE.Mesh(createSleeveGeometry(false), material);
  rightSleeve.name = 'sleeve_right';
  group.add(rightSleeve);

  // 3. Collar
  const collarGeo = new THREE.TorusGeometry(0.40, 0.065, 16, 36);
  collarGeo.scale(1.10, 0.70, 1.0);
  collarGeo.rotateX(Math.PI * 0.46);
  collarGeo.translate(0, 1.15, 0.02);
  const collarUv = collarGeo.attributes.uv;
  for (let i = 0; i < collarUv.count; i++) {
    const u = collarUv.getX(i);
    const v = collarUv.getY(i);
    collarUv.setXY(i, 0.35 + u * 0.13, 0.02 + v * 0.30);
  }
  collarGeo.computeVertexNormals();
  const collarMesh = new THREE.Mesh(collarGeo, material);
  collarMesh.name = 'collar';
  group.add(collarMesh);

  return group;
}

const jersey = buildJersey();
const exporter = new GLTFExporter();

try {
  const rawGltf = await exporter.parseAsync(jersey, { binary: true });
  const rawBuffer = Buffer.from(rawGltf);

  // Filament gltfio compiles ubershader materials without 'baseColorMap' if the GLTF
  // material does not have 'baseColorTexture'. We embed a 1x1 base PNG image into the GLB
  // so Filament compiles with 'baseColorMap' parameter support.
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

  for (const mat of gltfObj.materials) {
    if (!mat.pbrMetallicRoughness) mat.pbrMetallicRoughness = {};
    mat.pbrMetallicRoughness.baseColorTexture = { index: 0 };
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

  // 1. Android assets destination
  const androidAssetsDir = path.resolve('../native/app/src/main/assets/models');
  fs.mkdirSync(androidAssetsDir, { recursive: true });
  const androidGlbPath = path.join(androidAssetsDir, 'kit_jersey_pbr.glb');
  fs.writeFileSync(androidGlbPath, buffer);
  console.log(`Saved Android model: ${androidGlbPath} (${buffer.length} bytes)`);

  // 2. Web public models destination
  const webModelsDir = path.resolve('./public/models');
  fs.mkdirSync(webModelsDir, { recursive: true });
  const webGlbPath = path.join(webModelsDir, 'kit_jersey_pbr.glb');
  fs.writeFileSync(webGlbPath, buffer);
  console.log(`Saved Web model: ${webGlbPath} (${buffer.length} bytes)`);
} catch (error) {
  console.error('Error generating GLB:', error);
  process.exit(1);
}

