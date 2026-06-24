# 🕉️ gitaAi — AI-Powered Bhagavad Gita Learning App

> *Timeless wisdom of the Bhagavad Gita, powered by modern AI.*  
> Ask Krishna anything — about life, dharma, karma, or your struggles — and receive guidance grounded in the Gita's verses.

![Platform](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Language](https://img.shields.io/badge/Language-Kotlin-blue?logo=kotlin)
![AI](https://img.shields.io/badge/AI-NVIDIA%20NIM%20%7C%20Groq%20%7C%20LiteRT-orange)
![Version](https://img.shields.io/badge/Version-1.8.0-purple)
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

### AI & ML Capabilities
- On-device inference with **Qwen3 0.6B** and **Gemma 4 2B** via LiteRT-LM (no internet needed)
- Cloud AI via **NVIDIA Nemotron Super 120B** (primary) and **Groq Llama 3.3 70B** (fallback)
- Adaptive difficulty engine using **Elo rating + Item Response Theory**
- Google ML Kit for on-device translation
- Smart recommendations based on learning patterns
- Persistent AI memory — Krishna remembers your journey across sessions

### Gamification
- 🪙 Krishna Coins reward system
- 🧘 Yoga progression — 5 levels, 20 Sanskrit sub-stages
- 🔥 Daily check-in and streak tracking
- 🏆 Global leaderboard
- 🎖️ Achievement badges

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
│   ├── Room Database (24 entities, 38 versions)
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
| Database | Room (v38, 24 entities) |
| Networking | Retrofit, OkHttp |
| ML | TensorFlow Lite, LiteRT-LM |
| Navigation | Navigation Compose |
| Async | Coroutines, Flow |
| Work | WorkManager |
| Serialization | Gson, Moshi |

### Backend (Deno/Hono + Turso)

| Category | Technology |
|----------|------------|
| Runtime | Deno (TypeScript) |
| Framework | Hono |
| Database | Turso (libSQL / edge SQLite) |
| Deployment | Deno Deploy (zero-cost, global edge) |
| AI — Primary | NVIDIA NIM (Nemotron Super 120B, MoE) |
| AI — Fallback | Groq API (Llama 3.3 70B) |
| Tests | 142 Deno tests — all passing ✅ |

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

- **Prompt Injection Protection** — `GitaPromptEngine.kt` sanitizes user input to prevent persona hijacking
- **Emoji Stripping** — Backend strips emoji from AI responses for clean Telugu/Devanagari rendering
- **Timezone-Safe Streaks** — Daily check-in uses IST normalization to prevent midnight-rollover bugs
- **Hybrid Memory** — DataStore (local) + Turso cloud + Groq session summaries for persistent AI context
- **AI Proposes, Human Reviews** — All backend changes follow strict diff-review workflow with git rollback
- **142 Backend Tests** — Full test suite covering coins, streaks, sessions, sync, and auth logic

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
└── backend/                                   # Deno/Hono server
    ├── routes/                # API endpoints
    ├── db/                    # Turso queries
    ├── ai/                    # NVIDIA NIM + Groq handlers
    └── tests/                 # 142 test cases
```

---

## 🗄️ Database Schema

24 Room entities across 38 migration versions:

| Table | Purpose |
|-------|---------|
| user_stats | User statistics and progress |
| cached_verses | Offline verse cache |
| quiz_attempts | Quiz history |
| daily_activity | Daily learning activity |
| yoga_progression | Spiritual progression levels |
| voice_chat_messages | AI chat history |
| quiz_question_bank | Question repository |
| spaced_repetition_items | Flashcard scheduling |
| learning_patterns | Learning analytics |
| recommendations | AI recommendations |

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
BACKEND_URL=your_deno_deploy_url
```

---

## 🧪 Testing

```bash
# Android unit tests
./gradlew testDebugUnitTest

# Android instrumented tests
./gradlew connectedDebugAndroidTest

# Backend tests (Deno)
deno task test      # runs all 142 tests
```

Test stack: JUnit, MockK, Robolectric, OkHttp MockWebServer, Deno test runner

---

## 📦 Version History

| Version | Code | Highlights |
|---------|------|------------|
| 1.8.0 | 6 | Current release |
| 1.7.x | 5 | Voice Studio improvements |
| 1.6.x | 4 | Quiz enhancements |
| 1.5.x | 3 | Offline mode |
| 1.0.x | 1 | Initial release |

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
</p><img width="573" height="1280" alt="photo_2026-06-24_14-08-48" src="https://github.com/user-attachments/assets/f7af6c0a-d246-4bda-8680-f41edeefd72d" />
<img width="573" height="1280" alt="photo_2026-06-24_14-08-46" src="https://github.com/user-attachments/assets/f6ecb722-5b21-4d2d-b041-a666a8b35c2b" />
<img width="573" height="1280" alt="photo_2026-06-24_14-08-44" src="https://github.com/user-attachments/assets/01e66c35-5900-470f-a13e-092ef0436651" />
<img width="573" height="1280" alt="photo_2026-06-24_14-08-42" src="https://github.com/user-attachments/assets/2b816079-1fde-40ab-b86d-2676c696976a" />
<img width="573" height="1280" alt="photo_2026-06-24_14-08-39" src="https://github.com/user-attachments/assets/c8168d19-ed2c-4ae5-b62b-596fba131ad7" />
<img width="573" height="1280" alt="photo_2026-06-24_14-08-35" src="https://github.com/user-attachments/assets/98d4ee15-82ed-4421-a6e0-ea4dbc302c37" />
