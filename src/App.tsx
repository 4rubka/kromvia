import { useEffect, useRef, useState, type CSSProperties } from "react";
import { getVersion } from "@tauri-apps/api/app";
import { invoke } from "@tauri-apps/api/core";
import { LANGUAGES, language, setLanguage, useI18n } from "./i18n";
import { Select } from "./ui/Select";
import type { MessageKey } from "./i18n/en";
import { Accounts } from "./accounts/Accounts";
import { Builds } from "./builds/Builds";
import { Content, type Kind } from "./content/Content";
import { Friends } from "./friends/Friends";
import { SocialProvider, useSocial } from "./friends/social";
import { Home } from "./home/Home";
import { Profile } from "./profile/Profile";
import { Rooms } from "./rooms/Rooms";
import { LauncherProvider } from "./lib/launcher";
import { JavaSettings } from "./settings/JavaSettings";
import { Storage } from "./settings/Storage";
import { Updates, useUpdate } from "./settings/Updates";
import { Legal } from "./settings/Legal";
import { AppearanceExtras, SettingsBackup, useAppearance } from "./settings/Appearance";
import { UiEditor, useUiProfile } from "./settings/UiEditor";
import { HotkeyHelp, needsOnboarding, Onboarding } from "./home/Onboarding";
import { Intro } from "./home/Intro";
import { Wardrobe } from "./skins/Wardrobe";
import { useTheme, type ThemePref } from "./theme";
import { Icon, type IconName } from "./ui/Icon";
import { Glyph } from "./ui/Glyph";
import { MiniPlayer, miniPlayerOn, setMiniPlayerOn } from "./ui/MiniPlayer";

type Page = "home" | "profile" | "rooms" | "friends" | "builds" | "content" | "skins" | "accounts" | "settings";
const PAGES: Page[] = ["home", "profile", "rooms", "friends", "builds", "content", "skins", "accounts", "settings"];
const ICONS: Record<Page, IconName> = {
  home: "home",
  profile: "profile",
  rooms: "rooms",
  friends: "friends",
  builds: "library",
  content: "content",
  skins: "wardrobe",
  accounts: "accounts",
  settings: "settings",
};
const THEMES: ThemePref[] = ["system", "light", "dark"];
const HIDEABLE = PAGES.filter((p) => p !== "home" && p !== "settings");

