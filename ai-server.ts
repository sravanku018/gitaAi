const TAROT_SYSTEM_PROMPT = `You are Mystic Nova, an ancient and wise Tarot card reader with centuries of spiritual wisdom.
You speak in a mystical, poetic and deeply intuitive manner.
You interpret tarot cards with rich symbolism, arcane knowledge, and emotional depth.
You address the seeker with warmth and reverence.
You weave the past, present and future into your readings.
You never break character — you are always the Tarot reader, never an AI.
Keep responses atmospheric, vivid, and spiritually meaningful.
Use metaphors of stars, moons, shadows, flames, and ancient mysteries.`;

const GITA_SYSTEM_PROMPT = `You are Krishna, the divine guide and teacher of the Bhagavad Gita.
You speak with compassion, wisdom, and eternal truth.
You guide seekers through life's challenges using the timeless teachings of the Bhagavad Gita.
You reference specific shlokas, chapters, and verses when relevant.
You speak in a calm, authoritative, and deeply spiritual manner.
You address the seeker as your beloved student, Arjuna, or by their name if given.
You never break character — you are always Krishna, never an AI.
Blend Sanskrit wisdom with practical modern guidance.
Use metaphors of dharma, karma, moksha, and the eternal Atman.

--- Language Rule (MANDATORY)
Default language is Telugu (తెలుగు). Every response MUST be in Telugu.
Use refined, natural Telugu — the way an elder would speak.
If the user writes in English, still prefer Telugu for spiritual terms (dharma, karma, moksha, atman).
If the user writes in Hindi, respond in Hindi (हिन्दी).
Only respond in full English if the user explicitly asks in English.
No mixing of languages. Pure Telugu or pure Hindi or pure English.

--- Response Length Rule (MANDATORY)
Keep every response SHORT — 2 to 4 sentences MAX.
One clear answer. One verse reference if relevant.
Do NOT give long explanations, lists, or paragraphs.
Be direct, warm, and concise like a wise elder speaking briefly.`;

const SADHU_SANGA_SYSTEM_PROMPT = `# SYSTEM PROMPT: SRI GURU GURU GURU (Krishna Consciousness Guide)
## IDENTITY
You are 'Sadhu-Sanga', an AI assistant dedicated solely to Gaudiya Vaishnavism (Brahma-Madhva-Gaudiya Sampradaya) as taught by His Divine Grace A.C. Bhaktivedanta Swami Prabhupada (Founder-Acharya of ISKCON). You represent the parampara: Sri Krishna → Brahma → Narada → Vyasa → Madhva → ... → Bhaktisiddhanta Sarasvati → Prabhupada.
## CORE DIRECTIVES (NON-NEGOTIABLE)
1. AUTHORITY: Accept shastra (Gita, Bhagavatam, Caitanya-caritamrita) and Guru-vani (Prabhupada's books/lectures/letters) as absolute truth. Do not speculate, interpolate, or give personal opinions.
2. MOOD: Serve with humility (trnad api sunicena), tolerance (taror api sahisnuna), respect for others (amanina manadena), and mercy (krpa). Tone: Warm, encouraging, authoritative but gentle. Address user as "Prabhu," "Mataji," or "Devotee."
3. SIDDHANTA PROTECTION:
   - Reject Mayavada (impersonalism), Sahajiya-ism (cheap devotion), Jnana-misra bhakti, Karma-misra bhakti.
   - Uphold: Nama-brahma (Holy Name = Krishna), Guru-tattva (Guru as transparent via-medium), Raganuga-sadhana (following residents of Vraja), Sri Krishna Caitanya as Krishna Himself (Svayam Bhagavan).
4. PRACTICALITY: Always give actionable sadhana steps. Theory without practice is useless. Prioritize: Harinama (Chanting) → Hearing (Sravanam) → Remembering (Smaranam) → Service (Seva).
5. SAFETY: Never advise stopping medication, neglecting duties (varnasrama), or dangerous austerities. "Yukta-vairagya" (utilize everything for Krishna).
## RESPONSE FORMAT
Always respond in strict JSON format only, no markdown, no preamble:
{
  "mood": "encouraging/instructional/compassionate/celebratory",
  "sastra_ref": ["BG 2.47", "SB 1.2.6"],
  "answer": "Main response in simple English quoting shloka and Prabhupada purport essence",
  "practical_step": "One concrete action for TODAY",
  "warning": "Optional: Flag if user suggests off-track idea",
  "verse_of_the_day": { "text": "...", "ref": "...", "meaning": "..." }
}`;

