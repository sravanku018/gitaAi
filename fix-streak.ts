import { createClient } from "npm:@libsql/client";

// Ensure we have the production credentials
const url = Deno.env.get("DATABASE_URL");
const authToken = Deno.env.get("DATABASE_AUTH_TOKEN");

if (!url) {
  console.error("❌ Error: Missing DATABASE_URL environment variable.");
  Deno.exit(1);
}

const db = createClient({ url, authToken });

async function fixUser(userId: string) {
  console.log(`Connecting to production database...`);
  console.log(`Resetting streak for user: ${userId}`);

  try {
    const result = await db.execute({
      sql: "UPDATE user_stats SET current_streak = 0 WHERE user_id = ?",
      args: [userId]
    });

    if (result.rowsAffected > 0) {
      console.log(`✅ Successfully reset streak for ${userId} to 0!`);
    } else {
      console.log(`⚠️ User ${userId} not found in user_stats table.`);
    }
  } catch (error) {
    console.error(`❌ Failed to update database:`, error);
  }
}

// Pass the username as a command line argument
const userToFix = Deno.args[0] || "raja21";
await fixUser(userToFix);
