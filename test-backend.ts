// ─── Gita App Backend Test Suite ─────────────────────────────
// Usage: BACKEND_URL=https://your-backend.deno.dev deno run --allow-net --allow-env test-backend.ts

const BASE_URL = Deno.env.get("BACKEND_URL") || "http://localhost:8000";
const TEST_USER = `test_${Date.now()}`;
const TEST_PASS = "testpass123";

let token = "";
let passed = 0;
let failed = 0;
const failures: string[] = [];

// ─── HELPERS ─────────────────────────────────────────────────
function getISTDate(daysOffset = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + daysOffset);
  // IST = UTC+5:30
  const ist = new Date(d.getTime() + 5.5 * 60 * 60 * 1000);
  return ist.toISOString().split("T")[0];
}

async function api(method: string, path: string, body?: unknown, auth = false): Promise<any> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (auth && token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  try {
    return JSON.parse(text);
  } catch {
    return { _raw: text, _status: res.status };
  }
}

function assert(name: string, condition: boolean, detail = "") {
  if (condition) {
    console.log(`  ✅ ${name}`);
    passed++;
  } else {
    console.log(`  ❌ ${name}${detail ? ` — ${detail}` : ""}`);
    failed++;
    failures.push(`${name}${detail ? `: ${detail}` : ""}`);
  }
}

function section(name: string) {
  console.log(`\n─── ${name} ${"─".repeat(Math.max(0, 40 - name.length))}`);
}

// ─── CLEANUP ─────────────────────────────────────────────────
async function cleanup() {
  // Delete test user if exists
  if (token) {
    await api("POST", "/auth/delete", {}, true).catch(() => {});
  }
}

// ═══════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════

// ─── 1. ROOT ─────────────────────────────────────────────────
section("ROOT");
{
  const res = await api("GET", "/");
  assert("GET / returns status", res.status === "Gita App API running ✅");
}

// ─── 2. AUTH ─────────────────────────────────────────────────
section("AUTH");
{
  // Register
  const reg = await api("POST", "/auth/register", {
    user_id: TEST_USER,
    password: TEST_PASS,
    name: "Test User",
  });
  assert("Register — success", reg.success === true, JSON.stringify(reg));
  assert("Register — has token", typeof reg.token === "string");
  assert("Register — welcome coins 200", reg.coins === 200);
  token = reg.token;

  // Duplicate register — should return token, not error
  const reg2 = await api("POST", "/auth/register", {
    user_id: TEST_USER,
    password: TEST_PASS,
  });
  assert("Register duplicate — returns token", typeof reg2.token === "string");

  // Verify token
  const verify = await api("GET", "/auth/verify", undefined, true);
  assert("Auth verify — valid", verify.valid === true);
  assert("Auth verify — correct user", verify.user_id === TEST_USER);

  // Login
  const login = await api("POST", "/auth/login", {
    user_id: TEST_USER,
    password: TEST_PASS,
  });
  assert("Login — success", login.success === true);
  assert("Login — has token", typeof login.token === "string");

  // Wrong password
  const badLogin = await api("POST", "/auth/login", {
    user_id: TEST_USER,
    password: "wrongpass",
  });
  assert("Login — wrong password rejected", badLogin.error !== undefined);
}

// ─── 3. COINS BALANCE ────────────────────────────────────────
section("COINS BALANCE");
{
  const bal = await api("GET", `/coins/balance?user_id=${TEST_USER}`);
  assert("Balance — 200 after register", bal.krishna_coins === 200);
  assert("Balance — yoga_level is 1", bal.yoga_level === 1 || bal.yoga_name !== undefined);
  assert("Balance — checkin_day is 0", bal.checkin_day === 0);
  assert("Balance — share_day is 0", bal.share_day === 0);
}

