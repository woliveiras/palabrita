# Spec 12 — AI Chat Engagement

## Summary

The AI Chat is Palabrita's differentiator. After each game, the player can chat with the AI about the word, learning etymology, trivia, and usage. This spec defines the incentive system to encourage players to use the chat: bonus XP, contextual suggestions, Home nudges, and explorer badges.

## Context & Motivation

The AI Chat already exists (Spec 06), but without clear incentives for use, most players will ignore it. Successful educational games (Duolingo, Quizlet) show that tangible rewards for curiosity increase engagement. In Palabrita: **knowledge = XP**.

## Chat Entry Flow

### Main CTA — ResultScreen

The Chat Card is the **main CTA** on the ResultScreen (win or loss). It should be the most prominent element after the result.

```
┌──────────────────────────────┐
│         🎉 Congratulations!  │
│    You discovered it in 3/6  │
│    Word: GATOS               │
│                              │
│  ┌────────────────────────┐  │
│  │  💬 Explore "GATOS"    │  │  ← Main CTA
│  │                        │  │
│  │  🧬 Etymology          │  │
│  │  🌎 Trivia             │  │
│  │  📝 Example sentences  │  │
│  │                        │  │
│  │  +1 bonus XP ✨        │  │
│  │                        │  │
│  │  [ EXPLORE NOW ]       │  │
│  └────────────────────────┘  │
│                              │
│  [▶ Play again]              │
│  [📤 Share]                  │
│                              │
└──────────────────────────────┘
```

**Visual hierarchy:**
1. Result (congratulations/defeat + word)
2. **Chat Card** (main CTA, background `primaryContainer`, border `primary`)
3. Play again (secondary)
4. Share (tertiary)

### Alternative Entry Points

| Entry Point | Context |
|---|---|
| ResultScreen Chat Card | Main CTA — after finishing any game |
| HomeScreen Chat Nudge | If daily completed without chat |
| HomeScreen Daily Card | "💬 Explore?" on a completed daily without chat |
| StatsScreen (future) | Explored vs unexplored words |

## Contextual Suggestions

When opening the chat, the player sees **quick suggestions** based on the word and category:

### Suggestion Categories

```kotlin
data class ChatSuggestion(
    val icon: String,       // emoji
    val label: String,      // short text
    val prompt: String,     // message sent to LLM
    val category: SuggestionCategory,
)

enum class SuggestionCategory {
    ETYMOLOGY,      // 🧬 "Where does this word come from?"
    CURIOSITY,      // 🌎 "Trivia about {word}"
    USAGE,          // 📝 "Sentences with {word}"
    RELATED,        // 🔗 "Related words"
    CULTURAL,       // 🎭 "Expressions with {word}"
    CHALLENGE,      // 🧩 "Challenge me with another similar word"
}
```

### Suggestion Generation

```kotlin
fun generateSuggestions(
    word: String,
    category: String,
    language: String,
): List<ChatSuggestion> {
    // Always include 3 fixed suggestions:
    val suggestions = mutableListOf(
        ChatSuggestion("🧬", "Etymology", "Where does '$word' come from?", ETYMOLOGY),
        ChatSuggestion("🌎", "Trivia", "Tell me something curious about '$word'", CURIOSITY),
        ChatSuggestion("📝", "Examples", "Create 3 creative sentences with '$word'", USAGE),
    )
    
    // Add 1-2 suggestions based on category:
    when (category.lowercase()) {
        "animal" -> suggestions.add(
            ChatSuggestion("🐾", "Habitat", "Where do '$word' live?", CURIOSITY)
        )
        "comida" -> suggestions.add(
            ChatSuggestion("👨‍🍳", "Recipe", "How to prepare '$word'?", CULTURAL)
        )
        // ... other categories
    }
    
    return suggestions.take(5)  // maximum 5 visible suggestions
}
```

### Chat UI

```
┌──────────────────────────────┐
│  💬 About "GATOS"            │
│                              │
│  Quick suggestions:          │
│  ┌──────┐ ┌──────┐ ┌──────┐ │
│  │🧬    │ │🌎    │ │📝    │ │
│  │Etym- │ │Triv- │ │Exam- │ │
│  │ology │ │ia    │ │ples  │ │
│  └──────┘ └──────┘ └──────┘ │
│  ┌──────┐ ┌──────┐          │
│  │🐾    │ │🧩    │          │
│  │Habi- │ │Chal- │          │
│  │tat   │ │lenge │          │
│  └──────┘ └──────┘          │
│                              │
│  Or type your question...    │
│  ┌────────────────────┬────┐ │
│  │                    │ ➤  │ │
│  └────────────────────┴────┘ │
└──────────────────────────────┘
```

