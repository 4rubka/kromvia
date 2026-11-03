import { MODELS } from "@launcher/art/catalog";
import BOXES from "@launcher/art/cosmetics.json";

export type CatalogItem = { id: string; name: string; slot: string; kind: "model" | "box"; credit?: { author: string; url: string } };

export const CATALOG: CatalogItem[] = [
  ...(BOXES as { id: string; name: string; slot: string; pieces: unknown[] }[]).filter((b) => b.pieces.length).map((b) => ({ id: b.id, name: b.name, slot: b.slot, kind: "box" as const })),
  ...MODELS.filter((m) => m.slot).map((m) => ({ id: m.id, name: m.name, slot: m.slot!, kind: "model" as const, credit: { author: m.author, url: m.url } })),
];

export const itemName = (id: string) => CATALOG.find((c) => c.id === id)?.name ?? id;
