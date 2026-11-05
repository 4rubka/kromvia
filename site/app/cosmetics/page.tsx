import type { Metadata } from "next";
import { CATALOG } from "@/lib/catalog";
import { getLang } from "@/lib/i18n";
import { Wardrobe } from "./Wardrobe";

const T = {
  en: { meta: { title: "Cosmetics", description: "Voxel wings, helmets, armour and weapons for the Kromvia client, plus 10 full costumes. Try them on." }, head: "Cosmetics", lede: "Try on a whole costume or mix single pieces. In game, every Kromvia player on your server sees what you wear." },
  uk: { meta: { title: "Косметика", description: "Воксельні крила, шоломи, броня й зброя для клієнта Kromvia, а ще 10 повних костюмів. Приміряй." }, head: "Косметика", lede: "Приміряй цілий костюм або збери свій з окремих речей. У грі кожен гравець Kromvia на твоєму сервері бачить, що на тобі." },
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
