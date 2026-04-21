# Spec 05 — Game

## Resumo

A tela de jogo é o core do Palabrita. O jogador tenta descobrir a palavra do dia em até 6 tentativas, estilo Wordle. As letras recebem feedback visual por posição. O jogador pode revelar dicas progressivas. A dificuldade se adapta ao histórico do jogador.

## Mecânica do Jogo

### Regras Básicas

- O jogador tem **6 tentativas** para descobrir a palavra
- A palavra tem entre **5 e 8 letras** (dinâmico por puzzle)
- Cada tentativa deve ser uma palavra completa (com o mesmo número de letras)
- Após cada tentativa, cada letra recebe um feedback de cor:
  - � **Mint/Teal** (`#4ECDC4`): letra correta na posição correta
  - 🟧 **Amber/Gold** (`#FFB347`): letra existe na palavra mas está na posição errada
  - 🟥 **Coral** (`#FF6B6B`): letra não existe na palavra
- O jogador pode revelar **dicas** (máximo 5, progressivas da mais vaga à mais específica)
- **Vitória**: acertar a palavra em qualquer tentativa
- **Derrota**: usar todas as 6 tentativas sem acertar

**Regras de recompensa:**
- Vitória: ganha XP (varia com dificuldade, tentativas e dicas usadas)
- Derrota: não ganha XP, não perde nada
- Streak: conta **dias jogados consecutivamente**, independente de vitória/derrota. Jogou hoje = streak mantida.

### Algoritmo de Feedback de Letras

Para cada tentativa, o feedback é calculado assim:

```
1. Marcar todas as letras na posição correta como CORRECT (mint)
2. Para cada letra não marcada como CORRECT:
   a. Contar quantas vezes essa letra aparece na palavra-alvo
   b. Subtrair quantas vezes ela já foi marcada como CORRECT
   c. Se ainda há ocorrências restantes: PRESENT (amber)
   d. Se não: ABSENT (coral)
3. Processar da esquerda para a direita (para letras duplicadas)
```

Exemplo: palavra = "gatos", tentativa = "gagas"
- g[0] → CORRECT (mint)
- a[1] → CORRECT (mint)
- g[2] → ABSENT (coral — 'g' só aparece 1x e já foi usada)
- a[3] → ABSENT (coral — 'a' só aparece 1x e já foi usada)
- s[4] → CORRECT (mint)

### Seleção de Dificuldade (antes do jogo)

Antes de cada partida, o jogador escolhe a dificuldade:

```
┌──────────────────────────────┐
│ Escolha a dificuldade          │
│                              │
│  ● ⭐      Fácil        1 XP │
│  ○ ⭐⭐    Normal       2 XP │
│  ○ ⭐⭐⭐  Difícil      3 XP │
│  🔒 ⭐⭐⭐⭐ Desafiante  5 XP │
│  🔒 ⭐⭐⭐⭐⭐ Expert    8 XP │
│                              │
│  Recomendado: ⭐⭐ Normal       │
│                              │
│  [Jogar]                     │
└──────────────────────────────┘
```

**Regras:**
- Níveis disponíveis: até `maxUnlockedDifficulty + 1` (pode tentar 1 acima do que já desbloqueou)
- Níveis bloqueados (🔒): mostram "Desbloqueie vencendo no nível anterior"
- "Recomendado": sempre `currentDifficulty` (o nível em que a progressão automática colocou o jogador)
- Promoção automática continua funcionando — desbloqueia níveis, não força o jogador
- Rebaixamento automático só muda a recomendação, não bloqueia níveis já desbloqueados
- Novo jogador: só nível 1 disponível (nível 2 aparece com 🔒 mas clicando mostra "Vence 5 jogos no Fácil para desbloquear")
- O jogador pode jogar abaixo do nível recomendado (ganha menos XP, mas acelera streak bônus)
- Escolher acima do recomendado: mais risco de perder, mas mais XP se ganhar

### Seleção do Puzzle

```
1. Jogador escolhe dificuldade (chosenDifficulty)
2. Calcular wordLength via difficultyToWordLength(chosenDifficulty, wordSizePreference)
3. Buscar puzzle não jogado: PuzzleDao.getNextUnplayed(language, chosenDifficulty)
4. Se existe: usar esse puzzle
5. Se não existe (sem puzzles pré-gerados nessa dificuldade):
   a. Modo AI: gerar inline via PuzzleGenerator (mostrar loading)
   b. Modo Light: buscar do dataset estático pela dificuldade mais próxima
6. Marcar puzzle como "em jogo" (criar GameSessionEntity)
```

### Validação de Tentativa

