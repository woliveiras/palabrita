# Spec 11 — Daily Challenges

## Resumo

O jogador recebe 3 desafios diários com dificuldade progressiva (fácil → normal → difícil). Completar desafios desbloqueia o próximo, gera XP com bônus de 2x, e manter streak exige apenas 1 jogo por dia. Puzzles são únicos por jogador (IA local), não determinísticos.

## Context & Motivation

Sem desafios diários, o app não cria hábito. O Wordle provou que escassez + ritual = engajamento. Mas 1 puzzle por dia limita interação com o Chat IA (diferencial do Palabrita). Com 3 desafios: o jogador tem 3 oportunidades de explorar palavras com a IA, a dificuldade escala naturalmente dentro do dia, e o bônus de completude incentiva sessões mais longas.

## Mecânica

### 3 Desafios por Dia

| Desafio | Dificuldade | Desbloqueio |
|---------|-------------|-------------|
| ① | tier atual - 1 (mín 1) | Sempre disponível |
| ② | tier atual | Completar ① |
| ③ | tier atual + 1 (máx 5) | Completar ② |

**Onde "tier atual" = `currentDifficulty` do `PlayerStats`** (dificuldade adaptativa existente, spec 05).

Exemplos:
- Jogador nível 1: desafios ⭐/⭐/⭐⭐
- Jogador nível 3: desafios ⭐⭐/⭐⭐⭐/⭐⭐⭐⭐
- Jogador nível 5: desafios ⭐⭐⭐⭐/⭐⭐⭐⭐⭐/⭐⭐⭐⭐⭐

### Seleção de Puzzles

```kotlin
suspend fun selectDailyPuzzles(
    language: String,
    currentDifficulty: Int,
    date: LocalDate,
): List<Puzzle> {
    val difficulties = listOf(
        (currentDifficulty - 1).coerceAtLeast(1),
        currentDifficulty,
        (currentDifficulty + 1).coerceAtMost(5),
    )
    
    return difficulties.map { diff ->
        puzzleRepository.getNextUnplayed(language, diff)
            ?: puzzleGenerator.generateInline(language, diff)  // fallback
    }
}
```

- Puzzles são **únicos por jogador** (cada um recebe puzzles diferentes da IA ou do banco estático)
- Se não há puzzles no banco para a dificuldade, gera inline (AI mode) ou busca dificuldade adjacente (Light mode)
- Os 3 puzzles são selecionados ao acessar o primeiro daily do dia (lazy — não pré-seleciona à meia-noite)

### Desbloqueio Sequencial

```
Desafio 1: AVAILABLE desde o início do dia
Desafio 2: LOCKED → AVAILABLE quando desafio 1 completado (win ou loss)
Desafio 3: LOCKED → AVAILABLE quando desafio 2 completado (win ou loss)
```

**Completar = finalizar o jogo** (win ou loss). Não é necessário vencer para desbloquear o próximo. O objetivo é engajar, não frustrar.

### Fluxo (sem DifficultyPicker)

```
HomeScreen → tap "JOGAR #N" → PlayingScreen (puzzle pré-selecionado, dificuldade automática)
                                    │
                                    ▼
                              ResultScreen → Chat IA → Home
```

Não passa pelo DifficultyPicker. A dificuldade é automática (progressiva).

## XP e Recompensas

### XP por Daily Challenge

```kotlin
fun calculateDailyXp(
    won: Boolean,
    attempts: Int,
    difficulty: Int,
    hintsUsed: Int,
    currentStreak: Int,
): Int {
    val baseXp = calculateXpForGame(won, attempts, difficulty, currentStreak, hintsUsed)
    return baseXp * 2  // Bônus 2x para dailies
}
```

- Dailies dão **2x XP** comparado ao mesmo jogo no free play
- O bônus de streak (spec 05) se aplica normalmente

### Bônus de Completude (3/3)

```kotlin
fun calculateCompletionBonus(
    dailyResults: List<DailyChallengeResult>,
): Int {
    if (dailyResults.size < 3 || !dailyResults.all { it.completed }) return 0
    
    val winsCount = dailyResults.count { it.won }
    return when (winsCount) {
        3 -> 5   // Varreu: +5 XP bônus
        2 -> 3   // Quase: +3 XP bônus
        1 -> 1   // Persistente: +1 XP bônus
        0 -> 1   // Dedicado: +1 XP (participou dos 3)
        else -> 0
    }
}
```

- Completar os 3 (win ou loss) = bônus extra
- Quanto mais vitórias nos 3, maior o bônus
- Mesmo perdendo os 3, ganha +1 XP bônus por ter tentado todos

### Resumo de XP Diário Máximo

| Fonte | XP |
|-------|-----|
| Daily 1 (fácil, vitória 1ª) | (1+3) × 2 = 8 |
| Daily 2 (normal, vitória 1ª) | (2+3) × 2 = 10 |
| Daily 3 (difícil, vitória 1ª) | (3+3) × 2 = 12 |
| Bônus completude (3 wins) | 5 |
| Chat IA bônus (3 sessões) | 3 |
| **Máximo teórico/dia** | **38 XP** |

Na prática, ~15-20 XP/dia é mais realista (nem sempre 1ª tentativa).

## Streak

### Regras

- **Streak incrementa** quando o jogador **finaliza** (win ou loss) pelo menos **1 daily** no dia
- **Streak reseta** se o jogador não finalizar nenhum daily em um dia
- **Free play NÃO conta** para streak
- O jogador NÃO precisa vencer — basta participar

### Cálculo

