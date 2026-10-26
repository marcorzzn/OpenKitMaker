// Implements: SCR-EDITOR / FUN-EDIT-COLOR
<script lang="ts">
  import { studio } from '../state/studio.svelte';

  let {
    activeHex = '#0F52BA',
    secondaryHex = '#00C49F',
    hasSecondary = true,
    onColorChange,
  }: {
    activeHex: string;
    secondaryHex?: string;
    hasSecondary?: boolean;
    onColorChange: (hex: string, isSecondary: boolean, commitRecent: boolean) => void;
  } = $props();

  let editingSecondary = $state<boolean>(false);
  let hue = $state<number>(215);
  let sat = $state<number>(91);
  let val = $state<number>(73);
  let hexInput = $state<string>('#0F52BA');

  function hexToHsv(hexStr: string): { h: number; s: number; v: number } {
    const clean = hexStr.replace('#', '').slice(0, 6);
    if (clean.length !== 6) return { h: 215, s: 90, v: 75 };
    const r = parseInt(clean.slice(0, 2), 16) / 255;
    const g = parseInt(clean.slice(2, 4), 16) / 255;
    const b = parseInt(clean.slice(4, 6), 16) / 255;

    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const d = max - min;
    let h = 0;
    const s = max === 0 ? 0 : d / max;
    const v = max;

    if (max !== min) {
      switch (max) {
        case r:
          h = (g - b) / d + (g < b ? 6 : 0);
          break;
        case g:
          h = (b - r) / d + 2;
          break;
        case b:
          h = (r - g) / d + 4;
          break;
      }
      h /= 6;
    }
    return {
      h: Math.round(h * 360),
      s: Math.round(s * 100),
      v: Math.round(v * 100),
    };
  }

  function hsvToHex(h: number, s: number, v: number): string {
    const sNorm = s / 100;
    const vNorm = v / 100;
    const c = vNorm * sNorm;
    const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
    const m = vNorm - c;
    let r = 0;
    let g = 0;
    let b = 0;

    if (h >= 0 && h < 60) {
      r = c;
      g = x;
      b = 0;
    } else if (h >= 60 && h < 120) {
      r = x;
      g = c;
      b = 0;
    } else if (h >= 120 && h < 180) {
      r = 0;
      g = c;
      b = x;
    } else if (h >= 180 && h < 240) {
      r = 0;
      g = x;
      b = c;
    } else if (h >= 240 && h < 300) {
      r = x;
      g = 0;
      b = c;
    } else {
      r = c;
      g = 0;
      b = x;
    }

    const toHex = (n: number) =>
      Math.round((n + m) * 255)
        .toString(16)
        .padStart(2, '0')
        .toUpperCase();
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
  }

  // Sync local HSV state when layer selection or slot changes
  $effect(() => {
    const currentTargetHex = editingSecondary ? (secondaryHex || '#FFFFFF') : activeHex;
    hexInput = currentTargetHex;
    const parsed = hexToHsv(currentTargetHex);
    hue = parsed.h;
    sat = parsed.s;
    val = parsed.v;
  });

  function emitHsvChange(commit: boolean) {
    const computed = hsvToHex(hue, sat, val);
    hexInput = computed;
    onColorChange(computed, editingSecondary, commit);
  }

  function handleHexInputCommit() {
    const cleaned = hexInput.trim().toUpperCase();
    const formatted = cleaned.startsWith('#') ? cleaned : `#${cleaned}`;
    if (/^#[0-9A-F]{6}([0-9A-F]{2})?$/.test(formatted)) {
      const parsed = hexToHsv(formatted);
      hue = parsed.h;
      sat = parsed.s;
      val = parsed.v;
      onColorChange(formatted, editingSecondary, true);
    }
  }
</script>

