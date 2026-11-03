-- Game crashes the launcher sent with the player's consent. Logs are scrubbed of the player's
-- name, uuid, home folder and tokens on the device; net is a hash, never the raw address.
CREATE TABLE crashes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  created INTEGER NOT NULL,
  net TEXT NOT NULL,
  launcher TEXT NOT NULL,
  os TEXT NOT NULL,
  mc TEXT,
  loader TEXT,
  cause TEXT NOT NULL,
  sig TEXT NOT NULL,
  detail TEXT,
  mods TEXT NOT NULL,
  log TEXT NOT NULL,
  report TEXT NOT NULL
);
CREATE INDEX crashes_created ON crashes(created);
CREATE INDEX crashes_net ON crashes(net, created);
CREATE INDEX crashes_sig ON crashes(sig, created);
