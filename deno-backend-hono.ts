import { Hono } from "https://deno.land/x/hono@v4.3.11/mod.ts";
import { cors } from "https://deno.land/x/hono@v4.3.11/middleware.ts";
import { createClient } from "npm:@libsql/client";


const tursoUrl = Deno.env.get("TURSO_URL");
const tursoToken = Deno.env.get("TURSO_TOKEN");

const db = tursoUrl && tursoToken
  ? createClient({ url: tursoUrl, authToken: tursoToken })
  : createClient({ url: "file:local-test.db" });

// ─── Initialize tables if they don't exist ─────────────────────
async function initTables() {
  try {
    // Core tables for local testing
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL DEFAULT '',
        name TEXT DEFAULT '',
        email TEXT DEFAULT '',
        is_guest INTEGER DEFAULT 0,
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        token TEXT UNIQUE NOT NULL,
        expires_at TEXT NOT NULL,
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS user_stats (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT UNIQUE NOT NULL,
        krishna_coins INTEGER DEFAULT 0,
        yoga_level INTEGER DEFAULT 1,
        current_streak INTEGER DEFAULT 0,
        longest_streak INTEGER DEFAULT 0,
        total_quizzes_taken INTEGER DEFAULT 0,
        total_questions_answered INTEGER DEFAULT 0,
        total_correct_answers INTEGER DEFAULT 0,
        best_score INTEGER DEFAULT 0,
        best_score_out_of INTEGER DEFAULT 0,
        verses_read INTEGER DEFAULT 0,
        chapters_completed INTEGER DEFAULT 0,
        days_active INTEGER DEFAULT 1,
        last_activity_date TEXT DEFAULT '',
        updated_at TEXT DEFAULT (datetime('now')),
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS coin_transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        amount INTEGER NOT NULL,
        type TEXT NOT NULL,
        source TEXT NOT NULL,
        description TEXT DEFAULT '',
        idempotency_key TEXT,
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS level_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        from_level INTEGER NOT NULL,
        to_level INTEGER NOT NULL,
        coins_at_levelup INTEGER NOT NULL,
        bonus_coins INTEGER DEFAULT 0,
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS daily_checkins (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        checkin_date TEXT NOT NULL,
        day INTEGER NOT NULL,
        week INTEGER DEFAULT 1,
        coins_earned INTEGER DEFAULT 0,
        UNIQUE(user_id, checkin_date)
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS yoga_levels (
        level INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        min_coins INTEGER NOT NULL,
        max_coins INTEGER NOT NULL,
        multiplier REAL DEFAULT 1
      )`
    }).catch(() => {});
    const yogaCount = await db.execute({ sql: "SELECT COUNT(*) as cnt FROM yoga_levels" });
    if ((yogaCount.rows[0].cnt as number) === 0) {
      // Softened ladder: 1 / 1.5 / 2 / 2.5 / 3 (was 1–5 integer)
      await db.execute({ sql: "INSERT OR IGNORE INTO yoga_levels (level, name, min_coins, max_coins, multiplier) VALUES (1, 'Karma Yoga', 0, 999, 1)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO yoga_levels (level, name, min_coins, max_coins, multiplier) VALUES (2, 'Bhakti Yoga', 1000, 2999, 1.5)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO yoga_levels (level, name, min_coins, max_coins, multiplier) VALUES (3, 'Jnana Yoga', 3000, 5999, 2)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO yoga_levels (level, name, min_coins, max_coins, multiplier) VALUES (4, 'Dhyana Yoga', 6000, 8999, 2.5)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO yoga_levels (level, name, min_coins, max_coins, multiplier) VALUES (5, 'Raja Yoga', 9000, 99999, 3)" });
    }

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS sloka_shares (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        shared_at TEXT NOT NULL,
        sloka_id TEXT NOT NULL,
        chapter INTEGER NOT NULL,
        verse INTEGER NOT NULL,
        coins INTEGER NOT NULL DEFAULT 0,
        share_day INTEGER NOT NULL DEFAULT 1,
        share_week INTEGER NOT NULL DEFAULT 1,
        weekly_bonus INTEGER NOT NULL DEFAULT 0,
        protection INTEGER NOT NULL DEFAULT 0,
        UNIQUE(user_id, shared_at)
      )`
    }).catch(() => {});

    await db.execute({
      sql: `ALTER TABLE coin_transactions ADD COLUMN idempotency_key TEXT`
    }).catch(() => {});

    // Backfill any existing rows with NULL created_at
    await db.execute({
      sql: `UPDATE coin_transactions SET created_at = datetime('now') WHERE created_at IS NULL`
    }).catch(() => {});

    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN current_streak INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN longest_streak INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN total_quizzes_taken INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN total_questions_answered INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN total_correct_answers INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN best_score INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN best_score_out_of INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN verses_read INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN chapters_completed INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN last_activity_date TEXT` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE user_stats ADD COLUMN updated_at TEXT` }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS voice_chat_rules (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        min_chars INTEGER NOT NULL,
        max_chars INTEGER,
        coins INTEGER NOT NULL,
        label TEXT NOT NULL
      )`
    });

    const count = await db.execute({ sql: "SELECT COUNT(*) as cnt FROM voice_chat_rules" });
    if ((count.rows[0].cnt as number) === 0) {
      await db.execute({ sql: "INSERT OR IGNORE INTO voice_chat_rules (id, min_chars, max_chars, coins, label) VALUES (1, 0, 50, 2, 'Short')" });
      await db.execute({ sql: "INSERT OR IGNORE INTO voice_chat_rules (id, min_chars, max_chars, coins, label) VALUES (2, 51, 150, 3, 'Medium')" });
      await db.execute({ sql: "INSERT OR IGNORE INTO voice_chat_rules (id, min_chars, max_chars, coins, label) VALUES (3, 151, NULL, 5, 'Long')" });
      console.log("Initialized voice_chat_rules with default values");
    }

    // Ensure coin_rules table exists
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS coin_rules (
        source TEXT PRIMARY KEY,
        base_coins INTEGER NOT NULL,
        max_coins INTEGER
      )`
    });
    const ruleCount = await db.execute({ sql: "SELECT COUNT(*) as cnt FROM coin_rules" });
    if ((ruleCount.rows[0].cnt as number) === 0) {
      await db.execute({ sql: "INSERT OR IGNORE INTO coin_rules (source, base_coins, max_coins) VALUES ('quiz_completion', 5, 15)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO coin_rules (source, base_coins, max_coins) VALUES ('chapter_completion', 15, 15)" });
    }
    await db.execute({ sql: "INSERT OR IGNORE INTO coin_rules (source, base_coins, max_coins) VALUES ('battle_quiz', 0, 1000)" });
    
    // Ensure existing databases are updated
    await db.execute({ sql: "UPDATE coin_rules SET base_coins = 5, max_coins = 15 WHERE source = 'quiz_completion'" });
    await db.execute({ sql: "UPDATE coin_rules SET max_coins = 1000 WHERE source = 'battle_quiz'" });

    // Quiz attempts table
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS quiz_attempts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        score INTEGER NOT NULL DEFAULT 0,
        total_questions INTEGER NOT NULL DEFAULT 15,
        quiz_type TEXT DEFAULT 'general',
        time_spent_seconds INTEGER DEFAULT 0,
        avg_time_per_question INTEGER DEFAULT 0,
        coins_earned INTEGER DEFAULT 0,
        accuracy INTEGER DEFAULT 0,
        created_at TEXT DEFAULT (datetime('now')),
        attempt_id TEXT,
        language TEXT DEFAULT 'en'
      )`
    }).catch(() => {});

    // Add new columns if they don't exist (for existing databases)
    await db.execute({ sql: `ALTER TABLE quiz_attempts ADD COLUMN avg_time_per_question INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE quiz_attempts ADD COLUMN coins_earned INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE quiz_attempts ADD COLUMN accuracy INTEGER DEFAULT 0` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE quiz_attempts ADD COLUMN attempt_id TEXT` }).catch(() => {});
    await db.execute({ sql: `CREATE UNIQUE INDEX IF NOT EXISTS index_quiz_attempts_attempt_id ON quiz_attempts (attempt_id)` }).catch(() => {});
    await db.execute({ sql: `ALTER TABLE quiz_attempts ADD COLUMN language TEXT DEFAULT 'en'` }).catch(() => {});

    // Verse notes table — unique constraint required for ON CONFLICT upsert
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS verse_notes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        chapter_no INTEGER NOT NULL,
        verse_no INTEGER NOT NULL,
        note TEXT NOT NULL,
        created_at TEXT DEFAULT (datetime('now')),
        updated_at TEXT DEFAULT (datetime('now')),
        UNIQUE(user_id, chapter_no, verse_no)
      )`
    }).catch(() => {});
    // Backfill unique constraint on existing databases (idempotent)
    await db.execute({ sql: `CREATE UNIQUE INDEX IF NOT EXISTS idx_verse_notes_unique ON verse_notes(user_id, chapter_no, verse_no)` }).catch(() => {});
    await db.execute({ sql: `CREATE INDEX IF NOT EXISTS idx_verse_notes_user ON verse_notes(user_id)` }).catch(() => {});

    // Meditation sessions table
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS meditation_sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        minutes INTEGER NOT NULL,
        coins_earned INTEGER DEFAULT 0,
        session_date TEXT NOT NULL,
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});
    await db.execute({ sql: `CREATE INDEX IF NOT EXISTS idx_meditation_user ON meditation_sessions(user_id)` }).catch(() => {});

    // User feedback table
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS user_feedback (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        type TEXT DEFAULT 'feedback',
        subject TEXT DEFAULT '',
        message TEXT NOT NULL,
        status TEXT DEFAULT 'open',
        created_at TEXT DEFAULT (datetime('now'))
      )`
    }).catch(() => {});

    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS checkin_streaks (
        user_id TEXT PRIMARY KEY NOT NULL,
        current_day INTEGER DEFAULT 0,
        current_week INTEGER DEFAULT 1,
        share_day INTEGER DEFAULT 0,
        share_week INTEGER DEFAULT 1,
        last_checkin TEXT,
        last_share TEXT
      )`
    }).catch(() => {});

    // Checkin rewards table
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS checkin_rewards (
        day INTEGER PRIMARY KEY,
        coins INTEGER NOT NULL
      )`
    }).catch(() => {});
    const checkinRewardCount = await db.execute({ sql: "SELECT COUNT(*) as cnt FROM checkin_rewards" });
    if ((checkinRewardCount.rows[0].cnt as number) === 0) {
      for (let d = 1; d <= 7; d++) {
        await db.execute({ sql: "INSERT OR IGNORE INTO checkin_rewards (day, coins) VALUES (?, ?)", args: [d, d] });
      }
      console.log("Initialized checkin_rewards with default values (1-7 coins)");
    }

    // Weekly bonus rules table
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS weekly_bonus_rules (
        week INTEGER PRIMARY KEY,
        coins INTEGER NOT NULL
      )`
    }).catch(() => {});
    const weeklyBonusCount = await db.execute({ sql: "SELECT COUNT(*) as cnt FROM weekly_bonus_rules" });
    if ((weeklyBonusCount.rows[0].cnt as number) === 0) {
      await db.execute({ sql: "INSERT OR IGNORE INTO weekly_bonus_rules (week, coins) VALUES (1, 10)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO weekly_bonus_rules (week, coins) VALUES (2, 10)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO weekly_bonus_rules (week, coins) VALUES (3, 10)" });
      await db.execute({ sql: "INSERT OR IGNORE INTO weekly_bonus_rules (week, coins) VALUES (4, 20)" });
      console.log("Initialized weekly_bonus_rules with default values");
    }

    // Migrations
    await db.execute({
      sql: `ALTER TABLE users ADD COLUMN country_code TEXT DEFAULT ''`
    }).catch(() => {}); // Catch error if column already exists

  } catch (e) {
    console.error("Failed to init tables:", e);
  }
}

/** Bump when adding migrations/indexes so cold starts re-run init once. */
const SCHEMA_VERSION = 4;

/** Softened yoga coin multipliers: L1=1, L2=1.5, L3=2, L4=2.5, L5=3 (was 1–5). */
async function ensureYogaMultipliers() {
  await db.execute({ sql: `UPDATE yoga_levels SET multiplier = 1 WHERE level = 1` }).catch(() => {});
  await db.execute({ sql: `UPDATE yoga_levels SET multiplier = 1.5 WHERE level = 2` }).catch(() => {});
  await db.execute({ sql: `UPDATE yoga_levels SET multiplier = 2 WHERE level = 3` }).catch(() => {});
  await db.execute({ sql: `UPDATE yoga_levels SET multiplier = 2.5 WHERE level = 4` }).catch(() => {});
  await db.execute({ sql: `UPDATE yoga_levels SET multiplier = 3 WHERE level = 5` }).catch(() => {});
}

/** Hot-path indexes for coin history / quizzes / sessions (idempotent). */
async function ensureHotPathIndexes() {
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_coin_tx_user_created ON coin_transactions(user_id, created_at)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_coin_tx_user_idem ON coin_transactions(user_id, idempotency_key)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_coin_tx_user_type_created ON coin_transactions(user_id, type, created_at)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_checkin_streaks_user ON checkin_streaks(user_id)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_created ON quiz_attempts(user_id, created_at)`,
  }).catch(() => {});
  await db.execute({
    sql: `CREATE INDEX IF NOT EXISTS idx_level_history_user ON level_history(user_id)`,
  }).catch(() => {});
  await ensureYogaMultipliers();
}

