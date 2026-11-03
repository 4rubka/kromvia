import type { Metadata } from "next";
import { content } from "@/lib/content";
import { getLang } from "@/lib/i18n";
import s from "./faq.module.css";

const T = {
  en: { meta: { title: "FAQ", description: "Answers about the Kromvia launcher, client, cosmetics and Rooms." }, head: "Questions and answers" },
  uk: { meta: { title: "FAQ", description: "Відповіді про лаунчер Kromvia, клієнт, косметику й Кімнати." }, head: "Питання й відповіді" },
};

export async function generateMetadata(): Promise<Metadata> {
  return T[await getLang()].meta;
}

export default async function Faq() {
  const lang = await getLang();
  const { FAQ, MORE_FAQ } = content(lang);
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>{T[lang].head}</h1>
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
