import SPRITES from "./effects.json";

type Sprite = { rows: string[]; colors: Record<string, string> };

const SCALE = 8;
const cache = new Map<string, string | null>();

export function effectIcon(id: string): string | null {
  if (cache.has(id)) return cache.get(id)!;
  const sprite = (SPRITES as Record<string, Sprite>)[id];
  let url: string | null = null;
  if (sprite) {
    const c = document.createElement("canvas");
    c.width = c.height = 11 * SCALE;
    const g = c.getContext("2d")!;
    sprite.rows.forEach((row, y) =>
      [...row].forEach((ch, x) => {
        const col = sprite.colors[ch];
        if (!col) return;
        g.fillStyle = col;
        g.fillRect((x + 1) * SCALE, (y + 1) * SCALE, SCALE, SCALE);
      }),
    );
    url = c.toDataURL("image/png");
  }
  cache.set(id, url);
  return url;
}