Tap on a suggestion → automatically sends the prompt → AI responds → suggestions disappear (free chat from that point on).

## Bonus XP — "Curiosity Bonus"

### Rule

- Each chat session grants **+1 XP** ("Curiosity Bonus")
- Maximum 1 bonus per puzzle (does not stack if the chat is opened twice for the same puzzle)
- Requires a minimum of **1 message sent** (opening and closing without interacting does not count)
- Applies to dailies AND free play

### Calculation

```kotlin
fun calculateChatBonusXp(
    puzzleId: Long,
    messagesExchanged: Int,
    alreadyClaimed: Boolean,
): Int {
    if (alreadyClaimed) return 0
    if (messagesExchanged < 1) return 0
    return 1  // fixed +1 XP
}
```

### Visual Feedback

When earning the bonus, show inline in the chat:

```
┌──────────────────────────────┐
│  ✨ +1 XP — Curiosity        │
│     Bonus!                   │
│                              │
│  "Knowledge is also          │
│   a reward."                 │
└──────────────────────────────┘
```

Auto-dismiss after 3 seconds or on tap.

## Exploration Badges

Badges complement XP as a long-term visual incentive.

### Definitions

| Badge | Name | Condition | Icon |
|-------|------|-----------|------|
| Explorer | "Explorer" | Used AI chat 10x | 🔍 |
| Linguist | "Linguist" | Used AI chat 50x | 📚 |
| Etymologist | "Etymologist" | Asked about etymology 25x | 🧬 |
| Insatiably Curious | "Insatiably Curious" | Used AI chat 100x | 🌟 |
| Polymath | "Polymath" | All suggestion categories used | 🎓 |

### Data Model

```kotlin
data class BadgeProgress(
    val badgeId: String,
    val currentCount: Int,
    val targetCount: Int,
    val earnedAt: Long?,  // null if not yet earned
)
```

### Tracking

```kotlin
// Increment when sending a message in chat
fun trackChatInteraction(
    puzzleId: Long,
    suggestionCategory: SuggestionCategory?,
) {
    // Increment total chat sessions (for Explorer, Linguist, etc.)
    // If suggestion used: increment specific category (for Etymologist, Polymath)
}
```

### Display

- Badges appear on the player profile (StatsScreen)
- New badge → inline notification in chat: "🏅 New badge: Explorer!"
- Unearned badges appear in grey with progress (e.g. "🔍 7/10")

## Nudges — HomeScreen

### ChatNudge

Appears on the HomeScreen if:
- The player completed a daily but did **not** use the chat for that puzzle
- AI mode is active

```kotlin
data class ChatNudge(
    val word: String,
    val puzzleId: Long,
)

fun shouldShowChatNudge(
    dailyChallenges: List<DailyChallenge>,
    operatingMode: OperatingMode,
): ChatNudge? {
    if (operatingMode == OperatingMode.LIGHT) return null
    
    val unexplored = dailyChallenges
        .filter { it.state == COMPLETED && it.result?.chatExplored == false }
        .firstOrNull()
        ?: return null
    
    return ChatNudge(
        word = unexplored.word,
        puzzleId = unexplored.puzzleId!!,
    )
}
```

### Nudge UI

```
┌──────────────────────────────┐
│  💬 Want to learn more       │  ← Nudge card
│     about "GATOS"?           │
│                              │
│  [Explore now]       [✕]    │
└──────────────────────────────┘
```

- Tap "Explore now" → navigates to ChatRoute(puzzleId)
- Tap "✕" → dismiss (does not appear again for that puzzle)
- Maximum 1 nudge at a time

### Daily Card — "💬 Explore?"

Inside the DailyChallengesCard, completed challenges without chat show:

```
① ✅ Animals   3/6  · 💬 Explore?
```

Tap "💬 Explore?" → navigates to ChatRoute(puzzleId).

## Light Mode

In Light mode (no AI):
- AI Chat is not available
- Chat Card on ResultScreen is replaced by a **Static Curiosity Card**:

