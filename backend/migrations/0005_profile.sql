CREATE TABLE likes (
  from_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  to_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  created INTEGER NOT NULL,
  PRIMARY KEY (from_id, to_id)
);
CREATE INDEX likes_to ON likes(to_id);

CREATE TABLE links (
  account TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  ext_id TEXT NOT NULL,
  name TEXT NOT NULL,
  created INTEGER NOT NULL,
  PRIMARY KEY (account, provider),
  UNIQUE (provider, ext_id)
);

CREATE TABLE link_codes (
  code TEXT PRIMARY KEY,
  account TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  created INTEGER NOT NULL
);
