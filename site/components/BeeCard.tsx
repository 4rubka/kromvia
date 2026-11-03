import type { Card } from "@/lib/content";
import s from "./BeeCard.module.css";

const TONES = ["violet", "blue", "indigo"] as const;

export function BeeCard({ card, index }: { card: Card; index: number }) {
  return (
    <div className={`block ${s.card}`} data-tone={TONES[index % TONES.length]}>
      <div className={s.text}>
        <h3>{card.title}</h3>
        <p>{card.text}</p>
      </div>
      <img src={`/bee/${card.bee}.webp`} alt="" className={s.bee} loading="lazy" />
    </div>
  );
}

export function BeeCards({ cards }: { cards: Card[] }) {
  return (
    <div className={s.grid}>
      {cards.map((c, i) => (
        <BeeCard key={c.title} card={c} index={i} />
      ))}
    </div>
  );
}
