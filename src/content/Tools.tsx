import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { Glyph } from "../ui/Glyph";
import { Empty } from "../ui/Empty";

type Report = { files: { file: string; enabled: boolean; verified: boolean; infected: boolean; quarantined: boolean }[]; system: string[]; quarantineDir: string };

export function SafetyCheck({ instanceId }: { instanceId: string }) {
  const { t } = useI18n();
  const [report, setReport] = useState<Report | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => setReport(null), [instanceId]);

  const run = () => {
    setBusy(true);
    setError(null);
    invoke<Report>("content_safety", { instanceId }).then(setReport, (e) => setError(String(e))).finally(() => setBusy(false));
  };

  const infected = report?.files.filter((f) => f.infected) ?? [];
  const unknown = report?.files.filter((f) => !f.verified && !f.infected) ?? [];
  return (
    <div className="safety">
      <div className="actions">
        <button className="btn btn-ghost" disabled={busy} onClick={run}>
          <Glyph name="search" /> {busy ? t("safety.running") : t("safety.run")}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
      {report && (
        <>
          {report.system.length > 0 && (
            <p className="error">
              {t("safety.system")} {report.system.join(", ")}
            </p>
          )}
          {infected.map((f) => (
            <p key={f.file} className="error">
              {t(f.quarantined ? "safety.quarantined" : "safety.infected", { file: f.file, dir: report.quarantineDir })}
            </p>
          ))}
          {infected.length === 0 && report.system.length === 0 && <Empty bee="success">{t("safety.clean", { n: report.files.length })}</Empty>}
          {unknown.length > 0 && (
            <p className="muted">
              {t("safety.unknown")} {unknown.map((f) => f.file).join(", ")}
            </p>
          )}
        </>
      )}
    </div>
  );
}

type Order = { enabled: string[]; available: string[] };

export function PackOrder({ instanceId, version }: { instanceId: string; version: number }) {
  const { t } = useI18n();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = () => invoke<Order>("pack_order", { instanceId }).then(setOrder, (e) => setError(String(e)));
  useEffect(() => {
    load();
  }, [instanceId, version]);
  if (!order || order.enabled.length + order.available.length === 0) return null;

  const top = [...order.enabled].reverse();
  const save = (topFirst: string[]) =>
    invoke("pack_order_set", { instanceId, enabled: [...topFirst].reverse() }).then(
      () => (setError(null), load()),
      (e) => setError(String(e)),
    );
  const move = (i: number, by: number) => {
    const next = [...top];
    const j = i + by;
    if (j < 0 || j >= next.length) return;
    [next[i], next[j]] = [next[j], next[i]];
    save(next);
  };

  return (
    <div className="pack-order">
      <h3>{t("packs.title")}</h3>
      <p className="muted">{t("packs.hint")}</p>
      {error && <p className="error">{error}</p>}
      <ul className="rows compact">
        {top.map((p, i) => (
          <li key={p} className="row plain">
            <div className="list-main">
              <span>{p}</span>
            </div>
            <div className="row-tools">
              <button className="btn btn-ghost" disabled={i === 0} aria-label={t("packs.up")} title={t("packs.up")} onClick={() => move(i, -1)}>
                <Glyph name="chevron-down" />
              </button>
              <button className="btn btn-ghost" disabled={i === top.length - 1} aria-label={t("packs.down")} title={t("packs.down")} onClick={() => move(i, 1)}>
                <Glyph name="chevron-down" />
              </button>
              <button className="btn btn-ghost" aria-label={t("packs.off")} title={t("packs.off")} onClick={() => save(top.filter((x) => x !== p))}>
                <Glyph name="x" />
              </button>
            </div>
          </li>
        ))}
        {order.available.map((p) => (
          <li key={p} className="row plain">
            <div className="list-main">
              <span className="muted">{p}</span>
            </div>
            <div className="row-tools">
              <button className="btn btn-ghost" aria-label={t("packs.on")} title={t("packs.on")} onClick={() => save([p, ...top])}>
                <Glyph name="plus" />
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
