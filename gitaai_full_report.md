# gitaAi — Complete Bug, Improvement & Feature Report
> Generated: June 25, 2026 | Codebase: 48,000 lines, 311 Kotlin files, 494 total files

---

## 🔴 CRITICAL BUGS (Fix Immediately)

### Bug 1 — Day 7 Double Coins
**File:** `ui/components/DailyRewardsStrip.kt`
**Problem:** `claimDaily()` returns 7 coins for Day 7. Then `claimDay7BonusIfEligible()` is called separately and credits another 7 coins to the backend silently. UI shows correct amount but backend receives double.
**Fix:**
```kotlin
// Remove this line on Day 7 click:
tracker.claimDay7BonusIfEligible()  // ← DELETE THIS
// claimDaily() already handles Day 7 internally
val total = coins + weeklyState.reward
```

### Bug 2 — Share Verse No Coin Reward (UI)
**File:** `ui/NormalModeScreen.kt`
**Problem:** Share button in NormalModeScreen triggers Android share intent but never calls `onShareClaim` — users share verses and get zero coins.
**Fix:** After `context.startActivity(...)` in `shareVerse()`, trigger the share coin reward via ViewModel.

---

## 🟡 MEDIUM BUGS (Fix This Week)

### Bug 3 — Quiz Filter Chips Missing 15 & 25
**File:** `ui/QuizStatsScreen.kt` + `viewmodel/QuizStatsViewModel.kt`
**Problem:** App offers 15 & 25 question quizzes but stats screen only shows chips for 10, 20, 30. Attempts from 15/25 quizzes are invisible in filtered view.
**Fix:** Add `_quiz15Stats` and `_quiz25Stats` StateFlows in ViewModel, add corresponding chips in screen.

### Bug 4 — N+1 DB Queries in loadGroupedStats()
**File:** `viewmodel/QuizStatsViewModel.kt`
**Problem:** 4 suspend DB calls inside each `collect{}` block — fires on every list update causing lag on slow devices.
**Fix:** Use existing `getStatsByQuizSize()` DAO method which combines all 4 queries:
```kotlin
// Replace 4 separate calls with:
_quiz10Stats.value = questionBankDao.getStatsByQuizSize(10)
```

### Bug 5 — Difficulty Never Passed to Question Bank Query
**File:** `viewmodel/QuizViewModel.kt`
**Problem:** `AdaptiveDifficultyEngine` tracks skill level correctly but `getNextQuestions()` always called with `minDiff=1, maxDiff=10`. The entire adaptive system is built but disconnected from question selection.
**Fix:**
```kotlin
val diff = userState.skillLevel
val candidates = quizQuestionRepository.getNextQuestions(
    minDiff = (diff - 2).coerceAtLeast(1),
    maxDiff = (diff + 2).coerceAtMost(10),
    fetchLimit = fetchLimit,
    targetDifficulty = diff
)
```

### Bug 6 — Badges Stat Shows Wrong Data
**File:** `ui/ProfileScreen.kt`
**Problem:** Badges count shows `totalFavorites` value — wrong field.
**Fix:** Use correct badges/achievements field from UserStats.

### Bug 7 — Quiz Start Button Wrong Language
**File:** `ui/ProtectedQuizConfigScreen.kt`
**Problem:**
```kotlin
text = if (language == "tel") "ప్రారంభించండి" else "प्रारंभ करें"
```
English users see Hindi text.
**Fix:** Add English case: `else -> "Begin Quiz"`

### Bug 8 — correctAnswer vs correctOptionIndex Mismatch
**File:** `ml/DatasetIngestionPipeline.kt`
**Problem:** DB stores `correctAnswer` as full text string. `QuizViewModel.loadNextQuestion()` looks up correct answer by index. If alignment drifts, wrong answer is marked correct.
**Fix:** Ensure `correctOptionIndex` is always saved alongside `correctAnswer` and use index consistently.

---

## 🟢 MINOR BUGS (Fix When Time Permits)

### Bug 9 — LlmInferenceEngine.close() Does Nothing
**File:** `ml/LlmInferenceEngine.kt`
**Problem:** `close()` is empty — memory never freed when ViewModel destroyed.
**Fix:** Call `stopGeneration()` at minimum inside `close()`.

