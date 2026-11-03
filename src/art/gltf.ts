import { NearestFilter, type Texture, AnimationMixer, Box3, type Mesh, type MeshStandardMaterial, DirectionalLight, Group, HemisphereLight, PerspectiveCamera, Scene, Vector3, WebGLRenderer, type Object3D } from "three";
import { GLTFLoader, type GLTF } from "three/examples/jsm/loaders/GLTFLoader.js";
import { clone } from "three/examples/jsm/utils/SkeletonUtils.js";
import { MODELS } from "./catalog";

const loader = new GLTFLoader();
const cache = new Map<string, Promise<GLTF>>();

export function loadGltf(id: string) {
  let p = cache.get(id);
  if (!p) {
    p = loader.loadAsync(`/models/${id}.glb`);
    cache.set(id, p);
  }
  return p;
}

const UPRIGHT: Record<string, [number, number]> = { "diamond-sword": [Math.PI / 2, -Math.PI / 2] };

const KEEP: Record<string, string[]> = {
  wands: Array.from({ length: 13 }, (_, i) => `Object_${4 + i * 2}`),
};

export async function instance(id: string, size = 1) {
  const entry = MODELS.find((m) => m.id === id);
  const gltf = await loadGltf(entry?.file ?? id);
  const model = clone(gltf.scene);
  const keep = KEEP[id];
  const nodes = entry?.nodes;
  const kept = (o: Object3D) => {
    for (let p: Object3D | null = o; p; p = p.parent) if (nodes!.includes(p.name)) return true;
    return false;
  };
  if (keep || nodes) {
    const drop: Object3D[] = [];
    model.traverse((o) => (o as Mesh).isMesh && !(keep ? keep.includes(o.name) : kept(o)) && drop.push(o));
    drop.forEach((o) => o.removeFromParent());
  }
  const up = UPRIGHT[id];
  if (up) {
    model.rotateX(up[0]);
    model.rotateOnWorldAxis(new Vector3(0, 0, 1), up[1]);
  }
  matte(model);
  model.updateMatrixWorld(true);
  const box = new Box3().setFromObject(model, true);
  const dims = box.getSize(new Vector3());
  const centre = box.getCenter(new Vector3());
  const k = size / Math.max(dims.x, dims.y, dims.z, 1e-6);
  model.position.sub(centre);
  const holder = new Group();
  holder.add(model);
  holder.scale.setScalar(k);
  const mixer = new AnimationMixer(model);
  if (gltf.animations[0]) mixer.clipAction(gltf.animations[0]).play();
  return { object: holder as Object3D, mixer };
}

export function matte(model: Object3D) {
  model.traverse((o) => {
    const m = (o as Mesh).material as MeshStandardMaterial | undefined;
    if (m && "metalness" in m) {
      m.metalness = 0;
      m.roughness = Math.max(m.roughness, 0.8);
    }
    const map = m?.map as Texture | null | undefined;
    if (map && map.magFilter !== NearestFilter) {
      map.magFilter = NearestFilter;
      map.needsUpdate = true;
    }
  });
}

export function lights(scene: Scene) {
  scene.add(new HemisphereLight(0xffffff, 0x666677, 2.2));
  const sun = new DirectionalLight(0xffffff, 1.6);
  sun.position.set(3, 5, 4);
  scene.add(sun);
}

let shotRenderer: WebGLRenderer | null = null;
const shots = new Map<string, Promise<string>>();

export function modelShot(id: string, px = 512) {
  return shot(id, () => instance(id, 1).then(({ object }) => object), px);
}

export function shot(key: string, make: () => Promise<Object3D>, px = 512) {
  let p = shots.get(key);
  if (!p) {
    p = make().then((object) => {
      shotRenderer ??= new WebGLRenderer({ alpha: true, antialias: true, preserveDrawingBuffer: true });
      shotRenderer.setPixelRatio(1);
      shotRenderer.setSize(px, px);
      const scene = new Scene();
      lights(scene);
      object.rotation.y = -0.6;
      scene.add(object);
      const cam = new PerspectiveCamera(30, 1, 0.01, 50);
      cam.position.set(0, 0.65, 2.8);
      cam.lookAt(0, 0, 0);
      shotRenderer.render(scene, cam);
      return shotRenderer.domElement.toDataURL("image/png");
    });
    shots.set(key, p);
  }
  return p;
}