/**
 * Schema gate: skip full initTables (~63 SQLs) when DB already at SCHEMA_VERSION.
 * Cold starts pay 1–2 cheap queries instead of full CREATE/ALTER/seed spam.
 * Safe for old APKs — HTTP contract unchanged.
 */
async function ensureSchema() {
  try {
    await db.execute({
      sql: `CREATE TABLE IF NOT EXISTS schema_meta (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        version INTEGER NOT NULL DEFAULT 0
      )`,
    }).catch(() => {});

    const meta = await db.execute({
      sql: `SELECT version FROM schema_meta WHERE id = 1`,
    });
    const current = meta.rows.length ? Number(meta.rows[0].version) || 0 : 0;

    if (current >= SCHEMA_VERSION) {
      console.log(`Schema v${current} up to date — skip initTables`);
      return;
    }

    // Fresh DB: full init. Already-migrated (v2+): indexes only (cheap).
    if (current === 0) {
      console.log(`Schema v0 → v${SCHEMA_VERSION}: running initTables + indexes`);
      await initTables();
    } else {
      console.log(`Schema v${current} → v${SCHEMA_VERSION}: indexes only`);
    }
    await ensureHotPathIndexes();

    await db.execute({
      sql: `INSERT INTO schema_meta (id, version) VALUES (1, ?)
            ON CONFLICT(id) DO UPDATE SET version = excluded.version`,
      args: [SCHEMA_VERSION],
    });
    console.log(`Schema upgraded to v${SCHEMA_VERSION}`);
  } catch (e) {
    console.error("ensureSchema failed (falling back to initTables):", e);
    try {
      await initTables();
      await ensureHotPathIndexes();
    } catch (e2) {
      console.error("Fallback initTables failed:", e2);
    }
  }
}

await ensureSchema();

// ─── HELPERS ─────────────────────────────────────────────────
// ─── PBKDF2-SHA256 password helpers (salted, 100k iterations) ─
const PBKDF2_ITERATIONS = 100_000;
const PBKDF2_KEY_LENGTH = 32;

async function hashPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const encoder = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    "raw", encoder.encode(password), { name: "PBKDF2" }, false, ["deriveBits"]
  );
  const derivedBits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations: PBKDF2_ITERATIONS, hash: "SHA-256" },
    keyMaterial, PBKDF2_KEY_LENGTH * 8
  );
  const hash = new Uint8Array(derivedBits);
  const saltB64 = btoa(String.fromCharCode(...salt));
  const hashB64 = btoa(String.fromCharCode(...hash));
  return `${PBKDF2_ITERATIONS}:${saltB64}:${hashB64}`;
}

async function verifyPassword(password: string, stored: string): Promise<boolean> {
  // Support legacy plain SHA-256 hashes (hex, 64 chars) during migration
  if (!stored.includes(":")) {
    const encoder = new TextEncoder();
    const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(password));
    const legacyHash = Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('');
    return legacyHash === stored;
  }
  const parts = stored.split(":");
  if (parts.length !== 3) return false;
  const iterations = parseInt(parts[0], 10);
  const salt = Uint8Array.from(atob(parts[1]), c => c.charCodeAt(0));
  const expectedHash = Uint8Array.from(atob(parts[2]), c => c.charCodeAt(0));
  const encoder = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    "raw", encoder.encode(password), { name: "PBKDF2" }, false, ["deriveBits"]
  );
  const derivedBits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
    keyMaterial, PBKDF2_KEY_LENGTH * 8
  );
  const candidateHash = new Uint8Array(derivedBits);
  if (candidateHash.length !== expectedHash.length) return false;
  let diff = 0;
  for (let i = 0; i < candidateHash.length; i++) diff |= candidateHash[i] ^ expectedHash[i];
  return diff === 0;
}

function generateToken(): string {
  return crypto.randomUUID();
}

async function updateUserCountry(user_id: string, country_code: string | undefined | null): Promise<void> {
  if (country_code && country_code.trim().length > 0) {
    await db.execute({
      sql: `UPDATE users SET country_code = ? WHERE user_id = ?`,
      args: [country_code.trim().toUpperCase(), user_id],
    }).catch(e => console.error("Error updating country:", e));
  }
}

const COUNTRY_TIMEZONE_MAP: Record<string, string> = {
  IN: "Asia/Kolkata",
  US: "America/New_York",
  GB: "Europe/London",
  CA: "America/Toronto",
  AU: "Australia/Sydney",
  JP: "Asia/Tokyo",
  DE: "Europe/Berlin",
  FR: "Europe/Paris",
  BR: "America/Sao_Paulo",
  AE: "Asia/Dubai",
  SG: "Asia/Singapore",
  NZ: "Pacific/Auckland",
  ZA: "Africa/Johannesburg",
  MX: "America/Mexico_City",
  KR: "Asia/Seoul",
  IT: "Europe/Rome",
  ES: "Europe/Madrid",
  NL: "Europe/Amsterdam",
  SE: "Europe/Stockholm",
  NO: "Europe/Oslo",
  FI: "Europe/Helsinki",
  RU: "Europe/Moscow",
  ID: "Asia/Jakarta",
  MY: "Asia/Kuala_Lumpur",
  TH: "Asia/Bangkok",
  VN: "Asia/Ho_Chi_Minh",
  PH: "Asia/Manila",
  PK: "Asia/Karachi",
  BD: "Asia/Dhaka",
  LK: "Asia/Colombo",
  NP: "Asia/Kathmandu",
  SA: "Asia/Riyadh",
  EG: "Africa/Cairo",
  NG: "Africa/Lagos",
  KE: "Africa/Nairobi",
  AR: "America/Argentina/Buenos_Aires",
  CL: "America/Santiago",
  CO: "America/Bogota"
};

function getIPCountry(c: any): string | null {
  if (!c) return null;
  return c.req.header("cf-ipcountry") ||
         c.req.header("x-country-code") ||
         c.req.header("x-appengine-country") ||
         c.req.header("cloudfront-viewer-country") ||
         null;
}

function getTimezone(c?: any, countryCode?: string | null, clientTimezone?: string | null): string {
  if (clientTimezone && clientTimezone.includes("/")) {
    try {
      Intl.DateTimeFormat(undefined, { timeZone: clientTimezone });
      return clientTimezone;
    } catch (_) {}
  }
  const effectiveCountry = countryCode || getIPCountry(c);
  if (effectiveCountry) {
    const code = effectiveCountry.trim().toUpperCase();
    if (COUNTRY_TIMEZONE_MAP[code]) {
      return COUNTRY_TIMEZONE_MAP[code];
    }
  }
  return "Asia/Kolkata";
}

