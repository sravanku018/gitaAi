import { Database } from "https://deno.land/x/sqlite3@0.12.0/mod.ts";
const db = new Database("gita.db");
try {
  const user_id = "test_user_1";
  const today = new Date().toISOString().split("T")[0];
  console.log("Testing checkin_streaks...");
  db.prepare(`INSERT INTO checkin_streaks (user_id, current_day, current_week, share_day, share_week, last_share) VALUES (?, 0, 1, 1, 1, ?) ON CONFLICT(user_id) DO UPDATE SET share_day = excluded.share_day, share_week = excluded.share_week, last_share = excluded.last_share`).run(user_id, today);
  console.log("Testing sloka_shares...");
  db.prepare(`INSERT INTO sloka_shares (user_id, share_date, sloka_id, chapter, verse, coins_earned, day, week) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`).run(user_id, today, null, null, null, 10, 1, 1);
  console.log("Testing user_stats...");
  db.prepare(`UPDATE user_stats SET krishna_coins = MIN(krishna_coins + ?, 10000), updated_at = datetime('now') WHERE user_id = ?`).run(10, user_id);
  console.log("Testing coin_transactions...");
  db.prepare(`INSERT INTO coin_transactions (user_id, amount, type, source, description, idempotency_key, created_at) VALUES (?, ?, 'EARN', 'share_sloka', ?, ?, ?)`).run(user_id, 10, "Share day 1", "key123", today);
  console.log("All success!");
} catch (e) {
  console.error("Error:", e);
}
