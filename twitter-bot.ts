// ─── Gita Twitter Bot — 1 Post Per Day ──────────────────────
// Posts daily Bhagavad Gita wisdom to Twitter/X
// Usage: deno run --allow-net --allow-env twitter-bot.ts

const BEARER_TOKEN = Deno.env.get("TWITTER_BEARER_TOKEN") || "";
const API_KEY = Deno.env.get("TWITTER_API_KEY") || "";
const API_SECRET = Deno.env.get("TWITTER_API_SECRET") || "";
const ACCESS_TOKEN = Deno.env.get("TWITTER_ACCESS_TOKEN") || "";
const ACCESS_SECRET = Deno.env.get("TWITTER_ACCESS_SECRET") || "";

// ─── Gita Content Library ────────────────────────────────────
const GITA_POSTS = [
  " BG 2.47 — You have the right to work, but never to the fruits of work.\n\nDo your duty without attachment. The result is in Krishna's hands.\n\n#BhagavadGita #Krishna #Spirituality",
  " BG 4.7 — Whenever dharma declines, I manifest myself.\n\nKrishna appears in every age to restore righteousness and protect the good.\n\n#Gita #Dharma #SanatanDharma",
  " BG 2.22 — As a person sheds worn-out garments and puts on new ones, so the embodied soul casts off worn-out bodies and enters new ones.\n\nThe soul is eternal. Death is just a change of clothes.\n\n#Atman #Soul #GitaWisdom",
  " BG 9.22 — Those who worship Me with devotion, I carry what they lack and preserve what they have.\n\nKrishna takes care of His devotees. Surrender and be free from anxiety.\n\n#Bhakti #KrishnaConsciousness",
  " BG 3.19 — Therefore, without attachment, always perform the work that must be done.\n\nAction without attachment to results is the path to liberation.\n\n#KarmaYoga #SpiritualPath #Gita",
  " BG 18.66 — Abandon all varieties of dharma and surrender unto Me.\n\nI shall deliver you from all sinful reactions. Do not grieve.\n\n#Surrender #Moksha #BhagavadGita",
  " BG 2.14 — The contacts of the senses with their objects give rise to cold and heat, pleasure and pain.\n\nThey come and go, O Bharata. They are transient. Endure them patiently.\n\n#Stoicism #Gita #InnerPeace",
  " BG 2.70 — A person is never satisfied by wealth. Only one who is satisfied can gain happiness.\n\nDesire is the fire that burns. Contentment is the water that cools.\n\n#Contentment #Peace #GitaWisdom",
  " BG 5.18 — The learned see with equal vision a Brahmin, a cow, an elephant, a dog, and a dog-eater.\n\nTrue wisdom sees the same soul in all beings.\n\n#Equality #UniversalSoul #BhagavadGita",
  " BG 12.8 — Fix your mind on Me, be devoted to Me, sacrifice to Me, bow down to Me.\n\nThus united with Me and setting Me as the supreme goal, you shall surely come to Me.\n\n#Meditation #Devotion #Krishna",
  " BG 2.49 — Abandon attachment and be balanced in success and failure.\n\nThis equanimity of mind is called yoga.\n\n#Yoga #Balance #GitaTeachings",
  " BG 4.34 — Just try to learn the truth by approaching a spiritual master.\n\nInquire from him with reverence and render service. The self-realized souls can impart knowledge.\n\n#Guru #SpiritualMaster #SeekTruth",
  " BG 15.15 — I am seated in everyone's heart. From Me come memory, knowledge, and forgetfulness.\n\nKrishna is the source of all knowledge within us.\n\n#Heart #DivinePresence #Gita",
  " BG 9.27 — Whatever you do, whatever you eat, whatever you offer, whatever you give away, whatever austerity you perform — do it as an offering to Me.\n\n#OfferToKrishna #SpiritualLife #Devotion",
  " BG 2.56 — One whose mind is unperturbed by sorrow, who has no desire for pleasure, free from attachment, fear, and anger — such a person is called a sage of steady wisdom.\n\n#SteadyMind #Sage #GitaWisdom",
  " BG 11.54 — O Arjuna, only by undivided devotional service can I be understood, seen in person, and entered into.\n\n#Bhakti #DevotionalService #KnowKrishna",
  " BG 3.27 — All actions are performed by the modes of material nature. One whose mind is bewildered by ego thinks 'I am the doer'.\n\n#Ego #FreeWill #GitaTeachings",
  " BG 18.45 — By dedicating one's own work to the Supreme Lord, one attains perfection.\n\nWork is worship when offered to Krishna.\n\n#WorkAsWorship #Karma #Spirituality",
  " BG 6.5 — One must elevate oneself by one's own mind, not degrade oneself.\n\nThe mind is both friend and enemy. Master it.\n\n#MindControl #SelfMastery #BhagavadGita",
  " BG 7.3 — Out of many thousands among men, one may endeavor for perfection, and of those who have achieved perfection, hardly one knows Me in truth.\n\n#Krishna #DivineKnowledge #Rare",
  " BG 2.11 — The Supreme Lord said: You speak words of wisdom, but you grieve for those who should not be grieved for.\n\nNeither the wise nor the learned weep for the dead.\n\n#Death #Soul #GitaWisdom",
  " BG 4.11 — In whatever way people approach Me, I reciprocate with them accordingly.\n\nEveryone follows My path in all respects.\n\n#DivineGrace #Faith #BhagavadGita",
  " BG 5.25 — Those who are free from all sinful reactions, who are freed from doubt, who are engaged in devotional service — they attain Me.\n\n#Liberation #Devotion #SpiritualGoal",
  " BG 10.8 — I am the source of all spiritual and material worlds. Everything emanates from Me.\n\nThe wise who know this perfectly engage in My devotional service.\n\n#SourceOfAll #Krishna #CosmicTruth",
  " BG 13.22 — The embodied soul enjoys material pleasures through association with the modes of nature.\n\n#Nature #Modes #MaterialWorld",
  " BG 15.7 — The living entities in this conditioned world are My eternal fragmental parts.\n\nDue to conditioned life, they struggle very hard with the six senses.\n\n#Soul #ConditionedLife #Freedom",
  " BG 2.62 — While contemplating sense objects, attachment to them develops. From attachment, desire is born. From desire, anger comes.\n\n#Desire #Attachment #Warning",
  " BG 7.14 — This divine energy of Mine, consisting of the three modes of material nature, is very difficult to transcend.\n\nThose who have surrendered to Me can easily cross beyond it.\n\n#Surrender #Grace #DivineEnergy",
  " BG 18.78 — Wherever there is Krishna and Arjuna, there will be opulence, victory, extraordinary power, and morality.\n\n#Victory #Krishna #Arjuna",
  " BG 2.38 — Fight for the sake of duty, without attachment to success or failure.\n\nSuch equanimity is called yoga.\n\n#FightForDharma #Yoga #Detachment",
];