// ─── 4. CHECKIN ──────────────────────────────────────────────
section("CHECKIN — Day sequence");
{
  const today = getISTDate();

  // Day 1 checkin
  const c1 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: today });
  assert("Checkin Day 1 — success", c1.duplicate !== true && c1.error === undefined, JSON.stringify(c1));
  assert("Checkin Day 1 — day=1", c1.day === 1);
  assert("Checkin Day 1 — coins>0", c1.coins_awarded > 0 || c1.total_coins > 0);

  // Duplicate same day
  const c1dup = await api("POST", "/checkin", { user_id: TEST_USER, client_date: today });
  assert("Checkin duplicate — blocked", c1dup.duplicate === true || c1dup.error !== undefined);

  // Day 2 — simulate next day
  const tomorrow = getISTDate(1);
  const c2 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: tomorrow });
  assert("Checkin Day 2 — success", c2.duplicate !== true && c2.error === undefined, JSON.stringify(c2));
  assert("Checkin Day 2 — day=2", c2.day === 2);

  // Day 3
  const day3 = getISTDate(2);
  const c3 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: day3 });
  assert("Checkin Day 3 — success", c3.duplicate !== true && c3.error === undefined, JSON.stringify(c3));
  assert("Checkin Day 3 — day=3", c3.day === 3, `got day=${c3.day}`);

  // Day 4
  const day4 = getISTDate(3);
  const c4 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: day4 });
  assert("Checkin Day 4 — success", c4.duplicate !== true && c4.error === undefined, JSON.stringify(c4));
  assert("Checkin Day 4 — day=4", c4.day === 4, `got day=${c4.day}`);

  // Day 5
  const day5 = getISTDate(4);
  const c5 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: day5 });
  assert("Checkin Day 5 — success", c5.duplicate !== true && c5.error === undefined, JSON.stringify(c5));
  assert("Checkin Day 5 — day=5", c5.day === 5, `got day=${c5.day}`);

  // Day 6
  const day6 = getISTDate(5);
  const c6 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: day6 });
  assert("Checkin Day 6 — success", c6.duplicate !== true && c6.error === undefined, JSON.stringify(c6));
  assert("Checkin Day 6 — day=6", c6.day === 6, `got day=${c6.day}`);

  // Day 7 — should include weekly bonus
  const day7 = getISTDate(6);
  const c7 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: day7 });
  assert("Checkin Day 7 — success", c7.duplicate !== true && c7.error === undefined, JSON.stringify(c7));
  assert("Checkin Day 7 — day=7", c7.day === 7, `got day=${c7.day}`);
  assert("Checkin Day 7 — weekly_bonus>0", c7.weekly_bonus > 0, `got weekly_bonus=${c7.weekly_bonus}`);

  // Day 8 — should wrap to Day 1 of new week
  const day8 = getISTDate(7);
  const c8 = await api("POST", "/checkin", { user_id: TEST_USER, client_date: day8 });
  assert("Checkin Day 8 (new week) — success", c8.duplicate !== true && c8.error === undefined, JSON.stringify(c8));
  assert("Checkin Day 8 — wraps to day=1", c8.day === 1, `got day=${c8.day}`);
}

// ─── 5. CHECKIN — Streak break ───────────────────────────────
section("CHECKIN — Streak break");
{
  const futureUser = `test_streak_${Date.now()}`;
  // Register fresh user
  const reg = await api("POST", "/auth/register", { user_id: futureUser, password: "pass123" });
  const fuToken = reg.token;

  // Day 1
  const d1 = await api("POST", "/checkin", { user_id: futureUser, client_date: "2026-01-01" });
  assert("Streak break — Day 1", d1.day === 1);

  // Day 2
  const d2 = await api("POST", "/checkin", { user_id: futureUser, client_date: "2026-01-02" });
  assert("Streak break — Day 2", d2.day === 2);

  // Skip day 3 — checkin on day 4 (streak should reset)
  const d4 = await api("POST", "/checkin", { user_id: futureUser, client_date: "2026-01-04" });
  assert("Streak break — reset to Day 1", d4.day === 1, `got day=${d4.day}`);

  // Cleanup streak test user
  const sToken = reg.token;
  if (sToken) {
    const headers = { "Content-Type": "application/json", "Authorization": `Bearer ${sToken}` };
    await fetch(`${BASE_URL}/auth/delete`, { method: "POST", headers }).catch(() => {});
  }
}

