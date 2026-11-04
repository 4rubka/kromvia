
const PALETTE: Record<string, string> = {
  G: "#3f9b45", // leaf
  g: "#2b6e31", // leaf shade
  L: "#b993ff", // lilac petal
  B: "#6fa2ff", // blue petal
  Y: "#ffe27a", // flower center
  P: "#ff8fc8", // pink petal
  W: "#f2f0ff", // white petal
  O: "#ffb347", // glow berry
  o: "#e07b1f", // glow berry shade
};

function Pixels({ rows, size, className }: { rows: string[]; size: number; className?: string }) {
  const w = rows[0].length;
  return (
    <svg
      className={className}
      width={w * size}
      height={rows.length * size}
      viewBox={`0 0 ${w} ${rows.length}`}
      shapeRendering="crispEdges"
      aria-hidden
    >
      {rows.flatMap((row, y) =>
        [...row].map((c, x) => (PALETTE[c] ? <rect key={`${x}-${y}`} x={x} y={y} width={1} height={1} fill={PALETTE[c]} /> : null)),
      )}
    </svg>
  );
}

const LILAC = [".L.", "LYL", ".L.", ".G.", "GG.", ".G."];
const BLUE = [".B.", "BYB", ".B.", ".G.", ".GG", ".G.", ".G."];
const PINK = [".P.", "PYP", ".P.", ".G.", ".G."];
const TUFT = ["G.G", "GgG"];

export function Flowers() {
  return (
    <span className="launch-flowers" aria-hidden>
      <Pixels rows={LILAC} size={3} />
      <Pixels rows={TUFT} size={3} />
      <Pixels rows={BLUE} size={3} />
      <Pixels rows={TUFT} size={3} />
      <Pixels rows={PINK} size={3} />
    </span>
  );
}

const VINE = [
  "GGgGGGgG",
  ".GgG.Gg.",
  ".g...G..",
  ".GG..g..",
  ".g...GG.",
  "GG...G..",
  ".g......",
  ".G......",
  ".GG.....",
  ".g......",
  "GG......",
  ".G......",
  ".g......",
];

export function Vines() {
  return (
    <>
      <Pixels rows={VINE} size={4} className="vine vine-left" />
      <Pixels rows={VINE} size={4} className="vine vine-right" />
    </>
  );
}
