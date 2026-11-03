"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Glyph } from "./Glyph";
import s from "./Header.module.css";

const NAV = [
  {
    label: "Launcher",
    bee: "menu-launcher",
    title: "Launcher",
    links: [
      { href: "/download", label: "Download" },
      { href: "/features", label: "Features" },
      { href: "/changelog", label: "Changelog" },
      { href: "/faq", label: "FAQ" },
    ],
  },
  {
    label: "Client",
    bee: "menu-client",
    title: "Built-in client",
    links: [
      { href: "/features#client", label: "Modules" },
      { href: "/cosmetics", label: "Cosmetics" },
    ],
  },
  {
    label: "Community",
    bee: "menu-community",
    title: "Community",
    links: [
      { href: "/top", label: "Top players" },
      { href: "/rooms", label: "Rooms" },
    ],
  },
];

export function Header() {
  const path = usePathname();
  const [menu, setMenu] = useState(false);
  const [drop, setDrop] = useState<number | null>(null);
  const [scrolled, setScrolled] = useState(false);
  const close = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    setMenu(false);
    setDrop(null);
  }, [path]);
  useEffect(() => {
    const on = () => setScrolled(window.scrollY > 8);
    on();
    window.addEventListener("scroll", on, { passive: true });
    const esc = (e: KeyboardEvent) => e.key === "Escape" && setDrop(null);
    window.addEventListener("keydown", esc);
    return () => {
      window.removeEventListener("scroll", on);
      window.removeEventListener("keydown", esc);
    };
  }, []);

  const enter = (i: number) => {
    clearTimeout(close.current);
    setDrop(i);
  };
  const leave = () => {
    close.current = setTimeout(() => setDrop(null), 160);
  };

  return (
    <>
      <div className={s.veil} data-on={drop !== null} aria-hidden="true" onClick={() => setDrop(null)} />
      <header className={s.header} data-scrolled={scrolled || menu}>
        <div className={`wrap ${s.row}`}>
          <Link href="/" className={s.brand} aria-label="Kromvia home" onClick={() => path === "/" && window.scrollTo({ top: 0, behavior: "smooth" })}>
            <img src="/logo-mark.svg" alt="" width={30} height={30} />
            <span>Kromvia</span>
          </Link>
          <nav className={s.nav} data-open={menu}>
            {NAV.map((g, i) => (
              <div key={g.label} className={s.group} onPointerEnter={(e) => e.pointerType === "mouse" && enter(i)} onPointerLeave={(e) => e.pointerType === "mouse" && leave()}>
                <button className={s.trigger} aria-expanded={drop === i} onClick={() => setDrop(drop === i ? null : i)}>
                  {g.label}
                  <span className={s.chev}>
                    <svg width="10" height="10" viewBox="0 0 10 10" aria-hidden="true">
                      <path d="M2 3.5L5 6.5L8 3.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
                    </svg>
                  </span>
                </button>
                <div className={s.drop} data-on={drop === i}>
                  <div className={s.panel}>
                    <span className={s.dropTitle}>{g.title}</span>
                    {g.links.map((l) => (
                      <Link key={l.href} href={l.href} onClick={() => setDrop(null)}>
                        {l.label}
                      </Link>
                    ))}
                  </div>
                  <img src={`/bee/${g.bee}.webp`} alt="" className={s.peek} />
                </div>
              </div>
            ))}
            <Link href="/download" className={`btn ${s.download}`}>
              <Glyph name="download" size={16} /> Download
            </Link>
          </nav>
          <button className={s.menu} aria-label={menu ? "Close menu" : "Open menu"} aria-expanded={menu} onClick={() => setMenu(!menu)}>
            <Glyph name={menu ? "x" : "menu"} size={22} />
          </button>
        </div>
      </header>
    </>
  );
}
