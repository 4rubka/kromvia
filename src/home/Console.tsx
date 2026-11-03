import { useEffect, useRef, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";

export type LogLine = { level: string; thread: string; text: string; noise: boolean };

export function Console({ lines }: { lines: LogLine[] }) {
  const { t } = useI18n();
  const [showNoise, setShowNoise] = useState(false);
  const [shared, setShared] = useState<string | null>(null);
  const share = () => {
    const text = lines.map((l) => (l.thread ? `[${l.thread}/${l.level}] ${l.text}` : l.text)).join("\n").slice(-120_000);
    invoke<string>("share_log", { text }).then(
      (url) => {
        navigator.clipboard.writeText(url);
        setShared(url);
      },
      (e) => setShared(String(e)),
    );
  };
  const box = useRef<HTMLDivElement>(null);
  const stick = useRef(true);

  const visible = showNoise ? lines : lines.filter((l) => !l.noise);
  const hidden = lines.length - visible.length;

  useEffect(() => {
    if (stick.current && box.current) box.current.scrollTop = box.current.scrollHeight;
  }, [visible.length]);

  return (
    <section className="console-wrap">
      <div className="console-head">
        <h2>{t("console.title")}</h2>
        <label className="muted">
          <input type="checkbox" checked={showNoise} onChange={(e) => setShowNoise(e.currentTarget.checked)} />{" "}
          {t("console.showNoise", { count: hidden })}
        </label>
        <button className="btn btn-ghost" onClick={share}>
          {shared ?? t("console.share")}
        </button>
      </div>
      <div
        ref={box}
        className="console"
        onScroll={(e) => {
          const el = e.currentTarget;
          stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < 24;
        }}
      >
        {visible.map((l, i) => (
          <div key={i} className="log-line" data-level={l.level}>
            {l.thread && <span className="muted">[{l.thread}/{l.level}] </span>}
            {l.text}
          </div>
        ))}
      </div>
    </section>
  );
}
