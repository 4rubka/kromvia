import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Glyph } from "@/components/Glyph";
import { Player3D } from "@/components/Player3D";
import { player } from "@/lib/api";
import { itemName } from "@/lib/catalog";
import s from "./profile.module.css";

export async function generateMetadata({ params }: PageProps<"/u/[name]">): Promise<Metadata> {
  const p = await player((await params).name);
  return p ? { title: p.name, description: `${p.name} on Kromvia: skin, cosmetics and likes.` } : { title: "Player not found" };
}

export default async function Profile({ params }: PageProps<"/u/[name]">) {
  const p = await player((await params).name);
  if (!p) notFound();
  const worn = [...(p.cosmetics.items ?? []), ...(p.cosmetics.models ?? [])];
  const joined = new Date(p.created).toLocaleDateString("en", { month: "long", year: "numeric" });
  const badges = [
    p.created < Date.UTC(2027, 0, 1) && { name: "Early bird", text: "Joined in Kromvia's first year" },
    p.verified && { name: "Verified", text: "Linked a Minecraft account" },
    p.likes >= 10 && { name: "Liked", text: "10 likes or more" },
    p.likes >= 100 && { name: "Famous", text: "100 likes or more" },
  ].filter((b): b is { name: string; text: string } => !!b);

  return (
    <div className={`wrap ${s.page}`}>
      <div className={`block glass ${s.stageBox}`}>
        <Player3D className={s.stage} skin={`/api/skin/${p.verified && p.mc_uuid ? p.mc_uuid : p.name}`} cosmetics={p.cosmetics} zoom={0.75} />
      </div>
      <div className={s.info}>
        <h1 className={s.name}>
          {p.name}
          {p.verified && (
            <span className={s.verified} title="Verified Minecraft account">
              <Glyph name="verified" size={30} />
            </span>
          )}
        </h1>
        <div className={s.facts}>
          <div className="block">
            <b>{p.likes}</b>
            <span><Glyph name="heart" size={14} /> likes</span>
          </div>
          <div className="block">
            <b>{joined}</b>
            <span>joined</span>
          </div>
        </div>

        <h2 className={s.h2}>Wearing</h2>
        {worn.length || p.cosmetics.cape ? (
          <div className={s.chips}>
            {p.cosmetics.cape && <span className="chip on">Kromvia cape</span>}
            {worn.map((id) => (
              <span key={id} className="chip">{itemName(id)}</span>
            ))}
          </div>
        ) : (
          <p className="dim">Nothing yet.</p>
        )}

        {badges.length > 0 && (
          <>
            <h2 className={s.h2}>Badges</h2>
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
          Like {p.name} from the launcher: Profile → Top. <Link href="/download">Get Kromvia</Link>
        </p>
      </div>
    </div>
  );
}
