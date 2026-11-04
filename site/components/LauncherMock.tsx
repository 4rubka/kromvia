"use client";

import { useEffect, useRef, useState } from "react";
import gsap from "gsap";
import { Icon, type IconName } from "@launcher/ui/Icon";
import { MODULE_COUNT, MODULES } from "@/lib/content";
import { Player3D } from "./Player3D";
import s from "./LauncherMock.module.css";

const W = 960;
const H = 600;

const RAIL: IconName[] = ["home", "library", "content", "wardrobe", "rooms", "friends", "profile", "settings"];

const SERVERS = [
  { name: "My SMP", online: "12", hue: 270 },
  { name: "Skyblock", online: "340", hue: 205 },
  { name: "Bedwars", online: "1.2k", hue: 140 },
];

const INSTANCES = [
  { name: "Survival with friends", meta: "1.21.11 · Fabric", hue: 140 },
  { name: "Create modpack", meta: "1.20.1 · Forge", hue: 30 },
  { name: "Vanilla", meta: "26.3", hue: 205 },
];

const TABS = [
  { name: "All", n: MODULE_COUNT },
  { name: "HUD", n: MODULES.HUD.length },
  { name: "PvP", n: MODULES.PvP.length },
  { name: "Utility", n: MODULES["Utility & visuals"].length },
];

const CARDS: { name: string; on: boolean; hue: number; flip?: boolean }[] = [
  { name: "Keystrokes", on: true, hue: 270 },
  { name: "CPS", on: true, hue: 200 },
  { name: "Armor Status", on: true, hue: 30 },
  { name: "FPS", on: true, hue: 140 },
  { name: "Coordinates", on: false, hue: 50 },
  { name: "Crosshair", on: true, hue: 0 },
  { name: "Hit Colour", on: false, hue: 340 },
  { name: "Motion Blur", on: false, hue: 230, flip: true },
  { name: "Trails", on: true, hue: 290 },
  { name: "Jump Circles", on: false, hue: 180 },
  { name: "Potion Effects", on: true, hue: 310 },
  { name: "Ping", on: false, hue: 100 },
  { name: "Freelook", on: true, hue: 160 },
  { name: "Clock", on: false, hue: 40 },
  { name: "Toggle Sneak", on: true, hue: 250 },
  { name: "Reach Display", on: false, hue: 10 },
];

function offsetIn(el: HTMLElement, root: HTMLElement) {
  let x = el.offsetWidth / 2;
  let y = el.offsetHeight / 2;
  for (let n: HTMLElement | null = el; n && n !== root; n = n.offsetParent as HTMLElement | null) {
    x += n.offsetLeft;
    y += n.offsetTop;
  }
  return { x, y };
}

