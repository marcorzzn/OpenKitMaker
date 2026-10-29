// Implements: SCR-EDITOR, SCR-EXPORT / FUN-COMPOSITE-2D, FUN-EXPORT-PNG
import type {
  Kit,
  Layer,
  BlendMode,
  PatternStyle,
  CollarStyle,
  SponsorGeometry,
  BadgeShape,
  BadgePosition,
  Garment,
  GarmentType,
} from '../types/kit';
import { ensureKitGarments } from '../data/templates';

export const TEXTURE_SIZE = 2048;
export const PREVIEW_SIZE = 512;

/**
 * Standard UV Atlas Regions on the 2048x2048 Canvas:
 * - Front Torso: x=0..1024, y=0..1360
 * - Back Torso:  x=1024..2048, y=0..1360
 * - Left Sleeve & Collar Front: x=0..1024, y=1360..2048
 * - Right Sleeve & Collar Back: x=1024..2048, y=1360..2048
 */
export const UV_REGIONS = {
  front: { x: 0, y: 0, w: 1024, h: 1360 },
  back: { x: 1024, y: 0, w: 1024, h: 1360 },
  sleeveLeft: { x: 0, y: 1360, w: 680, h: 688 },
  sleeveRight: { x: 1024, y: 1360, w: 680, h: 688 },
  collarZone: { x: 680, y: 1360, w: 344, h: 688 },
  trimZone: { x: 1704, y: 1360, w: 344, h: 688 },
} as const;

function mapBlendMode(mode: BlendMode): GlobalCompositeOperation {
  switch (mode) {
    case 'MULTIPLY':
      return 'multiply';
    case 'OVERLAY':
      return 'overlay';
    case 'SCREEN':
      return 'screen';
    case 'NORMAL':
    default:
      return 'source-over';
  }
}

/**
 * Draws the subtle technical micro-mesh fabric weave across the 2048x2048 canvas.
 */
function drawFabricMicroWeave(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  finishType: string
): void {
  ctx.save();
  ctx.globalAlpha = finishType === 'PRO_VAPOR' ? 0.055 : finishType === 'RETRO_SATIN' ? 0.03 : 0.04;
  ctx.strokeStyle = '#FFFFFF';
  ctx.lineWidth = 2;

  const step = finishType === 'PRO_VAPOR' ? 32 : 24;
  ctx.beginPath();
  for (let y = 0; y < TEXTURE_SIZE; y += step) {
    ctx.moveTo(0, y);
    ctx.lineTo(TEXTURE_SIZE, y);
  }
  for (let x = 0; x < TEXTURE_SIZE; x += step) {
    ctx.moveTo(x, 0);
    ctx.lineTo(x, TEXTURE_SIZE);
  }
  ctx.stroke();
  ctx.restore();
}

function renderBaseLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  ctx.fillStyle = layer.color || '#0F52BA';
  ctx.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);

  if (layer.secondaryColor) {
    // Subtle lower torso hem accent band on Front and Back
    ctx.fillStyle = layer.secondaryColor;
    ctx.fillRect(0, 1296, 2048, 64);
  }
}

