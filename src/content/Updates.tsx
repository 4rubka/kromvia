import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { useJobs } from "../lib/jobs";
import { Progress } from "../ui/Progress";
import { Glyph } from "../ui/Glyph";
import { Empty } from "../ui/Empty";

type Update = { folder: string; file: string; title: string; newFile: string; version: string };
type Issue = { kind: "incompatible" | "missing"; title: string; other: string };

export function ContentUpdates({ instanceId, onChange }: { instanceId: string; onChange: () => void }) {
  const { t } = useI18n();
  const [updates, setUpdates] = useState<Update[] | null>(null);
  const [issues, setIssues] = useState<Issue[]>([]);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const check = () => {
    setChecking(true);
    setError(null);
    Promise.all([invoke<Update[]>("content_updates", { instanceId }), invoke<Issue[]>("content_audit", { instanceId }).catch(() => [])])
      .then(
        ([u, i]) => (setUpdates(u), setIssues(i)),
        (e) => setError(String(e)),
      )
      .finally(() => setChecking(false));
  };
  const jobs = useJobs("update-", (_, e) => {
    setError(e);
    onChange();
    check();
  });
  const job = jobs.jobs[`update-${instanceId}`];

  useEffect(() => {
    setUpdates(null);
    setIssues([]);
    setError(null);
  }, [instanceId]);

  const run = (files: string[] | null) =>
    invoke<number>("content_update", { instanceId, files }).then(
      (id) => jobs.started(`update-${instanceId}`, id),
      (e) => setError(String(e)),
    );

  return (
    <div className="content-updates">
      <div className="actions">
        {job ? (
          <div className="job">
            <Progress job={job} />
          </div>
        ) : (
          <>
            <button className="btn btn-ghost" disabled={checking} onClick={check}>
              <Glyph name="refresh" /> {checking ? t("updates.checking") : t("updates.check")}
            </button>
            {updates && updates.length > 0 && (
              <button className="btn" onClick={() => run(null)}>
                <Glyph name="download" /> {t("updates.all", { n: String(updates.length) })}
              </button>
            )}
          </>
        )}
      </div>
      {error && <p className="error">{error}</p>}
      {issues.map((i) => (
        <p key={i.kind + i.title + i.other} className="error">
          {t(i.kind === "missing" ? "audit.missing" : "audit.incompatible", { a: i.title, b: i.other })}
        </p>
      ))}
      {updates?.length === 0 && <Empty bee="success">{t("updates.none")}</Empty>}
      {updates && updates.length > 0 && (
        <ul className="rows compact">
          {updates.map((u) => (
            <li key={u.folder + u.file} className="row plain">
              <div className="list-main">
                <span>
                  {u.title} <span className="muted">· {u.version}</span>
                </span>
                <span className="path muted">
                  {u.file} → {u.newFile}
                </span>
              </div>
              <div className="row-tools">
                <button className="btn btn-ghost" disabled={!!job} onClick={() => run([`${u.folder}/${u.file}`])}>
                  <Glyph name="download" /> {t("updates.one")}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
