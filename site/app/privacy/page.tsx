import type { Metadata } from "next";

export const metadata: Metadata = { title: "Privacy", description: "What Kromvia stores about you and why." };

export default function Privacy() {
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Privacy</h1>
        <p>What Kromvia keeps on its servers, and what never leaves your computer.</p>
      </header>
      <div className="prose">
        <h2>Stays on your computer</h2>
        <p>
          Your Microsoft and Ely.by sign-in tokens, your instances, worlds, mods, screenshots and settings. The launcher talks to Microsoft, Mojang, Modrinth and
          CurseForge to download the game and content, and those services see the requests it makes.
        </p>

        <h2>Kept on our servers</h2>
        <p>You only get a Kromvia account if you make one in the launcher. With an account, we store:</p>
        <ul>
          <li>the nickname you chose, and the public half of a key the launcher creates on your computer to sign requests;</li>
          <li>your Minecraft account ID, if you verify it;</li>
          <li>the cosmetics you wear, so other Kromvia players can see them;</li>
          <li>your friends, friend requests, parties and the chat messages you send to friends;</li>
          <li>the likes you give and get;</li>
          <li>your Discord or Telegram name and ID, if you link them;</li>
          <li>while you play on a server, its address and the nickname you use there, so your cosmetics show up for other players;</li>
          <li>builds you save to the cloud;</li>
          <li>builds, logs and screenshots you share. Shares are deleted after 30 days.</li>
        </ul>
        <p>
          Your profile at kromvia.com/u/yourname is public: it shows your nickname, cosmetics, likes, when you joined, and whether your Minecraft account is
          verified.
        </p>

        <h2>Abuse protection</h2>
        <p>
          To limit spam, we count requests and new accounts per network. For sign-ups we keep your IP network (for IPv6, the /64 block) for 24 hours. Our
          hosting provider, Cloudflare, keeps short-lived request logs.
        </p>

        <h2>Deleting your data</h2>
        <p>
          Open Profile in the launcher and press <b>Delete account</b>. That removes your nickname, friends, chats, likes, links, cloud saves and shares from our
          servers right away.
        </p>

        <h2>No ads, no selling</h2>
        <p>We don&apos;t show ads, and we don&apos;t sell or share your data with anyone for marketing.</p>
      </div>
    </div>
  );
}
