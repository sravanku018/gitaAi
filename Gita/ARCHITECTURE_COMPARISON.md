# Gita App Architecture: Before vs After

## Overview

| Aspect | Current (Before) | Target (After UDF) |
|--------|------------------|-------------------|
| **Pattern** | MVVM (mixed) | MVVM + UDF |
| **State** | Multiple flows | Single UI state |
| **Events** | Direct callbacks | Sealed events |
| **Navigation** | Nav2 (strings) | Nav2 → Nav3 |
| **DI** | Manual | Hilt |
| **Testing** | Hard | Easy |

---

## 1. Navigation

### Before (Current - Nav2)
```kotlin
// String-based routes - error prone
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Chat : Screen("chat/{userId}")  // String template
}

// Navigation with strings
navController.navigate("profile/${userId}")
navController.navigate("chat/123")

// NavHost with string routes
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen() }
    composable("profile/{userId}") { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId")
        ProfileScreen(userId = userId)
    }
}
```

### After (UDF + Nav3)
```kotlin
// Type-safe route definitions
@Serializable data object HomeKey
@Serializable data class ProfileKey(val userId: String)
@Serializable data class ChatKey(val chatId: String)

// Navigation with types
navController.navigate(ProfileKey(userId = "123"))
navController.navigate(ChatKey(chatId = "456"))

// NavDisplay with type safety
val backStack = remember { mutableStateListOf<Any>(HomeKey) }

NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() }
) {
    when (it) {
        is HomeKey -> HomeScreen()
        is ProfileKey -> ProfileScreen(userId = it.userId)
        is ChatKey -> ChatScreen(chatId = it.chatId)
    }
}
```

### Impact
| Metric | Before | After |
|--------|--------|-------|
| Type safety | ❌ String errors at runtime | ✅ Compile-time errors |
| Refactoring | ❌ Find/replace strings | ✅ IDE rename works |
| Arguments | ❌ Manual extraction | ✅ Automatic |

---

## 2. ViewModel State Management

### Before (Current - Multiple Flows)
```kotlin
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    // 7 separate state flows - scattered
    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats.asStateFlow()

    private val _userBadges = MutableStateFlow<List<UserBadge>>(emptyList())
    val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    private val _userLevel = MutableStateFlow<UserLevel?>(null)
    val userLevel: StateFlow<UserLevel?> = _userLevel.asStateFlow()

    private val _dailyActivity = MutableStateFlow(DailyActivityData())
    val dailyActivity: StateFlow<DailyActivityData> = _dailyActivity.asStateFlow()

    private val _nextAction = MutableStateFlow(NextActionData())
    val nextAction: StateFlow<NextActionData> = _nextAction.asStateFlow()

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val _recommendations = MutableStateFlow<List<RecommendationData>>(emptyList())
    val recommendations: StateFlow<List<RecommendationData>> = _recommendations.asStateFlow()
}

// UI collects each flow separately
val stats by viewModel.stats.collectAsState()
val badges by viewModel.userBadges.collectAsState()
val level by viewModel.userLevel.collectAsState()
val daily by viewModel.dailyActivity.collectAsState()
val next by viewModel.nextAction.collectAsState()
val coins by viewModel.coinBalance.collectAsState()
val recs by viewModel.recommendations.collectAsState()
```

### After (UDF - Single State)
```kotlin
// Single UI state - single source of truth
data class ProfileUiState(
    val stats: UserStats? = null,
    val badges: List<UserBadge> = emptyList(),
    val level: UserLevel? = null,
    val dailyActivity: DailyActivityData = DailyActivityData(),
    val nextAction: NextActionData = NextActionData(),
    val coinBalance: Int = 0,
    val recommendations: List<RecommendationData> = emptyList(),
    val isLoading: Boolean = false
)

class ProfileViewModel(
    private val statsRepository: StatsRepository,
    private val contentRepo: ContentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadDashboard -> loadDashboard()
            is ProfileEvent.RefreshCoins -> refreshCoins()
            is ProfileEvent.UpdateProfile -> updateProfile(event.name, event.dob)
        }
    }
}

// UI collects single state
val uiState by viewModel.uiState.collectAsState()
// Access: uiState.stats, uiState.badges, uiState.coinBalance, etc.
```

### Impact
| Metric | Before | After |
|--------|--------|-------|
| State sources | ❌ 7 flows | ✅ 1 flow |
| Partial updates | ❌ Manual sync | ✅ Automatic |
| Loading state | ❌ Separate flow | ✅ Included |
| Code lines | ❌ ~50 lines | ✅ ~10 lines |

---

## 3. UI Events

