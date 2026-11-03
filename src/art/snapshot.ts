import { DirectionalLight, HemisphereLight, OrthographicCamera, Scene, WebGLRenderer } from "three";
import { blockMesh, type BlockKind } from "./blocks";

let renderer: WebGLRenderer | null = null;
const cache = new Map<BlockKind, string>();

export function snapshot(kind: BlockKind, px = 256): string {
  const hit = cache.get(kind);
  if (hit) return hit;
  renderer ??= new WebGLRenderer({ alpha: true, antialias: true, preserveDrawingBuffer: true });
  renderer.setPixelRatio(1);
  renderer.setSize(px, px);

  const scene = new Scene();
  scene.add(new HemisphereLight(0xffffff, 0x666677, 1.6));
  const sun = new DirectionalLight(0xffffff, 1.4);
  sun.position.set(2, 5, 3);
  scene.add(sun);
  const block = blockMesh(kind);
  scene.add(block);

  const camera = new OrthographicCamera(-0.95, 0.95, 0.95, -0.95, 0.1, 10);
  camera.position.set(2, 1.65, 2);
  camera.lookAt(0, 0, 0);
  renderer.render(scene, camera);
  const url = renderer.domElement.toDataURL("image/png");
  cache.set(kind, url);
  return url;
}
