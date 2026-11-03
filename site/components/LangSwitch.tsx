"use client";

import { useRouter } from "next/navigation";
import { LANGS, LANG_COOKIE, type Lang } from "@/lib/i18n-shared";

export function LangSwitch({ lang, className }: { lang: Lang; className?: string }) {
  const router = useRouter();
  const set = (code: Lang) => {
    document.cookie = `${LANG_COOKIE}=${code}; path=/; max-age=31536000; samesite=lax`;
    router.refresh();
  };
  return (
    <select className={className} value={lang} aria-label="Language" onChange={(e) => set(e.currentTarget.value as Lang)}>
      {Object.entries(LANGS).map(([code, name]) => (
        <option key={code} value={code}>
          {name}
        </option>
      ))}
    </select>
  );
}
