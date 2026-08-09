// test-prod-raja21.ts
const BASE_URL = "https://prime-gorilla-49.sravanku018.deno.net";

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
  console.log("=== Testing User on Production: raja21 ===");
  const user_id = "raja21";

  // 1. Fetch current balance/stats from prod server
  const balance = await api("GET", `/coins/balance?user_id=${user_id}`);
  console.log(`\nInitial Server State:`);
  console.log(`  Streak: ${balance.current_streak}`);
  console.log(`  Last Activity: ${balance.last_activity_date}`);

  // 2. Try to sync the broken streak 17 from mobile for today
  const today = new Date().toISOString().split("T")[0];
  console.log(`\nSimulating mobile app sending broken sync: Date=${today}, Streak=17`);
  
  const syncRes = await api("POST", "/users/stats/sync", {
    user_id,
    current_streak: 17,
    last_activity_date: today,
    total_quizzes_taken: balance.total_quizzes_taken || 0,
    verses_read: balance.verses_read || 0,
    longest_streak: balance.longest_streak || 17
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
