# Spec 12 — Chat IA Engagement

## Resumo

O Chat IA é o diferencial do Palabrita. Após cada jogo, o jogador pode conversar com a IA sobre a palavra, aprendendo etimologia, curiosidades e usos. Esta spec define o sistema de incentivos para que o jogador use o chat: XP bônus, sugestões contextuais, nudges na Home e badges de explorador.

## Context & Motivation

O Chat IA já existe (Spec 06), mas sem incentivos claros para uso, a maioria dos jogadores vai ignorá-lo. Jogos educacionais de sucesso (Duolingo, Quizlet) mostram que recompensas tangíveis por curiosidade aumentam engajamento. No Palabrita: **conhecimento = XP**.

## Fluxo de Entrada no Chat

### CTA Principal — ResultScreen

O Chat Card é o **CTA principal** do ResultScreen (vitória ou derrota). Deve ser o elemento mais proeminente após o resultado.

```
┌──────────────────────────────┐
│         🎉 Parabéns!         │
│    Você descobriu em 3/6     │
│    Palavra: GATOS            │
│                              │
│  ┌────────────────────────┐  │
│  │  💬 Explore "GATOS"    │  │  ← CTA principal
│  │                        │  │
│  │  🧬 Etimologia         │  │
│  │  🌎 Curiosidade        │  │
│  │  📝 Frases de exemplo  │  │
│  │                        │  │
│  │  +1 XP bônus ✨        │  │
│  │                        │  │
│  │  [ EXPLORAR AGORA ]    │  │
│  └────────────────────────┘  │
│                              │
│  [▶ Jogar de novo]           │
│  [📤 Compartilhar]           │
│                              │
└──────────────────────────────┘
```

**Hierarquia visual:**
1. Resultado (parabéns/derrota + palavra)
2. **Chat Card** (CTA principal, background `primaryContainer`, borda `primary`)
3. Jogar de novo (secundário)
4. Compartilhar (terciário)

### Entradas Alternativas

| Entrada | Contexto |
|---|---|
| ResultScreen Chat Card | CTA principal — após terminar qualquer jogo |
| HomeScreen Chat Nudge | Se daily completado sem chat |
| HomeScreen Daily Card | "💬 Explorar?" em daily completado sem chat |
| StatsScreen (futuro) | Palavras exploradas vs não exploradas |

## Sugestões Contextuais

Ao abrir o chat, o jogador vê **sugestões rápidas** baseadas na palavra e na categoria:

### Categorias de Sugestão

```kotlin
data class ChatSuggestion(
    val icon: String,       // emoji
    val label: String,      // texto curto
    val prompt: String,     // mensagem enviada ao LLM
    val category: SuggestionCategory,
)

enum class SuggestionCategory {
    ETYMOLOGY,      // 🧬 "De onde vem essa palavra?"
    CURIOSITY,      // 🌎 "Curiosidade sobre {palavra}"
    USAGE,          // 📝 "Frases com {palavra}"
    RELATED,        // 🔗 "Palavras relacionadas"
    CULTURAL,       // 🎭 "Expressões com {palavra}"
    CHALLENGE,      // 🧩 "Me desafie com outra palavra parecida"
}
```

### Geração de Sugestões

```kotlin
fun generateSuggestions(
    word: String,
    category: String,
    language: String,
): List<ChatSuggestion> {
    // Sempre incluir 3 sugestões fixas:
    val suggestions = mutableListOf(
        ChatSuggestion("🧬", "Etimologia", "De onde vem '$word'?", ETYMOLOGY),
        ChatSuggestion("🌎", "Curiosidade", "Me conte algo curioso sobre '$word'", CURIOSITY),
        ChatSuggestion("📝", "Exemplos", "Crie 3 frases criativas com '$word'", USAGE),
    )
    
    // Adicionar 1-2 sugestões baseadas na categoria:
    when (category.lowercase()) {
        "animal" -> suggestions.add(
            ChatSuggestion("🐾", "Habitat", "Onde vivem os '$word'?", CURIOSITY)
        )
        "comida" -> suggestions.add(
            ChatSuggestion("👨‍🍳", "Receita", "Como preparar '$word'?", CULTURAL)
        )
        // ... outras categorias
    }
    
    return suggestions.take(5)  // máximo 5 sugestões visíveis
}
```

### UI no Chat

