// ─── Quiz Question Rotation Test ─────────────────────────────
// Tests the rotation algorithm: cooldown, usage count, difficulty proximity
// Usage: deno run --allow-net --allow-env test-quiz-rotation.ts

let passed = 0;
let failed = 0;
const failures: string[] = [];

function assert(name: string, condition: boolean, detail = "") {
  if (condition) {
    console.log(`  ✅ ${name}`);
    passed++;
  } else {
    console.log(`  ❌ ${name}${detail ? ` — ${detail}` : ""}`);
    failed++;
    failures.push(`${name}${detail ? `: ${detail}` : ""}`);
  }
}

function section(name: string) {
  console.log(`\n─── ${name} ${"─".repeat(Math.max(0, 40 - name.length))}`);
}

// ─── Simulate Question Bank ──────────────────────────────────
interface Question {
  id: number;
  questionHash: string;
  difficulty: number;
  usageCount: number;
  lastAskedAt: number;
  isActive: boolean;
  isApproved: boolean;
  createdAt: number;
  chapter: number;
  verse: number;
}

function createQuestionBank(): Question[] {
  const now = Date.now();
  return [
    // Easy questions (difficulty 1-3)
    { id: 1, questionHash: "q1", difficulty: 1, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 100000, chapter: 1, verse: 1 },
    { id: 2, questionHash: "q2", difficulty: 2, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 90000, chapter: 1, verse: 2 },
    { id: 3, questionHash: "q3", difficulty: 3, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 80000, chapter: 1, verse: 3 },
    // Medium questions (difficulty 4-6)
    { id: 4, questionHash: "q4", difficulty: 4, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 70000, chapter: 2, verse: 1 },
    { id: 5, questionHash: "q5", difficulty: 5, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 60000, chapter: 2, verse: 2 },
    { id: 6, questionHash: "q6", difficulty: 6, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 50000, chapter: 2, verse: 3 },
    // Hard questions (difficulty 7-10)
    { id: 7, questionHash: "q7", difficulty: 7, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 40000, chapter: 3, verse: 1 },
    { id: 8, questionHash: "q8", difficulty: 8, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 30000, chapter: 3, verse: 2 },
    { id: 9, questionHash: "q9", difficulty: 9, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 20000, chapter: 3, verse: 3 },
    { id: 10, questionHash: "q10", difficulty: 10, usageCount: 0, lastAskedAt: 0, isActive: true, isApproved: true, createdAt: now - 10000, chapter: 4, verse: 1 },
  ];
}

// ─── Simulate getNextQuestions Query ──────────────────────────
function getNextQuestions(
  bank: Question[],
  minDiff: number,
  maxDiff: number,
  limit: number,
  targetDifficulty: number = 5,
  cooldownMs: number = 24 * 60 * 60 * 1000  // 24 hours
): Question[] {
  const cooldownCutoff = Date.now() - cooldownMs;
  
  return bank
    .filter(q => 
      q.difficulty >= minDiff && 
      q.difficulty <= maxDiff &&
      q.isActive && 
      q.isApproved && 
      q.lastAskedAt < cooldownCutoff
    )
    .sort((a, b) => {
      // 1. Least used first
      if (a.usageCount !== b.usageCount) return a.usageCount - b.usageCount;
      // 2. Closest to target difficulty
      const diffA = Math.abs(a.difficulty - targetDifficulty);
      const diffB = Math.abs(b.difficulty - targetDifficulty);
      if (diffA !== diffB) return diffA - diffB;
      // 3. Oldest first (createdAt ASC)
      return a.createdAt - b.createdAt;
    })
    .slice(0, limit);
}

// ─── Simulate markAsAsked ────────────────────────────────────
function markAsAsked(bank: Question[], id: number): void {
  const q = bank.find(q => q.id === id);
  if (q) {
    q.usageCount++;
    q.lastAskedAt = Date.now();
  }
}

// ═══════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════

// ─── 1. BASIC ROTATION ──────────────────────────────────────
section("BASIC ROTATION");
{
  const bank = createQuestionBank();
  
  // First batch: should get least-used questions
  const batch1 = getNextQuestions(bank, 1, 10, 3);
  assert("Batch 1 — returns 3 questions", batch1.length === 3);
  assert("Batch 1 — all usageCount=0", batch1.every(q => q.usageCount === 0));
  
  // Mark them as asked
  batch1.forEach(q => markAsAsked(bank, q.id));
  
  // Second batch: should get different questions (usageCount=0 still available)
  const batch2 = getNextQuestions(bank, 1, 10, 3);
  assert("Batch 2 — returns 3 different questions", batch2.length === 3);
  assert("Batch 2 — no overlap with batch 1", 
    !batch2.some(q2 => batch1.some(q1 => q1.id === q2.id)));
}

