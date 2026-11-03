import { Database } from "bun:sqlite";
import { readFileSync, readdirSync } from "node:fs";

class Stmt {
  constructor(private db: Database, private sql: string, private args: unknown[] = []) {}
  bind(...args: unknown[]) {
    return new Stmt(this.db, this.sql, args);
  }
  async first<T>(col?: string): Promise<T | null> {
    const row = this.db.query(this.sql).get(...(this.args as never[])) as Record<string, unknown> | null;
    if (!row) return null;
    return (col ? row[col] : row) as T;
  }
  async all<T>() {
    return { results: this.db.query(this.sql).all(...(this.args as never[])) as T[] };
  }
  async run() {
    const r = this.db.query(this.sql).run(...(this.args as never[]));
    return { meta: { changes: r.changes } };
  }
}

export function testDb() {
  const db = new Database(":memory:");
  db.exec("PRAGMA foreign_keys = ON");
  const dir = new URL("../migrations/", import.meta.url);
  for (const f of readdirSync(dir).sort()) db.exec(readFileSync(new URL(f, dir), "utf8"));
  return {
    prepare: (sql: string) => new Stmt(db, sql),
    batch: async (stmts: Stmt[]) => db.transaction(() => stmts.map((s) => s["db"].query(s["sql"]).all(...(s["args"] as never[]))))().map((results) => ({ results })),
  } as unknown as D1Database;
}
