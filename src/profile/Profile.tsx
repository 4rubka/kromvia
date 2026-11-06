import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { describe, formatDuration, useLauncher, type Instance } from "../lib/launcher";
import { BlockIcon, InstanceIcon } from "../art/ModelIcon";
import type { BlockKind } from "../art/blocks";
import type { MessageKey } from "../i18n/en";
import { SkinArt } from "../art/SkinArt";
import { outfitWorn, savedOutfit } from "../lib/outfit";
import { useSocial } from "../friends/social";
import { Glyph } from "../ui/Glyph";
import { Empty } from "../ui/Empty";
import "./profile.css";

type Me = { id: string; name: string; verified: boolean; created?: number; likes?: number; links?: Partial<Record<Provider, string>> };
type Provider = "discord" | "telegram";
type Ranked = Friend & { likes: number };
type Facts = { hours: number; launches: number; builds: number; worlds: number; friends: number; likes: number; verified: boolean; linked: boolean; days: number; created: number };

const BADGES: { id: string; block: BlockKind; earned: (f: Facts) => boolean }[] = [
  { id: "early", block: "portal", earned: (f) => f.created > 0 && f.created < Date.UTC(2027, 0, 1) },
  { id: "firstLaunch", block: "crafting", earned: (f) => f.launches >= 1 },
  { id: "launches100", block: "crafting", earned: (f) => f.launches >= 100 },
  { id: "hours10", block: "grass", earned: (f) => f.hours >= 10 },
  { id: "hours100", block: "grass", earned: (f) => f.hours >= 100 },
  { id: "hours500", block: "grass", earned: (f) => f.hours >= 500 },
  { id: "builds5", block: "stone", earned: (f) => f.builds >= 5 },
  { id: "worlds10", block: "earth", earned: (f) => f.worlds >= 10 },
  { id: "friends1", block: "wool", earned: (f) => f.friends >= 1 },
  { id: "friends10", block: "wool", earned: (f) => f.friends >= 10 },
  { id: "likes10", block: "wool", earned: (f) => f.likes >= 10 },
  { id: "likes100", block: "portal", earned: (f) => f.likes >= 100 },
  { id: "verified", block: "stone", earned: (f) => f.verified },
  { id: "linked", block: "portal", earned: (f) => f.linked },
  { id: "veteran", block: "earth", earned: (f) => f.days >= 365 },
];
type Friend = { id: string; name: string; verified: boolean };
type Stats = { playSeconds: number; launches: number; lastPlayed: number };
type World = { folder: string; name: string; lastPlayed: number; mode: string; icon: string | null };
type LibrarySkin = { id: string; slim: boolean; dataUrl: string };