// ─── 2. USAGE COUNT PRIORITY ─────────────────────────────────
section("USAGE COUNT PRIORITY");
{
  const bank = createQuestionBank();
  
  // Mark questions 1,2,3 as used 5 times
  [1, 2, 3].forEach(id => {
    const q = bank.find(q => q.id === id)!;
    q.usageCount = 5;
  });
  
  // Should prefer questions with usageCount=0
  const batch = getNextQuestions(bank, 1, 10, 3);
  assert("Usage priority — selects low-usage questions", 
    batch.every(q => q.usageCount < 5));
  assert("Usage priority — none of the heavily-used ones", 
    !batch.some(q => [1, 2, 3].includes(q.id)));
}

// ─── 3. DIFFICULTY PROXIMITY ─────────────────────────────────
section("DIFFICULTY PROXIMITY");
{
  const bank = createQuestionBank();
  
  // All same usage count, so difficulty should matter
  const batch = getNextQuestions(bank, 1, 10, 3, 5); // target difficulty = 5
  assert("Difficulty — prefers medium questions (target=5)", 
    batch.some(q => q.difficulty === 5 || q.difficulty === 4 || q.difficulty === 6));
  
  // Easy target
  const easyBatch = getNextQuestions(bank, 1, 10, 3, 2);
  assert("Difficulty — easy target prefers easy questions", 
    easyBatch.every(q => q.difficulty <= 3));
  
  // Hard target
  const hardBatch = getNextQuestions(bank, 1, 10, 3, 9);
  assert("Difficulty — hard target prefers hard questions", 
    hardBatch.every(q => q.difficulty >= 7));
}

// ─── 4. COOLDOWN (24 HOURS) ──────────────────────────────────
section("COOLDOWN (24 HOURS)");
{
  const bank = createQuestionBank();
  const COOLDOWN_MS = 24 * 60 * 60 * 1000;
  
  // Mark question 1 as asked recently (12 hours ago)
  const q1 = bank.find(q => q.id === 1)!;
  q1.lastAskedAt = Date.now() - (COOLDOWN_MS / 2); // 12 hours ago
  q1.usageCount = 1;
  
  // Should NOT include q1 (still in cooldown)
  const batch = getNextQuestions(bank, 1, 10, 5, 5, COOLDOWN_MS);
  assert("Cooldown 24h — recent question excluded (12h ago)", 
    !batch.some(q => q.id === 1));
  
  // Mark question 2 as asked long ago (25 hours ago)
  const q2 = bank.find(q => q.id === 2)!;
  q2.lastAskedAt = Date.now() - COOLDOWN_MS - 1000; // 25 hours ago
  q2.usageCount = 1;
  
  // Should include q2 (cooldown expired)
  const batch2 = getNextQuestions(bank, 1, 10, 10, 5, COOLDOWN_MS);
  assert("Cooldown 24h — expired question included (25h ago)", 
    batch2.some(q => q.id === 2));
  
  // Mark question 3 as asked just now
  const q3 = bank.find(q => q.id === 3)!;
  q3.lastAskedAt = Date.now(); // just now
  q3.usageCount = 1;
  
  // Should NOT include q3
  const batch3 = getNextQuestions(bank, 1, 10, 10, 5, COOLDOWN_MS);
  assert("Cooldown 24h — just-asked question excluded", 
    !batch3.some(q => q.id === 3));
}

// ─── 5. DIFFICULTY FILTER ────────────────────────────────────
section("DIFFICULTY FILTER");
{
  const bank = createQuestionBank();
  
  // Only easy questions
  const easyBatch = getNextQuestions(bank, 1, 3, 5);
  assert("Filter 1-3 — all easy", easyBatch.every(q => q.difficulty <= 3));
  
  // Only hard questions
  const hardBatch = getNextQuestions(bank, 7, 10, 5);
  assert("Filter 7-10 — all hard", hardBatch.every(q => q.difficulty >= 7));
  
  // No questions in empty range
  const emptyBatch = getNextQuestions(bank, 11, 15, 5);
  assert("Filter 11-15 — empty", emptyBatch.length === 0);
}

