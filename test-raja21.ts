// test-raja21.ts
const BASE_URL = "http://localhost:8000";

async function api(method: string, path: string, body?: unknown) {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  return await res.json();
}

async function run() {
  console.log("=== Testing User: raja21 ===");
  
  // 1. Try to register or login raja21
  const user_id = "raja21";
  console.log("Logging in...");
  let authRes = await api("POST", "/auth/login", { user_id, password: "raja21" });
  if (!authRes.success) {
    console.log("Login failed, attempting register...");
    authRes = await api("POST", "/auth/register", { user_id, password: "raja21" });
  }

  // 2. Fetch current balance/stats to see what the server thinks it is right now
  const balance = await api("GET", `/coins/balance?user_id=${user_id}`);
  console.log(`\nInitial Server State:`);
  console.log(`  Streak: ${balance.current_streak}`);
  console.log(`  Last Activity: ${balance.last_activity_date}`);

  // 3. Simulate mobile app syncing a broken streak of 17 for today
  const today = new Date().toISOString().split("T")[0];
  console.log(`\nSimulating mobile app sending broken sync: Date=${today}, Streak=17`);
  
  const syncRes = await api("POST", "/users/stats/sync", {
    user_id,
    current_streak: 17,
    last_activity_date: today,
    total_quizzes_taken: 5,
    verses_read: 10,
    longest_streak: 17
  });

  console.log(`\nResult after Auto-Correction:`);
  if (syncRes.stats) {
    console.log(`  Corrected Streak: ${syncRes.stats.current_streak}`);
    console.log(`  Corrected Last Activity: ${syncRes.stats.last_activity_date}`);
  } else {
    console.log(syncRes);
  }
}

run();
