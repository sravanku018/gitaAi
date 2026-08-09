// ─── Gita App Monitor Bot ────────────────────────────────────
// Collects data, checks health, reports issues
// Usage: deno run --allow-net --allow-env monitor-bot.ts

const TELEGRAM_BOT_TOKEN = Deno.env.get("TELEGRAM_BOT_TOKEN") || "";
const TELEGRAM_CHAT_ID = Deno.env.get("TELEGRAM_CHAT_ID") || "";

// ─── Services to Monitor ─────────────────────────────────────
const SERVICES = [
  {
    name: "AI Server",
    url: "https://noisy-sheep-76.sravanku018.deno.net/",
    type: "http" as const,
  },
  {
    name: "Backend API",
    url: "https://prime-gorilla-49.sravanku018.deno.net/",
    type: "http" as const,
  },
  {
    name: "NVIDIA API",
    url: "https://integrate.api.nvidia.com/v1/models",
    type: "api" as const,
    headers: { "Authorization": `Bearer ${Deno.env.get("NVIDIA_API_KEY") || ""}` },
  },
  {
    name: "Groq API",
    url: "https://api.groq.com/openai/v1/models",
    type: "api" as const,
    headers: { "Authorization": `Bearer ${Deno.env.get("GROQ_API_KEY") || ""}` },
  },
];

// ─── Data Collectors ─────────────────────────────────────────
interface CheckResult {
  service: string;
  status: "ok" | "slow" | "down";
  latencyMs: number;
  statusCode: number;
  error?: string;
  timestamp: string;
}

interface DailyReport {
  date: string;
  totalChecks: number;
  successes: number;
  failures: number;
  avgLatency: number;
  issues: CheckResult[];
}

// ─── Health Check ────────────────────────────────────────────
async function checkService(service: typeof SERVICES[0]): Promise<CheckResult> {
  const start = Date.now();
  const timestamp = new Date().toISOString();

  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);

    const res = await fetch(service.url, {
      method: service.type === "api" ? "GET" : "GET",
      headers: service.headers || {},
      signal: controller.signal,
    });
    clearTimeout(timeout);

    const latencyMs = Date.now() - start;
    const status = !res.ok ? "down" : latencyMs > 5000 ? "slow" : "ok";

    return {
      service: service.name,
      status,
      latencyMs,
      statusCode: res.status,
      timestamp,
    };
  } catch (e) {
    return {
      service: service.name,
      status: "down",
      latencyMs: Date.now() - start,
      statusCode: 0,
      error: (e as Error).message,
      timestamp,
    };
  }
}

// ─── Collect All Data ────────────────────────────────────────
async function collectData(): Promise<CheckResult[]> {
  const results = await Promise.all(SERVICES.map(checkService));
  return results;
}

// ─── Check Voice Chat Endpoints ──────────────────────────────
async function checkVoiceChat(): Promise<CheckResult[]> {
  const endpoints = [
    { name: "Voice-NVIDIA", provider: "nvidia" },
    { name: "Voice-Groq", provider: "groq" },
    { name: "Voice-NVIDIA-Basic", provider: "nvidia-basic" },
  ];

  const results: CheckResult[] = [];

  for (const ep of endpoints) {
    const start = Date.now();
    try {
      const res = await fetch("https://noisy-sheep-76.sravanku018.deno.net/", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: "test",
          app: "gita",
          provider: ep.provider,
        }),
        signal: AbortSignal.timeout(15000),
      });
      const data = await res.json();
      results.push({
        service: ep.name,
        status: data.reply ? "ok" : "down",
        latencyMs: Date.now() - start,
        statusCode: res.status,
        timestamp: new Date().toISOString(),
      });
    } catch (e) {
      results.push({
        service: ep.name,
        status: "down",
        latencyMs: Date.now() - start,
        statusCode: 0,
        error: (e as Error).message,
        timestamp: new Date().toISOString(),
      });
    }
  }
  return results;
}

// ─── Report Formatting ───────────────────────────────────────
function formatReport(results: CheckResult[]): string {
  const now = new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });

  let report = `📊 *Gita App Health Report*\n`;
  report += `🕐 ${now}\n\n`;

  const ok = results.filter(r => r.status === "ok").length;
  const slow = results.filter(r => r.status === "slow").length;
  const down = results.filter(r => r.status === "down").length;

  report += `✅ Healthy: ${ok} | ⚠️ Slow: ${slow} | ❌ Down: ${down}\n\n`;

  for (const r of results) {
    const icon = r.status === "ok" ? "✅" : r.status === "slow" ? "⚠️" : "❌";
    report += `${icon} *${r.service}*\n`;
    report += `   ${r.latencyMs}ms | ${r.statusCode || "N/A"}`;
    if (r.error) report += ` | ${r.error}`;
    report += "\n";
  }

  if (down > 0) {
    report += `\n🚨 *ACTION REQUIRED*: ${down} service(s) down!`;
  }

  return report;
}

