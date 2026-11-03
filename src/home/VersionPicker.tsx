import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { LOADER_NAMES, type LoaderKind } from "../lib/launcher";
import { Select } from "../ui/Select";

type McVersion = { id: string; type: string; releaseTime: string };
type LoaderVersion = { version: string; stable: boolean };

export type VersionChoice = { mc: string; loader: LoaderKind | null; build: string };

const TYPES = ["release", "snapshot", "old_beta", "old_alpha"] as const;
const LOADERS: (LoaderKind | null)[] = [null, "fabric", "quilt", "forge", "neoforge", "legacyfabric", "ornithe"];

export function VersionPicker({ value, onChange }: { value: VersionChoice; onChange: (v: VersionChoice) => void }) {
  const { t } = useI18n();
  const [versions, setVersions] = useState<McVersion[] | null>(null);
  const [types, setTypes] = useState<Set<string>>(new Set(["release"]));
  const [builds, setBuilds] = useState<LoaderVersion[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { mc, loader, build } = value;

  useEffect(() => {
    invoke<McVersion[]>("mc_versions").then(
      (v) => {
        setVersions(v);
        const current = v.find((x) => x.id === mc);
        if (current) setTypes((cur) => new Set(cur).add(current.type));
        else onChange({ ...value, mc: v.find((x) => x.type === "release")?.id ?? "" });
      },
      (e) => setError(String(e)),
    );
  }, []);

  useEffect(() => {
    setBuilds(null);
    if (!loader || !mc) return;
    let stale = false;
    invoke<LoaderVersion[]>("loader_versions", { kind: loader, mc }).then(
      (list) => {
        if (stale) return;
        setBuilds(list);
        if (!list.some((b) => b.version === build)) {
          onChange({ mc, loader, build: (list.find((b) => b.stable) ?? list[0])?.version ?? "" });
        }
      },
      () => !stale && setBuilds([]),
    );
    return () => {
      stale = true;
    };
  }, [loader, mc]);

  const list = versions?.filter((v) => types.has(v.type)) ?? [];
  const toggleType = (kind: string) =>
    setTypes((cur) => {
      const next = new Set(cur);
      if (next.has(kind) && next.size > 1) next.delete(kind);
      else next.add(kind);
      return next;
    });

  return (
    <>
      <div className="field">
        <span>{t("instance.version")}</span>
        {versions === null ? (
          <span className="muted">{error ?? t("common.loading")}</span>
        ) : (
          <Select
            value={mc}
            label={t("instance.version")}
            options={list.map((v) => ({ value: v.id, label: v.id }))}
            onChange={(id) => onChange({ ...value, mc: id })}
          />
        )}
      </div>
      <div className="field checks">
        {TYPES.map((kind) => (
          <label key={kind} className="muted">
            <input type="checkbox" checked={types.has(kind)} onChange={() => toggleType(kind)} /> {t(`version.${kind}` as MessageKey)}
          </label>
        ))}
      </div>
      <div className="field">
        <span>{t("instance.loader")}</span>
        <div className="segmented wrap">
          {LOADERS.map((l) => (
            <button key={l ?? "vanilla"} aria-pressed={loader === l} onClick={() => onChange({ mc, loader: l, build: "" })}>
              {l ? LOADER_NAMES[l] : t("instance.vanilla")}
            </button>
          ))}
        </div>
      </div>
      {loader && (
        <div className="field">
          <span>{t("instance.loaderVersion")}</span>
          {builds === null ? (
            <span className="muted">{t("common.loading")}</span>
          ) : builds.length === 0 ? (
            <span className="muted">{t("instance.noLoader", { loader: LOADER_NAMES[loader], mc })}</span>
          ) : (
            <Select
              value={build}
              label={t("instance.loaderVersion")}
              options={builds.map((b) => ({ value: b.version, label: b.stable ? b.version : `${b.version} (${t("instance.unstable")})` }))}
              onChange={(v) => onChange({ ...value, build: v })}
            />
          )}
        </div>
      )}
    </>
  );
}

export const versionReady = (v: VersionChoice) => Boolean(v.mc && (!v.loader || v.build));
