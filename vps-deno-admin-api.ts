/**
 * GitaAI — Secure Admin API (Deno) → VPS PostgreSQL
 * ─────────────────────────────────────────────────
 * Secrets live ONLY in Deno Deploy env vars. Never put them in GitHub Pages HTML.
 *
 * Required env:
 *   DATABASE_URL       postgres://user:pass@162.35.96.65:5432/coin_db
 *   ADMIN_SECRET_KEY   long random string (send as X-Admin-Key header)
 *
 * Optional env:
 *   TOTP_SECRET        base32 secret for Google Authenticator
 *   ADMIN_PIN          simple backup PIN (optional; prefer TOTP + ADMIN_SECRET_KEY)
 *   ALLOWED_ORIGINS    comma-separated, e.g. https://sravanku018.github.io
 *
 * Deploy (Deno Deploy — lofty-crocodile or new project):
 *   1. Paste this file as main
 *   2. Set DATABASE_URL, ADMIN_SECRET_KEY, TOTP_SECRET
 *   3. Redeploy
 *
 * Local:
 *   export DATABASE_URL=... ADMIN_SECRET_KEY=...
 *   deno run -A deno-admin-api.ts
 */

import { Hono } from "https://deno.land/x/hono@v4.3.11/mod.ts";
import { cors } from "https://deno.land/x/hono@v4.3.11/middleware.ts";
import pg from "npm:pg@8.11.5";

const { Pool } = pg;

// ─── ENV ─────────────────────────────────────────────────────
const DATABASE_URL = Deno.env.get("DATABASE_URL") ?? "";
const ADMIN_SECRET_KEY = Deno.env.get("ADMIN_SECRET_KEY") ?? "";
const TOTP_SECRET = (Deno.env.get("TOTP_SECRET") ?? "").replace(/\s+/g, "").toUpperCase();
const ADMIN_PIN = Deno.env.get("ADMIN_PIN") ?? "";

function parseAllowedOrigins(raw: string | undefined): string[] {
  const cleaned = (raw ?? "*")
    .split(",")
    .map((s) => s.trim())
    .map((s) => s.replace(/^ALLOWED_ORIGINS\s*=\s*/i, "").trim())
    .filter(Boolean);
  const defaults = [
    "https://sravanku018.github.io",
    "http://localhost:8000",
    "http://127.0.0.1:8000",
  ];
  return [...new Set([...cleaned, ...defaults])];
}

const ALLOWED_ORIGINS = parseAllowedOrigins(Deno.env.get("ALLOWED_ORIGINS"));

if (!DATABASE_URL) {
  console.error("FATAL: set DATABASE_URL (postgres://…@host:5432/coin_db)");
}
if (!ADMIN_SECRET_KEY) {
  console.error("Missing ADMIN_SECRET_KEY env — all admin routes will reject");
}
console.log("CORS allowed origins:", ALLOWED_ORIGINS.join(", "));
console.log("DB:", DATABASE_URL ? DATABASE_URL.replace(/:([^:@/]+)@/, ":***@") : "(empty)");

// ─── PostgreSQL (SQLite/Turso-compatible execute adapter) ────
const pool = new Pool({
  connectionString: DATABASE_URL || undefined,
  max: 8,
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 15_000,
});

