# Project Context & Session Status

- **Current Task**: Ship v2.19.1 (versionCode 52) — coin + admin APIs on VPS HTTPS.

- **Key Decisions**:
  - Coin API: https://gita.162.35.96.65.sslip.io/ (not flaky-kestrel).
  - Admin API: https://gita-admin.162.35.96.65.sslip.io/ (not lofty-crocodile).
  - Voice proxy: https://gita-voice.162.35.96.65.sslip.io/ (NVIDIA Nemotron 120B, Groq Qwen fallback).
  - Postgres 5432 is localhost-only.

- **Next Steps**:
  - Build and sideload 2.19.1 APK so phones leave Deno.
  - GitHub Pages dashboard uses updated index.html.
