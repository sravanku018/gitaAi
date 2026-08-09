# Gita App — Backend Test Results

**Date:** 2026-06-15  
**Backend:** `https://prime-gorilla-49.sravanku018.deno.net`  
**Voice Backend:** `https://noisy-sheep-76.sravanku018.deno.net`

---

## Backend API — 117/117 ✅

### Auth
| Test | Status |
|------|--------|
| Register — success | ✅ |
| Register — has token | ✅ |
| Register — welcome coins 200 | ✅ |
| Register duplicate — returns token | ✅ |
| Auth verify — valid | ✅ |
| Auth verify — correct user | ✅ |
| Login — success | ✅ |
| Login — has token | ✅ |
| Login — wrong password rejected | ✅ |

### Coins Balance
| Test | Status |
|------|--------|
| Balance — 200 after register | ✅ |
| Balance — yoga_level is 1 | ✅ |
| Balance — checkin_day is 0 | ✅ |
| Balance — share_day is 0 | ✅ |

### Checkin — Day Sequence (Day 1–8 + Wrap)
| Test | Status |
|------|--------|
| Checkin Day 1 — success | ✅ |
| Checkin Day 1 — day=1 | ✅ |
| Checkin Day 1 — coins>0 | ✅ |
| Checkin duplicate — blocked | ✅ |
| Checkin Day 2 — success | ✅ |
| Checkin Day 2 — day=2 | ✅ |
| Checkin Day 3 — success | ✅ |
| Checkin Day 3 — day=3 | ✅ |
| Checkin Day 4 — success | ✅ |
| Checkin Day 4 — day=4 | ✅ |
| Checkin Day 5 — success | ✅ |
| Checkin Day 5 — day=5 | ✅ |
| Checkin Day 6 — success | ✅ |
| Checkin Day 6 — day=6 | ✅ |
| Checkin Day 7 — success | ✅ |
| Checkin Day 7 — day=7 | ✅ |
| Checkin Day 7 — weekly_bonus>0 | ✅ |
| Checkin Day 8 (new week) — success | ✅ |
| Checkin Day 8 — wraps to day=1 | ✅ |

### Checkin — Streak Break
| Test | Status |
|------|--------|
| Streak break — Day 1 | ✅ |
| Streak break — Day 2 | ✅ |
| Streak break — reset to Day 1 | ✅ |

### Checkin — Multi-Week
| Test | Status |
|------|--------|
| 2 weeks — all 14 days correct | ✅ |
| 2 weeks — checkin_week wrapped | ✅ |
| 3 weeks — all 21 days correct | ✅ |
| 3 weeks — checkin_week >= 3 | ✅ |
| 4 weeks — all 28 days correct | ✅ |
| 4 weeks — got 4 weekly bonuses | ✅ |
| 4 weeks — week wrapped back to 1 | ✅ |

### Share — Day Sequence (Day 1–8 + Wrap)
| Test | Status |
|------|--------|
| Share Day 1 — success | ✅ |
| Share Day 1 — share_day=1 | ✅ |
| Share duplicate — blocked | ✅ |
| Share Day 2 — success | ✅ |
| Share Day 2 — share_day=2 | ✅ |
| Share Day 3 — success | ✅ |
| Share Day 3 — share_day=3 | ✅ |
| Share Day 4 — success | ✅ |
| Share Day 4 — share_day=4 | ✅ |
| Share Day 5 — success | ✅ |
| Share Day 5 — share_day=5 | ✅ |
| Share Day 6 — success | ✅ |
| Share Day 6 — share_day=6 | ✅ |
| Share Day 7 — success | ✅ |
| Share Day 7 — share_day=7 | ✅ |
| Share Day 7 — weekly_bonus>0 | ✅ |
| Share Day 8 (new week) — success | ✅ |
| Share Day 8 — wraps to share_day=1 | ✅ |

### Share — Multi-Week
| Test | Status |
|------|--------|
| Share 2 weeks — all 14 days correct | ✅ |
| Share 4 weeks — all 28 days correct | ✅ |
| Share 4 weeks — got 4 weekly bonuses | ✅ |

### Coin History
| Test | Status |
|------|--------|
| History — is array | ✅ |
| History — has entries | ✅ |
| History — no NULL created_at | ✅ |
| History — sorted DESC by created_at | ✅ |
| History — auto_reconcile filtered | ✅ |

### Quiz Accuracy
| Test | Status |
|------|--------|
| Accuracy 0% — base coins only (5) | ✅ |
| Accuracy 50% — 6 coins | ✅ |
| Accuracy 80% — 8 coins | ✅ |
| Accuracy 100% — 11 coins | ✅ |

### Streak Bonuses
| Test | Status |
|------|--------|
| Streak 4 days — no bonus | ✅ |
| Streak 5 days — +1 bonus = 9 coins | ✅ |
| Streak 10 days — +2 bonus = 10 coins | ✅ |
| Streak 15 days — +3 bonus = 11 coins | ✅ |
| Streak 100 days — still max +3 = 11 coins | ✅ |