/** Convert dashboard/Turso SQL quirks → Postgres */
function toPgSql(sql: string): { sql: string; hadIgnore: boolean } {
  let pgSql = sql
    .replace(/datetime\('now'\)/gi, "CURRENT_TIMESTAMP")
    .replace(/datetime\("now"\)/gi, "CURRENT_TIMESTAMP")
    .replace(/INSERT OR IGNORE INTO/gi, "INSERT INTO")
    .replace(/INSERT OR REPLACE INTO/gi, "INSERT INTO")
    // SQLite ON CONFLICT(col) → ON CONFLICT (col)
    .replace(/ON CONFLICT\s*\(/gi, "ON CONFLICT (")
    .replace(/ON CONFLICT\(([a-zA-Z0-9_]+)\)/gi, "ON CONFLICT ($1)");

  const hadIgnore = /INSERT\s+OR\s+IGNORE/i.test(sql);
  if (hadIgnore && !/ON CONFLICT/i.test(pgSql)) {
    pgSql = pgSql.trim().replace(/;\s*$/, "") + " ON CONFLICT DO NOTHING";
  }

  // ? placeholders → $1, $2, ...
  let paramIndex = 1;
  pgSql = pgSql.replace(/\?/g, () => `$${paramIndex++}`);

  return { sql: pgSql, hadIgnore };
}

async function pgExecute(
  sqlOrOpts: string | { sql: string; args?: unknown[] },
  maybeArgs?: unknown[],
) {
  let sql: string;
  let args: unknown[] = [];
  if (typeof sqlOrOpts === "string") {
    sql = sqlOrOpts;
    args = Array.isArray(maybeArgs) ? maybeArgs : [];
  } else {
    sql = sqlOrOpts.sql;
    args = sqlOrOpts.args ?? [];
  }

  const { sql: pgSql, hadIgnore } = toPgSql(sql);

  try {
    const res = await pool.query(pgSql, args);
    return {
      rows: res.rows ?? [],
      rowsAffected: res.rowCount ?? 0,
      rowCount: res.rowCount ?? 0,
    };
  } catch (err: unknown) {
    const e = err as { code?: string; message?: string };
    if (hadIgnore && e?.code === "23505") {
      return { rows: [], rowsAffected: 0, rowCount: 0 };
    }
    console.error("SQL error:", e?.message, "\nSQL:", pgSql.slice(0, 240));
    throw err;
  }
}

const db = { execute: pgExecute };

async function assertDb() {
  try {
    await pool.query("SELECT 1");
    console.log("PostgreSQL connected (VPS coin_db)");
  } catch (e) {
    console.error("PostgreSQL connection failed:", e);
  }
}
await assertDb();

const app = new Hono();

app.use(
  "*",
  cors({
    origin: (origin) => {
      if (!origin) return ALLOWED_ORIGINS[0] ?? "*";
      if (ALLOWED_ORIGINS.includes("*")) return origin;
      if (ALLOWED_ORIGINS.includes(origin)) return origin;
      if (origin.endsWith(".github.io") || origin.includes("github.io")) return origin;
      return ALLOWED_ORIGINS.find((o) => o.startsWith("http")) ?? "https://sravanku018.github.io";
    },
    allowHeaders: ["Content-Type", "Authorization", "X-Admin-Key"],
    allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    maxAge: 86400,
  }),
);

// ─── TOTP (RFC 6238, SHA-1, 30s, 6 digits) ───────────────────
function base32ToBytes(base32: string): Uint8Array {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  let bits = 0;
  let value = 0;
  const output: number[] = [];
  for (const ch of base32.replace(/=+$/, "")) {
    const idx = alphabet.indexOf(ch.toUpperCase());
    if (idx === -1) continue;
    value = (value << 5) | idx;
    bits += 5;
    if (bits >= 8) {
      output.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return new Uint8Array(output);
}

async function generateTOTP(secretBase32: string, timeOffset = 0): Promise<string> {
  const epoch = Math.floor(Date.now() / 1000 / 30) + timeOffset;
  const timeBytes = new Uint8Array(8);
  let temp = epoch;
  for (let i = 7; i >= 0; i--, temp = Math.floor(temp / 256)) {
    timeBytes[i] = temp & 0xff;
  }
  const keyBytes = base32ToBytes(secretBase32);
  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    keyBytes,
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(await crypto.subtle.sign("HMAC", cryptoKey, timeBytes));
  const offset = signature[signature.length - 1] & 0xf;
  const code =
    ((signature[offset] & 0x7f) << 24) |
    ((signature[offset + 1] & 0xff) << 16) |
    ((signature[offset + 2] & 0xff) << 8) |
    (signature[offset + 3] & 0xff);
  return (code % 1_000_000).toString().padStart(6, "0");
}

async function verifyTotp(inputCode: string): Promise<boolean> {
  if (!TOTP_SECRET || !/^\d{6}$/.test(inputCode.trim())) return false;
  for (const off of [0, -1, 1]) {
    try {
      if ((await generateTOTP(TOTP_SECRET, off)) === inputCode.trim()) return true;
    } catch {
      /* ignore */
    }
  }
  return false;
}

// ─── AUTH HELPERS ────────────────────────────────────────────
function requireAdminKey(c: { req: { header: (n: string) => string | undefined } }): boolean {
  if (!ADMIN_SECRET_KEY) return false;
  const key =
    c.req.header("X-Admin-Key") ||
    c.req.header("x-admin-key") ||
    c.req.header("Authorization")?.replace(/^Bearer\s+/i, "");
  return !!key && key === ADMIN_SECRET_KEY;
}

function sqlEscape(s: string): string {
  return String(s ?? "").replace(/'/g, "''");
}

function normalizeDate(raw: string | null | undefined): string | null {
  const s = String(raw || "").trim();
  if (!s) return null;
  const m = s.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})/);
  if (m) return `${m[1]} ${m[2]}`;
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return `${s} 12:00:00`;
  return s;
}

// ─── PUBLIC ──────────────────────────────────────────────────
app.get("/", (c) =>
  c.json({
    status: "GitaAI Admin API ✅",
    database: "PostgreSQL (VPS)",
    secrets: "env-only",
    db_configured: !!DATABASE_URL,
    totp_configured: !!TOTP_SECRET,
    admin_key_configured: !!ADMIN_SECRET_KEY,
    endpoints: [
      "POST /admin/login",
      "GET  /admin/users",
      "GET  /admin/coin-history",
      "GET  /admin/coin-history/:id",
      "POST /admin/coin-history",
      "PUT  /admin/coin-history/:id",
      "DELETE /admin/coin-history/:id",
      "POST /admin/sql  (read-only SELECT)",
      "POST /admin/query (dashboard SQL proxy)",
    ],
  }),
);

app.post("/admin/login", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const pin = String(body.pin ?? body.password ?? "").trim();
  const totp = String(body.totp ?? body.code ?? "").trim();
  const adminKey = String(body.admin_key ?? body.adminKey ?? "").trim();

  if (ADMIN_SECRET_KEY && adminKey === ADMIN_SECRET_KEY) {
    if (TOTP_SECRET) {
      const totpOk = await verifyTotp(totp);
      if (!totpOk) return c.json({ error: "Invalid or missing 6-digit TOTP code" }, 401);
    }
    return c.json({
      success: true,
      message: "Admin authenticated",
      admin_key: ADMIN_SECRET_KEY,
      totp_required: !!TOTP_SECRET,
    });
  }

  if (ADMIN_PIN && pin === ADMIN_PIN && ADMIN_SECRET_KEY) {
    if (TOTP_SECRET) {
      const totpOk = await verifyTotp(totp);
      if (!totpOk) return c.json({ error: "Invalid or missing 6-digit TOTP code" }, 401);
    }
    return c.json({
      success: true,
      message: "Admin authenticated via PIN",
      admin_key: ADMIN_SECRET_KEY,
      totp_required: !!TOTP_SECRET,
    });
  }

  return c.json({
    error: "Invalid Admin Secret Key (does not match Deno ADMIN_SECRET_KEY)",
    hint: "TOTP is only checked after the admin key matches. Paste the full ADMIN_SECRET_KEY from Deno env.",
  }, 401);
});

