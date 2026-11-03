import type { Metadata } from "next";
import { CATALOG } from "@/lib/catalog";
import { Wardrobe } from "./Wardrobe";

export const metadata: Metadata = { title: "Cosmetics", description: "Wings, capes, hats and more for the Kromvia client. Try them on." };

export default function Cosmetics() {
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Cosmetics</h1>
        <p>Try them on. In game, every Kromvia player on your server sees what you wear.</p>
      </header>
      <Wardrobe items={CATALOG} />
    </div>
  );
}
