import { useEffect, useRef } from "react";
import { IdleAnimation, SkinViewer } from "skinview3d";
import { capeTexture, type CapeStyle } from "./capes";

export function SkinArt({
  skin,
  slim,
  back,
  style = "kromvia",
  zoom = 0.78,
  spin = 0.0025,
}: {
  skin: string | null;
  slim: boolean;
  back: "elytra" | "cape";
  style?: CapeStyle;
  zoom?: number;
  spin?: number;
}) {
  const host = useRef<HTMLDivElement>(null);
  const canvas = useRef<HTMLCanvasElement>(null);
  const viewer = useRef<SkinViewer | null>(null);

  useEffect(() => {
    const el = host.current!;
    let v: SkinViewer;
    try {
      v = new SkinViewer({ canvas: canvas.current!, width: el.clientWidth || 300, height: el.clientHeight || 300, fov: 30 });
    } catch {
      return; // No WebGL.
    }
    v.animation = new IdleAnimation();
    v.controls.enableZoom = false;
    v.controls.enableRotate = false;
    v.zoom = zoom;
    v.globalLight.intensity = 2.4;
    v.cameraLight.intensity = 0.7;
    v.playerWrapper.rotation.y = back === "elytra" ? Math.PI * 0.82 : -0.5;
    v.loadCape(capeTexture(style), { backEquipment: back });

    let raf = 0;
    const wings = (now: number) => {
      if (back === "elytra") {
        const e = v.playerObject.elytra;
        e.leftWing.rotation.x = 0.35;
        e.leftWing.rotation.z = 1.05 + Math.sin(now / 700) * 0.18;
        e.updateRightWing();
      }
      v.playerWrapper.rotation.y += spin;
      raf = requestAnimationFrame(wings);
    };
    raf = requestAnimationFrame(wings);

    const ro = new ResizeObserver(() => v.setSize(el.clientWidth, el.clientHeight));
    ro.observe(el);
    viewer.current = v;
    return () => {
      cancelAnimationFrame(raf);
      ro.disconnect();
      v.dispose();
    };
  }, [back, style, zoom, spin]);

  useEffect(() => {
    const v = viewer.current;
    if (!v) return;
    if (skin) v.loadSkin(skin, { model: slim ? "slim" : "default" });
    else v.loadSkin(null);
  }, [skin, slim]);

  return (
    <div className="skin-art" ref={host}>
      <canvas ref={canvas} />
    </div>
  );
}
