import { cookies, headers } from "next/headers";
import { LANGS, LANG_COOKIE, type Lang } from "./i18n-shared";

export { LANGS, LANG_COOKIE, type Lang };

const isLang = (v: string | undefined): v is Lang => !!v && v in LANGS;

export async function getLang(): Promise<Lang> {
  const saved = (await cookies()).get(LANG_COOKIE)?.value;
  if (isLang(saved)) return saved;
  const accept = (await headers()).get("accept-language") ?? "";
  for (const part of accept.split(",")) {
    const code = part.split(";")[0].trim().toLowerCase().split("-")[0];
    if (isLang(code)) return code;
  }
  return "en";
}

export type Os = "windows" | "macos" | "linux";

export async function getOs(): Promise<Os | null> {
  const h = await headers();
  const hint = (h.get("sec-ch-ua-platform") ?? "").replace(/"/g, "").toLowerCase();
  const ua = (h.get("user-agent") ?? "").toLowerCase();
  if (/android|iphone|ipad/.test(ua)) return null;
  if (hint === "windows" || ua.includes("windows")) return "windows";
  if (hint === "macos" || ua.includes("mac os")) return "macos";
  if (hint === "linux" || ua.includes("linux") || ua.includes("x11")) return "linux";
  return null;
}

export const OS_NAME: Record<Os, string> = { windows: "Windows", macos: "macOS", linux: "Linux" };