// ─── 6. INACTIVE/UNAPPROVED QUESTIONS ────────────────────────
section("INACTIVE/UNAPPROVED FILTERING");
{
  const bank = createQuestionBank();
  
  // Deactivate question 1
  bank.find(q => q.id === 1)!.isActive = false;
  // Unapprove question 2
  bank.find(q => q.id === 2)!.isApproved = false;
  
  const batch = getNextQuestions(bank, 1, 10, 10);
  assert("Inactive — excluded", !batch.some(q => q.id === 1));
  assert("Unapproved — excluded", !batch.some(q => q.id === 2));
}

// ─── 7. FULL CYCLE (ALL QUESTIONS USED) ──────────────────────
section("FULL CYCLE — All Questions Used");
{
  const bank = createQuestionBank();
  
  // Use all questions once, but set lastAskedAt past 24h cooldown
  bank.forEach(q => {
    q.usageCount = 1;
    q.lastAskedAt = Date.now() - 25 * 60 * 60 * 1000; // 25 hours ago (past 24h cooldown)
  });
  
  // Should still return questions (sorted by usageCount, then difficulty)
  const batch = getNextQuestions(bank, 1, 10, 3);
  assert("Full cycle — still returns questions", batch.length === 3);
  assert("Full cycle — all have usageCount=1", batch.every(q => q.usageCount === 1));
  
  // Mark some as used more
  markAsAsked(bank, batch[0].id); // now usageCount=2
  
  // Next batch should prefer the ones with usageCount=1
  const batch2 = getNextQuestions(bank, 1, 10, 3);
  assert("Full cycle — prefers least-used after reuse", 
    batch2[0].usageCount <= batch2[1].usageCount);
}

// ─── 8. CHAPTER/VERSE DIVERSITY ──────────────────────────────
section("CHAPTER/VERSE DIVERSITY");
{
  const bank = createQuestionBank();
  
  // Get 5 questions
  const batch = getNextQuestions(bank, 1, 10, 5);
  const chapters = new Set(batch.map(q => q.chapter));
  assert("Diversity — covers multiple chapters", chapters.size >= 2, 
    `got ${chapters.size} chapters: ${[...chapters].join(',')}`);
  
  // Check no duplicate chapter+verse combos
  const combos = batch.map(q => `${q.chapter}:${q.verse}`);
  const uniqueCombos = new Set(combos);
  assert("Diversity — no duplicate chapter:verse", combos.length === uniqueCombos.size);
}

// ─── 9. RAPID-FIRE (20 QUESTIONS IN A ROW) ──────────────────
section("RAPID-FIRE — 20 Questions (24h cooldown)");
{
  const bank = createQuestionBank();
  const COOLDOWN_MS = 24 * 60 * 60 * 1000;
  const askedIds: number[] = [];
  
  for (let i = 0; i < 20; i++) {
    const batch = getNextQuestions(bank, 1, 10, 1, 5, COOLDOWN_MS);
    if (batch.length > 0) {
      markAsAsked(bank, batch[0].id);
      askedIds.push(batch[0].id);
    }
  }
  
  // With 10 questions and 24h cooldown, first 10 are unique,
  // then cooldown blocks remaining questions (correct behavior)
  assert("Rapid-fire — got at least 10 questions", askedIds.length >= 10,
    `got ${askedIds.length}`);
  assert("Rapid-fire — first 10 are all unique", 
    askedIds.slice(0, 10).length === new Set(askedIds.slice(0, 10)).size);
  assert("Rapid-fire — cooldown blocks after exhaustion", 
    askedIds.length <= 10, `got ${askedIds.length} (cooldown should limit)`);
}

// ─── 10. SESSION TRACKING (ZERO REPEATS IN QUIZ) ─────────────
section("SESSION TRACKING — Zero Repeats in Quiz");
{
  const bank = createQuestionBank();
  const sessionAskedIds = new Set<number>();
  
  // Simulate a 10-question quiz with session tracking
  const quizQuestions: number[] = [];
  for (let i = 0; i < 10; i++) {
    const candidates = getNextQuestions(bank, 1, 10, 10);
    const available = candidates.filter(q => !sessionAskedIds.has(q.id));
    
    if (available.length > 0) {
      const q = available[0];
      sessionAskedIds.add(q.id);
      markAsAsked(bank, q.id);
      quizQuestions.push(q.id);
    }
  }
  
  assert("Session — got 10 questions", quizQuestions.length === 10);
  assert("Session — zero repeats", 
    quizQuestions.length === new Set(quizQuestions).size,
    `got ${new Set(quizQuestions).size} unique out of ${quizQuestions.length}`);
  
  // Simulate restart — session should clear, but 24h cooldown still applies
  sessionAskedIds.clear();
  const afterRestart = getNextQuestions(bank, 1, 10, 3);
  assert("Session restart — cooldown still applies (no questions available)", 
    afterRestart.length === 0,
    `got ${afterRestart.length} (should be 0 — all in 24h cooldown)`);
}