function App() {
  const { t } = useI18n();
  const [page, setPage] = useState<Page>("home");
  const [contentTab, setContentTab] = useState<Kind>("mod");
  const [theme, setTheme] = useTheme();
  const background = useAppearance();
  const [intro, setIntro] = useState(needsOnboarding);
  const [onboarding, setOnboarding] = useState(needsOnboarding);
  const [keysHelp, setKeysHelp] = useState(false);
  const ui = useUiProfile();
  const pages = PAGES.filter((p) => !ui.hidden.includes(p));
  const pagesRef = useRef(pages);
  pagesRef.current = pages;
  const main = useRef<HTMLElement>(null);

  useEffect(() => {
    main.current?.scrollTo(0, 0);
  }, [page]);

  useEffect(() => {
    if (!pages.includes(page)) setPage("home");
  }, [pages, page]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const typing = e.target instanceof HTMLElement && (e.target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(e.target.tagName));
      const mod = e.ctrlKey || e.metaKey;
      if (e.key === "Escape") setKeysHelp(false);
      if (mod && /^[1-9]$/.test(e.key) && pagesRef.current[Number(e.key) - 1]) {
        e.preventDefault();
        setPage(pagesRef.current[Number(e.key) - 1]);
      } else if (mod && e.key === ",") {
        e.preventDefault();
        setPage("settings");
      } else if (!typing && !mod && e.key === "?") {
        setKeysHelp((v) => !v);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  useEffect(() => {
    invoke("tray_labels", { show: t("tray.show"), quit: t("tray.quit") }).catch(() => {});
  }, [t]);

  return (
    <LauncherProvider>
      <SocialProvider>
        {background ? (
          <img className="app-bg" src={background} alt="" aria-hidden />
        ) : (
          <video className="app-bg" autoPlay loop muted playsInline aria-hidden>
            <source src="/hero.webm" type="video/webm" />
            <source src="/hero.mp4" type="video/mp4" />
          </video>
        )}
        <div className="shell">
          <nav className="rail" aria-label="Kromvia">
            <img className="rail-logo" src="/logo.svg" alt="Kromvia" width="34" height="34" />
            <div className="rail-items" style={{ "--nav-index": pages.indexOf(page) } as CSSProperties}>
              <div className="rail-marker" aria-hidden="true" />
              {pages.map((p) => (
                <button
                  key={p}
                  className="rail-btn"
                  aria-current={page === p ? "page" : undefined}
                  aria-label={t(`nav.${p}` as MessageKey)}
                  data-tip={t(`nav.${p}` as MessageKey)}
                  onClick={() => setPage(p)}
                >
                  <Icon name={ICONS[p]} size={18} />
                  {p === "friends" && <UnreadBadge />}
                  {p === "settings" && <UpdateBadge />}
                </button>
              ))}
            </div>
            <MiniPlayer />
          </nav>
          <main className="main" ref={main}>
            <div className="page" key={page} data-page={page}>
              {page === "home" && <Home onNavigate={setPage} />}
              {page === "profile" && <Profile onFriends={() => setPage("friends")} />}
              {page === "rooms" && <Rooms />}
              {page === "friends" && <Friends />}
              {page === "builds" && (
                <Builds
                  onAddContent={(kind) => {
                    setContentTab(kind);
                    setPage("content");
                  }}
                />
              )}
              {page === "content" && <Content initialTab={contentTab} />}
              {page === "skins" && <Wardrobe />}
              {page === "accounts" && <Accounts />}
              {page === "settings" && <Settings theme={theme} setTheme={setTheme} />}
            </div>
          </main>
        </div>
        {intro && <Intro onDone={() => setIntro(false)} />}
        {onboarding && !intro && (
          <Onboarding
            theme={theme}
            setTheme={setTheme}
            onDone={(go) => {
              setOnboarding(false);
              setPage(go);
            }}
          />
        )}
        {keysHelp && <HotkeyHelp onClose={() => setKeysHelp(false)} />}
      </SocialProvider>
    </LauncherProvider>
  );
}

function UnreadBadge() {
  const { totalUnread } = useSocial();
  return totalUnread > 0 ? <span className="unread">{totalUnread}</span> : null;
}

function UpdateBadge() {
  return useUpdate() ? <span className="unread">↑</span> : null;
}

function MiniPlayerToggle() {
  const { t } = useI18n();
  const [on, setOn] = useState(miniPlayerOn);
  return (
    <label className="muted">
      <input
        type="checkbox"
        checked={on}
        onChange={(e) => {
          setOn(e.currentTarget.checked);
          setMiniPlayerOn(e.currentTarget.checked);
        }}
      />{" "}
      {t("player.setting")}
    </label>
  );
}

function Settings({ theme, setTheme }: { theme: ThemePref; setTheme: (t: ThemePref) => void }) {
  const { t } = useI18n();
  const [version, setVersion] = useState("");

  useEffect(() => {
    getVersion().then(setVersion, () => {});
  }, []);

  return (
    <>
      <h1 className="page-title">{t("nav.settings")}</h1>
      <Updates />
      <section className="card">
        <h2>{t("settings.appearance")}</h2>
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
        <div className="field">
          <span>{t("settings.language")}</span>
          <Select
            value={language}
            label={t("settings.language")}
            options={Object.entries(LANGUAGES).map(([value, label]) => ({ value, label }))}
            onChange={setLanguage}
          />
        </div>
        <MiniPlayerToggle />
        <AppearanceExtras />
      </section>
      <UiEditor pages={HIDEABLE} />
      <JavaSettings />
      <Storage />
      <SettingsBackup />
      <button className="btn btn-ghost" onClick={() => invoke("launcher_log_open")}>
        <Glyph name="folder" /> {t("settings.launcherLog")}
      </button>
      {version && <p className="version muted">{t("settings.version", { version })}</p>}
      <Legal />
    </>
  );
}

export default App;