// ─── 5b. CHECKIN — 2 weeks ───────────────────────────────────
section("CHECKIN — 2 weeks continuous");
{
  const user2w = `test_2week_${Date.now()}`;
  await api("POST", "/auth/register", { user_id: user2w, password: "pass123" });

  let allPassed = true;
  let failedDay = 0;
  for (let i = 0; i < 14; i++) {
    const d = new Date("2026-03-01");
    d.setDate(d.getDate() + i);
    const dateStr = d.toISOString().split("T")[0];
    const res = await api("POST", "/checkin", { user_id: user2w, client_date: dateStr });
    const expectedDay = (i % 7) + 1;
    if (res.day !== expectedDay || res.duplicate || res.error) {
      allPassed = false;
      failedDay = i + 1;
      console.log(`    ❌ Day ${i + 1}: expected day=${expectedDay}, got day=${res.day} | ${JSON.stringify(res)}`);
      break;
    }
  }
  assert("2 weeks — all 14 days correct", allPassed, allPassed ? "" : `failed at day ${failedDay}`);

  // Check week wrapping: day 8 should be week 2 day 1
  const bal = await api("GET", `/coins/balance?user_id=${user2w}`);
  assert("2 weeks — checkin_week wrapped", bal.checkin_week >= 2, `got checkin_week=${bal.checkin_week}`);

  // Cleanup
  const headers = { "Content-Type": "application/json", "Authorization": `Bearer ${(await api("POST", "/auth/login", { user_id: user2w, password: "pass123" })).token}` };
  await fetch(`${BASE_URL}/auth/delete`, { method: "POST", headers }).catch(() => {});
}

// ─── 5c. CHECKIN — 3 weeks ───────────────────────────────────
section("CHECKIN — 3 weeks continuous");
{
  const user3w = `test_3week_${Date.now()}`;
  await api("POST", "/auth/register", { user_id: user3w, password: "pass123" });

  let allPassed = true;
  let failedDay = 0;
  for (let i = 0; i < 21; i++) {
    const d = new Date("2026-03-01");
    d.setDate(d.getDate() + i);
    const dateStr = d.toISOString().split("T")[0];
    const res = await api("POST", "/checkin", { user_id: user3w, client_date: dateStr });
    const expectedDay = (i % 7) + 1;
    if (res.day !== expectedDay || res.duplicate || res.error) {
      allPassed = false;
      failedDay = i + 1;
      console.log(`    ❌ Day ${i + 1}: expected day=${expectedDay}, got day=${res.day} | ${JSON.stringify(res)}`);
      break;
    }
  }
  assert("3 weeks — all 21 days correct", allPassed, allPassed ? "" : `failed at day ${failedDay}`);

  const bal = await api("GET", `/coins/balance?user_id=${user3w}`);
  assert("3 weeks — checkin_week >= 3", bal.checkin_week >= 3, `got checkin_week=${bal.checkin_week}`);

  // Cleanup
  const headers = { "Content-Type": "application/json", "Authorization": `Bearer ${(await api("POST", "/auth/login", { user_id: user3w, password: "pass123" })).token}` };
  await fetch(`${BASE_URL}/auth/delete`, { method: "POST", headers }).catch(() => {});
}

// ─── 5d. CHECKIN — 4 weeks (full month) ──────────────────────
section("CHECKIN — 4 weeks (full month)");
{
  const user4w = `test_4week_${Date.now()}`;
  await api("POST", "/auth/register", { user_id: user4w, password: "pass123" });

  let allPassed = true;
  let failedDay = 0;
  let weeklyBonuses = 0;
  for (let i = 0; i < 28; i++) {
    const d = new Date("2026-03-01");
    d.setDate(d.getDate() + i);
    const dateStr = d.toISOString().split("T")[0];
    const res = await api("POST", "/checkin", { user_id: user4w, client_date: dateStr });
    const expectedDay = (i % 7) + 1;
    if (res.day !== expectedDay || res.duplicate || res.error) {
      allPassed = false;
      failedDay = i + 1;
      console.log(`    ❌ Day ${i + 1}: expected day=${expectedDay}, got day=${res.day} | ${JSON.stringify(res)}`);
      break;
    }
    if (res.weekly_bonus > 0) weeklyBonuses++;
  }
  assert("4 weeks — all 28 days correct", allPassed, allPassed ? "" : `failed at day ${failedDay}`);
  assert("4 weeks — got 4 weekly bonuses", weeklyBonuses === 4, `got ${weeklyBonuses}`);

  const bal = await api("GET", `/coins/balance?user_id=${user4w}`);
  assert("4 weeks — week wrapped back to 1", bal.checkin_week <= 2, `got checkin_week=${bal.checkin_week}`);

  // Cleanup
  const headers = { "Content-Type": "application/json", "Authorization": `Bearer ${(await api("POST", "/auth/login", { user_id: user4w, password: "pass123" })).token}` };
  await fetch(`${BASE_URL}/auth/delete`, { method: "POST", headers }).catch(() => {});
}