function getLocalDate(timeZone: string = "Asia/Kolkata"): string {
  try {
    const formatter = new Intl.DateTimeFormat("en-CA", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" });
    return formatter.format(new Date());
  } catch (_) {
    return new Date().toISOString().split("T")[0];
  }
}

// ─── HELPER FUNCTIONS ─────────────────────────

async function getUserBalance(user_id: string): Promise<number> {
  const stats = await db.execute({
    sql: `SELECT krishna_coins FROM user_stats WHERE user_id = ?`,
    args: [user_id],
  });
  return stats.rows.length ? (stats.rows[0].krishna_coins as number) : 0;
}

async function logCoinTransaction(
  user_id: string,
  amount: number,
  type: string,
  source: string,
  description: string,
  idempotency_key?: string
): Promise<void> {
  await db.execute({
    sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, idempotency_key, created_at) 
          VALUES (?, ?, ?, ?, ?, ?, datetime('now'))`,
    args: [user_id, amount, type, source, description, idempotency_key || null],
  });
}

// ─── APP SETUP ───────────────────────────────────────────────
const SYNC_BONUS = 50;
const app = new Hono();

app.use("*", cors({
  origin: "*",
  allowMethods: ["GET", "POST", "OPTIONS"],
  allowHeaders: ["Content-Type", "Authorization"],
}));

// ─── AUTH MIDDLEWARE (replaces 6+ duplicated token/session blocks) ──
type AuthVars = { userId: string };

async function requireAuth(c: any, next: any) {
  const token = c.req.header("Authorization")?.replace("Bearer ", "");
  if (!token) return c.json({ error: "Unauthorized" }, 401);

  const session = await db.execute({
    sql: `SELECT user_id FROM sessions WHERE token = ? AND expires_at > datetime('now')`,
    args: [token],
  });
  if (!session.rows.length) return c.json({ error: "Invalid or expired token" }, 401);

  c.set("userId", session.rows[0].user_id as string);
  await next();
}

// ─── ROOT ────────────────────────────────────────────────────
app.get("/", (c) => c.json({
  status: "Gita App API running ✅",
  endpoints: [
    "POST /auth/register",
    "POST /auth/login",
    "GET  /auth/verify",
    "POST /auth/logout",
    "POST /auth/delete",
    "POST /guest/create",
    "POST /guest/claim",
    "POST /users/create",
    "POST /users/stats/sync",
    "GET  /coins/balance?user_id=",
    "POST /coins/award",
    "POST /coins/spend",
    "GET  /coins/history?user_id=",
    "GET  /coins/leaderboard",
    "GET  /coins/voice-cost?question=",
    "POST /checkin",
    "POST /share",
    "GET  /yoga/stages",
    "POST /quiz/attempt",
    "GET  /quiz/history?user_id=",
    "GET  /activity/history?user_id=",
    "POST /admin/reset-stats",
  ],
}));

// ─── ADMIN DASHBOARD (browser UI) ────────────────────────────
app.get("/admin", (c) => {
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Gita App — Admin Dashboard</title>
<style>
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Inter',system-ui,sans-serif;background:#0a0a0f;color:#e2e8f0;min-height:100vh}
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
  #lock{display:flex;align-items:center;justify-content:center;min-height:100vh;flex-direction:column;gap:20px}
  #app{display:none;padding:24px;max-width:1100px;margin:0 auto}
  .card{background:#12121a;border:1px solid #1e1e2e;border-radius:14px;padding:20px;margin-bottom:20px}
  h1{font-size:22px;font-weight:700;color:#f59e0b;margin-bottom:4px}
  h2{font-size:15px;font-weight:600;color:#94a3b8;margin-bottom:16px}
  label{font-size:12px;color:#64748b;font-weight:500;display:block;margin-bottom:6px}
  input,select{width:100%;padding:10px 14px;background:#0f0f1a;border:1px solid #1e2a3a;border-radius:8px;color:#e2e8f0;font-size:14px;outline:none;transition:border .2s}
  input:focus,select:focus{border-color:#f59e0b}
  button{padding:10px 20px;border:none;border-radius:8px;font-weight:600;font-size:14px;cursor:pointer;transition:all .2s}
  .btn-gold{background:#f59e0b;color:#0a0a0f}.btn-gold:hover{background:#d97706}
  .btn-red{background:#dc2626;color:#fff}.btn-red:hover{background:#b91c1c}
  .btn-blue{background:#2563eb;color:#fff}.btn-blue:hover{background:#1d4ed8}
  .btn-sm{padding:6px 12px;font-size:12px;border-radius:6px}
  .grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}
  @media(max-width:600px){.grid{grid-template-columns:1fr}}
  .stat{background:#0f0f1a;border:1px solid #1e2a3a;border-radius:10px;padding:14px}
  .stat-val{font-size:22px;font-weight:700;color:#f59e0b}
  .stat-label{font-size:11px;color:#64748b;margin-top:2px}
  table{width:100%;border-collapse:collapse;font-size:13px}
  th{text-align:left;padding:8px 10px;color:#64748b;font-weight:500;border-bottom:1px solid #1e1e2e}
  td{padding:8px 10px;border-bottom:1px solid #12121a}
  tr:hover td{background:#0f0f1a}
  .badge{display:inline-block;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:600}
  .earn{background:#052e16;color:#4ade80}.spend{background:#450a0a;color:#f87171}
  .msg{padding:10px 14px;border-radius:8px;font-size:13px;margin-top:10px}
  .msg.ok{background:#052e16;color:#4ade80}.msg.err{background:#450a0a;color:#f87171}
  #lockBox{background:#12121a;border:1px solid #1e1e2e;border-radius:16px;padding:32px;width:340px;text-align:center}
  #lockBox h1{margin-bottom:8px}
  #lockBox p{font-size:13px;color:#64748b;margin-bottom:24px}
  #lockBox input{margin-bottom:14px}
  .sep{border:none;border-top:1px solid #1e1e2e;margin:20px 0}
  .row{display:flex;gap:10px;align-items:flex-end}
  .row .field{flex:1}
  .tag{font-size:10px;background:#1e2a3a;color:#64748b;border-radius:4px;padding:2px 6px;margin-left:6px}
  #userCard{display:none}
</style>
</head>
<body>

<!-- LOCK SCREEN -->
<div id="lock">
  <div id="lockBox">
    <h1>🪷 Gita Admin</h1>
    <p>Enter your admin key to continue</p>
    <label>Admin Key</label>
    <input type="password" id="keyInput" placeholder="Enter admin key" onkeydown="if(event.key==='Enter')unlock()">
    <button class="btn-gold" style="width:100%" onclick="unlock()">Unlock Dashboard</button>
    <div id="lockMsg"></div>
  </div>
</div>

<!-- DASHBOARD -->
<div id="app">
  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:24px">
    <div><h1>🪷 Gita Admin Dashboard</h1><p style="font-size:13px;color:#64748b">Manage users, coins &amp; streaks</p></div>
    <button class="btn-sm" style="background:#1e2a3a;color:#94a3b8" onclick="lockout()">🔒 Lock</button>
  </div>

  <!-- USER SEARCH -->
  <div class="card">
    <h2>👤 User Lookup</h2>
    <div class="row">
      <div class="field"><label>User ID</label><input type="text" id="uidInput" placeholder="e.g. raja21" onkeydown="if(event.key==='Enter')loadUser()"></div>
      <button class="btn-gold" onclick="loadUser()">Load</button>
    </div>
    <div id="searchMsg"></div>
  </div>

  <!-- USER CARD -->
  <div id="userCard">
    <div class="card">
      <h2>📊 Stats <span class="tag" id="uidTag"></span></h2>
      <div class="grid" id="statsGrid"></div>
    </div>

    <div class="card">
      <h2>🔧 Actions</h2>
      <div class="grid">
        <div>
          <label>Reset Streak to</label>
          <div class="row">
            <div class="field"><input type="number" id="streakInput" placeholder="e.g. 5" min="0"></div>
            <button class="btn-blue btn-sm" onclick="restoreStreak()">Restore Streak</button>
          </div>
        </div>
        <div>
          <label>Reset All Stats</label>
          <button class="btn-red btn-sm" style="width:100%;margin-top:6px" onclick="resetStats()">⚠️ Reset Stats</button>
        </div>
      </div>
      <div id="actionMsg"></div>
    </div>

    <div class="card">
      <h2>🪙 Coin History <span id="histCount" class="tag"></span></h2>
      <div style="overflow-x:auto">
        <table>
          <thead><tr><th>#</th><th>Date</th><th>Type</th><th>Source</th><th>Amount</th><th>Description</th></tr></thead>
          <tbody id="histBody"></tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<script>
let KEY = '';
let CURRENT_UID = '';
const BASE = '';

async function unlock() {
  const k = document.getElementById('keyInput').value.trim();
  if (!k) return;
  // Cheap key check only — never run clean-duplicates on unlock (that scanned huge tables)
  const res = await fetch(BASE + '/admin/verify?admin_key=' + encodeURIComponent(k), {method:'GET'});
  if (res.status === 403) {
    showMsg('lockMsg','err','❌ Wrong admin key');
    return;
  }
  if (!res.ok) {
    showMsg('lockMsg','err','❌ Unlock failed (' + res.status + ')');
    return;
  }
  KEY = k;
  localStorage.setItem('gita_admin_key', k);
  document.getElementById('lock').style.display = 'none';
  document.getElementById('app').style.display = 'block';
  showMsg('lockMsg','','');
}

function lockout() {
  KEY = '';
  localStorage.removeItem('gita_admin_key');
  document.getElementById('lock').style.display = 'flex';
  document.getElementById('app').style.display = 'none';
}

async function loadUser() {
  const uid = document.getElementById('uidInput').value.trim();
  if (!uid) return;
  CURRENT_UID = uid;
  document.getElementById('searchMsg').innerHTML = '<p style="color:#64748b;font-size:13px;margin-top:10px">Loading...</p>';
  try {
    // Use admin key to fetch balance (coins/balance is protected by requireAuth so call via admin endpoint approach)
    // We'll use a direct DB query via admin/clean-duplicates won't work - let's call coins/balance with a workaround
    // Instead, we add a dedicated admin user lookup below — for now show what we can
    const res = await fetch(BASE + '/admin/reset-stats', {
      method:'POST',
      headers:{'Content-Type':'application/json','x-admin-key':KEY},
      body: JSON.stringify({user_id: uid, _check_only: true})
    });
    const statsRes = await fetchAdmin('/admin/user-stats?user_id=' + encodeURIComponent(uid));
    if (!statsRes) { showMsg('searchMsg','err','User not found or API error'); return; }
    renderUser(statsRes);
  } catch(e) {
    showMsg('searchMsg','err','Error: ' + e.message);
  }
}

async function fetchAdmin(path) {
  try {
    const res = await fetch(BASE + path + (path.includes('?') ? '&' : '?') + 'admin_key=' + encodeURIComponent(KEY));
    if (!res.ok) return null;
    return await res.json();
  } catch { return null; }
}

async function loadUserDirect(uid) {
  CURRENT_UID = uid;
  document.getElementById('uidTag').textContent = uid;
  document.getElementById('searchMsg').innerHTML = '';
  // Load stats
  const stats = await fetchAdmin('/admin/user-stats?user_id=' + encodeURIComponent(uid));
  if (!stats || stats.error) { showMsg('searchMsg','err','User not found'); document.getElementById('userCard').style.display='none'; return; }
  renderStats(stats);
  // Load coin history
  const hist = await fetchAdmin('/admin/coin-history?user_id=' + encodeURIComponent(uid) + '&limit=50');
  renderHistory(hist || []);
  document.getElementById('userCard').style.display = 'block';
}

function renderStats(s) {
  const grid = document.getElementById('statsGrid');
  const items = [
    ['Krishna Coins', s.krishna_coins ?? 0, '🪙'],
    ['Yoga Level', s.yoga_level ?? 1, '🧘'],
    ['Current Streak', s.current_streak ?? 0, '🔥'],
    ['Longest Streak', s.longest_streak ?? 0, '⚡'],
    ['Quizzes Taken', s.total_quizzes_taken ?? 0, '📝'],
    ['Correct Answers', s.total_correct_answers ?? 0, '✅'],
    ['Verses Read', s.verses_read ?? 0, '📖'],
    ['Chapters Done', s.chapters_completed ?? 0, '📚'],
  ];
  grid.innerHTML = items.map(([label, val, icon]) =>
    \`<div class="stat"><div class="stat-val">\${icon} \${val}</div><div class="stat-label">\${label}</div></div>\`
  ).join('');
}

function renderHistory(rows) {
  document.getElementById('histCount').textContent = rows.length + ' records';
  document.getElementById('histBody').innerHTML = rows.map((r,i) => {
    const cls = r.type === 'EARN' ? 'earn' : 'spend';
    const sign = r.type === 'EARN' ? '+' : '';
    const date = (r.created_at || '').substring(0,16).replace('T',' ');
    return \`<tr><td>\${i+1}</td><td>\${date}</td><td><span class="badge \${cls}">\${r.type}</span></td><td>\${r.source}</td><td style="color:\${r.type==='EARN'?'#4ade80':'#f87171'}">\${sign}\${r.amount}</td><td style="color:#64748b">\${r.description||''}</td></tr>\`;
  }).join('') || '<tr><td colspan="6" style="text-align:center;color:#64748b;padding:20px">No records</td></tr>';
}

async function restoreStreak() {
  const target = document.getElementById('streakInput').value.trim();
  if (!CURRENT_UID) return showMsg('actionMsg','err','Load a user first');
  const res = await fetch(BASE + '/admin/restore-streak', {
    method:'POST',
    headers:{'Content-Type':'application/json','x-admin-key':KEY},
    body: JSON.stringify({user_id: CURRENT_UID, target_streak: target ? Number(target) : undefined})
  });
  const j = await res.json();
  if (j.success) showMsg('actionMsg','ok','✅ Streak restored to ' + j.current_streak);
  else showMsg('actionMsg','err','Error: ' + (j.error || 'unknown'));
  loadUserDirect(CURRENT_UID);
}

async function resetStats() {
  if (!CURRENT_UID) return showMsg('actionMsg','err','Load a user first');
  if (!confirm('Reset all stats for ' + CURRENT_UID + '? This cannot be undone.')) return;
  const res = await fetch(BASE + '/admin/reset-stats', {
    method:'POST',
    headers:{'Content-Type':'application/json','x-admin-key':KEY},
    body: JSON.stringify({user_id: CURRENT_UID})
  });
  const j = await res.json();
  if (j.success) showMsg('actionMsg','ok','✅ Stats reset');
  else showMsg('actionMsg','err','Error: ' + (j.error || 'unknown'));
  loadUserDirect(CURRENT_UID);
}

function showMsg(id, type, text) {
  const el = document.getElementById(id);
  if (!el) return;
  el.innerHTML = text ? \`<div class="msg \${type}">\${text}</div>\` : '';
}

// Override loadUser to use the direct approach
async function loadUser() {
  const uid = document.getElementById('uidInput').value.trim();
  if (!uid) return;
  document.getElementById('searchMsg').innerHTML = '<p style="color:#64748b;font-size:13px;margin-top:10px">Loading...</p>';
  await loadUserDirect(uid);
}

// Try restoring saved key on load
window.onload = () => {
  const saved = localStorage.getItem('gita_admin_key');
  if (saved) document.getElementById('keyInput').value = saved;
};
</script>
</body>
</html>`;
  return c.html(html);
});

// ─── ADMIN USER STATS (admin-key protected, returns full user stats) ──
app.get("/admin/user-stats", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const user_id = c.req.query("user_id");
  if (!user_id) return c.json({ error: "user_id required" }, 400);
  const stats = await db.execute({
    sql: `SELECT us.*, yl.name as yoga_name, yl.multiplier
          FROM user_stats us
          LEFT JOIN yoga_levels yl ON yl.level = us.yoga_level
          WHERE us.user_id = ?`,
    args: [user_id],
  });
  if (!stats.rows.length) return c.json({ error: "User not found" }, 404);
  const user = await db.execute({ sql: `SELECT name, email, is_guest, created_at FROM users WHERE user_id = ?`, args: [user_id] });
  return c.json({ ...stats.rows[0], ...(user.rows[0] || {}) });
});

