import { Box3, Group, Vector3, type Object3D } from "three";
import type { SkinViewer } from "skinview3d";
import { clone } from "three/examples/jsm/utils/SkeletonUtils.js";
import { instance, loadGltf, matte } from "./gltf";
import { MODELS } from "./catalog";

type Part = "head" | "body" | "rightArm" | "leftArm" | "rightLeg" | "leftLeg";

const CENTER: Record<Part, [number, number, number]> = {
  head: [0, 4, 0],
  body: [0, 0, 0],
  rightArm: [-1, -4, 0],
  leftArm: [1, -4, 0],
  rightLeg: [0, -6, 0],
  leftLeg: [0, -6, 0],
};

type Fit = { parts: Part[]; size: number; pos: [number, number, number]; rot?: [number, number, number] };

const FITS: Record<string, Fit> = {
  horns: { parts: ["head"], size: 22, pos: [0, 6, 0] },
  "horns-small": { parts: ["head"], size: 12, pos: [0, 6, 0] },
  "wings-angel": { parts: ["body"], size: 30, pos: [0, -2, -5] },
  "wings-low-poly": { parts: ["body"], size: 26, pos: [0, -3, -4] },
};

type Rig = { back?: boolean; pieces: { part: Part; anchor: string; nodes: string[] }[] };

const RIGS: Record<string, Rig> = {
  "santa-hat": { pieces: [{ part: "head", anchor: "Head_22", nodes: ["group4_31"] }] },
  antlers: { pieces: [{ part: "head", anchor: "Head_73", nodes: ["group3_90"] }] },
  "mage-robe": {
    back: true,
    pieces: [
      { part: "body", anchor: "cube_1", nodes: ["cube_2", "cube_3", "cube_4"] },
      { part: "leftArm", anchor: "cube_11", nodes: ["cube_10", "cube_12"] },
      { part: "rightArm", anchor: "cube_14", nodes: ["cube_15", "cube_16"] },
    ],
  },
  "mecha-boost": {
    back: true,
    pieces: [
      { part: "leftLeg", anchor: "cube_2", nodes: ["cube_3", "cube_4", "cube_5"] },
      { part: "rightLeg", anchor: "cube_7", nodes: ["cube_8", "cube_9", "cube_10"] },
      { part: "leftArm", anchor: "cube_13", nodes: ["cube_14", "cube_15", "cube_16", "cube_17", "cube_18", "cube_19"] },
      { part: "rightArm", anchor: "cube_22", nodes: ["cube_23", "cube_24", "cube_25", "cube_26", "cube_27", "cube_28"] },
    ],
  },
};

export const wearable = (id: string) => id in FITS || id in RIGS;

export async function wear(v: SkinViewer, id: string) {
  const skin = v.playerObject.skin;
  const placed: [Object3D, Object3D][] = [];
  let mixers: { update(dt: number): void }[] = [];

  const fit = FITS[id];
  const rig = RIGS[id];
  if (fit) {
    for (const [i, part] of fit.parts.entries()) {
      const { object, mixer } = await instance(id, fit.size);
      const holder = new Group();
      holder.add(object);
      holder.position.set(CENTER[part][0] + fit.pos[0], CENTER[part][1] + fit.pos[1], CENTER[part][2] + fit.pos[2]);
      if (fit.rot) holder.rotation.set(...fit.rot);
      let top: Object3D = holder;
      if (i > 0) {
        top = new Group();
        top.scale.x = -1;
        top.add(holder);
      }
      placed.push([skin[part], top]);
      mixers.push(mixer);
    }
  } else if (rig) {
    const file = MODELS.find((m) => m.id === id)?.file ?? id;
    const scene = clone((await loadGltf(file)).scene);
    matte(scene);
    scene.updateMatrixWorld(true);
    for (const piece of rig.pieces) {
      const centre = new Box3().setFromObject(scene.getObjectByName(piece.anchor)!).getCenter(new Vector3());
      const inner = new Group();
      for (const name of piece.nodes) inner.attach(scene.getObjectByName(name)!);
      inner.position.sub(centre);
      const outer = new Group();
      outer.add(inner);
      outer.scale.setScalar(16);
      if (rig.back) outer.rotation.y = Math.PI;
      outer.position.set(...CENTER[piece.part]);
      placed.push([skin[piece.part], outer]);
    }
  }
  for (const [part, obj] of placed) part.add(obj);

  let raf = 0, last = performance.now();
  const tick = (now: number) => {
    for (const m of mixers) m.update((now - last) / 1000);
    last = now;
    raf = requestAnimationFrame(tick);
  };
  if (mixers.length) raf = requestAnimationFrame(tick);
  return () => {
    cancelAnimationFrame(raf);
    mixers = [];
    for (const [part, obj] of placed) part.remove(obj);
  };
}
