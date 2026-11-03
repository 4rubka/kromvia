import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { useJobs } from "../lib/jobs";
import { Progress } from "../ui/Progress";
import { Glyph } from "../ui/Glyph";

type JavaRuntime = { path: string; major: number; version: string; is64bit: boolean; managed: boolean };

export function JavaSettings() {
  const { t } = useI18n();
  const [runtimes, setRuntimes] = useState<JavaRuntime[] | null>(null);
  const [supported, setSupported] = useState<number[]>([]);
  const [errors, setErrors] = useState<Record<number, string>>({});
  const [customPath, setCustomPath] = useState("");
  const [customResult, setCustomResult] = useState<string | null>(null);

  const refresh = () => invoke<JavaRuntime[]>("java_list").then(setRuntimes);
  const setError = (major: number, error: string | null) =>
    setErrors(({ [major]: _, ...rest }) => (error ? { ...rest, [major]: error } : rest));

  const { jobs, started } = useJobs("java-", (key, error) => {
    setError(Number(key.slice("java-".length)), error);
    refresh();
  });

  useEffect(() => {
    refresh();
    invoke<number[]>("java_supported").then(setSupported);
  }, []);

  const install = (major: number) => {
    setError(major, null);
    invoke<number>("java_install", { major }).then(
      (id) => started(`java-${major}`, id),
      (e) => setError(major, String(e)),
    );
  };

  const remove = (major: number) => {
    invoke("java_remove", { major }).then(refresh, (e) => setError(major, String(e)));
  };

  const check = () => {
    setCustomResult(null);
    invoke<JavaRuntime>("java_check", { path: customPath.trim() }).then(
      (rt) => setCustomResult(t("java.check.ok", { version: rt.version, major: rt.major })),
      (e) => setCustomResult(t("java.check.fail", { error: String(e) })),
    );
  };

  const managed = new Set(runtimes?.filter((r) => r.managed).map((r) => r.major));

  return (
    <section className="card">
      <h2>{t("java.title")}</h2>

      {runtimes === null ? (
        <p className="muted">{t("java.scanning")}</p>
      ) : runtimes.length === 0 ? (
        <p className="muted">{t("java.none")}</p>
      ) : (
        <ul className="list">
          {runtimes.map((rt) => (
            <li key={rt.path} className="list-row">
              <div className="list-main">
                <span>
                  Java {rt.major} <span className="muted">· {rt.version}</span>
                  <span className="badge">{rt.managed ? t("java.managed") : t("java.system")}</span>
                  {!rt.is64bit && <span className="badge badge-warn">32-bit</span>}
                </span>
                <span className="path muted" title={rt.path}>
                  {rt.path}
                </span>
              </div>
              {rt.managed && !jobs[`java-${rt.major}`] && (
                <button className="btn btn-ghost" onClick={() => remove(rt.major)}>
                  <Glyph name="trash" /> {t("java.remove")}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      <h3>{t("java.install")}</h3>
      {supported.map((major) => {
        const job = jobs[`java-${major}`];
        return (
          <div key={major} className="field">
            <span>Java {major}</span>
            {job ? (
              <div className="job">
                <Progress job={job} />
                <button className="btn btn-ghost" onClick={() => invoke("job_cancel", { id: job.id })}>
                  <Glyph name="x" /> {t("common.cancel")}
                </button>
              </div>
            ) : (
              <button className="btn" onClick={() => install(major)}>
                <Glyph name={managed.has(major) ? "refresh" : "download"} /> {managed.has(major) ? t("java.reinstall") : t("java.installOne")}
              </button>
            )}
            {errors[major] && <span className="error">{errors[major]}</span>}
          </div>
        );
      })}

      <h3>{t("java.custom")}</h3>
      <div className="field">
        <input
          className="input"
          value={customPath}
          placeholder={t("java.custom.placeholder")}
          onChange={(e) => setCustomPath(e.currentTarget.value)}
        />
        <button className="btn" disabled={!customPath.trim()} onClick={check}>
          <Glyph name="search" /> {t("java.check")}
        </button>
      </div>
      {customResult && <p className="muted">{customResult}</p>}
    </section>
  );
}