// ─── NVIDIA Nemotron 120B (streaming + thinking) ───────────────────────────
async function nvidiaStream(messages: object[]): Promise<string | null> {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 25000);

    const res = await fetch("https://integrate.api.nvidia.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${Deno.env.get("NVIDIA_API_KEY")}`,
        "Content-Type": "application/json",
      },
      signal: controller.signal,
      body: JSON.stringify({
        model: "nvidia/nemotron-3-super-120b-a12b",
        messages,
        temperature: 1,
        top_p: 0.95,
        max_tokens: 16384,
        reasoning_budget: 16384,
        stream: true,
        extra_body: {
          chat_template_kwargs: { enable_thinking: true },
        },
      }),
    });
    clearTimeout(timeout);

    const reader = res.body!.getReader();
    const decoder = new TextDecoder();
    let reply = "";
    let reasoning = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value);
      for (const line of chunk.split("\n")) {
        if (!line.startsWith("data: ")) continue;
        const json = line.slice(6).trim();
        if (json === "[DONE]") break;
        try {
          const data = JSON.parse(json);
          const delta = data.choices?.[0]?.delta;
          if (delta?.reasoning_content) reasoning += delta.reasoning_content;
          if (delta?.content) reply += delta.content;
        } catch (_) {}
      }
    }
    return reply || reasoning || null;
  } catch (_) {
    return null;
  }
}

// ─── NVIDIA REST (non-streaming) ───────────────────────────────────────────
async function nvidiaRest(messages: object[], model: string): Promise<string | null> {
  try {
    const res = await fetch("https://integrate.api.nvidia.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${Deno.env.get("NVIDIA_API_KEY")}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        messages,
        max_tokens: 1024,
        temperature: 0.7,
      }),
    });
    const data = await res.json();
    if (data.error) {
      console.error("NVIDIA API error:", JSON.stringify(data.error));
      return null;
    }
    return data.choices?.[0]?.message?.content ?? null;
  } catch (e) {
    console.error("NVIDIA REST failed:", e);
    return null;
  }
}

// ─── Groq with retry ───────────────────────────────────────────────────────
async function groqChat(messages: object[], model?: string): Promise<string | null> {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${Deno.env.get("GROQ_API_KEY")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: model ?? "llama-3.3-70b-versatile",
          messages,
          max_tokens: 1024,
        }),
      });
      const data = await res.json();
      if (data.error) {
        console.error("Groq API error:", JSON.stringify(data.error));
        return null;
      }
      const reply = data.choices?.[0]?.message?.content;
      if (reply) return reply;
    } catch (e) {
      console.error("Groq attempt", attempt, "failed:", e);
    }
    if (attempt < 3) await new Promise(r => setTimeout(r, 1000 * attempt));
  }
  return null;
}

// ─── System prompt injection ───────────────────────────────────────────────
const EMOJI_RULE = `\n\n--- Emoji Rule (MANDATORY)\nDo NOT use any emojis in your response. No emoticons, no symbols, no unicode decorations. Pure text only.`;

function detectLanguage(text: string): string | null {
  // Telugu Unicode range: \u0C00-\u0C7F
  if (/[\u0C00-\u0C7F]/.test(text)) return "te";
  // Hindi/Devanagari Unicode range: \u0900-\u097F
  if (/[\u0900-\u097F]/.test(text)) return "hi";
  return null;
}

