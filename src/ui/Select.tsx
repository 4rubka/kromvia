import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from "react";
import { createPortal } from "react-dom";
import { Glyph } from "./Glyph";

export type Option = { value: string; label: string };

const LIST_HEIGHT = 280;
const SEARCH_FROM = 12;

export function Select({
  value,
  options,
  onChange,
  label,
  className,
  disabled,
}: {
  value: string;
  options: Option[];
  onChange: (value: string) => void;
  label?: string;
  className?: string;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [place, setPlace] = useState<CSSProperties>({});
  const [query, setQuery] = useState("");
  const button = useRef<HTMLButtonElement>(null);
  const list = useRef<HTMLUListElement>(null);
  const current = options.find((o) => o.value === value);
  const searchable = options.length >= SEARCH_FROM;
  const q = query.trim().toLowerCase();
  const shown = q ? options.filter((o) => o.label.toLowerCase().includes(q)) : options;

  useLayoutEffect(() => {
    if (!open) return;
    const r = button.current!.getBoundingClientRect();
    const [roomBelow, roomAbove] = [window.innerHeight - r.bottom - 12, r.top - 12];
    const below = roomBelow > LIST_HEIGHT || roomBelow >= roomAbove;
    const maxHeight = Math.min(LIST_HEIGHT, below ? roomBelow : roomAbove);
    setPlace(
      below
        ? { left: r.left, top: r.bottom + 4, minWidth: r.width, maxHeight }
        : { left: r.left, bottom: window.innerHeight - r.top + 4, minWidth: r.width, maxHeight },
    );
    setQuery("");
  }, [open]);

  useLayoutEffect(() => {
    const ul = list.current;
    const item = ul?.querySelector<HTMLElement>('[aria-selected="true"]');
    if (ul && item) ul.scrollTop = item.offsetTop - ul.clientHeight / 2;
  }, [place]);

  useEffect(() => {
    if (!open) return;
    const outside = (e: Event) => {
      const t = e.target as Node;
      if (!list.current?.contains(t) && !button.current?.contains(t)) setOpen(false);
    };
    const key = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    const close = () => setOpen(false);
    document.addEventListener("mousedown", outside);
    document.addEventListener("keydown", key);
    window.addEventListener("resize", close);
    document.addEventListener("scroll", outside, true);
    return () => {
      document.removeEventListener("mousedown", outside);
      document.removeEventListener("keydown", key);
      window.removeEventListener("resize", close);
      document.removeEventListener("scroll", outside, true);
    };
  }, [open]);

  return (
    <>
      <button
        ref={button}
        type="button"
        className={className ? `input select ${className}` : "input select"}
        disabled={disabled}
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <span>{current?.label ?? value}</span>
        <Glyph name="chevron-down" size={12} />
      </button>
      {open &&
        createPortal(
          <ul ref={list} className="select-list" role="listbox" aria-label={label} style={place}>
            {searchable && (
              <li className="select-search">
                <input className="input" autoFocus value={query} placeholder="1.21…" aria-label={label} onChange={(e) => setQuery(e.currentTarget.value)} />
              </li>
            )}
            {shown.map((o) => (
              <li key={o.value} role="option" aria-selected={o.value === value}>
                <button
                  type="button"
                  onClick={() => {
                    onChange(o.value);
                    setOpen(false);
                  }}
                >
                  {o.label}
                </button>
              </li>
            ))}
          </ul>,
          document.body,
        )}
    </>
  );
}
