import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { Glyph } from "../ui/Glyph";
import { useJobs } from "../lib/jobs";
import type { Instance } from "../lib/launcher";

const PROJECT = "xaeros-minimap";
const DISMISSED_KEY = "kromvia.minimapDismissed";

function dismissed(): string[] {
  try {
    const list = JSON.parse(localStorage.getItem(DISMISSED_KEY) ?? "[]");
    return Array.isArray(list) ? list : [];
  } catch {
    return [];
  }
}

export function MinimapOffer({ inst }: { inst: Instance }) {
  const { t } = useI18n();
  const [missing, setMissing] = useState(false);
  const [hidden, setHidden] = useState(() => dismissed().includes(inst.id));
  const [result, setResult] = useState<string | null | undefined>(undefined);
  const key = `content-${inst.id}-${PROJECT}`;
  const { jobs, started } = useJobs(key, (_, error) => setResult(error));

  useEffect(() => {
    setHidden(dismissed().includes(inst.id));
    setResult(undefined);
    invoke<{ file: string; title: string }[]>("content_list", { instanceId: inst.id }).then(
      (files) => setMissing(!files.some((f) => /xaero/i.test(f.file) || /xaero/i.test(f.title))),
      () => setMissing(false),
    );
  }, [inst.id]);

  if (inst.loader?.kind !== "fabric" && inst.loader?.kind !== "quilt") return null;
  if (result === null) return <small className="muted">{t("minimap.added")}</small>;
  if (!missing || hidden) return null;
  if (jobs[key]) return <small className="muted">{t("minimap.adding")}</small>;

  const add = () =>
    invoke<number>("content_install", { instanceId: inst.id, projectId: PROJECT, kind: "mod", source: null, versionId: null, world: null }).then(
      (id) => started(key, id),
      (e) => setResult(String(e)),
    );
  const dismiss = () => {
    try {
      localStorage.setItem(DISMISSED_KEY, JSON.stringify([...dismissed(), inst.id]));
    } catch {}
    setHidden(true);
  };

  return (
    <div className="minimap-offer">
      <small className="muted">{t("minimap.offer")}</small>
      {result && <p className="error">{result}</p>}
      <span className="actions">
        <button className="btn" onClick={add}>
          <Glyph name="map" /> {t("minimap.add")}
        </button>
        <button className="btn btn-ghost" onClick={dismiss}>
          {t("minimap.dismiss")}
        </button>
      </span>
    </div>
  );
}