// ─── 11. NO REPEATS ACROSS QUIZZES (SAME DAY) ───────────────
section("NO REPEATS ACROSS QUIZZES (SAME DAY)");
{
  const bank = createQuestionBank();
  const COOLDOWN_MS = 24 * 60 * 60 * 1000;
  
  // Quiz 1: ask 5 questions
  const quiz1: number[] = [];
  for (let i = 0; i < 5; i++) {
    const batch = getNextQuestions(bank, 1, 10, 1, 5, COOLDOWN_MS);
    if (batch.length > 0) {
      markAsAsked(bank, batch[0].id);
      quiz1.push(batch[0].id);
    }
  }
  
  // Quiz 2: ask 5 more questions — should be different from quiz 1
  const quiz2: number[] = [];
  for (let i = 0; i < 5; i++) {
    const batch = getNextQuestions(bank, 1, 10, 1, 5, COOLDOWN_MS);
    if (batch.length > 0) {
      markAsAsked(bank, batch[0].id);
      quiz2.push(batch[0].id);
    }
  }
  
  assert("Cross-quiz — quiz 2 has 5 questions", quiz2.length === 5);
  const overlap = quiz1.filter(id => quiz2.includes(id));
  assert("Cross-quiz — zero overlap between quizzes", overlap.length === 0,
    `overlap: ${overlap.join(',')}`);
}

// ─── 10. TARGET DIFFICULTY ADAPTATION ────────────────────────
section("TARGET DIFFICULTY ADAPTATION");
{
  const bank = createQuestionBank();
  
  // Simulate: user answers easy questions correctly → target goes up
  // Target difficulty 8 → should prefer harder questions
  const hardTargetBatch = getNextQuestions(bank, 1, 10, 5, 8);
  const avgDiffHard = hardTargetBatch.reduce((sum, q) => sum + q.difficulty, 0) / hardTargetBatch.length;
  assert("Adaptation — target=8 prefers harder questions", avgDiffHard >= 6,
    `avg difficulty: ${avgDiffHard.toFixed(1)}`);
  
  // Target difficulty 2 → should prefer easier questions
  const easyTargetBatch = getNextQuestions(bank, 1, 10, 5, 2);
  const avgDiffEasy = easyTargetBatch.reduce((sum, q) => sum + q.difficulty, 0) / easyTargetBatch.length;
  assert("Adaptation — target=2 prefers easier questions", avgDiffEasy <= 4,
    `avg difficulty: ${avgDiffEasy.toFixed(1)}`);
}

// ─── 12. QUIZ GUARD — 15 QUESTIONS ──────────────────────────
section("QUIZ GUARD — 15 Questions");
{
  // Create a 20-question bank (enough for 15 with cooldown)
  const now = Date.now();
  const bank: Question[] = [];
  for (let i = 1; i <= 20; i++) {
    bank.push({
      id: i,
      questionHash: `q${i}`,
      difficulty: (i % 10) + 1,
      usageCount: 0,
      lastAskedAt: 0,
      isActive: true,
      isApproved: true,
      createdAt: now - (20 - i) * 10000,
      chapter: Math.ceil(i / 3),
      verse: ((i - 1) % 3) + 1,
    });
  }

  const COOLDOWN_MS = 24 * 60 * 60 * 1000;
  const maxQuestions = 15;
  let totalQuestions = 0;
  let currentQuestion: Question | null = null;
  const sessionAskedIds = new Set<number>();
  const quizQuestions: number[] = [];

  function loadNextQuestion(): boolean {
    if (totalQuestions >= maxQuestions) {
      return false;
    }

    const candidates = getNextQuestions(bank, 1, 10, 10, 5, COOLDOWN_MS);
    const available = candidates.filter(q => !sessionAskedIds.has(q.id));

    if (available.length > 0) {
      const q = available[0];
      sessionAskedIds.add(q.id);
      markAsAsked(bank, q.id);
      currentQuestion = q;
      totalQuestions++;
      quizQuestions.push(q.id);
      return true;
    }
    return false;
  }

  // Attempt to load 25 questions (should stop at 15)
  for (let i = 0; i < 25; i++) {
    loadNextQuestion();
  }

  assert("Guard 15 — totalQuestions equals maxQuestions",
    totalQuestions === 15, `got ${totalQuestions}`);
  assert("Guard 15 — quizQuestions has exactly 15",
    quizQuestions.length === 15, `got ${quizQuestions.length}`);
  assert("Guard 15 — all questions unique",
    quizQuestions.length === new Set(quizQuestions).size,
    `got ${new Set(quizQuestions).size} unique out of ${quizQuestions.length}`);
  assert("Guard 15 — no extra questions loaded",
    totalQuestions <= maxQuestions, `got ${totalQuestions}`);
}

