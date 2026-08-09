// test-streak-validation.ts
const BASE_URL = Deno.env.get("BACKEND_URL") || "http://localhost:8000";

async function api(method: string, path: string, body?: unknown) {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  return await res.json();
}

function getUTCStr(daysOffset = 0) {
  const d = new Date();
  d.setDate(d.getDate() + daysOffset);
  return d.toISOString().split("T")[0];
}

async function run() {
  console.log("=== STREAK VALIDATION TESTS ===");
  
  // 1. Create a user
  const user_id = `test_streak_${Date.now()}`;
  await api("POST", "/auth/register", { user_id, password: "password123" });
  
  // Server starts with 0 streak and today's date.
  const today = getUTCStr(0);
  const tomorrow = getUTCStr(1);
  const nextWeek = getUTCStr(7);
  const yesterday = getUTCStr(-1);

  // Helper to sync and print result
  async function syncAndAssert(name: string, clientDate: string, clientStreak: number, expectedStreak: number) {
    console.log(`\nTest: ${name}`);
    console.log(`  Client sends: Date=${clientDate}, Streak=${clientStreak}`);
    const res = await api("POST", "/users/stats/sync", {
      user_id,
      last_activity_date: clientDate,
      current_streak: clientStreak
    });
    
    if (!res.stats) {
      console.log("  ❌ FAILED: No stats returned!");
      return;
    }
    
    const actualStreak = res.stats.current_streak;
    if (actualStreak === expectedStreak) {
      console.log(`  ✅ PASSED: Server streak is ${actualStreak}`);
    } else {
      console.log(`  ❌ FAILED: Expected ${expectedStreak}, but got ${actualStreak}`);
    }
  }

  // Initial sync (same day, legit streak=0) -> should be 0
  await syncAndAssert("Same day legit", today, 0, 0);

  // Sync malicious on same day (trying to jump to 50) -> should be capped at 0
  await syncAndAssert("Same day cheat", today, 50, 0);

  // Sync valid next day (+1 day, +1 streak) -> should be 1
  await syncAndAssert("Valid next day", tomorrow, 1, 1);

  // Sync malicious next day (+1 day, +50 streak) -> capped at (previous server streak + 1) = 2
  await syncAndAssert("Next day cheat", tomorrow, 50, 1);

  // Sync valid offline progress (+6 days, +6 streak) -> should be 7
  await syncAndAssert("Valid offline progress", nextWeek, 7, 7);

  // Sync old offline progress (client date is yesterday) -> should be ignored, kept at 7
  await syncAndAssert("Old offline sync", yesterday, 1, 7);

  // Cleanup
  await api("POST", "/auth/delete", { user_id });
  console.log("\nDone!");
}

run();