function renderPatternInRect(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer,
  rx: number,
  ry: number,
  rw: number,
  rh: number,
  isBack: boolean
): void {
  const style: PatternStyle =
    (layer.properties.patternStyle as PatternStyle) || 'VERTICAL_STRIPES';
  const count = Math.max(1, Math.min(20, Number(layer.properties.stripeCount ?? 5)));
  const ratio = Math.max(0.1, Math.min(0.9, Number(layer.properties.stripeWidth ?? 0.48)));
  const angleDeg = Number(layer.properties.angle ?? 0);

  ctx.save();
  ctx.beginPath();
  ctx.rect(rx, ry, rw, rh);
  ctx.clip();

  ctx.fillStyle = layer.color || '#09111E';
  const secColor = layer.secondaryColor;

  if (style === 'VERTICAL_STRIPES') {
    const bandPitch = rw / (count * 2 - 1);
    const stripeW = bandPitch * ratio * 2;
    for (let i = 0; i < count; i++) {
      const cx = rx + (i * 2 + 0.5) * bandPitch;
      const sx = cx - stripeW / 2;
      ctx.fillStyle = layer.color;
      ctx.fillRect(sx, ry, stripeW, rh);
      if (secColor) {
        ctx.fillStyle = secColor;
        ctx.fillRect(sx - 6, ry, 6, rh);
        ctx.fillRect(sx + stripeW, ry, 6, rh);
      }
    }
  } else if (style === 'HORIZONTAL_HOOPS') {
    const bandPitch = rh / (count * 2);
    const hoopH = bandPitch * ratio * 2;
    for (let i = 0; i < count; i++) {
      const sy = ry + (i * 2 + 0.5) * bandPitch;
      ctx.fillStyle = layer.color;
      ctx.fillRect(rx, sy, rw, hoopH);
      if (secColor) {
        ctx.fillStyle = secColor;
        ctx.fillRect(rx, sy - 5, rw, 5);
        ctx.fillRect(rx, sy + hoopH, rw, 5);
      }
    }
  } else if (style === 'DIAGONAL_SASH') {
    const cx = rx + rw / 2;
    const cy = ry + rh / 2;
    const rad = ((isBack ? -angleDeg : angleDeg) * Math.PI) / 180 || (38 * Math.PI) / 180;
    const sashW = rw * Math.max(0.14, ratio * 0.45);
    ctx.translate(cx, cy);
    ctx.rotate(rad);
    ctx.fillStyle = layer.color;
    ctx.fillRect(-sashW / 2, -rh, sashW, rh * 2);
    if (secColor) {
      ctx.fillStyle = secColor;
      ctx.fillRect(-sashW / 2 - 18, -rh, 14, rh * 2);
      ctx.fillRect(sashW / 2 + 4, -rh, 14, rh * 2);
    }
  } else if (style === 'CHEVRON_BREAST') {
    const cx = rx + rw / 2;
    const baseApexY = ry + 540;
    const thickness = 85 * ratio * 2;
    const numChevrons = Math.min(4, count);
    for (let i = 0; i < numChevrons; i++) {
      const yOffset = i * (thickness + 28);
      ctx.fillStyle = i % 2 === 1 && secColor ? secColor : layer.color;
      ctx.beginPath();
      ctx.moveTo(rx, baseApexY - 260 + yOffset);
      ctx.lineTo(cx, baseApexY + yOffset);
      ctx.lineTo(rx + rw, baseApexY - 260 + yOffset);
      ctx.lineTo(rx + rw, baseApexY - 260 + yOffset + thickness);
      ctx.lineTo(cx, baseApexY + yOffset + thickness);
      ctx.lineTo(rx, baseApexY - 260 + yOffset + thickness);
      ctx.closePath();
      ctx.fill();
    }
  } else if (style === 'HALVES') {
    ctx.fillStyle = layer.color;
    if (isBack) {
      ctx.fillRect(rx, ry, rw * ratio, rh);
    } else {
      ctx.fillRect(rx + rw * (1 - ratio), ry, rw * ratio, rh);
    }
    if (secColor) {
      ctx.fillStyle = secColor;
      const midX = isBack ? rx + rw * ratio : rx + rw * (1 - ratio);
      ctx.fillRect(midX - 8, ry, 16, rh);
    }
  } else if (style === 'CHECKERED_GEOMETRIC') {
    const cols = Math.max(4, count);
    const cellW = rw / cols;
    const rows = Math.ceil(rh / cellW);
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        if ((r + c) % 2 === 0) {
          ctx.fillStyle = layer.color;
          ctx.fillRect(rx + c * cellW, ry + r * cellW, cellW, cellW);
        } else if (secColor && (r + c) % 4 === 1) {
          ctx.fillStyle = secColor;
          ctx.fillRect(
            rx + c * cellW + cellW * 0.25,
            ry + r * cellW + cellW * 0.25,
            cellW * 0.5,
            cellW * 0.5
          );
        }
      }
    }
  } else if (style === 'CROSS_GRID') {
    const cx = rx + rw / 2;
    const cy = ry + 480;
    const armW = rw * 0.16 * ratio * 2;
    ctx.fillStyle = layer.color;
    ctx.fillRect(cx - armW / 2, ry, armW, rh);
    ctx.fillRect(rx, cy - armW / 2, rw, armW);
    if (secColor) {
      ctx.strokeStyle = secColor;
      ctx.lineWidth = 10;
      ctx.strokeRect(cx - armW / 2, ry, armW, rh);
      ctx.strokeRect(rx, cy - armW / 2, rw, armW);
    }
  } else if (style === 'PINSTRIPES') {
    const lines = Math.max(6, count * 2);
    const step = rw / (lines + 1);
    ctx.fillStyle = layer.color;
    for (let i = 1; i <= lines; i++) {
      const lx = rx + i * step;
      ctx.fillRect(lx - 4, ry, 8, rh);
      if (secColor && i % 2 === 0) {
        ctx.fillStyle = secColor;
        ctx.fillRect(lx + 8, ry, 4, rh);
        ctx.fillStyle = layer.color;
      }
    }
  } else if (style === 'RADIAL_RAYS') {
    const originX = rx + rw * 0.25;
    const originY = ry + 340;
    const rayCount = Math.max(6, count * 2);
    for (let i = 0; i < rayCount; i++) {
      const a1 = (i * Math.PI * 2) / rayCount;
      const a2 = a1 + ((Math.PI * 2) / rayCount) * ratio;
      ctx.fillStyle = i % 2 === 0 ? layer.color : secColor || layer.color;
      ctx.beginPath();
      ctx.moveTo(originX, originY);
      ctx.arc(originX, originY, 1800, a1, a2);
      ctx.closePath();
      ctx.fill();
    }
  } else if (style === 'HEXAGONS') {
    const scale = Math.max(0.2, Math.min(5, Number(layer.properties.patternScale ?? 1.0)));
    const rotDeg = Number(layer.properties.patternRotation ?? angleDeg);
    const radius = Math.max(16, 72 * scale);
    const cx = rx + rw / 2;
    const cy = ry + rh / 2;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate((rotDeg * Math.PI) / 180);
    ctx.translate(-cx, -cy);
    ctx.strokeStyle = layer.color;
    ctx.lineWidth = Math.max(2, 6 * scale);
    let row = 0;
    for (let y = ry - 200; y < ry + rh + 200; y += radius * 0.866) {
      const offsetX = row % 2 === 1 ? radius * 1.5 : 0;
      for (let x = rx - 200 + offsetX; x < rx + rw + 200; x += radius * 3) {
        drawPolygon(ctx, x, y, radius, 6);
        ctx.stroke();
        if (secColor && (row + Math.floor(x / (radius * 3))) % 3 === 0) {
          ctx.fillStyle = secColor;
          drawPolygon(ctx, x, y, radius * 0.5, 6);
          ctx.fill();
        }
      }
      row++;
    }
    ctx.restore();
  } else if (style === 'WAVES') {
    const scale = Math.max(0.2, Math.min(5, Number(layer.properties.patternScale ?? 1.0)));
    const rotDeg = Number(layer.properties.patternRotation ?? angleDeg);
    const cx = rx + rw / 2;
    const cy = ry + rh / 2;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate((rotDeg * Math.PI) / 180);
    ctx.translate(-cx, -cy);
    const wavesCount = Math.max(3, Math.min(36, Math.round(count * 2 * scale)));
    const waveH = rh / wavesCount;
    const amplitude = waveH * 0.45;
    ctx.lineWidth = Math.max(3, 10 * scale);
    for (let i = 0; i <= wavesCount + 2; i++) {
      const baseWaveY = ry + (i - 1) * waveH;
      ctx.strokeStyle = i % 2 === 0 ? layer.color : secColor || layer.color;
      ctx.beginPath();
      const step = rw / 40;
      for (let x = rx - 100; x <= rx + rw + 100; x += step) {
        const y = baseWaveY + Math.sin(((x - rx) / rw) * Math.PI * 4 * scale) * amplitude;
        if (x === rx - 100) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    }
    ctx.restore();
  } else if (style === 'CAMOUFLAGE') {
    const scale = Math.max(0.2, Math.min(5, Number(layer.properties.patternScale ?? 1.0)));
    const rotDeg = Number(layer.properties.patternRotation ?? angleDeg);
    const cx = rx + rw / 2;
    const cy = ry + rh / 2;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate((rotDeg * Math.PI) / 180);
    ctx.translate(-cx, -cy);
    const cols = Math.max(3, Math.min(20, Math.round(7 * scale)));
    const rows = Math.max(4, Math.min(25, Math.round(11 * scale)));
    const stepX = rw / cols;
    const stepY = rh / rows;
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const pseudoSeed = (r * 17 + c * 31 + (c * r * 13)) % 100;
        if (pseudoSeed > 35) {
          const blobX = rx + (c + 0.5) * stepX + ((pseudoSeed % 7) - 3) * (stepX * 0.08);
          const blobY = ry + (r + 0.5) * stepY + (((pseudoSeed * 3) % 7) - 3) * (stepY * 0.08);
          const rad = (stepX * 0.42) * (0.6 + (pseudoSeed % 40) / 100);
          ctx.fillStyle = pseudoSeed % 2 === 0 ? layer.color : secColor || layer.color;
          ctx.save();
          ctx.translate(blobX, blobY);
          ctx.rotate((pseudoSeed * 11 * Math.PI) / 180);
          ctx.beginPath();
          ctx.ellipse(0, 0, rad * 1.3, rad * 0.8, 0, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();
        }
      }
    }
    ctx.restore();
  } else if (style === 'ABSTRACT_GEOMETRIC') {
    const scale = Math.max(0.2, Math.min(5, Number(layer.properties.patternScale ?? 1.0)));
    const rotDeg = Number(layer.properties.patternRotation ?? angleDeg);
    const cx = rx + rw / 2;
    const cy = ry + rh / 2;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate((rotDeg * Math.PI) / 180);
    ctx.translate(-cx, -cy);
    const diagCount = Math.max(4, Math.min(30, Math.round(count * 2.5 * scale)));
    const diagStep = (rw * 1.5) / diagCount;
    ctx.lineWidth = Math.max(3, 12 * scale);
    for (let i = 0; i < diagCount; i++) {
      ctx.strokeStyle = i % 2 === 0 ? layer.color : secColor || layer.color;
      const xStart = rx - rw * 0.25 + i * diagStep;
      ctx.beginPath();
      ctx.moveTo(xStart, ry - 100);
      ctx.lineTo(xStart + rw * 0.5, ry + rh + 100);
      ctx.stroke();
      if (i % 3 === 0) {
        ctx.fillStyle = i % 2 === 0 ? secColor || layer.color : layer.color;
        const tx = xStart + rw * 0.25;
        const ty = ry + rh * 0.45;
        const tr = rw * 0.04 * scale;
        drawPolygon(ctx, tx, ty, tr, 3);
        ctx.fill();
      }
    }
    ctx.restore();
  }

  ctx.restore();
}

function renderPatternLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  renderPatternInRect(
    ctx,
    layer,
    UV_REGIONS.front.x,
    UV_REGIONS.front.y,
    UV_REGIONS.front.w,
    UV_REGIONS.front.h,
    false
  );
  renderPatternInRect(
    ctx,
    layer,
    UV_REGIONS.back.x,
    UV_REGIONS.back.y,
    UV_REGIONS.back.w,
    UV_REGIONS.back.h,
    true
  );
}

function renderSleevesLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  const cuffWidth = Math.max(12, Math.min(120, Number(layer.properties.cuffWidth ?? 36))) * 2;
  const raglan = Boolean(layer.properties.raglanCut ?? true);

  const sleeves = [UV_REGIONS.sleeveLeft, UV_REGIONS.sleeveRight];
  for (const s of sleeves) {
    ctx.fillStyle = layer.color || '#09111E';
    ctx.fillRect(s.x, s.y, s.w, s.h);

    if (layer.secondaryColor) {
      // Sleeve cuff trim at bottom of sleeve UV island
      ctx.fillStyle = layer.secondaryColor;
      ctx.fillRect(s.x, s.y + s.h - cuffWidth, s.w, cuffWidth);
      // Secondary accent stripe above cuff
      ctx.fillRect(s.x, s.y + s.h - cuffWidth - 22, s.w, 12);
    }
  }

  // If raglan cut, render angled shoulder panels on Front & Back torso upper corners
  if (raglan) {
    const panels = [UV_REGIONS.front, UV_REGIONS.back];
    for (const p of panels) {
      ctx.fillStyle = layer.color || '#09111E';
      // Left shoulder wedge
      ctx.beginPath();
      ctx.moveTo(p.x, p.y);
      ctx.lineTo(p.x + 250, p.y);
      ctx.lineTo(p.x, p.y + 320);
      ctx.closePath();
      ctx.fill();

      // Right shoulder wedge
      ctx.beginPath();
      ctx.moveTo(p.x + p.w, p.y);
      ctx.lineTo(p.x + p.w - 250, p.y);
      ctx.lineTo(p.x + p.w, p.y + 320);
      ctx.closePath();
      ctx.fill();

      if (layer.secondaryColor) {
        ctx.strokeStyle = layer.secondaryColor;
        ctx.lineWidth = 10;
        ctx.beginPath();
        ctx.moveTo(p.x + 250, p.y);
        ctx.lineTo(p.x, p.y + 320);
        ctx.moveTo(p.x + p.w - 250, p.y);
        ctx.lineTo(p.x + p.w, p.y + 320);
        ctx.stroke();
      }
    }
  }
}

function renderCollarLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  const style: CollarStyle = (layer.properties.collarStyle as CollarStyle) || 'V_NECK';
  const trimW = Math.max(8, Math.min(48, Number(layer.properties.trimWidth ?? 16))) * 2;
  const col = layer.color || '#FFFFFF';
  const sec = layer.secondaryColor || '#00C49F';

  // Collar dedicated UV Zone
  const cz = UV_REGIONS.collarZone;
  ctx.fillStyle = col;
  ctx.fillRect(cz.x, cz.y, cz.w, cz.h);
  ctx.fillStyle = sec;
  ctx.fillRect(cz.x, cz.y + cz.h * 0.35, cz.w, trimW);
  ctx.fillRect(cz.x, cz.y + cz.h * 0.7, cz.w, trimW);

  const tz = UV_REGIONS.trimZone;
  ctx.fillStyle = col;
  ctx.fillRect(tz.x, tz.y, tz.w, tz.h);
  ctx.fillStyle = sec;
  ctx.fillRect(tz.x, tz.y + tz.h * 0.45, tz.w, trimW);

  // Front Neckline Geometric Cut
  const fcx = UV_REGIONS.front.x + UV_REGIONS.front.w / 2;
  ctx.save();
  if (style === 'V_NECK') {
    ctx.beginPath();
    ctx.moveTo(fcx - 185, 0);
    ctx.lineTo(fcx, 225);
    ctx.lineTo(fcx + 185, 0);
    ctx.closePath();
    ctx.fillStyle = '#090D16';
    ctx.fill();

    ctx.lineWidth = trimW;
    ctx.strokeStyle = col;
    ctx.stroke();

    ctx.lineWidth = Math.max(6, trimW * 0.35);
    ctx.strokeStyle = sec;
    ctx.stroke();
  } else if (style === 'ROUND_CREW') {
    ctx.beginPath();
    ctx.arc(fcx, -20, 185, 0.12 * Math.PI, 0.88 * Math.PI);
    ctx.lineWidth = trimW;
    ctx.strokeStyle = col;
    ctx.stroke();
    ctx.lineWidth = Math.max(6, trimW * 0.35);
    ctx.strokeStyle = sec;
    ctx.stroke();
  } else if (style === 'POLO_FOLDED') {
    // Classic Polo wings + placket
    ctx.fillStyle = sec;
    ctx.fillRect(fcx - 28, 120, 56, 175);
    ctx.strokeStyle = col;
    ctx.lineWidth = 6;
    ctx.strokeRect(fcx - 28, 120, 56, 175);

    // Left Wing
    ctx.fillStyle = col;
    ctx.beginPath();
    ctx.moveTo(fcx - 190, 0);
    ctx.lineTo(fcx - 24, 145);
    ctx.lineTo(fcx - 110, 215);
    ctx.lineTo(fcx - 235, 70);
    ctx.closePath();
    ctx.fill();
    ctx.strokeStyle = sec;
    ctx.lineWidth = 8;
    ctx.stroke();

    // Right Wing
    ctx.beginPath();
    ctx.moveTo(fcx + 190, 0);
    ctx.lineTo(fcx + 24, 145);
    ctx.lineTo(fcx + 110, 215);
    ctx.lineTo(fcx + 235, 70);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
  } else if (style === 'MANDARIN_TRIM') {
    ctx.beginPath();
    ctx.arc(fcx, -10, 175, 0.1 * Math.PI, 0.9 * Math.PI);
    ctx.lineWidth = trimW * 1.25;
    ctx.strokeStyle = col;
    ctx.stroke();
    // Center notch
    ctx.fillStyle = sec;
    ctx.fillRect(fcx - 18, 120, 36, 70);
  }

  // Back Neckline Trim
  const bcx = UV_REGIONS.back.x + UV_REGIONS.back.w / 2;
  ctx.beginPath();
  ctx.arc(bcx, -30, 175, 0.15 * Math.PI, 0.85 * Math.PI);
  ctx.lineWidth = trimW * 0.85;
  ctx.strokeStyle = col;
  ctx.stroke();
  ctx.restore();
}

function drawPolygon(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  cx: number,
  cy: number,
  r: number,
  sides: number,
  rotOffset = -Math.PI / 2
): void {
  ctx.beginPath();
  for (let i = 0; i < sides; i++) {
    const a = rotOffset + (i * Math.PI * 2) / sides;
    const x = cx + Math.cos(a) * r;
    const y = cy + Math.sin(a) * r;
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  }
  ctx.closePath();
}

function renderBadgeLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  const shape: BadgeShape = (layer.properties.badgeShape as BadgeShape) || 'HEXAGON_PRO';
  const pos: BadgePosition = (layer.properties.badgePosition as BadgePosition) || 'LEFT_CHEST';
  const scale = Math.max(0.5, Math.min(1.6, Number(layer.properties.scale ?? 1.0)));

  // Viewer's right on the front torso corresponds to wearer's LEFT_CHEST
  let cx = UV_REGIONS.front.x + 675;
  if (pos === 'CENTER_CHEST') cx = UV_REGIONS.front.x + 512;
  else if (pos === 'RIGHT_CHEST') cx = UV_REGIONS.front.x + 349;

  const offX = Number(layer.properties.offsetX ?? 0) + Number(layer.properties.posX ?? 0) * 1000;
  const offY = Number(layer.properties.offsetY ?? 0) + Number(layer.properties.posY ?? 0) * 1000;
  cx += offX;
  const cy = UV_REGIONS.front.y + 340 + offY;
  const r = 74 * scale;

  ctx.save();
  ctx.fillStyle = layer.color || '#00C49F';
  ctx.strokeStyle = layer.secondaryColor || '#FFFFFF';
  ctx.lineWidth = 8 * scale;

  if (shape === 'HEXAGON_PRO') {
    drawPolygon(ctx, cx, cy, r, 6);
    ctx.fill();
    ctx.stroke();
    drawPolygon(ctx, cx, cy, r * 0.62, 6);
    ctx.stroke();
    drawPolygon(ctx, cx, cy, r * 0.28, 6);
    ctx.fillStyle = layer.secondaryColor || '#FFFFFF';
    ctx.fill();
  } else if (shape === 'SHIELD_CLASSIC') {
    ctx.beginPath();
    ctx.moveTo(cx - r * 0.85, cy - r * 0.85);
    ctx.lineTo(cx + r * 0.85, cy - r * 0.85);
    ctx.lineTo(cx + r * 0.85, cy + r * 0.1);
    ctx.quadraticCurveTo(cx + r * 0.75, cy + r * 0.75, cx, cy + r * 1.05);
    ctx.quadraticCurveTo(cx - r * 0.75, cy + r * 0.75, cx - r * 0.85, cy + r * 0.1);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Geometric inner diagonal bar
    ctx.beginPath();
    ctx.moveTo(cx - r * 0.48, cy - r * 0.45);
    ctx.lineTo(cx + r * 0.48, cy + r * 0.35);
    ctx.stroke();
  } else if (shape === 'CIRCLE_MONO') {
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.68, 0, Math.PI * 2);
    ctx.stroke();
    // Geometric inner star/diamond
    drawPolygon(ctx, cx, cy, r * 0.38, 4, 0);
    ctx.fillStyle = layer.secondaryColor || '#FFFFFF';
    ctx.fill();
  } else if (shape === 'DIAMOND_CREST') {
    drawPolygon(ctx, cx, cy, r * 1.05, 4, 0);
    ctx.fill();
    ctx.stroke();
    drawPolygon(ctx, cx, cy, r * 0.62, 4, 0);
    ctx.stroke();
  }
  ctx.restore();

  // Also draw small geometric manufacturer placeholder on opposite chest when badge is on side
  if (pos !== 'CENTER_CHEST') {
    const oppX = pos === 'LEFT_CHEST' ? UV_REGIONS.front.x + 349 : UV_REGIONS.front.x + 675;
    ctx.save();
    ctx.fillStyle = layer.secondaryColor || '#FFFFFF';
    drawPolygon(ctx, oppX, cy - 10, 26 * scale, 3, -Math.PI / 2);
    ctx.fill();
    drawPolygon(ctx, oppX + 26 * scale, cy + 8, 20 * scale, 3, -Math.PI / 2);
    ctx.fillStyle = layer.color || '#00C49F';
    ctx.fill();
    ctx.restore();
  }
}

function renderSponsorLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  const geom: SponsorGeometry =
    (layer.properties.sponsorGeometry as SponsorGeometry) || 'HEX_TECH_BAR';
  const label = String(layer.properties.sponsorLabel ?? 'AERO·LAB')
    .slice(0, 14)
    .toUpperCase();
  const scale = Math.max(0.5, Math.min(1.5, Number(layer.properties.scale ?? 1.0)));
  const offsetX = Number(layer.properties.offsetX ?? 0) + Number(layer.properties.posX ?? 0) * 1000;
  const offsetY = Number(layer.properties.offsetY ?? 20) + Number(layer.properties.posY ?? 0) * 1000;

  const cx = UV_REGIONS.front.x + 512 + offsetX;
  const cy = UV_REGIONS.front.y + 560 + offsetY;
  const primary = layer.color || '#FFFFFF';
  const secondary = layer.secondaryColor || '#00C49F';

  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale, scale);

  if (geom === 'HEX_TECH_BAR') {
    // Purely geometric emblem + clean technical typography
    ctx.fillStyle = secondary;
    drawPolygon(ctx, -225, 0, 42, 6, 0);
    ctx.fill();
    ctx.fillStyle = primary;
    drawPolygon(ctx, -225, 0, 20, 6, 0);
    ctx.fill();

    ctx.font = '700 76px "Barlow Condensed", sans-serif';
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = primary;
    ctx.fillText(label, -162, 4);

    // Underline geometric bar
    ctx.fillStyle = secondary;
    ctx.fillRect(-162, 46, 360, 10);
  } else if (geom === 'TRIPLE_DIAMOND') {
    for (let i = -1; i <= 1; i++) {
      ctx.fillStyle = i === 0 ? secondary : primary;
      drawPolygon(ctx, i * 62, -52, 34, 4, 0);
      ctx.fill();
    }
    ctx.font = '700 72px "Barlow Condensed", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = primary;
    ctx.fillText(label, 0, 32);
  } else if (geom === 'GEOMETRIC_WORDMARK') {
    ctx.strokeStyle = secondary;
    ctx.lineWidth = 8;
    ctx.strokeRect(-260, -56, 520, 112);

    ctx.font = '700 80px "Barlow Condensed", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = primary;
    ctx.fillText(label, 0, 4);
  } else if (geom === 'ABSTRACT_WAVE') {
    ctx.strokeStyle = secondary;
    ctx.lineWidth = 14;
    ctx.beginPath();
    ctx.moveTo(-240, -45);
    ctx.bezierCurveTo(-80, -95, 80, 5, 240, -45);
    ctx.stroke();

    ctx.font = '700 76px "Barlow Condensed", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = primary;
    ctx.fillText(label, 0, 26);
  }

  ctx.restore();
}

/**
 * FUN-COMPOSITE-2D: Supports curved arc text and straight text on the dorsal torso.
 */
function renderTextNameLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  const rawText = String(layer.properties.text ?? 'ROSSI')
    .toUpperCase()
    .slice(0, 16);
  if (!rawText.trim()) return;

  const curveRadius = Number(layer.properties.curveRadius ?? 680);
  const fontSize = Math.max(40, Math.min(140, Number(layer.properties.fontSize ?? 86)));
  const strokeWidth = Math.max(0, Math.min(20, Number(layer.properties.strokeWidth ?? 8)));
  const offsetY = Number(layer.properties.offsetY ?? 0);

  const cx = UV_REGIONS.back.x + 512;
  const cy = UV_REGIONS.back.y + 280 + offsetY;

  ctx.save();
  ctx.font = `700 ${fontSize}px "Barlow Condensed", sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillStyle = layer.color || '#FFFFFF';
  ctx.strokeStyle = layer.secondaryColor || '#09111E';
  ctx.lineWidth = strokeWidth;
  ctx.lineJoin = 'round';

  if (curveRadius > 150) {
    // Render character by character along an upper circular arc
    const chars = Array.from(rawText);
    const charSpacingAngle = (fontSize * 0.62) / curveRadius;
    const totalAngle = charSpacingAngle * (chars.length - 1);
    const startAngle = -Math.PI / 2 - totalAngle / 2;
    const arcCenterY = cy + curveRadius;

    chars.forEach((ch, idx) => {
      const theta = startAngle + idx * charSpacingAngle;
      const x = cx + Math.cos(theta) * curveRadius;
      const y = arcCenterY + Math.sin(theta) * curveRadius;

      ctx.save();
      ctx.translate(x, y);
      ctx.rotate(theta + Math.PI / 2);
      if (strokeWidth > 0) {
        ctx.strokeText(ch, 0, 0);
      }
      ctx.fillText(ch, 0, 0);
      ctx.restore();
    });
  } else {
    if (strokeWidth > 0) {
      ctx.strokeText(rawText, cx, cy);
    }
    ctx.fillText(rawText, cx, cy);
  }

  ctx.restore();
}

/**
 * FUN-COMPOSITE-2D: Renders vector athletic numbers with multi-contour outline on Back (and optional Front).
 */
function renderNumberLayer(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  layer: Layer
): void {
  const numStr = String(layer.properties.numberText ?? '10')
    .trim()
    .slice(0, 3);
  if (!numStr) return;

  const fontSize = Math.max(180, Math.min(480, Number(layer.properties.fontSize ?? 360)));
  const strokeWidth = Math.max(0, Math.min(28, Number(layer.properties.strokeWidth ?? 12)));
  const showFront = Boolean(layer.properties.showFrontNumber ?? true);
  const innerLine = Number(layer.properties.innerLineOffset ?? 6);

  const backCx = UV_REGIONS.back.x + 512;
  const backCy = UV_REGIONS.back.y + 640;

  ctx.save();
  ctx.font = `700 ${fontSize}px "Barlow Condensed", sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.lineJoin = 'round';

  // Outer contrast contour
  if (strokeWidth > 0) {
    ctx.lineWidth = strokeWidth * 2;
    ctx.strokeStyle = layer.secondaryColor || '#00C49F';
    ctx.strokeText(numStr, backCx, backCy);
  }

  // Primary fill
  ctx.fillStyle = layer.color || '#FFFFFF';
  ctx.fillText(numStr, backCx, backCy);

  // Subtle geometric inner inline cut
  if (innerLine > 0) {
    ctx.lineWidth = 3;
    ctx.strokeStyle = layer.secondaryColor || '#09111E';
    ctx.strokeText(numStr, backCx, backCy);
  }

  // Small geometric emblem at bottom of dorsal number
  drawPolygon(ctx, backCx, backCy + fontSize * 0.34, 14, 6, 0);
  ctx.fillStyle = layer.secondaryColor || '#00C49F';
  ctx.fill();

  // Optional Front Chest/Abdomen Number
  if (showFront) {
    const frontSize = Math.round(fontSize * 0.34);
    const frontCx = UV_REGIONS.front.x + 512;
    const frontCy = UV_REGIONS.front.y + 790;
    ctx.font = `700 ${frontSize}px "Barlow Condensed", sans-serif`;
    if (strokeWidth > 0) {
      ctx.lineWidth = Math.max(4, strokeWidth * 0.8);
      ctx.strokeStyle = layer.secondaryColor || '#00C49F';
      ctx.strokeText(numStr, frontCx, frontCy);
    }
    ctx.fillStyle = layer.color || '#FFFFFF';
    ctx.fillText(numStr, frontCx, frontCy);
  }

  ctx.restore();
}

export function drawUvPatternOverlay(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D
): void {
  ctx.save();
  ctx.strokeStyle = 'rgba(148, 163, 184, 0.42)';
  ctx.lineWidth = 4;
  ctx.setLineDash([16, 12]);

  // Quadrant borders
  ctx.strokeRect(4, 4, 1016, 1352);
  ctx.strokeRect(1028, 4, 1016, 1352);
  ctx.strokeRect(4, 1364, 672, 680);
  ctx.strokeRect(1028, 1364, 672, 680);
  ctx.strokeRect(684, 1364, 336, 680);
  ctx.strokeRect(1708, 1364, 336, 680);

  // Tailor cut silhouettes on Front & Back panels
  ctx.setLineDash([]);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.24)';
  ctx.lineWidth = 3;

  for (const panel of [UV_REGIONS.front, UV_REGIONS.back]) {
    const px = panel.x;
    const py = panel.y;
    ctx.beginPath();
    ctx.moveTo(px + 280, py + 40);
    ctx.lineTo(px + 744, py + 40);
    ctx.lineTo(px + 940, py + 240);
    ctx.lineTo(px + 860, py + 1310);
    ctx.lineTo(px + 164, py + 1310);
    ctx.lineTo(px + 84, py + 240);
    ctx.closePath();
    ctx.stroke();
  }

  // Tailored sleeve contour outlines and cuff guide lines
  for (const s of [UV_REGIONS.sleeveLeft, UV_REGIONS.sleeveRight]) {
    ctx.beginPath();
    ctx.moveTo(s.x + 80, s.y + 40);
    ctx.lineTo(s.x + s.w - 80, s.y + 40);
    ctx.lineTo(s.x + s.w - 36, s.y + s.h - 40);
    ctx.lineTo(s.x + 36, s.y + s.h - 40);
    ctx.closePath();
    ctx.stroke();

    // Cuff guideline
    ctx.beginPath();
    ctx.moveTo(s.x + 40, s.y + s.h - 90);
    ctx.lineTo(s.x + s.w - 40, s.y + s.h - 90);
    ctx.stroke();
  }

  // Labels
  ctx.font = '600 28px "JetBrains Mono", monospace';
  ctx.fillStyle = 'rgba(255, 255, 255, 0.65)';
  ctx.fillText('UV: TORSO FRONTE (0..1024)', 36, 52);
  ctx.fillText('UV: TORSO RETRO (1024..2048)', 1060, 52);
  ctx.fillText('UV: MANICA SX (0..680, 1360..2048)', 36, 1412);
  ctx.fillText('UV: COLLETTO', 704, 1412);
  ctx.fillText('UV: MANICA DX (1024..1704, 1360..2048)', 1060, 1412);
  ctx.fillText('UV: BORDI/TRIM', 1724, 1412);

  ctx.restore();
}

/**
 * FUN-COMPOSITE-2D: Main 2048x2048 32-bit RGBA Compositor.
 * Executes deterministically in < 12ms to guarantee 60 FPS interactive color/layer updates (TEST-PAR-02).
 */
function renderDecalCrest(
  ctx: CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D,
  primaryColor: string,
  secondaryColor: string
): void {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(0, -90);
  ctx.lineTo(75, -50);
  ctx.lineTo(75, 40);
  ctx.quadraticCurveTo(0, 110, 0, 120);
  ctx.quadraticCurveTo(0, 110, -75, 40);
  ctx.lineTo(-75, -50);
  ctx.closePath();
  ctx.fillStyle = primaryColor;
  ctx.fill();
  ctx.strokeStyle = secondaryColor;
  ctx.lineWidth = 10;
  ctx.stroke();

  ctx.beginPath();
  ctx.arc(0, 0, 32, 0, Math.PI * 2);
  ctx.fillStyle = secondaryColor;
  ctx.fill();
  ctx.restore();
}

function extractGarment(input: Garment | Kit, fallbackType: GarmentType): Garment {
  if ('type' in input && 'layers' in input) {
    return input as Garment;
  }
  const kit = input as Kit;
  const safeGarments = ensureKitGarments(kit);
  return safeGarments[fallbackType.toLowerCase() as 'jersey' | 'shorts' | 'socks'];
}

export function compositeJersey2048(
  targetCanvas: HTMLCanvasElement | OffscreenCanvas,
  garmentOrKit: Garment | Kit,
  options: { finishType?: string; showUvGuides?: boolean } = {}
): void {
  const garment = extractGarment(garmentOrKit, 'JERSEY');
  if (targetCanvas.width !== TEXTURE_SIZE || targetCanvas.height !== TEXTURE_SIZE) {
    targetCanvas.width = TEXTURE_SIZE;
    targetCanvas.height = TEXTURE_SIZE;
  }
  const ctx = targetCanvas.getContext('2d') as
    | CanvasRenderingContext2D
    | OffscreenCanvasRenderingContext2D
    | null;
  if (!ctx) return;
  ctx.clearRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);

  const orderedLayers = [...garment.layers].sort((a, b) => a.zIndex - b.zIndex);
  for (const layer of orderedLayers) {
    if (!layer.visible || layer.opacity <= 0) continue;
    ctx.save();
    ctx.globalAlpha = Math.max(0, Math.min(1, layer.opacity));
    ctx.globalCompositeOperation = mapBlendMode(layer.blendMode);

    switch (layer.type) {
      case 'BASE': renderBaseLayer(ctx, layer); break;
      case 'PATTERN': renderPatternLayer(ctx, layer); break;
      case 'SLEEVES': renderSleevesLayer(ctx, layer); break;
      case 'COLLAR': renderCollarLayer(ctx, layer); break;
      case 'BADGE': renderBadgeLayer(ctx, layer); break;
      case 'SPONSOR': renderSponsorLayer(ctx, layer); break;
      case 'TEXT_NAME': renderTextNameLayer(ctx, layer); break;
      case 'NUMBER': renderNumberLayer(ctx, layer); break;
    }
    ctx.restore();
  }

  drawFabricMicroWeave(ctx, options.finishType || 'PRO_VAPOR');
  if (options.showUvGuides) {
    drawUvPatternOverlay(ctx);
  }
}

