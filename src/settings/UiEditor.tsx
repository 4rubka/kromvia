import { useEffect, useState } from "react";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { Glyph } from "../ui/Glyph";
import "./ui-editor.css";

export type Part = "button" | "secondary" | "panel";
const PARTS: Part[] = ["button", "secondary", "panel"];

export type PartStyle = {
  fill?: string;
  fill2?: string;
  angle?: number;
  text?: string;
  border?: string;
  borderWidth?: number;
  radius?: number;
  image?: string;
};

export type UiProfile = {
  id: string;
  name: string;
  parts: Partial<Record<Part, PartStyle>>;
  hidden: string[];
};

type Store = { active: string; profiles: UiProfile[] };

const KEY = "kromvia.uiProfiles";
const CHANGED = "kromvia-ui-profile";
const DEFAULT: UiProfile = { id: "default", name: "Default", parts: {}, hidden: [] };

const SELECTORS: Record<Part, string> = {
  button: ":root .btn:not(.btn-ghost):not(.btn-danger)",
  secondary: ":root .btn.btn-ghost",
  panel: ":root .card",
};

function load(): Store {
  try {
    const s = JSON.parse(localStorage.getItem(KEY) ?? "") as Store;
    if (Array.isArray(s.profiles) && s.profiles.some((p) => p.id === "default")) return s;
  } catch {}
  return { active: "default", profiles: [DEFAULT] };
}

function store(s: Store) {
  try {
    localStorage.setItem(KEY, JSON.stringify(s));
  } catch {}
  window.dispatchEvent(new Event(CHANGED));
}

function activeOf(s: Store) {
  return s.profiles.find((p) => p.id === s.active) ?? s.profiles[0];
}

function partCss(sel: string, st: PartStyle) {
  const rules: string[] = [];
  const layers: string[] = [];
  if (st.image) layers.push(`url("${st.image}") center / cover no-repeat`);
  if (st.fill && st.fill2) layers.push(`linear-gradient(${st.angle ?? 120}deg, ${st.fill}, ${st.fill2})`);
  else if (st.fill) layers.push(st.fill);
  if (layers.length) rules.push(`background: ${layers.join(", ")}`);
  if (st.text) rules.push(`color: ${st.text}`);
  if (st.borderWidth !== undefined) rules.push(`border: ${st.borderWidth}px solid ${st.border ?? "transparent"}`);
  if (st.radius !== undefined) rules.push(`border-radius: ${st.radius}px`);
  if (!rules.length) return "";
  const body = rules.join("; ");
  const hover = layers.length ? `${sel}:hover:not(:disabled) { background: ${layers.join(", ")}; filter: brightness(1.12); }` : "";
  return `${sel} { ${body}; }\n${hover}\n`;
}

export function profileCss(p: UiProfile) {
  return PARTS.map((part) => (p.parts[part] ? partCss(SELECTORS[part], p.parts[part]!) : "")).join("");
}

export function useUiProfile() {
  const [profile, setProfile] = useState(() => activeOf(load()));
  useEffect(() => {
    const apply = () => setProfile(activeOf(load()));
    window.addEventListener(CHANGED, apply);
    return () => window.removeEventListener(CHANGED, apply);
  }, []);
  useEffect(() => {
    const el = document.getElementById("ui-profile") ?? Object.assign(document.head.appendChild(document.createElement("style")), { id: "ui-profile" });
    el.textContent = profileCss(profile);
  }, [profile]);
  return profile;
}

async function shrink(file: Blob) {
  const img = await createImageBitmap(file);
  const k = Math.min(1, 600 / img.width, 600 / img.height);
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(img.width * k);
  canvas.height = Math.round(img.height * k);
  canvas.getContext("2d")!.drawImage(img, 0, 0, canvas.width, canvas.height);
  return canvas.toDataURL("image/png");
}