// ─── ADMIN: USERS ────────────────────────────────────────────
app.get("/admin/users", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden — set X-Admin-Key" }, 403);
  const limit = Math.min(Math.max(parseInt(c.req.query("limit") ?? "100", 10) || 100, 1), 500);
  try {
    const result = await db.execute({
      sql: `
        SELECT u.user_id, u.name, u.email, u.is_guest, u.created_at,
               us.current_streak, us.longest_streak, us.days_active, us.last_activity_date,
               us.krishna_coins, us.yoga_level,
               cs.current_day as checkin_day, cs.current_week as checkin_week,
               cs.share_day, cs.share_week
        FROM users u
        LEFT JOIN user_stats us ON us.user_id = u.user_id
        LEFT JOIN checkin_streaks cs ON cs.user_id = u.user_id
        ORDER BY u.created_at DESC NULLS LAST, u.user_id DESC NULLS LAST
        LIMIT ?
      `,
      args: [limit],
    });
    return c.json(result.rows);
  } catch (e) {
    const msg = (e as Error).message || String(e);
    console.error("admin/users error:", msg);
    return c.json({
      error: "Database error: " + msg,
      hint: "On Deno set DATABASE_URL=postgres://…@VPS:5432/coin_db (same as flaky-kestrel).",
    }, 502);
  }
});

// ─── ADMIN: COIN HISTORY ─────────────────────────────────────
app.get("/admin/coin-history", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden — set X-Admin-Key" }, 403);
  const userId = c.req.query("user_id")?.trim();
  const limit = Math.min(Math.max(parseInt(c.req.query("limit") ?? "100", 10) || 100, 1), 500);

  const result = userId
    ? await db.execute({
      sql: `SELECT id, user_id, amount, type, source, description, created_at
            FROM coin_transactions WHERE user_id = ? ORDER BY id DESC LIMIT ?`,
      args: [userId, limit],
    })
    : await db.execute({
      sql: `SELECT id, user_id, amount, type, source, description, created_at
            FROM coin_transactions ORDER BY id DESC LIMIT ?`,
      args: [limit],
    });
  return c.json(result.rows);
});

