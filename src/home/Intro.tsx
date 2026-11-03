import { useEffect, useRef } from "react";
import { useI18n } from "../i18n";

export const INTRO_CREDITS = [
  "Intro animation: “If Minecraft had a Netflix Opening Credits” by Cube Canvas (https://www.youtube.com/watch?v=5NARjQQKgf4).",
  "Music: “Aria Math” by C418, “Aria Math x Cinematic Orchestra” arrangement by AMBNT.",
  "Minecraft © Mojang AB / Microsoft. Kromvia is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft.",
];

export function Intro({ onDone }: { onDone: () => void }) {
  const { t } = useI18n();
  const video = useRef<HTMLVideoElement>(null);
  const retried = useRef(false);

  const play = (v: HTMLVideoElement) =>
    v.play().catch(() => {
      v.muted = true;
      return v.play();
    });

  const fallback = () => {
    const v = video.current;
    if (!v || retried.current) return onDone();
    retried.current = true;
    fetch("/intro.mp4")
      .then((r) => (r.ok ? r.blob() : Promise.reject()))
      .then((blob) => {
        v.src = URL.createObjectURL(new Blob([blob], { type: "video/mp4" }));
        return play(v);
      })
      .catch(onDone);
  };

  useEffect(() => {
    const v = video.current;
    if (v) play(v).catch(fallback);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="intro" role="dialog" aria-modal="true" aria-label="Kromvia">
      <video ref={video} playsInline onEnded={onDone} onError={fallback}>
        <source src="/intro.webm" type="video/webm" />
        <source src="/intro.mp4" type="video/mp4" onError={fallback} />
      </video>
      <div className="intro-credits">
        {INTRO_CREDITS.map((line) => (
          <p key={line}>{line}</p>
        ))}
      </div>
      <button className="btn btn-ghost intro-skip" onClick={onDone}>
        {t("onboarding.skip")}
      </button>
    </div>
  );
}
