import Link from "next/link";
import { BeeCards } from "@/components/BeeCard";
import { Glyph } from "@/components/Glyph";
import { FloatingCubes } from "@/components/FloatingCubes";
import { LauncherMock } from "@/components/LauncherMock";
import { Showcase } from "@/components/Showcase";
import { CLIENT, LAUNCHER, MODULE_COUNT, MODULES, SOCIAL } from "@/lib/content";
import s from "./home.module.css";

const ALL_MODULES = Object.values(MODULES).flat();
const ROWS = [0, 1, 2].map((r) => ALL_MODULES.filter((_, i) => i % 3 === r));

export default function Home() {
  return (
    <>
      <section className={s.hero}>
        <video className={s.sky} autoPlay muted loop playsInline aria-hidden="true">
          <source src="/hero.webm" type="video/webm" />
          <source src="/hero.mp4" type="video/mp4" />
        </video>
        <FloatingCubes />
        <div className={`wrap ${s.heroGrid}`}>
          <div className={s.heroText}>
            <h1 className={s.title}>Minecraft launcher with its own built-in client</h1>
            <p className={s.lede}>It runs any version on any loader. In game you get {MODULE_COUNT}+ modules on Right Shift and cosmetics your friends can see.</p>
            <div className={s.actions}>
              <Link href="/download" className="btn big">
                Launch your world <Glyph name="arrow" size={22} />
              </Link>
              <Link href="/features" className={`btn ghost ${s.second}`}>
                See features
              </Link>
            </div>
          </div>
          <div className={s.heroMock}>
            <LauncherMock />
          </div>
        </div>
      </section>


      <section className="section">
        <div className="wrap">
          <div className="section-head">
            <h2>Everything opens with Right Shift</h2>
            <p>You don&apos;t have to hunt for mods. The Kromvia client is added to every instance, and its menu holds the HUD, PvP tools, visuals and cosmetics.</p>
          </div>
          <BeeCards cards={CLIENT} />
        </div>
        <div className={s.marquee} aria-label={`${MODULE_COUNT} modules`}>
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
        <div className={`wrap ${s.split}`}>
          <Showcase />
          <div className="section-head">
            <h2>Dress up your player</h2>
            <p>Wings, capes, hats and auras. Put them on in the launcher wardrobe or in game, and other Kromvia players on your server will see them.</p>
            <div className={s.actionsLeft}>
              <Link href="/cosmetics" className="btn">
                Browse cosmetics <Glyph name="arrow" size={16} />
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="wrap">
          <div className="section-head center">
            <h2>The launcher handles the boring part</h2>
          </div>
          <BeeCards cards={LAUNCHER} />
        </div>
      </section>

      <section className="section">
        <div className={`wrap ${s.split}`}>
          <div className="section-head">
            <h2>Play with friends without running a server</h2>
            {SOCIAL.map((c) => (
              <div key={c.title} className={s.socialRow}>
                <div>
                  <b>{c.title}</b>
                  <p>{c.text}</p>
                </div>
              </div>
            ))}
          </div>
          <div className={s.roomsArt}>
            <img src="/bee/rooms.webp" alt="A Kromvia bee opening a door" className={s.roomsBee} />
          </div>
        </div>
      </section>



      <section className={s.final}>
        <FloatingCubes />
        <div className={`wrap ${s.finalInner}`}>
          <img src="/bee/welcome.webp" alt="" width={190} height={151} />
          <h2>Get Kromvia</h2>
          <p className="muted">For Windows, macOS and Linux.</p>
          <Link href="/download" className="btn big">
            <Glyph name="download" size={22} /> Download Kromvia
          </Link>
        </div>
      </section>
    </>
  );
}