// ─── ADMIN COIN HISTORY (admin-key protected) ─────────────────
app.get("/admin/coin-history", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const user_id = c.req.query("user_id");
  if (!user_id) return c.json({ error: "user_id required" }, 400);
  const limit = Math.min(parseInt(c.req.query("limit") ?? "100"), 500);
  const result = await db.execute({
    sql: `SELECT id, amount, type, source, description, created_at FROM coin_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?`,
    args: [user_id, limit],
  });
  return c.json(result.rows);
});

// ─── SERVER TIME (Internet Time Check for Any Country) ───────
app.get("/server-time", (c) => {
  const country = c.req.query("country_code");
  const tzQuery = c.req.query("timezone");
  const userTz = getTimezone(c, country, tzQuery);
  const now = new Date();
  const serverToday = getLocalDate(userTz);
  return c.json({
    server_time_iso: now.toISOString(),
    server_time_ms: now.getTime(),
    server_today: serverToday,
    timezone: userTz
  });
});

// ─── AUTH REGISTER ───────────────────────────────────────────
app.post("/auth/register", async (c) => {
  try {
    let { user_id, password, name = "", email = "" } = await c.req.json();

    if (!user_id || !password) {
      return c.json({ error: "user_id and password required" }, 400);
    }
    if (!email) email = `${user_id}@gita.com`;

    const existing = await db.execute({
      sql: "SELECT user_id, password_hash FROM users WHERE user_id = ?",
      args: [user_id],
    });
    if (existing.rows.length) {
      // User already registered — verify password before issuing a new token
      const storedHash = existing.rows[0].password_hash as string;
      const passwordOk = await verifyPassword(password, storedHash);
      if (!passwordOk) {
        return c.json({ error: "User already registered. Use /auth/login to sign in." }, 409);
      }
      // Correct password — issue a new session (acts like login)
      const token = generateToken();
      await db.execute({
        sql: `INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, datetime('now', '+30 days'))`,
        args: [user_id, token],
      });
      const stats = await db.execute({ sql: `SELECT krishna_coins FROM user_stats WHERE user_id = ?`, args: [user_id] });
      return c.json({ success: true, user_id, token, coins: stats.rows[0]?.krishna_coins ?? 0, duplicate: true });
    }

    const passwordHash = await hashPassword(password);

    await db.execute({
      sql: `INSERT INTO users (user_id, name, email, password_hash, is_guest) VALUES (?, ?, ?, ?, 0)`,
      args: [user_id, name, email, passwordHash],
    });
    await db.execute({
      sql: `INSERT INTO user_stats (user_id, krishna_coins, days_active, yoga_level, last_activity_date) VALUES (?, 200, 1, 1, ?)`,
      args: [user_id, new Date().toISOString().split("T")[0]],
    });
    await db.execute({
      sql: `INSERT OR IGNORE INTO checkin_streaks (user_id, current_day, current_week, share_day, share_week) VALUES (?, 0, 1, 0, 1)`,
      args: [user_id],
    });
    await db.execute({
      sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, 200, 'EARN', 'signup', 'Welcome bonus — new seeker', datetime('now'))`,
      args: [user_id],
    });

    const token = generateToken();
    await db.execute({
      sql: `INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, datetime('now', '+30 days'))`,
      args: [user_id, token],
    });

    return c.json({ success: true, user_id, token, coins: 200 });
  } catch (err: any) {
    return c.json({ error: "Register failed", details: err.message }, 500);
  }
});

// ─── AUTH LOGIN ───────────────────────────────────────────────
app.post("/auth/login", async (c) => {
  const { user_id, password } = await c.req.json();

  if (!user_id || !password) {
    return c.json({ error: "user_id and password required" }, 400);
  }

  const user = await db.execute({
    sql: "SELECT user_id, password_hash FROM users WHERE user_id = ?",
    args: [user_id],
  });
  if (!user.rows.length) {
    return c.json({ error: "User not found" }, 404);
  }

  const storedHash = user.rows[0].password_hash as string;
  const passwordOk = await verifyPassword(password, storedHash);
  if (!passwordOk) {
    return c.json({ error: "Invalid password" }, 401);
  }

  const token = generateToken();
  await db.execute({
    sql: `INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, datetime('now', '+30 days'))`,
    args: [user_id, token],
  });

  const stats = await db.execute({
    sql: `SELECT krishna_coins, yoga_level FROM user_stats WHERE user_id = ?`,
    args: [user_id],
  });

  return c.json({
    success: true,
    user_id,
    token,
    coins: stats.rows[0]?.krishna_coins ?? 0,
    yoga_level: stats.rows[0]?.yoga_level ?? 1,
  });
});

// ─── AUTH VERIFY ──────────────────────────────────────────────
app.get("/auth/verify", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;

  const stats = await db.execute({
    sql: `SELECT krishna_coins, yoga_level FROM user_stats WHERE user_id = ?`,
    args: [user_id],
  });

  return c.json({
    valid: true,
    user_id,
    coins: stats.rows[0]?.krishna_coins ?? 0,
    yoga_level: stats.rows[0]?.yoga_level ?? 1,
  });
});

// ─── AUTH LOGOUT ──────────────────────────────────────────────
app.post("/auth/logout", async (c) => {
  const token = c.req.header("Authorization")?.replace("Bearer ", "");
  if (token) {
    await db.execute({ sql: "DELETE FROM sessions WHERE token = ?", args: [token] });
  }
  return c.json({ success: true });
});

// ─── AUTH DELETE ──────────────────────────────────────────────
app.post("/auth/delete", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;

  await db.execute({ sql: `DELETE FROM sloka_shares WHERE user_id = ?`,      args: [user_id] });
  await db.execute({ sql: `DELETE FROM coin_transactions WHERE user_id = ?`, args: [user_id] });
  await db.execute({ sql: `DELETE FROM checkin_streaks WHERE user_id = ?`,   args: [user_id] });
  await db.execute({ sql: `DELETE FROM level_history WHERE user_id = ?`,     args: [user_id] });
  await db.execute({ sql: `DELETE FROM sessions WHERE user_id = ?`,          args: [user_id] });
  await db.execute({ sql: `DELETE FROM user_stats WHERE user_id = ?`,        args: [user_id] });
  await db.execute({ sql: `DELETE FROM users WHERE user_id = ?`,             args: [user_id] });

  return c.json({ success: true });
});

// ─── GUEST CREATE ─────────────────────────────────────────────
app.post("/guest/create", async (c) => {
  const guest_id = `guest_${crypto.randomUUID()}`;
  const guest_email = `${guest_id}@gita.com`;
  await db.execute({ sql: `INSERT INTO users (user_id, name, email, is_guest) VALUES (?, 'Guest', ?, 1)`, args: [guest_id, guest_email] });
  await db.execute({ sql: `INSERT INTO user_stats (user_id, krishna_coins, days_active, yoga_level, last_activity_date) VALUES (?, 50, 1, 1, ?)`, args: [guest_id, new Date().toISOString().split("T")[0]] });
  await db.execute({ sql: `INSERT OR IGNORE INTO checkin_streaks (user_id, current_day, current_week, share_day, share_week) VALUES (?, 0, 1, 0, 1)`, args: [guest_id] });
  await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, 50, 'EARN', 'signup', 'Guest welcome bonus', datetime('now'))`, args: [guest_id] });

  const token = generateToken();
  await db.execute({
    sql: `INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, datetime('now', '+30 days'))`,
    args: [guest_id, token],
  });

  return c.json({ guest_id, token, coins: 50 });
});

