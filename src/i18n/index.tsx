import { en, type Dict, type MessageKey } from "./en";

type Locale = { name: string; default: Dict };
const LOCALES = import.meta.glob<Locale>("./locales/*.ts", { eager: true });
const STORAGE_KEY = "kromvia.lang";

const codeOf = (path: string) => path.slice("./locales/".length, -".ts".length);

export const LANGUAGES: Record<string, string> = {
  en: "English",
  ...Object.fromEntries(Object.entries(LOCALES).map(([path, m]) => [codeOf(path), m.name])),
};

function initialLanguage() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved && saved in LANGUAGES) return saved;
  } catch {}
  const system = navigator.language;
  return [system, system.split("-")[0]].find((c) => c in LANGUAGES) ?? "en";
}

export const language = initialLanguage();
document.documentElement.lang = language;
const dict: Partial<Dict> = LOCALES[`./locales/${language}.ts`]?.default ?? en;

export function setLanguage(code: string) {
  try {
    localStorage.setItem(STORAGE_KEY, code);
  } catch {}
  location.reload();
}

type Translate = (key: MessageKey, vars?: Record<string, string | number>) => string;

const t: Translate = (key, vars) => {
  const text = dict[key] ?? en[key];
  return vars ? text.replace(/\{(\w+)\}/g, (m, name) => String(vars[name] ?? m)) : text;
};

export function useI18n() {
  return { t };
}
