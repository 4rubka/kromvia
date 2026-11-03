import { Box3, BoxGeometry, CanvasTexture, Group, Mesh, MeshStandardMaterial, NearestFilter, SRGBColorSpace, Vector3, type Object3D } from "three";
import type { SkinViewer } from "skinview3d";
import CATALOG from "./cosmetics.json";

export type Slot = "head" | "face" | "neck" | "back" | "body" | "feet" | "effect";
type Anchor = "head" | "body" | "right_arm" | "left_arm";
type Anim =
  | { type: "flap"; side: number; speed: number; sweep: number }
  | { type: "cape" }
  | { type: "bob" | "swing" | "flicker" | "hop"; base: number; speed: number; amount: number }
  | { type: "wag"; speed: number; amount: number };
type Piece = { anchor: Anchor; color: number; pattern: string; glow?: boolean; anim?: Anim; pose?: number[]; boxes: number[] };
export type BoxItem = { id: string; name: string; slot: Slot; color: number; pieces: Piece[] };

export const BOX_ITEMS = CATALOG as BoxItem[];
export const boxItem = (id: string) => BOX_ITEMS.find((i) => i.id === id);

export const PALETTE = [0xfff2f2f2, 0xff262626, 0xffd93a3a, 0xfff08a24, 0xffffc83d, 0xff7bd93a, 0xff2e8b3e, 0xff4fe3e8, 0xff3a6fd9, 0xff8e44d9, 0xffff9ec4, 0xff7a4e2d];
export const RAINBOW = 0x00ffffff;

const SCALE = 4;

type Face = "top" | "bottom" | "west" | "north" | "east" | "south";
const BASE: Record<Face, number> = { top: 238, north: 218, south: 206, west: 198, east: 198, bottom: 170 };

function rng(seed: number) {
  let s = seed % 2147483647 || 1;
  return (n: number) => ((s = (s * 16807) % 2147483647) % n);
}

function paint(pattern: string, face: Face, w: number, h: number, r: (n: number) => number) {
  const rw = Math.max(1, Math.round(w * SCALE)), rh = Math.max(1, Math.round(h * SCALE));
  const c = document.createElement("canvas");
  c.width = rw;
  c.height = rh;
  const g = c.getContext("2d")!;
  const img = g.createImageData(rw, rh);
  const col0 = r(97);
  const side = face !== "top" && face !== "bottom";
  for (let y = 0; y < rh; y++) {
    for (let x = 0; x < rw; x++) {
      let lum = BASE[face] + r(9) - 4;
      let alpha = 255;
      switch (pattern) {
        case "fabric":
          lum += Math.floor(y / 2) % 2 === 0 ? 7 : -7;
          if ((x + Math.floor(y / 2) * 2) % 4 === 0) lum -= 9;
          break;
        case "feather": {
          const fx = (x + col0) % 6;
          if (fx === 0) lum -= 38;
          else if ((fx + y) % 3 === 0) lum -= 10;
          if (side && y >= rh - 3 && fx >= 4) alpha = 0;
          lum -= Math.floor((y * 18) / Math.max(rh, 1));
          break;
        }
        case "membrane":
          lum -= 30 + Math.floor((y * 25) / Math.max(rh, 1));
          if ((x + col0) % 11 === 0 || (x + y + col0) % 17 === 0) lum += 45;
          break;
        case "metal": {
          const d = (x + y) % 16;
          if (d < 2) lum += 28;
          else if (d < 4) lum += 12;
          break;
        }
        case "fur":
          if ((x * 7 + Math.floor(y / 3) * 13 + col0) % 5 === 0) lum -= 22;
          if (r(6) === 0) lum += 10;
          break;
        case "leather": {
          const stitch = (x === 3 || y === 3 || x === rw - 4 || y === rh - 4) && (x + y) % 4 < 2 && rw > 8 && rh > 8;
          if (stitch) lum += 32;
          lum -= 12;
          break;
        }
        case "glass":
          lum = 55 + ((x + y) % 14 < 3 ? 70 : 0);
          break;
        case "stripes":
          lum += Math.floor((x + y) / 6) % 2 === 0 ? 10 : -38;
          break;
        case "spots": {
          const cx = (x % 10) - 5, cy = (y % 10) - 5;
          if (cx * cx + cy * cy < 8) lum += 25;
          else if (cx * cx + cy * cy < 13) lum -= 40;
          break;
        }
        case "ridged":
          if (y % 4 === 0) lum -= 30;
          break;
      }
      const edge = x === 0 || y === 0 || x === rw - 1 || y === rh - 1;
      if (edge) lum -= pattern === "metal" ? 60 : 45;
      else if (y === 1 && side) lum += 14;
      lum = Math.max(0, Math.min(255, lum));
      const i = (y * rw + x) * 4;
      img.data[i] = img.data[i + 1] = img.data[i + 2] = lum;
      img.data[i + 3] = alpha;
    }
  }
  g.putImageData(img, 0, 0);
  const t = new CanvasTexture(c);
  t.magFilter = NearestFilter;
  t.minFilter = NearestFilter;
  t.colorSpace = SRGBColorSpace;
  return t;
}

const rgb = (argb: number) => argb & 0xffffff;

