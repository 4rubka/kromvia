require('dotenv').config();
const fs = require('fs');
const path = require('path');
const {
  Client, Events, GatewayIntentBits, ChannelType, ComponentType, PermissionFlagsBits, MessageFlags,
  ActionRowBuilder, ButtonBuilder, ButtonStyle, ModalBuilder,
  TextInputBuilder, TextInputStyle, SlashCommandBuilder,
} = require('discord.js');
const cfg = require('./config');
const kromvia = require('./kromvia');

const BADGE_ROLES = ['player', 'early', 'verified', 'liked', 'famous'];

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

const TYPES = {
  text: ChannelType.GuildText,
  voice: ChannelType.GuildVoice,
  announcement: ChannelType.GuildAnnouncement,
  stage: ChannelType.GuildStageVoice,
  forum: ChannelType.GuildForum,
};
const FALLBACK = { announcement: 'text', forum: 'text', stage: 'voice' };
const VIEW = ['ViewChannel', 'ReadMessageHistory'];
const MUTE = ['SendMessages', 'SendMessagesInThreads', 'CreatePublicThreads', 'AddReactions', 'Speak'];
const ASSETS = path.join(__dirname, '..', 'assets');
const EMOJI = {};

const norm = s => s.toLowerCase().replace(/[^a-z0-9]/g, '');
const same = (a, b) => norm(a) === norm(b);

const ephemeral = content => ({ content, flags: MessageFlags.Ephemeral });
const text = content => ({ type: ComponentType.TextDisplay, content });
const LINE = { type: ComponentType.Separator };
const withAvatar = (content, url) => ({
  type: ComponentType.Section,
  components: [text(content)],
  accessory: { type: ComponentType.Thumbnail, media: { url } },
});
const card = (guild, banner, parts, row, allowedMentions = { parse: [] }) => ({
  flags: MessageFlags.IsComponentsV2,
  allowedMentions,
  files: [{ attachment: path.join(ASSETS, `${banner}.png`), name: `${banner}.png` }],
  components: [{
    type: ComponentType.Container,
    accent_color: cfg.color,
    components: [
      { type: ComponentType.MediaGallery, items: [{ media: { url: `attachment://${banner}.png` } }] },
      ...parts,
      LINE,
      text(`-# ${EMOJI.kromvia} ${guild.name} • <t:${Math.floor(Date.now() / 1000)}:f>`),
      ...(row ? [row.toJSON()] : []),
    ],
  }],
});
const role = (guild, key) => guild.roles.cache.find(r => same(r.name, cfg.roles[key].name));
const staffRoles = guild => cfg.staff.map(k => role(guild, k)).filter(Boolean);
const findText = (guild, name) => guild.channels.cache.find(c => c.type === ChannelType.GuildText && same(c.name, name));
const findCategory = (guild, name) => guild.channels.cache.find(c => c.type === ChannelType.GuildCategory && same(c.name, name));
const isStaff = m => m.permissions.has(PermissionFlagsBits.Administrator) || staffRoles(m.guild).some(r => m.roles.cache.has(r.id));
const buttons = (...defs) => new ActionRowBuilder().addComponents(
  defs.map(([id, label, style, emoji]) => {
    const b = new ButtonBuilder().setCustomId(id).setLabel(label).setStyle(style);
    return emoji ? b.setEmoji(emoji) : b;
  }));

function overwrites(guild, R, access) {
  const everyone = guild.roles.everyone.id;
  const staff = cfg.staff.map(k => ({ id: R[k].id, allow: [...VIEW, 'SendMessages', 'Connect', 'Speak'] }));
  const muted = { id: R.muted.id, deny: MUTE };
  switch (access) {
    case 'readonly': return [{ id: everyone, allow: VIEW, deny: ['SendMessages', 'AddReactions', 'CreatePublicThreads'] }, ...staff];
    case 'members': return [{ id: everyone, deny: ['ViewChannel'] }, { id: R.member.id, allow: [...VIEW, 'Connect'] }, muted, ...staff];
    case 'members-readonly': return [{ id: everyone, deny: ['ViewChannel'] }, { id: R.member.id, allow: VIEW, deny: ['SendMessages', 'CreatePublicThreads'] }, ...staff];
    case 'staff': return [{ id: everyone, deny: ['ViewChannel'] }, ...staff];
  }
}

