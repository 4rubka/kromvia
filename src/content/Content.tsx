import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { useJobs } from "../lib/jobs";
import { Progress } from "../ui/Progress";
import { describe, type Instance } from "../lib/launcher";
import { Glyph } from "../ui/Glyph";
import { Pager, usePaged } from "../ui/Pager";
import { Empty } from "../ui/Empty";
import { ProjectPage } from "./ProjectPage";

export type Kind = "mod" | "modpack" | "resourcepack" | "shader" | "datapack";
type Source = "modrinth" | "curseforge";
const SOURCES: [Source, string][] = [
  ["modrinth", "Modrinth"],
  ["curseforge", "CurseForge"],
];
type Tab = Kind | "installed";
type Hit = { project_id: string; slug: string; title: string; description: string; icon_url: string | null; downloads: number; author: string };
export type Installed = { folder: string; file: string; enabled: boolean; title: string; projectId: string | null };

const TABS: Tab[] = ["mod", "modpack", "resourcepack", "shader", "datapack", "installed"];
const TARGET_KEY = "kromvia.contentTarget";

export function aimContent(instanceId: string, query?: string) {
  try {
    localStorage.setItem(TARGET_KEY, instanceId);
    if (query) sessionStorage.setItem(QUERY_KEY, query);
  } catch {}
}

const QUERY_KEY = "kromvia.contentQuery";

function takeQuery() {
  try {
    const q = sessionStorage.getItem(QUERY_KEY) ?? "";
    sessionStorage.removeItem(QUERY_KEY);
    return q;
  } catch {
    return "";
  }
}

function savedTarget() {
  try {
    return localStorage.getItem(TARGET_KEY);
  } catch {
    return null;
  }
}

