import Link from "next/link";
import { BeeCards } from "@/components/BeeCard";
import { Glyph } from "@/components/Glyph";
import { FloatingCubes } from "@/components/FloatingCubes";
import { LauncherMock } from "@/components/LauncherMock";
import { Showcase } from "@/components/Showcase";
import { HomeMotion } from "@/components/HomeMotion";
import { content, MODULE_COUNT, MODULES, VERSIONS } from "@/lib/content";
import { getLang, getOs, OS_NAME } from "@/lib/i18n";
import { DISCORD_INVITE } from "@/lib/i18n-shared";
import s from "./home.module.css";

const ALL_MODULES = Object.values(MODULES).flat();
const ROWS = [0, 1, 2].map((r) => ALL_MODULES.filter((_, i) => i % 3 === r));

const T = {
  en: {
    title: "Minecraft launcher with its own built-in client",
    lede: `It runs any version on any loader. In game you get ${MODULE_COUNT}+ modules on Right Shift and cosmetics your friends can see.`,
    yourMods: "your mods",
    diff: [
      { title: "Your mods next to the client", text: "The client is a mod inside a normal instance. Put Sodium, shaders or anything from Modrinth and CurseForge right beside it." },
      { title: "Any version, any loader", text: "The launcher starts everything from alpha to the latest snapshot. The client runs on Fabric, and on Forge and NeoForge for 1.21 to 1.21.10." },
      { title: "Friends without a server", text: "Send a room code and your singleplayer world is open. No hosting, no port forwarding." },
    ],
    launch: "Launch your world", downloadFor: (os: string) => `Download for ${os}`, features: "See features",
    menuTitle: "Everything opens with Right Shift",
    menuText: "You don't have to hunt for mods. The Kromvia client is added to every instance, and its menu holds the HUD, PvP tools, visuals and cosmetics.",
    modules: `${MODULE_COUNT} modules`,
    dressTitle: "Dress up your player",
    dressText: "Wings, capes, hats and auras. Put them on in the launcher wardrobe or in game, and other Kromvia players on your server will see them.",
    browse: "Browse cosmetics",
    boring: "The launcher handles the boring part",
    friends: "Play with friends without running a server",
    roomsAlt: "A Kromvia bee opening a door",
    get: "Get Kromvia", platforms: "For Windows, macOS and Linux.", download: "Download Kromvia", discord: "Join our Discord",
  },
  uk: {
    title: "Лаунчер Minecraft з власним вбудованим клієнтом",
    lede: `Запускає будь-яку версію на будь-якому лоадері. У грі на Right Shift ${MODULE_COUNT}+ модулів і косметика, яку бачать друзі.`,
    yourMods: "твої моди",
    diff: [
      { title: "Твої моди поруч із клієнтом", text: "Клієнт це мод у звичайній збірці. Став поруч Sodium, шейдери чи будь-що з Modrinth і CurseForge." },
      { title: "Будь-яка версія, будь-який лоадер", text: "Лаунчер запускає все, від alpha до найсвіжішого снапшоту. Клієнт працює на Fabric, а на Forge і NeoForge для 1.21–1.21.10." },
      { title: "Друзі без сервера", text: "Кинь другу код кімнати, і твій одиночний світ відкритий. Без хостингу й прокидання портів." },
    ],
    launch: "Запустити свій світ", downloadFor: (os: string) => `Завантажити для ${os}`, features: "Усі можливості",
    menuTitle: "Усе відкривається на Right Shift",
    menuText: "Не треба шукати моди. Клієнт Kromvia додається в кожну збірку, а в його меню HUD, PvP-інструменти, візуали й косметика.",
    modules: `${MODULE_COUNT} модулів`,
    dressTitle: "Вдягни свого гравця",
    dressText: "Крила, плащі, капелюхи й аури. Вдягай у гардеробі лаунчера чи в грі, і інші гравці Kromvia на твоєму сервері це побачать.",
    browse: "Переглянути косметику",
    boring: "Нудну частину лаунчер бере на себе",
    friends: "Грай з друзями без свого сервера",
    roomsAlt: "Бджілка Kromvia відчиняє двері",
    get: "Отримати Kromvia", platforms: "Для Windows, macOS і Linux.", download: "Завантажити Kromvia", discord: "Наш Discord",
  },
};