function useDemo(stage: React.RefObject<HTMLDivElement | null>, win: React.RefObject<HTMLDivElement | null>, ready: boolean) {
  useEffect(() => {
    if (!ready) return;
    const root = win.current!;
    const q = (k: string) => root.querySelector<HTMLElement>(`[data-d=${k}]`)!;
    const [play, game, key, menu, cursor, flip] = ["play", "game", "key", "menu", "cursor", "flip"].map(q);
    const panel = menu.firstElementChild as HTMLElement;
    const cards = Array.from(menu.querySelectorAll("[data-card]"));
    const p = offsetIn(play, root);
    const at = `${p.x}px ${p.y}px`;

    const ctx = gsap.context(() => {
      if (matchMedia("(prefers-reduced-motion: reduce)").matches) {
        gsap.set(game, { clipPath: `circle(150% at ${at})`, autoAlpha: 1 });
        gsap.set(menu, { autoAlpha: 1 });
        return;
      }
      const tl = gsap.timeline({ repeat: -1, repeatDelay: 0.6, delay: 1.6 });
      tl.set(cursor, { x: W * 0.62, y: H * 0.95, autoAlpha: 0 })
        .set(game, { clipPath: `circle(0% at ${at})`, autoAlpha: 1 })
        .set(menu, { autoAlpha: 0 })
        .set(panel, { scale: 0.92 })
        .set(cards, { autoAlpha: 0, y: 8 })
        .set(key, { autoAlpha: 0, y: 30 })
        .set(flip, { attr: { "data-on": "false" } })
        .to(cursor, { autoAlpha: 1, duration: 0.2 })
        .to(cursor, { x: p.x - 4, y: p.y - 2, duration: 0.9, ease: "power3.inOut" })
        .to([cursor, play], { scale: 0.88, duration: 0.09, yoyo: true, repeat: 1 })
        .to(cursor, { autoAlpha: 0, duration: 0.2 })
        .to(game, { clipPath: `circle(150% at ${at})`, duration: 0.9, ease: "power2.in" }, "<")
        .to(key, { autoAlpha: 1, y: 0, duration: 0.45, ease: "back.out(2)" }, "+=0.5")
        .to(key, { y: 4, duration: 0.08, yoyo: true, repeat: 1 }, "+=0.25")
        .to(menu, { autoAlpha: 1, duration: 0.25 }, "<0.1")
        .to(panel, { scale: 1, duration: 0.45, ease: "back.out(1.6)" }, "<")
        .to(cards, { autoAlpha: 1, y: 0, duration: 0.3, stagger: { each: 0.025, grid: [4, 4] } }, "<0.1")
        .to(key, { autoAlpha: 0, y: 30, duration: 0.3 }, "+=0.3")
        .set(flip, { attr: { "data-on": "true" } }, "+=0.6")
        .to(flip, { scale: 1.05, duration: 0.12, yoyo: true, repeat: 1 }, "<")
        .to([menu, game], { autoAlpha: 0, duration: 0.6 }, "+=2.6");
      const io = new IntersectionObserver(([e]) => (e.isIntersecting ? tl.play() : tl.pause()));
      io.observe(stage.current!);
      return () => io.disconnect();
    }, root);
    return () => ctx.revert();
  }, [ready, stage, win]);
}