export function compositeShorts2048(
  targetCanvas: HTMLCanvasElement | OffscreenCanvas,
  garmentOrKit: Garment | Kit,
  options: { finishType?: string; showUvGuides?: boolean } = {}
): void {
  const garment = extractGarment(garmentOrKit, 'SHORTS');
  if (targetCanvas.width !== TEXTURE_SIZE || targetCanvas.height !== TEXTURE_SIZE) {
    targetCanvas.width = TEXTURE_SIZE;
    targetCanvas.height = TEXTURE_SIZE;
  }
  const ctx = targetCanvas.getContext('2d') as
    | CanvasRenderingContext2D
    | OffscreenCanvasRenderingContext2D
    | null;
  if (!ctx) return;
  ctx.clearRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);

  const orderedLayers = [...garment.layers].sort((a, b) => a.zIndex - b.zIndex);
  for (const layer of orderedLayers) {
    if (!layer.visible || layer.opacity <= 0) continue;
    ctx.save();
    ctx.globalAlpha = Math.max(0, Math.min(1, layer.opacity));
    ctx.globalCompositeOperation = mapBlendMode(layer.blendMode);

    switch (layer.type) {
      case 'BASE':
        ctx.fillStyle = layer.color || '#09111E';
        ctx.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        if (layer.secondaryColor) {
          ctx.fillStyle = layer.secondaryColor;
          ctx.fillRect(40, 1940, 940, 60);
          ctx.fillRect(1068, 1940, 940, 60);
          ctx.fillRect(40, 920, 940, 60);
          ctx.fillRect(1068, 920, 940, 60);
        }
        break;
      case 'PATTERN':
        ctx.fillStyle = layer.color || '#00C49F';
        ctx.fillRect(40, 1060, 140, 940);
        ctx.fillRect(1868, 1060, 140, 940);
        ctx.fillRect(40, 40, 140, 940);
        ctx.fillRect(1868, 40, 140, 940);
        break;
      case 'BADGE':
        ctx.save();
        ctx.translate(500, 1600);
        ctx.scale(0.8, 0.8);
        renderDecalCrest(ctx, layer.color || '#00C49F', layer.secondaryColor || '#FFFFFF');
        ctx.restore();
        break;
      case 'NUMBER':
        ctx.save();
        ctx.translate(1520, 1650);
        ctx.fillStyle = layer.color || '#FFFFFF';
        ctx.font = '700 220px "Barlow Condensed", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(String(layer.properties.numberText || '9'), 0, 0);
        ctx.restore();
        break;
      default:
        break;
    }
    ctx.restore();
  }

  drawFabricMicroWeave(ctx, options.finishType || 'PRO_VAPOR');
}

export function compositeSocks2048(
  targetCanvas: HTMLCanvasElement | OffscreenCanvas,
  garmentOrKit: Garment | Kit,
  options: { finishType?: string; showUvGuides?: boolean } = {}
): void {
  const garment = extractGarment(garmentOrKit, 'SOCKS');
  if (targetCanvas.width !== TEXTURE_SIZE || targetCanvas.height !== TEXTURE_SIZE) {
    targetCanvas.width = TEXTURE_SIZE;
    targetCanvas.height = TEXTURE_SIZE;
  }
  const ctx = targetCanvas.getContext('2d') as
    | CanvasRenderingContext2D
    | OffscreenCanvasRenderingContext2D
    | null;
  if (!ctx) return;
  ctx.clearRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);

  const orderedLayers = [...garment.layers].sort((a, b) => a.zIndex - b.zIndex);
  for (const layer of orderedLayers) {
    if (!layer.visible || layer.opacity <= 0) continue;
    ctx.save();
    ctx.globalAlpha = Math.max(0, Math.min(1, layer.opacity));
    ctx.globalCompositeOperation = mapBlendMode(layer.blendMode);

    switch (layer.type) {
      case 'BASE':
        ctx.fillStyle = layer.color || '#0F52BA';
        ctx.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        break;
      case 'COLLAR':
        ctx.fillStyle = layer.color || '#00C49F';
        ctx.fillRect(40, 40, 940, 320);
        ctx.fillRect(1068, 40, 940, 320);
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
        ctx.lineWidth = 4;
        for (let rx = 50; rx < 980; rx += 24) {
          ctx.beginPath();
          ctx.moveTo(rx, 40);
          ctx.lineTo(rx, 360);
          ctx.stroke();
        }
        for (let rx = 1080; rx < 2000; rx += 24) {
          ctx.beginPath();
          ctx.moveTo(rx, 40);
          ctx.lineTo(rx, 360);
          ctx.stroke();
        }
        break;
      case 'PATTERN':
        ctx.fillStyle = layer.color || '#FFFFFF';
        const hoopH = 75;
        const gapH = 65;
        for (let hy = 580; hy < 1100; hy += hoopH + gapH) {
          ctx.fillRect(40, hy, 940, hoopH);
          ctx.fillRect(1068, hy, 940, hoopH);
        }
        break;
      case 'BADGE':
        ctx.save();
        ctx.translate(512, 1350);
        ctx.scale(0.65, 0.65);
        renderDecalCrest(ctx, layer.color || '#00C49F', layer.secondaryColor || '#FFFFFF');
        ctx.restore();
        ctx.save();
        ctx.translate(1536, 1350);
        ctx.scale(0.65, 0.65);
        renderDecalCrest(ctx, layer.color || '#00C49F', layer.secondaryColor || '#FFFFFF');
        ctx.restore();
        break;
      default:
        break;
    }
    ctx.restore();
  }

  drawFabricMicroWeave(ctx, options.finishType || 'PRO_VAPOR');
}

export function compositeGarment2048(
  targetCanvas: HTMLCanvasElement | OffscreenCanvas,
  garmentOrKit: Garment | Kit,
  garmentTypeOrOptions?: GarmentType | { finishType?: string; showUvGuides?: boolean },
  maybeOptions?: { finishType?: string; showUvGuides?: boolean }
): void {
  let gType: GarmentType = 'JERSEY';
  let options: { finishType?: string; showUvGuides?: boolean } = {};

  if (typeof garmentTypeOrOptions === 'string') {
    gType = garmentTypeOrOptions as GarmentType;
    options = maybeOptions || {};
  } else if (garmentTypeOrOptions && typeof garmentTypeOrOptions === 'object') {
    options = garmentTypeOrOptions;
    if ('type' in garmentOrKit) {
      gType = (garmentOrKit as Garment).type;
    }
  } else if ('type' in garmentOrKit) {
    gType = (garmentOrKit as Garment).type;
  }

  switch (gType) {
    case 'SHORTS':
      compositeShorts2048(targetCanvas, garmentOrKit, options);
      break;
    case 'SOCKS':
      compositeSocks2048(targetCanvas, garmentOrKit, options);
      break;
    case 'JERSEY':
    default:
      compositeJersey2048(targetCanvas, garmentOrKit, options);
      break;
  }
}