### Before (Current - Direct Callbacks)
```kotlin
// Multiple callbacks - scattered
ChatScreen(
    onSendClick = { viewModel.sendMessage(it) },
    onDeleteClick = { viewModel.deleteMessage(it) },
    onRetryClick = { viewModel.retry() },
    onVoiceClick = { viewModel.startVoice() },
    onClearChat = { viewModel.clearChat() }
)

// In ViewModel - multiple functions
fun sendMessage(text: String) { ... }
fun deleteMessage(id: String) { ... }
fun retry() { ... }
fun startVoice() { ... }
fun clearChat() { ... }
```

### After (UDF - Sealed Events)
```kotlin
// Single event handler
ChatScreen(
    onEvent = { viewModel.onEvent(it) }
)

// Sealed class - all events in one place
sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class DeleteMessage(val id: String) : ChatEvent()
    object Retry : ChatEvent()
    object StartVoice : ChatEvent()
    object ClearChat : ChatEvent()
}

// ViewModel - single function
fun onEvent(event: ChatEvent) {
    when (event) {
        is ChatEvent.SendMessage -> sendMessage(event.text)
        is ChatEvent.DeleteMessage -> deleteMessage(event.id)
        is ChatEvent.Retry -> retry()
        is ChatEvent.StartVoice -> startVoice()
        is ChatEvent.ClearChat -> clearChat()
    }
}
```

### Impact
| Metric | Before | After |
|--------|--------|-------|
| Callbacks | ❌ 5+ parameters | ✅ 1 parameter |
| New events | ❌ Add parameter + handler | ✅ Add to sealed class |
| Compose stability | ❌ Lambda recreation | ✅ Stable reference |

---

## 4. One-Time Events

### Before (Current - State-based)
```kotlin
// ❌ Bug: Toast shows again on recomposition
_uiState.update { it.copy(showToast = true) }

// Or use flag (messy)
private var toastShown = false
fun showToast() {
    if (!toastShown) {
        toastShown = true
        // show toast
    }
}
```

### After (UDF - SharedFlow)
```kotlin
// Side effects - one-time events
sealed class ChatSideEffect {
    data class ShowToast(val message: String) : ChatSideEffect()
    object NavigateToQuiz : ChatSideEffect()
    object NavigateToLogin : ChatSideEffect()
}

private val _sideEffect = MutableSharedFlow<ChatSideEffect>()
val sideEffect: SharedFlow<ChatSideEffect> = _sideEffect.asSharedFlow()

// Emit
viewModelScope.launch {
    _sideEffect.emit(ChatSideEffect.ShowToast("Coins earned!"))
}

// Collect in UI
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is ChatSideEffect.ShowToast -> showToast(effect.message)
            is ChatSideEffect.NavigateToQuiz -> navigateToQuiz()
            is ChatSideEffect.NavigateToLogin -> navigateToLogin()
        }
    }
}
```

### Impact
| Metric | Before | After |
|--------|--------|-------|
| Toast re-show | ❌ Bug-prone | ✅ Never |
| Navigation events | ❌ State flags | ✅ Clean |
| Testability | ❌ Hard | ✅ Easy |

---

## 5. Dependency Injection

### Before (Current - Manual)
```kotlin
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    // Manual initialization - hard to test
    private val db = GitaDatabase.getDatabase(application)
    private val userStatsDao = db.userStatsDao()
    private val contentRepo = ContentRepository(db.recommendationDataDao())
    private val readingRepo = ReadingRepository(db.readVerseDao(), db.cachedVerseDao())
    private val dailyActivityRepo = DailyActivityRepository(db.dailyActivityDao())
    private val quizStatsRepo = QuizStatsRepository(db.quizAttemptDao())
    private val statsRepository = StatsRepository(
        userStatsDao = db.userStatsDao(),
        dailyActivityDao = db.dailyActivityDao(),
        appContext = getApplication()
    )
}
```

### After (UDF - Hilt)
```kotlin
// Repository - injected
class StatsRepository @Inject constructor(
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao,
    @ApplicationContext private val context: Context
)

// ViewModel - injected
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val contentRepo: ContentRepository,
    private val readingRepo: ReadingRepository
) : ViewModel()

// Module - provides dependencies
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GitaDatabase {
        return GitaDatabase.getDatabase(context)
    }
}
```

### Impact
| Metric | Before | After |
|--------|--------|-------|
| Setup code | ❌ ~20 lines | ✅ 0 lines |
| Testing | ❌ Can't mock | ✅ Easy mock |
| Swapping impl | ❌ Change code | ✅ Change module |

---

## 6. Testing

### Before (Current - Hard)
```kotlin
// ❌ Can't test - depends on Application
class ProfileViewModelTest {
    @Test
    fun testLoadDashboard() {
        // Can't create ViewModel without Application
        // Can't mock database
        // Can't verify behavior
    }
}
```

