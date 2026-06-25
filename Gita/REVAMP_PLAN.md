# Gita App - Total Architecture Revamp Plan

## Overview

**Goal:** Migrate from mixed MVVM to clean MVVM + UDF architecture

**Timeline:** 2-3 weeks (incremental migration)

**Approach:** Phase-by-phase, screen-by-screen

---

## Phase 1: Setup Hilt DI Framework

### Files to Modify
- `Gita/app/build.gradle.kts` - Add Hilt dependencies
- `Gita/app/src/main/java/com/aipoweredgita/app/GitaApp.kt` - Add @HiltAndroidApp
- `Gita/app/src/main/java/com/aipoweredgita/app/MainActivity.kt` - Add @AndroidEntryPoint

### Files to Create
- `Gita/app/src/main/java/com/aipoweredgita/app/di/AppModule.kt`
- `Gita/app/src/main/java/com/aipoweredgita/app/di/DatabaseModule.kt`
- `Gita/app/src/main/java/com/aipoweredgita/app/di/RepositoryModule.kt`

### Tasks
1. Add Hilt plugin to build.gradle.kts
2. Add Hilt dependencies
3. Create @HiltAndroidApp application class
4. Create database module (provides DAOs)
5. Create repository module (provides repositories)
6. Test DI setup

---

## Phase 2: Create Domain Layer

### Files to Create
```
app/src/main/java/com/aipoweredgita/app/domain/
├── model/
│   ├── UiState.kt           # Base UI state
│   ├── ChatUiState.kt       # Chat screen state
│   ├── ProfileUiState.kt    # Profile screen state
│   ├── ChatEvent.kt         # Chat events
│   ├── ProfileEvent.kt      # Profile events
│   ├── ChatSideEffect.kt    # Chat one-time events
│   └── ProfileSideEffect.kt # Profile one-time events
└── usecase/
    ├── GetKrishnaResponseUseCase.kt
    ├── UpdateCoinBalanceUseCase.kt
    ├── LoadDashboardUseCase.kt
    └── GenerateRecommendationsUseCase.kt
```

### Tasks
1. Create base UiState interface
2. Create screen-specific UiState data classes
3. Create Event sealed classes
4. Create SideEffect sealed classes
5. Create UseCases for shared business logic

---

## Phase 3: Refactor ViewModels

### Files to Modify
```
app/src/main/java/com/aipoweredgita/app/viewmodel/
├── ProfileViewModel.kt      # AndroidViewModel → ViewModel
├── VoiceChatViewModel.kt    # AndroidViewModel → ViewModel
├── QuizViewModel.kt         # AndroidViewModel → ViewModel
├── NormalModeViewModel.kt   # AndroidViewModel → ViewModel
├── FavoritesViewModel.kt    # AndroidViewModel → ViewModel
└── ... (10 total)
```

### Migration Pattern
```kotlin
// BEFORE
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GitaDatabase.getDatabase(application)
    private val repo = StatsRepository(db.userStatsDao())
}

// AFTER
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val contentRepo: ContentRepository
) : ViewModel()
```

### Tasks
1. Add @HiltViewModel annotation
2. Add @Inject constructor
3. Remove AndroidViewModel extension
4. Remove manual initialization
5. Inject repositories via constructor
6. Test each ViewModel

---

## Phase 4: Consolidate State Flows

### Files to Modify
All ViewModels (10 files)

### Migration Pattern
```kotlin
// BEFORE - Multiple flows
private val _stats = MutableStateFlow<UserStats?>(null)
private val _badges = MutableStateFlow<List<UserBadge>>(emptyList())
private val _coins = MutableStateFlow(0)

// AFTER - Single state
data class ProfileUiState(
    val stats: UserStats? = null,
    val badges: List<UserBadge> = emptyList(),
    val coinBalance: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

private val _uiState = MutableStateFlow(ProfileUiState())
val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
```

