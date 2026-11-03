import { useI18n } from "../i18n";
import { MODELS } from "../art/catalog";
import { INTRO_CREDITS } from "../home/Intro";

/** Licences and third-party credits, folded away at the bottom of Settings. */
export function Legal() {
  const { t } = useI18n();
  const credits = [...new Map(MODELS.map((m) => [`${m.title}|${m.author}`, m])).values()];
  return (
    <details className="legal">
      <summary>{t("legal.title")}</summary>
      <p>{t("legal.privacy")}</p>
      <p>{t("legal.models")}</p>
      <ul>
        {credits.map((m) => (
          <li key={m.id}>
            “{m.title}” by {m.author} ({m.url}), CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/)
          </li>
        ))}
      </ul>
      {INTRO_CREDITS.map((line) => (
        <p key={line}>{line}</p>
      ))}
      <p>Icons: Tabler Icons by Paweł Kuna (https://tabler.io/icons), MIT License.</p>
    </details>
  );
}
