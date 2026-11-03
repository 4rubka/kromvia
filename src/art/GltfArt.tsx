import { useEffect, useRef } from "react";
import { Clock, PerspectiveCamera, Scene, WebGLRenderer } from "three";
import { instance, lights } from "./gltf";

export function GltfArt({ id, spin = 0.4, tilt = 0.25, angle = 0 }: { id: string; spin?: number; tilt?: number; angle?: number }) {
  const host = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = host.current!;
    let renderer: WebGLRenderer;
    try {
      renderer = new WebGLRenderer({ alpha: true, antialias: true });
    } catch {
      return; // No WebGL: shown without the model.
    }
    renderer.setPixelRatio(window.devicePixelRatio);
    el.appendChild(renderer.domElement);
    const scene = new Scene();
    lights(scene);
    const camera = new PerspectiveCamera(30, 1, 0.01, 50);
    camera.position.set(0, tilt * 2.4, 2.4);
    camera.lookAt(0, 0, 0);

    const resize = () => {
      renderer.setSize(el.clientWidth, el.clientHeight);
      camera.aspect = el.clientWidth / Math.max(el.clientHeight, 1);
      camera.updateProjectionMatrix();
      renderer.render(scene, camera); // setSize clears the canvas; repaint right away
    };
    const ro = new ResizeObserver(resize);
    ro.observe(el);
    resize();

    let alive = true;
    let raf = 0;
    instance(id, 1).then(({ object, mixer }) => {
      if (!alive) return;
      object.rotation.y = angle;
      scene.add(object);
      const clock = new Clock();
      const tick = () => {
        const dt = clock.getDelta();
        mixer.update(dt);
        object.rotation.y += dt * spin;
        object.position.y = Math.sin(clock.elapsedTime * 1.2) * 0.03;
        renderer.render(scene, camera);
        raf = requestAnimationFrame(tick);
      };
      tick();
    });

    return () => {
      alive = false;
      cancelAnimationFrame(raf);
      ro.disconnect();
      renderer.dispose();
      renderer.forceContextLoss();
      el.removeChild(renderer.domElement);
    };
  }, [id, spin, tilt, angle]);

  return <div className="gltf-art" ref={host} />;
}
