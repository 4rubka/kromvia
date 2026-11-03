import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { Glyph } from "../ui/Glyph";
import { Empty } from "../ui/Empty";

type Kind = "missing" | "depVersion" | "duplicate" | "breaks" | "minecraft" | "loader";
type Conflict = {
  severity: "error" | "warning";
  kind: Kind;
  id: string;
  name: string | null;
  files: string[];
  wanted: string | null;
  found: string | null;
};

const GROUPS: [MessageKey, Kind[]][] = [
  ["conflicts.groupDeps", ["missing", "depVersion"]],
  ["conflicts.groupDuplicates", ["duplicate"]],
  ["conflicts.groupIncompatible", ["breaks"]],
  ["conflicts.groupVersion", ["minecraft"]],
  ["conflicts.groupLoader", ["loader"]],
];

export function Conflicts({ instanceId, onFind }: { instanceId: string; onFind: (query: string) => void }) {
  const { t } = useI18n();
  const [list, setList] = useState<Conflict[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const scan = () => {
    setBusy(true);
    setError(null);
    invoke<Conflict[]>("content_conflicts", { instanceId }).then(setList, (e) => setError(String(e))).finally(() => setBusy(false));
  };
  useEffect(() => {
    setList(null);
    scan();
  }, [instanceId]);

  const disable = (file: string) => invoke("content_set_enabled", { instanceId, file, enabled: false }).then(scan, (e) => setError(String(e)));

  const errors = list?.filter((c) => c.severity === "error").length ?? 0;
  const warnings = (list?.length ?? 0) - errors;
  return (
    <section className="card conflicts">
      <header className="conflicts-head">
        <h2>
          {t("conflicts.title")} <span className="badge">{t("conflicts.beta")}</span>
        </h2>
        <button className="btn btn-ghost" disabled={busy} onClick={scan}>
          <Glyph name="rotate" /> {busy ? t("conflicts.scanning") : t("conflicts.rescan")}
        </button>
      </header>
      <p className="muted">{t("conflicts.hint")}</p>
      {error && <p className="error">{error}</p>}
      {list && list.length > 0 && (
        <div className="conflicts-counts">
          <span className="conflict-chip error-chip">{t("conflicts.errors", { n: errors })}</span>
          <span className="conflict-chip warn-chip">{t("conflicts.warnings", { n: warnings })}</span>
        </div>
      )}
      {list && list.length === 0 && <Empty bee="success">{t("conflicts.none")}</Empty>}
      {list &&
        GROUPS.map(([label, kinds]) => {
          const items = list.filter((c) => kinds.includes(c.kind));
          if (items.length === 0) return null;
          return (
            <div key={label} className="conflict-group">
              <h3>
                {t(label)} <span className="muted">· {items.length}</span>
              </h3>
              {items.map((c, i) => (
                <div key={i} className={`conflict ${c.severity}`}>
                  <div className="conflict-top">
                    <strong>{t(`conflicts.${c.kind}` as MessageKey, { id: c.name ?? c.id })}</strong>
                    <span className={`conflict-chip ${c.severity === "error" ? "error-chip" : "warn-chip"}`}>
                      {t(c.severity === "error" ? "conflicts.error" : "conflicts.warning")}
                    </span>
                  </div>
                  <p className="muted">
                    {t(`conflicts.${c.kind}Text` as MessageKey, { id: c.id, name: c.name ?? c.id, wanted: c.wanted ?? "", found: c.found ?? "" })}
                  </p>
                  <span className="muted conflict-label">{t("conflicts.affected")}</span>
                  <ul className="conflict-files">
                    {c.files.map((f) => (
                      <li key={f}>
                        <span className="path">{f}</span>
                        <button className="btn btn-ghost" onClick={() => disable(f)}>
                          {t("conflicts.disable")}
                        </button>
                      </li>
                    ))}
                  </ul>
                  {c.kind === "missing" && (
                    <div className="actions">
                      <button className="btn" onClick={() => onFind(c.id)}>
                        <Glyph name="search" /> {t("conflicts.find")}
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          );
        })}
    </section>
  );
}
