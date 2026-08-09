import { chromium } from "npm:playwright@1.49.1";

// ─── Simple Playwright Bot ───────────────────────────────────
// Usage: deno run --allow-net --allow-read --allow-env playwright-bot.ts

const BOT_TOKEN = Deno.env.get("TELEGRAM_BOT_TOKEN") || "";

async function runBot() {
  console.log("🤖 Starting Playwright Bot...");

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  // ─── Example 1: Scrape a webpage ──────────────────────────
  async function scrapePage(url: string): Promise<string> {
    await page.goto(url, { waitUntil: "domcontentloaded" });
    const title = await page.title();
    const text = await page.evaluate(() => {
      return document.body?.innerText?.slice(0, 500) || "No content";
    });
    return `Title: ${title}\nContent: ${text}`;
  }

  // ─── Example 2: Fill a form ───────────────────────────────
  async function fillForm(url: string, data: Record<string, string>) {
    await page.goto(url);
    for (const [selector, value] of Object.entries(data)) {
      await page.fill(selector, value);
    }
    await page.screenshot({ path: "form-filled.png" });
    console.log("✅ Form filled and screenshot saved");
  }

  // ─── Example 3: Take screenshots ──────────────────────────
  async function screenshot(url: string, filename: string) {
    await page.goto(url, { waitUntil: "networkidle" });
    await page.screenshot({ path: filename, fullPage: true });
    console.log(`📸 Screenshot saved: ${filename}`);
  }

  // ─── Example 4: Monitor a page for changes ────────────────
  async function monitorPage(url: string, intervalMs: number) {
    let lastContent = "";
    while (true) {
      await page.goto(url);
      const content = await page.evaluate(() => document.body?.innerText || "");
      if (content !== lastContent) {
        console.log(`🔔 Page changed at ${new Date().toISOString()}`);
        lastContent = content;
      }
      await new Promise(r => setTimeout(r, intervalMs));
    }
  }

  // ─── Run examples ─────────────────────────────────────────
  console.log("\n--- Scrape Example ---");
  const result = await scrapePage("https://example.com");
  console.log(result);

  console.log("\n--- Screenshot Example ---");
  await screenshot("https://example.com", "example-screenshot.png");

  await browser.close();
  console.log("\n✅ Bot finished");
}

// ─── Telegram Bot Integration ────────────────────────────────
// Connect Playwright to a Telegram bot for automated browsing

async function telegramBot() {
  if (!BOT_TOKEN) {
    console.log("No TELEGRAM_BOT_TOKEN set — running scrape demo only");
    await runBot();
    return;
  }

  console.log("🤖 Starting Telegram + Playwright Bot...");
  const browser = await chromium.launch({ headless: true });

  let offset = 0;

  while (true) {
    try {
      const res = await fetch(
        `https://api.telegram.org/bot${BOT_TOKEN}/getUpdates?offset=${offset}&timeout=30`
      );
      const data = await res.json();

      for (const update of data.result || []) {
        offset = update.update_id + 1;
        const msg = update.message;
        if (!msg?.text) continue;

        const chatId = msg.chat.id;
        const text = msg.text;
        const user = msg.from?.first_name || "Seeker";

        console.log(`📩 ${user}: ${text}`);

        let reply = "";

        if (text.startsWith("/scrape ")) {
          const url = text.replace("/scrape ", "");
          const page = await browser.newPage();
          try {
            await page.goto(url, { waitUntil: "domcontentloaded", timeout: 10000 });
            const title = await page.title();
            reply = `📄 *${title}*\n\nScraped successfully from ${url}`;
          } catch (e) {
            reply = `❌ Failed to scrape: ${(e as Error).message}`;
          }
          await page.close();

        } else if (text.startsWith("/screenshot ")) {
          const url = text.replace("/screenshot ", "");
          const page = await browser.newPage();
          try {
            await page.goto(url, { waitUntil: "networkidle", timeout: 15000 });
            const filename = `screenshot_${chatId}.png`;
            await page.screenshot({ path: filename, fullPage: false });
            reply = `📸 Screenshot taken!`;
          } catch (e) {
            reply = `❌ Screenshot failed: ${(e as Error).message}`;
          }
          await page.close();

        } else if (text === "/start") {
          reply = `🙏 Namaste ${user}!\n\nI am your browser automation bot.\n\nCommands:\n/scrape <url> - Scrape a webpage\n/screenshot <url> - Take a screenshot\n/help - Show this message`;

        } else if (text === "/help") {
          reply = `Commands:\n/scrape <url> - Get page content\n/screenshot <url> - Capture page`;

        } else {
          reply = "Unknown command. Use /help";
        }

        await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/sendMessage`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ chat_id: chatId, text: reply, parse_mode: "Markdown" }),
        });
      }
    } catch (e) {
      console.error("Polling error:", (e as Error).message);
      await new Promise(r => setTimeout(r, 5000));
    }
  }
}

// ─── Entry point ─────────────────────────────────────────────
if (Deno.args.includes("--demo")) {
  await runBot();
} else {
  await telegramBot();
}