// ─── GUEST CLAIM ──────────────────────────────────────────────
app.post("/guest/claim", requireAuth, async (c) => {
  const token_user_id = c.get("userId" as any) as string;
  const { guest_id, real_user_id, name = "", email = "" } = await c.req.json();

  // Enforce: the caller's session token must belong to the guest being claimed
  if (token_user_id !== guest_id) {
    return c.json({ error: "Token does not match the guest_id being claimed" }, 403);
  }

  const guestStats = await db.execute({ sql: "SELECT * FROM user_stats WHERE user_id = ?", args: [guest_id] });
  if (!guestStats.rows.length) return c.json({ error: "Guest not found" }, 404);

  const guestCoins = guestStats.rows[0].krishna_coins as number;
  const guestDays  = guestStats.rows[0].days_active as number;
  const guestLevel = guestStats.rows[0].yoga_level as number;

  const existing = await db.execute({ sql: "SELECT user_id FROM users WHERE user_id = ?", args: [real_user_id] });

  if (existing.rows.length) {
    await db.execute({
      sql: `UPDATE user_stats
            SET krishna_coins = MIN(krishna_coins + ?, 10000),
                days_active   = days_active + ?,
                yoga_level    = MAX(yoga_level, ?),
                updated_at    = datetime('now')
            WHERE user_id = ?`,
      args: [guestCoins, guestDays, guestLevel, real_user_id],
    });
    await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, ?, 'EARN', 'guest_migration', 'Guest progress transferred', datetime('now'))`, args: [real_user_id, guestCoins] });
  } else {
    await db.execute({ sql: `INSERT INTO users (user_id, name, email, is_guest) VALUES (?, ?, ?, 0)`, args: [real_user_id, name, email] });
    await db.execute({ sql: `UPDATE user_stats SET user_id = ? WHERE user_id = ?`, args: [real_user_id, guest_id] });
    await db.execute({ sql: `UPDATE coin_transactions SET user_id = ? WHERE user_id = ?`, args: [real_user_id, guest_id] });
    await db.execute({ sql: `UPDATE checkin_streaks SET user_id = ? WHERE user_id = ?`, args: [real_user_id, guest_id] });
    await db.execute({ sql: `UPDATE sloka_shares SET user_id = ? WHERE user_id = ?`, args: [real_user_id, guest_id] });
    await db.execute({ sql: `UPDATE users SET name = ?, email = ?, is_guest = 0 WHERE user_id = ?`, args: [name, email, real_user_id] });
    await db.execute({ sql: `UPDATE user_stats SET krishna_coins = MIN(krishna_coins + 150, 10000), updated_at = datetime('now') WHERE user_id = ?`, args: [real_user_id] });
    await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, 150, 'EARN', 'signup', 'Welcome bonus — new seeker', datetime('now'))`, args: [real_user_id] });
  }

  await db.execute({ sql: `DELETE FROM user_stats WHERE user_id = ?`,        args: [guest_id] });
  await db.execute({ sql: `DELETE FROM checkin_streaks WHERE user_id = ?`,   args: [guest_id] });
  await db.execute({ sql: `DELETE FROM coin_transactions WHERE user_id = ?`, args: [guest_id] });
  await db.execute({ sql: `DELETE FROM sessions WHERE user_id = ?`,          args: [guest_id] });
  await db.execute({ sql: `DELETE FROM users WHERE user_id = ?`,             args: [guest_id] });

  return c.json({ success: true, user_id: real_user_id, sync_bonus: SYNC_BONUS });
});

// ─── USERS CREATE — REMOVED (passwordless accounts cannot log in) ───────────
// Use POST /auth/register instead (requires password).
app.post("/users/create", (c) =>
  c.json({ error: "This endpoint is removed. Use POST /auth/register to create an account with a password." }, 410)
);

// ─── ADMIN KEY HELPER ─────────────────────────────────────────
function requireAdminKey(c: any): boolean {
  const adminKey = Deno.env.get("ADMIN_SECRET_KEY");
  if (!adminKey) return false; // No key configured — deny all
  const provided = c.req.header("x-admin-key") || c.req.query("admin_key");
  return provided === adminKey;
}

// ─── STREAK HELPER ─────────────────────────────────────────────
function calculateStreak(
  serverLastActivityDate: string | null,
  serverCurrentStreak: number,
  clientLastActivityDate: string,
  clientCurrentStreak: number
): { current_streak: number; last_activity_date: string } {
  const serverDate = serverLastActivityDate || "";
  const clientDate = clientLastActivityDate || "";

  const safeServerStreak = Math.max(0, serverCurrentStreak || 0);
  const safeClientStreak = Math.max(0, clientCurrentStreak || 0);

  if (!clientDate && !serverDate) {
    return { current_streak: 0, last_activity_date: new Date().toISOString().split("T")[0] };
  }

  if (!clientDate) {
    return { current_streak: safeServerStreak, last_activity_date: serverDate };
  }

  if (!serverDate) {
    return { current_streak: safeClientStreak, last_activity_date: clientDate };
  }

  // Parse YYYY-MM-DD to UTC timestamps (midnight)
  const parseDate = (d: string) => new Date(d.substring(0, 10) + "T00:00:00Z").getTime();
  const sTime = parseDate(serverDate);
  const cTime = parseDate(clientDate);
  
  if (isNaN(sTime) || isNaN(cTime)) {
    return { current_streak: safeServerStreak, last_activity_date: serverDate }; // fallback if dates are malformed
  }

  const msPerDay = 1000 * 60 * 60 * 24;
  const dayDiff = Math.floor((cTime - sTime) / msPerDay);

  if (dayDiff < 0) {
    // Client date is older than server date (e.g. syncing old offline data after a newer sync)
    return { current_streak: Math.max(safeServerStreak, safeClientStreak), last_activity_date: serverDate };
  } else if (dayDiff === 0) {
    // Same day: allow client to increment streak (e.g. 5 -> 6) or restore streak
    const maxAllowed = Math.max(safeServerStreak + 1, safeClientStreak);
    const newStreak = Math.min(Math.max(safeServerStreak, safeClientStreak), maxAllowed);
    return { 
      current_streak: newStreak, 
      last_activity_date: clientDate.substring(0, 10) || serverDate.substring(0, 10) 
    };
  } else {
    // Client date is 1+ days newer
    const maxValidStreak = Math.max(safeServerStreak + dayDiff, safeClientStreak);
    const validatedStreak = Math.min(Math.max(safeServerStreak + 1, safeClientStreak), maxValidStreak);
    return { current_streak: validatedStreak, last_activity_date: clientDate.substring(0, 10) };
  }
}

// ─── USERS STATS SYNC ──────────────────────────────────────────
app.post("/users/stats/sync", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const {
    current_streak = 0,
    longest_streak = 0,
    total_quizzes_taken = 0,
    total_questions_answered = 0,
    total_correct_answers = 0,
    verses_read = 0,
    chapters_completed = 0,
    last_activity_date = "",
    country_code
  } = await c.req.json();

  if (country_code) await updateUserCountry(user_id, country_code);

  // Fetch current server stats for comparison
  const serverStats = await db.execute({
    sql: `SELECT current_streak, longest_streak, total_quizzes_taken, total_questions_answered,
                 total_correct_answers, verses_read, chapters_completed, last_activity_date, updated_at
          FROM user_stats WHERE user_id = ?`,
    args: [user_id]
  });

  const server = serverStats.rows[0] || {};

  // ── Streak: calculate server-side using date-based logic ──
  const streakResult = calculateStreak(
    server.last_activity_date as string | null,
    (server.current_streak as number) || 0,
    last_activity_date,
    current_streak
  );

  const newLongestStreak = Math.max(
    streakResult.current_streak,
    (server.longest_streak as number) || 0,
    longest_streak
  );

  // ── Cumulative stats: MAX() is correct for these (monotonically increasing) ──
  const newQuizzesTaken = Math.max((server.total_quizzes_taken as number) || 0, total_quizzes_taken);
  const newQuestionsAnswered = Math.max((server.total_questions_answered as number) || 0, total_questions_answered);
  const newCorrectAnswers = Math.max((server.total_correct_answers as number) || 0, total_correct_answers);
  const newVersesRead = Math.max((server.verses_read as number) || 0, verses_read);
  const newChaptersCompleted = Math.max((server.chapters_completed as number) || 0, chapters_completed);

  await db.execute({
    sql: `UPDATE user_stats
          SET current_streak = ?,
              longest_streak = ?,
              total_quizzes_taken = ?,
              total_questions_answered = ?,
              total_correct_answers = ?,
              verses_read = ?,
              chapters_completed = ?,
              last_activity_date = ?,
              updated_at = datetime('now')
          WHERE user_id = ?`,
    args: [
      streakResult.current_streak,
      newLongestStreak,
      newQuizzesTaken,
      newQuestionsAnswered,
      newCorrectAnswers,
      newVersesRead,
      newChaptersCompleted,
      streakResult.last_activity_date,
      user_id
    ]
  });

  // ── Return authoritative server state to client ──
  const updatedStats = await db.execute({
    sql: `SELECT current_streak, longest_streak, total_quizzes_taken, total_questions_answered,
                 total_correct_answers, verses_read, chapters_completed, last_activity_date, updated_at
          FROM user_stats WHERE user_id = ?`,
    args: [user_id]
  });

  return c.json({ success: true, stats: updatedStats.rows[0] || {} });
});

// ─── COINS BALANCE ────────────────────────────────────────────
app.get("/coins/balance", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;

  const result = await db.execute({
    sql: `SELECT us.krishna_coins, us.days_active,
            us.current_streak, us.longest_streak, us.total_quizzes_taken, 
            us.total_questions_answered, us.total_correct_answers, us.best_score, us.best_score_out_of, us.verses_read, us.chapters_completed,
            us.last_activity_date, us.updated_at,
            yl.name as yoga_name, yl.multiplier,
            CASE WHEN us.krishna_coins >= 10000 THEN 1 ELSE 0 END as is_max,
            COALESCE(cs.current_day, 0) as checkin_day, COALESCE(cs.current_week, 1) as checkin_week,
            COALESCE(cs.share_day, 0) as share_day, COALESCE(cs.share_week, 1) as share_week,
            COALESCE(cs.last_checkin, '') as last_checkin, COALESCE(cs.last_share, '') as last_share
          FROM user_stats us
          JOIN yoga_levels yl ON yl.level = us.yoga_level
          LEFT JOIN checkin_streaks cs ON cs.user_id = us.user_id
          WHERE us.user_id = ?`,
    args: [user_id as string],
  });
  if (!result.rows.length) return c.json({ error: "User not found" }, 404);
  return c.json(result.rows[0]);
});

/** Battle reward: same Fibonacci as app BattleState.battleCoins(correctAnswers). */
function battleFibCoins(correctAnswers: number): number {
  const n = Math.max(0, Math.floor(correctAnswers));
  if (n <= 0) return 0;
  let a = 1;
  let b = 1;
  for (let i = 3; i <= n; i++) {
    const temp = a + b;
    a = b;
    b = temp;
  }
  return n === 1 ? a : b;
}