### After (UDF - Easy)
```kotlin
// ✅ Easy to test
class ProfileViewModelTest {
    private val mockStatsRepo = mockk<StatsRepository>()
    private val mockContentRepo = mockk<ContentRepository>()
    
    @Test
    fun `should update state when dashboard loads`() = runTest {
        // Given
        every { mockStatsRepo.getBalance() } returns 100
        coEvery { mockStatsRepo.getUserStats() } returns flowOf(testStats)
        
        val viewModel = ProfileViewModel(mockStatsRepo, mockContentRepo)
        
        // When
        viewModel.onEvent(ProfileEvent.LoadDashboard)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(100, state.coinBalance)
        assertEquals(testStats, state.stats)
    }
    
    @Test
    fun `should emit error when network fails`() = runTest {
        // Given
        coEvery { mockStatsRepo.getBalance() } throws NetworkException()
        
        val viewModel = ProfileViewModel(mockStatsRepo, mockContentRepo)
        
        // When
        viewModel.onEvent(ProfileEvent.RefreshCoins)
        
        // Then
        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ProfileSideEffect.ShowError)
    }
}
```

### Impact
| Metric | Before | After |
|--------|--------|-------|
| Test setup | ❌ Impossible | ✅ Simple |
| Mocking | ❌ Can't mock DB | ✅ Mock interfaces |
| Coverage | ❌ Low | ✅ High |

---

## 7. File Structure

### Before (Current)
```
app/src/main/java/com/aipoweredgita/app/
├── viewmodel/           # 10 ViewModels
│   ├── ProfileViewModel.kt
│   ├── ChatViewModel.kt
│   └── ...
├── repository/          # 27 Repositories
│   ├── ChatRepository.kt
│   ├── StatsRepository.kt
│   └── ...
├── ui/                  # Screens
│   ├── ChatScreen.kt
│   ├── ProfileScreen.kt
│   └── ...
├── navigation/
│   └── NavGraph.kt      # String routes
└── database/
    └── GitaDatabase.kt
```

### After (UDF)
```
app/src/main/java/com/aipoweredgita/app/
├── domain/              # NEW: Business logic
│   ├── model/
│   │   ├── ChatUiState.kt
│   │   ├── ProfileUiState.kt
│   │   └── ChatEvent.kt
│   └── usecase/
│       ├── GetKrishnaResponseUseCase.kt
│       └── UpdateCoinBalanceUseCase.kt
├── data/                # Implementation
│   ├── remote/
│   │   └── GroqApiService.kt
│   ├── local/
│   │   └── GitaDatabase.kt
│   └── repository/
│       ├── ChatRepositoryImpl.kt
│       └── StatsRepositoryImpl.kt
├── ui/                  # Presentation
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   └── ChatViewModel.kt
│   └── profile/
│       ├── ProfileScreen.kt
│       └── ProfileViewModel.kt
├── di/                  # NEW: DI
│   ├── AppModule.kt
│   └── RepositoryModule.kt
└── navigation/
    └── NavGraph.kt      # Type-safe routes
```

---

## 8. Migration Checklist

### Phase 1: ViewModel Cleanup (Week 1)
- [ ] Add Hilt to project
- [ ] Convert `AndroidViewModel` → `ViewModel`
- [ ] Inject repositories via constructor
- [ ] Create `UiState` data classes

### Phase 2: State Consolidation (Week 2)
- [ ] Merge multiple `StateFlow`s into single state
- [ ] Add `isLoading` and `error` to state
- [ ] Update UI to collect single state

### Phase 3: Event System (Week 3)
- [ ] Create `Event` sealed classes
- [ ] Create `SideEffect` sealed classes
- [ ] Add `onEvent()` function to ViewModels
- [ ] Update UI to use `onEvent`

### Phase 4: Navigation (Week 4)
- [ ] Add type-safe route definitions
- [ ] Migrate from `NavHost` to `NavDisplay`
- [ ] Remove string-based routes

### Phase 5: Testing (Week 5)
- [ ] Add unit tests for ViewModels
- [ ] Add integration tests for UseCases
- [ ] Verify all features work

---

## 9. Benefits Summary

### For Users
- Same experience
- Fewer bugs
- Faster feature releases

### For Developers
- Cleaner code
- Easier debugging
- Faster development
- Better testing
- Easier onboarding

### For Business
- Lower maintenance cost
- Faster time-to-market
- Higher code quality
- Better team productivity

---

## 10. Conclusion

**UDF is not a rewrite. It's an evolution.**

Your current app works. UDF makes it:
- Easier to maintain
- Easier to test
- Easier to extend

**Start small. Pick one screen. Migrate. Learn. Repeat.**

The user sees nothing. The developer sees everything.
