import { player } from "@/lib/api";
import { OG_SIZE, ogCard } from "@/lib/og";

export const size = OG_SIZE;
export const contentType = "image/png";
export const alt = "Kromvia player profile";

export default async function Image({ params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  const p = await player(name);
  if (!p) return ogCard({ title: name, lines: ["kromvia.com"] });
  const who = p.verified && p.mc_uuid ? p.mc_uuid : p.name;
  const worn = (p.cosmetics.items?.length ?? 0) + (p.cosmetics.models?.length ?? 0) + (p.cosmetics.cape ? 1 : 0);
  return ogCard({
    art: `https://mc-heads.net/body/${who}/400`,
    title: p.name,
    badge: p.verified ? "Verified" : undefined,
    lines: [`♥ ${p.likes} likes · ${worn} cosmetics`, `kromvia.com/u/${p.name}`],
  });
}
