// Implements: SCR-EDITOR / FUN-PREVIEW-3D
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { KTX2Loader } from 'three/addons/loaders/KTX2Loader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import type { LightingPreset } from '../types/kit';

export interface Kit3DEngineController {
  updateCanvasTexture: (sourceCanvas: HTMLCanvasElement) => void;
  setLightingPreset: (preset: LightingPreset) => void;
  setFinishType: (finish: 'MATTE_MESH' | 'PRO_VAPOR' | 'RETRO_SATIN') => void;
  setAutoRotate: (enabled: boolean) => void;
  resetCamera: () => void;
  setCameraAngle: (view: 'FRONT' | 'BACK' | 'LEFT' | 'RIGHT') => void;
  resize: () => void;
  getFps: () => number;
  getLoaderCapabilities: () => { ktx2Ready: boolean; dracoReady: boolean; isSoftwareGl: boolean };
  dispose: () => void;
}

/**
 * Converts Kelvin color temperature (e.g., 6500K Studio, 5400K Stadium from tokens.json)
 * to a linear RGB THREE.Color.
 */
function kelvinToColor(kelvin: number): THREE.Color {
  const temp = kelvin / 100;
  let r: number;
  let g: number;
  let b: number;

  if (temp <= 66) {
    r = 255;
    g = 99.4708025861 * Math.log(temp) - 161.1195681661;
    if (temp <= 19) {
      b = 0;
    } else {
      b = 138.5177312231 * Math.log(temp - 10) - 305.0447927307;
    }
  } else {
    r = 329.698727446 * Math.pow(temp - 60, -0.1332047592);
    g = 288.1221695283 * Math.pow(temp - 60, -0.0755148492);
    b = 255;
  }

  return new THREE.Color(
    THREE.MathUtils.clamp(r, 0, 255) / 255,
    THREE.MathUtils.clamp(g, 0, 255) / 255,
    THREE.MathUtils.clamp(b, 0, 255) / 255
  );
}

/**
 * Generates a procedural breathable sports-fabric micro-normal map in linear space.
 */
function createProceduralFabricNormalMap(): THREE.DataTexture {
  const size = 128;
  const data = new Uint8Array(size * size * 4);

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const idx = (y * size + x) * 4;
      const waveX = Math.sin((x / 4) * Math.PI * 2) * 18;
      const waveY = Math.cos((y / 4) * Math.PI * 2) * 18;
      const pore = (x % 8 === 0 && y % 8 === 0) ? -22 : 0;

      data[idx] = THREE.MathUtils.clamp(128 + waveX, 0, 255);
      data[idx + 1] = THREE.MathUtils.clamp(128 + waveY + pore, 0, 255);
      data[idx + 2] = 235;
      data[idx + 3] = 255;
    }
  }

  const normalTex = new THREE.DataTexture(data, size, size, THREE.RGBAFormat);
  normalTex.wrapS = THREE.RepeatWrapping;
  normalTex.wrapT = THREE.RepeatWrapping;
  normalTex.repeat.set(28, 28);
  normalTex.needsUpdate = true;
  return normalTex;
}

/**
 * Builds a tailored, multi-part anatomical 3D football jersey group whose UV coordinates
 * map directly onto the OpenKitMaker 2048x2048 UV Atlas:
 * - Front Torso -> u: [0.0..0.5], v: [0.336..1.0]
 * - Back Torso  -> u: [0.5..1.0], v: [0.336..1.0]
 * - Left Sleeve -> u: [0.0..0.332], v: [0.0..0.336]
 * - Right Sleeve -> u: [0.5..0.832], v: [0.0..0.336]
 * - Collar -> u: [0.332..0.5], v: [0.0..0.336]
 */
