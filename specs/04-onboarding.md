# Spec 04 — Onboarding

## Resumo

O onboarding é o primeiro contato do usuário com o app. Guia o jogador pela explicação do jogo, escolha de idioma, seleção (ou auto-detecção) do modelo de IA e download. O fluxo adapta-se ao hardware do dispositivo.

## Fluxo Geral

```
App abre (primeira vez)
    │
    ▼
Screen 1: Welcome
    │
    ▼
Screen 2: Idioma
    │
    ▼
Screen 3: Seleção de IA ──── RAM < 4GB ────→ Light mode (auto) ──→ Screen 5
    │                                                                    
    ├── "Sim, quero escolher" ──→ Model Picker ──→ Screen 4
    │
    └── "Não, escolha pra mim" ──→ Auto-select ──→ Screen 4
                                                        │
                                                        ▼
                                                  Screen 4: Download
                                                        │
                                                        ▼
                                                  Screen 5: Geração inicial
                                                        │
                                                        ▼
                                                  Game screen
```

## Telas

### Screen 1 — Welcome

**Conteúdo:**
- Logo do Palabrita
- Título: "Descubra a palavra do dia"
- Subtítulo: "Um jogo de palavras com inteligência artificial, direto no seu celular"
- Ilustração ou animação simples (placeholder na V1)
- Botão: "Começar"

**Regras:**
- Se o usuário já completou o onboarding (flag em DataStore), pular direto para o game
- Animação de entrada suave (fade in)

### Screen 2 — Escolha de Idioma

**Conteúdo:**
- Título: "Em qual idioma você quer jogar?"
- Cards selecionáveis:
  - 🇧🇷 Português
  - 🇺🇸 English
  - 🇪🇸 Español
- Texto auxiliar: "Você pode mudar isso depois nas configurações"
- Botão: "Continuar"

**Regras:**
- Idioma padrão pré-selecionado baseado no locale do device
- Salvar em `PlayerStatsEntity.preferredLanguage`
- Este é o idioma das **PALAVRAS do jogo**, não da UI
- A UI segue o idioma do sistema operacional (locale do device) via `res/values-pt`, `res/values-en`, `res/values-es` ou usar en fallback
- O jogador pode mudar o idioma da UI nas configurações (independente do idioma do jogo)
- Exemplo: celular em inglês → UI em inglês, mas jogador escolhe palavras em português

### Screen 3 — Seleção de IA

**Caso A — Device com RAM < 4GB (tier LOW):**

- Título: "Seu dispositivo"
- Mensagem: "Seu dispositivo não suporta IA local. Mas não se preocupe! Você jogará com nosso banco de palavras que é super divertido igual!"
- Ícone informativo (não erro)
- Botão: "Entendi, vamos jogar!" → navega para Screen 5 (geração pula direto, carrega dataset estático)
- Salvar `ModelConfig(modelId = "none")`

**Caso B — Device com RAM ≥ 4GB:**

- Título: "Você quer escolher sua IA?"
- Dois botões:
  - **"Sim, quero escolher"** → expande Model Picker (inline ou nova tela)
  - **"Não, escolha pra mim"** → auto-select e navega para Screen 4

**Model Picker (expandido do "Sim"):**

- Título: "Escolha como sua IA deve se comportar!"
- Card 1: **"Não preciso economizar espaço"**
  - Subtítulo: "Gemma 4 E2B · ~2,6 GB de download"
  - Info: "Requer 8 GB de RAM"
  - Badge: "Recomendado" (se tier HIGH)
  - Ícone: estrela ou foguete
- Card 2: **"Preciso economizar espaço"**
  - Subtítulo: "Gemma 3 1B · ~529 MB de download"
  - Info: "Requer 4 GB de RAM"
  - Badge: "Recomendado" (se tier MEDIUM)
  - Ícone: folha ou leve
- Botão: "Continuar"

**Warning (se user escolher modelo acima do tier):**
- Dialog: "Seu dispositivo tem {X} GB de RAM. O modelo selecionado requer {Y} GB. A performance pode ser ruim ou o app pode travar. Deseja continuar?"
- Botões: "Continuar mesmo assim" / "Escolher outro"