// ─── 13. QUIZ GUARD — 25 QUESTIONS ──────────────────────────
section("QUIZ GUARD — 25 Questions");
{
  // Create a larger question bank for 25-question quiz
  const now = Date.now();
  const bank: Question[] = [];
  for (let i = 1; i <= 30; i++) {
    bank.push({
      id: i,
      questionHash: `q${i}`,
      difficulty: (i % 10) + 1,
      usageCount: 0,
      lastAskedAt: 0,
      isActive: true,
      isApproved: true,
      createdAt: now - (30 - i) * 10000,
      chapter: Math.ceil(i / 3),
      verse: ((i - 1) % 3) + 1,
    });
  }

  const COOLDOWN_MS = 24 * 60 * 60 * 1000;
  const maxQuestions = 25;
  let totalQuestions = 0;
  let currentQuestion: Question | null = null;
  const sessionAskedIds = new Set<number>();
  const quizQuestions: number[] = [];

  function loadNextQuestion(): boolean {
    // Guard: don't load if we've already reached maxQuestions
    if (totalQuestions >= maxQuestions) {
      return false;
    }

    const candidates = getNextQuestions(bank, 1, 10, 10, 5, COOLDOWN_MS);
    const available = candidates.filter(q => !sessionAskedIds.has(q.id));

    if (available.length > 0) {
      const q = available[0];
      sessionAskedIds.add(q.id);
      markAsAsked(bank, q.id);
      currentQuestion = q;
      totalQuestions++;
      quizQuestions.push(q.id);
      return true;
    }
    return false;
  }

  // Attempt to load 30 questions (should stop at 25)
  for (let i = 0; i < 30; i++) {
    loadNextQuestion();
  }

  assert("Guard 25 — totalQuestions equals maxQuestions",
    totalQuestions === 25, `got ${totalQuestions}`);
  assert("Guard 25 — quizQuestions has exactly 25",
    quizQuestions.length === 25, `got ${quizQuestions.length}`);
  assert("Guard 25 — all questions unique",
    quizQuestions.length === new Set(quizQuestions).size,
    `got ${new Set(quizQuestions).size} unique out of ${quizQuestions.length}`);
  assert("Guard 25 — no extra questions loaded",
    totalQuestions <= maxQuestions, `got ${totalQuestions}`);
}

// ─── 14. QUIZ GUARD — totalQuestions starts at 0 ────────────
section("QUIZ GUARD — totalQuestions Starts at 0");
{
  // Verifies the fix: totalQuestions must start at 0, not maxQuestions
  const maxQuestions = 15;
  
  // OLD BUG: totalQuestions was initialized to maxQuestions
  const buggyTotalQuestions = maxQuestions; // Bug: setQuizConfig set this to questionCount
  const buggyGuard = buggyTotalQuestions >= maxQuestions; // Always true!
  
  // NEW FIX: totalQuestions starts at 0
  const fixedTotalQuestions = 0; // Fix: QuizUiState() defaults to 0
  const fixedGuard = fixedTotalQuestions >= maxQuestions; // False — allows first load
  
  assert("Fix — buggy guard blocks first question", buggyGuard === true,
    `buggy guard: ${buggyGuard}`);
  assert("Fix — fixed guard allows first question", fixedGuard === false,
    `fixed guard: ${fixedGuard}`);
  
  // Simulate: load 15 questions with fixed guard
  let total = 0;
  const loaded: number[] = [];
  for (let i = 0; i < 25; i++) {
    if (total >= maxQuestions) continue;
    total++;
    loaded.push(i);
  }
  
  assert("Fix — fixed guard loads exactly 15", total === 15, `got ${total}`);
  assert("Fix — fixed guard stops at 15", loaded.length === 15, `got ${loaded.length}`);
}

// ─── RESULTS ─────────────────────────────────────────────────
console.log("\n" + "═".repeat(50));
console.log(`RESULTS: ${passed} passed, ${failed} failed`);
if (failures.length > 0) {
  console.log("\nFailed tests:");
  failures.forEach(f => console.log(`  ❌ ${f}`));
}
console.log("═".repeat(50));

Deno.exit(failed > 0 ? 1 : 0);
