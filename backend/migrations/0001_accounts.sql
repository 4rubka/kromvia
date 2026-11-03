-- Accounts are keyed by the launcher's device key; a Minecraft UUID gets linked later.
CREATE TABLE accounts (
  id TEXT PRIMARY KEY,
  pubkey TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  name_lower TEXT NOT NULL UNIQUE,
  mc_uuid TEXT UNIQUE,
  cosmetics TEXT NOT NULL DEFAULT '{}',
  created INTEGER NOT NULL,
  seen INTEGER NOT NULL
);

CREATE TABLE friend_requests (
  from_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  to_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  created INTEGER NOT NULL,
  PRIMARY KEY (from_id, to_id)
);
CREATE INDEX friend_requests_to ON friend_requests(to_id);

-- One row per pair, smaller id first.
CREATE TABLE friendships (
  a TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  b TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  created INTEGER NOT NULL,
  PRIMARY KEY (a, b),
  CHECK (a < b)
);
CREATE INDEX friendships_b ON friendships(b);