function formatDailyReport(report: DailyReport): string {
  let text = `📈 *Daily Summary — ${report.date}*\n\n`;
  text += `Total checks: ${report.totalChecks}\n`;
  text += `✅ Success: ${report.successes}\n`;
  text += `❌ Failures: ${report.failures}\n`;
  text += `⚡ Avg latency: ${report.avgLatency}ms\n`;

  if (report.issues.length > 0) {
    text += `\n⚠️ *Issues Found:*\n`;
    for (const issue of report.issues.slice(0, 5)) {
      text += `• ${issue.service}: ${issue.error || `HTTP ${issue.statusCode}`}\n`;
    }
  } else {
    text += `\n✨ All services healthy today!`;
  }

  return text;
}

// ─── Telegram Sender ─────────────────────────────────────────
async function sendTelegram(message: string) {
  if (!TELEGRAM_BOT_TOKEN || !TELEGRAM_CHAT_ID) {
    console.log("📱 Telegram not configured — printing report:\n");
    console.log(message);
    return;
  }

  try {
    await fetch(`https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: TELEGRAM_CHAT_ID,
        text: message,
        parse_mode: "Markdown",
      }),
    });
    console.log("📱 Report sent to Telegram");
  } catch (e) {
    console.error("Failed to send Telegram:", (e as Error).message);
  }
}

// ─── Store Results (JSON file) ───────────────────────────────
async function storeResults(results: CheckResult[]) {
  const filename = `monitor-${new Date().toISOString().split("T")[0]}.json`;
  let existing: CheckResult[] = [];

  try {
    const data = await Deno.readTextFile(filename);
    existing = JSON.parse(data);
  } catch {}

  existing.push(...results);
  await Deno.writeTextFile(filename, JSON.stringify(existing, null, 2));
}

// ─── Analyze Daily Data ──────────────────────────────────────
async function analyzeDay(date: string): Promise<DailyReport> {
  const filename = `monitor-${date}.json`;
  let data: CheckResult[] = [];

  try {
    const raw = await Deno.readTextFile(filename);
    data = JSON.parse(raw);
  } catch {
    return {
      date,
      totalChecks: 0,
      successes: 0,
      failures: 0,
      avgLatency: 0,
      issues: [],
    };
  }

  const successes = data.filter(r => r.status === "ok").length;
  const failures = data.filter(r => r.status !== "ok").length;
  const avgLatency = data.length > 0
    ? Math.round(data.reduce((sum, r) => sum + r.latencyMs, 0) / data.length)
    : 0;
  const issues = data.filter(r => r.status !== "ok");

  return {
    date,
    totalChecks: data.length,
    successes,
    failures,
    avgLatency,
    issues,
  };
}

// ─── Main Loop ───────────────────────────────────────────────
async function main() {
  console.log("🔍 Gita App Monitor Bot started");
  console.log(`Monitoring ${SERVICES.length} services + 3 voice chat endpoints`);

  const CHECK_INTERVAL = 5 * 60 * 1000; // 5 minutes
  const REPORT_HOUR = 21; // 9 PM IST daily report

  let lastReportDate = "";

  while (true) {
    try {
      // Collect health data
      const serviceResults = await collectData();
      const voiceResults = await checkVoiceChat();
      const allResults = [...serviceResults, ...voiceResults];

      // Store results
      await storeResults(allResults);

      // Print status
      const down = allResults.filter(r => r.status === "down");
      if (down.length > 0) {
        console.log(`\n🚨 ${down.length} service(s) down!`);
        const report = formatReport(allResults);
        await sendTelegram(report);
      } else {
        console.log(`✅ ${new Date().toLocaleTimeString()} — All services healthy`);
      }

      // Daily report at 9 PM IST
      const now = new Date();
      const istHour = (now.getUTCHours() + 5) % 24;
      const today = now.toISOString().split("T")[0];

      if (istHour === REPORT_HOUR && lastReportDate !== today) {
        const dailyReport = await analyzeDay(today);
        const reportText = formatDailyReport(dailyReport);
        await sendTelegram(reportText);
        lastReportDate = today;
      }

    } catch (e) {
      console.error("Monitor error:", (e as Error).message);
    }

    await new Promise(r => setTimeout(r, CHECK_INTERVAL));
  }
}

// ─── CLI Commands ────────────────────────────────────────────
const command = Deno.args[0];

if (command === "now") {
  // Run once and print
  const results = [...await collectData(), ...await checkVoiceChat()];
  console.log(formatReport(results));
} else if (command === "report") {
  // Daily report for today
  const today = new Date().toISOString().split("T")[0];
  const report = await analyzeDay(today);
  console.log(formatDailyReport(report));
} else {
  // Start monitoring loop
  await main();
}
