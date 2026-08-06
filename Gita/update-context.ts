import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

async function main() {
  const transport = new StdioClientTransport({
    command: "npx",
    args: ["-y", "@smithery/cli@latest", "run", "@hannesrudolph/mcp-dual-context-store", "--config", `{"graphDirectory":"/home/sravan/Downloads/16-10-2025/Gita"}`]
  });
  
  const client = new Client({ name: "cli", version: "1.0.0" }, { capabilities: {} });
  await client.connect(transport);
  
  await client.callTool({
    name: "graph_add_memory",
    arguments: {
      type: "task",
      content: "Fixed backend bug causing HTTP 500 on duplicate shares from uninitialized users.",
      tags: ["bug", "backend", "rewards"],
      files: ["deno-backend-hono.ts"]
    }
  });

  await client.callTool({
    name: "graph_add_memory",
    arguments: {
      type: "task",
      content: "Refactored remaining UI screens to modular feature directories.",
      tags: ["refactor", "ui", "architecture"],
      files: ["SettingsScreen.kt", "FavoritesScreen.kt", "FlashcardsScreen.kt"]
    }
  });
  
  console.log("Memory updated");
  process.exit(0);
}
main().catch(console.error);
