// Implements: SCR-CATALOG / FUN-SELECT-TEMPLATE
<script lang="ts">
  import { onMount } from 'svelte';
  import { studio } from '../state/studio.svelte';
  import { i18n } from '../i18n/it';
  import { BUILTIN_TEMPLATES, cloneTemplateToKit } from '../data/templates';
  import { renderFullKitMockup512 } from '../lib/compositor2d';
  import type { KitCategory, Template } from '../types/kit';

  let searchQuery = $state<string>('');
  let selectedCategory = $state<'ALL' | KitCategory>('ALL');
  let templatePreviews = $state<Record<string, string>>({});

  const categoryTabs: ('ALL' | KitCategory)[] = [
    'ALL',
    'HOME',
    'AWAY',
    'THIRD',
    'GK',
    'SPECIAL',
  ];

  let filteredTemplates = $derived<Template[]>(
    BUILTIN_TEMPLATES.filter((tpl) => {
      const matchesCat =
        selectedCategory === 'ALL' || tpl.category === selectedCategory;
      const q = searchQuery.trim().toLowerCase();
      const matchesSearch =
        !q ||
        tpl.name.toLowerCase().includes(q) ||
        tpl.description.toLowerCase().includes(q) ||
        tpl.patternSummary.toLowerCase().includes(q) ||
        i18n.catalog.categories[tpl.category].toLowerCase().includes(q);
      return matchesCat && matchesSearch;
    })
  );

  onMount(() => {
    // Generate crisp 2D Full Kit previews for all catalog templates
    const prevCanvas = document.createElement('canvas');
    prevCanvas.width = 512;
    prevCanvas.height = 512;

    const map: Record<string, string> = {};
    for (const tpl of BUILTIN_TEMPLATES) {
      const tempKit = cloneTemplateToKit(tpl);
      map[tpl.id] = renderFullKitMockup512(prevCanvas, tempKit);
    }
    templatePreviews = map;
  });
</script>

<section class="max-w-7xl mx-auto px-6 py-10 space-y-8">
  <!-- Header & Search / Filter Bar (SCR-CATALOG) -->
  <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 border-b border-[#2E3D56] pb-6">
    <div class="space-y-2">
      <div class="text-xs text-[#94A3B8] font-mono-tabular">
        SCR-CATALOG · FUN-SELECT-TEMPLATE · 100% Vettoriale & Procedurale
      </div>
      <h1 class="text-3xl sm:text-4xl font-bold font-display tracking-wide text-white">
        {i18n.catalog.title}
      </h1>
      <p class="text-sm text-[#94A3B8]">
        Seleziona un modello base per clonare i livelli con nuovo UUIDv4 e aprire lo Studio Grafico.
      </p>
    </div>

    <!-- Search input -->
    <div class="w-full md:w-80">
      <label for="catalog-search" class="sr-only">Cerca template</label>
      <input
        id="catalog-search"
        type="search"
        bind:value={searchQuery}
        placeholder={i18n.catalog.search_placeholder}
        class="w-full px-4 py-2.5 text-sm bg-[#1A2230] border border-[#2E3D56] rounded-lg text-white placeholder-[#64748B] focus:outline-none focus:border-[#0F52BA]"
      />
    </div>
  </div>

  <!-- Interactive Filter Controls (Segmented buttons per SCR-CATALOG) -->
  <div class="flex items-center gap-2 overflow-x-auto pb-1">
    <div class="inline-flex items-center gap-1 p-1 bg-[#1A2230] border border-[#2E3D56] rounded-lg">
      {#each categoryTabs as cat (cat)}
        <button
          type="button"
          onclick={() => (selectedCategory = cat)}
          class="px-4 py-2 text-xs font-semibold rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer {selectedCategory === cat
            ? 'bg-[#0F52BA] text-white shadow-sm'
            : 'text-[#94A3B8] hover:text-white'}"
        >
          {i18n.catalog.categories[cat]}
        </button>
      {/each}
    </div>
  </div>

  <!-- Template Grid -->
  {#if filteredTemplates.length === 0}
    <div class="bg-[#1A2230] border border-[#2E3D56] rounded-xl p-12 text-center space-y-3">
      <p class="text-base font-medium text-white">Nessun template trovato per i filtri selezionati.</p>
      <button
        type="button"
        onclick={() => {
          searchQuery = '';
          selectedCategory = 'ALL';
        }}
        class="px-4 py-2 text-xs font-semibold text-white bg-[#0F52BA] rounded-lg cursor-pointer"
      >
        Ripristina Filtri
      </button>
    </div>
  {:else}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {#each filteredTemplates as tpl (tpl.id)}
        <article class="bg-[#1A2230] border border-[#2E3D56] hover:border-[#00C49F] rounded-xl p-5 flex flex-col justify-between transition-colors group">
          <div>
            <!-- Quiet unboxed metadata header -->
            <div class="flex items-center justify-between text-xs text-[#94A3B8] mb-3 font-mono-tabular">
              <span>Categoria: {i18n.catalog.categories[tpl.category]}</span>
              <span>{tpl.defaultLayers.length} Livelli Base</span>
            </div>

            <!-- 2D Preview Container -->
            <button
              type="button"
              onclick={() => studio.selectTemplate(tpl)}
              class="w-full bg-[#0F141C] rounded-lg p-3 aspect-square flex items-center justify-center mb-4 overflow-hidden cursor-pointer relative"
            >
              {#if templatePreviews[tpl.id]}
                <img
                  src={templatePreviews[tpl.id]}
                  alt="Anteprima 2D template {tpl.name}"
                  class="w-full h-full object-contain group-hover:scale-103 transition-transform duration-200"
                />
              {:else}
                <div class="text-xs text-[#64748B] font-mono-tabular">Generazione minikit 2D…</div>
              {/if}
            </button>

            <h2 class="text-xl font-bold font-display tracking-wide text-white">
              {tpl.name}
            </h2>
            <p class="text-xs text-[#00C49F] font-mono-tabular mt-0.5">
              {tpl.patternSummary}
            </p>
            <p class="text-sm text-[#94A3B8] mt-2 leading-relaxed">
              {tpl.description}
            </p>
          </div>

          <div class="flex items-center justify-between mt-5 pt-4 border-t border-[#2E3D56]">
            <!-- Color Swatches Preview -->
            <div class="flex items-center gap-1.5">
              <span
                class="w-4 h-4 rounded-sm border border-white/20"
                style="background-color: {tpl.basePreviewColor};"
                title="Colore Base {tpl.basePreviewColor}"
              ></span>
              <span
                class="w-4 h-4 rounded-sm border border-white/20"
                style="background-color: {tpl.accentPreviewColor};"
                title="Colore Primario Pattern {tpl.accentPreviewColor}"
              ></span>
              <span
                class="w-4 h-4 rounded-sm border border-white/20"
                style="background-color: {tpl.secondaryPreviewColor};"
                title="Colore Accento {tpl.secondaryPreviewColor}"
              ></span>
            </div>

            <button
              type="button"
              onclick={() => studio.selectTemplate(tpl)}
              class="px-4 py-2 text-xs font-semibold text-white bg-[#0F52BA] group-hover:bg-[#00C49F] group-hover:text-[#0F141C] rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              Clona e Personalizza →
            </button>
          </div>
        </article>
      {/each}
    </div>
  {/if}
</section>
