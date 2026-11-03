import type { Metadata } from "next";

export const metadata: Metadata = { title: "Terms", description: "The rules for using Kromvia." };

export default function Terms() {
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>Terms of use</h1>
        <p>The short version: play fair, be decent to people, and don&apos;t try to break the service.</p>
      </header>
      <div className="prose">
        <h2>Minecraft</h2>
        <p>
          Kromvia is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft. To play online you need your own Minecraft
          account, and the Minecraft EULA applies to how you use the game.
        </p>

        <h2>Your account</h2>
        <ul>
          <li>Pick a nickname that doesn&apos;t pretend to be someone else and isn&apos;t offensive.</li>
          <li>Don&apos;t spam, harass or threaten people in chat, and don&apos;t share anything illegal through share codes or screenshots.</li>
          <li>Don&apos;t try to get around rate limits, read other people&apos;s data, or make accounts in bulk.</li>
        </ul>
        <p>If you break these rules we can remove your nickname, shares or the whole account.</p>

        <h2>Mods and content</h2>
        <p>
          Mods, resource packs and modpacks come from Modrinth and CurseForge and belong to their authors under their own licences. Kromvia downloads them for you
          but doesn&apos;t own them. Servers have their own rules: check them before you use any client feature there.
        </p>

        <h2>No warranty</h2>
        <p>
          Kromvia is provided as is. We work hard to keep your worlds and files safe, but back up anything you can&apos;t lose. The launcher has backups built
          in.
        </p>

        <h2>Changes</h2>
        <p>If these terms change, the new version goes on this page before it applies.</p>
      </div>
    </div>
  );
}
