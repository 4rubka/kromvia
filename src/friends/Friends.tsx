import { useEffect, useRef, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";
import { useLauncher } from "../lib/launcher";
import { Glyph } from "../ui/Glyph";
import { Empty } from "../ui/Empty";
import { useSocial } from "./social";
import "./friends.css";

type Player = { id: string; name: string; verified: boolean };
type Me = Player & { cosmetics: Record<string, unknown> };
type FriendList = { friends: (Player & { seen: number })[]; incoming: Player[]; outgoing: Player[] };

const NICK = /^[A-Za-z0-9_]{3,16}$/;

export function Friends() {
  const { t } = useI18n();
  const l = useLauncher();
  const [me, setMe] = useState<Me | null | undefined>(undefined);
  const [list, setList] = useState<FriendList | null>(null);
  const [name, setName] = useState("");
  const [renaming, setRenaming] = useState(false);
  const [add, setAdd] = useState("");
  const [error, setError] = useState<string | null>(null);
  const social = useSocial();
  const [chat, setChat] = useState<string | null>(null);

  const load = () =>
    invoke<Me | null>("social_me").then((m) => {
      setMe(m);
      if (m) return invoke<FriendList>("social_friends").then(setList);
    }, (e) => setError(String(e)));
  useEffect(() => {
    load();
  }, []);
  useEffect(() => {
    if (me === null) setName(l.account?.name ?? l.nick.trim());
    if (me && social.myId !== me.id) social.connect(me.id);
  }, [me]);
  useEffect(() => {
    social.openChat(chat);
    return () => social.openChat(null);
  }, [chat]);

  const act = (p: Promise<unknown>) => {
    setError(null);
    return p.then(load, (e) => setError(String(e)));
  };

  if (me === undefined) return error ? <Empty bee="offline">{error}</Empty> : <p className="muted">{t("friends.loading")}</p>;

  if (me === null)
    return (
      <>
        <h1 className="page-title">{t("nav.friends")}</h1>
        <section className="card">
          <h2>{t("friends.createTitle")}</h2>
          <p className="muted">{t("friends.createHint")}</p>
          <div className="field">
            <input
              className="input"
              autoFocus
              maxLength={16}
              value={name}
              placeholder={t("home.nickname")}
              aria-label={t("home.nickname")}
              onChange={(e) => setName(e.currentTarget.value)}
              onKeyDown={(e) => e.key === "Enter" && NICK.test(name) && act(invoke("social_sign_up", { name }))}
            />
            <button className="btn" disabled={!NICK.test(name)} onClick={() => act(invoke("social_sign_up", { name }))}>
              <Glyph name="user-plus" /> {t("friends.create")}
            </button>
          </div>
          {name && !NICK.test(name) && <p className="error">{t("account.badNick")}</p>}
          {error && <p className="error">{error}</p>}
        </section>
      </>
    );

  const rename = () => act(invoke("social_rename", { name })).then(() => setRenaming(false));
  const request = () => act(invoke("social_friend_request", { name: add.trim() })).then(() => setAdd(""));

  return (
    <>
      <div className="page-head">
        <h1 className="page-title">{t("nav.friends")}</h1>
        <div className="actions">
          {renaming ? (
            <>
              <input
                className="input"
                autoFocus
                maxLength={16}
                value={name}
                aria-label={t("home.nickname")}
                onChange={(e) => setName(e.currentTarget.value)}
                onKeyDown={(e) => e.key === "Enter" && NICK.test(name) && rename()}
              />
              <button className="btn btn-ghost" onClick={() => setRenaming(false)}>
                <Glyph name="x" /> {t("common.cancel")}
              </button>
              <button className="btn" disabled={!NICK.test(name)} onClick={rename}>
                <Glyph name="check" /> {t("builds.save")}
              </button>
            </>
          ) : (
            <button className="btn btn-ghost" title={t("friends.rename")} disabled={me.verified} onClick={() => (setName(me.name), setRenaming(true))}>
              {me.name} <Glyph name="pencil" />
            </button>
          )}
        </div>
      </div>
      {error && <p className="error">{error}</p>}

      <section className="card">
        <h2>{t("friends.add")}</h2>
        <div className="field">
          <input
            className="input"
            maxLength={16}
            value={add}
            placeholder={t("friends.addPlaceholder")}
            aria-label={t("friends.addPlaceholder")}
            onChange={(e) => setAdd(e.currentTarget.value)}
            onKeyDown={(e) => e.key === "Enter" && NICK.test(add.trim()) && request()}
          />
          <button className="btn" disabled={!NICK.test(add.trim())} onClick={request}>
            <Glyph name="user-plus" /> {t("friends.send")}
          </button>
        </div>
      </section>

      <PartyCard current={l.current} onJoin={(room) => l.current && l.play(l.current, { join: room })} />

      {list && list.incoming.length > 0 && (
        <section className="card">
          <h2>{t("friends.incoming")}</h2>
          <ul className="list">
            {list.incoming.map((p) => (
              <li key={p.id} className="list-row">
                <div className="list-main">
                  <span className="instance-name">{p.name}</span>
                </div>
                <button className="btn btn-ghost" onClick={() => act(invoke("social_request_delete", { id: p.id }))}>
                  <Glyph name="x" /> {t("friends.decline")}
                </button>
                <button className="btn" onClick={() => act(invoke("social_friend_accept", { id: p.id })).then(social.refreshPresence)}>
                  <Glyph name="check" /> {t("friends.accept")}
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      {list && (
        <section className="card">
          <h2>{t("nav.friends")}</h2>
          {list.friends.length === 0 && <Empty bee="friends-alone">{t("friends.none")}</Empty>}
          <ul className="list">
            {list.friends.map((p) => (
              <li key={p.id} className="list-row" data-selected={chat === p.id}>
                <span className="presence-dot" data-online={social.online.has(p.id)} />
                <div className="list-main">
                  <span className="instance-name">{p.name}</span>
                  <span className="muted">{social.online.has(p.id) ? t("friends.online") : t("friends.offline")}</span>
                </div>
                {!!social.unread[p.id] && <span className="unread">{social.unread[p.id]}</span>}
                {social.online.has(p.id) && !social.party?.members.some((m) => m.id === p.id) && (
                  <button className="btn btn-ghost" onClick={() => social.partyActions.invite(p.id)}>
                    <Glyph name="users-group" /> {t("party.invite")}
                  </button>
                )}
                <button className="btn btn-ghost" onClick={() => act(invoke("social_friend_remove", { id: p.id })).then(() => chat === p.id && setChat(null))}>
                  <Glyph name="trash" /> {t("friends.remove")}
                </button>
                <button className="btn" onClick={() => setChat(chat === p.id ? null : p.id)}>
                  <Glyph name={chat === p.id ? "x" : "pencil"} /> {chat === p.id ? t("friends.closeChat") : t("friends.chat")}
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      {list && chat && list.friends.some((f) => f.id === chat) && (
        <Chat friend={list.friends.find((f) => f.id === chat)!} myId={me.id} />
      )}

      {list && list.outgoing.length > 0 && (
        <section className="card">
          <h2>{t("friends.outgoing")}</h2>
          <ul className="list">
            {list.outgoing.map((p) => (
              <li key={p.id} className="list-row">
                <div className="list-main">
                  <span className="instance-name">{p.name}</span>
                  <span className="muted">{t("friends.pending")}</span>
                </div>
                <button className="btn btn-ghost" onClick={() => act(invoke("social_request_delete", { id: p.id }))}>
                  <Glyph name="x" /> {t("common.cancel")}
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

function PartyCard({ current, onJoin }: { current: unknown; onJoin: (room: string) => void }) {
  const { t } = useI18n();
  const social = useSocial();
  const { party, invites, partyActions: act } = social;
  if (!party && invites.length === 0) return null;
  return (
    <section className="card">
      <h2>{t("party.title")}</h2>
      {invites.map((i) => (
        <div key={i.party} className="list-row">
          <div className="list-main">
            <span className="instance-name">{t("party.invited", { name: i.from.name })}</span>
          </div>
          <button className="btn btn-ghost" onClick={() => act.decline(i.party)}>
            <Glyph name="x" /> {t("friends.decline")}
          </button>
          <button className="btn" onClick={() => act.join(i.party)}>
            <Glyph name="check" /> {t("party.join")}
          </button>
        </div>
      ))}
      {party && (
        <>
          <ul className="list">
            {party.members.map((m) => (
              <li key={m.id} className="list-row">
                <span className="presence-dot" data-online={m.id === social.myId || social.online.has(m.id)} />
                <div className="list-main">
                  <span className="instance-name">{m.name}</span>
                  {m.id === party.leader && <span className="muted">{t("party.leader")}</span>}
                </div>
              </li>
            ))}
          </ul>
          {party.room && party.leader !== social.myId && (
            <p className="muted">
              {t("party.room", { room: party.room })}{" "}
              <button className="btn" disabled={!current} onClick={() => onJoin(party.room!)}>
                <Glyph name="login-2" /> {t("rooms.join")}
              </button>
            </p>
          )}
          <button className="btn btn-ghost" onClick={act.leave}>
            <Glyph name="logout" /> {t("party.leave")}
          </button>
        </>
      )}
      {social.error && <p className="error">{social.error}</p>}
    </section>
  );
}

function Chat({ friend, myId }: { friend: Player; myId: string }) {
  const { t } = useI18n();
  const social = useSocial();
  const [text, setText] = useState("");
  const log = useRef<HTMLDivElement>(null);
  const messages = social.chats[friend.id] ?? [];
  useEffect(() => {
    log.current?.scrollTo({ top: log.current.scrollHeight });
  }, [messages.length]);

  const send = () => {
    const body = text.trim();
    if (!body) return;
    social.send(friend.id, body);
    setText("");
  };

  return (
    <section className="card">
      <h2>{t("friends.chatWith", { name: friend.name })}</h2>
      <div className="chat-log" ref={log}>
        {messages.length === 0 && <p className="muted">{t("friends.noMessages")}</p>}
        {messages.map((m) => (
          <div key={m.id} className="chat-msg" data-mine={m.from === myId}>
            {m.text}
            <time>{new Date(m.time).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</time>
          </div>
        ))}
      </div>
      {!social.connected && <p className="muted small">{t("friends.connecting")}</p>}
      {social.error && <p className="error">{social.error}</p>}
      <div className="field">
        <input
          className="input"
          autoFocus
          maxLength={500}
          value={text}
          placeholder={t("friends.messagePlaceholder")}
          aria-label={t("friends.messagePlaceholder")}
          onChange={(e) => setText(e.currentTarget.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
        />
        <button className="btn" disabled={!social.connected || !text.trim()} onClick={send}>
          <Glyph name="check" /> {t("friends.sendMessage")}
        </button>
      </div>
    </section>
  );
}
