import { useEffect, useRef, useState } from "react";
import { IdleAnimation, SkinViewer } from "skinview3d";
import { useI18n } from "../i18n";
import { Glyph } from "../ui/Glyph";
import type { MessageKey } from "../i18n/en";
import { MODELS, type ModelEntry, type Slot } from "../art/catalog";
import { modelShot, shot } from "../art/gltf";
import { BOX_ITEMS, PALETTE, RAINBOW, boxItem, boxModel, rainbow, wearBox, type BoxItem } from "../art/procedural";
import { wear, wearable } from "../art/wear";
import { effectIcon } from "../art/effectIcons";
import { capeTexture, type CapeStyle } from "../art/capes";
import { wearDevCape } from "../art/devCape";
import SETS from "../art/sets.json";
import { OUTFIT_EVENT, pushOutfit, savedOutfit, storeOutfit, type Back, type Outfit } from "../lib/outfit";

const SLOTS: (Slot | "effect")[] = ["head", "face", "neck", "back", "weapon", "body", "feet", "effect"];

const colorOf = (o: Outfit, id: string) => o.colors?.[id] ?? boxItem(id)?.color ?? 0xffffffff;
const STYLES: CapeStyle[] = ["kromvia", "ember", "frost"];

type CosmeticSet = { id: string; name: string; items: string[]; effect?: string };

function withSet(o: Outfit, set: CosmeticSet): Outfit {
  const equipped = { ...o.equipped };
  for (const id of set.items) {
    const item = boxItem(id);
    if (item) equipped[item.slot] = id;
  }
  if (set.effect) equipped.effect = set.effect;
  return { ...o, equipped, back: equipped.back ? "none" : o.back };
}

function Stage({ skin, slim, outfit }: { skin: string | null; slim: boolean; outfit: Outfit }) {
  const host = useRef<HTMLDivElement>(null);
  const canvas = useRef<HTMLCanvasElement>(null);
  const [viewer, setViewer] = useState<SkinViewer | null>(null);

  useEffect(() => {
    const el = host.current!;
    let v: SkinViewer;
    try {
      v = new SkinViewer({ canvas: canvas.current!, width: el.clientWidth || 280, height: el.clientHeight || 380, fov: 30 });
    } catch {
      return;
    }
    v.animation = new IdleAnimation();
    v.autoRotate = true;
    v.autoRotateSpeed = 0.5;
    v.controls.enableZoom = false;
    v.zoom = 0.75;
    v.globalLight.intensity = 2.4;
    const ro = new ResizeObserver(() => v.setSize(el.clientWidth, el.clientHeight));
    ro.observe(el);
    setViewer(v);
    return () => {
      ro.disconnect();
      v.dispose();
    };
  }, []);

  useEffect(() => {
    if (!viewer) return;
    if (skin) viewer.loadSkin(skin, { model: slim ? "slim" : "default" });
  }, [viewer, skin, slim]);

  useEffect(() => {
    if (!viewer) return;
    if (outfit.back === "none" || outfit.equipped.back) viewer.loadCape(null);
    else if (outfit.back === "cape" && outfit.dev) {
      const off = wearDevCape(viewer);
      return () => {
        off();
        viewer.loadCape(null);
      };
    } else {
      viewer.loadCape(capeTexture(outfit.style), { backEquipment: outfit.back });
      if (outfit.back === "elytra") {
        const e = viewer.playerObject.elytra;
        e.leftWing.rotation.set(Math.PI / 12, 0, Math.PI / 12);
        e.updateRightWing();
      }
    }
  }, [viewer, outfit.back, outfit.style, outfit.equipped.back, outfit.dev]);

  const worn = SLOTS.map((s) => outfit.equipped[s]).filter((id): id is string => !!id && wearable(id));
  const key = worn.join(",");
  useEffect(() => {
    if (!viewer) return;
    let offs: (() => void)[] = [];
    let alive = true;
    Promise.all(worn.map((id) => wear(viewer, id))).then((fns) => {
      if (alive) offs = fns;
      else fns.forEach((f) => f());
    });
    return () => {
      alive = false;
      offs.forEach((f) => f());
    };
  }, [viewer, key]);

  const latest = useRef(outfit);
  latest.current = outfit;
  const boxes = SLOTS.map((s) => outfit.equipped[s]).filter((id): id is string => !!id && !!boxItem(id)?.pieces.length);
  const boxKey = boxes.join(",");
  useEffect(() => {
    if (!viewer) return;
    const offs = boxes.map((id) => wearBox(viewer, id, () => colorOf(latest.current, id)));
    return () => offs.forEach((f) => f());
  }, [viewer, boxKey]);

  return (
    <div className="cosmetic-stage" ref={host}>
      <canvas ref={canvas} />
    </div>
  );
}

function BoxCard({ item, on, onClick }: { item: BoxItem; on: boolean; onClick: () => void }) {
  const [src, setSrc] = useState<string | null>(() => (item.pieces.length ? null : effectIcon(item.id)));
  useEffect(() => {
    if (item.pieces.length) shot(`box:${item.id}`, async () => boxModel(item.id)).then(setSrc, () => {});
  }, [item.id]);
  return (
    <button className="cosmetic-card" aria-pressed={on} onClick={onClick} title={item.name}>
      {src ? <img src={src} alt="" /> : <span className="cosmetic-ph" />}
      <span>{item.name}</span>
    </button>
  );
}