**Auto-select lógica:**
- RAM ≥ 8GB → Gemma 4 E2B
- 4GB ≤ RAM < 8GB → Gemma 3 1B

### Screen 4 — Download do Modelo

**Conteúdo:**
- Título: "Preparando sua IA"
- Info box: "O modelo de inteligência artificial será baixado para o seu celular. Depois disso, tudo funcionará offline!"
- Detalhes:
  - Modelo: {nome}
  - Tamanho: {size}
  - Espaço disponível: {available}
- Progress bar (determinada, com %)
- Velocidade estimada / tempo restante (se possível)
- Botão "Cancelar"

**Regras:**
- Verificar conexão antes de iniciar
  - Se Wi-Fi: iniciar automaticamente
  - Se dados móveis + modelo > 500MB: dialog "O download é de {size}. Deseja baixar usando dados móveis?"
  - Se sem conexão: "Conecte-se à internet para baixar o modelo"
- Se espaço insuficiente: "Espaço insuficiente. Você precisa de pelo menos {size + margem} disponíveis."
- Download via Play Asset Delivery (Play Store) ou download direto (dev)
- Salvar progress em `ModelConfigEntity.downloadState`
- Se app for fechado durante download: retomar ao reabrir (PAD suporta resume)
- Se download falhar: botão "Tentar novamente" + opção "Escolher modelo menor"

**State Machine — Download do Modelo:**

```
┌─────────┐
│  IDLE   │
└────┬────┘
     │ StartDownload
     ▼
┌──────────────┐    WifiRequired    ┌─────────────────┐
│  CHECKING    │──────────────────→│ WAITING_FOR_WIFI │
└──────┬───────┘                   └────────┬─────────┘
       │ SpaceOk                            │ WifiConnected
       ▼                                    ▼
┌──────────────┐                   ┌──────────────┐
│ DOWNLOADING  │←──────────────────│ DOWNLOADING  │
└──┬────────┬──┘                   └──────────────┘
   │        │
 Done    Failed
   │        │
   ▼        ▼
┌────────┐ ┌────────┐
│COMPLETE│ │ FAILED │
└────────┘ └───┬────┘
               │ Retry ──→ CHECKING
               │ Cancel ──→ IDLE
```

```kotlin
// State machine do download
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Checking : DownloadState()
    data object WaitingForWifi : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val modelPath: String) : DownloadState()
    data class Failed(val errorCode: Int) : DownloadState()
}

sealed class DownloadEvent {
    data object StartDownload : DownloadEvent()
    data object SpaceOk : DownloadEvent()
    data object WifiRequired : DownloadEvent()
    data object WifiConnected : DownloadEvent()
    data class Progress(val percent: Float) : DownloadEvent()
    data object Done : DownloadEvent()
    data class Fail(val errorCode: Int) : DownloadEvent()
    data object Retry : DownloadEvent()
    data object Cancel : DownloadEvent()
}
```

### Screen 5 — Geração Inicial de Puzzles

**Conteúdo (modo AI):**
- Título: "Gerando seus primeiros desafios..."
- Subtítulo: "Isso acontece apenas na primeira vez"
- Progress: "Puzzle 3 de 7..."
- Animação sutil (loading com personalidade)

**Conteúdo (modo Light):**
- Título: "Preparando o jogo..."
- Subtítulo: "Carregando palavras do banco curado"
- Progress rápido (< 1s, carregamento de assets)

**Regras (modo AI):**
- Gerar 7 puzzles no idioma selecionado
- Usar `PuzzleGenerator.generateBatch(count=7, ...)`
- Mostrar progresso por puzzle
- Se puzzle falhar validação após 3 retries: pular e continuar
- Mínimo aceitável: 3 puzzles válidos (se < 3, mostrar erro e oferecer retry ou Light mode)
- Ao completar: salvar puzzles em Room, navegar para Game

**Regras (modo Light):**
- Carregar dataset estático do assets
- Filtrar por idioma selecionado
- Salvar em Room com `source = STATIC`
- Navegação imediata para Game

## OnboardingViewModel

**State Machine — Fluxo de Onboarding:**

