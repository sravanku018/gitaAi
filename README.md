# 🕉️ gitaAi — AI-Powered Bhagavad Gita Learning App

> *Timeless wisdom of the Bhagavad Gita, powered by modern AI.*  
> Ask Krishna anything — about life, dharma, karma, or your struggles — and receive guidance grounded in the Gita's verses.

![Platform](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Language](https://img.shields.io/badge/Language-Kotlin-blue?logo=kotlin)
![AI](https://img.shields.io/badge/AI-NVIDIA%20NIM%20%7C%20Groq%20%7C%20LiteRT-orange)
![Version](https://img.shields.io/badge/Version-2.12.2-purple)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## 📸 Screenshots

<div align="center">
  <img src="screenshots/dashboard.png" width="200" alt="Dashboard"/>
  <img src="screenshots/voice_studio.png" width="200" alt="Voice Studio / AI Chat"/>
  <img src="screenshots/quiz.png" width="200" alt="Quiz Mode"/>
  <img src="screenshots/profile.png" width="200" alt="Profile & Progression"/>
</div>

---

## 📱 What is gitaAi?

**gitaAi** is an Android app that brings the Bhagavad Gita to life through conversational AI. Talk to an AI Krishna, test your knowledge with adaptive quizzes, track your spiritual progression through yoga levels, and earn Krishna Coins for daily learning — all with full offline support.

Supports **Telugu, English, and Sanskrit**.

---

## ✨ Features

### Learning Modes
- **Normal Mode** — Read and explore verses with translations and purports
- **Quiz Mode** — Adaptive MCQ, essay, comparison, and application questions
- **Voice Studio** — AI-powered Q&A with Krishna using on-device ML models
- **Random Sloka** — Daily inspiration from random verses
- **Flashcards** — Topic-based spaced repetition learning
- **Meditation Timer** — Track meditation sessions with coin rewards

### AI & ML Capabilities
- On-device inference with **Qwen3 0.6B** and **Gemma 4 2B** via LiteRT-LM (no internet needed)
- Cloud AI via **NVIDIA Nemotron Super 120B** (primary) and **Groq Llama 3.3 70B** (fallback)
- Adaptive difficulty engine using **Elo rating + Item Response Theory**
- Google ML Kit for on-device translation
- Smart recommendations based on learning patterns
- Persistent AI memory — Krishna remembers your journey across sessions

### Gamification
- 🪙 Krishna Coins reward system (server-synced)
- 🧘 Yoga progression — 5 levels with karma multipliers (1x/2x/2x/3x/3x)
- 🔥 Daily check-in streaks (7-day cycles)
- 📤 Daily share streaks
- 🏆 Global leaderboard
- 🎖️ Achievement badges

### Guest & User System
- **Guest Mode** — Instant access with unique ID (e.g., `guest_9Y93YB`)
- **Signed-in Users** — Full sync across devices
- **Coin History** — Server-backed transaction logging
- **Verse Notes** — Save and sync personal notes
- **Activity Tracking** — All actions logged to server

### Offline Support
- Download all verses (~3–4 MB) for offline reading
- Background sync with WorkManager
- Pending event queue for offline actions

---

## 🏗️ Architecture

```
MVVM + Clean Architecture
├── UI Layer (Jetpack Compose)
│   ├── Screens (40+ composables)
│   ├── Components (Glass cards, ambient orbs, animations)
│   └── Theme (Material3, dynamic colors, dark/light)
├── Domain Layer
│   ├── Models (UI state, events, side effects)
│   └── Use Cases
├── Data Layer
│   ├── Room Database (24 entities, 50+ versions)
│   ├── Retrofit APIs (Gita + Coin services)
│   └── Repositories (28 repositories)
└── DI (Hilt modules)
```

---

## 🛠️ Tech Stack

### Android Client

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose, Material3 |
| DI | Hilt |
| Database | Room (v50, 24 entities) |
| Networking | Retrofit, OkHttp |
| ML | TensorFlow Lite, LiteRT-LM |
| Navigation | Navigation Compose |
| Async | Coroutines, Flow |
| Work | WorkManager |
| Serialization | Gson, Moshi |

### Backend (Deno + PostgreSQL)

| Category | Technology |
|----------|------------|
| Runtime | Deno (TypeScript) |
| Framework | Hono |
| Database | PostgreSQL (VPS Oracle) |
| Deployment | Deno Deploy |
| AI — Primary | NVIDIA NIM (Nemotron Super 120B, MoE) |
| AI — Fallback | Groq API (Llama 3.3 70B) |
| Admin | TOTP 2FA + Admin Dashboard |

---

## 🧬 On-Device AI — Device Capability Tiers

gitaAi auto-detects hardware and selects the optimal model:

| Tier | Devices | Model |
|------|---------|-------|
| `FLAGSHIP` | Snapdragon 8 Gen 2+, 8GB+ RAM | Gemma 4B full |
| `MID` | Snapdragon 7+ Gen 3, 6GB RAM | Qwen3 0.6B / Gemma 2B |
| `LOW` | Older / low RAM | Cloud fallback only |

> Primary test device: **OnePlus Nord 4** (Snapdragon 7+ Gen 3)

---

## 🔐 Key Engineering Decisions

- **Unified Guest/User Flows** — Guests use same code paths as signed-in users
- **Server-Side Coins** — All coin transactions logged to PostgreSQL
- **Unique Guest IDs** — 6-character alphanumeric IDs for tracking
- **Prompt Injection Protection** — `GitaPromptEngine.kt` sanitizes user input
- **Emoji Stripping** — Backend strips emoji for clean Telugu/Devanagari rendering
- **Timezone-Safe Streaks** — Daily check-in uses IST normalization
- **Hybrid Memory** — DataStore (local) + PostgreSQL cloud + Groq session summaries
- **Splash → Login Flow** — Always shows login after splash for fresh sessions

---

## 📂 Project Structure

```
gitaAi/
├── Gita/                                      # Android app
│   └── app/src/main/java/com/aipoweredgita/
│       ├── coin/              # Coin reward engine
│       ├── data/              # Data models (GitaVerse, QuizQuestion)
│       ├── database/          # Room entities and DAOs (24 tables)
│       ├── di/                # Hilt dependency injection
│       ├── domain/            # Domain models and use cases
│       ├── ml/                # ML inference engines (LiteRT-LM)
│       ├── navigation/        # Navigation graph
│       ├── network/           # API services
│       ├── quiz/              # Quiz UI components
│       ├── recommendation/    # AI recommendation engine
│       ├── repository/        # Data repositories (28)
│       ├── ui/                # Compose screens and components
│       ├── viewmodel/         # ViewModels
│       └── widget/            # Home screen widget
├── index.html                 # Admin Dashboard
├── PRIVACY_POLICY.md          # Privacy Policy
└── .github/workflows/         # CI/CD pipelines
```

---

## 🗄️ Database Schema

### PostgreSQL (Server)

| Table | Purpose |
|-------|---------|
| users | User accounts (guest + signed-in) |
| user_stats | User statistics and progress |
| coin_transactions | All coin earn/spend history |
| checkin_streaks | Daily check-in progress |
| meditation_sessions | Meditation logs |
| verse_notes | Saved verse notes |
| user_feedback | Feedback and complaints |

### Room (Local)

| Table | Purpose |
|-------|---------|
| user_stats | User statistics and progress |
| cached_verses | Offline verse cache |
| quiz_attempts | Quiz history |
| daily_activity | Daily learning activity |
| yoga_progression | Spiritual progression levels |
| voice_chat_messages | AI chat history |
| pending_sync_events | Offline action queue |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- Android SDK 24+

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Configuration

Create `local.properties` in the project root:
```properties
sdk.dir=/path/to/android/sdk
```

---

## 🧪 Testing

```bash
# Android unit tests
./gradlew testDebugUnitTest

# Android instrumented tests
./gradlew connectedDebugAndroidTest
```

---

## 📦 Version History

| Version | Code | Highlights |
|---------|------|------------|
| 2.12.2 | 43 | Unified guest/user flows, onboarding → login navigation |
| 2.12.1 | 42 | BQ history labels, voice pricing 4/6/10 |
| 2.11.0 | 31 | Aligned rewards, server-only history, yoga 1/2/2/3/3 |
| 2.10.0 | 30 | Turso TTL/history limits, server schema gate |
| 1.8.0 | 6 | Previous release |
| 1.7.x | 5 | Voice Studio improvements |
| 1.6.x | 4 | Quiz enhancements |

---

## 🔗 Links

- **Admin Dashboard**: [sravanku018.github.io/gitaAi](https://sravanku018.github.io/gitaAi)
- **Privacy Policy**: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- **GitHub**: [github.com/sravanku018/gitaAi](https://github.com/sravanku018/gitaAi)

---

## 👨‍💻 Author

**Ryalli Sravan Kumar**  
Android Developer · AI Integration Engineer  
📧 srathesweet@gmail.com  
📍 Warangal, Telangana, India  
🐙 [github.com/sravanku018](https://github.com/sravanku018)

---

## 🙏 Acknowledgments

- Bhagavad Gita API for verse data
- Google ML Kit for on-device translation
- TensorFlow Lite / LiteRT for ML inference
- NVIDIA NIM & Groq for cloud AI APIs

---

## 📄 License

Proprietary — All rights reserved © 2024 Ryalli Sravan Kumar

---

<p align="center">
  <i>Built with devotion. Guided by dharma. Powered by AI.</i><br/>
  <b>🕉️ Hare Krishna 🕉️</b>
</p>