```
┌──────────────────────────────┐
│  💬 Sobre "GATOS"            │
│                              │
│  Sugestões rápidas:          │
│  ┌──────┐ ┌──────┐ ┌──────┐ │
│  │🧬    │ │🌎    │ │📝    │ │
│  │Etimo-│ │Curio-│ │Exem- │ │
│  │logia │ │sidade│ │plos  │ │
│  └──────┘ └──────┘ └──────┘ │
│  ┌──────┐ ┌──────┐          │
│  │🐾    │ │🧩    │          │
│  │Habi- │ │Desa- │          │
│  │tat   │ │fio   │          │
│  └──────┘ └──────┘          │
│                              │
│  Ou digite sua pergunta...   │
│  ┌────────────────────┬────┐ │
│  │                    │ ➤  │ │
│  └────────────────────┴────┘ │
└──────────────────────────────┘
```

Tap em sugestão → envia o prompt automaticamente → IA responde → sugestões desaparecem (chat livre a partir daí).

## XP Bônus — "Bônus de Curiosidade"

### Regra

- Cada sessão de chat dá **+1 XP** ("Bônus de Curiosidade")
- Máximo 1 bônus por puzzle (não acumula se abrir o chat 2x para o mesmo puzzle)
- Requer mínimo **1 mensagem enviada** (abrir e fechar sem interagir não conta)
- Vale para dailies E free play

### Cálculo

```kotlin
fun calculateChatBonusXp(
    puzzleId: Long,
    messagesExchanged: Int,
    alreadyClaimed: Boolean,
): Int {
    if (alreadyClaimed) return 0
    if (messagesExchanged < 1) return 0
    return 1  // +1 XP fixo
}
```

### Feedback Visual

Ao ganhar o bônus, mostrar inline no chat:

```
┌──────────────────────────────┐
│  ✨ +1 XP — Bônus de         │
│     Curiosidade!              │
│                              │
│  "O conhecimento também é    │
│   recompensa."               │
└──────────────────────────────┘
```

Auto-dismiss após 3 segundos ou tap.

## Badges de Exploração

Badges complementam XP como incentivo visual de longo prazo.

### Definições

| Badge | Nome | Condição | Ícone |
|-------|------|----------|-------|
| Explorador | "Explorador" | Usou chat IA 10x | 🔍 |
| Linguista | "Linguista" | Usou chat IA 50x | 📚 |
| Etimólogo | "Etimólogo" | Perguntou sobre etimologia 25x | 🧬 |
| Curioso Insaciável | "Curioso Insaciável" | Usou chat IA 100x | 🌟 |
| Polímata | "Polímata" | Todas as categorias de sugestão usadas | 🎓 |

### Data Model

```kotlin
data class BadgeProgress(
    val badgeId: String,
    val currentCount: Int,
    val targetCount: Int,
    val earnedAt: Long?,  // null se não ganhou ainda
)
```

### Tracking

```kotlin
// Incrementar ao enviar mensagem no chat
fun trackChatInteraction(
    puzzleId: Long,
    suggestionCategory: SuggestionCategory?,
) {
    // Incrementar total de sessões de chat (para Explorador, Linguista, etc.)
    // Se usou sugestão: incrementar categoria específica (para Etimólogo, Polímata)
}
```

### Exibição

- Badges aparecem no perfil do jogador (StatsScreen)
- Badge novo → notificação inline no chat: "🏅 Novo badge: Explorador!"
- Badges não ganhos aparecem em cinza com progresso (ex: "🔍 7/10")

## Nudges — HomeScreen

### ChatNudge

Aparece na HomeScreen se:
- O jogador completou um daily mas **não** usou o chat para aquele puzzle
- Modo AI ativo

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

### UI do Nudge

```
┌──────────────────────────────┐
│  💬 Quer saber mais sobre    │  ← Nudge card
│     "GATOS"?                 │
│                              │
│  [Explorar agora]    [✕]    │
└──────────────────────────────┘
```

- Tap "Explorar agora" → navega para ChatRoute(puzzleId)
- Tap "✕" → dismiss (não aparece de novo para esse puzzle)
- Máximo 1 nudge por vez

### Daily Card — "💬 Explorar?"

Dentro do DailyChallengesCard, desafios completados sem chat mostram:

```
① ✅ Animais   3/6  · 💬 Explorar?
```

Tap em "💬 Explorar?" → navega para ChatRoute(puzzleId).

