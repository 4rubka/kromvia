import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { LOADER_NAMES, type Instance } from "../lib/launcher";
import { VersionPicker, versionReady, type VersionChoice } from "./VersionPicker";
import { Glyph } from "../ui/Glyph";
import { Progress } from "../ui/Progress";
import { useJobs } from "../lib/jobs";

type Template = { id: string; name: string; instance: Instance; created: number };

export function NewInstance({ onDone, initialCode }: { onDone: (inst: Instance | null) => void; initialCode?: string | null }) {
  const { t } = useI18n();
  const [choice, setChoice] = useState<VersionChoice>({ mc: "", loader: null, build: "" });
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [templates, setTemplates] = useState<Template[]>([]);
  const [template, setTemplate] = useState("");
  const chosen = templates.find((x) => x.id === template);
  const [mode, setMode] = useState<"version" | "code">(initialCode ? "code" : "version");
  const [code, setCode] = useState(initialCode ?? "");
  const installs = useJobs("build-install", (_, e) => {
    if (e) return setError(e);
    invoke<Instance[]>("instance_list").then((list) => onDone([...list].sort((a, b) => b.created - a.created)[0] ?? null));
  });
  const installing = installs.jobs["build-install"];
  const fromCode = () => {
    setError(null);
    invoke<{ kind: string; data: unknown }>("share_get", { code: code.trim().split("/").filter(Boolean).pop() ?? "" })
      .then((s) => {
        if (s.kind !== "build") throw t("cloud.notBuild");
        return invoke<number>("build_install", { manifest: s.data });
      })
      .then((id) => installs.started("build-install", id), (e) => setError(String(e)));
  };

  const loadTemplates = () => invoke<Template[]>("template_list").then(setTemplates, () => {});
  useEffect(() => {
    loadTemplates();
  }, []);

  const removeTemplate = () =>
    invoke("template_delete", { id: template }).then(
      () => (setTemplate(""), loadTemplates()),
      (e) => setError(String(e)),
    );

  const { mc, loader, build } = choice;
  const defaultName = chosen ? chosen.name : loader ? `${LOADER_NAMES[loader]} ${mc}` : mc;

  const create = () => {
    setError(null);
    if (chosen) {
      invoke<Instance>("template_create", { templateId: chosen.id, name: name.trim() || defaultName }).then(onDone, (e) => setError(String(e)));
      return;
    }
    invoke<Instance>("instance_create", {
      name: name.trim() || defaultName,
      mcVersion: mc,
      loader: loader ? { kind: loader, version: build } : null,
    }).then(onDone, (e) => setError(String(e)));
  };

  return (
    <section className="card">
      <h2>{t("instance.new")}</h2>
      <div className="segmented">
        <button aria-pressed={mode === "version"} onClick={() => setMode("version")}>
          <Glyph name="plus" /> {t("instance.fromVersion")}
        </button>
        <button aria-pressed={mode === "code"} onClick={() => setMode("code")}>
          <Glyph name="users-group" /> {t("instance.fromCode")}
        </button>
      </div>
      {mode === "code" ? (
        <form
          className="field"
          onSubmit={(e) => {
            e.preventDefault();
            if (code.trim() && !installing) fromCode();
          }}
        >
          <span>{t("instance.codeHint")}</span>
          <input className="input" autoFocus value={code} placeholder={t("cloud.codePlaceholder")} aria-label={t("cloud.codePlaceholder")} onChange={(e) => setCode(e.currentTarget.value.trim())} />
          {installing && <Progress job={installing} />}
          {error && <p className="error">{error}</p>}
          <div className="actions">
            <button type="button" className="btn btn-ghost" onClick={() => onDone(null)}>
              <Glyph name="x" /> {t("common.cancel")}
            </button>
            <button className="btn" disabled={!code.trim() || !!installing}>
              <Glyph name="download" /> {t("cloud.install")}
            </button>
          </div>
        </form>
      ) : (
      <>
      <div className="field">
        <span>{t("instance.name")}</span>
        <input className="input" value={name} placeholder={defaultName} onChange={(e) => setName(e.currentTarget.value)} />
      </div>
      {templates.length > 0 && (
        <div className="field">
          <span>{t("instance.template")}</span>
          <div className="actions">
            <select className="input" value={template} onChange={(e) => setTemplate(e.currentTarget.value)}>
              <option value="">{t("instance.noTemplate")}</option>
              {templates.map((x) => (
                <option key={x.id} value={x.id}>
                  {x.name} · {x.instance.mcVersion}
                  {x.instance.loader ? ` · ${LOADER_NAMES[x.instance.loader.kind]}` : ""}
                </option>
              ))}
            </select>
            {chosen && (
              <button className="btn btn-ghost" onClick={removeTemplate}>
                <Glyph name="trash" /> {t("instance.deleteTemplate")}
              </button>
            )}
          </div>
        </div>
      )}
      {!chosen && <VersionPicker value={choice} onChange={setChoice} />}
      {error && <p className="error">{error}</p>}
      <div className="actions">
        <button className="btn btn-ghost" onClick={() => onDone(null)}>
          <Glyph name="x" /> {t("common.cancel")}
        </button>
        <button className="btn" disabled={!chosen && !versionReady(choice)} onClick={create}>
          <Glyph name="plus" /> {t("instance.create")}
        </button>
      </div>
      </>
      )}
    </section>
  );
}