<div class="bg-[#0F141C] border border-[#2E3D56] rounded-xl p-4 space-y-4">
  <div class="flex items-center justify-between">
    <span class="text-xs font-semibold text-white">Selettore Colore HSV & HEX</span>
    {#if hasSecondary}
      <div class="inline-flex items-center gap-1 p-0.5 bg-[#1A2230] border border-[#2E3D56] rounded-md">
        <button
          type="button"
          onclick={() => (editingSecondary = false)}
          class="px-2.5 py-1 text-xs font-medium rounded transition-colors flex items-center gap-1.5 cursor-pointer {!editingSecondary
            ? 'bg-[#0F52BA] text-white'
            : 'text-[#94A3B8] hover:text-white'}"
        >
          <span class="w-2.5 h-2.5 rounded-xs border border-white/30" style="background-color: {activeHex};"></span>
          Primario
        </button>
        <button
          type="button"
          onclick={() => (editingSecondary = true)}
          class="px-2.5 py-1 text-xs font-medium rounded transition-colors flex items-center gap-1.5 cursor-pointer {editingSecondary
            ? 'bg-[#0F52BA] text-white'
            : 'text-[#94A3B8] hover:text-white'}"
        >
          <span class="w-2.5 h-2.5 rounded-xs border border-white/30" style="background-color: {secondaryHex || '#FFFFFF'};"></span>
          Secondario
        </button>
      </div>
    {/if}
  </div>

  <!-- HSV Sliders with immediate < 16.6ms reactive update -->
  <div class="space-y-2.5">
    <div>
      <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
        <span>Tonalità (H)</span>
        <span>{hue}°</span>
      </div>
      <input
        type="range"
        min="0"
        max="360"
        bind:value={hue}
        oninput={() => emitHsvChange(false)}
        onchange={() => emitHsvChange(true)}
        class="w-full h-2 rounded-lg appearance-none cursor-pointer"
        style="background: linear-gradient(to right, #ff0000 0%, #ffff00 17%, #00ff00 33%, #00ffff 50%, #0000ff 67%, #ff00ff 83%, #ff0000 100%);"
      />
    </div>

    <div>
      <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
        <span>Saturazione (S)</span>
        <span>{sat}%</span>
      </div>
      <input
        type="range"
        min="0"
        max="100"
        bind:value={sat}
        oninput={() => emitHsvChange(false)}
        onchange={() => emitHsvChange(true)}
        class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
      />
    </div>

    <div>
      <div class="flex justify-between text-xs text-[#94A3B8] font-mono-tabular mb-1">
        <span>Luminosità (V)</span>
        <span>{val}%</span>
      </div>
      <input
        type="range"
        min="0"
        max="100"
        bind:value={val}
        oninput={() => emitHsvChange(false)}
        onchange={() => emitHsvChange(true)}
        class="w-full h-2 rounded-lg appearance-none cursor-pointer bg-[#253146] accent-[#00C49F]"
      />
    </div>
  </div>

  <!-- HEX Input + Native Color Input -->
  <div class="flex items-center gap-2 pt-1">
    <input
      type="color"
      value={hexInput.slice(0, 7)}
      oninput={(e) => {
        const v = (e.target as HTMLInputElement).value.toUpperCase();
        hexInput = v;
        const parsed = hexToHsv(v);
        hue = parsed.h;
        sat = parsed.s;
        val = parsed.v;
        onColorChange(v, editingSecondary, false);
      }}
      onchange={(e) => {
        const v = (e.target as HTMLInputElement).value.toUpperCase();
        onColorChange(v, editingSecondary, true);
      }}
      class="w-9 h-9 rounded bg-transparent border border-[#2E3D56] cursor-pointer shrink-0"
      title="Selettore colore rapido"
    />
    <input
      type="text"
      bind:value={hexInput}
      onblur={handleHexInputCommit}
      onkeydown={(e) => e.key === 'Enter' && handleHexInputCommit()}
      placeholder="#RRGGBB"
      maxlength="9"
      class="flex-1 px-3 py-1.5 text-xs font-mono-tabular bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white focus:outline-none focus:border-[#00C49F]"
    />
  </div>

  <!-- Recent 8 Colors Palette (FUN-EDIT-COLOR) -->
  <div class="space-y-1.5 pt-2 border-t border-[#2E3D56]">
    <div class="text-xs text-[#94A3B8] flex items-center justify-between">
      <span>Ultimi 8 Colori Usati</span>
      <span class="font-mono-tabular text-[11px] text-[#64748B]">FUN-EDIT-COLOR</span>
    </div>
    <div class="grid grid-cols-8 gap-1.5">
      {#each studio.recentColors as swatch (swatch)}
        <button
          type="button"
          onclick={() => {
            hexInput = swatch;
            const parsed = hexToHsv(swatch);
            hue = parsed.h;
            sat = parsed.s;
            val = parsed.v;
            onColorChange(swatch, editingSecondary, true);
          }}
          class="w-full aspect-square rounded-md border border-white/20 hover:scale-108 transition-transform cursor-pointer"
          style="background-color: {swatch};"
          title="Usa colore {swatch}"
          aria-label="Colore recente {swatch}"
        ></button>
      {/each}
    </div>
  </div>
</div>
