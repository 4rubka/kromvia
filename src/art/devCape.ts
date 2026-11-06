import type { SkinViewer } from "skinview3d";
import { DEV_MARK } from "./devMark";

const RES = 12;
const FRAMES = 40;
const FRAME_MS = 50;
const W = 22 * RES, H = 17 * RES;
const MARK_TOP = 4.2, MARK_H = 7.6;
const TOP = [0x1b1240, 0x170f38], BOTTOM = [0x0b1c52, 0x08143a];
const VIOLET = 0xa35fd6, BLUE = 0x3a7bff, PINK = 0xe07ad6, TRIM = 0xe4dcff, GOLD = 0xffd98a;
const MW = Math.round(MARK_H * RES * 149 / 256), MH = Math.round(MARK_H * RES);
const PAD = RES * 2, BW = MW + PAD * 2, BH = MH + PAD * 2;

type Mark = { rgb: Uint8ClampedArray; glow: Float32Array };

let mark: Promise<Mark> | null = null;
const frames: (HTMLCanvasElement | undefined)[] = [];

function loadMark(): Promise<Mark> {
  return (mark ??= new Promise<Mark>((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const c = document.createElement("canvas");
      c.width = MW;
      c.height = MH;
      const g = c.getContext("2d")!;
      g.imageSmoothingQuality = "high";
      g.drawImage(img, 0, 0, MW, MH);
      const rgb = g.getImageData(0, 0, MW, MH).data;
      const a = new Float32Array(MW * MH);
      for (let i = 0; i < a.length; i++) a[i] = rgb[i * 4 + 3] / 255;
      resolve({ rgb, glow: blur(a) });
    };
    img.onerror = reject;
    img.src = DEV_MARK;
  }));
}

function blur(a: Float32Array) {
  let img = new Float32Array(BW * BH);
  for (let y = 0; y < MH; y++) for (let x = 0; x < MW; x++) img[(y + PAD) * BW + x + PAD] = a[y * MW + x];
  const r = Math.floor(RES / 2);
  for (let pass = 0; pass < 3; pass++) {
    img = box(img, r, true);
    img = box(img, r, false);
  }
  return img;
}

function box(a: Float32Array, r: number, horizontal: boolean) {
  const out = new Float32Array(a.length);
  const n = horizontal ? BW : BH, lines = horizontal ? BH : BW;
  const at = (l: number, i: number) => (i < 0 || i >= n ? 0 : horizontal ? a[l * BW + i] : a[i * BW + l]);
  for (let l = 0; l < lines; l++) {
    let sum = 0;
    for (let i = -r; i <= r; i++) sum += at(l, i);
    for (let i = 0; i < n; i++) {
      out[horizontal ? l * BW + i : i * BW + l] = sum / (2 * r + 1);
      sum += at(l, i + r + 1) - at(l, i - r);
    }
  }
  return out;
}

const clamp = (f: number) => Math.max(0, Math.min(1, f));

function mix(a: number, b: number, f: number) {
  f = clamp(f);
  const ch = (s: number) => Math.round(((a >> s) & 255) + (((b >> s) & 255) - ((a >> s) & 255)) * f);
  return (ch(16) << 16) | (ch(8) << 8) | ch(0);
}

function add(a: number, b: number, f: number) {
  f = Math.max(0, f);
  const ch = (s: number) => Math.min(255, Math.round(((a >> s) & 255) + ((b >> s) & 255) * f));
  return (ch(16) << 16) | (ch(8) << 8) | ch(0);
}

function shade(c: number, m: number) {
  const ch = (s: number) => Math.min(255, Math.round(((c >> s) & 255) * m));
  return (ch(16) << 16) | (ch(8) << 8) | ch(0);
}

function smooth(e0: number, e1: number, v: number) {
  const t = clamp((v - e0) / (e1 - e0));
  return t * t * (3 - 2 * t);
}

function hash(x: number, y: number) {
  let h = (Math.imul(x, 374761393) + Math.imul(y, 668265263)) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return h ^ (h >>> 16);
}

function stars(x: number, y: number, t: number) {
  let best = 0;
  const cx = Math.floor(x / 1.25), cy = Math.floor(y / 1.25);
  for (let oy = -1; oy <= 1; oy++)
    for (let ox = -1; ox <= 1; ox++) {
      const gx = cx + ox, gy = cy + oy;
      const h = hash(gx, gy);
      if ((h & 3) !== 0) continue;
      const sx = (gx + ((h >> 4) & 255) / 255) * 1.25;
      const sy = (gy + ((h >> 12) & 255) / 255) * 1.25;
      const d2 = (x - sx) ** 2 + (y - sy) ** 2;
      const twinkle = 0.5 + 0.5 * Math.sin(Math.PI * 2 * (t * 2 + ((h >> 20) & 255) / 255));
      best = Math.max(best, Math.exp(-d2 / 0.006) * (0.4 + 0.6 * twinkle));
    }
  return best;
}