// ─── COINS AWARD ──────────────────────────────────────────────
app.post("/coins/award", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { source, metadata, client_date, country_code } = await c.req.json();
  if (country_code) await updateUserCountry(user_id, country_code);
  const dbDate = client_date ? client_date : new Date().toISOString();
  const rule = await db.execute({ sql: "SELECT base_coins, max_coins FROM coin_rules WHERE source = ?", args: [source] });
  if (!rule.rows.length) return c.json({ error: "Unknown source" }, 400);

  let coins = rule.rows[0].base_coins as number;
  const maxCoins = rule.rows[0].max_coins as number | null;

  if (source === "quiz_completion" && metadata?.accuracy != null) {
    const accuracy      = Math.max(0, Math.min(1, metadata.accuracy));
    // Tiered: <50%→1, 50%→2, 60%→3, 70%→4, 80%→5, 90/100%→6
    const accuracyBonus = accuracy >= 0.9 ? 6 : accuracy >= 0.8 ? 5 : accuracy >= 0.7 ? 4 : accuracy >= 0.6 ? 3 : accuracy >= 0.5 ? 2 : 1;
    coins = coins + accuracyBonus;
    if (maxCoins != null) coins = Math.min(coins, maxCoins);
  }

  // Battle: server recomputes Fibonacci from correct count (metadata.score), not client sum
  if (source === "battle_quiz") {
    const correct = Math.max(0, Math.floor(Number(metadata?.score ?? 0)));
    coins = Math.min(battleFibCoins(correct), maxCoins ?? 1000);
  }

  const userStats = await db.execute({
    sql: `SELECT yl.multiplier FROM user_stats us JOIN yoga_levels yl ON yl.level = us.yoga_level WHERE us.user_id = ?`,
    args: [user_id],
  });
  const multiplier = userStats.rows.length
    ? Number(userStats.rows[0].multiplier) || 1
    : 1;
  const coinsBeforeYoga = coins;
  // Softened ladder (1.5 / 2.5): round to nearest coin (1×1.5 → 2, not 1)
  coins = Math.max(0, Math.round(coins * multiplier));

  // Human-readable description for app + admin dashboard (not raw JSON metadata)
  let description = source.replace(/_/g, " ");
  if (source === "quiz_completion" && metadata) {
    const totalQ = metadata.totalQuestions ?? 0;
    const scoreQ = metadata.score ?? 0;
    const quizType = metadata.quizType ?? "general";
    description = totalQ > 0
      ? `Quiz (${quizType}): ${scoreQ}/${totalQ}`
      : `Quiz (${quizType}): ${scoreQ} correct`;
  } else if (source === "battle_quiz" && metadata) {
    const scoreQ = Math.max(0, Math.floor(Number(metadata?.score ?? 0)));
    description = multiplier > 1
      ? `Battle quiz: ${scoreQ} correct (+${coinsBeforeYoga}×${multiplier}=${coins})`
      : `Battle quiz: ${scoreQ} correct (+${coins})`;
  } else if (source === "chapter_completion") {
    description = "Chapter completed";
  }

  await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, ?, 'EARN', ?, ?, ?)`, args: [user_id, coins, source, description, dbDate] });
  await db.execute({ sql: `UPDATE user_stats SET krishna_coins = MIN(krishna_coins + ?, 10000), updated_at = datetime('now') WHERE user_id = ?`, args: [coins, user_id] });

  // Store quiz stats in user_stats
  if (source === "quiz_completion" && metadata) {
    const totalQ = metadata.totalQuestions ?? 0;
    const scoreQ = metadata.score ?? 0;
    const quizType = metadata.quizType ?? "general";
    await db.execute({
      sql: `UPDATE user_stats SET 
        total_quizzes_taken = total_quizzes_taken + 1,
        total_questions_answered = total_questions_answered + ?,
        total_correct_answers = total_correct_answers + ?,
        best_score = MAX(best_score, ?),
        best_score_out_of = CASE WHEN ? > best_score THEN ? ELSE best_score_out_of END
      WHERE user_id = ?`,
      args: [totalQ, scoreQ, scoreQ, scoreQ, totalQ, user_id]
    });
    const attempt_id = metadata?.attemptId ?? null;
    const language = metadata?.language ?? 'en';
    const accuracy = totalQ > 0 ? Math.round((scoreQ / totalQ) * 100) : 0;
    await db.execute({
      sql: `INSERT OR IGNORE INTO quiz_attempts (user_id, score, total_questions, quiz_type, time_spent_seconds, avg_time_per_question, coins_earned, accuracy, created_at, attempt_id, language) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      args: [user_id, scoreQ, totalQ, quizType, 0, 0, coins, accuracy, new Date().toISOString(), attempt_id, language]
    });
  }

  // Store battle quiz stats in user_stats
  if (source === "battle_quiz" && metadata) {
    const totalQ = metadata.questionsAnswered ?? 0;
    const scoreQ = metadata.score ?? 0;
    const timeSpent = metadata.timeSpentSeconds ?? 60;
    const avgTime = totalQ > 0 ? Math.round(timeSpent / totalQ) : 0;

    await db.execute({
      sql: `UPDATE user_stats SET 
        total_quizzes_taken = total_quizzes_taken + 1,
        total_questions_answered = total_questions_answered + ?,
        total_correct_answers = total_correct_answers + ?
      WHERE user_id = ?`,
      args: [totalQ, scoreQ, user_id]
    });
    const attempt_id = metadata?.attemptId ?? null;
    const language = metadata?.language ?? 'en';
    const accuracy = totalQ > 0 ? Math.round((scoreQ / totalQ) * 100) : 0;
    await db.execute({
      sql: `INSERT OR IGNORE INTO quiz_attempts (user_id, score, total_questions, quiz_type, time_spent_seconds, avg_time_per_question, coins_earned, accuracy, created_at, attempt_id, language) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      args: [user_id, scoreQ, totalQ, "battle_quiz", timeSpent, avgTime, coins, accuracy, new Date().toISOString(), attempt_id, language]
    });
  }

  const stats = await db.execute({ sql: `SELECT krishna_coins, yoga_level FROM user_stats WHERE user_id = ?`, args: [user_id] });
  const totalCoins   = stats.rows[0].krishna_coins as number;
  const currentLevel = stats.rows[0].yoga_level as number;

  const newLevel     = totalCoins >= 9000 ? 5 : totalCoins >= 6000 ? 4 : totalCoins >= 3000 ? 3 : totalCoins >= 1000 ? 2 : 1;

  if (newLevel > currentLevel) {
    await db.execute({ sql: `UPDATE user_stats SET yoga_level = ? WHERE user_id = ?`, args: [newLevel, user_id] });
    await db.execute({ sql: `INSERT INTO level_history (user_id, from_level, to_level, coins_at_levelup, bonus_coins) VALUES (?, ?, ?, ?, 10)`, args: [user_id, currentLevel, newLevel, totalCoins] });
    await db.execute({ sql: `UPDATE user_stats SET krishna_coins = MIN(krishna_coins + 10, 10000) WHERE user_id = ?`, args: [user_id] });
    await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, 10, 'EARN', 'level_up_bonus', 'Level up reward', datetime('now'))`, args: [user_id] });
  }

  return c.json({ awarded: coins, total_coins: totalCoins, levelled_up: newLevel > currentLevel, new_level: newLevel, multiplier });
});

// ─── COINS SPEND ──────────────────────────────────────────────
app.post("/coins/spend", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { question, idempotency_key, client_date, country_code } = await c.req.json();
  if (country_code) await updateUserCountry(user_id, country_code);
  const dbDate = client_date ? client_date : new Date().toISOString();

  if (idempotency_key) {
    const existing = await db.execute({
      sql: `SELECT id FROM coin_transactions WHERE user_id = ? AND idempotency_key = ? LIMIT 1`,
      args: [user_id, idempotency_key]
    });
    if (existing.rows.length) {
      const stats = await db.execute({ sql: "SELECT krishna_coins FROM user_stats WHERE user_id = ?", args: [user_id] });
      return c.json({
        spent: 0,
        label: "duplicate",
        remaining_balance: stats.rows[0]?.krishna_coins as number ?? 0,
        duplicate: true
      });
    }
  }

  const length = question.length;
  const rule   = await db.execute({ sql: `SELECT coins, label FROM voice_chat_rules WHERE min_chars <= ? AND (max_chars IS NULL OR max_chars >= ?)`, args: [length, length] });

  const cost = rule.rows.length > 0 ? (rule.rows[0].coins as number) : 2;
  const label = rule.rows.length > 0 ? (rule.rows[0].label as string) : "Short";

  const stats  = await db.execute({ sql: "SELECT krishna_coins FROM user_stats WHERE user_id = ?", args: [user_id] });
  if (!stats.rows.length) return c.json({ error: "User not found" }, 404);
  const balance = stats.rows[0].krishna_coins as number;
  if (balance < cost) return c.json({ error: "Not enough coins", balance, required: cost }, 400);
  await db.execute({ sql: `UPDATE user_stats SET krishna_coins = MAX(krishna_coins - ?, 0), updated_at = datetime('now') WHERE user_id = ?`, args: [cost, user_id] });
  await db.execute({
    sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, idempotency_key, created_at) VALUES (?, ?, 'SPEND', 'voice_chat', ?, ?, ?)`,
    args: [user_id, -cost, `Voice chat - ${label} (${length} chars)`, idempotency_key || null, dbDate]
  });
  const updatedBalance = Math.max(balance - cost, 0);
  return c.json({ spent: cost, label, remaining_balance: updatedBalance, duplicate: false });
});

// ─── CHECKIN ──────────────────────────────────────────────────
app.post("/checkin", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { idempotency_key: rawKey, client_date, country_code, timezone } = await c.req.json();
  if (country_code) await updateUserCountry(user_id, country_code);
  const dbDate = new Date().toISOString();
  const clientDateStr = (client_date ? String(client_date).trim().substring(0, 10) : "");
  const userTz = getTimezone(c, country_code, timezone);
  const today = (clientDateStr.length === 10 && /^\d{4}-\d{2}-\d{2}$/.test(clientDateStr)) ? clientDateStr : getLocalDate(userTz);
  const idempotency_key = rawKey || `checkin_${user_id}_${today}`;

  // Always check idempotency first — prevents race conditions
  const existing = await db.execute({
    sql: `SELECT id, amount, description FROM coin_transactions WHERE user_id = ? AND idempotency_key = ? LIMIT 1`,
    args: [user_id, idempotency_key]
  });
  const streak = await db.execute({ sql: "SELECT * FROM checkin_streaks WHERE user_id = ?", args: [user_id] });
  const existingDay = streak.rows.length ? ((streak.rows[0].current_day as number) || 1) : 1;
  const existingWeek = streak.rows.length ? ((streak.rows[0].current_week as number) || 1) : 1;

  if (existing.rows.length) {
    const stats = await db.execute({ sql: "SELECT krishna_coins FROM user_stats WHERE user_id = ?", args: [user_id] });
    return c.json({
      day: existingDay === 0 ? 7 : existingDay,
      week: existingWeek,
      coins_awarded: 0,
      weekly_bonus: 0,
      duplicate: true,
      message: "Already checked in",
      remaining_balance: stats.rows.length ? (stats.rows[0].krishna_coins as number) : 0
    });
  }

  let current_day = 1;
  let current_week = 1;

  if (streak.rows.length) {
    const rawCheckin = String(streak.rows[0].last_checkin || "").trim();
    const lastCheckinDate = rawCheckin.substring(0, 10);

    if (lastCheckinDate === today) {
      const stats = await db.execute({ sql: "SELECT krishna_coins FROM user_stats WHERE user_id = ?", args: [user_id] });
      return c.json({
        day: existingDay === 0 ? 7 : existingDay,
        week: existingWeek,
        coins_awarded: 0,
        weekly_bonus: 0,
        duplicate: true,
        message: "Already checked in today",
        remaining_balance: stats.rows.length ? (stats.rows[0].krishna_coins as number) : 0
      });
    }

    current_day = ((streak.rows[0].current_day as number) % 7) + 1;
    current_week = (streak.rows[0].current_week as number) || 1;

    if (lastCheckinDate) {
      const tDate = new Date(today + "T12:00:00Z");
      tDate.setUTCDate(tDate.getUTCDate() - 1);
      const yesterdayDate = tDate.toISOString().substring(0, 10);
      tDate.setUTCDate(tDate.getUTCDate() - 1);
      const dayBeforeYesterdayDate = tDate.toISOString().substring(0, 10);

      if (lastCheckinDate !== yesterdayDate && lastCheckinDate !== dayBeforeYesterdayDate) {
        current_day = 1;
        current_week = 1;
      }
    }
  }

  const reward = await db.execute({ sql: "SELECT coins FROM checkin_rewards WHERE day = ?", args: [current_day] });
  let coins = reward.rows[0].coins as number;
  let weekly_bonus = 0;
  if (current_day === 7) {
    const bonusRule = await db.execute({ sql: "SELECT coins FROM weekly_bonus_rules WHERE week = ?", args: [current_week] });
    weekly_bonus = bonusRule.rows[0].coins as number;
    coins += weekly_bonus;
  }
  const next_week = current_day === 7 ? (current_week % 4) + 1 : current_week;
  await db.execute({ sql: `INSERT INTO checkin_streaks (user_id, current_day, current_week, last_checkin) VALUES (?, ?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET current_day = ?, current_week = ?, last_checkin = ?`, args: [user_id, current_day, next_week, today, current_day, next_week, today] });
  await db.execute({ sql: `UPDATE user_stats SET krishna_coins = MIN(krishna_coins + ?, 10000), days_active = days_active + 1, updated_at = datetime('now') WHERE user_id = ?`, args: [coins, user_id] });
  await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, idempotency_key, created_at) VALUES (?, ?, 'EARN', 'checkin_day', ?, ?, ?)`, args: [user_id, coins, `Day ${current_day} check-in`, idempotency_key, dbDate] });
  const newBalance = await getUserBalance(user_id);
  return c.json({ day: current_day, week: current_week, coins_awarded: coins - weekly_bonus, weekly_bonus, total_coins: newBalance, duplicate: false });
});