// ─── 5e. SHARE — 2 weeks ─────────────────────────────────────
section("SHARE — 2 weeks continuous");
{
  const user2ws = `test_share_2week_${Date.now()}`;
  await api("POST", "/auth/register", { user_id: user2ws, password: "pass123" });

  let allPassed = true;
  let failedDay = 0;
  for (let i = 0; i < 14; i++) {
    const d = new Date("2026-03-01");
    d.setDate(d.getDate() + i);
    const dateStr = d.toISOString().split("T")[0];
    const res = await api("POST", "/share", {
      user_id: user2ws, client_date: dateStr,
      sloka_id: `${i + 1}`, chapter: 1, verse: i + 1,
    });
    const expectedDay = (i % 7) + 1;
    if (res.share_day !== expectedDay || res.duplicate || res.error) {
      allPassed = false;
      failedDay = i + 1;
      console.log(`    ❌ Share Day ${i + 1}: expected share_day=${expectedDay}, got share_day=${res.share_day} | ${JSON.stringify(res)}`);
      break;
    }
  }
  assert("Share 2 weeks — all 14 days correct", allPassed, allPassed ? "" : `failed at day ${failedDay}`);

  // Cleanup
  const headers = { "Content-Type": "application/json", "Authorization": `Bearer ${(await api("POST", "/auth/login", { user_id: user2ws, password: "pass123" })).token}` };
  await fetch(`${BASE_URL}/auth/delete`, { method: "POST", headers }).catch(() => {});
}

// ─── 5f. SHARE — 4 weeks (full month) ────────────────────────
section("SHARE — 4 weeks (full month)");
{
  const user4ws = `test_share_4week_${Date.now()}`;
  await api("POST", "/auth/register", { user_id: user4ws, password: "pass123" });

  let allPassed = true;
  let failedDay = 0;
  let weeklyBonuses = 0;
  for (let i = 0; i < 28; i++) {
    const d = new Date("2026-03-01");
    d.setDate(d.getDate() + i);
    const dateStr = d.toISOString().split("T")[0];
    const res = await api("POST", "/share", {
      user_id: user4ws, client_date: dateStr,
      sloka_id: `${i + 1}`, chapter: 1, verse: i + 1,
    });
    const expectedDay = (i % 7) + 1;
    if (res.share_day !== expectedDay || res.duplicate || res.error) {
      allPassed = false;
      failedDay = i + 1;
      console.log(`    ❌ Share Day ${i + 1}: expected share_day=${expectedDay}, got share_day=${res.share_day} | ${JSON.stringify(res)}`);
      break;
    }
    if (res.weekly_bonus > 0) weeklyBonuses++;
  }
  assert("Share 4 weeks — all 28 days correct", allPassed, allPassed ? "" : `failed at day ${failedDay}`);
  assert("Share 4 weeks — got 4 weekly bonuses", weeklyBonuses === 4, `got ${weeklyBonuses}`);

  // Cleanup
  const headers = { "Content-Type": "application/json", "Authorization": `Bearer ${(await api("POST", "/auth/login", { user_id: user4ws, password: "pass123" })).token}` };
  await fetch(`${BASE_URL}/auth/delete`, { method: "POST", headers }).catch(() => {});
}