Antes de aceitar uma tentativa:
- Deve ter exatamente o mesmo número de letras que a palavra-alvo
- Deve conter apenas caracteres `[a-z]`
- **NÃO** é necessário validar se é uma palavra real (V1 — sem dicionário local)

## UI — Compose

### Layout Geral

```
┌──────────────────────────────┐
│ Header: "Palabrita" + stats  │
├──────────────────────────────┤
│                              │
│     ┌─┬─┬─┬─┬─┬─┐          │
│     │ │ │ │ │ │ │  ← tentativa 1 (preenchida)
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← tentativa 2 (preenchida)
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← tentativa 3 (ativa)
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← vazia
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← vazia
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← vazia
│     └─┴─┴─┴─┴─┴─┘          │
│                              │
│  [💡 Dica (3/5 restantes)]   │
│                              │
│  ┌──────────────────────────┐│
│  │  Q W E R T Y U I O P    ││
│  │   A S D F G H J K L     ││
│  │  ⌫  Z X C V B N M  ↵   ││
│  └──────────────────────────┘│
└──────────────────────────────┘
```

### Componentes

**WordGrid**
- Rows: 6 (fixo — tentativas máximas)
- Columns: dinâmico (5-8, baseado no tamanho da palavra)
- Cada célula: `Box` com `Text` centralizado + background de cor
- Animação de flip ao revelar feedback (após confirmar tentativa)
- Animação de shake na row se tentativa inválida (tamanho errado)
- Animação de bounce/pop ao acertar (todas as células mint)

**GameKeyboard**
- Layout QWERTY (adaptado ao locale se necessário — V1 usa QWERTY padrão)
- Cada tecla mostra estado de cor baseado nas tentativas:
  - Sem uso: cor neutra
  - CORRECT em alguma tentativa: mint (`#4ECDC4`)
  - PRESENT em alguma tentativa: amber (`#FFB347`)
  - ABSENT em todas as tentativas: coral (`#FF6B6B`)
- Teclas especiais: Backspace (⌫) e Enter (↵)
- Teclas com tamanho acessível para touch

**HintButton**
- Floating ou fixo abaixo do grid
- Mostra: ícone de lâmpada + "Dica (X/5)"
- Ao clicar: revela a próxima dica (animação de slide-in de card)
- Dica revelada fica visível até o fim do jogo
- Se todas as 5 dicas reveladas: botão desabilitado

**HintCard**
- Card com a dica revelada
- Numeração: "Dica 1:", "Dica 2:", etc.
- Posição: entre o grid e o teclado (scrollável se necessário)
- Animação: fade-in + slide de baixo

### Tela de Vitória

```
┌──────────────────────────────┐
│                              │
│         🎉 Parabéns!         │
│                              │
│    Você descobriu em X/6     │
│                              │
│    Palavra: GATOS            │
│    Categoria: Animal         │
│                              │
│  [🔍 Explorar a palavra]     │  ← navega para Chat (se AI mode)
│  [📊 Ver estatísticas]       │
│  [📤 Compartilhar]           │
│                              │
│    Próximo puzzle em XX:XX   │
│                              │
└──────────────────────────────┘
```

- "Explorar a palavra" → navega para Chat screen (apenas em modo AI)
- Em modo Light: mostra card estático de curiosidade inline
- "Compartilhar" → gera grid de emojis (estilo Wordle)

### Tela de Derrota

```
┌──────────────────────────────┐
│                              │
│       😔 Não foi dessa vez   │
│                              │
│    A palavra era: GATOS      │
│    Categoria: Animal         │
│                              │
│  [🔍 Explorar a palavra]     │
│  [📊 Ver estatísticas]       │
│                              │
│    Próximo puzzle em XX:XX   │
│                              │
└──────────────────────────────┘
```

### Formato de Compartilhamento

```
Palabrita ⭐⭐⭐ — 4/6

🟥🟧🟥🟥🟥🟥
🟥🟥🟦🟥🟥🟥
🟦🟦🟦🟥🟦🟥
🟦🟦🟦🟦🟦🟦

A palavra era: GATOS
💡 2 dicas usadas · +4 XP
```

**Detalhes:**
- `⭐⭐⭐` = dificuldade escolhida (1-5 estrelas)
- A palavra é exibida no share — cada jogador tem uma palavra única (gerada por IA), não há risco de spoiler
- Emoji temático ao lado da palavra (derivado da categoria, opcional — fallback sem emoji)
- XP ganho no rodapé (incentiva o amigo a jogar em dificuldade alta)
- Em caso de derrota:

```
Palabrita ⭐⭐⭐ — X/6

🟥🟧🟥🟥🟥🟥
🟥🟥🟦🟥🟥🟥
🟥🟦🟦🟥🟦🟥
🟥🟧🟦🟦🟥🟥
🟦🟦🟦🟥🟦🟥
🟥🟦🟦🟦🟦🟥

A palavra era: GATOS
💡 3 dicas usadas
```