function buildTailoredJerseyMeshGroup(
  kitMaterial: THREE.MeshPhysicalMaterial,
  standMaterial: THREE.MeshStandardMaterial
): THREE.Group {
  const group = new THREE.Group();

  // 1. Anatomical Torso Shell (high-resolution cylinder with sculpted chest, waist taper, and shoulders)
  const radSegs = 64;
  const heightSegs = 48;
  const torsoHeight = 2.45;
  const torsoGeo = new THREE.CylinderGeometry(0.76, 0.72, torsoHeight, radSegs, heightSegs, true);

  const posAttr = torsoGeo.attributes.position;
  const uvAttr = torsoGeo.attributes.uv;
  const vertex = new THREE.Vector3();

  for (let i = 0; i < posAttr.count; i++) {
    vertex.fromBufferAttribute(posAttr, i);
    const normY = (vertex.y + torsoHeight / 2) / torsoHeight; // 0 (bottom hem) .. 1 (shoulders)
    const angle = Math.atan2(vertex.x, vertex.z); // 0 = front (+Z), PI/-PI = back (-Z)

    // Anatomical athletic silhouette shaping
    const waistTaper = 1.0 - 0.11 * Math.sin(normY * Math.PI);
    const chestExpand = 1.0 + 0.16 * Math.pow(normY, 1.35);
    const latWidth = 1.18 * waistTaper * chestExpand;
    const chestDepth = (0.72 + 0.08 * Math.sin(normY * Math.PI)) * waistTaper;

    // Subtle fabric drape folds near waist
    const drapeRipple = 0.008 * Math.sin(angle * 8 + normY * 6) * (1 - normY);

    vertex.x = Math.sin(angle) * latWidth + drapeRipple;
    vertex.z = Math.cos(angle) * chestDepth + drapeRipple;

    // Gentle shoulder slope at the very top
    if (normY > 0.86) {
      const shoulderFactor = (normY - 0.86) / 0.14;
      const sideDist = Math.abs(Math.sin(angle));
      vertex.y -= shoulderFactor * sideDist * 0.22;
      vertex.x *= 1.0 - shoulderFactor * 0.12;
      vertex.z *= 1.0 - shoulderFactor * 0.18;
    }

    posAttr.setXYZ(i, vertex.x, vertex.y, vertex.z);

    // Map UVs to Front Torso [0..0.5] and Back Torso [0.5..1.0]
    // Canvas y: 0..1360 out of 2048 -> Three.js v (flipY=true): 1.0 at top (y=0), 0.336 at bottom (y=1360)
    const vCoord = 0.336 + normY * (1.0 - 0.336);
    if (Math.cos(angle) >= 0) {
      // Front half: angle in [-PI/2 .. +PI/2]
      const uNorm = (Math.sin(angle) + 1) * 0.5; // 0 (left) .. 1 (right)
      const uCoord = 0.02 + uNorm * 0.46;
      uvAttr.setXY(i, uCoord, vCoord);
    } else {
      // Back half
      const uNorm = (-Math.sin(angle) + 1) * 0.5;
      const uCoord = 0.52 + uNorm * 0.46;
      uvAttr.setXY(i, uCoord, vCoord);
    }
  }

  torsoGeo.computeVertexNormals();
  const torsoMesh = new THREE.Mesh(torsoGeo, kitMaterial);
  torsoMesh.castShadow = true;
  torsoMesh.receiveShadow = true;
  group.add(torsoMesh);

  // 2. Left & Right Tailored Raglan Short Sleeves
  const createSleeveGeometry = (isLeft: boolean): THREE.BufferGeometry => {
    const sleeveLen = 0.88;
    const geo = new THREE.CylinderGeometry(0.34, 0.27, sleeveLen, 32, 16, true);
    const p = geo.attributes.position;
    const uvs = geo.attributes.uv;

    for (let i = 0; i < p.count; i++) {
      const u = uvs.getX(i);
      const v = uvs.getY(i);
      // Map to Sleeve UV islands at bottom of 2048x2048 canvas (v in [0.01..0.32])
      const uStart = isLeft ? 0.01 : 0.51;
      const uSpan = 0.31;
      uvs.setXY(i, uStart + u * uSpan, 0.02 + v * 0.30);
    }

    geo.rotateZ(isLeft ? -Math.PI * 0.32 : Math.PI * 0.32);
    geo.translate(isLeft ? 1.02 : -1.02, 0.72, 0.0);
    geo.computeVertexNormals();
    return geo;
  };

  const leftSleeve = new THREE.Mesh(createSleeveGeometry(true), kitMaterial);
  leftSleeve.castShadow = true;
  leftSleeve.receiveShadow = true;
  group.add(leftSleeve);

  const rightSleeve = new THREE.Mesh(createSleeveGeometry(false), kitMaterial);
  rightSleeve.castShadow = true;
  rightSleeve.receiveShadow = true;
  group.add(rightSleeve);

  // 3. Sculpted Collar Ring & Neckband
  const collarGeo = new THREE.TorusGeometry(0.42, 0.068, 20, 48);
  collarGeo.scale(1.12, 0.72, 1.0);
  collarGeo.rotateX(Math.PI * 0.46);
  collarGeo.translate(0, 1.19, 0.03);

  const collarUv = collarGeo.attributes.uv;
  for (let i = 0; i < collarUv.count; i++) {
    const u = collarUv.getX(i);
    const v = collarUv.getY(i);
    // Map to Collar UV zone [0.34..0.49, 0.02..0.32]
    collarUv.setXY(i, 0.34 + u * 0.15, 0.02 + v * 0.30);
  }
  collarGeo.computeVertexNormals();
  const collarMesh = new THREE.Mesh(collarGeo, kitMaterial);
  collarMesh.castShadow = true;
  group.add(collarMesh);

  // 4. Inner Neck Darkness Cap & Minimalist Tailor Mannequin Stand
  const neckCapGeo = new THREE.CylinderGeometry(0.39, 0.41, 0.25, 32);
  neckCapGeo.scale(1.1, 1.0, 0.72);
  neckCapGeo.translate(0, 1.12, 0.02);
  const neckCap = new THREE.Mesh(neckCapGeo, standMaterial);
  group.add(neckCap);

  // Lower Hem Trim Ring
  const hemGeo = new THREE.TorusGeometry(0.75, 0.026, 12, 56);
  hemGeo.scale(1.14, 1.0, 0.72);
  hemGeo.rotateX(Math.PI / 2);
  hemGeo.translate(0, -1.21, 0);
  const hemUv = hemGeo.attributes.uv;
  for (let i = 0; i < hemUv.count; i++) {
    hemUv.setXY(i, 0.84 + hemUv.getX(i) * 0.14, 0.02 + hemUv.getY(i) * 0.30);
  }
  const hemMesh = new THREE.Mesh(hemGeo, kitMaterial);
  group.add(hemMesh);

  // Brushed metallic mannequin pole & base
  const poleGeo = new THREE.CylinderGeometry(0.055, 0.055, 0.9, 24);
  poleGeo.translate(0, -1.65, 0);
  const poleMesh = new THREE.Mesh(poleGeo, standMaterial);
  group.add(poleMesh);

  const baseDiscGeo = new THREE.CylinderGeometry(0.68, 0.74, 0.06, 48);
  baseDiscGeo.translate(0, -2.08, 0);
  const baseDiscMesh = new THREE.Mesh(baseDiscGeo, standMaterial);
  baseDiscMesh.receiveShadow = true;
  group.add(baseDiscMesh);

  return group;
}

