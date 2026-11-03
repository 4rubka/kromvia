import type { Metadata } from "next";
import Link from "next/link";
import { Glyph } from "@/components/Glyph";
import { top } from "@/lib/api";
import s from "./top.module.css";

export const metadata: Metadata = { title: "Top players", description: "The most liked players on Kromvia." };

export default async function Top() {
  const list = await top();
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Top players</h1>
        <p>The most liked Kromvia players. To like someone, open Profile → Top in the launcher.</p>
      </header>
      {list === null ? (
        <Empty bee="offline" text="Can't reach Kromvia servers right now. Try again in a minute." />
      ) : list.length === 0 ? (
        <Empty bee="friends-alone" text="Nobody has any likes yet. Be the first." />
      ) : (
        <ol className={s.list}>
          {list.map((p, i) => (
            <li key={p.id}>
              <Link href={`/u/${p.name}`} className={`block ${s.row}`} data-rank={i < 3 ? i + 1 : undefined}>
                <span className={s.rank}>{i + 1}</span>
                <img src={`https://mc-heads.net/avatar/${p.name}/40`} alt="" width={40} height={40} className={s.head} />
                <span className={s.name}>
                  {p.name}
                  {p.verified && <Glyph name="verified" size={16} />}
                </span>
                <span className={s.likes}>
                  <Glyph name="heart" size={16} /> {p.likes}
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function Empty({ bee, text }: { bee: string; text: string }) {
  return (
    <div className={s.empty}>
      <img src={`/bee/${bee}.webp`} alt="" />
      <p className="muted">{text}</p>
    </div>
  );
}
