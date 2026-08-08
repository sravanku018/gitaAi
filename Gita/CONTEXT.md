# Project Context & Session Status

- **Current Task**: Ship v2.12.2 (versionCode 43) with flaky-kestrel Postgres coin API.

- **Key Decisions**:
  - Coin API: `https://flaky-kestrel-5072.sravanku018.deno.net/` (Oracle PG), not prime-gorilla Turso.
  - App version fallbacks in Splash/Settings match BuildConfig 2.12.2 / 43.
  - Signed-in coin history server-only; yoga mult 1/2/2/3/3; voice costs 4/6/10.

- **Next Steps**:
  - Install 2.12.2 APK on device; verify auth/coins against flaky-kestrel.
  - Afternoon: scrub GitHub Pages secrets; rotate Turso/TOTP if still exposed.
