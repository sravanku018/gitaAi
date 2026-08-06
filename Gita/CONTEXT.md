# Project Context & Session Status

- **Current Task**: Completed version bump to v2.1.0 (versionCode 17), implemented tiered accuracy coin rewards, fixed Light Mode contrast/two-tone UI issues, redesigned Quiz top header/tabs & Home Namaste board, added Jetpack Compose @Preview annotations, and pushed all changes to GitHub.

- **Key Decisions**:
  - Coin Algorithm: Set base coins to 5 with tiered accuracy bonus (<50% -> +1, 50% -> +2, 60% -> +3, 70% -> +4, 80% -> +5, 90-100% -> +6). Max 15 coins cap. Excluded streak & check-in bonuses from quiz completion.
  - Light Mode UI Polish: Disabled top glossy white gradient overlays in Light Mode across all GlassCards and buttons to eliminate two-tone splits. Used Deep Saffron (#D84315) for high-contrast headings & text on yellow backgrounds.
  - Drawer & Quiz Redesign: Moved Logout/Sign In button to the very bottom of the navigation drawer. Converted Quiz mode tabs (15 Qs, 25 Qs, Battle Quiz) into modern pill segments. Redesigned Home Namaste hero card with elevated surface styling.

- **Next Steps**:
  - Deploy and test v2.1.0 build on Android device to verify end-to-end quiz completion, coin rewards, and light mode UI.
  - Verify sync with Deno/Hono backend database rules for tiered accuracy bonuses.
