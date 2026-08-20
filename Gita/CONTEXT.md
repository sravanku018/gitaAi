# Project Context & Session Status

- **Current Task**: Ship v2.18.0 (versionCode 50) — verify release build after version bump.

- **Key Decisions**:
  - Version bumped 2.17.0 (49) → 2.18.0 (50) in gradle + splash/settings fallbacks.
  - Quiz timer fix and Random Sloka theme/share work included in this tree.
  - Coin API remains flaky-kestrel → VPS Postgres.

- **Next Steps**:
  - Confirm `assembleRelease` succeeds with no errors.
  - Install 2.18.0 APK and smoke-test splash version text + quiz/share flows.
  - Redeploy streak fix to flaky-kestrel if not already live.
