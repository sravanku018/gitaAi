// ─── HuggingFace Dataset Ingestion Test ──────────────────────
// Tests: CSV download, parsing, MCQ conversion, dedup, difficulty
// Usage: deno run --allow-net --allow-env test-huggingface-dataset.ts

const DATASET_URLS: Record<string, string> = {
  english: "https://huggingface.co/datasets/JDhruv14/Bhagavad-Gita-QA/resolve/main/English/english.csv",
  hindi: "https://huggingface.co/datasets/JDhruv14/Bhagavad-Gita-QA/resolve/main/Hindi/hindi.csv",
  gujarati: "https://huggingface.co/datasets/JDhruv14/Bhagavad-Gita-QA/resolve/main/Gujarati/gujarati.csv",
};

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

// ─── HELPERS (mirror DatasetIngestionPipeline logic) ─────────

interface RawQuestion {
  chapterNo: number;
  verseNo: number;
  question: string;
  answer: string;
}

function parseCsvLine(line: string): string[] {
  const fields: string[] = [];
  let sb = "";
  let inQuotes = false;
  for (const char of line) {
    if (char === '"') inQuotes = !inQuotes;
    else if (char === "," && !inQuotes) { fields.push(sb); sb = ""; }
    else sb += char;
  }
  fields.push(sb);
  return fields;
}

function parseCsv(content: string): RawQuestion[] {
  const questions: RawQuestion[] = [];
  const lines = content.split("\n").filter(l => l.trim());
  const dataLines = lines[0]?.includes("chapter_no") ? lines.slice(1) : lines;

  for (const line of dataLines) {
    try {
      const fields = parseCsvLine(line);
      if (fields.length < 4) continue;
      const chapterNo = parseInt(fields[0].trim());
      const verseNo = parseInt(fields[1].trim());
      const question = fields[2].trim().replace(/^"|"$/g, "").trim();
      const answer = fields[3].trim().replace(/^"|"$/g, "").trim();
      if (!chapterNo || !verseNo || !question || !answer) continue;
      questions.push({ chapterNo, verseNo, question, answer });
    } catch {}
  }
  return questions;
}

function extractKeyConcepts(answer: string): string[] {
  const concepts: string[] = [];
  const lower = answer.toLowerCase();
  const conceptMap: Record<string, string[]> = {
    dharma: ["dharma", "duty", "righteous", "moral"],
    karma: ["karma", "action", "work", "deed"],
    devotion: ["devotion", "bhakti", "love", "worship"],
    knowledge: ["knowledge", "wisdom", "understand"],
    soul: ["soul", "atman", "self", "eternal"],
    detachment: ["detachment", "desireless", "renounce"],
    meditation: ["meditation", "concentrate", "focus", "mind"],
    peace: ["peace", "calm", "equanimity", "joy"],
    liberation: ["liberation", "moksha", "freedom"],
    anger: ["anger", "lust", "greed", "passion"],
  };
  for (const [concept, keywords] of Object.entries(conceptMap)) {
    if (keywords.some(k => lower.includes(k))) concepts.push(concept);
  }
  return concepts;
}

function estimateDifficulty(chapter: number): number {
  if (chapter <= 6) return Math.min(10, Math.max(1, 3 + Math.floor(chapter / 2)));
  if (chapter <= 12) return Math.min(10, Math.max(1, 5 + Math.floor(chapter / 3)));
  return Math.min(10, Math.max(1, 7 + Math.floor(chapter / 4)));
}

// ═══════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════

// ─── 1. CSV DOWNLOAD ─────────────────────────────────────────
section("CSV DOWNLOAD");
let englishCsv = "";

{
  const start = Date.now();
  const res = await fetch(DATASET_URLS.english);
  const latency = Date.now() - start;
  assert("English CSV — HTTP 200", res.ok, `status=${res.status}`);
  englishCsv = await res.text();
  assert("English CSV — has content", englishCsv.length > 1000, `size=${englishCsv.length}`);
  assert("English CSV — latency < 30s", latency < 30000, `${latency}ms`);
  console.log(`  ℹ️  Downloaded ${englishCsv.length} bytes in ${latency}ms`);
}