### Checkin Day Bonuses
| Test | Status |
|------|--------|
| Checkin day 1 — no bonus = 8 | ✅ |
| Checkin day 2 — +1 bonus = 9 | ✅ |
| Checkin day 5 — +2 bonus = 10 | ✅ |
| Checkin day 7 — +3 bonus = 11 | ✅ |

### Max Coin Cap
| Test | Status |
|------|--------|
| Max cap — coins <= raw maximum (17) | ✅ |
| Max cap — coins > 0 | ✅ |

### Score Recording
| Test | Status |
|------|--------|
| Quiz attempt — success | ✅ |
| Quiz history — has entries | ✅ |
| Quiz history — latest score=12 | ✅ |
| Quiz history — latest total=15 | ✅ |
| Quiz history — quiz_type=general | ✅ |
| Quiz history — time recorded | ✅ |

### Edge Cases
| Test | Status |
|------|--------|
| Unknown source — rejected | ✅ |
| Empty user_id — handled | ✅ |
| Accuracy >1 — clamped to 1.0 | ✅ |
| Negative accuracy — clamped to 0 | ✅ |

### Coins Spend
| Test | Status |
|------|--------|
| Spend — success | ✅ |
| Spend — balance decreased | ✅ |
| Spend duplicate — blocked | ✅ |

### Voice Cost
| Test | Status |
|------|--------|
| Voice cost — short question | ✅ |
| Voice cost — medium question | ✅ |
| Voice cost — long question | ✅ |

### Quiz
| Test | Status |
|------|--------|
| Quiz attempt — success | ✅ |
| Quiz history — is array | ✅ |
| Quiz history — has entry | ✅ |
| Quiz history — score correct | ✅ |

### Leaderboard
| Test | Status |
|------|--------|
| Leaderboard — is array | ✅ |
| Leaderboard — no guests | ✅ |

### Yoga Stages
| Test | Status |
|------|--------|
| Yoga stages — has levels | ✅ |
| Yoga stages — has sub_stages | ✅ |

### Guest
| Test | Status |
|------|--------|
| Guest create — has guest_id | ✅ |
| Guest create — starts with guest_ | ✅ |
| Guest create — 50 coins | ✅ |
| Guest create — has token | ✅ |

### Stats Sync
| Test | Status |
|------|--------|
| Stats sync — success | ✅ |
| Stats sync — streak updated | ✅ |

---

## Voice/Chat API — 25/25 ✅

| Test | Status | Latency |
|------|--------|---------|
| GET / — server alive | ✅ | — |
| Simple message — has reply | ✅ | — |
| Simple message — reply not empty | ✅ | — |
| Simple message — not silent | ✅ | — |
| Messages format — has reply | ✅ | — |
| Messages format — reply not empty | ✅ | — |
| Gita Q: duty | ✅ | — |
| Gita Q: Dharma | ✅ | — |
| Gita Q: Ch2 v47 | ✅ | — |
| Gita Q: moksha | ✅ | — |
| Telugu: భగవద్గీత | ✅ | — |
| Telugu: కర్మ యోగం | ✅ | — |
| Telugu: ధర్మం | ✅ | — |
| Multi-turn — has reply | ✅ | — |
| Multi-turn — context aware | ✅ | — |
| Empty message — no crash | ✅ | — |
| Short message — responds | ✅ | — |
| Long message — responds | ✅ | — |
| Off-topic — no crash | ✅ | — |
| Quality — Gita context in reply | ✅ | — |
| Quality — reply > 50 chars | ✅ | — |
| Quality — not error fallback | ✅ | — |
| Latency — under 10s | ✅ | 5520ms |
| Empty body — no 500 crash | ✅ | — |
| Empty body — has reply field | ✅ | — |

---

## Bugs Fixed

| Bug | Fix | File |
|-----|-----|------|
| Timezone: `/checkin` streak breaks for IST users | Compute yesterday from `client_date` instead of UTC | `deno-backend-hono.ts` |
| Timezone: `/share` streak breaks for IST users | Same fix — `today + "T12:00:00Z"` minus 1 day | `deno-backend-hono.ts` |
| Timezone: `last_share = date('now')` uses UTC | Changed to `last_share = ?` using `today` from client | `deno-backend-hono.ts` |
| Coin history: `created_at` missing from INSERTs | Added `datetime('now')` to all 11 `INSERT INTO coin_transactions` | `deno-backend-hono.ts` |
| Coin history: NULL `created_at` rows invisible | Added startup backfill: `UPDATE ... SET created_at = datetime('now') WHERE created_at IS NULL` | `deno-backend-hono.ts` |
| Share week wrap: `share_day` stored as next_day | Stored `share_day` (claimed day) instead of `next_share_day` to match formula `((day % 7) + 1)` | `deno-backend-hono.ts` |
| `checkin_rewards` table not initialized | Added `CREATE TABLE IF NOT EXISTS` + default rows 1–7 | `deno-backend-hono.ts` |
| `weekly_bonus_rules` table not initialized | Added `CREATE TABLE IF NOT EXISTS` + default weeks 1–4 | `deno-backend-hono.ts` |

---

**Total: 142/142 passed** (117 backend + 25 voice)