async function upsertRole(guild, r, log) {
  const data = { name: r.name, colors: { primaryColor: r.color }, hoist: !!r.hoist, permissions: r.permissions };
  const existing = guild.roles.cache.find(x => same(x.name, r.name));
  if (!existing) return guild.roles.create({ ...data, reason: 'setup' });
  return existing.edit(data).catch(e => {
    log.push(`⚠️ role ${existing.name}: ${e.message} (move the bot role above it)`);
    return existing;
  });
}

async function createChannel(guild, parent, ch, permissionOverwrites, log) {
  const base = { name: ch.name, parent: parent.id, permissionOverwrites, ...(ch.topic && { topic: ch.topic }) };
  try {
    return await guild.channels.create({
      ...base,
      type: TYPES[ch.type],
      ...(ch.type === 'forum' && { availableTags: (ch.tags ?? []).map(name => ({ name })) }),
    });
  } catch (e) {
    const fb = FALLBACK[ch.type];
    if (!fb) throw e;
    log.push(`⚠️ ${ch.name}: ${ch.type} needs Community enabled, created as ${fb}`);
    return guild.channels.create({ ...base, type: TYPES[fb] });
  }
}

function panelMessage(guild, type) {
  const p = cfg.panels[type];
  const [first, ...rest] = p.sections;
  return card(guild, p.banner, [
    text(`# ${EMOJI[p.icon]} ${p.title}\n${first}`),
    ...rest.map(text),
  ], p.button && buttons([`${type}:open`, p.button, ButtonStyle[p.style], EMOJI[p.icon]]));
}

async function upsertPanel(channel, type) {
  const msgs = await channel.messages.fetch({ limit: 20 });
  const old = msgs.find(m => m.author.id === client.user.id && m.components.length);
  const msg = panelMessage(channel.guild, type);
  if (old?.flags.has(MessageFlags.IsComponentsV2)) return old.edit({ ...msg, attachments: [] });
  await old?.delete();
  return channel.send(msg);
}

async function upsertEmoji(guild, log) {
  const dir = path.join(ASSETS, 'server-emoji');
  const existing = await guild.emojis.fetch();
  for (const file of fs.readdirSync(dir)) {
    const name = path.parse(file).name;
    if (existing.some(e => e.name === name)) continue;
    await guild.emojis.create({ attachment: path.join(dir, file), name })
      .catch(e => log.push(`⚠️ emoji ${name}: ${e.message}`));
  }
}

async function setup(guild) {
  const log = [];
  const R = {};
  for (const [key, r] of Object.entries(cfg.roles)) R[key] = await upsertRole(guild, r, log);
  await upsertEmoji(guild, log);

  for (const cat of cfg.categories) {
    const ow = overwrites(guild, R, cat.access);
    let parent = findCategory(guild, cat.name);
    parent = parent
      ? await parent.edit({ name: cat.name, permissionOverwrites: ow })
      : await guild.channels.create({ name: cat.name, type: ChannelType.GuildCategory, permissionOverwrites: ow });

    for (const ch of cat.channels) {
      let channel = guild.channels.cache.find(c => c.parentId === parent.id && same(c.name, ch.name));
      channel = channel
        ? await channel.edit({ name: ch.name, permissionOverwrites: ow, ...(ch.topic && { topic: ch.topic }) })
        : await createChannel(guild, parent, ch, ow, log);
      if (ch.panel) await upsertPanel(channel, ch.panel);
    }
  }
  return log;
}

function modal(id, form) {
  return new ModalBuilder().setCustomId(id).setTitle(form.title).addComponents(
    form.fields.map(f => new ActionRowBuilder().addComponents(
      new TextInputBuilder()
        .setCustomId(f.id)
        .setLabel(f.label)
        .setStyle(f.long ? TextInputStyle.Paragraph : TextInputStyle.Short)
        .setMaxLength(f.long ? 1000 : 100)
        .setRequired(f.required ?? true))));
}
const answers = (i, form) => form.fields.map(f =>
  text(`**${f.label}**\n> ${(i.fields.getTextInputValue(f.id) || '-').replace(/\n/g, '\n> ')}`));

