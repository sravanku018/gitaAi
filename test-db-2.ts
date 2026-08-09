import { createClient } from "npm:@libsql/client";
const db = createClient({ url: "file:local-test.db" });
const res = await db.execute("SELECT * FROM coin_transactions WHERE source = 'share_sloka'");
console.log(res.rows);
