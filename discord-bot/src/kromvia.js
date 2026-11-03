const fs = require('fs');
const path = require('path');

const API = process.env.KROMVIA_API || 'https://api.kromvia.com';
const DATA = path.join(__dirname, '..', 'data');

function load(file, fallback) {
  try {
    return JSON.parse(fs.readFileSync(path.join(DATA, file), 'utf8'));
  } catch {
    return fallback;
  }
}

function save(file, value) {
  fs.mkdirSync(DATA, { recursive: true });
  fs.writeFileSync(path.join(DATA, file), JSON.stringify(value));
}

async function linkedAccounts() {
  const key = process.env.BOT_API_KEY;
  if (!key) return null;
  const res = await fetch(`${API}/bot/links`, { headers: { authorization: `Bearer ${key}` } });
  if (!res.ok) throw new Error(`/bot/links: HTTP ${res.status}`);
  return res.json();
}

// Gives linked members the player role plus one role per badge, and takes them away again
// after an unlink. The ids we touched are kept on disk, so no member list intent is needed.
async function syncRoles(guild, roleOf, keys) {
  const links = await linkedAccounts();
  if (!links) return;
  const managed = keys.map(roleOf).filter(Boolean);
  const want = new Map(links.map(l => [l.discord, new Set(['player', ...l.badges].map(roleOf).filter(Boolean).map(r => r.id))]));
  const seen = load(`linked-${guild.id}.json`, []);
  for (const id of new Set([...seen, ...want.keys()])) {
    const member = await guild.members.fetch(id).catch(() => null);
    if (!member) continue;
    const ids = want.get(id) ?? new Set();
    const add = managed.filter(r => ids.has(r.id) && !member.roles.cache.has(r.id));
    const remove = managed.filter(r => !ids.has(r.id) && member.roles.cache.has(r.id));
    if (add.length) await member.roles.add(add, 'Kromvia account link');
    if (remove.length) await member.roles.remove(remove, 'Kromvia account link');
  }
  save(`linked-${guild.id}.json`, [...want.keys()]);
}

// Same rules as the launcher's crash analyser (src-tauri/src/engine/game/crash.rs).
const RULES = [
  { cause: 'tooMuchMemory', any: ['Could not reserve enough space for', 'Invalid maximum heap size', 'Initial heap size set to a larger value'],
    fix: 'Java could not get that much memory. Set memory back to **Auto** in the build settings, or lower it.' },
  { cause: 'outOfMemory', any: ['java.lang.OutOfMemoryError'],
    fix: 'The game ran out of memory. Give the build more memory in its settings (or press the fix button in the launcher).' },
  { cause: 'wrongJava', any: ['UnsupportedClassVersionError', 'compiled by a more recent version of the Java Runtime'], line: 'class file version',
    fix: 'The game or a mod needs a newer Java. Set Java back to **Automatic** in the build settings.' },
  { cause: 'duplicateMod', any: ['Found duplicate mods', 'DuplicateModsFoundException', 'Duplicate mods found'], line: 'Mod ID',
    fix: 'The same mod is installed twice. Open the mods folder and delete the older copy.' },
  { cause: 'incompatible', any: ['Incompatible mods found', 'is incompatible with'], line: 'incompatible',
    fix: 'Two mods don\'t work together. Remove or update one of them.' },
  { cause: 'missingMod', any: ['which is missing', 'Missing or unsupported mandatory dependencies'], line: 'missing',
    fix: 'A mod needs another mod (or a different version of it). Install what the line above names, or update the mod.' },
  { cause: 'brokenMod', any: ['Mixin apply failed', 'MixinApplyError', 'InvalidInjectionException', 'InjectionError', 'MixinTransformerError'],
    fix: 'A mod failed to load, probably made for another Minecraft version. Update or remove it.' },
  { cause: 'driver', any: ['Pixel format not accelerated', 'GLFW error 65542', 'GLFW error 65543', 'does not appear to support OpenGL', 'Failed to create OpenGL context', 'No OpenGL context'], line: 'GLFW error',
    fix: 'No working OpenGL. Update your graphics drivers (on laptops, make sure the game uses the real GPU).' },
];

function analyze(log) {
  const lines = log.split('\n');
  const find = needle => lines.find(l => l.toLowerCase().includes(needle.toLowerCase()))?.trim().slice(0, 300);
  const rule = RULES.find(r => r.any.some(n => log.includes(n)));
  if (!rule) return null;
  let detail = rule.line ? find(rule.line) : null;
  if (rule.cause === 'brokenMod') {
    const m = log.match(/(?:from|for) mod ([\w.-]+)/);
    detail = m ? `Mod: ${m[1]}` : null;
  }
  return { ...rule, detail };
}

async function fetchLog({ code, file }) {
  if (file) {
    if (file.size > 8 * 1024 * 1024) throw new Error('That file is too big. Send latest.log or the crash report.');
    const res = await fetch(file.url);
    return res.ok ? res.text() : null;
  }
  if (!/^[A-Za-z0-9]{4,16}$/.test(code)) throw new Error('That doesn\'t look like a share code.');
  const res = await fetch(`${API}/share/${code}`);
  if (!res.ok || !(res.headers.get('content-type') ?? '').startsWith('text/plain')) throw new Error('No shared log with that code, or it expired.');
  return res.text();
}

async function latestRelease() {
  const res = await fetch(`${API}/download`);
  return res.ok ? res.json() : null;
}

// Posts each new launcher release once. The first run only remembers the current version.
async function checkRelease(post) {
  const rel = await latestRelease();
  if (!rel?.version) return;
  const last = load('release.json', null);
  if (last?.version === rel.version) return;
  save('release.json', { version: rel.version });
  if (last) await post(rel);
}

module.exports = { syncRoles, analyze, fetchLog, checkRelease };
