# Bhagavad Gita - AI-Powered Learning App

An intelligent Android application for learning the Bhagavad Gita with AI-powered features, voice interactions, and personalized learning paths.

## Features

### Learning Modes
- **Normal Mode**: Read and explore verses with translations and purports
- **Quiz Mode**: Test knowledge with MCQ, essay, comparison, and application questions
- **Voice Studio**: AI-powered Q&A with Krishna using on-device ML models
- **Random Sloka**: Discover daily inspiration from random verses
- **Flashcards**: Topic-based spaced repetition learning

### AI & ML Capabilities
- On-device inference with Qwen3 0.6B and Gemma 4 2B models
- Adaptive difficulty engine using Elo rating and Item Response Theory
- Google ML Kit for on-device translation
- Smart recommendations based on learning patterns

### Gamification
- Krishna Coins reward system
- Yoga progression levels with multipliers
- Daily check-in and streak tracking
- Achievement badges
- Leaderboard

### Offline Support
- Download all verses (~3-4 MB) for offline reading
- Background sync with WorkManager
- Pending event queue for offline actions

## Architecture

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

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose, Material3 |
| DI | Hilt |
| Database | Room (v38) |
| Networking | Retrofit, OkHttp |
| ML | TensorFlow Lite, LiteRT-LM |
| Navigation | Navigation Compose |
| Async | Coroutines, Flow |
| Work | WorkManager |
| Serialization | Gson, Moshi |

## Screenshots

<div align="center">
  <img src="screenshots/dashboard.png" width="200" alt="Dashboard"/>
  <img src="screenshots/quiz.png" width="200" alt="Quiz"/>
  <img src="screenshots/voice_studio.png" width="200" alt="Voice Studio"/>
  <img src="screenshots/profile.png" width="200" alt="Profile"/>
</div>

## Getting Started

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

## Project Structure

```
app/src/main/java/com/aipoweredgita/app/
├── coin/              # Coin reward engine
├── data/              # Data models (GitaVerse, QuizQuestion)
├── database/          # Room entities and DAOs (24 tables)
├── di/                # Hilt dependency injection
├── domain/            # Domain models and use cases
├── ml/                # ML inference engines
├── navigation/        # Navigation graph
├── network/           # API services
├── notifications/     # Workers and notification managers
├── quiz/              # Quiz UI components
├── recommendation/    # AI recommendation engine
├── repository/        # Data repositories (28)
├── services/          # Background workers
├── ui/                # Compose screens and components
├── utils/             # Utilities and preferences
├── viewmodel/         # ViewModels
└── widget/            # Home screen widget
```

## Database Schema

24 Room entities with 38 migration versions:

| Table | Purpose |
|-------|---------|
| user_stats | User statistics and progress |
| cached_verses | Offline verse cache |
| quiz_attempts | Quiz history |
| daily_activity | Daily learning activity |
| yoga_progression | Spiritual progression |
| voice_chat_messages | AI chat history |
| quiz_question_bank | Question repository |
| spaced_repetition_items | Flashcard scheduling |
| learning_patterns | Learning analytics |
| recommendations | AI recommendations |

## API Integration

### Gita API
- Base URL: Configured in `GitaConstants`
- Endpoints: Verse retrieval with multi-language support

### Coin API
- User management (create, auth, sync)
- Coin operations (award, spend, reconcile)
- Activity tracking (check-in, share, quiz)
- Leaderboard and yoga stages

## Testing

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests
./gradlew connectedDebugAndroidTest
```

Test stack: JUnit, MockK, Robolectric, OkHttp MockWebServer

## Build Scripts

- `BUILD_APK.sh` - Linux/macOS build script
- `BUILD_APK.bat` - Windows build script

## Version History

| Version | Code | Highlights |
|---------|------|------------|
| 2.11.0 | 31 | Current release — aligned rewards, server-only history, yoga 1/2/2/3/3 |
| 2.10.0 | 30 | Turso TTL/history limits, server schema gate |
| 1.8.0 | 6 | Earlier release |
| 1.7.x | 5 | Voice Studio improvements |
| 1.6.x | 4 | Quiz enhancements |
| 1.5.x | 3 | Offline mode |
| 1.0.x | 1 | Initial release |

## License

Proprietary - All rights reserved

## Acknowledgments

- Bhagavad Gita API for verse data
- Google ML Kit for on-device translation
- TensorFlow Lite for ML inference
