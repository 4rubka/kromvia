import { afterEach, expect, test } from "bun:test";
import { downloads, manifest } from "../src/update";

const real = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = real;
});

test("points release files back through the worker", async () => {
  const seen: string[] = [];
  globalThis.fetch = (async (url: string, init: RequestInit) => {
    seen.push(`${url} ${(init.headers as Record<string, string>).authorization}`);
    if (url.endsWith("/releases/latest")) {
      return Response.json({ assets: [{ id: 1, name: "latest.json" }, { id: 2, name: "Kromvia_0.2.0_amd64.AppImage" }] });
    }
    return Response.json({
      version: "0.2.0",
      platforms: {
        "linux-x86_64": { url: "https://github.com/4rubka/kromvia/releases/download/v0.2.0/Kromvia_0.2.0_amd64.AppImage", signature: "sig" },
        "windows-x86_64": { url: "https://github.com/x/missing.exe", signature: "sig" },
      },
    });
  }) as typeof fetch;
  const out = (await manifest("tok", "https://api.kromvia.com")) as any;
  expect(out.platforms["linux-x86_64"]).toEqual({ url: "https://api.kromvia.com/update/asset/2", signature: "sig" });
  expect(out.platforms["windows-x86_64"].url).toBe("https://github.com/x/missing.exe");
  expect(seen.every((s) => s.endsWith("Bearer tok"))).toBe(true);
});

test("no release means no manifest", async () => {
  globalThis.fetch = (async () => new Response("", { status: 404 })) as unknown as typeof fetch;
  expect(await manifest("tok", "https://api.kromvia.com")).toBeNull();
});

test("downloads pick one installer per platform", async () => {
  globalThis.fetch = (async () =>
    Response.json({
      tag_name: "v0.3.0",
      published_at: "2026-11-03T10:00:00Z",
      assets: [
        { id: 1, name: "latest.json", size: 900 },
        { id: 2, name: "Kromvia_0.3.0_x64-setup.exe", size: 10 },
        { id: 3, name: "Kromvia_0.3.0_x64-setup.exe.sig", size: 1 },
        { id: 4, name: "Kromvia_0.3.0_aarch64.dmg", size: 20 },
        { id: 5, name: "Kromvia_0.3.0_amd64.AppImage", size: 30 },
      ],
    })) as unknown as typeof fetch;
  const out = await downloads("tok");
  expect(out?.version).toBe("0.3.0");
  expect(out?.files.map((f) => [f.platform, f.id])).toEqual([["windows", 2], ["macos", 4], ["linux-appimage", 5]]);
});