```kotlin
fun updateStreak(stats: PlayerStats, now: LocalDate): PlayerStats {
    val lastPlayed = stats.lastPlayedAt?.toLocalDate()
    
    return when {
        lastPlayed == now -> stats // Já jogou hoje, sem mudança
        lastPlayed == now.minusDays(1) -> stats.copy(
            currentStreak = stats.currentStreak + 1,
            maxStreak = maxOf(stats.maxStreak, stats.currentStreak + 1),
            lastPlayedAt = now.toEpochMillis(),
        )
        else -> stats.copy(
            currentStreak = 1, // Reset, novo streak começa
            lastPlayedAt = now.toEpochMillis(),
        )
    }
}
```

### Streak Milestones

| Dias | Marco | Bônus |
|------|-------|-------|
| 7 | 🔥 1 semana | +5 XP |
| 30 | 🔥🔥 1 mês | +20 XP |
| 100 | 🔥🔥🔥 100 dias | +50 XP (novo!) |
| 365 | 🏆 1 ano | +100 XP + badge "Lendário" |

## Compartilhamento

Formato (puzzles únicos por jogador, sem número compartilhado):

```
Palabrita 🔥12 · Astuto · 350 XP

Desafio 1 ⭐ — 3/6
🟩🟩🟨⬜⬜
🟩🟩🟩⬜🟩
🟩🟩🟩🟩🟩

Desafio 2 ⭐⭐ — 4/6
🟩🟨🟨⬜⬜⬜
🟩🟩🟨⬜🟩⬜
🟩🟩🟩⬜🟩🟩
🟩🟩🟩🟩🟩🟩

💡 1 dica usada · +22 XP hoje
```

Destaques: streak, tier e XP (identidade do jogador, não do puzzle).

## Data Model — Mudanças

### GameSessionEntity (campos novos)

```kotlin
val dailyChallengeIndex: Int?    // 0, 1, 2 = daily; null = free play
val dailyChallengeDate: String?  // "2026-04-21" (ISO date, null = free play)
```

### PlayerStatsEntity (campo novo)

```kotlin
val lastDailyDate: String?       // "2026-04-21" — última data com daily finalizado
```

Isso separa `lastPlayedAt` (qualquer jogo) de `lastDailyDate` (daily específico para streak).

## Reset Diário

- Os dailies resetam à **meia-noite local** (`LocalDate.now()`)
- Ao acessar o HomeScreen, verifica se `today != lastDailyDate` → novos dailies
- Puzzles dos dailies anteriores não completados são liberados de volta ao banco
- Se o jogador está jogando um daily às 23:59 e termina às 00:01: o jogo conta para o dia em que **começou**

## Edge Cases

| Cenário | Comportamento |
|---|---|
| Jogador completa daily 1, fecha app, volta amanhã | Daily 2 e 3 de ontem não completados; novos dailies do novo dia |
| Jogador no tier 1, daily 1 seria tier 0 | `coerceAtLeast(1)` — todos os 3 são tier 1 |
| Jogador no tier 5, daily 3 seria tier 6 | `coerceAtMost(5)` — daily 3 é tier 5 |
| Sem puzzles para a dificuldade | AI mode: gera inline. Light mode: busca dificuldade adjacente |
| Jogador perde todos os 3 dailies | Streak mantido (participou). XP = 0 dos jogos + 1 bônus completude |
| Free play não afeta streak | Correto — só dailies contam |
| App kill durante daily | GameSession salva, restaura ao reabrir |

## Decisões

| Decisão | Escolha | Razão |
|---------|---------|-------|
| Puzzles determinísticos | Não | Cada jogador tem experiência única (IA local) |
| Desbloquear próximo requer vitória | Não | Completar (win/loss) basta. Engajamento > frustração |
| Daily sem DifficultyPicker | Sim | Dificuldade automática simplifica fluxo |
| Streak conta loss | Sim | Jogou = mantém streak. Recompensa participação |
| 3 dailies (não 1) | Sim | 3x oportunidades de Chat IA + escalada de dificuldade |

## Out of Scope

- Dailies determinísticos / iguais para todos (decidido: não)
- Leaderboard comparando dailies entre amigos (futuro)
- Notificação push "Seu streak está em risco!" (futuro)
- Replay de dailies de dias anteriores (futuro)

## Critérios de Aceite

- [ ] 3 desafios diários aparecem no HomeScreen com estado correto
- [ ] Dificuldade do daily 1 = `currentDifficulty - 1` (mín 1)
- [ ] Dificuldade do daily 2 = `currentDifficulty`
- [ ] Dificuldade do daily 3 = `currentDifficulty + 1` (máx 5)
- [ ] Daily 2 desbloqueia ao completar daily 1 (win ou loss)
- [ ] Daily 3 desbloqueia ao completar daily 2 (win ou loss)
- [ ] Tap em daily navega direto para PlayingScreen (sem DifficultyPicker)
- [ ] Daily dá 2x XP comparado ao free play
- [ ] Completar 3/3 dailies dá bônus extra de XP
- [ ] Streak incrementa ao finalizar 1º daily do dia
- [ ] Streak NÃO incrementa com free play
- [ ] Streak reseta se nenhum daily jogado no dia anterior
- [ ] Dailies resetam à meia-noite local
- [ ] `dailyChallengeIndex` e `dailyChallengeDate` salvos no GameSession
- [ ] Compartilhamento mostra streak + tier + XP (não número de puzzle)
- [ ] Bônus de streak aos 7, 30, 100 e 365 dias funciona
- [ ] Puzzles do daily são únicos por jogador
- [ ] Daily iniciado antes da meia-noite conta para o dia em que começou
