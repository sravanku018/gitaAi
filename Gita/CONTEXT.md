# Project Context & Session Status

- **Current Task**: Ship v2.16.0 (versionCode 47) — pill reading UI, compact quiz buttons, drawer without broken Karma, VPS streak fix.

- **Key Decisions**:
  - Coin API: flaky-kestrel → VPS Postgres; admin dashboard via lofty-crocodile + DATABASE_URL.
  - Streak: server calculateStreak resets on gaps, no max(client)+1 double-count.
  - Reading UI: chapter/verse pills + pill bottom bar; Start Quiz compact width.

- **Next Steps**:
  - Redeploy vps-postrage-deno.ts / main.ts to flaky-kestrel for streak fix.
  - Build & install 2.16.0 APK; verify drawer + reading + quiz buttons.