function Swatches({ value, onPick }: { value: number; onPick: (c: number) => void }) {
  const [, redraw] = useState(0);
  useEffect(() => {
    if (value !== RAINBOW) return;
    const id = setInterval(() => redraw((n) => n + 1), 100);
    return () => clearInterval(id);
  }, [value]);
  return (
    <div className="cosmetic-swatches">
      {[...PALETTE, RAINBOW].map((c) => (
        <button
          key={c}
          aria-pressed={value === c}
          style={{ background: `#${(c === RAINBOW ? rainbow() : c & 0xffffff).toString(16).padStart(6, "0")}` }}
          onClick={() => onPick(c)}
        />
      ))}
    </div>
  );
}

function Card({ m, on, onClick }: { m: ModelEntry; on: boolean; onClick: () => void }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    modelShot(m.id).then(setSrc, () => {});
  }, [m.id]);
  return (
    <button className="cosmetic-card" aria-pressed={on} onClick={onClick} title={m.name}>
      {src ? <img src={src} alt="" /> : <span className="cosmetic-ph" />}
      <span>{m.name}</span>
    </button>
  );
}

export function Cosmetics({ skin, slim }: { skin: string | null; slim: boolean }) {
  const { t } = useI18n();
  const [outfit, setOutfit] = useState<Outfit>(savedOutfit);

  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    storeOutfit(outfit);
    pushOutfit(outfit).catch(() => {});
  }, [outfit]);
  useEffect(() => {
    const reload = () => setOutfit((o) => (JSON.stringify(o) === JSON.stringify(savedOutfit()) ? o : savedOutfit()));
    window.addEventListener(OUTFIT_EVENT, reload);
    return () => window.removeEventListener(OUTFIT_EVENT, reload);
  }, []);

  const pick = (slot: Slot | "effect", id: string) => {
    setOutfit((o) => ({ ...o, equipped: { ...o.equipped, [slot]: o.equipped[slot] === id ? undefined : id } }));
  };
  const [preview, setPreview] = useState<number | null>(null);
  const sets = SETS as CosmeticSet[];
  const step = (d: number) =>
    setPreview((i) => {
      const next = (((i ?? -1) + 1 + d) % (sets.length + 1) + sets.length + 1) % (sets.length + 1);
      return next === 0 ? null : next - 1;
    });
  const shown = preview === null ? outfit : withSet(outfit, sets[preview]);
  const recolor = (id: string, c: number) => setOutfit((o) => ({ ...o, colors: { ...o.colors, [id]: c } }));

  return (
    <div className="cosmetics">
      <section className="card cosmetics-stage-card">
        <Stage skin={skin} slim={slim} outfit={shown} />
        <div className="cosmetic-sets">
          <button className="btn btn-ghost" aria-label={t("cosmetics.prevSet")} onClick={() => step(-1)}>
            <Glyph name="chevron-left" />
          </button>
          <div className="cosmetic-set-name">
            <small className="muted">{t("cosmetics.sets")}</small>
            <strong>{preview === null ? t("cosmetics.yourLook") : sets[preview].name}</strong>
          </div>
          <button className="btn btn-ghost" aria-label={t("cosmetics.nextSet")} onClick={() => step(1)}>
            <Glyph name="chevron-right" />
          </button>
        </div>
        {preview !== null && (
          <div className="cosmetic-set-actions">
            <button className="btn" onClick={() => (setOutfit(shown), setPreview(null))}>
              <Glyph name="check" /> {t("cosmetics.wearSet")}
            </button>
            <button className="btn btn-ghost" onClick={() => setPreview(null)}>
              {t("common.cancel")}
            </button>
          </div>
        )}
        <div className="cosmetic-controls">
          <div className="segmented" role="group" aria-label={t("cosmetics.back")}>
            {(["none", "cape", "elytra"] as Back[]).map((b) => (
              <button key={b} aria-pressed={outfit.back === b} onClick={() => setOutfit((o) => ({ ...o, back: b }))}>
                {t(`cosmetics.back.${b}` as MessageKey)}
              </button>
            ))}
          </div>
          <div className="segmented" role="group" aria-label={t("cosmetics.style")}>
            {STYLES.map((s) => (
              <button key={s} aria-pressed={outfit.style === s} disabled={outfit.back === "none"} onClick={() => setOutfit((o) => ({ ...o, style: s }))}>
                {t(`cosmetics.style.${s}` as MessageKey)}
              </button>
            ))}
          </div>
        </div>
      </section>

      <div className="cosmetic-groups">
        {SLOTS.map((slot) => {
          const items = MODELS.filter((m) => m.slot === slot && wearable(m.id));
          const box = BOX_ITEMS.filter((i) => i.slot === slot);
          if (items.length === 0 && box.length === 0) return null;
          const worn = boxItem(outfit.equipped[slot] ?? "");
          return (
            <section key={slot}>
              <h2 className="section-title">{t(`cosmetics.slot.${slot}` as MessageKey)}</h2>
              <div className="cosmetic-grid">
                {items.map((m) => (
                  <Card key={m.id} m={m} on={outfit.equipped[slot] === m.id} onClick={() => pick(slot, m.id)} />
                ))}
                {box.map((i) => (
                  <BoxCard key={i.id} item={i} on={outfit.equipped[slot] === i.id} onClick={() => pick(slot, i.id)} />
                ))}
              </div>
              {worn && worn.pieces.some((p) => p.color === 0) && <Swatches value={colorOf(outfit, worn.id)} onPick={(c) => recolor(worn.id, c)} />}
            </section>
          );
        })}
      </div>
    </div>
  );
}