### Bug 10 — Duplicate WrongOverlay.kt
**Files:** `com/aipoweredgita/app/WrongOverlay.kt` AND `com/aipoweredgita/app/ui/WrongOverlay.kt`
**Problem:** Same file in two packages — dead code confusion.
**Fix:** Delete root-level one, keep `ui/WrongOverlay.kt`.

### Bug 11 — mapAssetModel() Resource Leak
**File:** `ml/ModelInferenceEngine.kt`
**Problem:** `FileChannel` closed while `MappedByteBuffer` still holds reference — potential `ClosedChannelException` on some Android versions.
**Fix:** Keep `FileInputStream` open until buffer is no longer needed, or use `FileChannel.open()` directly.

### Bug 12 — Thompson Sampling Normal Approximation
**File:** `ml/MultiArmedBandit.kt` (if restored)
**Problem:** Uses Normal distribution approximation instead of true Beta distribution — inaccurate for low attempt counts (0-5).
**Fix:** Use proper Beta distribution sampling via inverse CDF.

### Bug 13 — now() Inconsistency Edge Case
**File:** `coin/DailyRewardsTracker.kt`
**Problem:** Some methods call `now()` multiple times — if called across midnight boundary, date could change mid-logic.
**Fix:** Cache `val today = now()` at top of each method consistently (partially done, not complete).

### Bug 14 — HomeScreen DB Access in Composable
**File:** `ui/HomeScreen.kt`
**Problem:** Direct Room DB access inside Composable violates MVVM — can cause ANR on main thread.
**Fix:** Move yoga progression fetch to a ViewModel.

### Bug 15 — Light Mode No Background Animation
**File:** `ui/HomeScreen.kt`
**Problem:** `AmbientOrbs` only shown in dark mode — light mode looks plain/empty.
**Fix:** Add a subtle light-mode background (soft mandala or particle effect).

### Bug 16 — Appearance Card Looks Broken
**File:** `ui/ProfileScreen.kt`
**Problem:** Appearance card with dark/light mode icon shows only text — looks like a broken button.
**Fix:** Either make it functional or remove the card icon.

### Bug 17 — IRT Derivatives Missing Guessing Correction
**File:** `ml/ItemResponseTheory.kt`
**Problem:** MLE derivatives missing `(P - c) / (1 - c)` correction for guessing parameter — ability estimation slightly optimistic for lucky guessers.
**Fix:** Add guessing correction to `calculateDerivatives()`.

### Bug 18 — Fill-in-blank Distractor is Reversed Word
**File:** `ml/HuggingFaceMLManager.kt`
**Problem:** One distractor option was reversed word (e.g. "dharma" → "amrahd"). Now partially fixed but verify fully.

### Bug 19 — UCB Hardcoded Total Visits
**File:** `ml/MultiArmedBandit.kt` (if restored)
**Problem:** `ln(10.0)` hardcoded instead of `ln(totalAttempts across ALL arms)` — UCB exploration formula incorrect.
**Fix:** Pass total arm pulls count to UCB calculation.

---

## 🔀 CODE IMPROVEMENTS

### Improvement 1 — Delete Dead Screens
**Files to delete:**
- `ui/QuizScreenNew.kt` — 9 lines, just wraps QuizContent, no nav route
- `ui/QuizConfigScreen.kt` — old plain version replaced by ProtectedQuizConfigScreen
- `ui/VerseScreen.kt` — 10 lines, just calls NormalModeScreen

### Improvement 2 — Combine DailyActivityScreen + ActivityHistoryScreen
**Problem:** Both show activity data with overlapping content.
**Fix:** Add DailyActivity content as a new "Calendar" tab inside ActivityHistoryScreen. Delete DailyActivityScreen.

### Improvement 3 — Combine Quiz Config Screens
**Problem:** `QuizConfigScreen`, `ProtectedQuizConfigScreen`, `SegmentBasedQuizConfigScreen` all exist separately.
**Fix:** Merge segment selection as a collapsible section inside `ProtectedQuizConfigScreen`.

### Improvement 4 — Move ModelDownload into Settings
**Problem:** `ModelDownloadScreen` is a one-time setup screen that doesn't need its own nav destination.
**Fix:** Make it a section inside `SettingsScreen`.

