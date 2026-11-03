import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { share } from "@/lib/api";
import s from "./share.module.css";

export const metadata: Metadata = { title: "Shared from Kromvia" };

const LOADER_KEYS: Record<string, string> = { "fabric-loader": "Fabric", "quilt-loader": "Quilt", forge: "Forge", neoforge: "NeoForge" };

export default async function Shared({ params }: PageProps<"/s/[code]">) {
  const { code } = await params;
  const sh = await share(code);
  if (!sh) notFound();

  if (sh.kind === "screenshot")
    return (
      <div className={`wrap ${s.page}`}>
        <img src={sh.url} alt="Shared screenshot" className={s.shot} />
        <p className="dim">Shared from Kromvia · code {code.toUpperCase()}</p>
      </div>
    );

  if (sh.kind === "log")
    return (
      <div className={`wrap ${s.page}`}>
        <h1 className={s.title}>Shared log</h1>
        <pre className={`block ${s.log}`}>{sh.text}</pre>
      </div>
    );

  const { data, from } = sh;
  const loader = Object.entries(data.dependencies).find(([k]) => k !== "minecraft");
  const mods = data.files.filter((f) => f.path.startsWith("mods/"));
  const other = data.files.length - mods.length;
  return (
    <div className={`wrap ${s.page}`}>
      <h1 className={s.title}>{data.name}</h1>
      <div className={s.chips}>
        <span className="chip on">Minecraft {data.dependencies.minecraft}</span>
        {loader && <span className="chip">{LOADER_KEYS[loader[0]] ?? loader[0]} {loader[1]}</span>}
        <span className="chip">{mods.length} mods</span>
        {other > 0 && <span className="chip">{other} packs and shaders</span>}
      </div>
      <p className="muted">
        From <Link href={`/u/${from.name}`}>{from.name}</Link>. To install it, open any build in Kromvia, find "Install from a share code" and enter <b>{code.toUpperCase()}</b>.
      </p>
      <ul className={`block ${s.files}`}>
        {data.files.map((f) => (
          <li key={f.path}>
            <span>{f.path.split("/").pop()}</span>
            <span className="dim">{f.path.split("/")[0]}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