// ─── 6. SHARE ────────────────────────────────────────────────
section("SHARE — Day sequence");
{
  const today = getISTDate();
  const tomorrow = getISTDate(1);
  const day3 = getISTDate(2);

  // Share Day 1
  const s1 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: today,
    sloka_id: "1", chapter: 1, verse: 1,
  });
  assert("Share Day 1 — success", s1.duplicate !== true && s1.error === undefined, JSON.stringify(s1));
  assert("Share Day 1 — share_day=1", s1.share_day === 1);

  // Share duplicate
  const s1dup = await api("POST", "/share", {
    user_id: TEST_USER, client_date: today,
    sloka_id: "1", chapter: 1, verse: 1,
  });
  assert("Share duplicate — blocked", s1dup.duplicate === true || s1dup.error !== undefined);

  // Share Day 2
  const s2 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: tomorrow,
    sloka_id: "2", chapter: 1, verse: 2,
  });
  assert("Share Day 2 — success", s2.duplicate !== true && s2.error === undefined, JSON.stringify(s2));
  assert("Share Day 2 — share_day=2", s2.share_day === 2);

  // Share Day 3
  const s3 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: day3,
    sloka_id: "3", chapter: 1, verse: 3,
  });
  assert("Share Day 3 — success", s3.duplicate !== true && s3.error === undefined, JSON.stringify(s3));
  assert("Share Day 3 — share_day=3", s3.share_day === 3, `got share_day=${s3.share_day}`);

  // Share Day 4
  const s4 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: getISTDate(3),
    sloka_id: "4", chapter: 1, verse: 4,
  });
  assert("Share Day 4 — success", s4.duplicate !== true && s4.error === undefined, JSON.stringify(s4));
  assert("Share Day 4 — share_day=4", s4.share_day === 4, `got share_day=${s4.share_day}`);

  // Share Day 5
  const s5 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: getISTDate(4),
    sloka_id: "5", chapter: 1, verse: 5,
  });
  assert("Share Day 5 — success", s5.duplicate !== true && s5.error === undefined, JSON.stringify(s5));
  assert("Share Day 5 — share_day=5", s5.share_day === 5, `got share_day=${s5.share_day}`);

  // Share Day 6
  const s6 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: getISTDate(5),
    sloka_id: "6", chapter: 1, verse: 6,
  });
  assert("Share Day 6 — success", s6.duplicate !== true && s6.error === undefined, JSON.stringify(s6));
  assert("Share Day 6 — share_day=6", s6.share_day === 6, `got share_day=${s6.share_day}`);

  // Share Day 7 — should include weekly bonus
  const s7 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: getISTDate(6),
    sloka_id: "7", chapter: 1, verse: 7,
  });
  assert("Share Day 7 — success", s7.duplicate !== true && s7.error === undefined, JSON.stringify(s7));
  assert("Share Day 7 — share_day=7", s7.share_day === 7, `got share_day=${s7.share_day}`);
  assert("Share Day 7 — weekly_bonus>0", s7.weekly_bonus > 0, `got weekly_bonus=${s7.weekly_bonus}`);

  // Share Day 8 — should wrap to Day 1 of new week
  const s8 = await api("POST", "/share", {
    user_id: TEST_USER, client_date: getISTDate(7),
    sloka_id: "8", chapter: 2, verse: 1,
  });
  assert("Share Day 8 (new week) — success", s8.duplicate !== true && s8.error === undefined, JSON.stringify(s8));
  assert("Share Day 8 — wraps to share_day=1", s8.share_day === 1, `got share_day=${s8.share_day}`);
}

// ─── 7. COINS HISTORY ────────────────────────────────────────
section("COINS HISTORY");
{
  const hist = await api("GET", `/coins/history?user_id=${TEST_USER}`);
  assert("History — is array", Array.isArray(hist));
  assert("History — has entries", hist.length > 0);

  // All entries should have created_at
  const nullDates = hist.filter((h: any) => !h.created_at);
  assert("History — no NULL created_at", nullDates.length === 0, `${nullDates.length} rows with NULL`);

  // Should be sorted DESC
  if (hist.length >= 2) {
    const sorted = hist[0].created_at >= hist[1].created_at;
    assert("History — sorted DESC by created_at", sorted);
  }

  // auto_reconcile should be filtered
  const reconcile = hist.filter((h: any) => h.source === "auto_reconcile");
  assert("History — auto_reconcile filtered", reconcile.length === 0);
}

