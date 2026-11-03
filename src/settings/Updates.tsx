import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { useJobs } from "../lib/jobs";
import { Progress } from "../ui/Progress";

type Available = { version: string; notes: string | null };

let checked: Promise<Available | null> | null = null;
const check = () => (checked ??= invoke<Available | null>("update_check").catch(() => null));

export function useUpdate() {
  const [update, setUpdate] = useState<Available | null>(null);
  useEffect(() => {
    check().then(setUpdate);
  }, []);
  return update;
}

export function Updates() {
  const { t } = useI18n();
  const update = useUpdate();
  const [error, setError] = useState<string | null>(null);
  const updates = useJobs("update", (_, e) => setError(e));
  const job = updates.jobs.update;
  const install = () => {
    setError(null);
    invoke<number>("update_install").then(
      (id) => updates.started("update", id),
      (e) => setError(String(e)),
    );
  };
  if (!update) return null;
  return (
    <section className="card">
      <h2>{t("update.title", { version: update.version })}</h2>
      {error && <p className="error">{error}</p>}
      {update.notes && <p className="muted">{update.notes}</p>}
      <div className="actions">
        {job ? (
          <div className="job">
            <Progress job={job} />
          </div>
        ) : (
          <button className="btn" onClick={install}>
            {t("update.install")}
          </button>
        )}
      </div>
    </section>
  );
}
