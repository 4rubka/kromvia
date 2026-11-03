const PARTS: { x: number; y: number; w: number; h: number; from: number; to: number }[] = [
  { x: 0, y: 0, w: 32, h: 16, from: 0, to: 0.25 }, // head
  { x: 16, y: 16, w: 24, h: 16, from: 0.25, to: 0.62 }, // body
  { x: 40, y: 16, w: 16, h: 16, from: 0.25, to: 0.62 }, // right arm
  { x: 32, y: 48, w: 16, h: 16, from: 0.25, to: 0.62 }, // left arm
  { x: 0, y: 16, w: 16, h: 16, from: 0.62, to: 1 }, // right leg
  { x: 16, y: 48, w: 16, h: 16, from: 0.62, to: 1 }, // left leg
];

const STOPS: [number, [number, number, number]][] = [
  [0, [0xb4, 0x9c, 0xff]],
  [0.35, [0xa3, 0x5f, 0xd6]],
  [0.65, [0x6a, 0x5c, 0xf0]],
  [1, [0x1f, 0x63, 0xd6]],
];

function shade(t: number, jitter: number) {
  const i = Math.max(1, STOPS.findIndex(([at]) => at >= t));
  const [a, ca] = STOPS[i - 1];
  const [b, cb] = STOPS[i];
  const k = (t - a) / (b - a || 1);
  const c = ca.map((v, j) => Math.round((v + (cb[j] - v) * k) * jitter));
  return `rgb(${c[0]} ${c[1]} ${c[2]})`;
}

let cached: HTMLCanvasElement | null = null;

export function mascotSkin() {
  if (cached) return cached;
  const c = document.createElement("canvas");
  c.width = 64;
  c.height = 64;
  const g = c.getContext("2d")!;
  let seed = 4111;
  const rnd = () => ((seed = (seed * 16807) % 2147483647) / 2147483647);
  const px = (x: number, y: number, col: string) => {
    g.fillStyle = col;
    g.fillRect(x, y, 1, 1);
  };
  for (const p of PARTS) {
    const faces = p.from === 0 ? 8 : 4;
    for (let y = p.y; y < p.y + p.h; y++)
      for (let x = p.x; x < p.x + p.w; x++) {
        const row = y - p.y;
        const local = row < faces ? 0 : (row - faces) / (p.h - faces - 1 || 1);
        px(x, y, shade(p.from + (p.to - p.from) * local, 0.9 + rnd() * 0.12));
      }
  }
  for (const [x, pupil] of [[9, 10], [13, 14]] as const) {
    px(x, 12, "#ffffff");
    px(pupil, 12, "#1a1240");
    px(x, 13, "#ffffff");
    px(pupil, 13, "#1a1240");
  }
  px(11, 15, "#3b1f86");
  px(12, 15, "#3b1f86");
  cached = c;
  return c;
}