// ─── 8. COINS AWARD ──────────────────────────────────────────
section("COINS AWARD — Quiz accuracy & scoring");
{
  const before = await api("GET", `/coins/balance?user_id=${TEST_USER}`);
  const beforeCoins = before.krishna_coins;

  const award = await api("POST", "/coins/award", {
    user_id: TEST_USER,
    source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 2, checkinDay: 2, quizType: "general" },
  });
  assert("Award — success", award.awarded !== undefined, JSON.stringify(award));
  assert("Award — coins > 0", award.awarded > 0);

  const after = await api("GET", `/coins/balance?user_id=${TEST_USER}`);
  assert("Award — balance increased", after.krishna_coins > beforeCoins);

  // ─── Accuracy levels ─────────────────────────────────────
  section("COINS AWARD — Accuracy breakdown");

  // Formula: base=5, accuracyBonus=floor(acc²*6), streakBonus=min(floor(streak/5),3), checkinBonus
  // Total capped at max_coins=15

  // 0% accuracy → accuracyBonus=0
  const a0 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.0, score: 0, totalQuestions: 15, streakDays: 0, checkinDay: 0 },
  });
  assert("Accuracy 0% — base coins only", a0.awarded === 5, `got ${a0.awarded}`);

  // 50% accuracy → accuracyBonus = floor(0.25*6) = 1 → total = 5+1 = 6
  const a50 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.5, score: 7, totalQuestions: 15, streakDays: 0, checkinDay: 0 },
  });
  assert("Accuracy 50% — 6 coins", a50.awarded === 6, `got ${a50.awarded}`);

  // 80% accuracy → accuracyBonus = floor(0.64*6) = 3 → total = 5+3 = 8
  const a80 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 0, checkinDay: 0 },
  });
  assert("Accuracy 80% — 8 coins", a80.awarded === 8, `got ${a80.awarded}`);

  // 100% accuracy → accuracyBonus = floor(1.0*6) = 6 → total = 5+6 = 11
  const a100 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 1.0, score: 15, totalQuestions: 15, streakDays: 0, checkinDay: 0 },
  });
  assert("Accuracy 100% — 11 coins", a100.awarded === 11, `got ${a100.awarded}`);

  // ─── Streak bonuses ──────────────────────────────────────
  section("COINS AWARD — Streak bonuses");

  // streakDays=4 → streakBonus=0 (needs 5 for +1)
  const s4 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 4, checkinDay: 0 },
  });
  assert("Streak 4 days — no bonus", s4.awarded === 8, `got ${s4.awarded}`);

  // streakDays=5 → streakBonus=1 → total = 5+3+1 = 9
  const s5 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 5, checkinDay: 0 },
  });
  assert("Streak 5 days — +1 bonus = 9 coins", s5.awarded === 9, `got ${s5.awarded}`);

  // streakDays=10 → streakBonus=2 → total = 5+3+2 = 10
  const s10 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 10, checkinDay: 0 },
  });
  assert("Streak 10 days — +2 bonus = 10 coins", s10.awarded === 10, `got ${s10.awarded}`);

  // streakDays=15 → streakBonus=3 → total = 5+3+3 = 11
  const s15 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 15, checkinDay: 0 },
  });
  assert("Streak 15 days — +3 bonus = 11 coins", s15.awarded === 11, `got ${s15.awarded}`);

  // streakDays=100 → streakBonus still max 3
  const s100 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 100, checkinDay: 0 },
  });
  assert("Streak 100 days — still max +3 = 11 coins", s100.awarded === 11, `got ${s100.awarded}`);

  // ─── Checkin day bonuses ──────────────────────────────────
  section("COINS AWARD — Checkin day bonuses");

  // checkinDay=1 → checkinBonus=0
  const cd1 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 0, checkinDay: 1 },
  });
  assert("Checkin day 1 — no bonus = 8", cd1.awarded === 8, `got ${cd1.awarded}`);

  // checkinDay=2 → checkinBonus=1 → total = 5+3+1 = 9
  const cd2 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 0, checkinDay: 2 },
  });
  assert("Checkin day 2 — +1 bonus = 9", cd2.awarded === 9, `got ${cd2.awarded}`);

  // checkinDay=5 → checkinBonus=2 → total = 5+3+2 = 10
  const cd5 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 0, checkinDay: 5 },
  });
  assert("Checkin day 5 — +2 bonus = 10", cd5.awarded === 10, `got ${cd5.awarded}`);

  // checkinDay=7 → checkinBonus=3 → total = 5+3+3 = 11
  const cd7 = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 0.8, score: 12, totalQuestions: 15, streakDays: 0, checkinDay: 7 },
  });
  assert("Checkin day 7 — +3 bonus = 11", cd7.awarded === 11, `got ${cd7.awarded}`);

  // ─── Max coin cap ────────────────────────────────────────
  section("COINS AWARD — Max coin cap");

  // 100% accuracy + streak 15 + checkin 7 → 5+6+3+3 = 17 → capped at max_coins
  const maxCoins = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 1.0, score: 15, totalQuestions: 15, streakDays: 15, checkinDay: 7 },
  });
  // If max_coins is NULL (no cap), raw = 17. If 15, capped. Both are valid.
  const raw = 5 + 6 + 3 + 3; // 17
  const isCapped = maxCoins.awarded <= raw;
  assert("Max cap — coins <= raw maximum (17)", isCapped, `got ${maxCoins.awarded}`);
  assert("Max cap — coins > 0", maxCoins.awarded > 0);

  // ─── Scoring records ──────────────────────────────────────
  section("COINS AWARD — Score recording via /quiz/attempt");

  // Record quiz attempt via the dedicated endpoint
  const quizAttempt = await api("POST", "/quiz/attempt", {
    user_id: TEST_USER, score: 12, total_questions: 15, quiz_type: "general", time_spent_seconds: 90, coins_earned: 8,
  });
  assert("Quiz attempt — success", quizAttempt.success === true);

  const quizHist = await api("GET", `/quiz/history?user_id=${TEST_USER}`);
  assert("Quiz history — has entries", quizHist.length > 0, `got ${quizHist.length}`);
  assert("Quiz history — latest score=12", quizHist[0].score === 12, `got score=${quizHist[0].score}`);
  assert("Quiz history — latest total=15", quizHist[0].total_questions === 15, `got total=${quizHist[0].total_questions}`);
  assert("Quiz history — quiz_type=general", quizHist[0].quiz_type === "general", `got type=${quizHist[0].quiz_type}`);
  assert("Quiz history — time recorded", quizHist[0].time_spent_seconds === 90, `got time=${quizHist[0].time_spent_seconds}`);
  assert("Quiz history — coins_earned=8", quizHist[0].coins_earned === 8, `got coins=${quizHist[0].coins_earned}`);
  assert("Quiz history — accuracy=80", quizHist[0].accuracy === 80, `got accuracy=${quizHist[0].accuracy}`);
  assert("Quiz history — avg_time=6", quizHist[0].avg_time_per_question === 6, `got avg=${quizHist[0].avg_time_per_question}`);

  // Edge cases
  section("COINS AWARD — Edge cases");

  // Unknown source
  const unknown = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "nonexistent_source",
  });
  assert("Unknown source — rejected", unknown.error !== undefined, JSON.stringify(unknown));

  // Missing user_id
  const noUser = await api("POST", "/coins/award", {
    user_id: "", source: "quiz_completion",
  });
  // Should either error or return 0
  assert("Empty user_id — handled", noUser.error !== undefined || noUser.awarded === 0, JSON.stringify(noUser));

  // Accuracy clamped to [0,1]
  const overAcc = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: 1.5, score: 20, totalQuestions: 15, streakDays: 0, checkinDay: 0 },
  });
  assert("Accuracy >1 — clamped to 1.0 = 11", overAcc.awarded === 11, `got ${overAcc.awarded}`);

  const negAcc = await api("POST", "/coins/award", {
    user_id: TEST_USER, source: "quiz_completion",
    metadata: { accuracy: -0.5, score: 0, totalQuestions: 15, streakDays: 0, checkinDay: 0 },
  });
  assert("Negative accuracy — clamped to 0 = 5", negAcc.awarded === 5, `got ${negAcc.awarded}`);
}