## Dificuldade Adaptativa

### Sistema de XP e Progressão

O jogador acumula XP ao ganhar jogos. XP nunca decresce. O tier (ranking) é derivado do XP total e nunca rebaixa — é recompensa, não punição. A **dificuldade**, por outro lado, pode subir e descer com base na performance.

### Cálculo de XP por Jogo

```kotlin
fun calculateXpForGame(
    won: Boolean,
    attempts: Int,
    difficulty: Int,
    currentStreak: Int,
    hintsUsed: Int
): Int {
    if (!won) return 0

    val baseXp = when (difficulty) {
        1 -> 1
        2 -> 2
        3 -> 3
        4 -> 5
        5 -> 8
        else -> 1
    }

    val attemptBonus = when (attempts) {
        1 -> 3
        2 -> 1
        else -> 0
    }

    val streakBonus = when {
        currentStreak >= 30 && currentStreak % 30 == 0 -> 20
        currentStreak >= 7 && currentStreak % 7 == 0 -> 5
        else -> 0
    }

    // Penalidade por dicas: -1 XP por dica usada (não pode zerar o base)
    val hintPenalty = hintsUsed

    return (baseXp + attemptBonus + streakBonus - hintPenalty).coerceAtLeast(1)
}
```

**Penalidade de dicas:**
- Cada dica usada reduz **1 XP** do total
- O XP mínimo por vitória é sempre **1** (nunca zero — o jogador ganhou, merece algo)
- Dicas não afetam streak bônus (bônus de streak é separado)

Exemplo: vitória no nível 3 (base 3 XP) + 1ª tentativa (+3) + 2 dicas usadas (-2) = **4 XP**
Exemplo: vitória no nível 1 (base 1 XP) + 5 dicas usadas (-5) = **1 XP** (mínimo)

### Tier (Ranking) — derivado do XP

```kotlin
enum class PlayerTier(val minXp: Int, val displayName: String) {
    NOVATO(0, "Novato"),
    CURIOSO(50, "Curioso"),
    ASTUTO(150, "Astuto"),
    SABIO(400, "Sábio"),
    EPICO(1000, "Épico"),
    LENDARIO(2500, "Lendário");

    companion object {
        fun fromXp(totalXp: Int): PlayerTier =
            entries.last { totalXp >= it.minXp }
    }
}
```

Tier **nunca desce**. Se o jogador para de jogar por meses e volta, mantém o tier.

### Promoção / Rebaixamento de Dificuldade

A dificuldade do próximo puzzle é ajustada automaticamente:

```kotlin
fun checkDifficultyProgression(stats: PlayerStats): Int {
    val current = stats.currentDifficulty
    val winsAtCurrent = stats.gamesWonByDifficulty[current] ?: 0
    val winRateAtCurrent = stats.winRateByDifficulty[current] ?: 0f
    val requiredWins = if (stats.currentStreak >= 7) 4 else 5

    // Promoção: ganhou N+ jogos no nível atual com winRate ≥ 70%
    if (winsAtCurrent >= requiredWins && winRateAtCurrent >= 0.70f && current < 5) {
        return current + 1
    }

    // Rebaixamento: perdeu 3 seguidos no nível atual
    if (stats.consecutiveLossesAtCurrent >= 3 && current > 1) {
        return current - 1
    }

    return current  // sem mudança
}
```

**Regras:**
- Promoção requer **5 vitórias** no nível atual com **winRate ≥ 70%** (4 vitórias se streak ≥ 7 dias)
- Rebaixamento após **3 derrotas seguidas** no nível atual
- Novo jogador começa no **nível 1**
- Ao promover/rebaixar: `consecutiveLossesAtCurrent` reseta para 0
- Ao promover: `gamesWonByDifficulty[novoNível]` começa a contar do 0 que já tinha (histórico preservado)

### Como Dificuldade Afeta o Puzzle

| Dificuldade | Tamanho | Tipo de Palavra | Estilo das Dicas |
|---|---|---|---|
| 1 (fácil) | 5 letras | Muito comum, cotidiana | Dicas diretas |
| 2 | 5-6 letras | Comum | Dicas claras |
| 3 (média) | 6-7 letras | Menos frequente | Dicas moderadas |
| 4 | 7-8 letras | Incomum | Dicas mais vagas |
| 5 (difícil) | 7-8 letras | Raro, técnico | Dicas abstratas |

