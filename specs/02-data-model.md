# Spec 02 — Modelo de Dados

## Resumo

Toda persistência local usa Room (SQLite). As entities representam puzzles, sessões de jogo, estatísticas do jogador, mensagens de chat e configuração do modelo. Um dataset estático pré-bundled serve como fallback para o modo Light.

## Entities

### PuzzleEntity

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` (auto-generate) | PK |
| `word` | `String` | Palavra do puzzle (sem acentos, minúscula) |
| `wordDisplay` | `String` | Palavra com acentos para exibição pós-jogo |
| `language` | `String` | Código ISO 639-1 (ex: "pt", "en", "es") |
| `difficulty` | `Int` | 1 a 5 |
| `category` | `String` | Categoria da palavra (ex: "animal", "fruta") |
| `hints` | `String` | JSON array de 5 strings (dicas progressivas) |
| `source` | `String` | Enum: `"AI"` ou `"STATIC"` |
| `generatedAt` | `Long` | Timestamp de geração (epoch ms) |
| `playedAt` | `Long?` | Timestamp de quando foi jogada (null se não jogada) |
| `isPlayed` | `Boolean` | Se já foi usada em um jogo, não pode ser usada de novo |
| `isValid` | `Boolean` | Se passou na validação |

**Índices**: `(isPlayed, language)`, `(word)` (unique constraint)

### PlayerStatsEntity

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Int` | PK (sempre 1 — singleton) |
| `totalPlayed` | `Int` | Total de jogos completados |
| `totalWon` | `Int` | Total de vitórias |
| `currentStreak` | `Int` | Dias consecutivos jogando (independente de vitória/derrota) |
| `maxStreak` | `Int` | Maior sequência de dias consecutivos |
| `avgAttempts` | `Float` | Média de tentativas por jogo |
| `preferredLanguage` | `String` | Idioma preferido (ISO 639-1) |
| `currentDifficulty` | `Int` | Nível de dificuldade recomendado pelo sistema (1-5) |
| `maxUnlockedDifficulty` | `Int` | Maior nível desbloqueado (nunca decresce, jogador pode selecionar até este +1) |
| `totalXp` | `Int` | XP acumulado (nunca decresce) |
| `playerTier` | `String` | Tier atual: `"NOVATO"`, `"CURIOSO"`, `"ASTUTO"`, `"SABIO"`, `"EPICO"`, `"LENDARIO"` |
| `gamesWonByDifficulty` | `String` | JSON: `{"1": 45, "2": 30, "3": 12, "4": 5, "5": 0}` |
| `winRateByDifficulty` | `String` | JSON: `{"1": 0.85, "2": 0.72, "3": 0.60, "4": 0.50, "5": 0.0}` |
| `consecutiveLossesAtCurrent` | `Int` | Derrotas seguidas no nível atual (reset ao ganhar ou mudar nível) |
| `wordSizePreference` | `String` | `"DEFAULT"`, `"SHORT"`, `"LONG"`, `"EPIC"` (desbloqueado no tier Astuto+) |
| `guessDistribution` | `String` | JSON: `{"1": 5, "2": 10, "3": 8, "4": 3, "5": 1, "6": 0}` |
| `lastPlayedAt` | `Long` | Timestamp do último jogo (para streak de dias) |

**Tiers por XP:**

| Tier | XP mínimo | Descrição |
|---|---|---|
| Novato | 0 | Início |
| Curioso | 50 | ~50 jogos fáceis |
| Astuto | 150 | ~3 meses jogando |
| Sábio | 400 | ~1 ano consistente |
| Épico | 1000 | ~2-3 anos, domina nível 4-5 |
| Lendário | 2500 | Veterano dedicado |

**XP por jogo:**

| Ação | XP |
|---|---|
| Vitória nível 1 | 1 |
| Vitória nível 2 | 2 |
| Vitória nível 3 | 3 |
| Vitória nível 4 | 5 |
| Vitória nível 5 | 8 |
| Acertou na 1ª tentativa | +3 bônus |
| Acertou na 2ª tentativa | +1 bônus |
| Streak 7 dias | +5 bônus |
| Streak 30 dias | +20 bônus |
| Cada dica usada | -1 (mínimo final: 1 XP) |

### GameSessionEntity

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` (auto-generate) | PK |
| `puzzleId` | `Long` | FK → PuzzleEntity |
| `attempts` | `String` | JSON array de strings (cada tentativa) |
| `startedAt` | `Long` | Timestamp de início |
| `completedAt` | `Long?` | Timestamp de fim (null se em andamento) |
| `hintsUsed` | `Int` | Quantidade de dicas reveladas (0-5) |
| `won` | `Boolean` | Se o jogador acertou |

**Índice**: `(puzzleId)` unique

### ChatMessageEntity

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` (auto-generate) | PK |
| `puzzleId` | `Long` | FK → PuzzleEntity (agrupa mensagens por puzzle) |
| `role` | `String` | `"user"` ou `"model"` |
| `content` | `String` | Texto da mensagem |
| `timestamp` | `Long` | Timestamp de criação |

**Índice**: `(puzzleId, timestamp)`

