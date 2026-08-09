import { createClient } from "npm:@libsql/client";
const db = createClient({ url: "file:local-test.db" });
const res = await db.execute("SELECT * FROM user_stats WHERE user_id = 'test_user_1'");
console.log(res.rows);