async function loadEmoji() {
  const existing = await client.application.emojis.fetch();
  for (const file of fs.readdirSync(path.join(ASSETS, 'emoji'))) {
    const key = path.parse(file).name;
    const name = `kromvia_${key}`;
    const emoji = existing.find(e => e.name === name)
      ?? await client.application.emojis.create({ attachment: path.join(ASSETS, 'emoji', file), name });
    EMOJI[key] = emoji.toString();
  }
}

async function decide(i, userId, accepted) {
  if (!isStaff(i.member)) return i.reply(ephemeral('No permission.'));
  const member = await i.guild.members.fetch(userId).catch(() => null);
  if (accepted && member) await member.roles.add(role(i.guild, cfg.applyRole));
  await member?.send(`Your application on **${i.guild.name}** was ${accepted ? 'accepted' : 'denied'} ${EMOJI[accepted ? 'check' : 'cross']}.`).catch(() => {});
  const box = i.message.components[0].toJSON();
  await i.update({
    allowedMentions: { parse: [] },
    components: [{
      ...box,
      components: [
        ...box.components.filter(c => c.type !== ComponentType.ActionRow).map(c =>
          c.type === ComponentType.MediaGallery ? { ...c, items: [{ media: { url: 'attachment://apply.png' } }] } : c),
        text(`${EMOJI[accepted ? 'check' : 'cross']} **${accepted ? 'Accepted' : 'Denied'}** by ${i.user}`),
      ],
    }],
  });
}

const handlers = {
  'verify:open': async i => {
    await i.member.roles.add(role(i.guild, 'member'));
    await i.reply(ephemeral(`${EMOJI.check} Welcome aboard, you are verified.`));
  },

  'ticket:open': i => i.showModal(modal('ticket:submit', cfg.forms.ticket)),

  'ticket:submit': async i => {
    const g = i.guild;
    const existing = g.channels.cache.find(c => c.topic === `ticket:${i.user.id}`);
    if (existing) return i.reply(ephemeral(`You already have a ticket: ${existing}`));
    await i.deferReply({ flags: MessageFlags.Ephemeral });
    const ch = await g.channels.create({
      name: `${cfg.ticketPrefix}${i.user.username}`,
      type: ChannelType.GuildText,
      parent: findCategory(g, cfg.ticketCategory)?.id,
      topic: `ticket:${i.user.id}`,
      permissionOverwrites: [
        { id: g.roles.everyone.id, deny: ['ViewChannel'] },
        { id: i.user.id, allow: [...VIEW, 'SendMessages', 'AttachFiles'] },
        ...staffRoles(g).map(r => ({ id: r.id, allow: [...VIEW, 'SendMessages', 'AttachFiles'] })),
      ],
    });
    const support = role(g, 'support');
    await ch.send(card(g, 'ticket', [
      text(`## ${EMOJI.ticket} New ticket\nOpened by ${i.user} ${support ?? ''}`),
      ...answers(i, cfg.forms.ticket),
    ], buttons(['ticket:close', 'Close', ButtonStyle.Danger, EMOJI.lock]),
    { users: [i.user.id], roles: support ? [support.id] : [] }));
    await i.editReply(`${EMOJI.ticket} Ticket created: ${ch}`);
  },

  'ticket:close': async i => {
    if (!isStaff(i.member) && i.channel.topic !== `ticket:${i.user.id}`) return i.reply(ephemeral('No permission.'));
    await i.reply(`${EMOJI.lock} Closing in 5 seconds...`);
    await findText(i.guild, cfg.logChannel)?.send({ content: `${EMOJI.rules} \`${i.channel.name}\` closed by ${i.user}`, allowedMentions: { parse: [] } });
    setTimeout(() => i.channel.delete().catch(() => {}), 5000);
  },

  'apply:open': i => i.showModal(modal('apply:submit', cfg.forms.apply)),

  'apply:submit': async i => {
    const ch = findText(i.guild, cfg.applicationsChannel);
    if (!ch) throw new Error(`${cfg.applicationsChannel} not found, run npm run setup`);
    await ch.send(card(i.guild, 'apply', [
      withAvatar(`## ${EMOJI.pen} Application\nFrom ${i.user}`, i.user.displayAvatarURL()),
      ...answers(i, cfg.forms.apply),
    ], buttons(
      [`apply:accept:${i.user.id}`, 'Accept', ButtonStyle.Success, EMOJI.check],
      [`apply:deny:${i.user.id}`, 'Deny', ButtonStyle.Danger, EMOJI.cross])));
    await i.reply(ephemeral(`${EMOJI.pen} Application sent.`));
  },

  'apply:accept': (i, userId) => decide(i, userId, true),
  'apply:deny': (i, userId) => decide(i, userId, false),
};