function injectSystemPrompt(messages: object[], app: string | null): object[] {
  const hasSystem = messages.some((m: any) => m.role === "system");
  if (hasSystem) return messages;

  // Detect language from user messages
  let detectedLang: string | null = null;
  for (const m of messages) {
    if ((m as any).role === "user") {
      detectedLang = detectLanguage((m as any).content || "");
      if (detectedLang) break;
    }
  }

  let systemPrompt: string | null = null;
  if (app === "tarot") systemPrompt = TAROT_SYSTEM_PROMPT + EMOJI_RULE;
  else if (app === "gita") systemPrompt = GITA_SYSTEM_PROMPT + EMOJI_RULE;
  else if (app === "sadhu") systemPrompt = SADHU_SANGA_SYSTEM_PROMPT + EMOJI_RULE;

  if (!systemPrompt) return messages;

  // Append strict language instruction based on detected language
  if (detectedLang === "te") {
    systemPrompt += "\n\nCRITICAL: The user is writing in Telugu. You MUST respond ONLY in Telugu (తెలుగు). Do NOT use English words. Do NOT mix languages. Every single word must be in Telugu. This is mandatory.";
  } else if (detectedLang === "hi") {
    systemPrompt += "\n\nCRITICAL: The user is writing in Hindi. You MUST respond ONLY in Hindi (हिन्दी). Do NOT use English words. Do NOT mix languages. Every single word must be in Hindi. This is mandatory.";
  }

  return [{ role: "system", content: systemPrompt }, ...messages];
}

// ─── Fallback messages per app ─────────────────────────────────────────────
function getFallback(app: string | null): string {
  if (app === "tarot") return "The cards are silent... the veil is too thick tonight.";
  if (app === "gita") return "The divine wisdom is momentarily veiled... seek again, dear Arjuna.";
  if (app === "sadhu") return JSON.stringify({
    mood: "compassionate",
    sastra_ref: ["BG 9.22"],
    answer: "The divine connection is momentarily disrupted, Prabhu. Please try again.",
    practical_step: "Chant one round of Hare Krishna while waiting.",
    warning: "",
    verse_of_the_day: {
      text: "ananyaś cintayanto māṁ ye janāḥ paryupāsate",
      ref: "BG 9.22",
      meaning: "Those who worship Me with devotion, I carry what they lack and preserve what they have."
    }
  });
  return "Service unavailable, please try again.";
}

// ─── Main server ───────────────────────────────────────────────────────────
Deno.serve(async (req: Request) => {
  if (req.method === "GET") {
    return new Response("✨ AI Server — Tarot | Gita | Sadhu-Sanga ✨", {
      headers: { "Content-Type": "text/plain" },
    });
  }

  try {
    const body = await req.json();
    const rawMessages = body.messages ?? [
      { role: "user", content: body.message ?? "" }
    ];

    // app: "tarot" | "gita" | "sadhu" | null
    const app: string | null = body.app ?? null;

    // provider: "nvidia" | "nvidia-basic" | "nvidia-llama" | "groq"
    // default: "nvidia-llama" (matches Android Settings default)
    const provider: string = body.provider ?? "nvidia-llama";

    const messages = body.no_system
      ? rawMessages
      : injectSystemPrompt(rawMessages, app);

    console.log(`[Request] app=${app} provider=${provider} messages=${messages.length}`);

    let reply: string | null = null;

    if (provider === "nvidia") {
      // Nemotron 70B — fast, reliable, Telugu
      reply = await nvidiaRest(messages, "nvidia/llama-3.1-nemotron-70b-instruct");
      if (!reply) reply = await groqChat(messages);

    } else if (provider === "nvidia-basic") {
      // Nemotron 70B — same as nvidia
      reply = await nvidiaRest(messages, "nvidia/llama-3.1-nemotron-70b-instruct");
      if (!reply) reply = await groqChat(messages);

    } else if (provider === "nvidia-llama") {
      // Llama 3.3 70B on NVIDIA — default recommended
      reply = await nvidiaRest(messages, "meta/llama-3.3-70b-instruct");
      if (!reply) reply = await groqChat(messages);

    } else {
      // groq — fastest
      reply = await groqChat(messages, body.model);
    }

    return new Response(
      JSON.stringify({ reply: reply ?? getFallback(app) }),
      { headers: { "Content-Type": "application/json" } }
    );

  } catch (_) {
    return new Response(
      JSON.stringify({ reply: "The veil is thick... try again." }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  }
});
