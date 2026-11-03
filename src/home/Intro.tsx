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

  useEffect(() => {
    const v = video.current;
    v?.play().catch(() => {
      v.muted = true;
      v.play().catch(onDone);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="intro" role="dialog" aria-modal="true" aria-label="Kromvia">
      <video ref={video} playsInline onEnded={onDone} onError={onDone}>
        <source src="/intro.webm" type="video/webm" />
        <source src="/intro.mp4" type="video/mp4" />
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
