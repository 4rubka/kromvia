import { useState } from "react";
import { LANGUAGES, language, setLanguage, useI18n } from "../i18n";
import type { MessageKey } from "../i18n/en";
import { useLauncher } from "../lib/launcher";
import type { ThemePref } from "../theme";
import { Select } from "../ui/Select";
import { Glyph } from "../ui/Glyph";

const DONE_KEY = "kromvia.onboarded";
const THEMES: ThemePref[] = ["system", "light", "dark"];

export function needsOnboarding() {
  try {
    if (localStorage.getItem(DONE_KEY) === "1") return false;
    for (let i = 0; i < localStorage.length; i++) if (localStorage.key(i)?.startsWith("kromvia.")) return false;
    return true;
  } catch {
    return false;
  }
}

function finish() {
  try {
    localStorage.setItem(DONE_KEY, "1");
  } catch {}
}

export function Onboarding({
  theme,
  setTheme,
  onDone,
}: {
  theme: ThemePref;
  setTheme: (t: ThemePref) => void;
  onDone: (go: "home" | "accounts" | "builds") => void;
}) {
  const { t } = useI18n();
  const l = useLauncher();
  const [step, setStep] = useState(0);
  const done = (go: "home" | "accounts" | "builds") => {
    finish();
    onDone(go);
  };

  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label={t("onboarding.title")}>
      <section className="card onboarding">
        <span className="muted">{t("onboarding.step", { n: step + 1, total: 3 })}</span>
        {step === 0 && (
          <>
            <img className="onboarding-bee" src="/bee/welcome.webp" alt="" draggable={false} />
            <h2>{t("onboarding.title")}</h2>
            <p className="muted">{t("onboarding.intro")}</p>
            <div className="field">
              <span>{t("settings.language")}</span>
              <Select value={language} label={t("settings.language")} options={Object.entries(LANGUAGES).map(([value, label]) => ({ value, label }))} onChange={setLanguage} />
            </div>
            <div className="field">
              <span>{t("settings.theme")}</span>
              <div className="segmented">
                {THEMES.map((th) => (
                  <button key={th} aria-pressed={theme === th} onClick={() => setTheme(th)}>
                    {t(`settings.theme.${th}` as MessageKey)}
                  </button>
                ))}
              </div>
            </div>
          </>
        )}
        {step === 1 && (
          <>
            <h2>{t("onboarding.accountTitle")}</h2>
            <p className="muted">{t("onboarding.accountText")}</p>
            <div className="field">
              <span>{t("onboarding.nick")}</span>
              <input className="input" value={l.nick} maxLength={16} placeholder="Steve" onChange={(e) => l.setNick(e.currentTarget.value)} />
            </div>
            <button className="btn btn-ghost" onClick={() => done("accounts")}>
              <Glyph name="login-2" /> {t("onboarding.microsoft")}
            </button>
          </>
        )}
        {step === 2 && (
          <>
            <h2>{t("onboarding.buildTitle")}</h2>
            <p className="muted">{t("onboarding.buildText")}</p>
            <div className="actions">
              <button className="btn" onClick={() => done("home")}>
                <Glyph name="plus" /> {t("onboarding.newBuild")}
              </button>
              <button className="btn btn-ghost" onClick={() => done("builds")}>
                <Glyph name="file-import" /> {t("onboarding.import")}
              </button>
            </div>
            <p className="muted">{t("onboarding.keys")}</p>
          </>
        )}
        <div className="actions">
          <button className="btn btn-ghost" onClick={() => done("home")}>
            {t("onboarding.skip")}
          </button>
          {step > 0 && (
            <button className="btn btn-ghost" onClick={() => setStep(step - 1)}>
              <Glyph name="chevron-left" /> {t("onboarding.back")}
            </button>
          )}
          {step < 2 && (
            <button className="btn" onClick={() => setStep(step + 1)}>
              {t("onboarding.next")} <Glyph name="chevron-right" />
            </button>
          )}
        </div>
      </section>
    </div>
  );
}

export const HOTKEYS: [string, MessageKey][] = [
  ["Ctrl+1…9", "keys.pages"],
  ["Ctrl+,", "keys.settings"],
  ["?", "keys.help"],
  ["Esc", "keys.close"],
];

export function HotkeyHelp({ onClose }: { onClose: () => void }) {
  const { t } = useI18n();
  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label={t("keys.title")} onClick={onClose}>
      <section className="card onboarding" onClick={(e) => e.stopPropagation()}>
        <h2>{t("keys.title")}</h2>
        <ul className="rows compact">
          {HOTKEYS.map(([k, label]) => (
            <li key={k} className="row plain">
              <kbd>{k}</kbd>
              <span>{t(label)}</span>
            </li>
          ))}
        </ul>
        <div className="actions">
          <button className="btn" onClick={onClose}>
            {t("keys.ok")}
          </button>
        </div>
      </section>
    </div>
  );
}