app.get("/admin/coin-history/:id", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const id = parseInt(c.req.param("id"), 10);
  if (!id) return c.json({ error: "Invalid id" }, 400);
  const result = await db.execute({
    sql: `SELECT id, user_id, amount, type, source, description, created_at
          FROM coin_transactions WHERE id = ?`,
    args: [id],
  });
  if (!result.rows.length) return c.json({ error: "Not found" }, 404);
  return c.json(result.rows[0]);
});

/** Create transaction (+ adjust balance by default) */
app.post("/admin/coin-history", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const body = await c.req.json();
  const user_id = String(body.user_id ?? "").trim();
  const type = String(body.type ?? "EARN").toUpperCase() === "SPEND" ? "SPEND" : "EARN";
  const amount = Math.abs(Number(body.amount) || 0);
  const source = String(body.source ?? "admin_adjustment").trim() || "admin_adjustment";
  const description = String(body.description ?? "").trim();
  const created_at = normalizeDate(body.created_at) ?? null;
  const adjust_balance = body.adjust_balance !== false;

  if (!user_id || amount < 1) {
    return c.json({ error: "user_id and amount (>=1) required" }, 400);
  }

  if (created_at) {
    await db.execute({
      sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?)`,
      args: [user_id, amount, type, source, description, created_at],
    });
  } else {
    await db.execute({
      sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)`,
      args: [user_id, amount, type, source, description],
    });
  }

  const delta = type === "EARN" ? amount : -amount;
  let new_balance: number | null = null;
  if (adjust_balance) {
    const stats = await db.execute({
      sql: `SELECT krishna_coins FROM user_stats WHERE user_id = ?`,
      args: [user_id],
    });
    if (stats.rows.length) {
      const current = Number((stats.rows[0] as { krishna_coins?: number }).krishna_coins || 0);
      new_balance = Math.max(0, current + delta);
      await db.execute({
        sql: `UPDATE user_stats SET krishna_coins = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?`,
        args: [new_balance, user_id],
      });
    } else {
      new_balance = Math.max(0, delta);
      await db.execute({
        sql: `INSERT INTO user_stats (user_id, krishna_coins, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)`,
        args: [user_id, new_balance],
      });
    }
  }

  return c.json({ success: true, delta, new_balance });
});

/** Update transaction + adjust balance by net change */
app.put("/admin/coin-history/:id", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const id = parseInt(c.req.param("id"), 10);
  if (!id) return c.json({ error: "Invalid id" }, 400);

  const body = await c.req.json();
  const existing = await db.execute({
    sql: `SELECT * FROM coin_transactions WHERE id = ?`,
    args: [id],
  });
  if (!existing.rows.length) return c.json({ error: "Not found" }, 404);
  const old = existing.rows[0] as Record<string, unknown>;

  const user_id = String(body.user_id ?? old.user_id ?? "").trim();
  const type = String(body.type ?? old.type ?? "EARN").toUpperCase() === "SPEND" ? "SPEND" : "EARN";
  const amount = Math.abs(Number(body.amount ?? old.amount) || 0);
  const source = String(body.source ?? old.source ?? "admin_adjustment").trim();
  const description = String(body.description ?? old.description ?? "").trim();
  const created_at = normalizeDate(body.created_at ?? (old.created_at as string));
  const adjust_balance = body.adjust_balance !== false;

  const oldType = String(old.type || "EARN").toUpperCase();
  const oldAmt = Math.abs(Number(old.amount) || 0);
  const oldNet = oldType === "SPEND" ? -oldAmt : oldAmt;
  const newNet = type === "EARN" ? amount : -amount;
  const delta = newNet - oldNet;

  if (created_at) {
    await db.execute({
      sql: `UPDATE coin_transactions
            SET user_id = ?, type = ?, amount = ?, source = ?, description = ?, created_at = ?
            WHERE id = ?`,
      args: [user_id, type, amount, source, description, created_at, id],
    });
  } else {
    await db.execute({
      sql: `UPDATE coin_transactions
            SET user_id = ?, type = ?, amount = ?, source = ?, description = ?
            WHERE id = ?`,
      args: [user_id, type, amount, source, description, id],
    });
  }

  let new_balance: number | null = null;
  if (adjust_balance && delta !== 0) {
    const stats = await db.execute({
      sql: `SELECT krishna_coins FROM user_stats WHERE user_id = ?`,
      args: [user_id],
    });
    if (stats.rows.length) {
      const current = Number((stats.rows[0] as { krishna_coins?: number }).krishna_coins || 0);
      new_balance = Math.max(0, current + delta);
      await db.execute({
        sql: `UPDATE user_stats SET krishna_coins = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?`,
        args: [new_balance, user_id],
      });
    }
  }

  return c.json({ success: true, id, delta, new_balance });
});

