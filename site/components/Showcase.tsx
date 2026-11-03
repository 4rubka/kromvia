"use client";

import { useEffect, useState } from "react";
import { shot } from "@launcher/art/gltf";
import { boxItem, boxModel } from "@launcher/art/procedural";
import { Player3D } from "./Player3D";
import s from "./Showcase.module.css";

const PICKS = ["crown", "halo", "wizard_hat", "headphones", "sunglasses", "dragon_wings", "fairy_wings", "jetpack", "fox_tail", "chain"];

export function Showcase() {
  const [worn, setWorn] = useState<Record<string, string>>({ head: "crown", back: "dragon_wings" });
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    if (touched) return;
    let i = 0;
    const id = setInterval(() => {
      i = (i + 1) % PICKS.length;
      const item = boxItem(PICKS[i]);
      if (item) setWorn((w) => ({ ...w, [item.slot]: item.id }));
    }, 2600);
    return () => clearInterval(id);
  }, [touched]);

  const toggle = (id: string) => {
    const slot = boxItem(id)!.slot;
    setTouched(true);
    setWorn((w) => {
      const next = { ...w };
      if (next[slot] === id) delete next[slot];
      else next[slot] = id;
      return next;
    });
  };

  return (
    <div className={s.showcase}>
      <div className={s.stage}>
        <div className={s.glow} />
        <Player3D className={s.player} cosmetics={{ items: Object.values(worn) }} zoom={0.78} turn={0.7} />
        <svg className={s.pedestal} viewBox="0 0 280 110" aria-hidden="true">
          <path d="M140 0L280 40L140 80L0 40Z" fill="#c9b5ff" />
          <path d="M0 40L140 80V110L0 70Z" fill="#8b5cf6" />
          <path d="M280 40L140 80V110L280 70Z" fill="#2f55c8" />
        </svg>
      </div>
      <div className={s.picks}>
        {PICKS.map((id) => (
          <Pick key={id} id={id} on={Object.values(worn).includes(id)} onClick={() => toggle(id)} />
        ))}
      </div>
    </div>
  );
}

function Pick({ id, on, onClick }: { id: string; on: boolean; onClick: () => void }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    shot(`box:${id}`, async () => boxModel(id), 160).then(setSrc, () => {});
  }, [id]);
  return (
    <button className={s.pick} aria-pressed={on} onClick={onClick} title={boxItem(id)?.name}>
      {src && <img src={src} alt={boxItem(id)?.name ?? id} />}
    </button>
  );
}
