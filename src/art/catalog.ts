/**
 * 3D models bundled in public/models, all CC BY 4.0 from Sketchfab: free to use, the authors
 * are credited on the Licenses page (Settings).
 */
export type Slot = "head" | "face" | "neck" | "back" | "weapon" | "feet" | "body";

export type ModelEntry = {
  id: string;
  title: string;
  name: string;
  slot?: Slot;
  author: string;
  url: string;
  file?: string;
  nodes?: string[];
};

const range = (prefix: string, from: number, to: number) => Array.from({ length: to - from + 1 }, (_, i) => `${prefix}${from + i}`);

const sf = (user: string) => `https://sketchfab.com/${user}`;

const CHRISTMAS = { title: "Christmas cosmetics", author: "Nyux xi", url: sf("Nyuxxi"), file: "christmas" };
const MIRU = { author: "Anesteva Miru", url: sf("anesteva_miru") };

export const MODELS: ModelEntry[] = [
  { id: "horns", title: "Horn accessory", name: "Shadow Horns", slot: "head", author: "state_L", url: sf("state_L") },
  { id: "horns-small", title: "Horn accessory", name: "Crimson Horns", slot: "head", author: "state_L", url: sf("state_L") },
  { id: "santa-hat", name: "Santa Hat", slot: "head", nodes: ["group4_31"], ...CHRISTMAS },
  { id: "antlers", name: "Antlers", slot: "head", nodes: ["group3_90"], ...CHRISTMAS },
  { id: "wings-angel", title: "Wings Angel - port of Wings mod Minecraft", name: "Angel Wings", slot: "back", author: "playswolrdgames", url: sf("playswolrdgames") },
  { id: "wings-low-poly", title: "Low poly wings", name: "Crystal Wings", slot: "back", author: "Lut1k", url: sf("Lut1k") },
  { id: "boots-of-swiftness", title: "Boots of Swiftness", name: "Boots of Swiftness", author: "Crodattilo", url: sf("Crodattilo") },
  { id: "mage-robe", title: "Mage Robe", name: "Mage Robe", slot: "body", nodes: ["cube_2", "cube_3", "cube_4", "cube_10", "cube_12", "cube_15", "cube_16"], ...MIRU },
  { id: "mecha-boost", title: "Mecha Boost", name: "Mecha Armor", slot: "body", nodes: [...range("cube_", 3, 5), ...range("cube_", 8, 10), ...range("cube_", 14, 19), ...range("cube_", 23, 28)], ...MIRU },
  { id: "wands", title: "Wands Minecraft", name: "Magic Wands", author: "sedona1029", url: sf("sedona1029") },
  { id: "diamond-sword", title: "Minecraft Diamond Sword", name: "Diamond Sword", author: "Blender3D", url: sf("Blender3D") },
  { id: "wrench", title: "Create Wrench", name: "Wrench", author: "Lumely", url: sf("Lumely") },
  { id: "book", title: "Book for Minecraft", name: "Spell Book", author: "SebastianFEnriquez", url: sf("SebastianFEnriquez") },
  { id: "bee", title: "Minecraft Bee", name: "Bee", author: "Marina Vildanova", url: sf("kittenpuff") },
  { id: "fox", title: "Fox Minecraft", name: "Fox", author: "kuzneciv", url: sf("kuzneciv") },
  { id: "allay", title: "Minecraft Allay", name: "Allay", author: "None", url: sf("None_Yaroslav") },
  { id: "warden", title: "Minecraft Warden", name: "Warden", author: "BeckBroEYTube", url: sf("BeckBroEYTube") },
  { id: "earth-cube", title: "earth-cube", name: "Earth Cube", author: "madexc", url: sf("madexc") },
];