app.delete("/admin/coin-history/:id", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const id = parseInt(c.req.param("id"), 10);
  if (!id) return c.json({ error: "Invalid id" }, 400);

  const existing = await db.execute({
    sql: `SELECT * FROM coin_transactions WHERE id = ?`,
    args: [id],
  });
  if (!existing.rows.length) return c.json({ error: "Not found" }, 404);
  const old = existing.rows[0] as Record<string, unknown>;
  const user_id = String(old.user_id || "");
  const oldType = String(old.type || "EARN").toUpperCase();
  const oldAmt = Math.abs(Number(old.amount) || 0);
  const delta = -(oldType === "SPEND" ? -oldAmt : oldAmt);
  const adjust_balance = c.req.query("adjust_balance") !== "false";

  await db.execute({ sql: `DELETE FROM coin_transactions WHERE id = ?`, args: [id] });

  let new_balance: number | null = null;
  if (adjust_balance && user_id && delta !== 0) {
    const stats = await db.execute({
      sql: `SELECT krishna_coins FROM user_stats WHERE user_id = ?`,
      args: [user_id],
    });
    if (stats.rows.length) {
      const current = Number((stats.rows[0] as { krishna_coins?: number }).krishna_coins || 0);
      new_balance = Math.max(0, current + delta);
      await db.execute({
        sql: `UPDATE user_stats SET krishna_coins = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?`,
        args: [new_balance, user_id],
      });
    }
  }

  return c.json({ success: true, id, delta, new_balance });
});

// ─── ADMIN: safe read-only SQL (SELECT only) ─────────────────
app.post("/admin/sql", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const { sql } = await c.req.json();
  const q = String(sql || "").trim();
  if (!q) return c.json({ error: "sql required" }, 400);
  if (!/^\s*select\b/i.test(q)) {
    return c.json({ error: "Only SELECT allowed on this endpoint" }, 400);
  }
  if (/\b(insert|update|delete|drop|alter|attach|pragma|truncate)\b/i.test(q)) {
    return c.json({ error: "Forbidden keyword in SQL" }, 400);
  }
  const result = await db.execute(q);
  return c.json(result.rows);
});

/**
 * Full SQL proxy for the GitHub Pages dashboard.
 * Requires X-Admin-Key. Blocks destructive DDL.
 * Translates datetime('now') and other SQLite-isms for Postgres.
 */
app.post("/admin/query", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden — set X-Admin-Key" }, 403);
  const body = await c.req.json().catch(() => ({}));
  const q = String(body.sql || "").trim();
  if (!q) return c.json({ error: "sql required" }, 400);
  if (/\b(drop|alter|attach|detach|vacuum|reindex|truncate)\b/i.test(q)) {
    return c.json({ error: "DDL keyword not allowed" }, 400);
  }
  try {
    const result = await db.execute(q);
    const rows = result.rows ?? [];
    const plain = (Array.isArray(rows) ? rows : []).map((r: Record<string, unknown>) => {
      if (r && typeof r === "object" && !Array.isArray(r)) {
        const o: Record<string, unknown> = {};
        for (const [k, v] of Object.entries(r)) o[k] = v;
        return o;
      }
      return r;
    });
    return c.json({
      success: true,
      rows: plain,
      affected_rows: Number(result.rowsAffected ?? 0),
    });
  } catch (e) {
    const msg = (e as Error).message || String(e);
    console.error("admin/query error:", msg);
    return c.json({
      error: msg,
      hint: "SQL runs on VPS PostgreSQL. Check table names and set DATABASE_URL on this Deploy project.",
    }, 400);
  }
});

app.onError((err, c) => {
  console.error("Admin API error:", err);
  return c.json({ error: (err as Error).message }, 500);
});

Deno.serve(app.fetch);
console.log("GitaAI Admin API (PostgreSQL) listening — secrets from env only");