const commands = [
  new SlashCommandBuilder()
    .setName('panel')
    .setDescription('Post a panel in this channel')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
    .addStringOption(o => o.setName('type').setDescription('Panel type').setRequired(true)
      .addChoices(...Object.keys(cfg.panels).map(k => ({ name: k, value: k })))),
  new SlashCommandBuilder()
    .setName('crash')
    .setDescription('Find out why Minecraft crashed')
    .addAttachmentOption(o => o.setName('log').setDescription('latest.log or the crash report'))
    .addStringOption(o => o.setName('code').setDescription('Share code of a log from the launcher')),
];

const commandHandlers = {
  crash: async i => {
    const file = i.options.getAttachment('log');
    const code = i.options.getString('code');
    if (!file && !code) return i.reply(ephemeral('Attach a log or enter a share code.'));
    await i.deferReply();
    const log = await kromvia.fetchLog({ file, code: code?.trim() });
    if (!log) throw new Error('Could not read that log.');
    const r = kromvia.analyze(log);
    const body = r
      ? [`## ${EMOJI.check} Found it`, r.fix, r.detail && `\`\`\`\n${r.detail.replace(/`/g, "'")}\n\`\`\``].filter(Boolean).join('\n')
      : `## ${EMOJI.cross} No known cause\nThe bot doesn't recognise this crash. Open a ticket in the support channel and attach the same log.`;
    await i.editReply({ content: body, allowedMentions: { parse: [] } });
  },

  panel: async i => {
    await i.channel.send(panelMessage(i.guild, i.options.getString('type')));
    await i.reply(ephemeral(`${EMOJI.check} Panel posted.`));
  },
};

client.on(Events.InteractionCreate, async i => {
  try {
    if (i.isChatInputCommand()) return await commandHandlers[i.commandName]?.(i);
    if (i.isButton() || i.isModalSubmit()) {
      const [kind, action, arg] = i.customId.split(':');
      return await handlers[`${kind}:${action}`]?.(i, arg);
    }
  } catch (e) {
    console.error(e);
    const msg = ephemeral(`${EMOJI.cross} ${e.message}`);
    await (i.deferred || i.replied ? i.followUp(msg) : i.reply(msg)).catch(() => {});
  }
});

client.on(Events.GuildCreate, g => g.commands.set(commands));

async function syncAll() {
  for (const g of client.guilds.cache.values()) {
    await kromvia.syncRoles(g, k => role(g, k), BADGE_ROLES).catch(e => console.error(`role sync ${g.name}:`, e.message));
  }
}

async function announce(rel) {
  for (const g of client.guilds.cache.values()) {
    const ch = g.channels.cache.find(c => same(c.name, cfg.announcementsChannel));
    if (!ch) continue;
    const notes = (rel.notes || '').trim().slice(0, 3000);
    const msg = await ch.send(card(g, 'rules', [
      text(`# ${EMOJI.kromvia} Kromvia ${rel.version} is out`),
      ...(notes ? [text(notes)] : []),
      text('The launcher updates itself on the next start. New here? [Download Kromvia](https://kromvia.com/download) · [Changelog](https://kromvia.com/changelog)'),
    ]));
    if (ch.type === ChannelType.GuildAnnouncement) await msg.crosspost().catch(() => {});
  }
}

client.once(Events.ClientReady, async c => {
  await loadEmoji();
  syncAll();
  setInterval(syncAll, cfg.syncMinutes * 60_000);
  const release = () => kromvia.checkRelease(announce).catch(e => console.error('release check:', e.message));
  release();
  setInterval(release, cfg.releaseMinutes * 60_000);
  for (const g of c.guilds.cache.values()) await g.commands.set(commands);
  console.log(`Logged in as ${c.user.tag}`);
  if (process.argv.includes('--setup')) {
    for (const g of c.guilds.cache.values()) {
      const log = await setup(g);
      console.log([`Setup done: ${g.name}`, ...log].join('\n'));
    }
  }
});

client.login(process.env.DISCORD_TOKEN);