export function rainbow(now = performance.now()) {
  const h = (now % 6000) / 6000;
  const f = (n: number) => {
    const k = (n + h * 6) % 6;
    return Math.round(255 * (1 - 0.7 * Math.max(0, Math.min(k, 4 - k, 1))));
  };
  return (f(5) << 16) | (f(3) << 8) | f(1);
}

const OFFSET: Record<Anchor, number> = { head: 0, body: 6, right_arm: 0, left_arm: 0 };

type Built = { group: Group; anchor: Anchor; anim?: Anim; tinted: MeshStandardMaterial[] };

function buildPiece(item: BoxItem, p: Piece, index: number): Built {
  const r = rng(item.id.length * 7919 + index * 104729 + p.boxes.length);
  const tinted: MeshStandardMaterial[] = [];
  const pivot = new Group();
  const pose = p.pose ?? [0, 0, 0, 0, 0, 0];
  pivot.position.set(pose[0], -pose[1] + OFFSET[p.anchor], -pose[2]);
  pivot.rotation.set(pose[3], -pose[4], -pose[5], "ZYX");
  for (let i = 0; i < p.boxes.length; i += 6) {
    const [x, y, z, w, h, d] = p.boxes.slice(i, i + 6);
    const faces: [Face, number, number][] = [
      ["east", d, h],
      ["west", d, h],
      ["top", w, d],
      ["bottom", w, d],
      ["north", w, h],
      ["south", w, h],
    ];
    const mats = faces.map(([f, fw, fh]) => {
      const m = new MeshStandardMaterial({ map: paint(p.pattern, f, fw, fh, r), roughness: 1, metalness: 0, alphaTest: 0.5 });
      if (p.glow) m.emissive.set(0x222222);
      tinted.push(m);
      return m;
    });
    const mesh = new Mesh(new BoxGeometry(w, h, d), mats);
    mesh.position.set(x + w / 2, -(y + h / 2), -(z + d / 2));
    pivot.add(mesh);
  }
  const group = new Group();
  group.add(pivot);
  return { group, anchor: p.anchor, anim: p.anim, tinted };
}

function tint(b: Built, piece: Piece, color: number) {
  const c = piece.color !== 0 ? piece.color : color === RAINBOW ? rainbow() : color;
  for (const m of b.tinted) m.color.setHex(rgb(c));
}

function animate(b: Built, base: number[], age: number) {
  const pivot = b.group.children[0];
  const a = b.anim;
  if (!a) return;
  switch (a.type) {
    case "flap": {
      const f = Math.sin(age * a.speed) * 0.14;
      pivot.rotation.set(base[3], a.side * (a.sweep + f), a.side * 0.2, "ZYX");
      break;
    }
    case "cape":
      pivot.rotation.set(((6 + Math.sin(age * 0.05) * 2) * Math.PI) / 180 + 0.1, 0, 0, "ZYX");
      break;
    case "bob":
      pivot.position.y = -(a.base + Math.sin(age * a.speed) * a.amount) + OFFSET[b.anchor];
      break;
    case "swing":
      pivot.rotation.x = a.base + Math.sin(age * a.speed) * a.amount;
      break;
    case "flicker":
      pivot.scale.y = a.base + Math.sin(age * a.speed) * a.amount;
      break;
    case "wag":
      pivot.rotation.set(base[3], -Math.sin(age * a.speed) * a.amount, -base[5], "ZYX");
      break;
    case "hop":
      pivot.position.y = -(a.base - Math.abs(Math.sin(age * a.speed)) * a.amount) + OFFSET[b.anchor];
      break;
  }
}

const PART: Record<Anchor, "head" | "body" | "rightArm" | "leftArm"> = { head: "head", body: "body", right_arm: "rightArm", left_arm: "leftArm" };

export function wearBox(v: SkinViewer, id: string, color: () => number) {
  const item = boxItem(id);
  if (!item) return () => {};
  const built = item.pieces.map((p, i) => buildPiece(item, p, i));
  for (const b of built) v.playerObject.skin[PART[b.anchor]].add(b.group);
  let raf = 0;
  const start = performance.now();
  const tick = (now: number) => {
    const age = ((now - start) / 1000) * 20;
    built.forEach((b, i) => {
      tint(b, item.pieces[i], color());
      animate(b, item.pieces[i].pose ?? [0, 0, 0, 0, 0, 0], age);
    });
    raf = requestAnimationFrame(tick);
  };
  raf = requestAnimationFrame(tick);
  return () => {
    cancelAnimationFrame(raf);
    for (const b of built) b.group.parent?.remove(b.group);
  };
}

export function boxModel(id: string): Object3D {
  const item = boxItem(id)!;
  const root = new Group();
  item.pieces.forEach((p, i) => {
    const b = buildPiece(item, p, i);
    tint(b, p, item.color);
    root.add(b.group);
  });
  const box = new Box3().setFromObject(root);
  const size = box.getSize(new Vector3());
  const centre = box.getCenter(new Vector3());
  const holder = new Group();
  root.position.sub(centre);
  holder.add(root);
  holder.scale.setScalar(1 / Math.max(size.x, size.y, size.z, 1e-3));
  return holder;
}
