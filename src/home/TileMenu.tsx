import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { useLauncher, type Instance } from "../lib/launcher";
import { Glyph } from "../ui/Glyph";
import { quickPlayWorlds, validAddress } from "../builds/InstanceExtras";

type World = { folder: string; name: string; lastPlayed: number };
type Server = { name: string; address: string };

export function TileMenu({ inst, x, y, onClose, onOpen }: { inst: Instance; x: number; y: number; onClose: () => void; onOpen: () => void }) {
  const { t } = useI18n();
  const l = useLauncher();
  const [worlds, setWorlds] = useState<World[]>([]);
  const [servers, setServers] = useState<Server[]>([]);
  const busy = !!l.runOf(inst.id) || !!l.jobOf(inst.id);
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState({ left: x, top: y });
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    setPos({ left: Math.max(8, Math.min(x, window.innerWidth - el.offsetWidth - 8)), top: Math.max(8, Math.min(y, window.innerHeight - el.offsetHeight - 8)) });
  }, [x, y, worlds.length, servers.length]);

  useEffect(() => {
    if (quickPlayWorlds(inst.mcVersion))
      invoke<World[]>("world_list", { instanceId: inst.id }).then((w) => setWorlds([...w].sort((a, b) => b.lastPlayed - a.lastPlayed).slice(0, 5)), () => {});
    invoke<Server[]>("saved_servers", { instanceId: inst.id }).then((s) => setServers(s.filter((x) => validAddress(x.address)).slice(0, 5)), () => {});
    const close = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [inst.id]);

  const pick = (f: () => void) => () => (f(), onClose());
  return createPortal(
    <div className="ctx-backdrop" onClick={onClose} onContextMenu={(e) => (e.preventDefault(), onClose())}>
      <div
        className="ctx-menu"
        role="menu"
        ref={ref}
        style={pos}
        onClick={(e) => e.stopPropagation()}
      >
        <button className="btn btn-ghost" role="menuitem" disabled={busy} onClick={pick(() => l.play(inst))}>
          <Glyph name="player-play" /> {t("game.play")}
        </button>
        <button className="btn btn-ghost" role="menuitem" onClick={pick(onOpen)}>
          <Glyph name="tool" /> {t("home.openBuild")}
        </button>
        {worlds.length > 0 && <div className="ctx-label">{t("builds.worlds")}</div>}
        {worlds.map((w) => (
          <button key={w.folder} className="btn btn-ghost" role="menuitem" disabled={busy} onClick={pick(() => l.play(inst, { world: w.folder }))}>
            <Glyph name="map" /> {w.name}
          </button>
        ))}
        {servers.length > 0 && <div className="ctx-label">{t("builds.servers")}</div>}
        {servers.map((s) => (
          <button key={s.address} className="btn btn-ghost" role="menuitem" disabled={busy} onClick={pick(() => l.play(inst, { join: s.address }))}>
            <Glyph name="broadcast" /> {s.name || s.address}
          </button>
        ))}
      </div>
    </div>,
    document.body,
  );
}
