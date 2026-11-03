import type { Metadata } from "next";
import Link from "next/link";
import { BeeCards } from "@/components/BeeCard";
import { Glyph } from "@/components/Glyph";
import { CLIENT, LAUNCHER, MODULE_COUNT, MODULES, SOCIAL, VERSIONS } from "@/lib/content";
import s from "./features.module.css";

export const metadata: Metadata = { title: "Features", description: `Everything in the Kromvia launcher and its built-in client: ${MODULE_COUNT}+ modules, any version, any loader.` };

const LOADERS = ["Vanilla", "Fabric", "Quilt", "Forge", "NeoForge", "Legacy Fabric", "Ornithe"];

export default function Features() {
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Everything in Kromvia</h1>
        <p>The launcher handles versions, mods and Java. The built-in client adds the menu, HUD and cosmetics on top of every instance.</p>
      </header>

      <section className={s.block}>
        <h2>Launcher</h2>
        <BeeCards cards={[...LAUNCHER, ...SOCIAL]} />
        <div className={s.loaders}>
          {LOADERS.map((l) => (
            <span key={l} className="chip">{l}</span>
          ))}
        </div>
      </section>

      <section className={s.block} id="client">
        <h2>Built-in client</h2>
        <p className="muted">Runs on Fabric for Minecraft {VERSIONS}. Press Right Shift in any instance to open it.</p>
        <BeeCards cards={CLIENT} />
        <div className={s.modules}>
          {Object.entries(MODULES).map(([cat, list]) => (
            <div key={cat} className={`block ${s.cat}`}>
              <h3>
                {cat} <span className="dim">{list.length}</span>
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
          <Glyph name="download" size={22} /> Download Kromvia
        </Link>
      </div>
    </div>
  );
}