## Modo Light

No modo Light (sem IA):
- Chat IA não está disponível
- Chat Card no ResultScreen é substituído por **Card de Curiosidade Estática**:

```
┌──────────────────────────────┐
│  📖 Sobre "GATOS"            │
│                              │
│  "Gato" vem do latim         │
│  "cattus". Domesticados há   │
│  ~10.000 anos no Oriente.    │
│                              │
│  — Curiosidade do dia         │
└──────────────────────────────┘
```

- Curiosidade estática vem do campo `staticCuriosity` do puzzle (dataset estático)
- Sem XP bônus no modo Light (sem interação de chat)
- Badges de chat não são trackáveis no modo Light

## ChatViewModel — Mudanças

### State (novos campos)

```kotlin
data class ChatState(
    // ... campos existentes
    val suggestions: List<ChatSuggestion>,    // sugestões contextuais
    val bonusXpClaimed: Boolean,              // se já ganhou +1 XP nesse puzzle
    val newBadge: Badge?,                     // badge recém-desbloqueado (null se nenhum)
)
```

### Actions (novas)

```kotlin
sealed class ChatAction {
    // ... actions existentes
    data class SelectSuggestion(val suggestion: ChatSuggestion) : ChatAction()
    data object DismissBadgeNotification : ChatAction()
}
```

## Data Model — Mudanças

### GameSessionEntity (campo novo)

```kotlin
val chatExplored: Boolean = false  // true se jogador usou chat para esse puzzle
```

### ChatSessionEntity (nova)

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

### BadgeEntity (nova)

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

| Cenário | Comportamento |
|---|---|
| Jogador abre chat sem enviar mensagem | Sem bônus XP. `chatExplored` = false |
| Jogador envia 1 mensagem e fecha | +1 XP bônus. `chatExplored` = true |
| Jogador reabre chat do mesmo puzzle | Sugestões somem (já teve interação). Sem bônus duplo |
| Modo Light: tap no chat card | Mostra curiosidade estática inline (sem navegar) |
| IA offline / erro de geração | Mostra fallback estático + "IA indisponível no momento" |
| Puzzle sem categoria | Sugestões genéricas (3 fixas, sem extras por categoria) |
| Jogador ganha badge durante chat | Notificação inline, não interrompe o fluxo |

## Decisões

| Decisão | Escolha | Razão |
|---------|---------|-------|
| XP bônus por chat | +1 XP fixo | Simples, previsível, não quebra economia |
| Badges vs apenas XP | Ambos | XP = progressão, badges = colecionismo |
| Chat como CTA principal | Sim | Diferencial do app, deve ser proeminente |
| Sugestões desaparecem após 1ª | Sim | Guia entrada, libera chat para conversa livre |
| Nudge no Home | 1 por vez | Não poluir, gentil reminder |

## Out of Scope

- Chat com voz (futuro)
- Compartilhar curiosidade aprendida (futuro)
- Chat multiplayer / comparar respostas (futuro)
- Streak de chat (dias consecutivos usando chat — futuro)
- Recompensas cosméticas por badges (temas, avatares — futuro)

## Critérios de Aceite

- [ ] Chat Card é o CTA principal no ResultScreen (acima de "jogar de novo")
- [ ] Chat Card mostra 3+ sugestões contextuais com ícones
- [ ] Tap em sugestão envia mensagem automaticamente ao LLM
- [ ] Sugestões desaparecem após a 1ª interação
- [ ] +1 XP bônus ao enviar 1ª mensagem no chat (por puzzle)
- [ ] Bônus não duplica se reabrir chat do mesmo puzzle
- [ ] Feedback visual "✨ +1 XP — Bônus de Curiosidade" aparece no chat
- [ ] Badge "Explorador" desbloqueado após 10 sessões de chat
- [ ] Badge "Linguista" desbloqueado após 50 sessões de chat
- [ ] Notificação inline ao ganhar novo badge
- [ ] ChatNudge aparece no HomeScreen para daily completado sem chat
- [ ] ChatNudge não aparece em modo Light
- [ ] Daily card mostra "💬 Explorar?" para daily sem chat
- [ ] Modo Light mostra curiosidade estática no ResultScreen
- [ ] `chatExplored` salvo no GameSession
- [ ] ChatSessionEntity registra mensagens e sugestões usadas
- [ ] BadgeEntity persiste progresso entre sessões
