import type { Metadata } from "next";
import { FAQ, MORE_FAQ } from "@/lib/content";
import s from "./faq.module.css";

export const metadata: Metadata = { title: "FAQ", description: "Answers about the Kromvia launcher, client, cosmetics and Rooms." };

export default function Faq() {
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Questions and answers</h1>
      </header>
      <div className={s.list}>
        {[...FAQ, ...MORE_FAQ].map((f) => (
          <details key={f.q} className={`block ${s.qa}`}>
            <summary>{f.q}</summary>
            <p>{f.a}</p>
          </details>
        ))}
      </div>
    </div>
  );
}
