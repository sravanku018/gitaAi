const fs = require('fs');
const content = fs.readFileSync('deno-backend-hono.ts', 'utf-8');

const updated = content.replace(
  "await db.execute({ sql: `UPDATE checkin_streaks SET share_day = ?, share_week = ?, last_share = ? WHERE user_id = ?`, args: [share_day, next_week, today, user_id] });",
  `await db.execute({
    sql: \`INSERT INTO checkin_streaks (user_id, current_day, current_week, share_day, share_week, last_share)
          VALUES (?, 0, 1, ?, ?, ?)
          ON CONFLICT(user_id) DO UPDATE SET
          share_day = excluded.share_day,
          share_week = excluded.share_week,
          last_share = excluded.last_share\`,
    args: [user_id, share_day, next_week, today]
  });`
);

const updated2 = updated.replace(
  "remaining_balance: stats.rows[0].krishna_coins as number",
  "remaining_balance: stats.rows.length ? stats.rows[0].krishna_coins as number : 0"
);

fs.writeFileSync('deno-backend-hono.ts', updated2);
