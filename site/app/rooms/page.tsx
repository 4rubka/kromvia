import type { Metadata } from "next";
import Link from "next/link";
import s from "./rooms.module.css";

export const metadata: Metadata = { title: "Rooms", description: "Play your singleplayer Minecraft world with friends through a code, without a server." };

const HOST = [
  "Open Rooms in the launcher and press Host a room. The game starts with the relay mod added.",
  "Open any singleplayer world. The room opens by itself.",
  "A code appears in the launcher. Send it to your friends.",
];

const JOIN = [
  "Open Rooms in the launcher.",
  "Paste the code your friend sent and press Join.",
  "You land in their world. Your instance has to be on the same Minecraft version as theirs.",
];

export default function Rooms() {
  return (
    <div className="wrap">
      <header className={`page-head ${s.head}`}>
        <div>
          <h1>Rooms</h1>
          <p>Play your singleplayer world with friends. Nobody has to run a server or open ports on their router: Kromvia relays the connection.</p>
        </div>
        <img src="/bee/card-rooms.webp" alt="" className={s.bee} />
      </header>

      <div className={s.cols}>
        <section className={`block ${s.col}`}>
          <h2>Host your world</h2>
          <ol>
            {HOST.map((t) => (
              <li key={t}>{t}</li>
            ))}
          </ol>
        </section>
        <section className={`block ${s.col}`}>
          <h2>Join a friend</h2>
          <ol>
            {JOIN.map((t) => (
              <li key={t}>{t}</li>
            ))}
          </ol>
        </section>
      </div>

      <div className="prose">
        <h2>Good to know</h2>
        <ul>
          <li>Rooms need an instance with a mod loader. Fabric works best.</li>
          <li>The world stays on the host&apos;s computer. When the host closes the game, the room closes.</li>
          <li>Friends in your Kromvia party can join from the friends list without typing a code.</li>
        </ul>
        <p>
          Don&apos;t have Kromvia yet? <Link href="/download">Download it here</Link>.
        </p>
      </div>
    </div>
  );
}
