// Implements: SCR-SAVED, SCR-HOME / FUN-SAVE-LOCAL, FUN-OFFLINE-MODE
import Dexie, { type Table } from 'dexie';
import type { Kit } from '../types/kit';

export interface AppSettingRecord {
  key: string;
  value: unknown;
}

class OpenKitMakerDatabase extends Dexie {
  kits!: Table<Kit, string>;
  settings!: Table<AppSettingRecord, string>;

  constructor() {
    super('OpenKitMakerWebDB');
    this.version(1).stores({
      kits: 'id, name, category, updatedAt, createdAt, templateId',
      settings: 'key',
    });
  }
}

export const db = new OpenKitMakerDatabase();

export async function saveKitToDb(kit: Kit): Promise<void> {
  const plainKit: Kit = JSON.parse(JSON.stringify(kit));
  await db.kits.put(plainKit);
}

export async function loadAllKitsFromDb(): Promise<Kit[]> {
  const list = await db.kits.orderBy('updatedAt').reverse().toArray();
  return list;
}

export async function deleteKitFromDb(id: string): Promise<void> {
  await db.kits.delete(id);
}

export async function saveRecentColorsToDb(colors: string[]): Promise<void> {
  await db.settings.put({ key: 'recentColors', value: colors.slice(0, 8) });
}

export async function loadRecentColorsFromDb(): Promise<string[]> {
  const rec = await db.settings.get('recentColors');
  if (rec && Array.isArray(rec.value)) {
    return rec.value as string[];
  }
  return [
    '#0F52BA',
    '#00C49F',
    '#EF4444',
    '#F59E0B',
    '#10B981',
    '#FFFFFF',
    '#0F141C',
    '#1E3A8A',
  ];
}
