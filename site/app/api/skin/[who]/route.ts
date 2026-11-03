export async function GET(_: Request, ctx: RouteContext<"/api/skin/[who]">) {
  const { who } = await ctx.params;
  if (!/^([0-9a-f]{32}|[0-9a-f-]{36}|[A-Za-z0-9_]{1,16})$/i.test(who)) return new Response("bad name", { status: 400 });
  const png = (await fromMojang(who)) ?? (await fromMcHeads(who));
  if (!png) return new Response("no skin", { status: 404 });
  return new Response(png, { headers: { "content-type": "image/png", "cache-control": "public, max-age=3600" } });
}

async function fromMojang(who: string) {
  try {
    let uuid = who.replace(/-/g, "");
    if (!/^[0-9a-f]{32}$/i.test(uuid)) {
      const res = await fetch(`https://api.mojang.com/users/profiles/minecraft/${who}`);
      if (!res.ok) return null;
      uuid = ((await res.json()) as { id: string }).id;
    }
    const profile = await fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${uuid}`);
    if (!profile.ok) return null;
    const { properties } = (await profile.json()) as { properties: { name: string; value: string }[] };
    const url: string | undefined = JSON.parse(atob(properties.find((p) => p.name === "textures")?.value ?? "e30=")).textures?.SKIN?.url;
    if (!url) return null;
    const png = await fetch(url.replace("http://", "https://"));
    return png.ok ? png.arrayBuffer() : null;
  } catch {
    return null;
  }
}

async function fromMcHeads(who: string) {
  try {
    const res = await fetch(`https://mc-heads.net/skin/${who}`);
    return res.ok && res.headers.get("content-type")?.startsWith("image/png") ? res.arrayBuffer() : null;
  } catch {
    return null;
  }
}
