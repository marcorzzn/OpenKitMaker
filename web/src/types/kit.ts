// Implements: SCR-HOME, SCR-CATALOG, SCR-EDITOR, SCR-SAVED, SCR-EXPORT / FUN-SELECT-TEMPLATE, FUN-MANAGE-LAYERS, FUN-EDIT-COLOR, FUN-EXPORT-ZIP

export type ScreenId =
  | 'SCR-HOME'
  | 'SCR-CATALOG'
  | 'SCR-EDITOR'
  | 'SCR-SAVED'
  | 'SCR-EXPORT';

export type KitCategory = 'HOME' | 'AWAY' | 'THIRD' | 'GK' | 'SPECIAL';

export type LayerType =
  | 'BASE'
  | 'PATTERN'
  | 'COLLAR'
  | 'SLEEVES'
  | 'SPONSOR'
  | 'BADGE'
  | 'TEXT_NAME'
  | 'NUMBER';

export type BlendMode = 'NORMAL' | 'MULTIPLY' | 'OVERLAY' | 'SCREEN';

export type PatternStyle =
  | 'VERTICAL_STRIPES'
  | 'HORIZONTAL_HOOPS'
  | 'DIAGONAL_SASH'
  | 'CHEVRON_BREAST'
  | 'HALVES'
  | 'CHECKERED_GEOMETRIC'
  | 'CROSS_GRID'
  | 'PINSTRIPES'
  | 'RADIAL_RAYS'
  | 'HEXAGONS'
  | 'WAVES'
  | 'CAMOUFLAGE'
  | 'ABSTRACT_GEOMETRIC';

export type CollarStyle =
  | 'ROUND_CREW'
  | 'V_NECK'
  | 'POLO_FOLDED'
  | 'MANDARIN_TRIM';

export type SponsorGeometry =
  | 'GEOMETRIC_WORDMARK'
  | 'HEX_TECH_BAR'
  | 'TRIPLE_DIAMOND'
  | 'ABSTRACT_WAVE';

export type BadgeShape =
  | 'SHIELD_CLASSIC'
  | 'HEXAGON_PRO'
  | 'CIRCLE_MONO'
  | 'DIAMOND_CREST';

export type BadgePosition = 'LEFT_CHEST' | 'CENTER_CHEST' | 'RIGHT_CHEST';

export type LightingPreset = 'studio' | 'stadium' | 'sunset' | 'radiant';
export type ViewportBackground = 'dark' | 'neutral' | 'pitch';
export type CameraPreset = 'FRONT' | 'BACK' | 'LEFT' | 'RIGHT';
export type FabricType = 'MESH' | 'POLYESTER' | 'COTTON' | 'JACQUARD';
export type ExportResolution = 2048 | 4096;
export type ExportFormatType = 'PNG_2K' | 'PNG_4K' | 'ZIP_FULL_KIT' | 'PBR_BUNDLE' | 'GLB_3D' | 'SOCIAL_1080';

export interface Decal {
  id: string;
  shape: 'HEXAGON' | 'SHIELD' | 'DIAMOND' | 'CIRCLE' | 'CHEVRON' | 'STAR_8';
  u: number;
  v: number;
  scale: number;
  rotation: number;
  color: string;
  strokeColor: string;
}

export interface LayerProperties {
  // PATTERN
  patternStyle?: PatternStyle;
  stripeCount?: number;
  stripeWidth?: number;
  angle?: number;
  patternScale?: number;
  patternRotation?: number;

  // COLLAR
  collarStyle?: CollarStyle;
  trimWidth?: number;

  // SLEEVES
  cuffWidth?: number;
  raglanCut?: boolean;

  // SPONSOR (strictly geometric/abstract placeholders)
  sponsorGeometry?: SponsorGeometry;
  sponsorLabel?: string;
  scale?: number;
  offsetY?: number;
  offsetX?: number;
  posX?: number;
  posY?: number;
  snappedToGuide?: boolean;

  // BADGE (strictly geometric crest)
  badgeShape?: BadgeShape;
  badgePosition?: BadgePosition;
  innerRing?: boolean;

  // TEXT_NAME
  text?: string;
  curveRadius?: number; // 0 = straight, >0 = curved arc radius in px (2048 canvas)
  fontSize?: number;
  strokeWidth?: number;

  // NUMBER
  numberText?: string;
  showFrontNumber?: boolean;
  innerLineOffset?: number;

  [key: string]: unknown;
}

export interface Layer {
  id: string;
  name: string;
  type: LayerType;
  visible: boolean;
  opacity: number; // 0.0 .. 1.0
  zIndex: number;
  color: string; // "#RRGGBB" or "#RRGGBBAA"
  secondaryColor?: string;
  blendMode: BlendMode;
  properties: LayerProperties;
}

export interface KitMetadata {
  author: string;
  version: string;
  description?: string;
  tags: string[];
  thumbnailDataUrl?: string; // 512x512 minikit PNG data URL
  finishType: 'MATTE_MESH' | 'PRO_VAPOR' | 'RETRO_SATIN';
  fabricType?: FabricType;
  lightingPreset: LightingPreset;
  viewportBackground?: ViewportBackground;
  showAnatomicalView?: boolean;
  decals?: Decal[];
}

export type GarmentType = 'JERSEY' | 'SHORTS' | 'SOCKS';

export interface Garment {
  type: GarmentType;
  layers: Layer[];
  baseColor: string;
  secondaryColor?: string;
}

export interface KitGarments {
  jersey: Garment;
  shorts: Garment;
  socks: Garment;
}

export interface Kit {
  id: string; // UUID v4
  name: string;
  category: KitCategory;
  createdAt: string; // ISO-8601 UTC
  updatedAt: string; // ISO-8601 UTC
  templateId: string;
  garments?: KitGarments;
  layers: Layer[]; // Ordered bottom to top (active jersey layers / backward compatibility)
  metadata: KitMetadata;
}

export interface Template {
  id: string;
  name: string;
  category: KitCategory;
  description: string;
  basePreviewColor: string;
  accentPreviewColor: string;
  secondaryPreviewColor: string;
  patternSummary: string;
  defaultLayers: Omit<Layer, 'id'>[];
}

export interface KitManifestV1 {
  $schema: 'https://openkitmaker.dev/schemas/kit-manifest-v1.json';
  version: '1.0.0';
  kitId: string;
  name: string;
  category: KitCategory;
  exportTimestamp: string;
  generator: {
    platform: 'OpenKitMaker-Web' | 'OpenKitMaker-Native';
    version: string;
    buildNumber: string;
  };
  dimensions: {
    width: 2048;
    height: 2048;
  };
  checksums: {
    textureSha256: string;
    previewSha256: string;
    jerseySha256?: string;
    shortsSha256?: string;
    socksSha256?: string;
  };
  kitData: {
    templateId: string;
    layers: Layer[];
    garments?: KitGarments;
    metadata?: KitMetadata;
  };
}

export type EditorViewMode = '2D' | '3D' | 'SPLIT';