/**
 * FUN-PREVIEW-3D: Initializes the WebGL2 Three.js PBR viewport with KTX2Loader & DRACOLoader,
 * constrained orbital camera, Studio/Stadium lighting presets, and real-time 60 FPS loop.
 */
function createFallbackCanvas(): HTMLCanvasElement {
  const c = document.createElement('canvas');
  c.width = 512;
  c.height = 512;
  const ctx = c.getContext('2d');
  if (ctx) {
    ctx.fillStyle = '#0F52BA';
    ctx.fillRect(0, 0, 512, 512);
    ctx.fillStyle = '#09111E';
    ctx.fillRect(0, 340, 512, 172);
  }
  return c;
}

/**
 * FUN-PREVIEW-3D: Initializes the WebGL2 Three.js PBR viewport with KTX2Loader & DRACOLoader,
 * constrained orbital camera, Studio/Stadium lighting presets, and real-time 60 FPS loop.
 */
export function createKit3DEngine(
  container: HTMLElement,
  sourceCanvas2048: HTMLCanvasElement | null,
  initialLighting: LightingPreset = 'studio'
): Kit3DEngineController {
  // Detect software WebGL per Three.js skill guidelines
  const isSoftwareGl = (() => {
    try {
      const probeCanvas = document.createElement('canvas');
      const gl = probeCanvas.getContext('webgl2') || probeCanvas.getContext('webgl');
      if (!gl) return true;
      const ext = gl.getExtension('WEBGL_debug_renderer_info');
      const param = ext ? ext.UNMASKED_RENDERER_WEBGL : gl.RENDERER;
      const gpu = String(gl.getParameter(param));
      return /SwiftShader|llvmpipe|softpipe|Software|Basic Render/i.test(gpu);
    } catch {
      return false;
    }
  })();

  const renderer = new THREE.WebGLRenderer({
    antialias: !isSoftwareGl,
    powerPreference: 'high-performance',
    alpha: false,
  });
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.1;
  renderer.setPixelRatio(isSoftwareGl ? 1 : Math.min(window.devicePixelRatio || 1, 2));
  renderer.shadowMap.enabled = !isSoftwareGl;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;

  const getW = () => Math.max(300, container.clientWidth || container.parentElement?.clientWidth || 400);
  const getH = () => Math.max(300, container.clientHeight || container.parentElement?.clientHeight || 460);

  const initialW = getW();
  const initialH = getH();
  renderer.setSize(initialW, initialH, false);
  renderer.domElement.style.width = '100%';
  renderer.domElement.style.height = '100%';
  renderer.domElement.style.display = 'block';
  container.innerHTML = '';
  container.appendChild(renderer.domElement);

  // Safe loader initialization
  let ktx2Loader: KTX2Loader | null = null;
  let dracoLoader: DRACOLoader | null = null;
  try {
    ktx2Loader = new KTX2Loader();
    ktx2Loader.setTranscoderPath('/basis/');
    ktx2Loader.detectSupport(renderer);
  } catch {
    ktx2Loader = null;
  }

  try {
    dracoLoader = new DRACOLoader();
    dracoLoader.setDecoderPath('/draco/');
  } catch {
    dracoLoader = null;
  }

  const gltfLoader = new GLTFLoader();
  if (ktx2Loader) gltfLoader.setKTX2Loader(ktx2Loader);
  if (dracoLoader) gltfLoader.setDRACOLoader(dracoLoader);

  const scene = new THREE.Scene();
  scene.background = new THREE.Color('#0F141C');
  scene.fog = new THREE.FogExp2('#0F141C', 0.085);

  const pmremGenerator = new THREE.PMREMGenerator(renderer);
  scene.environment = pmremGenerator.fromScene(new RoomEnvironment(), 0.04).texture;
  pmremGenerator.dispose();

  const camera = new THREE.PerspectiveCamera(38, initialW / initialH, 0.1, 50);
  camera.position.set(0, 0.25, 4.6);

  const controls = new OrbitControls(camera, renderer.domElement);
  controls.target.set(0, 0.05, 0);
  controls.enableDamping = true;
  controls.dampingFactor = 0.07;
  controls.enablePan = false;
  controls.minDistance = 2.4;
  controls.maxDistance = 7.5;
  // Constrained pitch as specified in SCR-EDITOR / FUN-PREVIEW-3D
  controls.minPolarAngle = Math.PI * 0.22;
  controls.maxPolarAngle = Math.PI * 0.75;
  controls.update();

  // Dynamic 2048x2048 CanvasTexture bound to the 2D compositor (with fallback)
  const effectiveCanvas = sourceCanvas2048 || createFallbackCanvas();
  const kitTexture = new THREE.CanvasTexture(effectiveCanvas);
  kitTexture.colorSpace = THREE.SRGBColorSpace;
  kitTexture.anisotropy = renderer.capabilities.getMaxAnisotropy();
  kitTexture.needsUpdate = true;

  const normalTexture = createProceduralFabricNormalMap();

  const kitMaterial = new THREE.MeshPhysicalMaterial({
    map: kitTexture,
    normalMap: normalTexture,
    normalScale: new THREE.Vector2(0.28, 0.28),
    roughness: 0.58,
    metalness: 0.04,
    sheen: 0.65,
    sheenRoughness: 0.5,
    sheenColor: new THREE.Color('#FFFFFF'),
    clearcoat: 0.06,
    clearcoatRoughness: 0.4,
    side: THREE.DoubleSide,
  });

  const standMaterial = new THREE.MeshStandardMaterial({
    color: new THREE.Color('#1E293B'),
    roughness: 0.3,
    metalness: 0.8,
  });

  const jerseyGroup = buildTailoredJerseyMeshGroup(kitMaterial, standMaterial);
  scene.add(jerseyGroup);

  // Soft contact shadow ground plane
  const shadowPlaneGeo = new THREE.PlaneGeometry(10, 10);
  const shadowPlaneMat = new THREE.ShadowMaterial({ opacity: 0.38 });
  const shadowPlane = new THREE.Mesh(shadowPlaneGeo, shadowPlaneMat);
  shadowPlane.rotation.x = -Math.PI / 2;
  shadowPlane.position.y = -2.11;
  shadowPlane.receiveShadow = true;
  scene.add(shadowPlane);

  // Ambient & Multi-directional lighting rig ensuring model is NEVER black or in complete darkness
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
  scene.add(ambientLight);

  const hemiLight = new THREE.HemisphereLight(0xffffff, 0x1a2230, 0.65);
  scene.add(hemiLight);

  const keyLight = new THREE.DirectionalLight(kelvinToColor(6500), 1.3);
  keyLight.position.set(3.2, 4.5, 4.2);
  keyLight.castShadow = !isSoftwareGl;
  keyLight.shadow.mapSize.width = 1024;
  keyLight.shadow.mapSize.height = 1024;
  keyLight.shadow.bias = -0.0005;
  keyLight.shadow.normalBias = 0.02;
  scene.add(keyLight);

  const fillLight = new THREE.DirectionalLight(0x94a3b8, 0.65);
  fillLight.position.set(-3.5, 2.0, 2.5);
  scene.add(fillLight);

  const rimLight = new THREE.DirectionalLight(0x00c49f, 0.85);
  rimLight.position.set(0, 3.2, -4.0);
  scene.add(rimLight);

  const applyLightingPreset = (preset: LightingPreset) => {
    if (preset === 'stadium') {
      keyLight.color.copy(kelvinToColor(5400));
      keyLight.intensity = 1.8;
      rimLight.color.set('#38BDF8');
      rimLight.intensity = 1.25;
      hemiLight.intensity = 0.85;
      ambientLight.intensity = 0.95;
      scene.background = new THREE.Color('#090E17');
    } else {
      keyLight.color.copy(kelvinToColor(6500));
      keyLight.intensity = 1.3;
      rimLight.color.set('#00C49F');
      rimLight.intensity = 0.75;
      hemiLight.intensity = 0.65;
      ambientLight.intensity = 0.85;
      scene.background = new THREE.Color('#0F141C');
    }
  };

  applyLightingPreset(initialLighting);

  // Smooth camera transitions and animation loop
  let targetCamPos: THREE.Vector3 | null = null;
  controls.addEventListener('start', () => {
    targetCamPos = null;
  });

  let autoRotate = false;
  let currentFps = 60;
  let frameCount = 0;
  let lastFpsStamp = performance.now();
  let animFrameId = 0;
  let disposed = false;

  const clock = new THREE.Clock();

  function animateLoop(now: number) {
    if (disposed) return;
    animFrameId = requestAnimationFrame(animateLoop);

    const dt = Math.min(clock.getDelta(), 0.1);
    frameCount++;
    if (now - lastFpsStamp >= 500) {
      currentFps = Math.min(60, Math.round((frameCount * 1000) / (now - lastFpsStamp)));
      frameCount = 0;
      lastFpsStamp = now;
    }

    if (autoRotate) {
      jerseyGroup.rotation.y += dt * 0.65;
    }

    if (targetCamPos) {
      camera.position.lerp(targetCamPos, 1 - Math.exp(-8 * dt));
      if (camera.position.distanceTo(targetCamPos) < 0.02) {
        targetCamPos = null;
      }
    }

    controls.update();
    renderer.render(scene, camera);
  }

  animFrameId = requestAnimationFrame(animateLoop);

  return {
    updateCanvasTexture(newCanvas: HTMLCanvasElement) {
      if (!newCanvas) return;
      if (kitTexture.image !== newCanvas) {
        kitTexture.image = newCanvas;
      }
      kitTexture.needsUpdate = true;
    },
    setLightingPreset(preset: LightingPreset) {
      applyLightingPreset(preset);
    },
    setFinishType(finish: 'MATTE_MESH' | 'PRO_VAPOR' | 'RETRO_SATIN') {
      if (finish === 'MATTE_MESH') {
        kitMaterial.roughness = 0.78;
        kitMaterial.clearcoat = 0.0;
        kitMaterial.sheen = 0.35;
      } else if (finish === 'RETRO_SATIN') {
        kitMaterial.roughness = 0.34;
        kitMaterial.clearcoat = 0.22;
        kitMaterial.sheen = 0.9;
      } else {
        kitMaterial.roughness = 0.56;
        kitMaterial.clearcoat = 0.08;
        kitMaterial.sheen = 0.65;
      }
      kitMaterial.needsUpdate = true;
    },
    setAutoRotate(enabled: boolean) {
      autoRotate = enabled;
    },
    resetCamera() {
      jerseyGroup.rotation.set(0, 0, 0);
      targetCamPos = new THREE.Vector3(0, 0.25, 4.6);
    },
    setCameraAngle(view: 'FRONT' | 'BACK' | 'LEFT' | 'RIGHT') {
      jerseyGroup.rotation.set(0, 0, 0);
      switch (view) {
        case 'FRONT':
          targetCamPos = new THREE.Vector3(0, 0.25, 4.6);
          break;
        case 'BACK':
          targetCamPos = new THREE.Vector3(0, 0.25, -4.6);
          break;
        case 'LEFT':
          targetCamPos = new THREE.Vector3(4.4, 0.25, 0);
          break;
        case 'RIGHT':
          targetCamPos = new THREE.Vector3(-4.4, 0.25, 0);
          break;
      }
    },
    resize() {
      if (disposed) return;
      const w = getW();
      const h = getH();
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h, false);
    },
    getFps() {
      return currentFps;
    },
    getLoaderCapabilities() {
      return {
        ktx2Ready: Boolean(ktx2Loader),
        dracoReady: Boolean(dracoLoader),
        isSoftwareGl,
      };
    },
    dispose() {
      disposed = true;
      cancelAnimationFrame(animFrameId);
      controls.dispose();
      ktx2Loader?.dispose();
      dracoLoader?.dispose();
      kitTexture.dispose();
      normalTexture.dispose();
      kitMaterial.dispose();
      standMaterial.dispose();
      renderer.dispose();
    },
  };
}