// ─── SHARE ────────────────────────────────────────────────────
app.post("/share", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { sloka_id = null, chapter = null, verse = null, idempotency_key: rawKey, client_date, country_code, timezone } = await c.req.json();
  if (country_code) await updateUserCountry(user_id, country_code);
  const dbDate = new Date().toISOString();
  const clientDateStr = (client_date ? String(client_date).trim().substring(0, 10) : "");
  const userTz = getTimezone(c, country_code, timezone);
  const today = (clientDateStr.length === 10 && /^\d{4}-\d{2}-\d{2}$/.test(clientDateStr)) ? clientDateStr : getLocalDate(userTz);
  const idempotency_key = rawKey || `share_${user_id}_${today}`;

  // Always check idempotency first — prevents race conditions
  const existing = await db.execute({
    sql: `SELECT id, amount, description FROM coin_transactions WHERE user_id = ? AND idempotency_key = ? LIMIT 1`,
    args: [user_id, idempotency_key]
  });
  const streak = await db.execute({ sql: "SELECT * FROM checkin_streaks WHERE user_id = ?", args: [user_id] });
  const existingShareDay = streak.rows.length ? ((streak.rows[0].share_day as number) || 1) : 1;
  const existingShareWeek = streak.rows.length ? ((streak.rows[0].share_week as number) || 1) : 1;

  if (existing.rows.length) {
    const stats = await db.execute({ sql: "SELECT krishna_coins FROM user_stats WHERE user_id = ?", args: [user_id] });
    return c.json({
      share_day: existingShareDay === 0 ? 7 : existingShareDay,
      share_week: existingShareWeek,
      coins_awarded: 0,
      weekly_bonus: 0,
      duplicate: true,
      message: "Already shared",
      remaining_balance: stats.rows.length ? (stats.rows[0].krishna_coins as number) : 0
    });
  }

  let share_day = 1;
  let share_week = 1;

  if (streak.rows.length) {
    const rawShare = String(streak.rows[0].last_share || "").trim();
    const lastShareDate = rawShare.substring(0, 10);

    if (lastShareDate === today) {
      const stats = await db.execute({ sql: "SELECT krishna_coins FROM user_stats WHERE user_id = ?", args: [user_id] });
      return c.json({
        share_day: existingShareDay === 0 ? 7 : existingShareDay,
        share_week: existingShareWeek,
        coins_awarded: 0,
        weekly_bonus: 0,
        duplicate: true,
        message: "Already shared today",
        remaining_balance: stats.rows.length ? (stats.rows[0].krishna_coins as number) : 0
      });
    }

    share_day = ((streak.rows[0].share_day as number) % 7) + 1;
    share_week = (streak.rows[0].share_week as number) || 1;

    if (lastShareDate) {
      const tDate = new Date(today + "T12:00:00Z");
      tDate.setUTCDate(tDate.getUTCDate() - 1);
      const yesterdayDate = tDate.toISOString().substring(0, 10);
      tDate.setUTCDate(tDate.getUTCDate() - 1);
      const dayBeforeYesterdayDate = tDate.toISOString().substring(0, 10);

      if (lastShareDate !== yesterdayDate && lastShareDate !== dayBeforeYesterdayDate) {
        share_day = 1;
        share_week = 1;
      }
    }
  }

  const reward = await db.execute({ sql: "SELECT coins FROM checkin_rewards WHERE day = ?", args: [share_day] });
  let coins = reward.rows[0].coins as number;
  let weekly_bonus = 0;
  if (share_day === 7) {
    const bonusRule = await db.execute({ sql: "SELECT coins FROM weekly_bonus_rules WHERE week = ?", args: [share_week] });
    weekly_bonus = bonusRule.rows[0].coins as number;
    coins += weekly_bonus;
  }
  const next_week = share_day === 7 ? (share_week % 4) + 1 : share_week;
  
  const safe_sloka_id = sloka_id || "app_share";
  const safe_chapter = chapter || 0;
  const safe_verse = verse || 0;
  
  await db.execute({
    sql: `INSERT INTO checkin_streaks (user_id, current_day, current_week, share_day, share_week, last_share)
          VALUES (?, 0, 1, ?, ?, ?)
          ON CONFLICT(user_id) DO UPDATE SET
          share_day = excluded.share_day,
          share_week = excluded.share_week,
          last_share = excluded.last_share`,
    args: [user_id, share_day, next_week, today]
  });
  await db.execute({ sql: `INSERT INTO sloka_shares (user_id, shared_at, sloka_id, chapter, verse, coins, share_day, share_week) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`, args: [user_id, today, safe_sloka_id, safe_chapter, safe_verse, coins, share_day, share_week] });
  await db.execute({ sql: `UPDATE user_stats SET krishna_coins = MIN(krishna_coins + ?, 10000), updated_at = datetime('now') WHERE user_id = ?`, args: [coins, user_id] });
  await db.execute({ sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, idempotency_key, created_at) VALUES (?, ?, 'EARN', 'share_sloka', ?, ?, ?)`, args: [user_id, coins, `Share day ${share_day}`, idempotency_key, dbDate] });
  const shareBalance = await getUserBalance(user_id);
  return c.json({ share_day, share_week, coins_awarded: coins - weekly_bonus, weekly_bonus, total_coins: shareBalance, duplicate: false });
});

// ─── COINS HISTORY ────────────────────────────────────────────
app.get("/coins/history", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;

  // Default 100 (was 500) — old APKs that pass limit=500 still get up to max 500
  const limitParam  = parseInt(c.req.query("limit")  ?? "100");
  const offsetParam = parseInt(c.req.query("offset") ?? "0");
  const limit  = Math.min(Math.max(isNaN(limitParam)  ? 100 : limitParam,  1), 500);
  const offset = Math.max(isNaN(offsetParam) ? 0 : offsetParam, 0);
  // Cursor pagination preferred: before_id = last id from previous page (avoids deep OFFSET scans)
  const beforeIdParam = parseInt(c.req.query("before_id") ?? "");
  const beforeId = !isNaN(beforeIdParam) && beforeIdParam > 0 ? beforeIdParam : null;

  if (beforeId != null) {
    const result = await db.execute({
      sql: `SELECT id, amount, type, source, description, idempotency_key, created_at
            FROM coin_transactions
            WHERE user_id = ? AND source != 'auto_reconcile' AND id < ?
            ORDER BY id DESC
            LIMIT ?`,
      args: [user_id as string, beforeId, limit],
    });
    return c.json(result.rows);
  }

  // First page (or legacy offset). Prefer offset=0; deep OFFSET still supported for old clients.
  const result = await db.execute({
    sql: `SELECT id, amount, type, source, description, idempotency_key, created_at
          FROM coin_transactions
          WHERE user_id = ? AND source != 'auto_reconcile'
          ORDER BY id DESC
          LIMIT ? OFFSET ?`,
    args: [user_id as string, limit, offset],
  });
  return c.json(result.rows);
});

// ─── LEADERBOARD ──────────────────────────────────────────────
app.get("/coins/leaderboard", async (c) => {
  const result = await db.execute(`
    SELECT u.name, us.krishna_coins, yl.name AS yoga_level,
      RANK() OVER (ORDER BY us.krishna_coins DESC) AS rank
    FROM user_stats us
    JOIN users u ON u.user_id = us.user_id
    JOIN yoga_levels yl ON yl.level = us.yoga_level
    WHERE u.is_guest = 0
    ORDER BY us.krishna_coins DESC LIMIT 50
  `);
  return c.json(result.rows);
});

// ─── YOGA STAGES ──────────────────────────────────────────────
app.get("/yoga/stages", async (c) => {
  const levels    = await db.execute("SELECT * FROM yoga_levels ORDER BY level ASC");
  const subStages = await db.execute("SELECT * FROM yoga_sub_stages ORDER BY level ASC, sub_level ASC");
  return c.json({ levels: levels.rows, sub_stages: subStages.rows });
});

// ─── VOICE CHAT COST (for guests) ──────────────────────────────
app.get("/coins/voice-cost", async (c) => {
  const question = c.req.query("question") || "";
  const length = question.length;
  const rule = await db.execute({
    sql: `SELECT coins, label FROM voice_chat_rules WHERE min_chars <= ? AND (max_chars IS NULL OR max_chars >= ?)`,
    args: [length, length]
  });

  if (!rule.rows.length) {
    return c.json({ cost: 2, label: "Short", error: "No rule found, using default" });
  }

  return c.json({
    cost: rule.rows[0].coins as number,
    label: rule.rows[0].label as string,
    length: length
  });
});

// ─── QUIZ ATTEMPTS ────────────────────────────────────────────
app.post("/quiz/attempt", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { score, total_questions, quiz_type = "general", time_spent_seconds = 0, coins_earned = 0, client_date, country_code, attempt_id, language = "en" } = await c.req.json();
  if (country_code) await updateUserCountry(user_id, country_code);
  const dbDate = client_date ? client_date : new Date().toISOString();
  const avg_time = total_questions > 0 ? Math.round(time_spent_seconds / total_questions) : 0;
  const accuracy = total_questions > 0 ? Math.round((score / total_questions) * 100) : 0;
  await db.execute({
    sql: `INSERT OR IGNORE INTO quiz_attempts (user_id, score, total_questions, quiz_type, time_spent_seconds, avg_time_per_question, coins_earned, accuracy, created_at, attempt_id, language) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    args: [user_id, score, total_questions, quiz_type, time_spent_seconds, avg_time, coins_earned, accuracy, dbDate, attempt_id, language],
  });
  return c.json({ success: true });
});

app.get("/quiz/history", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  // Default 50, max 200 — old clients asking for more are capped (shorter list, no crash)
  const limit = Math.min(Math.max(parseInt(c.req.query("limit") ?? "50") || 50, 1), 200);
  const result = await db.execute({
    sql: `SELECT id, score, total_questions, quiz_type, time_spent_seconds, avg_time_per_question, coins_earned, accuracy, created_at, attempt_id, language FROM quiz_attempts WHERE user_id = ? ORDER BY created_at DESC LIMIT ?`,
    args: [user_id, limit],
  });
  return c.json(result.rows);
});