### Tasks
1. Create UiState data class for each screen
2. Merge multiple flows into single state
3. Add isLoading and error to state
4. Update all state updates to use copy()
5. Update UI to collect single state

---

## Phase 5: Add Event System

### Files to Modify
All ViewModels + Screens

### Migration Pattern
```kotlin
// BEFORE - Direct callbacks
ChatScreen(
    onSendClick = { viewModel.sendMessage(it) },
    onDeleteClick = { viewModel.deleteMessage(it) }
)

// AFTER - Sealed events
sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class DeleteMessage(val id: String) : ChatEvent()
}

ChatScreen(
    onEvent = { viewModel.onEvent(it) }
)
```

### Tasks
1. Create Event sealed classes for each screen
2. Create SideEffect sealed classes for one-time events
3. Add onEvent() function to ViewModels
4. Add SharedFlow for side effects
5. Update UI to use onEvent
6. Add LaunchedEffect for side effects

---

## Phase 6: Migrate Navigation

### Files to Modify
- `Gita/app/src/main/java/com/aipoweredgita/app/navigation/NavGraph.kt`

### Migration Pattern
```kotlin
// BEFORE - String routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile/{userId}")
}

navController.navigate("profile/123")

// AFTER - Type-safe routes
@Serializable data object HomeKey
@Serializable data class ProfileKey(val userId: String)

navController.navigate(ProfileKey(userId = "123"))
```

### Tasks
1. Add kotlinx-serialization plugin
2. Create @Serializable route classes
3. Migrate NavHost to NavDisplay
4. Remove string-based routes
5. Test navigation

---

## Phase 7: Add Unit Tests

### Files to Create
```
app/src/test/java/com/aipoweredgita/app/
├── viewmodel/
│   ├── ProfileViewModelTest.kt
│   ├── ChatViewModelTest.kt
│   └── ...
└── usecase/
    ├── GetKrishnaResponseUseCaseTest.kt
    └── ...
```

### Tasks
1. Add test dependencies (JUnit, MockK, Turbine)
2. Create ViewModel tests
3. Create UseCase tests
4. Create Repository tests
5. Achieve 80%+ coverage

---

## Phase 8: Integration Testing

### Tasks
1. Test all screens
2. Test navigation flows
3. Test error handling
4. Test loading states
5. Performance testing

---

## Risk Mitigation

### Backup Strategy
1. Create git branch before each phase
2. Commit after each phase
3. Keep old code until new code is verified

### Rollback Plan
1. If phase fails, revert to previous commit
2. Keep AndroidViewModel versions as backup
3. Test thoroughly before removing old code

---

## Success Criteria

### Phase 1-2: DI Setup
- [ ] Hilt compiles successfully
- [ ] Application class has @HiltAndroidApp
- [ ] Database module provides all DAOs

### Phase 3-4: ViewModel Refactor
- [ ] All ViewModels use @HiltViewModel
- [ ] All ViewModels have single UiState
- [ ] No AndroidViewModel remaining

### Phase 5: Event System
- [ ] All screens use onEvent pattern
- [ ] One-time events use SharedFlow
- [ ] No state-based one-time events

### Phase 6: Navigation
- [ ] All routes are type-safe
- [ ] No string-based navigation
- [ ] All features work correctly

### Phase 7-8: Testing
- [ ] 80%+ unit test coverage
- [ ] All integration tests pass
- [ ] No regressions

---

## Timeline

| Week | Phase | Deliverable |
|------|-------|-------------|
| 1 | Phase 1-2 | Hilt setup + Domain layer |
| 2 | Phase 3-5 | ViewModel refactor + Events |
| 3 | Phase 6-8 | Navigation + Testing |

---

## Notes

- **Don't rush.** Incremental > Big Bang
- **Test after each phase.** Don't skip verification
- **Keep old code as backup.** Remove only after verification
- **Document decisions.** Future you will thank present you
