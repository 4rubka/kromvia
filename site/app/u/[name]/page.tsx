import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Glyph } from "@/components/Glyph";
import { Player3D } from "@/components/Player3D";
import { player } from "@/lib/api";
import { itemName } from "@/lib/catalog";
import { getLang } from "@/lib/i18n";
import s from "./profile.module.css";

const T = {
  en: {
    desc: (n: string) => `${n} on Kromvia: skin, cosmetics and likes.`, missing: "Player not found",
    verified: "Verified Minecraft account", likes: "likes", joined: "joined", wearing: "Wearing", cape: "Kromvia cape", nothing: "Nothing yet.", badges: "Badges",
    early: ["Early bird", "Joined in Kromvia's first year"], verifiedBadge: ["Verified", "Linked a Minecraft account"], liked: ["Liked", "10 likes or more"], famous: ["Famous", "100 likes or more"],
    like: (n: string) => `Like ${n} from the launcher: Profile → Top.`, get: "Get Kromvia",
  },
  uk: {
    desc: (n: string) => `${n} у Kromvia: скін, косметика й лайки.`, missing: "Гравця не знайдено",
    verified: "Підтверджений акаунт Minecraft", likes: "лайків", joined: "з нами від", wearing: "Вдягнено", cape: "Плащ Kromvia", nothing: "Поки нічого.", badges: "Бейджі",
    early: ["Рання пташка", "Приєднався в перший рік Kromvia"], verifiedBadge: ["Підтверджений", "Привʼязав акаунт Minecraft"], liked: ["Улюбленець", "10 лайків і більше"], famous: ["Зірка", "100 лайків і більше"],
    like: (n: string) => `Лайкнути ${n} можна в лаунчері: Профіль → Топ.`, get: "Отримати Kromvia",
  },
};

export async function generateMetadata({ params }: PageProps<"/u/[name]">): Promise<Metadata> {
  const [p, lang] = await Promise.all([player((await params).name), getLang()]);
  return p ? { title: p.name, description: T[lang].desc(p.name) } : { title: T[lang].missing };
}

export default async function Profile({ params }: PageProps<"/u/[name]">) {
  const [p, lang] = await Promise.all([player((await params).name), getLang()]);
  if (!p) notFound();
  const t = T[lang];
  const worn = [...(p.cosmetics.items ?? []), ...(p.cosmetics.models ?? [])];
  const joined = new Date(p.created).toLocaleDateString(lang, { month: "long", year: "numeric" });
  const badges = [
    p.created < Date.UTC(2027, 0, 1) && t.early,
    p.verified && t.verifiedBadge,
    p.likes >= 10 && t.liked,
    p.likes >= 100 && t.famous,
  ]
    .filter((b): b is string[] => !!b)
    .map(([name, text]) => ({ name, text }));

  return (
    <div className={`wrap ${s.page}`}>
      <div className={`block glass ${s.stageBox}`}>
        <Player3D className={s.stage} skin={`/api/skin/${p.verified && p.mc_uuid ? p.mc_uuid : p.name}`} cosmetics={p.cosmetics} zoom={0.75} />
      </div>
      <div className={s.info}>
        <h1 className={s.name}>
          {p.name}
          {p.verified && (
            <span className={s.verified} title={t.verified}>
              <Glyph name="verified" size={30} />
            </span>
          )}
        </h1>
        <div className={s.facts}>
          <div className="block">
            <b>{p.likes}</b>
            <span><Glyph name="heart" size={14} /> {t.likes}</span>
          </div>
          <div className="block">
            <b>{joined}</b>
            <span>{t.joined}</span>
          </div>
        </div>

        <h2 className={s.h2}>{t.wearing}</h2>
        {worn.length || p.cosmetics.cape ? (
          <div className={s.chips}>
            {p.cosmetics.cape && <span className="chip on">{t.cape}</span>}
            {worn.map((id) => (
              <span key={id} className="chip">{itemName(id)}</span>
            ))}
          </div>
        ) : (
          <p className="dim">{t.nothing}</p>
        )}

        {badges.length > 0 && (
          <>
            <h2 className={s.h2}>{t.badges}</h2>
            <div className={s.badges}>
              {badges.map((b) => (
                <div key={b.name} className={`block ${s.badge}`}>
                  <b>{b.name}</b>
                  <span>{b.text}</span>
                </div>
              ))}
            </div>
          </>
        )}

        <p className={s.like}>
          {t.like(p.name)} <Link href="/download">{t.get}</Link>
        </p>
      </div>
    </div>
  );
}
