module.exports = {
  color: 0x8b5cf6,
  ticketCategory: '🎫 TICKETS',
  ticketPrefix: '🎫┃ticket-',
  logChannel: '📋┃ticket-logs',
  announcementsChannel: '📢┃announcements',
  syncMinutes: 3,
  releaseMinutes: 10,
  applicationsChannel: '📨┃applications',
  applyRole: 'support',
  staff: ['admin', 'mod', 'support'],

  roles: {
    admin: { name: '🌌 Admin', color: 0xa855f7, hoist: true, permissions: ['Administrator'] },
    mod: { name: '🔨 Moderator', color: 0x8b5cf6, hoist: true, permissions: ['KickMembers', 'BanMembers', 'ModerateMembers', 'ManageMessages', 'ManageThreads', 'MuteMembers', 'MoveMembers'] },
    support: { name: '🔧 Support', color: 0x6366f1, hoist: true, permissions: ['ManageMessages'] },
    member: { name: '💜 Member', color: 0xc4b5fd, permissions: [] },
    muted: { name: '🔇 Muted', color: 0x3b3552, permissions: [] },
    player: { name: '🎮 Player', color: 0x8b5cf6, hoist: true, permissions: [] },
    early: { name: '🐣 Early bird', color: 0xfbbf24, permissions: [] },
    verified: { name: '✔️ Verified', color: 0x22c55e, permissions: [] },
    liked: { name: '💗 Liked', color: 0xf472b6, permissions: [] },
    famous: { name: '⭐ Famous', color: 0xfacc15, permissions: [] },
  },

  categories: [
    { name: '🔮 INFO', access: 'readonly', channels: [
      { name: '📜┃rules', type: 'text', panel: 'rules' },
      { name: '📢┃announcements', type: 'announcement' },
      { name: '✅┃verify', type: 'text', panel: 'verify' },
    ] },
    { name: '🌐 COMMUNITY', access: 'members', channels: [
      { name: '💬┃general', type: 'text', topic: 'General chat' },
      { name: '🎬┃media', type: 'text' },
      { name: '💡┃ideas', type: 'forum', tags: ['Idea', 'Bug', 'Question'] },
    ] },
    { name: '🆘 SUPPORT', access: 'members-readonly', channels: [
      { name: '📩┃create-ticket', type: 'text', panel: 'ticket' },
      { name: '🩺┃crash-help', type: 'text', panel: 'crash' },
      { name: '📝┃apply', type: 'text', panel: 'apply' },
    ] },
    { name: '🎧 VOICE', access: 'members', channels: [
      { name: '🔊┃General', type: 'voice' },
      { name: '🎮┃Gaming', type: 'voice' },
      { name: '🎤┃Events', type: 'stage' },
    ] },
    { name: '💠 STAFF', access: 'staff', channels: [
      { name: '💼┃staff-chat', type: 'text' },
      { name: '📨┃applications', type: 'text' },
      { name: '📋┃ticket-logs', type: 'text' },
      { name: '🔒┃Staff Voice', type: 'voice' },
    ] },
    { name: '🎫 TICKETS', access: 'staff', channels: [] },
  ],

  panels: {
    rules: { banner: 'rules', icon: 'rules', title: 'Welcome to Kromvia', sections: [
      'Minecraft launcher with a built-in client. By being here you agree to the rules below.',
      '### Rules\n**1.** Be respectful. No harassment, hate speech or discrimination.\n**2.** No spam, flooding or mass mentions.\n**3.** No advertising other servers or projects.\n**4.** No NSFW, gore or shock content, including avatars and names.\n**5.** Keep topics in the right channel and don\'t impersonate staff.',
      '-# Press **Verify** in the verify channel to unlock the server.',
    ] },
    verify: { banner: 'verify', icon: 'shield', title: 'Verification', sections: [
      'Press the button below to unlock the server.',
      '### Kromvia account\nLink Discord in the launcher (**Profile → Links → Discord**) to get the **Player** role and a role for every badge you earn. Roles update within a few minutes.',
    ], button: 'Verify', style: 'Success' },
    crash: { banner: 'support', icon: 'rules', title: 'Game crashed?', sections: [
      'Use **/crash** anywhere on the server and attach `latest.log` or the crash report, or enter the share code from the launcher (**Share log**). The bot reads it and tells you what broke.',
      '-# latest.log is in the build folder → `logs`. Crash reports are in `crash-reports`.',
    ] },
    ticket: { banner: 'support', icon: 'ticket', title: 'Support', sections: ['Need help? Open a ticket and the team will answer you.'], button: 'Open ticket', style: 'Primary' },
    apply: { banner: 'apply', icon: 'pen', title: 'Staff application', sections: ['Want to join the team? Fill out the form.'], button: 'Apply', style: 'Secondary' },
  },

  forms: {
    ticket: { title: 'New ticket', fields: [
      { id: 'subject', label: 'Subject' },
      { id: 'details', label: 'Describe the problem', long: true },
    ] },
    apply: { title: 'Staff application', fields: [
      { id: 'age', label: 'Age' },
      { id: 'timezone', label: 'Timezone' },
      { id: 'experience', label: 'Moderation experience', long: true },
      { id: 'why', label: 'Why do you want to join?', long: true },
    ] },
  },
};
