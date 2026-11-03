-- Which multiplayer server each online launcher's game is on, and under what nick. Badges and
-- cosmetics are only shown for a nick some Kromvia account really plays as there right now,
-- so a stranger on an offline-mode server can't wear someone else's name and look.
CREATE TABLE playing (
  account TEXT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
  host TEXT NOT NULL,
  nick TEXT NOT NULL,
  nick_lower TEXT NOT NULL,
  -- The nick is this account's own Kromvia name; such a claim wins over anyone else's.
  owned INTEGER NOT NULL,
  updated INTEGER NOT NULL
);
CREATE INDEX playing_host ON playing(host, nick_lower);