### ModelConfigEntity

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Int` | PK (sempre 1 — singleton) |
| `modelId` | `String` | Enum: `"gemma4_e2b"`, `"gemma3_1b"`, `"none"` |
| `downloadState` | `String` | Enum: `"NOT_DOWNLOADED"`, `"DOWNLOADING"`, `"DOWNLOADED"`, `"FAILED"` |
| `modelPath` | `String?` | Caminho do modelo no filesystem |
| `sizeBytes` | `Long` | Tamanho do modelo em bytes |
| `selectedAt` | `Long` | Timestamp de seleção |

## DAOs

### PuzzleDao

```kotlin
@Query("SELECT * FROM puzzles WHERE isPlayed = 0 AND language = :lang AND difficulty = :difficulty ORDER BY generatedAt LIMIT 1")
suspend fun getNextUnplayed(lang: String, difficulty: Int): PuzzleEntity?

@Query("SELECT COUNT(*) FROM puzzles WHERE isPlayed = 0 AND language = :lang AND difficulty = :difficulty")
suspend fun countUnplayed(lang: String, difficulty: Int): Int

@Query("SELECT word FROM puzzles")
suspend fun getAllWords(): List<String>

@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insert(puzzle: PuzzleEntity): Long

@Query("UPDATE puzzles SET isPlayed = 1, playedAt = :playedAt WHERE id = :id")
suspend fun markAsPlayed(id: Long, playedAt: Long)
```

### PlayerStatsDao

```kotlin
@Query("SELECT * FROM player_stats WHERE id = 1")
suspend fun get(): PlayerStatsEntity?

@Upsert
suspend fun upsert(stats: PlayerStatsEntity)
```

### GameSessionDao

```kotlin
@Insert
suspend fun insert(session: GameSessionEntity): Long

@Update
suspend fun update(session: GameSessionEntity)

@Query("SELECT * FROM game_sessions WHERE puzzleId = :puzzleId")
suspend fun getByPuzzleId(puzzleId: Long): GameSessionEntity?
```

### ChatMessageDao

```kotlin
@Insert
suspend fun insert(message: ChatMessageEntity)

@Query("SELECT * FROM chat_messages WHERE puzzleId = :puzzleId ORDER BY timestamp")
suspend fun getByPuzzleId(puzzleId: Long): List<ChatMessageEntity>

@Query("SELECT COUNT(*) FROM chat_messages WHERE puzzleId = :puzzleId AND role = 'user'")
suspend fun countUserMessages(puzzleId: Long): Int
```

### ModelConfigDao

```kotlin
@Query("SELECT * FROM model_config WHERE id = 1")
suspend fun get(): ModelConfigEntity?

@Upsert
suspend fun upsert(config: ModelConfigEntity)
```

## Interfaces de Repositório (em `core/model`)

```kotlin
interface PuzzleRepository {
    suspend fun getNextUnplayed(language: String): Puzzle?
    suspend fun countUnplayed(language: String): Int
    suspend fun getAllGeneratedWords(): List<String>
    suspend fun savePuzzle(puzzle: Puzzle): Long
    suspend fun markAsPlayed(puzzleId: Long)
}

interface StatsRepository {
    suspend fun getStats(): PlayerStats
    suspend fun updateAfterGame(won: Boolean, attempts: Int, difficulty: Int, hintsUsed: Int)
    suspend fun calculateXpForGame(won: Boolean, attempts: Int, difficulty: Int, currentStreak: Int, hintsUsed: Int): Int
    suspend fun checkAndPromoteDifficulty(): Int  // retorna novo nível
    fun observeStats(): Flow<PlayerStats>
}

interface ModelRepository {
    suspend fun getConfig(): ModelConfig
    suspend fun updateConfig(config: ModelConfig)
    fun observeConfig(): Flow<ModelConfig>
}
```

## JSON Contracts

### Puzzle hints (stored in PuzzleEntity.hints)

```json
["Dica mais vaga", "Dica menos vaga", "Dica média", "Dica mais específica", "Dica quase direta"]
```

### Game attempts (stored in GameSessionEntity.attempts)

```json
["carro", "campo", "gatos", "mundo", "plano"]
```

### Guess distribution (stored in PlayerStatsEntity.guessDistribution)

```json
{"1": 5, "2": 12, "3": 18, "4": 8, "5": 3, "6": 1}
```

## Dataset Estático (Light Mode)

- Formato: arquivo JSON em `assets/static_puzzles.json`
- Estrutura:

```json
{
  "version": 1,
  "puzzles": [
    {
      "word": "gatos",
      "wordDisplay": "gatos",
      "language": "pt",
      "difficulty": 2,
      "category": "animal",
      "hints": ["Tem quatro patas", "É um animal doméstico", "Ronrona", "Persegue ratos", "Mia"],
      "curiosity": "A palavra 'gato' vem do latim tardio 'cattus'."
    }
  ]
}
```

- Mínimo 200 puzzles divididos entre PT (~100), EN (~60), ES (~40)
- Campo `curiosity` usado no fallback do chat (Light mode mostra card estático)

## Critérios de Aceite

- [ ] Todas as entities criam tabelas corretamente via Room
- [ ] Migrations funcionam (teste com versão anterior do schema)
- [ ] DAOs retornam dados corretos em testes com in-memory DB
- [ ] Unique constraint em `PuzzleEntity.word` previne duplicatas
- [ ] Dataset estático carrega corretamente do assets
- [ ] JSON de hints, attempts e guessDistribution serializa/deserializa sem erro
- [ ] `ModelConfigEntity` persiste estado de download entre restarts do app