### Improvement 5 — Wire IRT + Elo + Bandit to Quiz Flow
**Problem:** `EloRatingSystem`, `ItemResponseTheory`, `MultiArmedBandit` are fully built but never called.
**Fix:** After each answer in `QuizViewModel.confirmAnswer()`:
```kotlin
// Update Elo ratings
eloSystem.updateFromQuiz(studentEntity, questionEntity, isCorrect)
// Update IRT ability
irtEngine.updateAbility(studentAbility, listOf(itemParams to isCorrect))
// Update Bandit
bandit.update(questionId, isCorrect)
```

### Improvement 6 — Use getStatsByQuizSize() Already in DAO
**Problem:** ViewModel makes 4 separate DB calls when DAO already has a combined query `getStatsByQuizSize()`.
**Fix:** Replace 4 calls with single DAO method call.

### Improvement 7 — Persist userState Across Sessions
**File:** `viewmodel/QuizViewModel.kt`
**Problem:** `userState` (AdaptiveDifficultyEngine.UserState) is in-memory only — resets every app launch.
**Fix:** `AdaptiveDifficultyEngine.saveState()` and `loadState()` already exist — just call them in `init` and `onCleared()`.
```kotlin
init {
    userState = AdaptiveDifficultyEngine.loadState(prefs)
}
override fun onCleared() {
    AdaptiveDifficultyEngine.saveState(userState, prefs)
}
```

### Improvement 8 — QuizSectionScreen Not in Bottom Nav
**Problem:** Quiz section accessible from drawer but bottom nav Quiz tab goes directly to QuizConfig — users skip segment selection entirely.
**Fix:** Bottom nav Quiz → QuizSectionScreen first, then user chooses config.

### Improvement 9 — HomeScreen vs DashboardScreen Overlap
**Problem:** Both screens exist. Nav uses `HomeKey` — verify which one is actually rendered and delete the other.

### Improvement 10 — SmartTimingPredictor Uses Simulated Hourly Data
**File:** `notifications/SmartTimingPredictor.kt`
**Problem:** "Peak activity assumed 6-10 AM and 6-10 PM" — hardcoded assumption, not real user data. Real hourly tracking not implemented.
**Fix:** Store hourly activity timestamps in Room DB for accurate pattern detection.

---

## 🆕 NEW FEATURES (After Core is Stable)

### Feature 1 — Weekly AI Insights Card (LOW EFFORT, HIGH VALUE)
**What:** Every Sunday, Groq generates a personal learning summary shown as a card on Dashboard:
- "This week you focused on Karma Yoga"
- "Quiz accuracy improved 12% in Chapter 3"
- "Krishna's message for you this week: ..."
**Effort:** 1-2 days. All data exists. One Groq API call per week.
**Cost:** Free (Groq free tier, one call/week per user).

### Feature 2 — Verse Notes / Personal Journal (LOW EFFORT)
**What:** Users can annotate any verse with personal notes:
- Tap "Add Note" on any verse in NormalModeScreen
- Stored locally in Room DB (new entity: `VerseNote`)
- Viewable in Favorites or new Notes tab
- Exportable as PDF using existing PDF skill
**Effort:** 2-3 days. Just one new Room entity + simple UI.
**Cost:** Zero — fully offline.

### Feature 3 — Smart Study Plans (MEDIUM EFFORT)
**What:** Structured reading plans:
- "14-day Karma Yoga deep dive"
- "7-day quiz challenge"
- "18-day complete Gita journey"
- Progress tracked per plan
- WorkManager reminders to stay on track
**Effort:** 3-4 days. `RecommendationsScreen` exists as a starting point.

### Feature 4 — Meditation Timer (MEDIUM EFFORT)
**What:** Simple verse-based meditation experience:
- Select a verse → enter meditation mode
- Breathing guide (inhale 4s / hold 4s / exhale 4s)
- Ambient background (existing MandalaBackground + ParticleField)
- Configurable timer (5/10/15/20 min)
- Earns Krishna Coins on completion
- Tracks meditation time in UserStats
**Effort:** 3-4 days. Reuses existing background components.
**Why:** Differentiates from other Gita apps — none have meditation mode.

