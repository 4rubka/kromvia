import Link from "next/link";
import { getLang } from "@/lib/i18n";

const T = {
  en: { head: "Nothing here", text: "The bee looked everywhere. This page doesn't exist, or it moved.", back: "Back home" },
  uk: { head: "Тут нічого", text: "Бджілка шукала всюди. Цієї сторінки нема, або вона переїхала.", back: "На головну" },
};

export default async function NotFound() {
  const t = T[await getLang()];
  return (
    <div className="wrap" style={{ display: "grid", justifyItems: "center", gap: 16, padding: "100px 0 120px", textAlign: "center" }}>
      <img src="/bee/not-found.webp" alt="" width={154} height={149} />
      <h1 style={{ fontSize: "clamp(32px, 5vw, 52px)" }}>{t.head}</h1>
      <p className="muted">{t.text}</p>
      <Link href="/" className="btn" style={{ marginTop: 8 }}>
        {t.back}
      </Link>
    </div>
  );
}
