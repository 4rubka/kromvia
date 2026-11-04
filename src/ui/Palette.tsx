import { useEffect, useMemo, useRef, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { describe, useLauncher } from "../lib/launcher";
import type { Installed } from "../content/Content";
import { Glyph } from "./Glyph";

export const MOD_QUERY_KEY = "kromvia.modQuery";

type Item = { id: string; label: string; hint: string; icon: Parameters<typeof Glyph>[0]["name"]; run: () => void };

export function Palette<P extends string>({ pages, onPage, onClose }: { pages: P[]; onPage: (p: P) => void; onClose: () => void }) {
  const { t } = useI18n();
  const l = useLauncher();
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const [mods, setMods] = useState<Installed[]>([]);
  const list = useRef<HTMLUListElement>(null);
  const current = l.current;

  useEffect(() => {
    if (current) invoke<Installed[]>("content_list", { instanceId: current.id }).then(setMods, () => {});
  }, [current?.id]);

  const items = useMemo<Item[]>(() => {
    const go = (p: P) => () => onPage(p);
    const out: Item[] = pages.map((p) => ({ id: `page-${p}`, label: t(`nav.${p}` as MessageKey), hint: t("palette.page"), icon: "arrow-right", run: go(p) }));
    for (const i of l.instances) {
      out.push({ id: `open-${i.id}`, label: i.name, hint: describe(i, t("instance.vanilla")), icon: "tool", run: () => (l.setSelected(i.id), onPage("builds" as P)) });
      out.push({ id: `play-${i.id}`, label: `${t("game.play")}: ${i.name}`, hint: t("palette.play"), icon: "player-play", run: () => l.play(i) });
    }
    if (current)
      for (const m of mods)
        out.push({
          id: `mod-${m.folder}-${m.file}`,
          label: m.title,
          hint: `${current.name} · ${m.folder}`,
          icon: "search",
          run: () => {
            try {
              sessionStorage.setItem(MOD_QUERY_KEY, m.title);
            } catch {}
            l.setSelected(current.id);
            onPage("builds" as P);
          },
        });
    return out;
  }, [pages, l.instances, mods, current?.id, t]);

  const q = query.trim().toLowerCase();
  const shown = (q ? items.filter((i) => i.label.toLowerCase().includes(q) || i.hint.toLowerCase().includes(q)) : items.filter((i) => !i.id.startsWith("mod-"))).slice(0, 40);
  const pick = (i: Item | undefined) => {
    if (!i) return;
    onClose();
    i.run();
  };

  useEffect(() => setActive(0), [q]);
  useEffect(() => {
    list.current?.children[active]?.scrollIntoView({ block: "nearest" });
  }, [active]);

  return (
    <div className="overlay palette-overlay" role="dialog" aria-modal="true" aria-label={t("palette.title")} onClick={onClose}>
      <section className="card palette" onClick={(e) => e.stopPropagation()}>
        <input
          autoFocus
          className="input"
          placeholder={t("palette.placeholder")}
          aria-label={t("palette.title")}
          value={query}
          onChange={(e) => setQuery(e.currentTarget.value)}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") (e.preventDefault(), setActive((a) => Math.min(a + 1, shown.length - 1)));
            else if (e.key === "ArrowUp") (e.preventDefault(), setActive((a) => Math.max(a - 1, 0)));
            else if (e.key === "Enter") (e.preventDefault(), pick(shown[active]));
            else if (e.key === "Escape") onClose();
          }}
        />
        <ul className="palette-list" ref={list} role="listbox">
          {shown.map((i, n) => (
            <li key={i.id} role="option" aria-selected={n === active} onMouseEnter={() => setActive(n)} onClick={() => pick(i)}>
              <Glyph name={i.icon} />
              <span className="palette-label">{i.label}</span>
              <span className="muted small">{i.hint}</span>
            </li>
          ))}
          {shown.length === 0 && <li className="muted">{t("palette.empty")}</li>}
        </ul>
      </section>
    </div>
  );
}