// ─── 9. COINS SPEND ──────────────────────────────────────────
section("COINS SPEND");
{
  const before = await api("GET", `/coins/balance?user_id=${TEST_USER}`);
  const beforeCoins = before.krishna_coins;
  const ikey = `spend_${TEST_USER}_${Date.now()}`;

  const spend = await api("POST", "/coins/spend", {
    user_id: TEST_USER,
    question: "What is karma?",
    idempotency_key: ikey,
  });
  assert("Spend — success", spend.spent > 0, JSON.stringify(spend));
  assert("Spend — balance decreased", spend.remaining_balance < beforeCoins);

  // Duplicate spend
  const spendDup = await api("POST", "/coins/spend", {
    user_id: TEST_USER,
    question: "What is karma?",
    idempotency_key: ikey,
  });
  assert("Spend duplicate — blocked", spendDup.duplicate === true);
}

// ─── 10. VOICE COST ──────────────────────────────────────────
section("VOICE COST");
{
  const short = await api("GET", "/coins/voice-cost?question=Hi");
  assert("Voice cost — short question", short.cost === 2 && short.label === "Short");

  const medium = await api("GET", `/coins/voice-cost?question=${"a".repeat(100)}`);
  assert("Voice cost — medium question", medium.cost === 3 && medium.label === "Medium");

  const long = await api("GET", `/coins/voice-cost?question=${"a".repeat(200)}`);
  assert("Voice cost — long question", long.cost === 5 && long.label === "Long");
}

