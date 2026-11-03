import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import type { JobProgress } from "../lib/jobs";

const MB = 1024 * 1024;

export function Progress({ job }: { job: JobProgress }) {
  const { t } = useI18n();
  const pct = job.total ? Math.floor((job.done / job.total) * 100) : null;
  return (
    <div className="progress-wrap">
      <span className="muted">
        {t(`stage.${job.stage}` as MessageKey)}
        {pct !== null && ` ${pct}%`}
        {job.total ? ` · ${Math.round(job.done / MB)}/${Math.round(job.total / MB)} MB` : ""}
      </span>
      <div className="progress">
        <div className="progress-bar" style={{ width: pct !== null ? `${pct}%` : "100%" }} data-indeterminate={pct === null} />
      </div>
    </div>
  );
}
