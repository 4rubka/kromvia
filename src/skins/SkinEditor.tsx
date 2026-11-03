import { useEffect, useRef, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { Glyph } from "../ui/Glyph";
import { SkinViewer3D } from "./SkinViewer3D";

type Tool = "pencil" | "eraser" | "fill" | "pick";
const TOOLS: Tool[] = ["pencil", "eraser", "fill", "pick"];
const SIZE = 64;
const HISTORY = 50;

function hexToRgba(hex: string): [number, number, number, number] {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255, 255];
}

function rgbaToHex([r, g, b]: number[]) {
  return "#" + [r, g, b].map((v) => v.toString(16).padStart(2, "0")).join("");
}

export function SkinEditor({ initial, initialSlim, onSaved }: { initial: string | null; initialSlim: boolean; onSaved: () => void }) {
  const { t } = useI18n();
  const canvas = useRef<HTMLCanvasElement>(null);
  const history = useRef<ImageData[]>([]);
  const last = useRef<[number, number] | null>(null);
  const [tool, setTool] = useState<Tool>("pencil");
  const [color, setColor] = useState("#3b82f6");
  const [slim, setSlim] = useState(initialSlim);
  const [name, setName] = useState("");
  const [preview, setPreview] = useState<string | null>(null);
  const [status, setStatus] = useState<{ error: boolean; text: string } | null>(null);

  const ctx = () => canvas.current!.getContext("2d", { willReadFrequently: true })!;
  const refreshPreview = () => setPreview(canvas.current!.toDataURL("image/png"));

  useEffect(() => {
    const c = ctx();
    c.clearRect(0, 0, SIZE, SIZE);
    history.current = [];
    if (!initial) return refreshPreview();
    const img = new Image();
    img.onload = () => {
      c.drawImage(img, 0, 0);
      refreshPreview();
    };
    img.src = initial;
  }, [initial]);

  const snapshot = () => {
    history.current.push(ctx().getImageData(0, 0, SIZE, SIZE));
    if (history.current.length > HISTORY) history.current.shift();
  };

  const undo = () => {
    const prev = history.current.pop();
    if (prev) {
      ctx().putImageData(prev, 0, 0);
      refreshPreview();
    }
  };

  const at = (e: React.PointerEvent): [number, number] => {
    const r = canvas.current!.getBoundingClientRect();
    const x = Math.floor(((e.clientX - r.left) / r.width) * SIZE);
    const y = Math.floor(((e.clientY - r.top) / r.height) * SIZE);
    return [Math.min(SIZE - 1, Math.max(0, x)), Math.min(SIZE - 1, Math.max(0, y))];
  };

  const plot = (x: number, y: number) => {
    const c = ctx();
    if (tool === "eraser") c.clearRect(x, y, 1, 1);
    else {
      c.fillStyle = color;
      c.fillRect(x, y, 1, 1);
    }
  };

  const line = (from: [number, number], to: [number, number]) => {
    let [x, y] = from;
    const dx = Math.abs(to[0] - x);
    const dy = -Math.abs(to[1] - y);
    const sx = x < to[0] ? 1 : -1;
    const sy = y < to[1] ? 1 : -1;
    let err = dx + dy;
    for (;;) {
      plot(x, y);
      if (x === to[0] && y === to[1]) break;
      const e2 = 2 * err;
      if (e2 >= dy) (err += dy), (x += sx);
      if (e2 <= dx) (err += dx), (y += sy);
    }
  };

  const fill = (x: number, y: number) => {
    const c = ctx();
    const img = c.getImageData(0, 0, SIZE, SIZE);
    const d = img.data;
    const idx = (px: number, py: number) => (py * SIZE + px) * 4;
    const target = Array.from(d.slice(idx(x, y), idx(x, y) + 4));
    const paint = hexToRgba(color);
    if (target.every((v, i) => v === paint[i])) return;
    const same = (i: number) => target.every((v, k) => d[i + k] === v);
    const stack = [[x, y]];
    while (stack.length) {
      const [px, py] = stack.pop()!;
      if (px < 0 || py < 0 || px >= SIZE || py >= SIZE) continue;
      const i = idx(px, py);
      if (!same(i)) continue;
      paint.forEach((v, k) => (d[i + k] = v));
      stack.push([px + 1, py], [px - 1, py], [px, py + 1], [px, py - 1]);
    }
    c.putImageData(img, 0, 0);
  };

  const down = (e: React.PointerEvent) => {
    const p = at(e);
    if (tool === "pick") {
      const px = ctx().getImageData(p[0], p[1], 1, 1).data;
      if (px[3] > 0) setColor(rgbaToHex(Array.from(px)));
      return;
    }
    canvas.current!.setPointerCapture(e.pointerId);
    snapshot();
    if (tool === "fill") {
      fill(...p);
      refreshPreview();
      return;
    }
    plot(...p);
    last.current = p;
  };

  const move = (e: React.PointerEvent) => {
    if (!last.current) return;
    const p = at(e);
    line(last.current, p);
    last.current = p;
  };

  const up = () => {
    if (last.current) {
      last.current = null;
      refreshPreview();
    }
  };

  const clear = () => {
    snapshot();
    ctx().clearRect(0, 0, SIZE, SIZE);
    refreshPreview();
  };

  const save = () => {
    setStatus(null);
    invoke("wardrobe_add_data", { dataUrl: canvas.current!.toDataURL("image/png"), name: name.trim() || "Edited skin", slim }).then(
      () => (setStatus({ error: false, text: t("editor.saved") }), onSaved()),
      (e) => setStatus({ error: true, text: String(e) }),
    );
  };

  return (
    <div className="wardrobe">
      <section className="card wardrobe-stage">
        <SkinViewer3D skin={preview} slim={slim} />
        <label className="muted">
          <input type="checkbox" checked={slim} onChange={(e) => setSlim(e.currentTarget.checked)} /> {t("editor.slim")}
        </label>
      </section>
      <section className="card skin-editor">
        <div className="segmented wrap">
          {TOOLS.map((k) => (
            <button key={k} aria-pressed={tool === k} onClick={() => setTool(k)}>
              {t(`editor.${k}` as const)}
            </button>
          ))}
        </div>
        <div className="actions">
          <input type="color" value={color} aria-label={t("editor.color")} onChange={(e) => setColor(e.currentTarget.value)} />
          <button className="btn btn-ghost" onClick={undo}>
            <Glyph name="rotate" /> {t("editor.undo")}
          </button>
          <button className="btn btn-ghost" onClick={clear}>
            <Glyph name="trash" /> {t("editor.clear")}
          </button>
        </div>
        <canvas
          ref={canvas}
          width={SIZE}
          height={SIZE}
          className="skin-canvas"
          onPointerDown={down}
          onPointerMove={move}
          onPointerUp={up}
          onPointerCancel={up}
        />
        <div className="actions">
          <input className="input" value={name} placeholder={t("editor.name")} onChange={(e) => setName(e.currentTarget.value)} />
          <button className="btn" onClick={save}>
            <Glyph name="device-floppy" /> {t("editor.save")}
          </button>
        </div>
        {status && <p className={status.error ? "error" : "muted"}>{status.text}</p>}
      </section>
    </div>
  );
}