export function Profile({ onFriends }: { onFriends: () => void }) {
  const { t } = useI18n();
  const l = useLauncher();
  const social = useSocial();
  const [me, setMe] = useState<Me | null | undefined>(undefined);
  const [friends, setFriends] = useState<Friend[]>([]);
  const [stats, setStats] = useState<Record<string, Stats>>({});
  const [worlds, setWorlds] = useState<(World & { inst: Instance })[]>([]);
  const [skin, setSkin] = useState<LibrarySkin | null>(null);
  const [tab, setTab] = useState<"overview" | "friends" | "top">("overview");
  const [liked, setLiked] = useState<Set<string>>(new Set());
  const [top, setTop] = useState<Ranked[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadMe = () =>
    invoke<Me | null>("social_me").then((m) => {
      setMe(m);
      if (m) {
        invoke<{ friends: Friend[] }>("social_friends").then((f) => setFriends(f.friends), () => {});
        invoke<string[]>("social_likes").then((l) => setLiked(new Set(l)), () => {});
      }
    }, () => setMe(null));
  const loadTop = () => invoke<Ranked[]>("social_top").then(setTop, () => {});
  useEffect(() => {
    loadMe();
    loadTop();
  }, []);

  const like = (id: string) => {
    const on = !liked.has(id);
    setError(null);
    invoke("social_like", { id, on }).then(
      () => {
        setLiked((s) => {
          const n = new Set(s);
          if (on) n.add(id);
          else n.delete(id);
          return n;
        });
        loadTop();
      },
      (e) => setError(String(e)),
    );
  };
  const link = (p: Provider) => {
    setError(null);
    invoke(me?.links?.[p] ? "social_unlink" : "social_link", { provider: p }).then(loadMe, (e) => setError(String(e)));
  };

  const remove = () => {
    if (!deleting) return setDeleting(true);
    setDeleting(false);
    setError(null);
    invoke("social_delete").then(() => {
      setMe(null);
      setFriends([]);
      setLiked(new Set());
      loadTop();
    }, (e) => setError(String(e)));
  };

  const who = l.account?.name ?? l.nick.trim();
  useEffect(() => {
    const look = (name: string) => invoke<LibrarySkin>("skin_lookup", { nick: name });
    invoke<LibrarySkin[]>("wardrobe_list")
      .then((list) => list[0] ?? Promise.reject())
      .catch(() => look(who || "MHF_Steve"))
      .catch(() => look("MHF_Steve"))
      .then(setSkin, () => {});
  }, [who]);

  const ids = l.instances.map((i) => i.id).join(",");
  useEffect(() => {
    l.instances.forEach((inst) => {
      invoke<{ stats: Stats }>("instance_info", { id: inst.id }).then((info) => setStats((s) => ({ ...s, [inst.id]: info.stats })), () => {});
    });
    Promise.all(l.instances.map((inst) => invoke<World[]>("world_list", { instanceId: inst.id }).then((ws) => ws.map((w) => ({ ...w, inst })), () => [])))
      .then((all) => setWorlds(all.flat().sort((a, b) => b.lastPlayed - a.lastPlayed)));
  }, [ids]);

  if (me === undefined) return <p className="muted">{t("friends.loading")}</p>;

  const name = me?.name ?? l.account?.name ?? (l.nick.trim() || t("home.guest"));
  const all = Object.values(stats);
  const played = all.reduce((n, s) => n + s.playSeconds, 0);
  const launches = all.reduce((n, s) => n + s.launches, 0);
  const builds = [...l.instances]
    .filter((i) => (stats[i.id]?.playSeconds ?? 0) > 0)
    .sort((a, b) => stats[b.id].playSeconds - stats[a.id].playSeconds)
    .slice(0, 5);
  const facts: Facts = {
    hours: played / 3600,
    launches,
    builds: l.instances.length,
    worlds: worlds.length,
    friends: friends.length,
    likes: me?.likes ?? 0,
    verified: !!me?.verified,
    linked: !!me?.links && Object.keys(me.links).length > 0,
    days: me?.created ? (Date.now() - me.created) / 86_400_000 : 0,
    created: me?.created ?? 0,
  };
  const badges = BADGES.map((b) => ({ ...b, has: b.earned(facts) })).sort((a, b) => Number(b.has) - Number(a.has));
  const likeButton = (id: string) =>
    me && id !== me.id ? (
      <button className="btn btn-ghost profile-like" data-on={liked.has(id)} title={t("profile.like")} onClick={() => like(id)}>
        ♥
      </button>
    ) : null;
  const online = friends.filter((f) => social.online.has(f.id));
  const sorted = [...online, ...friends.filter((f) => !social.online.has(f.id))];

  return (
    <>
      <section className="card profile-hero">
        <div className="profile-art">
          <SkinArt skin={skin?.dataUrl ?? null} slim={skin?.slim ?? false} back="cape" worn={outfitWorn(savedOutfit())} zoom={0.9} />
        </div>
        <div className="profile-info">
          <h1 className="page-title">
            {name}
            {me?.verified && (
              <span className="profile-verified" title={t("profile.verified")}>
                <Glyph name="check" size={12} />
              </span>
            )}
          </h1>
          {me ? (
            <p className="muted">
              {me.created ? t("profile.since", { date: new Date(me.created).toLocaleDateString() }) : t("profile.kromvia")}
              {" · "}
              <span className="presence-dot" data-online={social.connected} /> {social.connected ? t("friends.online") : t("friends.offline")}
            </p>
          ) : (
            <p className="muted">
              {t("profile.noAccount")}{" "}
              <button className="btn btn-ghost" onClick={onFriends}>
                <Glyph name="user-plus" /> {t("friends.create")}
              </button>
            </p>
          )}
          <dl className="stats profile-stats">
            {(
              [
                [t("files.playTime"), formatDuration(played)],
                [t("files.launches"), launches],
                [t("builds.worlds"), worlds.length],
                [t("nav.friends"), friends.length],
                [t("profile.reputation"), me?.likes ?? 0],
              ] as [string, string | number][]
            ).map(([label, value]) => (
              <div key={label} className="stat">
                <dt>{label}</dt>
                <dd>{value}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      <div className="segmented tabs" role="tablist">
        {(["overview", "friends", "top"] as const).map((k) => (
          <button key={k} role="tab" aria-pressed={tab === k} aria-selected={tab === k} onClick={() => setTab(k)}>
            {k === "friends" ? `${t("nav.friends")} ${online.length}/${friends.length}` : t(k === "top" ? "profile.top" : "profile.overview")}
          </button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}

      {tab === "overview" && (
        <>
          <section className="card">
            <h2>
              {t("profile.badges")} <span className="muted">{badges.filter((b) => b.has).length}/{badges.length}</span>
            </h2>
            <div className="profile-badges">
              {badges.map((b) => (
                <div key={b.id} className="profile-badge" data-has={b.has} title={t(`badge.${b.id}.hint` as MessageKey)}>
                  <BlockIcon kind={b.block} size={40} />
                  <span className="small">{t(`badge.${b.id}` as MessageKey)}</span>
                </div>
              ))}
            </div>
          </section>
          {me && (
            <section className="card">
              <h2>{t("profile.links")}</h2>
              <ul className="list">
                {(["discord", "telegram"] as const).map((p) => (
                  <li key={p} className="list-row">
                    <div className="list-main">
                      <span className="instance-name">{p === "discord" ? "Discord" : "Telegram"}</span>
                      <span className="muted">{me.links?.[p] ?? t("profile.notLinked")}</span>
                    </div>
                    <button className={me.links?.[p] ? "btn btn-ghost" : "btn"} onClick={() => link(p)}>
                      <Glyph name={me.links?.[p] ? "x" : "external-link"} /> {t(me.links?.[p] ? "profile.unlink" : "profile.link")}
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}
          {me && (
            <section className="card">
              <h2>{t("profile.delete")}</h2>
              <p className="muted">{t("profile.deleteHint")}</p>
              <button className="btn btn-danger" onClick={remove} onBlur={() => setDeleting(false)}>
                <Glyph name="trash" /> {t(deleting ? "profile.deleteConfirm" : "profile.delete")}
              </button>
            </section>
          )}
        <div className="profile-columns">
          <section className="card">
            <h2>{t("profile.topBuilds")}</h2>
            {builds.length === 0 && <p className="muted">{t("profile.noPlay")}</p>}
            <ul className="list">
              {builds.map((inst) => (
                <li key={inst.id} className="list-row">
                  <InstanceIcon inst={inst} size={32} />
                  <div className="list-main">
                    <span className="instance-name">{inst.name}</span>
                    <span className="muted">{describe(inst, t("instance.vanilla"))}</span>
                  </div>
                  <span className="muted">{formatDuration(stats[inst.id].playSeconds)}</span>
                </li>
              ))}
            </ul>
          </section>
          <section className="card">
            <h2>{t("profile.recentWorlds")}</h2>
            {worlds.length === 0 && <Empty bee="empty-worlds">{t("worlds.empty")}</Empty>}
            <ul className="list">
              {worlds.slice(0, 5).map((w) => (
                <li key={`${w.inst.id}/${w.folder}`} className="list-row">
                  {w.icon ? <img className="profile-world-icon" src={w.icon} alt="" /> : <InstanceIcon inst={w.inst} size={32} />}
                  <div className="list-main">
                    <span className="instance-name">{w.name}</span>
                    <span className="muted">{w.inst.name}</span>
                  </div>
                  {w.lastPlayed > 0 && <span className="muted">{new Date(w.lastPlayed).toLocaleDateString()}</span>}
                </li>
              ))}
            </ul>
          </section>
        </div>
        </>
      )}
      {tab === "top" && (
        <section className="card">
          {top.length === 0 && <p className="muted">{t("profile.topEmpty")}</p>}
          <ol className="list">
            {top.map((p, i) => (
              <li key={p.id} className="list-row" data-selected={p.id === me?.id}>
                <span className="profile-rank">{i + 1}</span>
                <span className="profile-avatar small-avatar" aria-hidden>
                  {p.name[0].toUpperCase()}
                </span>
                <div className="list-main">
                  <span className="instance-name">{p.name}</span>
                  <span className="muted">{t("profile.likes", { n: p.likes })}</span>
                </div>
                {likeButton(p.id)}
              </li>
            ))}
          </ol>
        </section>
      )}
      {tab === "friends" && (
        <section className="card">
          {friends.length === 0 && <p className="muted">{t("friends.none")}</p>}
          <div className="profile-friends">
            {sorted.map((f) => (
              <div key={f.id} className="profile-friend">
                <span className="profile-avatar" aria-hidden>
                  {f.name[0].toUpperCase()}
                </span>
                <span className="instance-name">{f.name}</span>
                <span className="muted small">
                  <span className="presence-dot" data-online={social.online.has(f.id)} />{" "}
                  {social.online.has(f.id) ? t("friends.online") : t("friends.offline")}
                </span>
                {likeButton(f.id)}
              </div>
            ))}
          </div>
        </section>
      )}
    </>
  );
}