export function Content({ initialTab = "mod" }: { initialTab?: Tab }) {
  const { t } = useI18n();
  const [instances, setInstances] = useState<Instance[]>([]);
  const [target, setTarget] = useState<string | null>(savedTarget);
  const [tab, setTab] = useState<Tab>(initialTab);
  const [source, setSource] = useState<Source>("modrinth");
  const [query, setQuery] = useState(takeQuery);
  const [hits, setHits] = useState<Hit[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [done, setDone] = useState<Set<string>>(new Set());
  const [installed, setInstalled] = useState<Installed[]>([]);
  const [open, setOpen] = useState<Hit | null>(null);
  const [asking, setAsking] = useState<{ hit: Hit; versionId: string | null } | null>(null);
  const curseforgeAllowed = tab !== "datapack";
  const from: Source = curseforgeAllowed ? source : "modrinth";

  const inst = instances.find((i) => i.id === target) ?? instances[0];
  const needsLoader = (tab === "mod" || tab === "shader") && inst && !inst.loader;

  const setError = (key: string, error: string | null) =>
    setErrors(({ [key]: _, ...rest }) => (error ? { ...rest, [key]: error } : rest));
  const onFinished = (key: string, error: string | null) => {
    setError(key, error);
    if (!error) setDone((d) => new Set(d).add(key));
    if (inst) refreshInstalled(inst.id);
    invoke<Instance[]>("instance_list").then(setInstances);
  };
  const content = useJobs("content-", onFinished);
  const modpacks = useJobs("modpack-", onFinished);

  const refreshInstalled = (id: string) => invoke<Installed[]>("content_list", { instanceId: id }).then(setInstalled, () => setInstalled([]));

  useEffect(() => {
    invoke<Instance[]>("instance_list").then(setInstances);
  }, []);

  useEffect(() => {
    if (inst) refreshInstalled(inst.id);
  }, [inst?.id]);

  const search = (page: number) => {
    if (tab === "installed" || needsLoader) return;
    setLoading(true);
    setPage(page);
    invoke<{ hits: Hit[]; total_hits: number }>("content_search", {
      query,
      kind: tab,
      instanceId: tab === "modpack" ? null : inst?.id ?? null,
      offset: page * SEARCH_PAGE,
      source: from,
    }).then(
      (r) => {
        setHits(r.hits);
        setTotal(r.total_hits);
        setError("search", null);
      },
      (e) => setError("search", String(e)),
    ).finally(() => setLoading(false));
  };

  useEffect(() => {
    const timer = setTimeout(() => search(0), 300);
    return () => clearTimeout(timer);
  }, [query, tab, inst?.id, source]);

  useEffect(() => setOpen(null), [tab, source]);

  const pickTarget = (id: string) => {
    setTarget(id);
    try {
      localStorage.setItem(TARGET_KEY, id);
    } catch {}
  };

  const install = (hit: Hit, versionId: string | null = null, world: string | null = null, into = inst) => {
    const isPack = tab === "modpack";
    const key = isPack ? `modpack-${hit.project_id}` : `content-${into!.id}-${hit.project_id}`;
    setError(key, null);
    const call = isPack
      ? invoke<number>("modpack_install", { projectId: hit.project_id, source: from })
      : invoke<number>("content_install", { instanceId: into!.id, projectId: hit.project_id, kind: tab, source: from, versionId, world });
    call.then(
      (id) => (isPack ? modpacks : content).started(key, id),
      (e) => setError(key, String(e)),
    );
  };

  const askInstall = (hit: Hit, versionId: string | null = null, world: string | null = null) => {
    if (tab === "modpack" || tab === "datapack" || instances.length < 2) install(hit, versionId, world);
    else setAsking({ hit, versionId });
  };

  const installInto = (i: Instance) => {
    const a = asking!;
    setAsking(null);
    pickTarget(i.id);
    install(a.hit, i.id === inst?.id ? a.versionId : null, null, i);
  };

  const installedIds = new Set(installed.map((f) => f.projectId).filter(Boolean));

  return (
    <>
      <div className="page-head">
        <h1 className="page-title">{t("nav.content")}</h1>
        {instances.length > 0 && (
          <label className="nick">
            <span className="muted">{t("content.target")}</span>
            <select className="input" value={inst?.id} onChange={(e) => pickTarget(e.currentTarget.value)}>
              {instances.map((i) => (
                <option key={i.id} value={i.id}>
                  {i.name}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      <div className="segmented tabs">
        {TABS.map((k) => (
          <button key={k} aria-pressed={tab === k} onClick={() => setTab(k)}>
            {t(`content.tab.${k}` as MessageKey)}
          </button>
        ))}
      </div>

      {!inst && tab !== "modpack" ? (
        <p className="muted">{t("content.noInstance")}</p>
      ) : tab === "installed" ? (
        <InstalledList files={installed} instanceId={inst!.id} onChange={() => refreshInstalled(inst!.id)} />
      ) : needsLoader ? (
        <p className="muted">{t("content.needsLoader")}</p>
      ) : open ? (
        (() => {
          const key = tab === "modpack" ? `modpack-${open.project_id}` : `content-${inst?.id}-${open.project_id}`;
          return (
            <>
              {errors[key] && <p className="error">{errors[key]}</p>}
              <ProjectPage
                projectId={open.project_id}
                kind={tab}
                inst={inst}
                job={(tab === "modpack" ? modpacks : content).jobs[key]}
                installed={(tab !== "modpack" && tab !== "datapack" && installedIds.has(open.project_id)) || done.has(key)}
                onInstall={(versionId, world) => askInstall(open, versionId, world)}
                onBack={() => setOpen(null)}
              />
            </>
          );
        })()
      ) : (
        <>
          <div className="segmented">
            {SOURCES.filter(([s]) => curseforgeAllowed || s === "modrinth").map(([s, name]) => (
              <button key={s} aria-pressed={from === s} onClick={() => setSource(s)}>
                {name}
              </button>
            ))}
          </div>
          <input
            className="input search"
            value={query}
            placeholder={t(from === "curseforge" ? "content.searchCurseforge" : "content.search")}
            onChange={(e) => setQuery(e.currentTarget.value)}
          />
          {errors.search && <Empty bee="offline">{errors.search}</Empty>}
          {!loading && !errors.search && query.trim() && hits.length === 0 && <Empty bee="not-found">{t("content.noResults")}</Empty>}
          <ul className="hits">
            {hits.map((hit) => {
              const key = tab === "modpack" ? `modpack-${hit.project_id}` : `content-${inst?.id}-${hit.project_id}`;
              const job = (tab === "modpack" ? modpacks : content).jobs[key];
              const have = tab !== "modpack" && installedIds.has(hit.project_id);
              return (
                <li key={hit.project_id} className="row plain hit">
                  {hit.icon_url ? <img className="hit-icon" src={hit.icon_url} alt="" loading="lazy" /> : <div className="hit-icon" />}
                  <div className="list-main">
                    <span>
                      {from === "modrinth" ? (
                        <button className="link instance-name" onClick={() => setOpen(hit)}>
                          {hit.title}
                        </button>
                      ) : (
                        <span className="instance-name">{hit.title}</span>
                      )}{" "}
                      <span className="muted">· {hit.author}</span>
                    </span>
                    <span className="muted hit-desc">{hit.description}</span>
                    {errors[key] && <span className="error">{errors[key]}</span>}
                  </div>
                  <span className="muted downloads">{formatCount(hit.downloads)}</span>
                  {job ? (
                    <div className="job">
                      <Progress job={job} />
                    </div>
                  ) : have || done.has(key) ? (
                    <span className="muted">{tab === "modpack" ? t("content.packCreated") : t("content.installed")}</span>
                  ) : tab === "datapack" ? (
                    <button className="btn" onClick={() => setOpen(hit)}>
                      <Glyph name="download" /> {t("content.pickWorld")}
                    </button>
                  ) : (
                    <button className="btn" onClick={() => askInstall(hit)}>
                      <Glyph name="download" /> {t("content.install")}
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
          {loading && <p className="muted">{t("common.loading")}</p>}
          <Pager page={page} pages={Math.ceil(total / SEARCH_PAGE)} onPage={search} />
        </>
      )}

      {asking && (
        <div className="overlay" role="dialog" aria-modal="true" aria-label={t("content.target")} onClick={() => setAsking(null)}>
          <section className="card onboarding" onClick={(e) => e.stopPropagation()}>
            <h2>
              {t("content.target")} · {asking.hit.title}
            </h2>
            <ul className="rows compact">
              {instances.map((i) => (
                <li key={i.id}>
                  <button className="row plain pick-instance" aria-pressed={i.id === inst?.id} onClick={() => installInto(i)}>
                    <span className="instance-name">{i.name}</span>
                    <span className="muted">{describe(i, t("instance.vanilla"))}</span>
                  </button>
                </li>
              ))}
            </ul>
            <div className="actions">
              <button className="btn btn-ghost" onClick={() => setAsking(null)}>
                {t("common.cancel")}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

export function InstalledList({ files, instanceId, onChange }: { files: Installed[]; instanceId: string; onChange: () => void }) {
  const { t } = useI18n();
  const [error, setError] = useState<string | null>(null);
  const paged = usePaged(files, PAGE);
  if (files.length === 0) return <p className="muted">{t("content.nothingInstalled")}</p>;

  const act = (p: Promise<unknown>) => p.then(() => (setError(null), onChange()), (e) => setError(String(e)));
  return (
    <>
      {error && <p className="error">{error}</p>}
      <ul className="rows compact">
        {paged.items.map((f) => (
          <li key={f.folder + f.file} className="row plain">
            {f.folder === "mods" && (
              <input
                type="checkbox"
                checked={f.enabled}
                title={t("content.enabled")}
                onChange={(e) => act(invoke("content_set_enabled", { instanceId, file: f.file, enabled: e.currentTarget.checked }))}
              />
            )}
            <ContentIcon instanceId={instanceId} folder={f.folder} file={f.file} />
            <div className="list-main">
              <span className={f.enabled ? "" : "muted"}>{f.title}</span>
              <span className="path muted">
                {f.folder}/{f.file}
              </span>
            </div>
            <div className="row-tools">
              <button
                className="btn btn-ghost"
                title={t("instance.delete")}
                aria-label={t("instance.delete")}
                onClick={() => act(invoke("content_remove", { instanceId, folder: f.folder, file: f.file }))}
              >
                <Glyph name="trash" />
              </button>
            </div>
          </li>
        ))}
      </ul>
      {paged.pager}
    </>
  );
}

function ContentIcon({ instanceId, folder, file }: { instanceId: string; folder: string; file: string }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    let live = true;
    setSrc(null);
    invoke<string | null>("content_icon", { instanceId, folder, file }).then((s) => live && setSrc(s), () => {});
    return () => {
      live = false;
    };
  }, [instanceId, folder, file]);
  return src ? <img className="content-icon" src={src} alt="" /> : <span className="content-icon" />;
}

const PAGE = 20;
const SEARCH_PAGE = 20;

function formatCount(n: number) {
  if (n >= 1e6) return `${(n / 1e6).toFixed(1)}M`;
  if (n >= 1e3) return `${Math.round(n / 1e3)}K`;
  return String(n);
}