### Feature 5 — Telugu Transliteration Input (HIGH VALUE, MEDIUM EFFORT)
**What:** When user types in Voice Studio chat box:
- Type in English phonetics → auto-converts to Telugu script
- Example: "nenu krishnudu" → "నేను కృష్ణుడు"
- Toggle button to switch between transliteration and normal
**Effort:** 4-5 days. Google Indic keyboard library available.
**Why:** Makes the app genuinely native for Telugu users — huge differentiator.

### Feature 6 — Verse of the Day Home Widget (LOW EFFORT)
**What:** Enhance existing widget:
- Today's verse with Telugu translation
- Streak counter visible on widget
- Tap to open app at that specific verse
- Share directly from widget
- Dark/light theme aware
**Effort:** 2 days. `WidgetSettingsScreen` already exists.

### Feature 7 — Quiz Battle Mode (HIGH EFFORT, HIGH VALUE)
**What:** Timed quiz with pressure mechanics:
- 60 second countdown per question (not 30)
- Combo multiplier for consecutive correct answers
- "Last Stand" — 3 lives system
- Score multiplied by yoga level bonus
- Results shareable as image card
**Effort:** 5-7 days. Reuses existing QuizViewModel with new config.
**Why:** Highly shareable, drives organic installs.

### Feature 8 — Gita Search (MEDIUM EFFORT)
**What:** Full-text search across all 700 verses:
- Search by keyword ("karma", "dharma", "peace")
- Search by chapter/verse reference
- Results show verse snippet + chapter context
- Tap to open in NormalModeScreen
- Uses existing Room DB cached verses
**Effort:** 2-3 days. Room FTS (Full Text Search) is straightforward.
**Why:** Most Gita apps lack good search. High utility feature.

### Feature 9 — Audio Pronunciation Guide (LOW EFFORT)
**What:** Sanskrit verse pronunciation for each verse:
- Play button on verse card
- Pre-recorded Sanskrit pronunciation audio
- Playback speed control (0.75x / 1x / 1.5x)
- Download for offline use
**Effort:** 2-3 days (if audio files sourced). Zero AI cost.
**Why:** Sanskrit pronunciation is a real user need — common request in Gita apps.

### Feature 10 — Progress Sharing Card (LOW EFFORT, HIGH MARKETING VALUE)
**What:** Beautiful shareable image card showing:
- Yoga progression level + Sanskrit name
- Current streak
- Verses read count
- Krishna Coins balance
- Generated as Bitmap from Compose canvas
- Share via Android share intent
**Effort:** 1-2 days. Canvas drawing + bitmap export.
**Why:** Viral loop — users share to Instagram/WhatsApp → organic installs.

---

## 📊 PRIORITY MATRIX

### Fix Now (Before Play Store):
1. Bug 1 — Day 7 double coins
2. Bug 2 — Share verse no coin reward
3. Bug 3 — Quiz filter chips 15/25
4. Bug 5 — Difficulty not passed to DB query
5. Improvement 7 — Persist userState across sessions

### Fix Soon (First Week After Launch):
6. Bug 4 — N+1 DB queries
7. Bug 6 — Badges wrong data
8. Bug 7 — Quiz start button wrong language
9. Improvement 1 — Delete dead screens
10. Improvement 5 — Wire IRT/Elo/Bandit

### After Launch Feedback:
11. Feature 1 — Weekly AI Insights (easiest, highest value)
12. Feature 10 — Progress Sharing Card (viral potential)
13. Feature 8 — Gita Search (high utility)
14. Feature 2 — Verse Notes (low effort)

### When Mature:
15. Feature 4 — Meditation Timer
16. Feature 3 — Study Plans
17. Feature 5 — Telugu Transliteration
18. Feature 7 — Quiz Battle Mode
19. Feature 9 — Audio Pronunciation

---

## 📈 SUMMARY

| Category | Count |
|----------|-------|
| Critical bugs | 2 |
| Medium bugs | 6 |
| Minor bugs | 11 |
| Code improvements | 10 |
| New features | 10 |
| **Total items** | **39** |

**Estimated fix time for critical + medium bugs:** 3-5 days
**Estimated time to Play Store ready:** 1-2 weeks

