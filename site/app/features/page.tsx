import type { Metadata } from "next";
import Link from "next/link";
import { BeeCards } from "@/components/BeeCard";
import { Glyph } from "@/components/Glyph";
import { content, MODULE_COUNT, MODULES, VERSIONS } from "@/lib/content";
import { getLang } from "@/lib/i18n";
import s from "./features.module.css";

const T = {
  en: {
    meta: { title: "Features", description: `Everything in the Kromvia launcher and its built-in client: ${MODULE_COUNT}+ modules, any version, any loader.` },
    head: "Everything in Kromvia",
    lede: "The launcher handles versions, mods and Java. The built-in client adds the menu, HUD and cosmetics on top of every instance.",
    launcher: "Launcher", client: "Built-in client",
    clientNote: `Runs on Fabric for Minecraft ${VERSIONS}, and on Forge and NeoForge for 1.21 to 1.21.10. Press Right Shift in any instance to open it.`,
    download: "Download Kromvia",
  },
  uk: {
    meta: { title: "Можливості", description: `Усе в лаунчері Kromvia і його вбудованому клієнті: ${MODULE_COUNT}+ модулів, будь-яка версія, будь-який лоадер.` },
    head: "Усе, що є в Kromvia",
    lede: "Лаунчер бере на себе версії, моди й Java. Вбудований клієнт додає меню, HUD і косметику в кожну збірку.",
    launcher: "Лаунчер", client: "Вбудований клієнт",
    clientNote: `Працює на Fabric для Minecraft ${VERSIONS.replace(" to ", " до ")}, а на Forge і NeoForge для 1.21–1.21.10. Натисни Right Shift у будь-якій збірці, щоб відкрити.`,
    download: "Завантажити Kromvia",
  },
};

export async function generateMetadata(): Promise<Metadata> {
  return T[await getLang()].meta;
}

const LOADERS = ["Vanilla", "Fabric", "Quilt", "Forge", "NeoForge", "Legacy Fabric", "Ornithe"];

export default async function Features() {
  const lang = await getLang();
  const t = T[lang];
  const { CLIENT, LAUNCHER, SOCIAL, CATEGORIES } = content(lang);
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>{t.head}</h1>
        <p>{t.lede}</p>
      </header>

      <section className={s.block}>
        <h2>{t.launcher}</h2>
        <BeeCards cards={[...LAUNCHER, ...SOCIAL]} />
        <div className={s.loaders}>
          {LOADERS.map((l) => (
            <span key={l} className="chip">{l}</span>
          ))}
        </div>
      </section>

      <section className={s.block} id="client">
        <h2>{t.client}</h2>
        <p className="muted">{t.clientNote}</p>
        <BeeCards cards={CLIENT} />
        <div className={s.modules}>
          {Object.entries(MODULES).map(([cat, list]) => (
            <div key={cat} className={`block ${s.cat}`}>
              <h3>
                {CATEGORIES[cat] ?? cat} <span className="dim">{list.length}</span>
              </h3>
              <ul>
                {list.map((m) => (
                  <li key={m}>{m}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <div className={s.cta}>
        <Link href="/download" className="btn big">
          <Glyph name="download" size={22} /> {t.download}
        </Link>
      </div>
    </div>
  );
}
