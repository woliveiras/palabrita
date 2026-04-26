# Leveling System Review

## What Works

- Cycle 0 with only 5 four-letter words is a good decision — the user exits onboarding quickly and starts playing
- Using a single difficulty per batch simplifies both the LLM prompt and validation logic
- The logic is trivial to understand and test

---

## Pain Points

### 1. The cycle is global, not per language

If the user played up to cycle 3 in Portuguese and switches to English, they immediately start with 6-letter words in English — with no ramp-up for the new language.

### 2. "Leveling" ends at cycle 2

From cycle 2 onward, every generation is identical: 10 six-letter words, forever. This is not a progression system — it's a 3-state switch. The name "leveling" over-promises what it delivers.

### 3. No variety within a session

At cycle ≥2, all puzzles are 6 letters. A mixed batch (e.g., 7 × 6-letter + 3 × 5-letter) would keep the pool less monotonous without complicating generation much.

### 4. `REPLENISHMENT_THRESHOLD == batchSize` at cycle 0

In cycle 0, `REPLENISHMENT_THRESHOLD = 5` equals the batch size of 5. This means:
1. User plays all 5 puzzles → unplayed count drops to 0
2. Generation triggers again → now at cycle 1 → generates 5-letter words

The difficulty jump happens at the first reload, with no warning to the user. This may be intentional, but it is not explicit anywhere.

---

## Suggestions

| Pain Point | Possible Fix |
|------------|--------------|
| Global cycle | Track `generationCycle` per language in `AppPreferences` |
| Progression ends at cycle 2 | Add more levels (7, 8 letters) or introduce mixed-length batches |
| Monotonous batches | Allow a variable ratio per level (e.g., `listOf(4 to 5, 5 to 10, Pair(6, 7) to 10)`) |
| Silent difficulty jump | Show a "difficulty increased" message when the cycle advances |

---

## Framing Note

The system would be more honest if called **"difficulty ramp-up for onboarding"** rather than "leveling". If the intent is genuine ongoing progression, the levels should continue growing. If the intent is just to make onboarding fast and then stabilize, the current design is correct — but that intent should be documented explicitly in [Spec 15](../specs/15-simplified-level-generation.md).
