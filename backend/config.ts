// ─── Environment & App Config ────────────────────────────────

function requireEnv(key: string): string {
  const value = Deno.env.get(key);
  if (!value) throw new Error(`Missing required environment variable: ${key}`);
  return value;
}

export const TURSO_URL = requireEnv("TURSO_URL");
export const TURSO_TOKEN = requireEnv("TURSO_TOKEN");
export const CORS_ORIGIN = Deno.env.get("CORS_ORIGIN") ?? "*";

// ─── Game Balance Constants ──────────────────────────────────
export const COIN_CAP = 10_000;
export const SIGNUP_BONUS = 200;
export const GUEST_BONUS = 50;
export const GUEST_CLAIM_BONUS = 150;
export const SYNC_BONUS = 50;
export const LEVEL_UP_BONUS = 10;

export const YOGA_LEVELS = [
  { threshold: 0, level: 1 },
  { threshold: 1_000, level: 2 },
  { threshold: 3_000, level: 3 },
  { threshold: 6_000, level: 4 },
  { threshold: 9_000, level: 5 },
];

export function getLevelForCoins(coins: number): number {
  let level = 1;
  for (const yl of YOGA_LEVELS) {
    if (coins >= yl.threshold) level = yl.level;
  }
  return level;
}

// ─── Auth Constants ──────────────────────────────────────────
export const SESSION_DAYS = 30;
export const TOKEN_BYTES = 32;

// ─── Rate Limiting ───────────────────────────────────────────
export const RATE_LIMIT_WINDOW_MS = 60_000;
export const RATE_LIMIT_MAX_REQUESTS = 60;

// ─── Validation Limits ───────────────────────────────────────
export const MAX_USER_ID_LENGTH = 64;
export const MAX_NAME_LENGTH = 100;
export const MAX_EMAIL_LENGTH = 254;
export const MIN_PASSWORD_LENGTH = 6;