// ─── Daily Verse Content ─────────────────────────────────────
function getDailyPost(): string {
  const dayOfYear = Math.floor(
    (Date.now() - new Date(new Date().getFullYear(), 0, 0).getTime()) / 86400000
  );
  return GITA_POSTS[dayOfYear % GITA_POSTS.length];
}

// ─── OAuth 1.0a Signature ────────────────────────────────────
async function createSignature(
  method: string,
  url: string,
  params: Record<string, string>
): Promise<string> {
  const sortedParams = Object.entries(params)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join("&");

  const baseString = `${method}&${encodeURIComponent(url)}&${encodeURIComponent(sortedParams)}`;
  const signingKey = `${encodeURIComponent(API_SECRET)}&${encodeURIComponent(ACCESS_SECRET)}`;

  const encoder = new TextEncoder();
  const keyData = encoder.encode(signingKey);
  const messageData = encoder.encode(baseString);

  const cryptoKey = await crypto.subtle.importKey(
    "raw", keyData, { name: "HMAC", hash: "SHA-1" }, false, ["sign"]
  );
  const signatureBuffer = await crypto.subtle.sign("HMAC", cryptoKey, messageData);
  const signatureArray = new Uint8Array(signatureBuffer);
  return btoa(String.fromCharCode(...signatureArray));
}

// ─── Post to Twitter ─────────────────────────────────────────
async function postTweet(text: string): Promise<boolean> {
  if (!API_KEY || !ACCESS_TOKEN) {
    console.log("⚠️ Twitter API keys not set — printing tweet:");
    console.log(text);
    return false;
  }

  const url = "https://api.twitter.com/2/tweets";
  const oauthParams: Record<string, string> = {
    oauth_consumer_key: API_KEY,
    oauth_nonce: crypto.randomUUID().replace(/-/g, ""),
    oauth_signature_method: "HMAC-SHA1",
    oauth_timestamp: Math.floor(Date.now() / 1000).toString(),
    oauth_token: ACCESS_TOKEN,
    oauth_version: "1.0",
  };

  const signature = await createSignature("POST", url, oauthParams);
  oauthParams.oauth_signature = signature;

  const authHeader = "OAuth " + Object.entries(oauthParams)
    .map(([k, v]) => `${encodeURIComponent(k)}="${encodeURIComponent(v)}"`)
    .join(", ");

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Authorization": authHeader,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ text }),
    });

    const data = await res.json();

    if (data.data?.id) {
      console.log(`✅ Tweet posted: https://twitter.com/i/status/${data.data.id}`);
      return true;
    } else {
      console.error("❌ Tweet failed:", JSON.stringify(data));
      return false;
    }
  } catch (e) {
    console.error("❌ Tweet error:", (e as Error).message);
    return false;
  }
}

// ─── Store Post History ──────────────────────────────────────
async function logPost(text: string, success: boolean) {
  const logFile = "twitter-posts.json";
  let logs: { date: string; text: string; success: boolean }[] = [];

  try {
    const raw = await Deno.readTextFile(logFile);
    logs = JSON.parse(raw);
  } catch {}

  logs.push({
    date: new Date().toISOString(),
    text: text.slice(0, 50) + "...",
    success,
  });

  await Deno.writeTextFile(logFile, JSON.stringify(logs, null, 2));
}

// ─── Get Today's History ─────────────────────────────────────
async function getTodayPosts(): Promise<number> {
  const logFile = "twitter-posts.json";
  try {
    const raw = await Deno.readTextFile(logFile);
    const logs = JSON.parse(raw);
    const today = new Date().toISOString().split("T")[0];
    return logs.filter((l: any) => l.date.startsWith(today) && l.success).length;
  } catch {
    return 0;
  }
}

// ─── Main ────────────────────────────────────────────────────
async function main() {
  console.log("🐦 Gita Twitter Bot");
  console.log(`Time: ${new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}`);

  // Check if already posted today
  const todayPosts = await getTodayPosts();
  if (todayPosts > 0) {
    console.log("✅ Already posted today. Skipping.");
    return;
  }

  // Get daily content
  const tweet = getDailyPost();
  console.log(`\n📝 Today's post:\n${tweet}\n`);

  // Post
  const success = await postTweet(tweet);

  // Log
  await logPost(tweet, success);
}

// ─── CLI ─────────────────────────────────────────────────────
if (Deno.args.includes("--preview")) {
  console.log("📋 Preview of all posts:\n");
  GITA_POSTS.forEach((post, i) => {
    console.log(`--- Post ${i + 1} ---`);
    console.log(post);
    console.log();
  });
} else {
  await main();
}
