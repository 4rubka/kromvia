import { useState } from "react";
import { Glyph } from "./Glyph";

export function Pager({ page, pages, onPage }: { page: number; pages: number; onPage: (page: number) => void }) {
  if (pages <= 1) return null;
  const go = (p: number, el: HTMLElement) => {
    onPage(p);
    const list = el.closest(".pager")?.previousElementSibling;
    if (list && list.getBoundingClientRect().top < 0) list.scrollIntoView({ block: "start" });
  };
  const shown = [...new Set([0, page - 1, page, page + 1, pages - 1])].filter((p) => p >= 0 && p < pages).sort((a, b) => a - b);
  return (
    <nav className="pager">
      <button className="pager-step" disabled={page === 0} onClick={(e) => go(page - 1, e.currentTarget)} aria-label="←">
        <Glyph name="chevron-left" size={14} />
      </button>
      {shown.map((p, i) => (
        <span key={p} className="pager-group">
          {i > 0 && p - shown[i - 1] > 1 && <span className="pager-gap">…</span>}
          <button aria-current={p === page ? "page" : undefined} onClick={(e) => go(p, e.currentTarget)}>
            {p + 1}
          </button>
        </span>
      ))}
      <button className="pager-step" disabled={page === pages - 1} onClick={(e) => go(page + 1, e.currentTarget)} aria-label="→">
        <Glyph name="chevron-right" size={14} />
      </button>
    </nav>
  );
}

export function usePaged<T>(items: T[], size: number) {
  const [page, setPage] = useState(0);
  const pages = Math.max(1, Math.ceil(items.length / size));
  const current = Math.min(page, pages - 1);
  return {
    items: items.slice(current * size, (current + 1) * size),
    pager: <Pager page={current} pages={pages} onPage={setPage} />,
  };
}
