import { BoxGeometry, CanvasTexture, Mesh, MeshLambertMaterial, NearestFilter, SRGBColorSpace } from "three";

export type BlockKind = "grass" | "crafting" | "wool" | "stone" | "earth" | "portal";

type Paint = (put: (x: number, y: number, c: string) => void, r: () => number) => void;

function rng(seed: number) {
  return () => ((seed = (seed * 16807) % 2147483647) / 2147483647);
}

function texture(seed: number, paint: Paint) {
  const c = document.createElement("canvas");
  c.width = c.height = 16;
  const g = c.getContext("2d")!;
  paint((x, y, col) => {
    g.fillStyle = col;
    g.fillRect(x, y, 1, 1);
  }, rng(seed));
  const t = new CanvasTexture(c);
  t.magFilter = NearestFilter;
  t.minFilter = NearestFilter;
  t.colorSpace = SRGBColorSpace;
  return t;
}

const pick = (r: () => number, pal: string[]) => pal[Math.floor(r() * pal.length)];

const GRASS = ["#6cc24a", "#7ed957", "#5fb83f", "#8fe068", "#58a83a"];
const DIRT = ["#7a5536", "#6b4a2f", "#5a3e26", "#86603d", "#4f3621"];
const PLANK = ["#b3894f", "#a07a45", "#c49a5c", "#9a7240"];
const STONE = ["#8a9196", "#7d8489", "#959ca1", "#6f767b", "#868d92"];

const fill = (pal: string[]): Paint => (put, r) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) put(x, y, pick(r, pal));
};

const grassSide: Paint = (put, r) => {
  fill(DIRT)(put, r);
  for (let x = 0; x < 16; x++) {
    const hang = 3 + Math.floor(r() * r() * 4);
    for (let y = 0; y < hang; y++) put(x, y, pick(r, GRASS));
  }
};

const planks: Paint = (put, r) => {
  for (let y = 0; y < 16; y++)
    for (let x = 0; x < 16; x++) {
      const seam = y % 4 === 3 || (x === ((Math.floor(y / 4) * 7) % 16) && y % 4 !== 3);
      put(x, y, seam ? "#6e4f2c" : pick(r, PLANK));
    }
};

const craftingTop: Paint = (put, r) => {
  planks(put, r);
  for (let i = 0; i < 16; i++) {
    put(i, 0, "#5a3e26");
    put(i, 15, "#5a3e26");
    put(0, i, "#5a3e26");
    put(15, i, "#5a3e26");
  }
  for (let i = 3; i < 13; i++) {
    put(i, 5, "#4a3423");
    put(i, 10, "#4a3423");
    put(5, i, "#4a3423");
    put(10, i, "#4a3423");
  }
};

const craftingSide: Paint = (put, r) => {
  planks(put, r);
  for (let x = 3; x < 8; x++) put(x, 4, "#c9ced2");
  for (let y = 4; y < 11; y++) put(3, y, "#6e4f2c");
  for (let x = 9; x < 13; x++) put(x, 5, "#9aa1a6");
  for (let y = 5; y < 12; y++) put(11, y, "#6e4f2c");
  for (let x = 0; x < 16; x++) put(x, 0, "#5a3e26");
};

const wool: Paint = (put, r) => {
  const quads = [["#b98cf0", "#a77be0"], ["#5fb8e8", "#4ea6d6"], ["#e0a6f2", "#cf95e2"], ["#f2c94c", "#e2b93c"]];
  for (let y = 0; y < 16; y++)
    for (let x = 0; x < 16; x++) {
      const q = quads[(x < 8 ? 0 : 1) + (y < 8 ? 0 : 2)];
      const stitch = (x === 7 || x === 8 || y === 7 || y === 8) && (x + y) % 2 === 0;
      put(x, y, stitch ? "#f4f5f7" : pick(r, q));
    }
};

const stone: Paint = (put, r) => {
  fill(STONE)(put, r);
  for (let i = 0; i < 9; i++) put(Math.floor(r() * 16), Math.floor(r() * 16), "#5f666b");
};

const earth = (seed: number): Paint => (put, r) => {
  const land: boolean[] = [];
  for (let i = 0; i < 256; i++) land.push(false);
  for (let k = 0; k < 3; k++) {
    let x = Math.floor(r() * 16), y = Math.floor(r() * 16);
    for (let s = 0; s < 26 + seed % 7; s++) {
      land[y * 16 + x] = true;
      land[y * 16 + ((x + 1) % 16)] = true;
      x = (x + Math.floor(r() * 3) - 1 + 16) % 16;
      y = (y + Math.floor(r() * 3) - 1 + 16) % 16;
    }
  }
  for (let y = 0; y < 16; y++)
    for (let x = 0; x < 16; x++) {
      const l = land[y * 16 + x];
      put(x, y, l ? pick(r, ["#7ed957", "#6cc24a", "#5fb83f", "#e8d9a0"]) : pick(r, ["#3a8fd9", "#3383cc", "#2f7bc4", "#4096dd"]));
    }
  for (let c = 0; c < 4; c++) {
    const cx = Math.floor(r() * 14), cy = Math.floor(r() * 15);
    put(cx, cy, "#f4f5f7");
    put(cx + 1, cy, "#ffffff");
    if (r() > 0.5) put(cx + 2, cy, "#e6eaee");
  }
};

const portalFace: Paint = (put, r) => {
  for (let y = 0; y < 16; y++)
    for (let x = 0; x < 16; x++) {
      const frame = x < 2 || x > 13 || y < 2 || y > 13;
      if (frame) {
        put(x, y, pick(r, ["#1f1a33", "#2a2440", "#17132a"]));
        continue;
      }
      const d = Math.hypot(x - 7.5, y - 7.5), a = Math.atan2(y - 7.5, x - 7.5);
      const swirl = Math.sin(a * 3 + d * 0.9);
      put(x, y, swirl > 0.3 ? "#c9b5ff" : swirl > -0.3 ? "#8b5cf6" : "#2f55c8");
    }
};

type Faces = [Paint, Paint, Paint, Paint, Paint, Paint]; // +x, -x, +y, -y, +z, -z

const KINDS: Record<BlockKind, () => Faces> = {
  grass: () => [grassSide, grassSide, fill(GRASS), fill(DIRT), grassSide, grassSide],
  crafting: () => [craftingSide, planks, craftingTop, planks, craftingSide, planks],
  wool: () => [wool, wool, wool, wool, wool, wool],
  stone: () => [stone, stone, stone, stone, stone, stone],
  earth: () => [earth(1), earth(2), earth(3), earth(4), earth(5), earth(6)],
  portal: () => [portalFace, portalFace, fill(["#1f1a33", "#2a2440"]), fill(["#1f1a33", "#2a2440"]), portalFace, portalFace],
};

const geometry = new BoxGeometry(1, 1, 1);

export function blockMesh(kind: BlockKind) {
  const faces = KINDS[kind]();
  const mats = faces.map((paint, i) => new MeshLambertMaterial({ map: texture(i * 101 + kind.length * 7, paint) }));
  return new Mesh(geometry, mats);
}