```
WELCOME ──→ LANGUAGE ──→ MODEL_SELECTION ──→ DOWNLOAD ──→ GENERATION ──→ COMPLETE
                              │                                ▲
                              │ (tier LOW)                     │
                              └────────────────────────────────┘
                                (pula download, vai direto para geração/load estático)
```

```kotlin
// State machine do onboarding
val onboardingStateMachine = StateMachine<OnboardingStep, OnboardingEvent>(
    initialState = OnboardingStep.WELCOME,
    transitions = mapOf(
        (OnboardingStep.WELCOME to OnboardingEvent.Next) to OnboardingStep.LANGUAGE,
        (OnboardingStep.LANGUAGE to OnboardingEvent.Next) to OnboardingStep.MODEL_SELECTION,
        (OnboardingStep.MODEL_SELECTION to OnboardingEvent.Next) to OnboardingStep.DOWNLOAD,
        (OnboardingStep.MODEL_SELECTION to OnboardingEvent.SkipToLight) to OnboardingStep.GENERATION,
        (OnboardingStep.DOWNLOAD to OnboardingEvent.DownloadComplete) to OnboardingStep.GENERATION,
        (OnboardingStep.GENERATION to OnboardingEvent.GenerationComplete) to OnboardingStep.COMPLETE,
        // Back navigation
        (OnboardingStep.LANGUAGE to OnboardingEvent.Back) to OnboardingStep.WELCOME,
        (OnboardingStep.MODEL_SELECTION to OnboardingEvent.Back) to OnboardingStep.LANGUAGE,
    )
)
```

**State:**

```kotlin
data class OnboardingState(
    val currentStep: OnboardingStep,
    val selectedLanguage: String = Locale.getDefault().language,
    val deviceTier: DeviceTier,
    val selectedModel: ModelId? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val generationProgress: GenerationProgress? = null,
    val error: OnboardingError? = null
)

enum class OnboardingStep {
    WELCOME, LANGUAGE, MODEL_SELECTION, DOWNLOAD, GENERATION, COMPLETE
}

data class GenerationProgress(
    val current: Int,
    val total: Int,
    val lastGeneratedWord: String? = null
)
```

**Actions:**

```kotlin
sealed class OnboardingAction {
    data object StartOnboarding : OnboardingAction()
    data class SelectLanguage(val language: String) : OnboardingAction()
    data class SelectModel(val modelId: ModelId) : OnboardingAction()
    data object AutoSelectModel : OnboardingAction()
    data object StartDownload : OnboardingAction()
    data object CancelDownload : OnboardingAction()
    data object RetryDownload : OnboardingAction()
    data object StartGeneration : OnboardingAction()
    data object SkipToLightMode : OnboardingAction()
}
```

## Edge Cases

| Cenário | Comportamento |
|---|---|
| App fechado durante download | PAD retoma; direct download recomeça (mostrar progresso salvo) |
| App fechado durante geração | Puzzles parciais ficam em Room; ao reabrir, verificar se tem ≥3 e completar se necessário |
| Sem internet | Bloquear Screen 4; oferecer Light mode como alternativa |
| Download completo mas modelo corrompido | Detectar na inicialização do Engine; oferecer re-download |
| Usuário volta para tela anterior | Cancelar download/geração em progresso |
| Espaço acaba durante download | PAD trata; direct download: detectar IOException, alertar |

## Critérios de Aceite

- [ ] Onboarding completo funciona em device tier HIGH (Gemma 4 download + geração)
- [ ] Onboarding completo funciona em device tier MEDIUM (Gemma 3 download + geração)
- [ ] Onboarding completo funciona em device tier LOW (Light mode, sem download)
- [ ] Auto-select escolhe modelo correto baseado na RAM
- [ ] Warning aparece quando user escolhe modelo acima do tier
- [ ] Download mostra progresso real (não fake)
- [ ] Download pode ser cancelado
- [ ] Download retoma após app ser fechado (PAD)
- [ ] Geração produz ≥3 puzzles válidos
- [ ] Se geração falhar completamente, oferece Light mode como fallback
- [ ] Onboarding não reaparece após ser completado
- [ ] Idioma selecionado persiste em PlayerStats
- [ ] Wi-Fi warning aparece para downloads >500MB em dados móveis
