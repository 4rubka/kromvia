-- One row per account created, by client network, to cap sign-ups (and name squatting) per day.
CREATE TABLE signups (
  net TEXT NOT NULL,
  created INTEGER NOT NULL
);
CREATE INDEX signups_net ON signups(net, created);
