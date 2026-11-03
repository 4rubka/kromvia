type Sprite = { rows: string[]; colors: Record<string, string> };

const HEART = [".##...##.", "#+o#.#oo#", "#+ooooos#", "#oooooos#", ".#oooos#.", "..#oos#..", "...#s#...", "....#....", "........."];
const FLAME = ["....#....", "...#+#...", "...#o+#..", "..#oo+#..", ".#oo++o#.", ".#o+yy+#.", "#oo+yy+o#", "#so+yy+s#", ".#sssss#."];
const STAR = ["....#....", "....+....", "...#+#...", "..#o+o#..", "#+++y+++#", "..#o+o#..", "...#+#...", "....+....", "....#...."];
const NOTE = ["....##...", "....#o#..", "....#.o#.", "....#..#.", "....#....", ".###o....", "#ooo#....", "#o+o#....", ".###....."];
const FLAKE = ["....o....", ".o..o..o.", "..o.o.o..", "...ooo...", "oooo+oooo", "...ooo...", "..o.o.o..", ".o..o..o.", "....o...."];
const PETAL = ["..++.....", ".+oo.....", "..o...++.", "......oo+", "...+o....", "..+oo....", ".........", ".oo...+o.", ".+.....o."];
const TOTEM = ["....+....", "...+o+...", "....+..#.", ".......#.", ".#.......", "#o#...+..", ".#...+o+.", "......+..", "..#......"];
const RUNES = ["ooo..o.o.", "o.o..o.o.", "ooo..ooo.", "o......o.", ".........", ".o...ooo.", "ooo..o...", ".o...ooo.", ".o.....o."];
const RING = ["...+++...", ".+o...o+.", ".o.....o.", "+...y...+", "+..yyy..+", "+...y...+", ".o.....o.", ".+o...o+.", "...+++..."];
const ARCS = ["..aaaaa..", ".abbbbba.", "abcccccba", "abcdddcba", "abcd.dcba", "abcd.dcba", "abcd.dcba", ".........", "........."];
const HALO = [".........", "....+....", "..+ooo+..", ".o.....o.", "+...y...+", ".o.....o.", "..+ooo+..", ".........", "........."];
const CLOUD = ["...###...", ".##+++#..", "#++ooo+##", "#oooooos#", ".#sssss#.", ".........", ".b..b..b.", "b..b..b..", "........."];
const SWIRL = ["..ooooo..", ".o+...+o.", "o+.ooo.+o", "o.o+..o.o", "o.o.y.o.o", "o.o..+o.o", "o+.ooo.+o", ".o+...+o.", "..ooooo.."];
const MAGIC = [".+.......", "+o+...#..", ".+...#o#.", "......#..", "...+.....", "..+o+....", "...+...+.", "......+o+", ".......+."];
const BURST = ["o...o...o", ".o..o..o.", "..o.+.o..", "...+++...", "oo++y++oo", "...+++...", "..o.+.o..", ".o..o..o.", "o...o...o"];
const GHOST = ["..#####..", ".#+oooo#.", "#+ooooos#", "#o#oo#os#", "#oooooos#", "#oooooos#", "#oooooos#", "#o#o#o#s#", ".#.#.#.#."];
const DUST = [".o....+..", ".........", "...+...o.", "o........", ".....o...", "..o......", "......+.o", ".+.......", "....o...."];
const SPORE = ["......oo.", ".oo...o+.", ".o+......", "....oo...", "....o+...", ".o.......", "......oo.", "..oo..o+.", "..o+....."];
const FIREWORK = ["a...b...c", ".a..b..c.", "..a...c..", ".........", "dd..y..dd", ".........", "..c...a..", ".c..b..a.", "c...b...a"];
const BOLT = [".....###.", "....#++#.", "...#+o#..", "..#+o#...", ".#+ooo##.", ".####+o#.", "....#o#..", "...#o#...", "...##...."];

const SPRITES: Record<string, Sprite> = {
  hearts: { rows: HEART, colors: { "#": "#5a1020", o: "#e8364f", "+": "#ff9aa8", s: "#a91d36" } },
  flames: { rows: FLAME, colors: { "#": "#5a1d08", o: "#f07a1a", "+": "#ffb43a", y: "#fff0a0", s: "#b8400e" } },
  soul_flames: { rows: FLAME, colors: { "#": "#08343a", o: "#20b8c8", "+": "#6ff0f0", y: "#e0ffff", s: "#127280" } },
  sparkles: { rows: STAR, colors: { "#": "#7a6a30", o: "#ffe680", "+": "#fff6c8", y: "#ffffff" } },
  notes: { rows: NOTE, colors: { "#": "#1f6e3a", o: "#4fd26b", "+": "#b8ffc8" } },
  snow: { rows: FLAKE, colors: { o: "#bfe6ff", "+": "#ffffff" } },
  petals: { rows: PETAL, colors: { o: "#ffaed0", "+": "#ffd8ea" } },
  totem: { rows: TOTEM, colors: { "#": "#3fbf4f", o: "#fff0a8", "+": "#f2c94c" } },
  enchant: { rows: RUNES, colors: { o: "#b48cff" } },
  glow: { rows: RING, colors: { o: "#57e0c8", "+": "#a8fff0", y: "#e8fff9" } },
  rainbow: { rows: ARCS, colors: { a: "#e8364f", b: "#f2c94c", c: "#4fd26b", d: "#4f8fd9" } },
  aura: { rows: HALO, colors: { o: "#d9d2ff", "+": "#ffffff", y: "#8b5cf6" } },
  rain_cloud: { rows: CLOUD, colors: { "#": "#5d6675", o: "#d9dee8", "+": "#ffffff", s: "#a2abba", b: "#4f8fd9" } },
  portal: { rows: SWIRL, colors: { o: "#8e44d9", "+": "#d08cff", y: "#f0d8ff" } },
  witch: { rows: MAGIC, colors: { "#": "#5b2a86", o: "#c070ff", "+": "#8e44d9" } },
  crit: { rows: BURST, colors: { o: "#c8c8c8", "+": "#f2f2f2", y: "#ffffff" } },
  souls: { rows: GHOST, colors: { "#": "#10323a", o: "#7fd8e0", "+": "#d0fbff", s: "#3f9aa6" } },
  ash: { rows: DUST, colors: { o: "#9a9a9a", "+": "#e0e0e0" } },
  spores: { rows: SPORE, colors: { o: "#3a9a4a", "+": "#b8f0a0" } },
  fireworks: { rows: FIREWORK, colors: { a: "#ff5a7a", b: "#ffd84a", c: "#5ad8ff", d: "#a87bff", y: "#ffffff" } },
  sparks: { rows: BOLT, colors: { "#": "#2a4a7a", o: "#7fc8ff", "+": "#e8f6ff" } },
};

const SCALE = 8;
const cache = new Map<string, string | null>();

export function effectIcon(id: string): string | null {
  if (cache.has(id)) return cache.get(id)!;
  const sprite = SPRITES[id];
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
