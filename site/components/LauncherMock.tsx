"use client";

import { useEffect, useRef, useState } from "react";
import { Icon, type IconName } from "@launcher/ui/Icon";
import { Player3D } from "./Player3D";
import s from "./LauncherMock.module.css";

const W = 960;
const H = 600;

const RAIL: IconName[] = ["home", "library", "content", "wardrobe", "rooms", "friends", "profile", "settings"];

const SERVERS = [
  { name: "Hypixel", online: "27k", hue: 38 },
  { name: "CubeCraft", online: "1.5k", hue: 205 },
  { name: "Wynncraft", online: "1.6k", hue: 140 },
];

export function LauncherMock() {
  const box = useRef<HTMLDivElement>(null);
  const [scale, setScale] = useState<number | null>(null);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });

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
                <strong>New instance</strong>
                <span>Create an instance: pick a version and a loader, the rest is automatic.</span>
              </div>
            </div>

            <div className={s.instances}>
              <strong>Instances</strong>
              <span className={`field ${s.search}`}>Search instances</span>
              <span className={`btn ghost ${s.import}`}>Import</span>
            </div>
            <p className={s.empty}>No instances yet. Create one to play.</p>
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
      </div>
    </div>
  );
}
