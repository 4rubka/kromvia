import { useEffect, useState } from "react";

export type ThemePref = "system" | "light" | "dark";
const STORAGE_KEY = "kromvia.theme";

function initialTheme(): ThemePref {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === "light" || saved === "dark" || saved === "system") return saved;
  } catch {}
  return "dark";
}

export function useTheme() {
  const [theme, setTheme] = useState<ThemePref>(initialTheme);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === "system") delete root.dataset.theme;
    else root.dataset.theme = theme;
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {}
  }, [theme]);

  return [theme, setTheme] as const;
}
