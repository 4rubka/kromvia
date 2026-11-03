import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { open, save } from "@tauri-apps/plugin-dialog";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { useJobs } from "../lib/jobs";
import { Progress } from "../ui/Progress";
import { LOADER_NAMES, useLauncher, type Instance } from "../lib/launcher";
import { VersionPicker, versionReady, type VersionChoice } from "../home/VersionPicker";
import { Glyph } from "../ui/Glyph";
import { usePaged } from "../ui/Pager";
import { Empty } from "../ui/Empty";
import { BlockIcon, ICON_BLOCKS, InstanceIcon } from "../art/ModelIcon";
import { MODELS } from "../art/catalog";
import { modelShot } from "../art/gltf";
import { aimContent, InstalledList, type Installed, type Kind } from "../content/Content";
import { PackOrder, SafetyCheck } from "../content/Tools";
import { ContentUpdates } from "../content/Updates";
import { Backups, Cloud, Files, SavedServers, Screenshots, Worlds } from "./InstanceExtras";
import { Conflicts } from "./Conflicts";
import { Metrics } from "./Metrics";

type Preset = "auto" | "maxFps" | "balanced" | "quality";
const PRESETS: Preset[] = ["auto", "maxFps", "balanced", "quality"];

type Tab = "content" | "conflicts" | "worlds" | "servers" | "screenshots" | "settings" | "performance" | "files" | "backups" | "cloud" | "icon";
const TABS: Tab[] = ["content", "conflicts", "worlds", "servers", "screenshots", "settings", "performance", "files", "backups", "cloud", "icon"];
const TAB_LABELS: Record<Tab, MessageKey> = {
  content: "builds.content",
  conflicts: "conflicts.title",
  worlds: "builds.worlds",
  servers: "builds.servers",
  screenshots: "builds.screenshots",
  settings: "builds.settings",
  performance: "builds.performance",
  files: "builds.files",
  backups: "backups.title",
  cloud: "cloud.title",
  icon: "builds.icon",
};

