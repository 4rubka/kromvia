import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { Glyph } from "../ui/Glyph";
import { Empty } from "../ui/Empty";

type Fix =
  | { type: "disable"; file: string }
  | { type: "find"; query: string }
  | { type: "memory"; mb: number }
  | { type: "option"; key: string; value: string }
  | { type: "shadersOff" }
  | { type: "conflicts" };

type Finding = {
  severity: "high" | "medium" | "low";
  kind: string;
  subject: string | null;
  value: string | null;
  fix: Fix | null;
};

const SEVERITY: Record<Finding["severity"], MessageKey> = { high: "lag.high", medium: "lag.medium", low: "lag.low" };

export function LagCheck({ instanceId, onFind, onConflicts }: { instanceId: string; onFind: (query: string) => void; onConflicts: () => void }) {
  const { t } = useI18n();
  const [list, setList] = useState<Finding[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const scan = () => {
    setBusy(true);
    setError(null);
    invoke<Finding[]>("lag_scan", { instanceId }).then(setList, (e) => setError(String(e))).finally(() => setBusy(false));
  };
  useEffect(() => {
    setList(null);
    scan();
  }, [instanceId]);

  const apply = (fix: Fix) => {
    if (fix.type === "find") return onFind(fix.query);
    if (fix.type === "conflicts") return onConflicts();
    const done = fix.type === "disable" ? invoke("content_set_enabled", { instanceId, file: fix.file, enabled: false }) : invoke("lag_fix", { instanceId, fix });
    done.then(scan, (e) => setError(String(e)));
  };

  const params = (f: Finding) => {
    const [a, b] = (f.value ?? "").split("|");
    return { subject: f.subject ?? "", value: a ?? "", recommended: b ?? "" };
  };

  return (
    <section className="card conflicts">
      <header className="conflicts-head">
        <h2>{t("lag.title")}</h2>
        <button className="btn btn-ghost" disabled={busy} onClick={scan}>
          <Glyph name="rotate" /> {busy ? t("lag.scanning") : t("lag.rescan")}
        </button>
      </header>
      <p className="muted">{t("lag.hint")}</p>
      {error && <p className="error">{error}</p>}
      {list && list.length === 0 && <Empty bee="success">{t("lag.none")}</Empty>}
      {list?.map((f, i) => (
        <div key={i} className={`conflict ${f.severity === "high" ? "error" : "warning"}`}>
          <div className="conflict-top">
            <strong>{t(`lag.${f.kind}` as MessageKey, params(f))}</strong>
            <span className={`conflict-chip ${f.severity === "high" ? "error-chip" : "warn-chip"}`}>{t(SEVERITY[f.severity])}</span>
          </div>
          <p className="muted">{t(`lag.${f.kind}Text` as MessageKey, params(f))}</p>
          {f.fix && (
            <div className="actions">
              <button className="btn" onClick={() => apply(f.fix!)}>
                {f.fix.type === "find" && <Glyph name="search" />} {t(`lag.fix.${f.fix.type}` as MessageKey, { ...params(f), mb: f.fix.type === "memory" ? f.fix.mb : "" })}
              </button>
            </div>
          )}
        </div>
      ))}
    </section>
  );
}