export default async function Home() {
  const lang = await getLang();
  const os = await getOs();
  const t = T[lang];
  const { CLIENT, LAUNCHER, SOCIAL } = content(lang);
  return (
    <>
      <HomeMotion />
      <noscript>
        <style>{"[data-hide]{visibility:visible!important}"}</style>
      </noscript>
      <section className={s.hero} data-m="hero">
        <FloatingCubes />
        <div className={`wrap ${s.heroGrid}`}>
          <div className={s.heroText} data-m="hero-text">
            <h1 className={s.title} data-m="hero-title" data-hide>{t.title}</h1>
            <p className={s.lede} data-m="hero-lede" data-hide>{t.lede}</p>
            <div className={s.actions} data-m="hero-actions" data-hide>
              <Link href="/download" className="btn big">
                {os ? t.downloadFor(OS_NAME[os]) : t.launch} <Glyph name="arrow" size={22} />
              </Link>
              <Link href="/features" className={`btn ghost ${s.second}`}>
                {t.features}
              </Link>
            </div>
          </div>
          <div className={s.heroMock} data-m="hero-mock" data-hide>
            <div data-m="hero-tilt">
              <LauncherMock />
            </div>
          </div>
        </div>
      </section>

      <section className={s.diffSection}>
        <div className={`wrap ${s.diff}`}>
          <div className={s.diffItem}>
            <div className={s.stack} aria-hidden="true">
              <span className={s.stackOn}>Kromvia client</span>
              <span>Sodium</span>
              <span>Iris Shaders</span>
              <span className={s.stackMore}>+ {t.yourMods}</span>
            </div>
            <h3>{t.diff[0].title}</h3>
            <p>{t.diff[0].text}</p>
          </div>
          <div className={s.diffItem}>
            <div className={s.range} aria-hidden="true">
              <b>a1.0.4</b>
              <i />
              <b>{VERSIONS.split(" to ")[1]}</b>
            </div>
            <h3>{t.diff[1].title}</h3>
            <p>{t.diff[1].text}</p>
          </div>
          <div className={s.diffItem}>
            <div className={s.code} aria-hidden="true">
              {"K7Q2MX".split("").map((c, i) => (
                <kbd key={i}>{c}</kbd>
              ))}
            </div>
            <h3>{t.diff[2].title}</h3>
            <p>{t.diff[2].text}</p>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="wrap">
          <div className="section-head">
            <h2>{t.menuTitle}</h2>
            <p>{t.menuText}</p>
          </div>
          <BeeCards cards={CLIENT} />
        </div>
        <div className={s.marquee} aria-label={t.modules} data-m="marquee">
          {ROWS.map((row, i) => (
            <div key={i} className={s.marqueeRow} style={{ animationDuration: `${60 + i * 12}s`, animationDirection: i % 2 ? "reverse" : "normal" }}>
              {[...row, ...row].map((m, j) => (
                <span key={j} className="chip" aria-hidden={j >= row.length}>{m}</span>
              ))}
            </div>
          ))}
        </div>
      </section>

      <section className={`section ${s.cosmetics}`}>
        <div className={`wrap ${s.split}`} data-m="cosm">
          <Showcase />
          <div className="section-head">
            <h2>{t.dressTitle}</h2>
            <p>{t.dressText}</p>
            <div className={s.actionsLeft}>
              <Link href="/cosmetics" className="btn">
                {t.browse} <Glyph name="arrow" size={16} />
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="wrap">
          <div className="section-head center">
            <h2>{t.boring}</h2>
          </div>
          <BeeCards cards={LAUNCHER} />
        </div>
      </section>

      <section className="section">
        <div className={`wrap ${s.split}`}>
          <div className="section-head">
            <h2>{t.friends}</h2>
            {SOCIAL.map((c) => (
              <div key={c.title} className={s.socialRow} data-m="social">
                <div>
                  <b>{c.title}</b>
                  <p>{c.text}</p>
                </div>
              </div>
            ))}
          </div>
          <div className={s.roomsArt}>
            <img src="/bee/rooms.webp" alt={t.roomsAlt} className={s.roomsBee} data-m="rooms" />
          </div>
        </div>
      </section>



      <section className={s.final} data-m="final">
        <FloatingCubes />
        <div className={`wrap ${s.finalInner}`}>
          <img src="/bee/welcome.webp" alt="" width={190} height={151} />
          <h2>{t.get}</h2>
          <p className="muted">{t.platforms}</p>
          <Link href="/download" className="btn big">
            <Glyph name="download" size={22} /> {os ? t.downloadFor(OS_NAME[os]) : t.download}
          </Link>
          <a href={DISCORD_INVITE} target="_blank" rel="noopener" className="btn ghost">
            <Glyph name="discord" size={20} /> {t.discord}
          </a>
        </div>
      </section>
    </>
  );
}