// ─── 2. CSV PARSING ──────────────────────────────────────────
section("CSV PARSING");
let rawQuestions: RawQuestion[] = [];

{
  rawQuestions = parseCsv(englishCsv);
  assert("Parsed — has questions", rawQuestions.length > 100, `got ${rawQuestions.length}`);
  
  // All questions should have valid chapter/verse
  const valid = rawQuestions.filter(q => q.chapterNo >= 1 && q.chapterNo <= 18 && q.verseNo >= 1);
  assert("Parsed — valid chapter/verse", valid.length === rawQuestions.length, 
    `${valid.length}/${rawQuestions.length} valid`);
  
  // Check chapter distribution
  const chapters = new Set(rawQuestions.map(q => q.chapterNo));
  assert("Parsed — covers multiple chapters", chapters.size >= 10, `got ${chapters.size} chapters`);
  
  // Sample question
  const sample = rawQuestions[0];
  console.log(`  ℹ️  Sample: Ch${sample.chapterNo} V${sample.verseNo} — "${sample.question.slice(0, 60)}..."`);
}

// ─── 3. MCQ CONVERSION ──────────────────────────────────────
section("MCQ CONVERSION");
{
  // Build topic index
  const topicAnswers = new Map<string, string[]>();
  rawQuestions.forEach(q => {
    extractKeyConcepts(q.answer).forEach(topic => {
      if (!topicAnswers.has(topic)) topicAnswers.set(topic, []);
      topicAnswers.get(topic)!.push(q.answer);
    });
  });
  assert("MCQ — topic index built", topicAnswers.size > 0, `${topicAnswers.size} topics`);

  // Convert first 50 questions
  const sample = rawQuestions.slice(0, 50);
  const mcqs = sample.map(raw => {
    const topics = extractKeyConcepts(raw.answer);
    const distractors: string[] = [];
    topics.forEach(t => {
      (topicAnswers.get(t) || []).forEach(a => {
        if (a !== raw.answer && a.length > 10 && distractors.length < 3) distractors.push(a);
      });
    });
    const generalDistractors = [
      "By performing rituals and ceremonies",
      "By accumulating wealth and power",
      "By avoiding all worldly duties",
    ];
    while (distractors.length < 3) distractors.push(generalDistractors[distractors.length] || "Unknown");
    
    const options = [raw.answer, ...distractors.slice(0, 3)].sort(() => Math.random() - 0.5);
    const correctIdx = options.indexOf(raw.answer);
    
    return {
      question: raw.question,
      options,
      correctAnswer: ["A", "B", "C", "D"][correctIdx],
      difficulty: estimateDifficulty(raw.chapterNo),
      topics,
    };
  });

  assert("MCQ — generated 50 MCQs", mcqs.length === 50);
  assert("MCQ — all have 4 options", mcqs.every(q => q.options.length === 4));
  assert("MCQ — all have correct answer", mcqs.every(q => ["A", "B", "C", "D"].includes(q.correctAnswer)));
  assert("MCQ — correct answer in options", mcqs.every(q => {
    const idx = "ABCD".indexOf(q.correctAnswer);
    return q.options[idx] !== undefined;
  }));

  // Topic coverage
  const allTopics = new Set(mcqs.flatMap(q => q.topics));
  assert("MCQ — covers Gita topics", allTopics.size >= 5, `topics: ${[...allTopics].join(", ")}`);
}

// ─── 4. DEDUPLICATION ────────────────────────────────────────
section("DEDUPLICATION");
{
  const hashes = rawQuestions.map(q => `${q.chapterNo}:${q.verseNo}:${q.question.hashCode}`);
  const uniqueHashes = new Set(hashes);
  const dupCount = hashes.length - uniqueHashes.size;
  
  // Manual hash check
  const manualHashes = rawQuestions.map(q => {
    let hash = 0;
    const str = `${q.chapterNo}:${q.verseNo}:${q.question}`;
    for (let i = 0; i < str.length; i++) {
      hash = ((hash << 5) - hash) + str.charCodeAt(i);
      hash |= 0;
    }
    return hash.toString();
  });
  const uniqueManual = new Set(manualHashes);
  
  assert("Dedup — hashes computed", manualHashes.length === rawQuestions.length);
  console.log(`  ℹ️  ${rawQuestions.length} total → ${uniqueManual.size} unique (${dupCount} duplicates)`);
}

