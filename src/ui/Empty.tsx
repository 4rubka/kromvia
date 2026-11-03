import type { ReactNode } from "react";

export function Empty({ bee, children }: { bee: string; children: ReactNode }) {
  return (
    <div className="empty">
      <img className="empty-bee" src={`/bee/${bee}.webp`} alt="" draggable={false} />
      <div className="empty-text">{children}</div>
    </div>
  );
}