export function compositeKit2048(
  targetCanvas: HTMLCanvasElement | OffscreenCanvas,
  kit: Kit,
  options: { garmentType?: GarmentType; showUvGuides?: boolean } = {}
): void {
  const gType = options.garmentType || 'JERSEY';
  if (kit.garments) {
    const garment = kit.garments[gType.toLowerCase() as 'jersey' | 'shorts' | 'socks'];
    if (garment) {
      compositeGarment2048(targetCanvas, garment, {
        finishType: kit.metadata?.finishType,
        showUvGuides: options.showUvGuides,
      });
      return;
    }
  }

  compositeJersey2048(
    targetCanvas,
    {
      type: 'JERSEY',
      layers: kit.layers,
      baseColor: kit.layers[0]?.color || '#0F52BA',
      secondaryColor: kit.layers[0]?.secondaryColor,
    },
    { finishType: kit.metadata?.finishType, showUvGuides: options.showUvGuides }
  );
}

function drawStudioBackdrop(ctx: CanvasRenderingContext2D): void {
  const grad = ctx.createRadialGradient(256, 220, 40, 256, 256, 320);
  grad.addColorStop(0, '#1E293B');
  grad.addColorStop(1, '#0F141C');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);

  ctx.strokeStyle = 'rgba(46, 61, 86, 0.45)';
  ctx.lineWidth = 1;
  for (let i = 64; i < PREVIEW_SIZE; i += 64) {
    ctx.beginPath();
    ctx.moveTo(i, 0);
    ctx.lineTo(i, PREVIEW_SIZE);
    ctx.moveTo(0, i);
    ctx.lineTo(PREVIEW_SIZE, i);
    ctx.stroke();
  }
}

function drawMockupFooter(ctx: CanvasRenderingContext2D, kit: Kit, garmentLabel: string): void {
  ctx.fillStyle = 'rgba(15, 20, 28, 0.88)';
  ctx.fillRect(0, 464, PREVIEW_SIZE, 48);
  ctx.font = '700 17px "Barlow Condensed", sans-serif';
  ctx.fillStyle = '#FFFFFF';
  ctx.fillText(`${kit.name.toUpperCase().slice(0, 24)} · ${garmentLabel}`, 18, 494);

  ctx.font = '500 13px "JetBrains Mono", monospace';
  ctx.fillStyle = '#00C49F';
  ctx.textAlign = 'right';
  ctx.fillText(`${kit.category} · 2048px`, PREVIEW_SIZE - 18, 493);
  ctx.textAlign = 'left';
}

function drawSingleJerseyOnCanvas(
  ctx: CanvasRenderingContext2D,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  cx: number,
  cy: number,
  scale: number,
  isBack: boolean
): void {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale, scale);

  const defineJerseyPath = () => {
    ctx.beginPath();
    ctx.moveTo(-58, -148);
    if (isBack) {
      ctx.quadraticCurveTo(0, -134, 58, -148);
    } else {
      ctx.lineTo(0, -108);
      ctx.lineTo(58, -148);
    }
    ctx.lineTo(106, -136);
    ctx.lineTo(154, -52);
    ctx.lineTo(116, -24);
    ctx.lineTo(92, -66);
    ctx.lineTo(92, 148);
    ctx.lineTo(-92, 148);
    ctx.lineTo(-92, -66);
    ctx.lineTo(-116, -24);
    ctx.lineTo(-154, -52);
    ctx.lineTo(-106, -136);
    ctx.closePath();
  };

  ctx.save();
  ctx.shadowColor = 'rgba(0, 0, 0, 0.55)';
  ctx.shadowBlur = 24;
  ctx.shadowOffsetY = 12;
  defineJerseyPath();
  ctx.fillStyle = '#0F141C';
  ctx.fill();
  ctx.restore();

  defineJerseyPath();
  ctx.clip();

  const srcX = isBack ? UV_REGIONS.back.x : UV_REGIONS.front.x;
  ctx.drawImage(sourceTexture2048 as CanvasImageSource, srcX, 0, 1024, 1360, -105, -152, 210, 304);

  ctx.save();
  ctx.translate(-120, -80);
  ctx.rotate(0.52);
  ctx.drawImage(
    sourceTexture2048 as CanvasImageSource,
    UV_REGIONS.sleeveLeft.x, UV_REGIONS.sleeveLeft.y, UV_REGIONS.sleeveLeft.w, UV_REGIONS.sleeveLeft.h,
    -36, -56, 74, 126
  );
  ctx.restore();

  ctx.save();
  ctx.translate(120, -80);
  ctx.rotate(-0.52);
  ctx.drawImage(
    sourceTexture2048 as CanvasImageSource,
    UV_REGIONS.sleeveRight.x, UV_REGIONS.sleeveRight.y, UV_REGIONS.sleeveRight.w, UV_REGIONS.sleeveRight.h,
    -38, -56, 74, 126
  );
  ctx.restore();

  const shade = ctx.createLinearGradient(-140, 0, 140, 0);
  shade.addColorStop(0, 'rgba(0,0,0,0.28)');
  shade.addColorStop(0.2, 'rgba(255,255,255,0.06)');
  shade.addColorStop(0.5, 'rgba(255,255,255,0)');
  shade.addColorStop(0.8, 'rgba(255,255,255,0.06)');
  shade.addColorStop(1, 'rgba(0,0,0,0.32)');
  ctx.fillStyle = shade;
  ctx.fillRect(-160, -160, 320, 320);

  ctx.restore();

  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale, scale);
  defineJerseyPath();
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.28)';
  ctx.lineWidth = 2.5;
  ctx.stroke();
  ctx.restore();
}

function drawSingleShortsOnCanvas(
  ctx: CanvasRenderingContext2D,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  cx: number,
  cy: number,
  scale: number,
  isBack: boolean
): void {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale, scale);

  const defineShortsPath = () => {
    ctx.beginPath();
    ctx.moveTo(-92, -90);
    ctx.lineTo(92, -90);
    ctx.lineTo(105, 50);
    ctx.lineTo(110, 110);
    ctx.lineTo(24, 110);
    ctx.lineTo(0, 36);
    ctx.lineTo(-24, 110);
    ctx.lineTo(-110, 110);
    ctx.lineTo(-105, 50);
    ctx.closePath();
  };

  ctx.save();
  ctx.shadowColor = 'rgba(0, 0, 0, 0.55)';
  ctx.shadowBlur = 24;
  ctx.shadowOffsetY = 12;
  defineShortsPath();
  ctx.fillStyle = '#0F141C';
  ctx.fill();
  ctx.restore();

  defineShortsPath();
  ctx.clip();

  const srcY = isBack ? 40 : 1060;
  ctx.drawImage(
    sourceTexture2048 as CanvasImageSource,
    0, srcY, 2048, 988,
    -110, -90, 220, 200
  );

  ctx.strokeStyle = 'rgba(0, 0, 0, 0.35)';
  ctx.lineWidth = 4;
  ctx.beginPath();
  ctx.moveTo(-92, -70);
  ctx.lineTo(92, -70);
  ctx.stroke();

  const shade = ctx.createLinearGradient(-110, 0, 110, 0);
  shade.addColorStop(0, 'rgba(0,0,0,0.25)');
  shade.addColorStop(0.2, 'rgba(255,255,255,0.08)');
  shade.addColorStop(0.5, 'rgba(255,255,255,0)');
  shade.addColorStop(0.8, 'rgba(255,255,255,0.08)');
  shade.addColorStop(1, 'rgba(0,0,0,0.3)');
  ctx.fillStyle = shade;
  ctx.fillRect(-120, -100, 240, 220);

  ctx.restore();

  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale, scale);
  defineShortsPath();
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.28)';
  ctx.lineWidth = 2.5;
  ctx.stroke();
  ctx.restore();
}

