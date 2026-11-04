import { Cube } from "./Cube";
import s from "./FloatingCubes.module.css";

const CUBES: { x: number; y: number; size: number; blur?: number; delay: number; dim?: boolean }[] = [
  { x: 4, y: 18, size: 46, delay: 0, dim: true },
  { x: 38, y: 8, size: 18, delay: 2.4, dim: true },
  { x: 47, y: 86, size: 30, blur: 2, delay: 0.6, dim: true },
  { x: 62, y: 4, size: 64, blur: 4, delay: 1.8, dim: true },
  { x: 88, y: 12, size: 38, delay: 3, dim: true },
  { x: 94, y: 64, size: 72, blur: 3, delay: 0.3, dim: true },
  { x: 76, y: 90, size: 20, delay: 2 },
];

export function FloatingCubes() {
  return (
    <div className={s.layer} aria-hidden="true" data-cubes>
      {CUBES.map((c, i) => (
        <Cube
          key={i}
          size={c.size}
          className={s.cube}
          tones={c.dim ? ["#4a3a8a", "#2e2366", "#1a2a6a"] : undefined}
          style={{ "--d": c.size / 40, left: `${c.x}%`, top: `${c.y}%`, filter: c.blur ? `blur(${c.blur}px)` : undefined, animationDelay: `${-c.delay}s` } as React.CSSProperties}
        />
      ))}
    </div>
  );
}