// ─── 5. DIFFICULTY ESTIMATION ────────────────────────────────
section("DIFFICULTY ESTIMATION");
{
  // Easy: chapters 1-6
  const easy = rawQuestions.filter(q => q.chapterNo <= 6);
  const easyDiffs = easy.map(q => estimateDifficulty(q.chapterNo));
  const avgEasy = easyDiffs.reduce((a, b) => a + b, 0) / easyDiffs.length;
  assert("Difficulty — early chapters easy", avgEasy <= 5, `avg=${avgEasy.toFixed(1)}`);

  // Medium: chapters 7-12
  const medium = rawQuestions.filter(q => q.chapterNo >= 7 && q.chapterNo <= 12);
  const medDiffs = medium.map(q => estimateDifficulty(q.chapterNo));
  const avgMed = medDiffs.reduce((a, b) => a + b, 0) / medDiffs.length;
  assert("Difficulty — mid chapters medium", avgMed >= 4 && avgMed <= 8, `avg=${avgMed.toFixed(1)}`);

  // Hard: chapters 13-18
  const hard = rawQuestions.filter(q => q.chapterNo >= 13);
  const hardDiffs = hard.map(q => estimateDifficulty(q.chapterNo));
  const avgHard = hardDiffs.reduce((a, b) => a + b, 0) / hardDiffs.length;
  assert("Difficulty — late chapters hard", avgHard >= 7, `avg=${avgHard.toFixed(1)}`);

  console.log(`  ℹ️  Easy avg: ${avgEasy.toFixed(1)}, Medium avg: ${avgMed.toFixed(1)}, Hard avg: ${avgHard.toFixed(1)}`);
}

// ─── 6. CHAPTER/VERSE COVERAGE ───────────────────────────────
section("CHAPTER/VERSE COVERAGE");
{
  const chapterCounts = new Map<number, number>();
  rawQuestions.forEach(q => chapterCounts.set(q.chapterNo, (chapterCounts.get(q.chapterNo) || 0) + 1));
  
  assert("Coverage — all 18 chapters", chapterCounts.size === 18, `got ${chapterCounts.size}`);
  
  // Check each chapter has questions
  for (let ch = 1; ch <= 18; ch++) {
    const count = chapterCounts.get(ch) || 0;
    if (count === 0) {
      console.log(`    ⚠️  Chapter ${ch}: 0 questions`);
    }
  }
  const minCount = Math.min(...chapterCounts.values());
  assert("Coverage — every chapter has questions", minCount > 0, `min=${minCount}`);
  
  console.log(`  ℹ️  Chapter range: ${Math.min(...chapterCounts.keys())}-${Math.max(...chapterCounts.keys())}, questions per chapter: ${minCount}-${Math.max(...chapterCounts.values())}`);
}

// ─── 7. QUESTION QUALITY ─────────────────────────────────────
section("QUESTION QUALITY");
{
  // Check question length
  const shortQ = rawQuestions.filter(q => q.question.length < 10);
  assert("Quality — no too-short questions", shortQ.length === 0, `${shortQ.length} too short`);
  
  // Check answer length
  const shortA = rawQuestions.filter(q => q.answer.length < 5);
  assert("Quality — no too-short answers", shortA.length === 0, `${shortA.length} too short`);
  
  // Check for empty options
  const sample50 = rawQuestions.slice(0, 50);
  const emptyOptions = sample50.filter(q => !q.question.trim() || !q.answer.trim());
  assert("Quality — no empty fields", emptyOptions.length === 0);
  
  // Check encoding (no garbage chars)
  const garbled = rawQuestions.filter(q => /[\x00-\x08\x0E-\x1F]/.test(q.question));
  assert("Quality — no garbled encoding", garbled.length === 0, `${garbled.length} garbled`);
}