function drawSingleSockOnCanvas(
  ctx: CanvasRenderingContext2D,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  cx: number,
  cy: number,
  scale: number,
  isRight: boolean
): void {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale * (isRight ? -1 : 1), scale);

  const defineSockPath = () => {
    ctx.beginPath();
    ctx.moveTo(-36, -145);
    ctx.lineTo(36, -145);
    ctx.lineTo(34, 40);
    ctx.lineTo(32, 90);
    ctx.lineTo(76, 126);
    ctx.lineTo(60, 150);
    ctx.lineTo(-14, 115);
    ctx.lineTo(-24, 40);
    ctx.lineTo(-36, -145);
    ctx.closePath();
  };

  ctx.save();
  ctx.shadowColor = 'rgba(0, 0, 0, 0.55)';
  ctx.shadowBlur = 24;
  ctx.shadowOffsetY = 12;
  defineSockPath();
  ctx.fillStyle = '#0F141C';
  ctx.fill();
  ctx.restore();

  defineSockPath();
  ctx.clip();

  const srcX = isRight ? 1068 : 40;
  ctx.drawImage(
    sourceTexture2048 as CanvasImageSource,
    srcX, 40, 940, 1960,
    -36, -145, 112, 295
  );

  ctx.strokeStyle = 'rgba(0, 0, 0, 0.4)';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(-36, -95);
  ctx.lineTo(36, -95);
  ctx.stroke();

  const shade = ctx.createLinearGradient(-36, 0, 36, 0);
  shade.addColorStop(0, 'rgba(0,0,0,0.3)');
  shade.addColorStop(0.3, 'rgba(255,255,255,0.08)');
  shade.addColorStop(0.7, 'rgba(255,255,255,0)');
  shade.addColorStop(1, 'rgba(0,0,0,0.35)');
  ctx.fillStyle = shade;
  ctx.fillRect(-45, -150, 130, 310);

  ctx.restore();

  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(scale * (isRight ? -1 : 1), scale);
  defineSockPath();
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.28)';
  ctx.lineWidth = 2.5;
  ctx.stroke();
  ctx.restore();
}

export function renderJerseyMockup512(
  previewCanvas: HTMLCanvasElement,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  kit: Kit
): string {
  previewCanvas.width = PREVIEW_SIZE;
  previewCanvas.height = PREVIEW_SIZE;
  const ctx = previewCanvas.getContext('2d');
  if (!ctx) return '';

  drawStudioBackdrop(ctx);
  drawSingleJerseyOnCanvas(ctx, sourceTexture2048, 330, 258, 0.92, true);
  drawSingleJerseyOnCanvas(ctx, sourceTexture2048, 188, 244, 1.02, false);
  drawMockupFooter(ctx, kit, 'MAGLIA GARA');
  return previewCanvas.toDataURL('image/png');
}

export function renderShortsMockup512(
  previewCanvas: HTMLCanvasElement,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  kit: Kit
): string {
  previewCanvas.width = PREVIEW_SIZE;
  previewCanvas.height = PREVIEW_SIZE;
  const ctx = previewCanvas.getContext('2d');
  if (!ctx) return '';

  drawStudioBackdrop(ctx);
  drawSingleShortsOnCanvas(ctx, sourceTexture2048, 330, 250, 0.96, true);
  drawSingleShortsOnCanvas(ctx, sourceTexture2048, 185, 240, 1.05, false);
  drawMockupFooter(ctx, kit, 'PANTALONCINI');
  return previewCanvas.toDataURL('image/png');
}

export function renderSocksMockup512(
  previewCanvas: HTMLCanvasElement,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  kit: Kit
): string {
  previewCanvas.width = PREVIEW_SIZE;
  previewCanvas.height = PREVIEW_SIZE;
  const ctx = previewCanvas.getContext('2d');
  if (!ctx) return '';

  drawStudioBackdrop(ctx);
  drawSingleSockOnCanvas(ctx, sourceTexture2048, 190, 235, 1.0, false);
  drawSingleSockOnCanvas(ctx, sourceTexture2048, 322, 235, 1.0, true);
  drawMockupFooter(ctx, kit, 'CALZETTONI');
  return previewCanvas.toDataURL('image/png');
}

export function renderFullKitMockup512(
  previewCanvas: HTMLCanvasElement,
  texturesOrKit:
    | {
        jersey: HTMLCanvasElement | OffscreenCanvas;
        shorts: HTMLCanvasElement | OffscreenCanvas;
        socks: HTMLCanvasElement | OffscreenCanvas;
      }
    | Kit,
  maybeKit?: Kit
): string {
  previewCanvas.width = PREVIEW_SIZE;
  previewCanvas.height = PREVIEW_SIZE;
  const ctx = previewCanvas.getContext('2d');
  if (!ctx) return '';

  let textures: {
    jersey: HTMLCanvasElement | OffscreenCanvas;
    shorts: HTMLCanvasElement | OffscreenCanvas;
    socks: HTMLCanvasElement | OffscreenCanvas;
  };
  let kit: Kit;

  if ('id' in texturesOrKit && 'layers' in texturesOrKit) {
    kit = texturesOrKit as Kit;
    const safeGarments = ensureKitGarments(kit);
    const jCanv = document.createElement('canvas');
    jCanv.width = TEXTURE_SIZE;
    jCanv.height = TEXTURE_SIZE;
    compositeJersey2048(jCanv, safeGarments.jersey);

    const shCanv = document.createElement('canvas');
    shCanv.width = TEXTURE_SIZE;
    shCanv.height = TEXTURE_SIZE;
    compositeShorts2048(shCanv, safeGarments.shorts);

    const soCanv = document.createElement('canvas');
    soCanv.width = TEXTURE_SIZE;
    soCanv.height = TEXTURE_SIZE;
    compositeSocks2048(soCanv, safeGarments.socks);

    textures = { jersey: jCanv, shorts: shCanv, socks: soCanv };
  } else {
    textures = texturesOrKit as {
      jersey: HTMLCanvasElement | OffscreenCanvas;
      shorts: HTMLCanvasElement | OffscreenCanvas;
      socks: HTMLCanvasElement | OffscreenCanvas;
    };
    kit = maybeKit!;
  }

  drawStudioBackdrop(ctx);
  drawSingleSockOnCanvas(ctx, textures.socks, 215, 385, 0.58, false);
  drawSingleSockOnCanvas(ctx, textures.socks, 297, 385, 0.58, true);
  drawSingleShortsOnCanvas(ctx, textures.shorts, 256, 275, 0.72, false);
  drawSingleJerseyOnCanvas(ctx, textures.jersey, 256, 125, 0.76, false);
  drawMockupFooter(ctx, kit, 'COMPLETO UFFICIALE');
  return previewCanvas.toDataURL('image/png');
}

export function renderMinikitPreview512(
  previewCanvas: HTMLCanvasElement,
  sourceTexture2048: HTMLCanvasElement | OffscreenCanvas,
  kit: Kit,
  garmentType: GarmentType | 'ALL' = 'JERSEY'
): string {
  if (garmentType === 'SHORTS') {
    return renderShortsMockup512(previewCanvas, sourceTexture2048, kit);
  }
  if (garmentType === 'SOCKS') {
    return renderSocksMockup512(previewCanvas, sourceTexture2048, kit);
  }
  if (garmentType === 'ALL' && kit.garments) {
    const shortsTex = document.createElement('canvas');
    shortsTex.width = TEXTURE_SIZE;
    shortsTex.height = TEXTURE_SIZE;
    compositeShorts2048(shortsTex, kit.garments.shorts);

    const socksTex = document.createElement('canvas');
    socksTex.width = TEXTURE_SIZE;
    socksTex.height = TEXTURE_SIZE;
    compositeSocks2048(socksTex, kit.garments.socks);

    return renderFullKitMockup512(
      previewCanvas,
      { jersey: sourceTexture2048, shorts: shortsTex, socks: socksTex },
      kit
    );
  }
  return renderJerseyMockup512(previewCanvas, sourceTexture2048, kit);
}
