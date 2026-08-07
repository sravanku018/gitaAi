# Project Context & Session Status

- **Current Task**: Version bump to v2.11.0 (versionCode 31); reward parity, server-only coin history, yoga mult 1/2/2/3/3.

- **Key Decisions**:
  - Server + app share quiz/battle/chapter formulas; yoga multipliers 1/2/2/3/3 integers.
  - Signed-in coin history is server-only (no double local+server logs).
  - Turso: schema gate, admin verify (not clean-duplicates on unlock).

- **Next Steps**:
  - Redeploy deno-backend-hono.ts; ship 2.11.0 APK.
  - Optional: push gitaAi main to origin.
