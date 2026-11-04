"use client";

import { useEffect, useRef, useState } from "react";
import { IdleAnimation, SkinViewer } from "skinview3d";
import { capeTexture, type CapeStyle } from "@launcher/art/capes";
import { boxItem, wearBox } from "@launcher/art/procedural";
import { wear, wearable } from "@launcher/art/wear";
import type { Cosmetics } from "@/lib/api";
import { mascotSkin } from "@/lib/mascotSkin";

const STYLES: CapeStyle[] = ["kromvia", "ember", "frost"];

export function Player3D({
  skin,
  cosmetics,
  zoom = 0.8,
  turn = 0.5,
  drag = true,
  className,
  onReady,
}: {
  skin?: string | null;
  cosmetics: Cosmetics;
  zoom?: number;
  turn?: number;
  drag?: boolean;
  className?: string;
  onReady?: () => void;
}) {
  const host = useRef<HTMLDivElement>(null);
  const canvas = useRef<HTMLCanvasElement>(null);
  const [viewer, setViewer] = useState<SkinViewer | null>(null);

  useEffect(() => {
    const el = host.current!;
    let v: SkinViewer;
    try {
      v = new SkinViewer({ canvas: canvas.current!, width: el.clientWidth || 280, height: el.clientHeight || 380, fov: 30 });
    } catch {
      return; // No WebGL.
    }
    v.animation = new IdleAnimation();
    v.autoRotate = turn > 0;
    v.autoRotateSpeed = turn;
    v.controls.enableZoom = false;
    v.controls.enableRotate = drag;
    v.zoom = zoom;
    v.globalLight.intensity = 2.4;
    v.cameraLight.intensity = 0.7;
    if (turn === 0) v.playerWrapper.rotation.y = -0.5;
    const ro = new ResizeObserver(() => v.setSize(el.clientWidth, el.clientHeight));
    ro.observe(el);
    const io = new IntersectionObserver(([e]) => (v.renderPaused = !e.isIntersecting));
    io.observe(el);
    setViewer(v);
    return () => {
      ro.disconnect();
      io.disconnect();
      v.dispose();
    };
  }, [zoom, turn, drag]);

  useEffect(() => {
    if (!viewer) return;
    const loaded = skin ? viewer.loadSkin(skin, { model: "auto-detect" }).catch(() => viewer.loadSkin(mascotSkin())) : viewer.loadSkin(mascotSkin());
    Promise.resolve(loaded).then(() => onReady?.());
  }, [viewer, skin]);

  const models = (cosmetics.models ?? []).filter(wearable);
  const items = (cosmetics.items ?? []).filter((id) => boxItem(id)?.pieces.length);
  const style = STYLES.includes(cosmetics.style as CapeStyle) ? (cosmetics.style as CapeStyle) : "kromvia";
  const wings = models.some((id) => id.startsWith("wings"));

  useEffect(() => {
    if (!viewer) return;
    if (cosmetics.cape && !wings) viewer.loadCape(capeTexture(style));
    else viewer.loadCape(null);
  }, [viewer, cosmetics.cape, style, wings]);

  const modelKey = models.join(",");
  useEffect(() => {
    if (!viewer) return;
    let offs: (() => void)[] = [];
    let alive = true;
    Promise.all(models.map((id) => wear(viewer, id).catch(() => () => {}))).then((fns) => {
      if (alive) offs = fns;
      else fns.forEach((f) => f());
    });
    return () => {
      alive = false;
      offs.forEach((f) => f());
    };
  }, [viewer, modelKey]);

  const itemKey = items.join(",");
  useEffect(() => {
    if (!viewer) return;
    const offs = items.map((id) => wearBox(viewer, id, () => boxItem(id)?.color ?? 0xffffffff));
    return () => offs.forEach((f) => f());
  }, [viewer, itemKey]);

  return (
    <div ref={host} className={className} style={{ cursor: drag ? "grab" : undefined }}>
      <canvas ref={canvas} style={{ display: "block", width: "100%", height: "100%" }} />
    </div>
  );
}
