# Spec 10 — HomeScreen

## Resumo

A HomeScreen é o hub central do Palabrita. Substitui o fluxo atual (DifficultyPicker como tela inicial) por um hub que mostra streak, desafios diários, modo livre, stats rápidos e indicador de geração de puzzles. Toda navegação parte daqui e retorna aqui.

## Context & Motivation

Hoje o app abre direto no DifficultyPicker — sem contexto, sem sensação de progresso, sem ritual. Jogos de sucesso (Wordle, Words of Wonders, Wordscapes) usam uma tela de hub que mostra o estado do jogador antes de iniciar. A HomeScreen resolve isso e cria o ritual diário: abrir → ver streak → jogar → explorar palavras → compartilhar.

## Navegação

```
App Launch
  ├── 1ª vez → Onboarding → HomeScreen
  └── Já completou → HomeScreen

HomeScreen
  ├── Daily Challenges → PlayingScreen (sem DifficultyPicker)
  ├── Modo Livre → DifficultyPicker → PlayingScreen
  ├── Bottom Nav: Stats → StatsScreen
  └── Bottom Nav: Mais → SettingsScreen
```

A HomeScreen é a `startDestination` após o onboarding. O `GameRoute` atual se torna `HomeRoute`. O `DifficultyPicker` se torna acessível apenas via Modo Livre.

## Layout

```
┌──────────────────────────────────┐
│    P A L A B R I T A             │  ← TopBar: logo centralizado
├──────────────────────────────────┤
│                                  │
│  ┌────────────────────────────┐  │
│  │  🔥 7 dias de streak!     │  │  ← Streak Card
│  │  ████████░░  Próximo: 🏆  │  │     Barra visual, próximo marco
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │  ⭐ DESAFIOS DO DIA (1/3)  │  │  ← Daily Challenges Card
│  │                            │  │
│  │  ① ✅ Animais   3/6       │  │
│  │  ② 🔓 Comida    ⭐⭐       │  │
│  │  ③ 🔒 ???       ⭐⭐⭐     │  │
│  │                            │  │
│  │       [ JOGAR #2 ]         │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │  🎲 MODO LIVRE             │  │  ← Free Play Card
│  │  Escolha dificuldade e     │  │
│  │  jogue quantas vezes       │  │
│  │  quiser                    │  │
│  │       [ JOGAR ]            │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌──────────┬──────────┐        │
│  │ 42 jogos │ 87% wins │        │  ← Quick Stats
│  │ 🏆 Astuto│ 350 XP   │        │
│  └──────────┴──────────┘        │
│                                  │
│  ┌────────────────────────────┐  │  ← Generation Indicator (se ativo)
│  │  ⟳ Gerando novos puzzles… │  │
│  └────────────────────────────┘  │
│                                  │
├──────────────────────────────────┤
│  🏠 Home   📊 Stats   ⚙️ Mais  │  ← Bottom Navigation
└──────────────────────────────────┘
```

## Componentes

### StreakCard

- Mostra `currentStreak` de dias
- Barra de progresso visual até o próximo marco (7, 30, 100 dias)
- Se streak = 0: "Comece seu streak hoje!"
- Se streak > 0: "🔥 {N} dias de streak!"

### DailyChallengesCard

- Mostra 3 desafios com progresso individual
- Cada desafio tem: número (①②③), estado (✅/🔓/🔒), categoria (teaser), dificuldade (estrelas)
- Desafio 1: sempre desbloqueado
- Desafio 2: desbloqueado ao completar o 1
- Desafio 3: desbloqueado ao completar o 2
- CTA: "JOGAR #N" aponta para o próximo desafio não completado
- Ao completar os 3: "✓ 3/3 completos! +bônus XP"
- Se o jogador usou chat IA após um desafio: "✅ Completado · 💬 Explorado"
- Se NÃO usou chat: "✅ Completado · 💬 Explorar?" (link para o chat)
- Reset diário: à meia-noite local, os 3 desafios resetam

### FreePlayCard

- Card simples com "MODO LIVRE" + descrição + CTA "JOGAR"
- Tap → navega para DifficultyPicker (o fluxo existente)

### QuickStatsRow

- 2×2 grid compacto: total de jogos, win rate, tier, XP
- Tap em qualquer stat → navega para StatsScreen

### GenerationIndicator

- Visível **somente** quando WorkManager está gerando puzzles em background
- Texto: "⟳ Gerando novos puzzles…"
- Ao finalizar: transição para "✓ Novos puzzles prontos!" (auto-dismiss após 3s)
- Em modo Light: nunca aparece
- **Fonte de estado**: `WorkManager.getWorkInfoByIdLiveData()` ou Flow do `PuzzleGenerationScheduler`

### ChatNudge (condicional)

- Aparece se o jogador completou um desafio mas NÃO usou o chat IA
- Texto: "Quer saber mais sobre 'GATOS'? [Explorar agora]"
- Tap → navega para ChatRoute(puzzleId)
- Dismiss: "✕" no canto
- Só aparece em modo AI

### BottomNavigation

- 3 tabs: Home (🏠), Stats (📊), Mais (⚙️)
- Home = HomeScreen (este)
- Stats = StatsScreen (existente)
- Mais = SettingsScreen (existente, inclui perfil e about)
- Indicador visual no tab ativo

