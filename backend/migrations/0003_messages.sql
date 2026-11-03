-- Chat between friends. `a` < `b` is the pair, as in friendships, so one index serves both sides.
CREATE TABLE messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  a TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  b TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  from_id TEXT NOT NULL,
  text TEXT NOT NULL,
  created INTEGER NOT NULL
);
CREATE INDEX messages_pair ON messages(a, b, id);
