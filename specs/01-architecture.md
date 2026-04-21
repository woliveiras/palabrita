# Spec 01 — Arquitetura

## Resumo

Palabrita é um app Android nativo (Kotlin + Jetpack Compose) com arquitetura multi-módulo Gradle. O app roda um LLM local via LiteRT-LM para gerar puzzles de palavras e oferecer chat educacional pós-acerto. Dispositivos com pouca RAM recebem um modo Light com dataset estático.

## Stack Técnica

| Camada | Tecnologia |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose (type-safe routes) |
| DI | Hilt |
| Persistência | Room + DataStore (preferences) |
| LLM Runtime | LiteRT-LM Android (`com.google.ai.edge.litertlm:litertlm-android`) |
| Background | WorkManager |
| Serialização | Kotlin Serialization (JSON) |
| Build | Gradle Kotlin DSL + Version Catalog |
| Min SDK | Android 12 (API 31) |
| Target SDK | Último estável |
| Linguagem | Kotlin 2.x |

## Estrutura de Módulos

```
palabrita/
├── app/                        → Entry point, navegação, DI root
├── core/
│   ├── model/                  → Data classes, enums, interfaces de repositório
│   ├── data/                   → Room DB, DAOs, implementações de repositório, dataset estático
│   ├── ai/                     → LiteRT-LM wrapper, prompts, parser, validador
│   └── common/                 → Device capabilities, storage checker, extensões
├── feature/
│   ├── onboarding/             → Onboarding, seleção de modelo, download
│   ├── game/                   → Tela do jogo (Wordle-style)
│   ├── chat/                   → Chat pós-acerto
│   └── settings/               → Configurações, troca de modelo, estatísticas
└── gradle/
    └── libs.versions.toml      → Version catalog
```

## Grafo de Dependências

```
app ──→ feature/onboarding
   ──→ feature/game
   ──→ feature/chat
   ──→ feature/settings

feature/onboarding ──→ core/ai, core/data, core/model, core/common
feature/game       ──→ core/data, core/model, core/common
feature/chat       ──→ core/ai, core/data, core/model
feature/settings   ──→ core/ai, core/data, core/model, core/common

core/data ──→ core/model
core/ai   ──→ core/model
core/common ──→ (nenhuma dependência interna)
core/model  ──→ (nenhuma dependência interna)
```

**Regra**: nenhum módulo `feature/*` depende de outro `feature/*`.

## Responsabilidades por Módulo

### `app`
- `MainActivity` (single activity)
- `PalabritaApp` (Application class, Hilt entry point)
- `NavGraph` (rotas: Onboarding → Game → Chat → Settings)
- Hilt modules de nível app

### `core/model`
- Data classes puros: `Puzzle`, `PlayerStats`, `GameSession`, `ChatMessage`, `ModelConfig`, `DeviceTier`
- Interfaces de repositório: `PuzzleRepository`, `StatsRepository`, `ModelRepository`
- Enums: `PuzzleSource (AI, STATIC)`, `ModelId (GEMMA4_E2B, GEMMA3_1B, NONE)`, `DownloadState`

### `core/data`
- Room database (`PalabritaDatabase`)
- Entities: `PuzzleEntity`, `PlayerStatsEntity`, `GameSessionEntity`, `ChatMessageEntity`, `ModelConfigEntity`
- DAOs: `PuzzleDao`, `PlayerStatsDao`, `GameSessionDao`, `ChatMessageDao`, `ModelConfigDao`
- Implementações de repositório
- `StaticPuzzleProvider`: carrega puzzles do dataset pré-bundled (assets JSON)

### `core/ai`
- `LlmEngineManager`: singleton, lifecycle do Engine (init/destroy)
- `PuzzleGenerator`: geração em batch com prompts por modelo
- `ChatEngine`: conversa pós-acerto
- `PromptTemplates`: constantes de prompt, variantes por modelo
- `LlmResponseParser`: parse JSON, fallback regex
- `PuzzleValidator`: validação determinística
- `StaticPuzzleProvider` (se não acoplado em data)

### `core/common`
- `DeviceCapabilities`: detecção de RAM, classificação em tiers
- `StorageChecker`: espaço disponível
- `StateMachine<S, E>`: mini state machine genérica (~30 linhas), usada em fluxos complexos
- Extension functions compartilhadas

**Onde usar StateMachine vs sealed class + when:**
- **StateMachine formal**: Engine lifecycle, download de modelo, fluxo de onboarding (muitos estados, transições condicionais)
- **Sealed class + when**: Game status, chat status (poucos estados, transições simples)

### `feature/onboarding`
- Telas de boas-vindas, seleção de idioma, seleção de modelo, download, geração inicial
- `OnboardingViewModel`

### `feature/game`
- Tela do jogo, grid de letras, teclado virtual, sistema de dicas
- `GameViewModel`

### `feature/chat`
- Chat pós-acerto com LLM, fallback estático para Light mode
- `ChatViewModel`

### `feature/settings`
- Configurações: idioma, modelo, estatísticas, storage
- `SettingsViewModel`

## Critérios de Aceite

- [ ] Projeto compila com todos os módulos
- [ ] Grafo de dependências respeita as regras (sem feature→feature)
- [ ] Hilt injeta dependências corretamente entre módulos
- [ ] Navigation Compose navega entre todas as rotas
- [ ] Version catalog centraliza todas as versões de dependências
- [ ] Build incremental funciona (alterar um módulo não recompila tudo)