function edge(u: number, v: number, t: number) {
  const shimmer = 0.5 + 0.5 * Math.sin(Math.PI * 2 * (t + (u + v) / 12));
  return mix(mix(VIOLET, BLUE, v / 17), TRIM, 0.25 + 0.25 * shimmer);
}

function face(px: number, py: number, x: number, y: number, outside: boolean, t: number, m: Mark) {
  const wave = Math.PI * 2 * t;
  const fy = y / 16;
  let c = mix(mix(TOP[0], BOTTOM[0], fy), mix(TOP[1], BOTTOM[1], fy), x / 10);
  if (!outside) return shade(mix(c, VIOLET, 0.12 + 0.06 * Math.sin(wave + y * 0.4)), 0.75);

  for (let i = 0; i < 3; i++) {
    const phase = i * 2.1;
    const centre = 3 + i * 4.6 + 1.1 * Math.sin(x * 0.55 + wave + phase) + 0.5 * Math.sin(x * 1.3 - wave * 2 + phase);
    const d = (y - centre) / (1.1 + 0.35 * Math.sin(x * 0.8 + wave + phase));
    const band = Math.exp(-d * d) * (0.55 + 0.2 * Math.sin(wave + phase));
    c = add(c, i === 0 ? VIOLET : i === 1 ? BLUE : PINK, band * 0.55);
  }
  c = add(c, 0xffffff, stars(x, y, t));

  const mx = px - Math.round(((10 - (MARK_H * 149) / 256) / 2) * RES), my = py - Math.round(MARK_TOP * RES);
  const gx = mx + PAD, gy = my + PAD;
  if (gx >= 0 && gx < BW && gy >= 0 && gy < BH) c = add(c, VIOLET, m.glow[gy * BW + gx] * (0.75 + 0.25 * Math.sin(wave)) * 0.9);
  if (mx >= 0 && mx < MW && my >= 0 && my < MH) {
    const i = (my * MW + mx) * 4;
    const a = m.rgb[i + 3] / 255;
    if (a > 0) c = mix(c, mix((m.rgb[i] << 16) | (m.rgb[i + 1] << 8) | m.rgb[i + 2], 0xffffff, 0.12 + 0.1 * Math.sin(wave)), a);
  }

  const aa = 1 / RES;
  const inset = Math.min(x, 10 - x, y, 16 - y);
  const line = smooth(0.32 - aa, 0.32, inset) * (1 - smooth(0.52, 0.52 + aa, inset));
  c = mix(c, mix(TRIM, GOLD, 0.5 + 0.5 * Math.sin(wave + (x + y) * 0.3)), line * 0.9);
  c = mix(c, mix(VIOLET, BLUE, y / 16), 1 - smooth(0, 0.3, inset));

  const q = x * 0.6 + y * 0.8;
  const sweep = Math.exp(-(((q - (-4 + t * 26.8)) / 1.6) ** 2)) * 0.22;
  return add(c, 0xffffff, sweep);
}

function texel(tx: number, ty: number, t: number, m: Mark): number | null {
  const u = (tx + 0.5) / RES, v = (ty + 0.5) / RES;
  if (v < 1) return u >= 1 && u < 21 ? edge(u, v, t) : null;
  if (v >= 17) return null;
  if (u < 1 || (u >= 11 && u < 12)) return edge(u, v, t);
  if (u < 11) return face(tx - RES, ty - RES, u - 1, v - 1, true, t, m);
  if (u < 22) return face(tx - 12 * RES, ty - RES, u - 12, v - 1, false, t, m);
  return null;
}

function frame(f: number, m: Mark) {
  const done = frames[f];
  if (done) return done;
  const c = document.createElement("canvas");
  c.width = W;
  c.height = H;
  const g = c.getContext("2d")!;
  const img = g.createImageData(W, H);
  const t = f / FRAMES;
  for (let ty = 0; ty < H; ty++)
    for (let tx = 0; tx < W; tx++) {
      const col = texel(tx, ty, t, m);
      if (col === null) continue;
      const i = (ty * W + tx) * 4;
      img.data[i] = (col >> 16) & 255;
      img.data[i + 1] = (col >> 8) & 255;
      img.data[i + 2] = col & 255;
      img.data[i + 3] = 255;
    }
  g.putImageData(img, 0, 0);
  return (frames[f] = c);
}

export function wearDevCape(v: SkinViewer) {
  let raf = 0;
  let shown = -1;
  let alive = true;
  loadMark().then((m) => {
    if (!alive) return;
    v.loadCape(frame(0, m));
    shown = 0;
    const tick = (now: number) => {
      const f = Math.floor(now / FRAME_MS) % FRAMES;
      const map = v.playerObject.cape.map;
      if (f !== shown && map) {
        const g = v.capeCanvas.getContext("2d")!;
        g.clearRect(0, 0, v.capeCanvas.width, v.capeCanvas.height);
        g.drawImage(frame(f, m), 0, 0);
        map.needsUpdate = true;
        shown = f;
      }
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
  }, () => {});
  return () => {
    alive = false;
    cancelAnimationFrame(raf);
  };
}
