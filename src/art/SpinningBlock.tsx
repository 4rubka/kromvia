import { useEffect, useRef } from "react";
import { DirectionalLight, HemisphereLight, PerspectiveCamera, Scene, WebGLRenderer } from "three";
import { blockMesh, type BlockKind } from "./blocks";

export function SpinningBlock({ kind }: { kind: BlockKind }) {
  const host = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = host.current!;
    let renderer: WebGLRenderer;
    try {
      renderer = new WebGLRenderer({ alpha: true, antialias: true });
    } catch {
      return; // No WebGL: the panel shows without its block.
    }
    renderer.setPixelRatio(window.devicePixelRatio);
    el.appendChild(renderer.domElement);

    const scene = new Scene();
    scene.add(new HemisphereLight(0xffffff, 0x666677, 1.6));
    const sun = new DirectionalLight(0xffffff, 1.4);
    sun.position.set(2, 5, 3);
    scene.add(sun);
    const block = blockMesh(kind);
    block.rotation.x = 0.35;
    scene.add(block);

    const camera = new PerspectiveCamera(28, 1, 0.1, 50);
    camera.position.set(0, 0.6, 4.2);
    camera.lookAt(0, 0, 0);

    const resize = () => {
      const w = el.clientWidth, h = el.clientHeight;
      renderer.setSize(w, h);
      camera.aspect = w / Math.max(h, 1);
      camera.updateProjectionMatrix();
    };
    const ro = new ResizeObserver(resize);
    ro.observe(el);
    resize();

    const hoverEl = el.parentElement ?? el;
    let hover = 0, target = 0;
    const on = () => (target = 1), off = () => (target = 0);
    hoverEl.addEventListener("pointerenter", on);
    hoverEl.addEventListener("pointerleave", off);

    let raf = 0, last = performance.now();
    const tick = (now: number) => {
      const dt = Math.min(0.05, (now - last) / 1000);
      last = now;
      hover += (target - hover) * Math.min(1, dt * 6);
      block.rotation.y += dt * (0.35 + hover * 1.2);
      block.position.y = Math.sin(now / 800) * 0.06;
      renderer.render(scene, camera);
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);

    return () => {
      cancelAnimationFrame(raf);
      ro.disconnect();
      hoverEl.removeEventListener("pointerenter", on);
      hoverEl.removeEventListener("pointerleave", off);
      renderer.dispose();
      renderer.forceContextLoss();
      el.removeChild(renderer.domElement);
    };
  }, [kind]);

  return <div className="spinning-block" ref={host} />;
}
