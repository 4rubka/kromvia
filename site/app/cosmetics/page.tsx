import type { Metadata } from "next";
import { CATALOG } from "@/lib/catalog";
import { getLang } from "@/lib/i18n";
import { Wardrobe } from "./Wardrobe";

const T = {
  en: { meta: { title: "Cosmetics", description: "Wings, capes, hats and more for the Kromvia client. Try them on." }, head: "Cosmetics", lede: "Try them on. In game, every Kromvia player on your server sees what you wear." },
  uk: { meta: { title: "Косметика", description: "Крила, плащі, капелюхи й не тільки для клієнта Kromvia. Приміряй." }, head: "Косметика", lede: "Приміряй. У грі кожен гравець Kromvia на твоєму сервері бачить, що на тобі." },
};

export async function generateMetadata(): Promise<Metadata> {
  return T[await getLang()].meta;
}

export default async function Cosmetics() {
  const t = T[await getLang()];
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>{t.head}</h1>
        <p>{t.lede}</p>
      </header>
      <Wardrobe items={CATALOG} />
    </div>
  );
}
