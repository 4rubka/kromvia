export const MODULES: Record<"PvP" | "HUD" | "Utility & visuals", string[]> = {
  PvP: [
    "1.7 Animations", "Armor Status", "Armour Break Alert", "Attack Indicator", "Bow Charge", "Combo Counter", "CPS",
    "Crosshair", "Damage Tint by Type", "Durability Alert", "Freelook", "Hit Bubbles", "Hit Colour", "Hit Particles",
    "Hit Sound", "Item Counter", "Keystrokes", "Low HP Warning", "Potion Effects", "Reach Display", "Stretched 4:3",
    "Target HUD", "Target Marker", "Totem Pop Counter", "Visible Damage",
  ],
  HUD: [
    "Biome", "Clock", "Compass", "Coordinates", "Day Counter", "Detailed Armour Bar", "Effect Bars", "Effect Timers",
    "Food & Saturation", "FPS", "Light Level", "Memory", "Music", "Pack Display", "Ping", "Server Address", "Server TPS",
    "Session Time", "Speed",
  ],
  "Utility & visuals": [
    "Ambience", "Behind You", "Better Chat", "Block Outline", "Bobbing", "Camera", "Chat Filter", "Chat History",
    "Chat Macros", "Chat Timestamps", "Clear Weather", "Confirm Disconnect", "Custom FOV", "Death Coordinates",
    "Gamma per Dimension", "Hand Position", "Hand Sway", "Hide Scoreboard", "Inventory Scale", "IRL Weather",
    "Jump Circles", "Low Fire", "Motion Blur", "Nametags", "No Fog", "No Hurt Cam", "No Pumpkin Blur",
    "Numeric Tab Ping", "Particles", "Screen Colours", "Sound Volumes", "Time Changer", "Toggle Sneak", "Trails",
  ],
};

export const MODULE_COUNT = Object.values(MODULES).reduce((n, l) => n + l.length, 0);

export type Card = { bee: string; title: string; text: string };

export const LAUNCHER: Card[] = [
  { bee: "card-versions", title: "Any version, any loader", text: "Releases, snapshots, beta and alpha. Fabric, Quilt, Forge, NeoForge, Legacy Fabric and Ornithe, and the launcher downloads the right Java for each one." },
  { bee: "card-tuned", title: "Tuned out of the box", text: "The Max FPS, Balance and Quality presets install Sodium and similar mods from Modrinth and size the JVM memory to your RAM." },
  { bee: "card-mods", title: "Modrinth and CurseForge", text: "Search mods, resource packs, shaders and datapacks for your exact version. Kromvia installs the dependencies for you." },
  { bee: "card-safe", title: "Safe mods", text: "Kromvia checks every file against its hash. Anything that matches known malware goes to quarantine before the game starts." },
  { bee: "card-import", title: "Bring your instances", text: "Import from Prism, MultiMC, CurseForge App, Modrinth App, ATLauncher, GDLauncher or the official launcher." },
  { bee: "card-worlds", title: "Worlds, servers, screenshots", text: "Back up and copy worlds, ping and pin servers, and browse screenshots. When the game crashes, the analyser suggests a fix you can apply in one click." },
];

export const CLIENT: Card[] = [
  { bee: "card-menu", title: "Right Shift menu", text: "One menu for every module. Drag HUD elements around, change colours and switch modules on or off. It comes with every instance." },
  { bee: "card-pvp", title: "PvP that stays fair", text: "Keystrokes, CPS, combo and reach displays, hit colours. There is no reach hack, killaura or X-ray, so the client won't get you banned." },
  { bee: "card-cosmetics", title: "Cosmetics", text: "Capes, wings, hats and more. Other Kromvia players on your server see them too." },
  { bee: "card-visuals", title: "Visuals", text: "Motion blur, trails, jump circles, your own crosshair, fog off, and control over time and weather." },
];

export const SOCIAL: Card[] = [
  { bee: "card-rooms", title: "Rooms", text: "Open your singleplayer world to friends with a code. You don't need a server or port forwarding." },
  { bee: "card-friends", title: "Friends and chat", text: "See who's online and what they're playing, chat with them, and join their party in one click." },
  { bee: "card-profile", title: "Profile and badges", text: "Your skin, cosmetics, likes and badges, on a public page at kromvia.com/u/yourname." },
];

export const VERSIONS = "1.15.2 to 26.3";

export const FAQ: { q: string; a: string }[] = [
  { q: "Is Kromvia free?", a: "Yes. The launcher, the built-in client and the default cosmetics are free." },
  { q: "Is the built-in client allowed on servers?", a: "It only has modules that don't give you an unfair edge: HUD, visuals and quality-of-life tweaks. There is no reach hack, killaura or X-ray." },
  { q: "Which versions does the client support?", a: `The built-in client runs on Fabric for Minecraft ${VERSIONS}. The launcher itself starts any version, from alpha to the latest snapshot, on any loader.` },
  { q: "Do I need a Minecraft account?", a: "You need one for online servers, and you sign in with Microsoft. Without an account you can still play offline." },
  { q: "Does it touch my existing .minecraft?", a: "No. Kromvia keeps its own folder and can import your instances from other launchers." },
];

export const MORE_FAQ: { q: string; a: string }[] = [
  { q: "Why doesn't Right Shift open the menu?", a: "The Kromvia client only runs on Fabric instances. On Forge, NeoForge, Quilt or vanilla the game starts without it." },
  { q: "Who can see my cosmetics?", a: "Other Kromvia players on the same server as you. Players on other launchers see a normal skin." },
  { q: "Can I play with a friend who doesn't have Kromvia?", a: "On a regular server, yes. Rooms need Kromvia on both sides, because the launcher relays the connection." },
  { q: "Does Kromvia make Minecraft faster?", a: "The Max FPS preset installs Sodium, Lithium, FerriteCore and a few more performance mods from Modrinth, and sizes Java memory to your RAM. How much it helps depends on your PC." },
  { q: "Where are my worlds and screenshots?", a: "Each instance has its own folder inside Kromvia's game folder. You can move the whole game folder in Settings." },
  { q: "How do I delete my Kromvia account?", a: "Open Profile in the launcher and press Delete account. Your nickname, friends, chats, likes, links, cloud saves and shares are removed from our servers." },
];
