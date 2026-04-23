# Spec 02 — Data Model

## Summary

All local persistence uses Room (SQLite). The entities represent puzzles, game sessions, player statistics, chat messages, and model configuration. A pre-bundled static dataset serves as fallback for Light mode.

## Entities

### PuzzleEntity

| Field | Type | Description |
|---|---|---|
| `id` | `Long` (auto-generate) | PK |
| `word` | `String` | Puzzle word (no accents, lowercase) |
| `wordDisplay` | `String` | Word with accents for post-game display |
| `language` | `String` | ISO 639-1 code (e.g., "pt", "en", "es") |
| `difficulty` | `Int` | Word length (4–8) |
| `category` | `String` | Always empty string `""`. Column kept for backward compatibility (no Room migration). |
| `hints` | `String` | JSON array of 3 strings (progressive hints). Legacy rows may have 5. |
| `source` | `String` | Enum: `"AI"` or `"STATIC"` |
| `generatedAt` | `Long` | Generation timestamp (epoch ms) |
| `playedAt` | `Long?` | Timestamp when played (null if not played) |
| `isPlayed` | `Boolean` | If already used in a game, cannot be used again |
| `isValid` | `Boolean` | If it passed validation |

**Indexes**: `(isPlayed, language)`, `(word)` (unique constraint)

### PlayerStatsEntity

| Field | Type | Description |
|---|---|---|
| `id` | `Int` | PK (always 1 — singleton) |
| `totalPlayed` | `Int` | Total games completed |
| `totalWon` | `Int` | Total wins |
| `currentStreak` | `Int` | Consecutive days played (regardless of win/loss) |
| `maxStreak` | `Int` | Longest streak of consecutive days |
| `avgAttempts` | `Float` | Average attempts per game |
| `preferredLanguage` | `String` | Preferred language (ISO 639-1) |
| `guessDistribution` | `String` | JSON: `{"1": 5, "2": 10, "3": 8, "4": 3, "5": 1, "6": 0}` |
| `lastPlayedAt` | `Long` | Timestamp of last game (for day streak) |

> **Note:** Legacy columns (`totalXp`, `playerTier`, `currentDifficulty`, `maxUnlockedDifficulty`, `gamesWonByDifficulty`, `winRateByDifficulty`, `consecutiveLossesAtCurrent`, `wordSizePreference`) are kept in the DB schema for backward compatibility (no Room migration) but are no longer used by the application.

### GameSessionEntity

| Field | Type | Description |
|---|---|---|
| `id` | `Long` (auto-generate) | PK |
| `puzzleId` | `Long` | FK → PuzzleEntity |
| `attempts` | `String` | JSON array of strings (each attempt) |
| `startedAt` | `Long` | Start timestamp |
| `completedAt` | `Long?` | End timestamp (null if in progress) |
| `hintsUsed` | `Int` | Number of hints revealed |
| `won` | `Boolean` | Whether the player won |

**Index**: `(puzzleId)` unique

### ChatMessageEntity

| Field | Type | Description |
|---|---|---|
| `id` | `Long` (auto-generate) | PK |
| `puzzleId` | `Long` | FK → PuzzleEntity (groups messages by puzzle) |
| `role` | `String` | `"user"` or `"model"` |
| `content` | `String` | Message text |
| `timestamp` | `Long` | Creation timestamp |

**Index**: `(puzzleId, timestamp)`

### ModelConfigEntity

| Field | Type | Description |
|---|---|---|
| `id` | `Int` | PK (always 1 — singleton) |
| `modelId` | `String` | Enum: `"gemma4_e2b"`, `"gemma3_1b"`, `"none"` |
| `downloadState` | `String` | Enum: `"NOT_DOWNLOADED"`, `"DOWNLOADING"`, `"DOWNLOADED"`, `"FAILED"` |
| `modelPath` | `String?` | Model path on filesystem |
| `sizeBytes` | `Long` | Model size in bytes |
| `selectedAt` | `Long` | Selection timestamp |

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

## Repository Interfaces (in `core/model`)

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
    suspend fun updateAfterGame(won: Boolean, attempts: Int, hintsUsed: Int)
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
["Dica mais vaga", "Dica intermediária", "Dica mais específica"]
```

### Game attempts (stored in GameSessionEntity.attempts)

```json
["carro", "campo", "gatos", "mundo", "plano"]
```

### Guess distribution (stored in PlayerStatsEntity.guessDistribution)

```json
{"1": 5, "2": 12, "3": 18, "4": 8, "5": 3, "6": 1}
```

## Static Dataset (Light Mode)

- Format: JSON file at `assets/static_puzzles.json`
- Structure:

```json
{
  "version": 1,
  "puzzles": [
    {
      "word": "gatos",
      "wordDisplay": "gatos",
      "language": "pt",
      "difficulty": 5,
      "category": "",
      "hints": ["Tem quatro patas", "É um animal doméstico", "Ronrona"],
      "curiosity": "A palavra 'gato' vem do latim tardio 'cattus'."
    }
  ]
}
```

> **Note:** `difficulty` now stores the word length (4–8). `category` is always empty string. Hints are 3 per puzzle.

- Minimum 200 puzzles split between PT (~100), EN (~60), ES (~40)
- `curiosity` field used in chat fallback (Light mode shows a static card)

## Acceptance Criteria

- [ ] All entities correctly create tables via Room
- [ ] Migrations work (tested with previous schema version)
- [ ] DAOs return correct data in tests with in-memory DB
- [ ] Unique constraint on `PuzzleEntity.word` prevents duplicates
- [ ] Static dataset loads correctly from assets
- [ ] JSON for hints, attempts, and guessDistribution serializes/deserializes without error
- [ ] `ModelConfigEntity` persists download state between app restarts