```kotlin
fun difficultyToWordLength(difficulty: Int, wordSizePreference: String): IntRange {
    // Se o jogador escolheu um tamanho fixo nas configurações (tier Astuto+)
    return when (wordSizePreference) {
        "SHORT" -> 5..6
        "LONG" -> 7..9
        "EPIC" -> 8..10
        else -> when (difficulty) {  // "DEFAULT" — dinâmico por dificuldade
            1 -> 5..5
            2 -> 5..6
            3 -> 6..7
            4 -> 7..8
            5 -> 7..8
            else -> 5..6
        }
    }
}
```

Quando o jogador usa um range fixo, a dificuldade ainda afeta a **raridade da palavra** e o **estilo das dicas** (controlados pelo prompt), mas não o tamanho.

Dificuldade 4 e 5 têm mesmo range de letras, mas diferem na **raridade da palavra** (controlada pelo prompt: "uncommon" vs "rare/technical").

Esses parâmetros são passados no prompt do LLM como `min_length`, `max_length`, `target_difficulty`.

## GameViewModel

### State

```kotlin
data class GameState(
    val puzzle: Puzzle?,
    val chosenDifficulty: Int,
    val availableDifficulties: List<DifficultyOption>,
    val attempts: List<Attempt>,
    val currentInput: String,
    val revealedHints: List<String>,
    val keyboardState: Map<Char, LetterState>,
    val gameStatus: GameStatus,
    val isLoading: Boolean,
    val error: String?
)

data class DifficultyOption(
    val level: Int,
    val label: String,        // "Fácil", "Normal", etc.
    val baseXp: Int,
    val isUnlocked: Boolean,
    val isRecommended: Boolean
)

data class Attempt(
    val word: String,
    val feedback: List<LetterFeedback>
)

data class LetterFeedback(
    val letter: Char,
    val state: LetterState
)

enum class LetterState { CORRECT, PRESENT, ABSENT, UNUSED }
enum class GameStatus { CHOOSING_DIFFICULTY, PLAYING, WON, LOST, LOADING }
```

### Actions

```kotlin
sealed class GameAction {
    data class SelectDifficulty(val level: Int) : GameAction()
    data object StartGame : GameAction()
    data class TypeLetter(val letter: Char) : GameAction()
    data object DeleteLetter : GameAction()
    data object SubmitAttempt : GameAction()
    data object RevealHint : GameAction()
    data object ShareResult : GameAction()
    data object NavigateToChat : GameAction()
    data object NavigateToStats : GameAction()
    data object LoadNextPuzzle : GameAction()
}
```

## PuzzleGenerationWorker (WorkManager)

- **Trigger**: periodic diário (mínimo 15 min interval do WorkManager, mas setado para 24h)
- **Constraint**: device idle ou charging (para não impactar UX)
- **Lógica**:
  1. Checar `PuzzleDao.countUnplayed(language)`
  2. Se < 3: gerar 7 novos puzzles via `PuzzleGenerator`
  3. Se Light mode: não fazer nada (dataset estático é finito)
- **Retry**: se falhar, WorkManager faz retry com backoff exponencial

## Edge Cases

| Cenário | Comportamento |
|---|---|
| Usuário digita menos letras e aperta Enter | Shake animation na row + ignorar |
| Palavra duplicada (mesma tentativa 2x) | Permitir (V1 — sem restrição) |
| Todos os puzzles jogados (AI mode) | Mostrar loading, gerar inline via PuzzleGenerator |
| Todos os puzzles jogados (Light mode) | Mensagem "Aguarde atualização do app" |
| App fechado durante jogo | Salvar state em GameSessionEntity, restaurar ao reabrir |
| Rotação de tela | Compose handles automaticamente (ViewModel preserva state) |
| Teclado físico | Capturar key events e mapear para GameAction |

## Critérios de Aceite

- [ ] Grid renderiza corretamente para palavras de 5, 6, 7 e 8 letras
- [ ] Feedback de cores está correto para letras duplicadas (conforme algoritmo)
- [ ] Teclado atualiza cores corretamente após cada tentativa
- [ ] Animação de flip funciona ao revelar feedback
- [ ] Animação de shake funciona para tentativa inválida
- [ ] Dicas revelam progressivamente (1 a 5)
- [ ] Vitória detectada corretamente e navega para tela de resultado
- [ ] Derrota detectada corretamente após 6 tentativas
- [ ] Compartilhamento gera grid de emojis correto
- [ ] State persiste entre app kills (via GameSessionEntity)
- [ ] Dificuldade adaptativa muda baseado no histórico
- [ ] Seletor de dificuldade mostra níveis desbloqueados corretamente
- [ ] Níveis bloqueados não são selecionáveis
- [ ] Nível recomendado é destacado
- [ ] "Explorar a palavra" só aparece em modo AI
- [ ] WorkManager gera puzzles em background quando estoque < 3