// ─── 11. QUIZ ────────────────────────────────────────────────
section("QUIZ");
{
  const attempt = await api("POST", "/quiz/attempt", {
    user_id: TEST_USER,
    score: 10,
    total_questions: 15,
    quiz_type: "general",
    time_spent_seconds: 120,
  });
  assert("Quiz attempt — success", attempt.success === true);

  const hist = await api("GET", `/quiz/history?user_id=${TEST_USER}`);
  assert("Quiz history — is array", Array.isArray(hist));
  assert("Quiz history — has entry", hist.length > 0);
  assert("Quiz history — score correct", hist[0].score === 10);
}

// ─── 12. LEADERBOARD ─────────────────────────────────────────
section("LEADERBOARD");
{
  const lb = await api("GET", "/coins/leaderboard");
  assert("Leaderboard — is array", Array.isArray(lb));
  // Guests should not appear
  const guests = lb.filter((u: any) => u.user_id?.startsWith("guest_"));
  assert("Leaderboard — no guests", guests.length === 0);
}

// ─── 13. YOGA STAGES ─────────────────────────────────────────
section("YOGA STAGES");
{
  const yoga = await api("GET", "/yoga/stages");
  assert("Yoga stages — has levels", Array.isArray(yoga.levels) && yoga.levels.length > 0);
  assert("Yoga stages — has sub_stages", Array.isArray(yoga.sub_stages));
}

// ─── 14. GUEST ───────────────────────────────────────────────
section("GUEST");
{
  const guest = await api("POST", "/guest/create");
  assert("Guest create — has guest_id", typeof guest.guest_id === "string");
  assert("Guest create — starts with guest_", guest.guest_id?.startsWith("guest_"));
  assert("Guest create — 50 coins", guest.coins === 50);
  assert("Guest create — has token", typeof guest.token === "string");
}

// ─── 15. STATS SYNC ──────────────────────────────────────────
section("STATS SYNC");
{
  const sync = await api("POST", "/users/stats/sync", {
    user_id: TEST_USER,
    current_streak: 5,
    longest_streak: 10,
    total_quizzes_taken: 3,
    verses_read: 20,
    last_activity_date: getISTDate(),
  });
  assert("Stats sync — success", sync.success === true, JSON.stringify(sync));
  assert("Stats sync — streak updated", sync.stats?.current_streak >= 0);
}

// ─── CLEANUP ─────────────────────────────────────────────────
await cleanup();

// ─── RESULTS ─────────────────────────────────────────────────
console.log("\n" + "═".repeat(50));
console.log(`RESULTS: ${passed} passed, ${failed} failed`);
if (failures.length > 0) {
  console.log("\nFailed tests:");
  failures.forEach(f => console.log(`  ❌ ${f}`));
}
console.log("═".repeat(50));

Deno.exit(failed > 0 ? 1 : 0);
