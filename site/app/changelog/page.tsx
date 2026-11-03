import type { Metadata } from "next";
import { CHANGELOG } from "@/lib/changelog";
import s from "./changelog.module.css";

export const metadata: Metadata = { title: "Changelog", description: "What's new in the Kromvia launcher and client." };

export default function Changelog() {
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Changelog</h1>
        <p>Kromvia isn&apos;t out yet. This is what we built so far, newest first.</p>
      </header>
      <ol className={s.list}>
        {CHANGELOG.map((e) => (
          <li key={e.date} className={s.entry}>
            <time dateTime={e.date}>{new Date(e.date).toLocaleDateString("en", { day: "numeric", month: "long", year: "numeric" })}</time>
            <div className={`block ${s.card}`}>
              <h2>{e.title}</h2>
              <ul>
                {e.items.map((i) => (
                  <li key={i}>{i}</li>
                ))}
              </ul>
            </div>
          </li>
        ))}
      </ol>
    </div>
  );
}