// ─── 8. HINDI DATASET ────────────────────────────────────────
section("HINDI DATASET");
{
  const res = await fetch(DATASET_URLS.hindi);
  assert("Hindi CSV — HTTP 200", res.ok, `status=${res.status}`);
  const hindiCsv = await res.text();
  assert("Hindi CSV — has content", hindiCsv.length > 1000, `size=${hindiCsv.length}`);
  
  const hindiQ = parseCsv(hindiCsv);
  assert("Hindi — parsed questions", hindiQ.length > 100, `got ${hindiQ.length}`);
  
  // Check for Devanagari characters
  const hasDevanagari = hindiQ.some(q => /[\u0900-\u097F]/.test(q.question));
  assert("Hindi — contains Devanagari script", hasDevanagari);
  console.log(`  ℹ️  Hindi: ${hindiQ.length} questions`);
}

// ─── 9. GUJARATI DATASET ─────────────────────────────────────
section("GUJARATI DATASET");
{
  const res = await fetch(DATASET_URLS.gujarati);
  assert("Gujarati CSV — HTTP 200", res.ok, `status=${res.status}`);
  const gujCsv = await res.text();
  assert("Gujarati CSV — has content", gujCsv.length > 1000, `size=${gujCsv.length}`);
  
  const gujQ = parseCsv(gujCsv);
  assert("Gujarati — parsed questions", gujQ.length > 100, `got ${gujQ.length}`);
  
  // Check for Gujarati characters
  const hasGujarati = gujQ.some(q => /[\u0A80-\u0AFF]/.test(q.question));
  assert("Gujarati — contains Gujarati script", hasGujarati);
  console.log(`  ℹ️  Gujarati: ${gujQ.length} questions`);
}

// ─── 10. FULL PIPELINE SIMULATION ────────────────────────────
section("FULL PIPELINE — End to End");
{
  const englishQ = parseCsv(englishCsv);
  
  // 1. Parse
  assert("Pipeline step 1 — parse CSV", englishQ.length > 100);
  
  // 2. Convert to MCQ
  const topicAnswers = new Map<string, string[]>();
  englishQ.forEach(q => {
    extractKeyConcepts(q.answer).forEach(t => {
      if (!topicAnswers.has(t)) topicAnswers.set(t, []);
      topicAnswers.get(t)!.push(q.answer);
    });
  });
  
  const mcqs = englishQ.map(raw => {
    const topics = extractKeyConcepts(raw.answer);
    const distractors: string[] = [];
    topics.forEach(t => {
      (topicAnswers.get(t) || []).forEach(a => {
        if (a !== raw.answer && a.length > 10 && distractors.length < 3) distractors.push(a);
      });
    });
    while (distractors.length < 3) distractors.push("Fallback distractor " + distractors.length);
    const options = [raw.answer, ...distractors.slice(0, 3)];
    return { ...raw, options, difficulty: estimateDifficulty(raw.chapterNo) };
  });
  assert("Pipeline step 2 — convert to MCQ", mcqs.length > 100);
  
  // 3. Normalize
  const normalized = mcqs.map(q => ({
    ...q,
    question: q.question.replace(/\s+/g, " ").trim(),
  }));
  assert("Pipeline step 3 — normalize", normalized.length === mcqs.length);
  
  // 4. Deduplicate
  const seen = new Set<string>();
  const deduped = normalized.filter(q => {
    const key = `${q.chapterNo}:${q.verseNo}:${q.question}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  assert("Pipeline step 4 — deduplicate", deduped.length <= normalized.length, 
    `${normalized.length} → ${deduped.length}`);
  
  // 5. Quality check
  const highQuality = deduped.filter(q => 
    q.question.length > 10 && q.options.every(o => o.length > 3)
  );
  assert("Pipeline step 5 — quality filter", highQuality.length > 100, 
    `${highQuality.length} high-quality`);
  
  console.log(`  ℹ️  Pipeline: ${englishQ.length} raw → ${mcqs.length} MCQ → ${deduped.length} deduped → ${highQuality.length} quality`);
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
