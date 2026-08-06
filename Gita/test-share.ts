import { config } from "https://deno.land/x/dotenv@v3.2.2/mod.ts";
config({ export: true, path: "./backend/.env" });

const req = await fetch("http://localhost:8000/share", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    user_id: "test_user_123",
    client_date: new Date().toISOString().split("T")[0]
  })
});
console.log(req.status);
console.log(await req.text());