export function LauncherMock() {
  const box = useRef<HTMLDivElement>(null);
  const [scale, setScale] = useState<number | null>(null);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const win = useRef<HTMLDivElement>(null);
  useDemo(box, win, scale !== null);

  useEffect(() => {
    const el = box.current!;
    const ro = new ResizeObserver(() => setScale(Math.min(1, el.clientWidth / W)));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const move = (e: React.PointerEvent) => {
    if (e.pointerType !== "mouse") return;
    const r = box.current!.getBoundingClientRect();
    setTilt({ x: (e.clientX - r.left) / r.width - 0.5, y: (e.clientY - r.top) / r.height - 0.5 });
  };

  return (
    <div ref={box} className={s.stage} style={{ height: H * (scale ?? 1), opacity: scale === null ? 0 : 1, transition: "opacity 400ms" }} onPointerMove={move} onPointerLeave={() => setTilt({ x: 0, y: 0 })}>
      <div
        ref={win}
        className={s.window}
        style={{
          width: W,
          height: H,
          transform: `scale(${scale ?? 1}) perspective(2200px) rotateY(${-13 + tilt.x * 6}deg) rotateX(${4 - tilt.y * 4}deg)`,
        }}
        aria-label="The Kromvia launcher"
        role="img"
      >
        <div className={s.titlebar}>
          <img src="/logo-mark.svg" alt="" width={14} height={14} />
          <span>Kromvia</span>
          <i className={s.dots}>
            <b style={{ background: "#f2c94c" }} />
            <b style={{ background: "#7bd93a" }} />
            <b style={{ background: "#e45c5c" }} />
          </i>
        </div>
        <div className={s.body}>
          <nav className={s.rail}>
            <img src="/logo-mark.svg" alt="" width={26} height={26} className={s.railLogo} />
            {RAIL.map((name, i) => (
              <span key={name} className={i === 0 ? s.railOn : s.railItem}>
                <Icon name={name} size={16} />
              </span>
            ))}
          </nav>

          <main className={s.main}>
            <div className={`block ${s.quickbar}`}>
              <span className={s.label}>Quick play</span>
              {SERVERS.map((sv) => (
                <span key={sv.name} className={s.server} style={{ "--h": sv.hue } as React.CSSProperties}>
                  <b>{sv.name[0]}</b>
                  {sv.name}
                  <em>● {sv.online}</em>
                </span>
              ))}
              <span className={s.plus}>
                <Icon name="plus" size={10} />
              </span>
            </div>

            <div className={`block ${s.launchpad}`}>
              <img src="/bee/welcome.webp" alt="" className={s.padBee} />
              <div className={s.padCard}>
                <span className={s.label}>PvP 1.21.11</span>
                <span className={s.padMeta}>Fabric · Kromvia client</span>
                <span className={s.play} data-d="play">
                  <Icon name="play" size={16} /> Play
                </span>
              </div>
            </div>

            <div className={s.instances}>
              <strong>Instances</strong>
              <span className={`field ${s.search}`}>Search instances</span>
              <span className={`btn ghost ${s.import}`}>Import</span>
            </div>
            <div className={s.list}>
              {INSTANCES.map((it) => (
                <div key={it.name} className={`block ${s.row}`}>
                  <b style={{ "--h": it.hue } as React.CSSProperties} />
                  <span>
                    {it.name}
                    <em>{it.meta}</em>
                  </span>
                  <Icon name="play" size={12} />
                </div>
              ))}
            </div>
          </main>

          <aside className={s.aside}>
            <div className={`block ${s.side} ${s.playing}`}>
              <span className={s.label}>Playing as</span>
              <Player3D className={s.player} cosmetics={{ cape: true, style: "kromvia" }} zoom={0.9} turn={0.6} drag={false} />
              <strong className={s.nick}>Kromvia</strong>
              <span className={`field ${s.nickField}`}>Kromvia</span>
            </div>
            <div className={`block ${s.side}`}>
              <span className={s.label}>Rooms</span>
              <p>Play your singleplayer world with friends. No server, no port forwarding.</p>
              <span className={`btn ghost ${s.sideBtn}`}>
                <Icon name="rooms" size={10} /> Open rooms
              </span>
            </div>
            <div className={`block ${s.side}`}>
              <span className={s.label}>What&apos;s new</span>
              <b>Rooms</b>
              <p>Play your singleplayer world with friends through a code.</p>
            </div>
          </aside>
        </div>
        <Demo />
      </div>
    </div>
  );
}

function Demo() {
  return (
    <div className={s.demo} aria-hidden="true">
      <div className={s.game} data-d="game">
        <img src="/game/peak.webp" alt="" />
      </div>
      <div className={s.menu} data-d="menu">
        <div className={s.panel}>
          <div className={s.sidebar}>
            <div className={s.brand}>
              <img src="/logo-mark.svg" alt="" width={22} height={22} />
              <span>
                KROMVIA
                <em>client</em>
              </span>
            </div>
            {TABS.map((t, i) => (
              <span key={t.name} className={i === 0 ? s.tabOn : s.tab}>
                {t.name} <em>{t.n}</em>
              </span>
            ))}
            <span className={s.count}>
              {CARDS.filter((c) => c.on).length + 18} / {MODULE_COUNT} enabled
              <i style={{ width: `${((CARDS.filter((c) => c.on).length + 18) / MODULE_COUNT) * 100}%` }} />
            </span>
            <div className={s.sideBtns}>
              {["Profiles", "Edit HUD", "Wardrobe", "Done"].map((b) => (
                <span key={b}>{b}</span>
              ))}
            </div>
          </div>
          <div className={s.grid}>
            <span className={s.find}>⌕ Search modules…</span>
            <div className={s.cards}>
              {CARDS.map((c) => (
                <span key={c.name} className={s.card} data-card data-on={c.on} data-d={c.flip ? "flip" : undefined}>
                  <b style={{ "--h": c.hue } as React.CSSProperties} />
                  {c.name}
                  <i />
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
      <span className={s.key} data-d="key">
        <kbd>⇧ Right Shift</kbd>
      </span>
      <svg className={s.cursor} data-d="cursor" width="26" height="26" viewBox="0 0 24 24">
        <path d="M4 2l15 11.5-6.6.9 3.9 7.4-2.8 1.4-3.8-7.5L4 20.5z" fill="#fff" stroke="#111" strokeWidth="1.4" strokeLinejoin="round" />
      </svg>
    </div>
  );
}
