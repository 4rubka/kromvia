import { useEffect, useRef } from "react";
import { IdleAnimation, SkinViewer } from "skinview3d";

export function SkinViewer3D({ skin, slim, cape = null }: { skin: string | null; slim: boolean; cape?: string | null }) {
  const canvas = useRef<HTMLCanvasElement>(null);
  const viewer = useRef<SkinViewer | null>(null);

  useEffect(() => {
    const v = new SkinViewer({ canvas: canvas.current!, width: 240, height: 340 });
    v.animation = new IdleAnimation();
    v.autoRotate = true;
    v.autoRotateSpeed = 0.6;
    v.zoom = 0.85;
    viewer.current = v;
    return () => v.dispose();
  }, []);

  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    if (skin) v.loadSkin(skin, { model: slim ? "slim" : "default" });
    else v.loadSkin(null);
  }, [skin, slim]);

  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    if (cape) v.loadCape(cape);
    else v.resetCape();
  }, [cape]);

  return <canvas ref={canvas} className="skin-viewer" />;
}