export function UiEditor({ pages }: { pages: string[] }) {
  const { t } = useI18n();
  const [s, setS] = useState(load);
  const [part, setPart] = useState<Part>("button");
  const [error, setError] = useState<string | null>(null);
  const profile = activeOf(s);
  const st = profile.parts[part] ?? {};
  const locked = profile.id === "default";

  const commit = (next: Store) => {
    setS(next);
    store(next);
  };
  const editProfile = (change: (p: UiProfile) => UiProfile) =>
    commit({ ...s, profiles: s.profiles.map((p) => (p.id === profile.id ? change(p) : p)) });
  const set = (patch: Partial<PartStyle>) =>
    editProfile((p) => ({ ...p, parts: { ...p.parts, [part]: { ...(p.parts[part] ?? {}), ...patch } } }));
  const create = (from: UiProfile) => {
    const id = `p${Date.now().toString(36)}`;
    const name = from.id === "default" ? t("ui.newProfile") : `${from.name} 2`;
    commit({ active: id, profiles: [...s.profiles, { ...structuredClone(from), id, name }] });
  };
  const remove = () => commit({ active: "default", profiles: s.profiles.filter((p) => p.id !== profile.id) });
  const toggleHidden = (page: string) =>
    editProfile((p) => ({ ...p, hidden: p.hidden.includes(page) ? p.hidden.filter((x) => x !== page) : [...p.hidden, page] }));

  return (
    <section className="card">
      <h2>{t("ui.title")}</h2>
      <p className="muted">{t("ui.hint")}</p>
      <div className="field">
        <span>{t("ui.profile")}</span>
        <div className="actions">
          <select className="input" value={profile.id} onChange={(e) => commit({ ...s, active: e.currentTarget.value })}>
            {s.profiles.map((p) => (
              <option key={p.id} value={p.id}>
                {p.id === "default" ? t("ui.default") : p.name}
              </option>
            ))}
          </select>
          <button className="btn btn-ghost" onClick={() => create(profile)}>
            <Glyph name="plus" /> {t(locked ? "ui.new" : "ui.duplicate")}
          </button>
          {!locked && (
            <button className="btn btn-ghost" onClick={remove}>
              <Glyph name="trash" /> {t("ui.delete")}
            </button>
          )}
        </div>
      </div>
      {locked ? (
        <p className="muted">{t("ui.defaultHint")}</p>
      ) : (
        <>
          <label className="field">
            <span>{t("ui.name")}</span>
            <input className="input" value={profile.name} maxLength={40} onChange={(e) => editProfile((p) => ({ ...p, name: e.currentTarget.value }))} />
          </label>
          <div className="segmented">
            {PARTS.map((p) => (
              <button key={p} aria-pressed={part === p} onClick={() => setPart(p)}>
                {t(`ui.part.${p}` as MessageKey)}
              </button>
            ))}
          </div>
          <div className="ui-sample">
            {part === "panel" ? (
              <div className="card">{t("ui.part.panel")}</div>
            ) : (
              <button className={part === "secondary" ? "btn btn-ghost" : "btn"}>{t(`ui.part.${part}` as MessageKey)}</button>
            )}
          </div>
          <div className="ui-grid">
            <label>
              <span>{t("ui.fill")}</span>
              <input type="color" value={st.fill ?? "#8b5cf6"} onChange={(e) => set({ fill: e.currentTarget.value })} />
            </label>
            <label>
              <span>{t("ui.gradient")}</span>
              <input type="checkbox" checked={!!st.fill2} onChange={(e) => set({ fill: st.fill ?? "#8b5cf6", fill2: e.currentTarget.checked ? "#1f63d6" : undefined })} />
              {st.fill2 && <input type="color" value={st.fill2} onChange={(e) => set({ fill2: e.currentTarget.value })} />}
            </label>
            {st.fill2 && (
              <label>
                <span>{t("ui.angle")}</span>
                <input type="range" min={0} max={360} step={15} value={st.angle ?? 120} onChange={(e) => set({ angle: Number(e.currentTarget.value) })} />
              </label>
            )}
            <label>
              <span>{t("ui.text")}</span>
              <input type="color" value={st.text ?? "#ffffff"} onChange={(e) => set({ text: e.currentTarget.value })} />
            </label>
            <label>
              <span>{t("ui.border")}</span>
              <input type="range" min={0} max={4} value={st.borderWidth ?? 0} onChange={(e) => set({ borderWidth: Number(e.currentTarget.value) })} />
              <input type="color" value={st.border ?? "#2a253d"} onChange={(e) => set({ border: e.currentTarget.value, borderWidth: st.borderWidth ?? 1 })} />
            </label>
            <label>
              <span>{t("ui.corners")}</span>
              <input type="range" min={0} max={24} value={st.radius ?? (part === "panel" ? 6 : 5)} onChange={(e) => set({ radius: Number(e.currentTarget.value) })} />
            </label>
          </div>
          <div className="actions">
            <label className="btn btn-ghost">
              <Glyph name="upload" /> {t("ui.image")}
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                hidden
                onChange={(e) => {
                  const file = e.currentTarget.files?.[0];
                  e.currentTarget.value = "";
                  if (file) shrink(file).then((image) => set({ image }), (err) => setError(String(err)));
                }}
              />
            </label>
            {st.image && (
              <button className="btn btn-ghost" onClick={() => set({ image: undefined })}>
                <Glyph name="x" /> {t("ui.imageRemove")}
              </button>
            )}
            <button className="btn btn-ghost" onClick={() => editProfile((p) => ({ ...p, parts: { ...p.parts, [part]: undefined } }))}>
              <Glyph name="rotate" /> {t("ui.reset")}
            </button>
          </div>
          <div className="field">
            <span>{t("ui.hide")}</span>
            <div className="ui-hide">
              {pages.map((p) => (
                <label key={p} className="muted">
                  <input type="checkbox" checked={profile.hidden.includes(p)} onChange={() => toggleHidden(p)} /> {t(`nav.${p}` as MessageKey)}
                </label>
              ))}
            </div>
          </div>
        </>
      )}
      {error && <p className="error">{error}</p>}
    </section>
  );
}
