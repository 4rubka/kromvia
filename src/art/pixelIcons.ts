import data from "./pixelIcons.json";

type Icon = { p: Record<string, string>; g: string[] };

const ICONS = data as Record<string, Icon>;
const SCALE = 4;
const cache = new Map<string, string>();

export const PIXEL_ICONS = Object.keys(ICONS);

export function pixelIcon(id: string): string {
  const hit = cache.get(id);
  if (hit) return hit;
  const icon = ICONS[id];
  const c = document.createElement("canvas");
  c.width = c.height = 16 * SCALE;
  const g = c.getContext("2d")!;
  icon.g.forEach((row, y) =>
    [...row].forEach((ch, x) => {
      const colour = icon.p[ch];
      if (!colour) return;
      g.fillStyle = colour;
      g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
    }),
  );
  const url = c.toDataURL("image/png");
  cache.set(id, url);
  return url;
}
