# Spec 06 — Post-Guess Chat

## Summary

After the player discovers (or fails to discover) the word, they can explore the word via chat with the local LLM. The chat is educational: etymology, fun facts, usage in sentences, synonyms, translations. It works only in AI mode. In Light mode, a static card replaces the chat.

## Flow

```
Result screen (win or loss)
    │
    ├── AI Mode ──→ "Explore the word" button ──→ Chat screen
    │
    └── Light Mode ──→ Static curiosity card (inline on result screen)
```

## Chat Screen (AI Mode)

### Layout

```
┌──────────────────────────────┐
│ ← Back      Explore: GATOS  │
├──────────────────────────────┤
│                              │
│  🤖 "Gatos" é uma palavra    │
│  fascinante! Vem do latim    │
│  tardio "cattus"...          │
│                              │
│  👤 Where does this word come from? │
│                              │
│  🤖 The word "gatos" has     │
│  its origin in late Latin... │
│  ████████▎ (typing)          │
│                              │
│  ─── 2 of 10 messages ───   │
│                              │
├──────────────────────────────┤
│  [    Type your question   ] │
│  [         Send ➤          ] │
└──────────────────────────────┘
```

### Behavior

**Automatic initial message:**
- When opening the chat, the LLM automatically sends a first message about the word
- Internal prompt (not visible to the user): "Tell an interesting fact about the word '{word}'"
- Streaming: tokens appear one by one with a typing indicator

**User messages:**
- Free text input
- Send button (or Enter on keyboard)
- Input disabled while model responds (streaming)
- Input disabled when limit is reached

**Response streaming:**
- Tokens appear progressively (via `Flow<String>`)
- Typing indicator (3 animated dots) before the first token
- Automatic scroll to bottom as tokens arrive

**Message limit:**
- Maximum 10 USER messages per session (model responses do not count)
- Visual indicator: "X of 10 messages"
- When limit is reached:
  - Input disabled
  - Message: "You have reached the question limit for this word. Play again tomorrow to explore another!"
  - Button "Back to game"

**Persistence:**
- All messages saved in `ChatMessageEntity`
- If the user returns to the result screen and reopens the chat: previous conversation is restored
- History only exists for the current puzzle (does not load conversations from previous puzzles)

### System Prompt

**Gemma 4 (native system role):**

```
System: You are an educational assistant for the word game Palabrita.
The player just discovered the word "{word}" (category: {category}).

Your role:
- Answer questions about the word in an educational and engaging way
- Cover: origin/etymology, fun facts, everyday usage in sentences, synonyms, translations to other languages
- Keep responses short (3 paragraphs max)
- Use accessible, enthusiastic language
- Always respond in {language}
- Do not make up facts — if unsure, say so

You must NOT:
- Generate offensive content
- Go off-topic from the word
- Give overly long responses
```

**Gemma 3 (prepend to first user message):**

```
[Context: You are an educational assistant. The player discovered the word "{word}" (category: {category}). Answer about: origin, etymology, fun facts, usage in sentences, synonyms, translations. Keep responses short (3 paragraphs max). Always respond in {language}.]

{user_message}
```

### Suggested Questions

Below the input, show suggested chips (disappear after the first manual message):

- "Where does this word come from?"
- "Give me synonyms"
- "How to use it in a sentence?"
- "How do you say it in English?"
- "Any fun facts?"

Clicking a chip: fills the input and sends automatically.

## Static Card (Light Mode)

When the user is in Light mode, chat is not available. Instead, the result screen shows an inline card:

```
┌──────────────────────────────┐
│ 💡 Fun Fact                  │
│                              │
│ Category: Animal             │
│                              │
│ A palavra "gatos" vem do     │
│ latim tardio "cattus".       │
│ Presente em 90% dos lares    │
│ brasileiros como animal de   │
│ estimação.                   │
│                              │
│ [Activate AI to explore      │
│  more about words →]         │
└──────────────────────────────┘
```

- Content comes from the `curiosity` field of the static dataset
- If `curiosity` is empty: show only category
- "Activate AI" link → navigates to Settings (model switching)

## ChatViewModel

### State

```kotlin
data class ChatState(
    val word: String,
    val category: String,
    val language: String,
    val messages: List<ChatMessage>,
    val currentInput: String,
    val isModelResponding: Boolean,
    val userMessageCount: Int,
    val maxMessages: Int = 10,
    val isAtLimit: Boolean,
    val error: String?,
    val suggestionsVisible: Boolean
)

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false  // true while tokens are arriving
)

enum class MessageRole { USER, MODEL }
```

### Actions

```kotlin
sealed class ChatAction {
    data class UpdateInput(val text: String) : ChatAction()
    data object SendMessage : ChatAction()
    data class SelectSuggestion(val suggestion: String) : ChatAction()
    data object GoBack : ChatAction()
}
```

## Edge Cases

| Scenario | Behavior |
|---|---|
| LLM takes too long (>30s) to respond | Show timeout message + retry button |
| LLM generates offensive response | V1: we do not filter (Gemma models already have safety filters). V2: post-filter |
| LLM generates response in wrong language | V1: accept (prompt instructs language, but it is not guaranteed) |
| App closed during streaming | Save tokens received so far as partial message |
| User sends empty message | Ignore (button disabled if input is empty) |
| Engine not initialized | Show loading + initialize engine, then continue |
| Very long conversation (many tokens) | 10-message limit controls this; Gemma 4 context is 128K |

## Acceptance Criteria

- [ ] Chat opens with automatic initial message from the model
- [ ] Token streaming works (tokens appear progressively)
- [ ] Typing indicator appears before the first token
- [ ] Automatic scroll follows the response
- [ ] 10-message limit is respected
- [ ] Input disabled while model is responding
- [ ] Input disabled when limit is reached
- [ ] Suggested questions appear and work
- [ ] Messages persist if user exits and returns to chat
- [ ] Light mode shows static card instead of chat
- [ ] "Activate AI" link in static card navigates to Settings
- [ ] System prompt includes correct word and category