```
┌──────────────────────────────┐
│  📖 About "GATOS"            │
│                              │
│  "Gato" comes from the Latin │
│  "cattus". Domesticated ~    │
│  10,000 years ago in the     │
│  Middle East.                │
│                              │
│  — Fact of the day           │
└──────────────────────────────┘
```

- Static curiosity comes from the `staticCuriosity` field of the puzzle (static dataset)
- No bonus XP in Light mode (no chat interaction)
- Chat badges are not trackable in Light mode

## ChatViewModel — Changes

### State (new fields)

```kotlin
data class ChatState(
    // ... existing fields
    val suggestions: List<ChatSuggestion>,    // contextual suggestions
    val bonusXpClaimed: Boolean,              // whether +1 XP was already claimed for this puzzle
    val newBadge: Badge?,                     // newly unlocked badge (null if none)
)
```

### Actions (new)

```kotlin
sealed class ChatAction {
    // ... existing actions
    data class SelectSuggestion(val suggestion: ChatSuggestion) : ChatAction()
    data object DismissBadgeNotification : ChatAction()
}
```

## Data Model — Changes

### GameSessionEntity (new field)

```kotlin
val chatExplored: Boolean = false  // true if player used chat for this puzzle
```

### ChatSessionEntity (new)

```kotlin
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleId: Long,
    val gameSessionId: Long,
    val messagesCount: Int,
    val suggestionsUsed: String,  // JSON array: ["ETYMOLOGY", "CURIOSITY"]
    val bonusXpClaimed: Boolean,
    val startedAt: Long,
    val endedAt: Long?,
)
```

### BadgeEntity (new)

```kotlin
@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val badgeId: String,
    val currentCount: Int,
    val targetCount: Int,
    val earnedAt: Long?,
)
```

## Edge Cases

| Scenario | Behavior |
|---|---|
| Player opens chat without sending a message | No XP bonus. `chatExplored` = false |
| Player sends 1 message and closes | +1 XP bonus. `chatExplored` = true |
| Player reopens chat for the same puzzle | Suggestions are gone (already interacted). No double bonus |
| Light mode: tap on chat card | Shows static curiosity inline (no navigation) |
| AI offline / generation error | Shows static fallback + "AI unavailable at the moment" |
| Puzzle without category | Generic suggestions (3 fixed, no category extras) |
| Player earns badge during chat | Inline notification, does not interrupt the flow |

## Decisions

| Decision | Choice | Reason |
|---------|---------|-------|
| Bonus XP per chat | Fixed +1 XP | Simple, predictable, does not break economy |
| Badges vs XP only | Both | XP = progression, badges = collectibles |
| Chat as main CTA | Yes | App differentiator, should be prominent |
| Suggestions disappear after 1st | Yes | Guides entry, frees chat for open conversation |
| Home nudge | 1 at a time | Not intrusive, gentle reminder |

## Out of Scope

- Voice chat (future)
- Sharing learned curiosity (future)
- Multiplayer chat / compare answers (future)
- Chat streak (consecutive days using chat — future)
- Cosmetic rewards for badges (themes, avatars — future)

## Acceptance Criteria

- [ ] Chat Card is the main CTA on ResultScreen (above "play again")
- [ ] Chat Card shows 3+ contextual suggestions with icons
- [ ] Tapping a suggestion automatically sends the message to the LLM
- [ ] Suggestions disappear after the 1st interaction
- [ ] +1 XP bonus when sending 1st message in chat (per puzzle)
- [ ] Bonus does not duplicate if chat is reopened for the same puzzle
- [ ] Visual feedback "✨ +1 XP — Curiosity Bonus" appears in chat
- [ ] "Explorer" badge unlocked after 10 chat sessions
- [ ] "Linguist" badge unlocked after 50 chat sessions
- [ ] Inline notification when a new badge is earned
- [ ] ChatNudge appears on HomeScreen for a completed daily without chat
- [ ] ChatNudge does not appear in Light mode
- [ ] Daily card shows "💬 Explore?" for daily without chat
- [ ] Light mode shows static curiosity on ResultScreen
- [ ] `chatExplored` saved in GameSession
- [ ] ChatSessionEntity records messages and suggestions used
- [ ] BadgeEntity persists progress across sessions
