
export const WINDOW_MS = 60_000;

export function b64url(bytes: ArrayBuffer | Uint8Array): string {
  const u8 = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
  let s = "";
  for (const b of u8) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function fromB64url(s: string): Uint8Array | null {
  if (!/^[A-Za-z0-9_-]*$/.test(s)) return null;
  try {
    const bin = atob(s.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (s.length % 4)) % 4));
    return Uint8Array.from(bin, (c) => c.charCodeAt(0));
  } catch {
    return null;
  }
}

async function sha256Hex(data: Uint8Array): Promise<string> {
  const h = new Uint8Array(await crypto.subtle.digest("SHA-256", data));
  return Array.from(h, (b) => b.toString(16).padStart(2, "0")).join("");
}

export async function payload(method: string, pathAndQuery: string, time: string, body: Uint8Array) {
  return new TextEncoder().encode(`${method.toUpperCase()}\n${pathAndQuery}\n${time}\n${await sha256Hex(body)}`);
}

export async function verify(req: Request, body: Uint8Array, now = Date.now()): Promise<{ key: string } | { error: string }> {
  const key = req.headers.get("x-kromvia-key") ?? "";
  const time = req.headers.get("x-kromvia-time") ?? "";
  const sig = req.headers.get("x-kromvia-sig") ?? "";
  const raw = fromB64url(key);
  const sigBytes = fromB64url(sig);
  if (!raw || raw.length !== 32 || !sigBytes || sigBytes.length !== 64) return { error: "unsigned request" };
  if (b64url(raw) !== key || b64url(sigBytes) !== sig) return { error: "unsigned request" };
  if (!/^\d{1,15}$/.test(time) || Math.abs(now - Number(time)) > WINDOW_MS) return { error: "request expired, check the clock" };
  const url = new URL(req.url);
  const data = await payload(req.method, url.pathname + url.search, time, body);
  try {
    const pub = await crypto.subtle.importKey("raw", raw, { name: "Ed25519" }, false, ["verify"]);
    if (await crypto.subtle.verify({ name: "Ed25519" }, pub, sigBytes, data)) return { key };
  } catch {
  }
  return { error: "bad signature" };
}
