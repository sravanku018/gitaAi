// ─── Gita Voice/Chat Model Test Suite ───────────────────────
// Usage: VOICE_URL=https://noisy-sheep-76.sravanku018.deno.net deno run --allow-net --allow-env test-voice.ts

const VOICE_URL = Deno.env.get("VOICE_URL") || "https://noisy-sheep-76.sravanku018.deno.net";

let passed = 0;
let failed = 0;
const failures: string[] = [];

// ─── HELPERS ─────────────────────────────────────────────────
async function chat(messages: { role: string; content: string }[]): Promise<any> {
  const res = await fetch(VOICE_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ messages }),
  });
  return res.json();
}

async function simpleChat(message: string): Promise<any> {
  const res = await fetch(VOICE_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });
  return res.json();
}

function assert(name: string, condition: boolean, detail = "") {
  if (condition) {
    console.log(`  ✅ ${name}`);
    passed++;
  } else {
    console.log(`  ❌ ${name}${detail ? ` — ${detail}` : ""}`);
    failed++;
    failures.push(`${name}${detail ? `: ${detail}` : ""}`);
  }
}

function section(name: string) {
  console.log(`\n─── ${name} ${"─".repeat(40 - name.length)}`);
}

function printReply(label: string, reply: string) {
  console.log(`\n  💬 ${label}:`);
  console.log(`     ${reply.slice(0, 200)}${reply.length > 200 ? "..." : ""}`);
}

// ═══════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════

// ─── 1. SERVER HEALTH ────────────────────────────────────────
section("SERVER HEALTH");
{
  const res = await fetch(VOICE_URL);
  const text = await res.text();
  assert("GET / — server alive", res.ok, text);
  console.log(`  ℹ️  Server: ${text}`);
}

// ─── 2. SIMPLE MESSAGE FORMAT ────────────────────────────────
section("SIMPLE MESSAGE FORMAT");
{
  const res = await simpleChat("What is the Bhagavad Gita?");
  assert("Simple message — has reply", typeof res.reply === "string", JSON.stringify(res));
  assert("Simple message — reply not empty", res.reply.length > 10);
  assert("Simple message — not silent", res.reply !== "The cards are silent...");
  printReply("Gita question", res.reply);
}

// ─── 3. MESSAGES ARRAY FORMAT ────────────────────────────────
section("MESSAGES ARRAY FORMAT");
{
  const res = await chat([
    { role: "user", content: "What is karma?" }
  ]);
  assert("Messages format — has reply", typeof res.reply === "string");
  assert("Messages format — reply not empty", res.reply.length > 10);
  printReply("Karma question", res.reply);
}

// ─── 4. GITA CONTENT QUESTIONS ───────────────────────────────
section("GITA CONTENT QUESTIONS");
{
  const questions = [
    "What does Krishna say about duty in Bhagavad Gita?",
    "Explain the concept of Dharma",
    "What is the meaning of Chapter 2 verse 47?",
    "How to achieve moksha according to Gita?",
  ];

  for (const q of questions) {
    const res = await simpleChat(q);
    const hasReply = typeof res.reply === "string" && res.reply.length > 20;
    assert(`Gita Q: "${q.slice(0, 40)}..."`, hasReply);
    if (hasReply) printReply("Reply", res.reply);
  }
}

// ─── 5. TELUGU QUESTIONS ─────────────────────────────────────
section("TELUGU LANGUAGE");
{
  const teluguQuestions = [
    "భగవద్గీత అంటే ఏమిటి?",
    "కర్మ యోగం గురించి చెప్పండి",
    "ధర్మం అంటే ఏమిటి?",
  ];

  for (const q of teluguQuestions) {
    const res = await simpleChat(q);
    const hasReply = typeof res.reply === "string" && res.reply.length > 10;
    assert(`Telugu Q: "${q}"`, hasReply);
    if (hasReply) printReply("Reply", res.reply);
  }
}

// ─── 6. MULTI-TURN CONVERSATION ──────────────────────────────
section("MULTI-TURN CONVERSATION");
{
  const res = await chat([
    { role: "user", content: "What is karma?" },
    { role: "assistant", content: "Karma refers to the law of cause and effect..." },
    { role: "user", content: "Can you give me an example from Gita?" },
  ]);
  assert("Multi-turn — has reply", typeof res.reply === "string" && res.reply.length > 10);
  assert("Multi-turn — context aware", !res.reply.includes("The cards are silent"));
  printReply("Multi-turn reply", res.reply);
}

// ─── 7. EDGE CASES ───────────────────────────────────────────
section("EDGE CASES");
{
  // Empty message
  const empty = await simpleChat("");
  assert("Empty message — no crash", typeof empty.reply === "string");

  // Very short message
  const short = await simpleChat("Hi");
  assert("Short message — responds", typeof short.reply === "string" && short.reply.length > 0);
  printReply("Hi reply", short.reply);

  // Very long message
  const long = await simpleChat("What does Krishna say about " + "karma ".repeat(50) + "in the Bhagavad Gita?");
  assert("Long message — responds", typeof long.reply === "string" && long.reply.length > 0);

  // Off-topic question
  const offTopic = await simpleChat("What is the capital of France?");
  assert("Off-topic — no crash", typeof offTopic.reply === "string");
  printReply("Off-topic reply", offTopic.reply);
}

// ─── 8. RESPONSE QUALITY ─────────────────────────────────────
section("RESPONSE QUALITY");
{
  const res = await simpleChat("What does Bhagavad Gita Chapter 2 Verse 47 say?");
  const reply = res.reply || "";

  // Check for Gita-related keywords
  const hasGitaContext = /karma|duty|action|result|fruit|कर्म|dharma|arjuna|krishna/i.test(reply);
  assert("Quality — Gita context in reply", hasGitaContext, `Reply: ${reply.slice(0, 100)}`);

  // Check reply is substantial
  assert("Quality — reply > 50 chars", reply.length > 50, `Length: ${reply.length}`);

  // Check not error message
  const isError = reply.includes("veil is thick") || reply.includes("cards are silent");
  assert("Quality — not error fallback", !isError);

  printReply("Chapter 2:47", reply);
}

// ─── 9. LATENCY CHECK ────────────────────────────────────────
section("LATENCY");
{
  const start = Date.now();
  await simpleChat("What is yoga?");
  const latency = Date.now() - start;

  assert("Latency — under 10s", latency < 10000, `${latency}ms`);
  console.log(`  ℹ️  Response time: ${latency}ms`);

  if (latency < 3000) console.log("  ⚡ Fast response!");
  else if (latency < 6000) console.log("  🟡 Acceptable response time");
  else console.log("  🔴 Slow — check Groq API or model");
}

// ─── 10. INVALID REQUEST ─────────────────────────────────────
section("INVALID REQUEST");
{
  // No body
  const res = await fetch(VOICE_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: "{}",
  });
  const data = await res.json();
  assert("Empty body — no 500 crash", res.status !== 500, `Status: ${res.status}`);
  assert("Empty body — has reply field", typeof data.reply === "string");
}

// ─── RESULTS ─────────────────────────────────────────────────
console.log("\n" + "═".repeat(50));
console.log(`RESULTS: ${passed} passed, ${failed} failed`);
if (failures.length > 0) {
  console.log("\nFailed tests:");
  failures.forEach(f => console.log(`  ❌ ${f}`));
}
console.log("═".repeat(50));

Deno.exit(failed > 0 ? 1 : 0);