export function Builds({ onAddContent }: { onAddContent: (kind: Kind) => void }) {
  const { t } = useI18n();
  const [instances, setInstances] = useState<Instance[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>("content");

  const launcher = useLauncher();
  const refresh = () => {
    launcher.refresh();
    return invoke<Instance[]>("instance_list").then(setInstances);
  };
  useEffect(() => {
    refresh();
  }, []);

  const imports = useJobs("modpack-file-", (_, e) => {
    setError(e);
    if (!e) refresh();
  });
  const exports = useJobs("export-", (_, e) => {
    setError(e);
    if (!e) setNotice(t("builds.exported"));
  });

  const inst = instances.find((i) => i.id === selected) ?? instances[0];
  const groups = [...new Set(instances.map((i) => i.group).filter((g): g is string => !!g))].sort((a, b) => a.localeCompare(b));
  const ordered = [...instances].sort((a, b) => (a.group ?? "").localeCompare(b.group ?? ""));
  const list = usePaged(ordered, 10);
  const [cover, setCover] = useState<string | null>(null);
  const [coverRev, setCoverRev] = useState(0);
  useEffect(() => {
    setCover(null);
    if (inst) invoke<string | null>("instance_cover", { id: inst.id }).then(setCover, () => {});
  }, [inst?.id, coverRev]);

  const importPack = async () => {
    setError(null);
    const path = await open({ filters: [{ name: "Modpack", extensions: ["mrpack", "zip"] }] });
    if (!path) return;
    invoke<number>("modpack_import", { path }).then(
      (id) => imports.started(`modpack-file-${path}`, id),
      (e) => setError(String(e)),
    );
  };

  const exportPack = async (i: Instance) => {
    setError(null);
    setNotice(null);
    const path = await save({ defaultPath: `${i.name}.mrpack`, filters: [{ name: "Modrinth modpack", extensions: ["mrpack"] }] });
    if (!path) return;
    invoke<number>("modpack_export", { instanceId: i.id, path }).then(
      (id) => exports.started(`export-${i.id}`, id),
      (e) => setError(String(e)),
    );
  };

  const shortcut = (i: Instance) => {
    setError(null);
    setNotice(null);
    invoke<string>("shortcut_create", { instanceId: i.id }).then(
      (path) => setNotice(t("builds.shortcutMade", { path })),
      (e) => setError(String(e)),
    );
  };

  const saveTemplate = (i: Instance) => {
    setError(null);
    setNotice(null);
    invoke("template_save", { instanceId: i.id, name: i.name }).then(
      () => setNotice(t("builds.templateSaved", { name: i.name })),
      (e) => setError(String(e)),
    );
  };

  const duplicate = (i: Instance) =>
    invoke<Instance>("instance_duplicate", { id: i.id }).then(
      (copy) => (refresh(), setSelected(copy.id)),
      (e) => setError(String(e)),
    );

  const remove = (i: Instance) => {
    if (confirmDelete !== i.id) return setConfirmDelete(i.id);
    setConfirmDelete(null);
    invoke("instance_delete", { id: i.id }).then(
      () => (refresh(), setSelected(null)),
      (e) => setError(String(e)),
    );
  };

  const presets = useJobs("preset-", (_, e) => {
    setError(e);
    if (!e) setNotice(t("builds.presetApplied"));
  });
  const applyPreset = (i: Instance, preset: Preset) => {
    setError(null);
    setNotice(null);
    invoke<number>("preset_apply", { instanceId: i.id, preset }).then(
      (id) => presets.started(`preset-${i.id}`, id),
      (e) => setError(String(e)),
    );
  };

  const importing = Object.values(imports.jobs)[0];
  const presetJob = inst && presets.jobs[`preset-${inst.id}`];
  const exporting = inst && exports.jobs[`export-${inst.id}`];

  return (
    <>
      <div className="page-head">
        <h1 className="page-title">{t("nav.builds")}</h1>
        {importing ? (
          <div className="job">
            <Progress job={importing} />
          </div>
        ) : (
          <button className="btn btn-ghost" onClick={importPack}>
            <Glyph name="file-import" /> {t("builds.import")}
          </button>
        )}
      </div>
      {error && <p className="error">{error}</p>}
      {notice && <p className="muted">{notice}</p>}
      {instances.length === 0 ? (
        <Empty bee="empty-builds">{t("builds.empty")}</Empty>
      ) : (
        <div className="builds">
          <div>
            <ul className="list instances">
              {list.items.map((i, n) => [
                (n === 0 || list.items[n - 1].group !== i.group) && (i.group || n > 0) && (
                  <li key={`group-${i.group ?? ""}`} className="instance-group muted">
                    {i.group ?? t("builds.ungrouped")}
                  </li>
                ),
                <li key={i.id} className="list-row instance" data-selected={i.id === inst?.id} onClick={() => setSelected(i.id)}>
                  <InstanceIcon inst={i} size={32} />
                  <div className="list-main">
                    <span className="instance-name">{i.name}</span>
                    <span className="muted">
                      {i.mcVersion} · {i.loader ? LOADER_NAMES[i.loader.kind] : t("instance.vanilla")}
                    </span>
                  </div>
                </li>,
              ])}
            </ul>
            {list.pager}
          </div>
          {inst && (
            <div className="build">
              <header className="card build-head" data-cover={cover ? "" : undefined} style={cover ? { backgroundImage: `linear-gradient(90deg, var(--surface) 30%, transparent), url(${cover})` } : undefined}>
                <InstanceIcon inst={inst} size={56} />
                <div className="list-main">
                  <h2>{inst.name}</h2>
                  <span className="muted">
                    {inst.mcVersion} · {inst.loader ? `${LOADER_NAMES[inst.loader.kind]} ${inst.loader.version}` : t("instance.vanilla")}
                  </span>
                </div>
                <div className="actions">
                  <button className="btn btn-ghost" onClick={() => shortcut(inst)}>
                    <Glyph name="external-link" /> {t("builds.shortcut")}
                  </button>
                  <button className="btn btn-ghost" onClick={() => duplicate(inst)}>
                    <Glyph name="copy" /> {t("builds.duplicate")}
                  </button>
                  <button className="btn btn-ghost" onClick={() => saveTemplate(inst)}>
                    <Glyph name="device-floppy" /> {t("builds.saveTemplate")}
                  </button>
                  {exporting ? (
                    <div className="job">
                      <Progress job={exporting} />
                    </div>
                  ) : (
                    <button className="btn btn-ghost" onClick={() => exportPack(inst)}>
                      <Glyph name="file-export" /> {t("builds.export")}
                    </button>
                  )}
                  <button className="btn btn-danger" onClick={() => remove(inst)} onBlur={() => setConfirmDelete(null)}>
                    <Glyph name="trash" /> {confirmDelete === inst.id ? t("instance.deleteConfirm") : t("instance.delete")}
                  </button>
                </div>
              </header>
              <div className="segmented tabs">
                {TABS.map((k) => (
                  <button key={k} aria-pressed={tab === k} onClick={() => setTab(k)}>
                    {t(TAB_LABELS[k])}
                  </button>
                ))}
              </div>
              {tab === "content" && (
                <InstanceContent
                  inst={inst}
                  onAdd={(kind) => {
                    aimContent(inst.id);
                    onAddContent(kind);
                  }}
                />
              )}
              {tab === "conflicts" && (
                <Conflicts
                  instanceId={inst.id}
                  onFind={(query) => {
                    aimContent(inst.id, query);
                    onAddContent("mod");
                  }}
                />
              )}
              {tab === "worlds" && <Worlds inst={inst} />}
              {tab === "servers" && <SavedServers inst={inst} />}
              {tab === "screenshots" && <Screenshots inst={inst} />}
              {tab === "files" && <Files inst={inst} />}
              {tab === "backups" && <Backups inst={inst} onRestored={refresh} />}
              {tab === "cloud" && <Cloud inst={inst} onInstalled={refresh} />}
              {tab === "settings" && <InstanceSettings key={inst.id} inst={inst} groups={groups} onSaved={refresh} />}
              {tab === "performance" && (
                <>
                  <section className="card">
                    <p className="muted">{t(inst.loader ? "builds.presetHint" : "builds.presetVanilla")}</p>
                    {presetJob ? (
                      <div className="job">
                        <Progress job={presetJob} />
                      </div>
                    ) : (
                      <div className="segmented">
                        {PRESETS.map((p) => (
                          <button key={p} onClick={() => applyPreset(inst, p)}>
                            {t(`preset.${p}` as MessageKey)}
                          </button>
                        ))}
                      </div>
                    )}
                  </section>
                  <Metrics instanceId={inst.id} />
                </>
              )}
              {tab === "icon" && (
                <>
                  <IconPicker inst={inst} onChange={refresh} onError={setError} />
                  <CoverPicker inst={inst} hasCover={!!cover} onChange={() => setCoverRev((r) => r + 1)} onError={setError} />
                </>
              )}
            </div>
          )}
        </div>
      )}
    </>
  );
}

function InstanceSettings({ inst, groups, onSaved }: { inst: Instance; groups: string[]; onSaved: () => void }) {
  const { t } = useI18n();
  const [name, setName] = useState(inst.name);
  const [choice, setChoice] = useState<VersionChoice>({
    mc: inst.mcVersion,
    loader: inst.loader?.kind ?? null,
    build: inst.loader?.version ?? "",
  });
  const [memory, setMemory] = useState(inst.memoryMb ? String(inst.memoryMb) : "");
  const [java, setJava] = useState(inst.javaPath ?? "");
  const [jvmArgs, setJvmArgs] = useState(inst.jvmArgs ?? "");
  const [gpu, setGpu] = useState(inst.gpu);
  const [group, setGroup] = useState(inst.group ?? "");
  const [width, setWidth] = useState(inst.resolution ? String(inst.resolution.width) : "");
  const [height, setHeight] = useState(inst.resolution ? String(inst.resolution.height) : "");
  const [status, setStatus] = useState<{ error: boolean; text: string } | null>(null);

  const submit = () => {
    setStatus(null);
    const num = (s: string) => (s.trim() ? Number(s) : null);
    const [w, h] = [num(width), num(height)];
    invoke<Instance>("instance_update", {
      id: inst.id,
      settings: {
        name,
        mcVersion: choice.mc,
        loader: choice.loader ? { kind: choice.loader, version: choice.build } : null,
        memoryMb: num(memory),
        javaPath: java,
        jvmArgs,
        gpu,
        group,
        resolution: w && h ? { width: w, height: h } : null,
      },
    }).then(
      () => (setStatus({ error: false, text: t("builds.saved") }), onSaved()),
      (e) => setStatus({ error: true, text: String(e) }),
    );
  };

  return (
    <section className="card">
      <div className="field">
        <span>{t("instance.name")}</span>
        <input className="input" value={name} onChange={(e) => setName(e.currentTarget.value)} />
      </div>
      <div className="field">
        <span>{t("builds.group")}</span>
        <input className="input" list="instance-groups" value={group} maxLength={32} placeholder={t("builds.ungrouped")} onChange={(e) => setGroup(e.currentTarget.value)} />
        <datalist id="instance-groups">
          {groups.map((g) => (
            <option key={g} value={g} />
          ))}
        </datalist>
      </div>
      <VersionPicker value={choice} onChange={setChoice} />
      <h3>{t("builds.launch")}</h3>
      <div className="field">
        <span>{t("builds.memory")}</span>
        <input className="input" inputMode="numeric" value={memory} placeholder={t("builds.auto")} onChange={(e) => setMemory(e.currentTarget.value)} />
      </div>
      <div className="field">
        <span>{t("java.title")}</span>
        <input className="input" value={java} placeholder={t("builds.auto")} onChange={(e) => setJava(e.currentTarget.value)} />
      </div>
      <div className="field">
        <span>{t("builds.jvmArgs")}</span>
        <input className="input" value={jvmArgs} placeholder="-Dfoo=bar" onChange={(e) => setJvmArgs(e.currentTarget.value)} />
      </div>
      <RendererChoice inst={inst} />
      {!navigator.userAgent.includes("Mac") && (
        <div className="field">
          <span>{t("builds.gpu")}</span>
          <div className="segmented wrap">
            {([null, "discrete", "integrated"] as const).map((g) => (
              <button key={g ?? "auto"} aria-pressed={gpu === g} onClick={() => setGpu(g)}>
                {t(g === "discrete" ? "builds.gpuDiscrete" : g === "integrated" ? "builds.gpuIntegrated" : "builds.gpuSystem")}
              </button>
            ))}
          </div>
        </div>
      )}
      <div className="field">
        <span>{t("builds.window")}</span>
        <div className="resolution">
          <input className="input" inputMode="numeric" value={width} placeholder="854" onChange={(e) => setWidth(e.currentTarget.value)} />
          <span className="muted">×</span>
          <input className="input" inputMode="numeric" value={height} placeholder="480" onChange={(e) => setHeight(e.currentTarget.value)} />
        </div>
      </div>
      {status && <p className={status.error ? "error" : "muted"}>{status.text}</p>}
      <div className="actions">
        <button className="btn" disabled={!versionReady(choice)} onClick={submit}>
          <Glyph name="device-floppy" /> {t("builds.save")}
        </button>
      </div>
    </section>
  );
}

async function toIcon(file: Blob) {
  const img = await createImageBitmap(file);
  const canvas = document.createElement("canvas");
  canvas.width = canvas.height = 64;
  const g = canvas.getContext("2d")!;
  const k = Math.min(64 / img.width, 64 / img.height);
  g.imageSmoothingEnabled = k < 1;
  const [w, h] = [img.width * k, img.height * k];
  g.drawImage(img, (64 - w) / 2, (64 - h) / 2, w, h);
  return canvas.toDataURL("image/png");
}

function RendererChoice({ inst }: { inst: Instance }) {
  const { t } = useI18n();
  const [vulkan, setVulkan] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = () => invoke<boolean>("renderer_get", { instanceId: inst.id }).then(setVulkan, () => setVulkan(false));
  const jobs = useJobs("renderer-", (_, e) => (setError(e), load()));
  useEffect(() => {
    load();
  }, [inst.id]);
  if (inst.loader?.kind !== "fabric" && inst.loader?.kind !== "quilt") return null;
  const job = jobs.jobs[`renderer-${inst.id}`];
  const pick = (on: boolean) => {
    setError(null);
    invoke<number>("renderer_set", { instanceId: inst.id, vulkan: on }).then(
      (id) => jobs.started(`renderer-${inst.id}`, id),
      (e) => setError(String(e)),
    );
  };
  return (
    <div className="field">
      <span>{t("builds.renderer")}</span>
      {job ? (
        <div className="job">
          <Progress job={job} />
        </div>
      ) : (
        <div className="segmented wrap">
          <button aria-pressed={vulkan === false} disabled={vulkan === null} onClick={() => vulkan && pick(false)}>
            OpenGL
          </button>
          <button aria-pressed={vulkan === true} disabled={vulkan === null} onClick={() => !vulkan && pick(true)}>
            {t("builds.rendererVulkan")}
          </button>
        </div>
      )}
      <span className="muted">{t("builds.rendererHint")}</span>
      {error && <span className="error">{error}</span>}
    </div>
  );
}

async function toCover(file: Blob) {
  const img = await createImageBitmap(file);
  const k = Math.min(1, 1280 / img.width, 480 / img.height);
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(img.width * k);
  canvas.height = Math.round(img.height * k);
  canvas.getContext("2d")!.drawImage(img, 0, 0, canvas.width, canvas.height);
  return canvas.toDataURL("image/jpeg", 0.85);
}

function CoverPicker({ inst, hasCover, onChange, onError }: { inst: Instance; hasCover: boolean; onChange: () => void; onError: (e: string) => void }) {
  const { t } = useI18n();
  const set = (cover: string | null) => invoke("instance_set_cover", { id: inst.id, cover }).then(onChange, (e) => onError(String(e)));
  return (
    <section className="card">
      <h3>{t("builds.cover")}</h3>
      <div className="actions">
        <label className="btn btn-ghost">
          <Glyph name="upload" /> {t("builds.coverUpload")}
          <input
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            hidden
            onChange={(e) => {
              const file = e.currentTarget.files?.[0];
              e.currentTarget.value = "";
              if (file) toCover(file).then(set, (err) => onError(String(err)));
            }}
          />
        </label>
        {hasCover && (
          <button className="btn btn-ghost" onClick={() => set(null)}>
            <Glyph name="trash" /> {t("builds.coverRemove")}
          </button>
        )}
      </div>
    </section>
  );
}

function IconPicker({ inst, onChange, onError }: { inst: Instance; onChange: () => void; onError: (e: string) => void }) {
  const { t } = useI18n();
  const set = (icon: string | null) => invoke("instance_set_icon", { id: inst.id, icon }).then(onChange, (e) => onError(String(e)));
  return (
    <section className="card icon-picker">
      <InstanceIcon inst={inst} size={56} />
      <div className="icon-choices">
        {ICON_BLOCKS.map((b) => (
          <button key={b} className="icon-choice" aria-pressed={inst.icon === `block:${b}`} aria-label={b} onClick={() => set(`block:${b}`)}>
            <BlockIcon kind={b} size={32} />
          </button>
        ))}
        {MODELS.filter((m) => ICON_MODELS.includes(m.id)).map((m) => (
          <ModelChoice key={m.id} id={m.id} name={m.name} onPick={(shot) => fetch(shot).then((r) => r.blob()).then(toIcon).then(set, (err) => onError(String(err)))} />
        ))}
        <label className="btn btn-ghost">
          <Glyph name="upload" /> {t("builds.iconUpload")}
          <input
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            hidden
            onChange={(e) => {
              const file = e.currentTarget.files?.[0];
              e.currentTarget.value = "";
              if (file) toIcon(file).then(set, (err) => onError(String(err)));
            }}
          />
        </label>
        {inst.icon && (
          <button className="btn btn-ghost" onClick={() => set(null)}>
            <Glyph name="rotate" /> {t("builds.iconReset")}
          </button>
        )}
      </div>
    </section>
  );
}

const ICON_MODELS = ["bee", "fox", "allay", "warden", "wands", "diamond-sword", "wrench", "book", "boots-of-swiftness", "earth-cube"];

function ModelChoice({ id, name, onPick }: { id: string; name: string; onPick: (shot: string) => void }) {
  const [shot, setShot] = useState<string | null>(null);
  useEffect(() => {
    modelShot(id).then(setShot, () => {});
  }, [id]);
  if (!shot) return null;
  return (
    <button className="icon-choice" title={name} aria-label={name} onClick={() => onPick(shot)}>
      <img src={shot} width={32} height={32} alt="" />
    </button>
  );
}

const FOLDERS: { folder: string; kind: Kind }[] = [
  { folder: "mods", kind: "mod" },
  { folder: "resourcepacks", kind: "resourcepack" },
  { folder: "shaderpacks", kind: "shader" },
];

function InstanceContent({ inst, onAdd }: { inst: Instance; onAdd: (kind: Kind) => void }) {
  const { t } = useI18n();
  const [files, setFiles] = useState<Installed[]>([]);
  const [tab, setTab] = useState(FOLDERS[0]);
  const [query, setQuery] = useState("");
  const [packsRev, setPacksRev] = useState(0);
  const refresh = () => {
    setPacksRev((r) => r + 1);
    return invoke<Installed[]>("content_list", { instanceId: inst.id }).then(setFiles, () => setFiles([]));
  };
  const q = query.trim().toLowerCase();
  const shown = files.filter((x) => x.folder === tab.folder && (!q || x.title.toLowerCase().includes(q) || x.file.toLowerCase().includes(q)));
  useEffect(() => {
    refresh().then(() => invoke<number>("content_identify", { instanceId: inst.id })).then((n) => {
      if (n) refresh();
    }, () => {});
  }, [inst.id]);

  return (
    <section className="card">
      <div className="card-head">
        <div className="segmented">
          {FOLDERS.map((f) => (
            <button key={f.folder} aria-pressed={tab.folder === f.folder} onClick={() => setTab(f)}>
              {t(`content.tab.${f.kind}` as MessageKey)} · {files.filter((x) => x.folder === f.folder).length}
            </button>
          ))}
        </div>
        <button className="btn" onClick={() => onAdd(tab.kind)}>
          <Glyph name="plus" /> {t("builds.add")}
        </button>
      </div>
      <input
        className="input content-filter"
        placeholder={t("builds.filter")}
        aria-label={t("builds.filter")}
        value={query}
        onChange={(e) => setQuery(e.currentTarget.value)}
      />
      <ContentUpdates instanceId={inst.id} onChange={refresh} />
      <InstalledList files={shown} instanceId={inst.id} onChange={refresh} />
      {tab.folder === "mods" && <SafetyCheck instanceId={inst.id} />}
      {tab.folder === "resourcepacks" && <PackOrder instanceId={inst.id} version={packsRev} />}
    </section>
  );
}
