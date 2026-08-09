// ─── Battle & Notes Test Suite ──────────────────────────────
// Usage: BACKEND_URL=https://your-backend.deno.dev deno run --allow-net --allow-env test-battle-notes.ts

const BASE_URL = Deno.env.get("BACKEND_URL") || "http://localhost:8000";
const TEST_USER = `test_${Date.now()}`;
const TEST_PASS = "testpass123";

let token = "";
let passed = 0;
let failed = 0;
const failures: string[] = [];

async function api(method: string, path: string, body?: unknown, auth = false): Promise<any> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (auth && token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  try { return JSON.parse(text); } catch { return { _raw: text, _status: res.status }; }
}

function assert(name: string, condition: boolean, detail = "") {
  if (condition) { console.log(`  ✅ ${name}`); passed++; }
  else { console.log(`  ❌ ${name}${detail ? ` — ${detail}` : ""}`); failed++; failures.push(`${name}${detail ? `: ${detail}` : ""}`); }
}

function section(name: string) { console.log(`\n─── ${name} ${"─".repeat(Math.max(0, 40 - name.length))}`); }

async function cleanup() {
  if (token) await api("POST", "/auth/delete", {}, true).catch(() => {});
}

// ═══════════════════════════════════════════════════════════════
// SETUP: Register & login
// ═══════════════════════════════════════════════════════════════
section("SETUP");
{
  const reg = await api("POST", "/auth/register", { user_id: TEST_USER, password: TEST_PASS, name: "Battle Tester" });
  assert("Register user", reg.token != null, JSON.stringify(reg));
  token = reg.token;
}

// ═══════════════════════════════════════════════════════════════
// 1. BATTLE QUIZ — coin award
// ═══════════════════════════════════════════════════════════════
section("BATTLE QUIZ COINS");
{
  // Award battle coins (e.g. 3 correct at 3 hearts = 3pts, 2 at 2 hearts = 1pt, 1 at 1 heart = 0.25pt => total 4.25 => 4 coins)
  const award = await api("POST", "/coins/award", {
    user_id: TEST_USER,
    source: "battle_quiz",
    metadata: { battleCoins: 4, score: 80, questionsAnswered: 6 }
  });
  assert("Battle award — returns awarded", typeof award.awarded === "number", JSON.stringify(award));
  assert("Battle award — awarded > 0", award.awarded > 0, `got ${award.awarded}`);
  assert("Battle award — has total_coins", typeof award.total_coins === "number", JSON.stringify(award));

  const balanceBefore = award.total_coins;

  // Award again with 0 coins (all wrong answers scenario)
  const award0 = await api("POST", "/coins/award", {
    user_id: TEST_USER,
    source: "battle_quiz",
    metadata: { battleCoins: 0, score: 0, questionsAnswered: 3 }
  });
  assert("Battle award 0 coins — awarded is 0", award0.awarded === 0, `got ${award0.awarded}`);

  // Award with max cap (battle_quiz max_coins = 10)
  const awardMax = await api("POST", "/coins/award", {
    user_id: TEST_USER,
    source: "battle_quiz",
    metadata: { battleCoins: 20, score: 100, questionsAnswered: 10 }
  });
  assert("Battle award — capped at 10", awardMax.awarded <= 10, `got ${awardMax.awarded}`);

  // Check balance reflects awards
  const bal = await api("GET", `/coins/balance?user_id=${TEST_USER}`);
  assert("Balance — updated after battle", bal.krishna_coins > 50, `coins: ${bal.krishna_coins}`);

  // Check transaction history includes battle_quiz
  const hist = await api("GET", `/coins/history?user_id=${TEST_USER}`);
  const battleTx = hist.filter((t: any) => t.source === "battle_quiz");
  assert("History — has battle_quiz entries", battleTx.length >= 2, `found ${battleTx.length}`);
}

// ═══════════════════════════════════════════════════════════════
// 2. NOTES — CRUD
// ═══════════════════════════════════════════════════════════════
section("NOTES");
{
  // Sync notes (create)
  const sync1 = await api("POST", "/notes/sync", {
    user_id: TEST_USER,
    notes: [
      { chapterNo: 2, verseNo: 47, note: "Karmanye vadhikaraste - my first note" },
      { chapterNo: 4, verseNo: 7, note: "Yada yada hi dharmasya - second note" },
      { chapterNo: 15, verseNo: 7, note: "Mamaivamso jivaloke - third note" },
    ]
  });
  assert("Notes sync — success", sync1.success === true, JSON.stringify(sync1));
  assert("Notes sync — synced 3", sync1.synced === 3, `got ${sync1.synced}`);

  // Get notes
  const notes = await api("GET", `/notes?user_id=${TEST_USER}`);
  assert("Get notes — is array", Array.isArray(notes), JSON.stringify(notes));
  assert("Get notes — has 3 entries", notes.length === 3, `got ${notes.length}`);
  assert("Get notes — first note text", notes.some((n: any) => n.note.includes("Karmanye")), JSON.stringify(notes[0]));

  // Sync again (update existing)
  const sync2 = await api("POST", "/notes/sync", {
    user_id: TEST_USER,
    notes: [
      { chapterNo: 2, verseNo: 47, note: "Updated: Karmanye vadhikaraste - edited note" },
    ]
  });
  assert("Notes update — success", sync2.success === true, JSON.stringify(sync2));

  const updated = await api("GET", `/notes?user_id=${TEST_USER}`);
  const edited = updated.find((n: any) => n.chapter_no === 2 && n.verse_no === 47);
  assert("Notes update — text changed", edited?.note?.includes("Updated"), `got: ${edited?.note}`);

  // Delete note
  const del = await api("POST", "/notes/delete", {
    user_id: TEST_USER,
    chapter_no: 15,
    verse_no: 7,
  });
  assert("Notes delete — success", del.success === true, JSON.stringify(del));

  const afterDel = await api("GET", `/notes?user_id=${TEST_USER}`);
  assert("Notes delete — removed from list", afterDel.length === 2, `got ${afterDel.length}`);

  // Sync empty array (no-op)
  const syncEmpty = await api("POST", "/notes/sync", {
    user_id: TEST_USER,
    notes: []
  });
  assert("Notes sync empty — success", syncEmpty.success === true);
  assert("Notes sync empty — synced 0", syncEmpty.synced === 0);

  // Missing user_id
  const noUser = await api("POST", "/notes/sync", { notes: [] });
  assert("Notes sync no user_id — 400", noUser._status === 400 || noUser.error === "user_id required");

  const noUserGet = await api("GET", "/notes");
  assert("Notes get no user_id — 400", noUserGet._status === 400 || noUserGet.error === "user_id required");
}

// ═══════════════════════════════════════════════════════════════
// CLEANUP
// ═══════════════════════════════════════════════════════════════
await cleanup();

// ═══════════════════════════════════════════════════════════════
// RESULTS
// ═══════════════════════════════════════════════════════════════
console.log("\n" + "═".repeat(50));
console.log(`RESULTS: ${passed} passed, ${failed} failed`);
if (failures.length > 0) {
  console.log("\nFailed tests:");
  failures.forEach(f => console.log(`  ❌ ${f}`));
}
console.log("═".repeat(50));

Deno.exit(failed > 0 ? 1 : 0);
