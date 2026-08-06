# Project Context & Session Status

- **Current Task**: Version bump to v2.10.0 (versionCode 30); Turso cost cuts (schema gate, history limits, balance/history TTL).

- **Key Decisions**:
  - Server: schema_meta gate skips full initTables on cold start; coin history default 100.
  - App: 10 min balance TTL; history limit 100 with force refresh on pull-to-refresh.
  - Keep server-truth coins; no pure local coin DB.

- **Next Steps**:
  - Confirm Deno deploy has SCHEMA_VERSION / history default 100 (raja21 no-limit ≤100).
  - Ship 2.10.0 APK; admin secrets / batch SQL later.