## ViewModel

### HomeState

```kotlin
data class HomeState(
    val streak: Int,
    val nextStreakMilestone: Int,           // 7, 30, 100
    val dailyChallenges: List<DailyChallenge>,
    val completedDailies: Int,             // 0, 1, 2, 3
    val allDailiesComplete: Boolean,
    val totalPlayed: Int,
    val winRate: Float,
    val playerTier: String,
    val totalXp: Int,
    val isGeneratingPuzzles: Boolean,
    val generationComplete: Boolean,
    val chatNudge: ChatNudge?,             // null se não aplicável
)

data class DailyChallenge(
    val index: Int,                        // 0, 1, 2
    val state: DailyChallengeState,        // LOCKED, AVAILABLE, COMPLETED
    val difficulty: Int,                   // 1-5
    val categoryHint: String?,             // "Animais" (teaser, null se locked)
    val result: DailyChallengeResult?,     // tentativas, chat usado (null se não completado)
    val puzzleId: Long?,                   // para navegar ao chat
)

enum class DailyChallengeState { LOCKED, AVAILABLE, COMPLETED }

data class DailyChallengeResult(
    val attempts: Int,
    val won: Boolean,
    val chatExplored: Boolean,
)

data class ChatNudge(
    val word: String,
    val puzzleId: Long,
)
```

### HomeAction

```kotlin
sealed class HomeAction {
    data class StartDailyChallenge(val index: Int) : HomeAction()
    data object StartFreePlay : HomeAction()
    data object DismissChatNudge : HomeAction()
    data class NavigateToChat(val puzzleId: Long) : HomeAction()
    data object DismissGenerationBanner : HomeAction()
}
```

## Data — Daily Challenges Tracking

### Novo campo em GameSessionEntity

```kotlin
// Adicionar ao GameSessionEntity existente:
val dailyChallengeIndex: Int?   // 0, 1, 2 para dailies; null para free play
val dailyChallengeDate: String? // "2026-04-21" (date ISO para saber de qual dia é)
```

### DailyChallengeDao (queries)

```kotlin
@Query("""
    SELECT * FROM game_sessions 
    WHERE dailyChallengeDate = :date 
    ORDER BY dailyChallengeIndex
""")
suspend fun getDailyChallengesForDate(date: String): List<GameSessionEntity>

@Query("""
    SELECT COUNT(*) FROM game_sessions 
    WHERE dailyChallengeDate = :date AND completedAt IS NOT NULL
""")
suspend fun countCompletedDailies(date: String): Int
```

## Edge Cases

| Cenário | Comportamento |
|---|---|
| Primeiro acesso (sem stats) | Streak = 0, "Comece seu streak!", dailies disponíveis |
| Todos os dailies feitos | Card mostra "3/3 completos! +bônus", CTA desaparece |
| Sem puzzles no banco (daily) | Gerar inline via PuzzleGenerator; se Light: fallback estático |
| Meia-noite durante uso do app | Dailies resetam no próximo acesso ao Home (não mid-screen) |
| App aberto às 23:59, fecha às 00:01 | Ao voltar para Home, dailies do novo dia |
| Modo Light sem IA | Chat nudge nunca aparece; card de curiosidade estática no result |
| Geração background falha | Indicador some; puzzles estáticos servem como fallback |
| Jogador volta do PlayingScreen (back) | Home atualiza estado dos dailies |

## Decisões

| Decisão | Escolha | Razão |
|---------|---------|-------|
| DifficultyPicker no daily | Removido | Dificuldade é automática (progressiva) |
| Bottom nav vs hamburger | Bottom nav (3 tabs) | Padrão Android, mais acessível |
| Streak trigger | 1º jogo diário finalizado | Mínimo esforço para manter streak |
| Geração feedback | Indicador no Home | Transparência sem interromper |

## Out of Scope

- Leaderboard / amigos (futuro)
- Perfil com avatar customizável (futuro)
- Eventos temporários / sazonais (futuro)
- Notificações push para streak em risco (futuro)

## Critérios de Aceite

- [ ] HomeScreen é a tela inicial após o onboarding
- [ ] Streak card mostra `currentStreak` e barra de progresso
- [ ] Daily Challenges card mostra 3 desafios com estado correto (locked/available/completed)
- [ ] Desafio 2 só desbloqueia após completar desafio 1
- [ ] Desafio 3 só desbloqueia após completar desafio 2
- [ ] Tap em "JOGAR #N" navega para PlayingScreen com o puzzle do daily (sem DifficultyPicker)
- [ ] Após completar os 3 dailies, card mostra bônus
- [ ] Card do daily mostra "💬 Explorar?" se jogador não usou chat (modo AI)
- [ ] Free Play card navega para DifficultyPicker
- [ ] Quick stats mostra total de jogos, win rate, tier e XP
- [ ] Indicador de geração aparece quando WorkManager está gerando e some ao finalizar
- [ ] Chat nudge aparece se último daily completado sem chat (modo AI)
- [ ] Bottom Navigation com 3 tabs funciona corretamente
- [ ] Dailies resetam à meia-noite local
- [ ] HomeScreen atualiza ao voltar de um jogo
- [ ] Modo Light não mostra chat nudge nem indicador de geração
