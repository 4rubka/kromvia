module.exports = {
  color: 0x8b5cf6,
  ticketCategory: '🎫 TICKETS',
  ticketPrefix: '🎫┃ticket-',
  logChannel: '📋┃ticket-logs',
  applicationsChannel: '📨┃applications',
  applyRole: 'support',
  staff: ['admin', 'mod', 'support'],

  roles: {
    admin: { name: '🌌 Admin', color: 0xa855f7, hoist: true, permissions: ['Administrator'] },
    mod: { name: '🔨 Moderator', color: 0x8b5cf6, hoist: true, permissions: ['KickMembers', 'BanMembers', 'ModerateMembers', 'ManageMessages', 'ManageThreads', 'MuteMembers', 'MoveMembers'] },
    support: { name: '🔧 Support', color: 0x6366f1, hoist: true, permissions: ['ManageMessages'] },
    member: { name: '💜 Member', color: 0xc4b5fd, permissions: [] },
    muted: { name: '🔇 Muted', color: 0x3b3552, permissions: [] },
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
    verify: { banner: 'verify', icon: 'shield', title: 'Verification', sections: ['Press the button below to unlock the server.'], button: 'Verify', style: 'Success' },
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
