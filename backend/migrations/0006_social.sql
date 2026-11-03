CREATE TABLE parties (
  id TEXT PRIMARY KEY,
  leader TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  room TEXT,
  created INTEGER NOT NULL
);

CREATE TABLE party_members (
  account TEXT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  party TEXT NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
  joined INTEGER NOT NULL
);
CREATE INDEX party_members_party ON party_members(party, joined);

CREATE TABLE party_invites (
  party TEXT NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
  to_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  from_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  created INTEGER NOT NULL,
  PRIMARY KEY (party, to_id)
);
CREATE INDEX party_invites_to ON party_invites(to_id);

CREATE TABLE sync (
  account TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  key TEXT NOT NULL,
  data TEXT NOT NULL,
  updated INTEGER NOT NULL,
  PRIMARY KEY (account, key)
);

CREATE TABLE shares (
  code TEXT PRIMARY KEY,
  account TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  kind TEXT NOT NULL,
  data TEXT NOT NULL,
  created INTEGER NOT NULL
);
CREATE INDEX shares_account ON shares(account, created);