app.get("/activity/history", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  // Only last ~90 calendar days of raw rows (not full-history GROUP BY then LIMIT)
  const result = await db.execute({
    sql: `SELECT DATE(created_at) as date,
            COUNT(CASE WHEN source = 'checkin_day' THEN 1 END) as checkins,
            COUNT(CASE WHEN source IN ('quiz_completion', 'battle_quiz') THEN 1 END) as quizzes,
            COUNT(CASE WHEN source = 'share_sloka' THEN 1 END) as shares,
            COUNT(CASE WHEN source = 'meditation' THEN 1 END) as meditations,
            COUNT(CASE WHEN source = 'voice_chat' THEN 1 END) as voice_chats,
            COUNT(CASE WHEN source = 'battle_quiz' THEN 1 END) as battle_quizzes,
            COUNT(*) as total_events
          FROM coin_transactions
          WHERE user_id = ? AND type = 'EARN'
            AND date(created_at) >= date('now', '-90 days')
          GROUP BY DATE(created_at)
          ORDER BY date DESC
          LIMIT 90`,
    args: [user_id],
  });
  return c.json(result.rows);
});

// ─── VERSE NOTES ──────────────────────────────────────────────
app.post("/notes/save", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { chapter_no, verse_no, note } = await c.req.json();
  if (chapter_no == null || verse_no == null || !note) {
    return c.json({ error: "chapter_no, verse_no, note required" }, 400);
  }

  // Enforce 200 character limit
  const trimmedNote = String(note).trim().slice(0, 200);

  await db.execute({
    sql: `INSERT INTO verse_notes (user_id, chapter_no, verse_no, note, updated_at)
          VALUES (?, ?, ?, ?, datetime('now'))
          ON CONFLICT(user_id, chapter_no, verse_no) DO UPDATE SET note = ?, updated_at = datetime('now')`,
    args: [user_id, chapter_no, verse_no, trimmedNote, trimmedNote],
  });

  return c.json({ success: true, note: trimmedNote });
});

app.get("/notes/list", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const result = await db.execute({
    sql: `SELECT id, chapter_no, verse_no, note, created_at, updated_at FROM verse_notes WHERE user_id = ? ORDER BY chapter_no, verse_no`,
    args: [user_id],
  });
  return c.json(result.rows);
});

// (first /notes/sync removed — canonical version is below at line ~1412)

// ─── ADMIN RESET STATS ────────────────────────────────────────
app.post("/admin/reset-stats", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const { user_id, current_streak = 1, longest_streak = 1, total_quizzes_taken = 0, total_questions_answered = 0, total_correct_answers = 0, best_score = 0, best_score_out_of = 0, verses_read = 0, chapters_completed = 0 } = await c.req.json();
  if (!user_id) return c.json({ error: "user_id required" }, 400);
  await db.execute({
    sql: `UPDATE user_stats SET current_streak = ?, longest_streak = ?, total_quizzes_taken = ?, total_questions_answered = ?, total_correct_answers = ?, best_score = ?, best_score_out_of = ?, verses_read = ?, chapters_completed = ?, updated_at = datetime('now') WHERE user_id = ?`,
    args: [current_streak, longest_streak, total_quizzes_taken, total_questions_answered, total_correct_answers, best_score, best_score_out_of, verses_read, chapters_completed, user_id],
  });
  return c.json({ success: true, message: `Stats reset for ${user_id}` });
});

// ─── ADMIN VERIFY (cheap key check — use this for unlock) ─────
app.get("/admin/verify", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  return c.json({ success: true, ok: true });
});

// ─── ADMIN CLEANUP (explicit only — heavy; do NOT call on unlock) ─
// POST preferred; GET kept but returns 405 with message so old unlock can't silently scan.
async function runAdminCleanDuplicates(c: any) {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  try {
    // 1. Delete duplicate coin transactions for battle_quiz
    await db.execute(`
      DELETE FROM coin_transactions 
      WHERE id NOT IN (
        SELECT MIN(id) FROM coin_transactions 
        WHERE source = 'battle_quiz' 
        GROUP BY user_id, amount, created_at
      ) AND source = 'battle_quiz';
    `);

    // 2. Delete duplicate quiz attempts
    await db.execute(`
      DELETE FROM quiz_attempts 
      WHERE id NOT IN (
        SELECT MIN(id) FROM quiz_attempts 
        WHERE quiz_type = 'battle_quiz' 
        GROUP BY user_id, score, total_questions, created_at
      ) AND quiz_type = 'battle_quiz';
    `);

    // 3. Fix the 0 total_questions records
    await db.execute(`
      UPDATE quiz_attempts 
      SET total_questions = score, 
          time_spent_seconds = 60,
          accuracy = 100,
          avg_time_per_question = CASE WHEN score > 0 THEN 60 / score ELSE 0 END
      WHERE quiz_type = 'battle_quiz' AND total_questions = 0;
    `);

    return c.json({ success: true, message: "Turso database duplicates and 0s cleaned successfully!" });
  } catch (e: any) {
    return c.json({ success: false, error: e.message });
  }
}

app.post("/admin/clean-duplicates", runAdminCleanDuplicates);
app.get("/admin/clean-duplicates", async (c) => {
  // Block accidental unlock-style GET cleanup (was scanning huge tables)
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  return c.json({
    error: "Use POST /admin/clean-duplicates for cleanup. GET is disabled to prevent unlock-time full scans. Use GET /admin/verify to check the admin key.",
  }, 405);
});

app.get("/notes", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const result = await db.execute({
    sql: `SELECT id, chapter_no, verse_no, note, created_at, updated_at FROM verse_notes WHERE user_id = ? ORDER BY updated_at DESC`,
    args: [user_id],
  });
  return c.json(result.rows);
});

app.post("/notes/sync", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const body = await c.req.json();
  const notes = body?.notes;
  if (!Array.isArray(notes)) return c.json({ error: "notes array required" }, 400);

  // Cap batch size to avoid huge free-tier spikes
  const MAX_NOTES = 100;
  const batch = notes.slice(0, MAX_NOTES);
  let synced = 0;

  // Bulk upsert — no per-note SELECT (unique index on user_id, chapter_no, verse_no)
  for (const note of batch) {
    const chapterNo = note.chapterNo ?? note.chapter_no;
    const verseNo = note.verseNo ?? note.verse_no;
    const text = String(note.note ?? "").trim().slice(0, 200);
    if (chapterNo == null || verseNo == null || !text) continue;

    await db.execute({
      sql: `INSERT INTO verse_notes (user_id, chapter_no, verse_no, note, updated_at)
            VALUES (?, ?, ?, ?, datetime('now'))
            ON CONFLICT(user_id, chapter_no, verse_no) DO UPDATE SET
              note = excluded.note,
              updated_at = datetime('now')`,
      args: [user_id, chapterNo, verseNo, text],
    });
    synced++;
  }
  return c.json({ success: true, synced, truncated: notes.length > MAX_NOTES });
});

app.post("/notes/delete", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { chapter_no, verse_no } = await c.req.json();
  await db.execute({
    sql: `DELETE FROM verse_notes WHERE user_id = ? AND chapter_no = ? AND verse_no = ?`,
    args: [user_id, chapter_no, verse_no],
  });
  return c.json({ success: true });
});

// ─── ADMIN RESTORE STREAK ─────────────────────────────────────
app.post("/admin/restore-streak", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const { user_id, target_streak } = await c.req.json();
  if (!user_id) return c.json({ error: "user_id required" }, 400);

  const stats = await db.execute({
    sql: `SELECT longest_streak FROM user_stats WHERE user_id = ?`,
    args: [user_id],
  });
  if (!stats.rows.length) return c.json({ error: "User not found" }, 404);

  const longest = (stats.rows[0].longest_streak as number) || 1;
  const newStreak = target_streak ? Number(target_streak) : Math.max(longest, 1);
  const newLongest = Math.max(longest, newStreak);
  const todayStr = new Date().toISOString().split("T")[0];

  await db.execute({
    sql: `UPDATE user_stats SET current_streak = ?, longest_streak = ?, last_activity_date = ?, updated_at = datetime('now') WHERE user_id = ?`,
    args: [newStreak, newLongest, todayStr, user_id],
  });

  return c.json({ success: true, user_id, current_streak: newStreak, longest_streak: newLongest });
});

// ─── MEDITATION TIME TRACKER & REWARDS ────────────────────────
app.post("/meditation/log", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { minutes } = await c.req.json();
  if (!minutes) return c.json({ error: "minutes required" }, 400);

  const min = Math.max(1, Number(minutes));
  // Reward Rule: 5 min -> 10 coins, 10 min -> 20 coins, 15 min -> 30 coins, 20 min -> 40 coins
  const coinsEarned = Math.min(40, Math.floor(min / 5) * 10);
  const todayStr = new Date().toISOString().split("T")[0];

  await db.execute({
    sql: `INSERT INTO meditation_sessions (user_id, minutes, coins_earned, session_date) VALUES (?, ?, ?, ?)`,
    args: [user_id, min, coinsEarned, todayStr],
  });

  if (coinsEarned > 0) {
    await db.execute({
      sql: `UPDATE user_stats SET krishna_coins = MIN(krishna_coins + ?, 10000), last_activity_date = ?, updated_at = datetime('now') WHERE user_id = ?`,
      args: [coinsEarned, todayStr, user_id],
    });

    await db.execute({
      sql: `INSERT INTO coin_transactions (user_id, amount, type, source, description, created_at) VALUES (?, ?, 'EARN', 'meditation', ?, datetime('now'))`,
      args: [user_id, coinsEarned, `Meditation ${min} mins reward (${coinsEarned} coins)`],
    });
  }

  const stats = await db.execute({ sql: `SELECT krishna_coins FROM user_stats WHERE user_id = ?`, args: [user_id] });
  return c.json({ success: true, user_id, minutes: min, coins_earned: coinsEarned, total_coins: stats.rows[0]?.krishna_coins ?? 0 });
});

app.get("/meditation/history", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const result = await db.execute({
    sql: `SELECT id, minutes, coins_earned, session_date, created_at FROM meditation_sessions WHERE user_id = ? ORDER BY id DESC LIMIT 50`,
    args: [user_id],
  });
  return c.json(result.rows);
});

// ─── USER FEEDBACK & COMPLAINTS ───────────────────────────────
app.post("/feedback", requireAuth, async (c) => {
  const user_id = c.get("userId" as any) as string;
  const { type = "feedback", subject = "", message, client_timestamp } = await c.req.json();
  if (!message) return c.json({ error: "message required" }, 400);

  // Enforce 200 character max limit & auto timestamp
  const trimmedMsg = String(message).trim().slice(0, 200);
  const trimmedSub = String(subject).trim().slice(0, 100);
  const timestamp = client_timestamp || new Date().toISOString().replace("T", " ").substring(0, 19);

  await db.execute({
    sql: `INSERT INTO user_feedback (user_id, type, subject, message, status, created_at) VALUES (?, ?, ?, ?, 'open', ?)`,
    args: [user_id, type, trimmedSub, trimmedMsg, timestamp],
  });
  return c.json({ success: true, message: "Feedback submitted successfully", created_at: timestamp, content: trimmedMsg });
});

app.get("/feedback/list", async (c) => {
  if (!requireAdminKey(c)) return c.json({ error: "Forbidden" }, 403);
  const result = await db.execute(`SELECT * FROM user_feedback ORDER BY id DESC LIMIT 100`);
  return c.json(result.rows);
});

// ─── ERROR HANDLER (replaces try/catch wrapper around everything) ──
app.onError((error, c) => {
  console.error("FULL ERROR:", error);
  return c.json({ error: (error as Error).message }, 500);
});

// ─── 404 ────────────────────────────────────────────────────
app.notFound((c) => c.text("Not Found", 404));

Deno.serve(app.fetch);
