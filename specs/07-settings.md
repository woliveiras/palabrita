# Spec 07 — Settings

## Summary

The settings screen allows the player to: change the game language, switch the AI model, view statistics, manage storage, and view app information. Model switching is the most complex feature on this screen.

## Layout

```
┌──────────────────────────────┐
│ ← Voltar      Configurações  │
├──────────────────────────────┤
│                              │
│ 🎮 JOGO                      │
│ ├── Idioma das palavras      │
│ │   Português (BR) ▸        │
│ ├── Tamanho das palavras 🔒  │
│ │   5-8 letras (padrão)     │
│ │   Desbloqueado no Astuto  │
│ └── Estatísticas ▸          │
│                              │
│ 🤖 INTELIGÊNCIA ARTIFICIAL   │
│ ├── Modelo atual             │
│ │   Gemma 4 E2B (2,6 GB) ▸  │
│ ├── Trocar modelo ▸         │
│ └── Espaço usado             │
│     Modelo: 2,6 GB           │
│     Puzzles: 12 KB           │
│                              │
│ ℹ️  SOBRE                     │
│ ├── Versão do app            │
│ │   1.0.0                    │
│ ├── Política de privacidade  │
│ └── Licenças open source     │
│                              │
│ ⚠️  DADOS                     │
│ ├── Excluir modelo           │
│ │   (muda para modo Light)   │
│ └── Resetar progresso        │
│     (limpa stats e puzzles)  │
│                              │
└──────────────────────────────┘
```

## Sections

### Word Language

- Opens a selector with the same options as onboarding (PT/EN/ES)
- On change: updates `PlayerStatsEntity.preferredLanguage`
- Future puzzles will be generated in the new language
- Already-generated puzzles (cache) in the old language are NOT deleted (can be used if the user switches back)
- Does not affect the UI language (follows device locale, changeable in Android settings)

### Word Size

**Locked** until the **Astuto** tier (150 XP). When locked:
- Shows lock icon (🔒)
- Text: "Unlocked at Astuto"
- Tap shows toast: "Keep playing to unlock!"

**Unlocked** (tier ≥ Astuto):
- Opens a selector with the following options:

| Option | Range | Description |
|---|---|---|
| Default | Dynamic by difficulty (5-8) | Normal behavior |
| Short words | 5-6 letters (fixed) | For players who prefer speed |
| Long words | 7-9 letters (fixed) | For players who want more challenge |
| Epic words | 8-10 letters (fixed) | Maximum challenge, Epic+ tier |

- "Epic words" only appears for tier ≥ Epic (1000 XP)
- Saved in `PlayerStatsEntity.wordSizePreference`
- When the player chooses a fixed range, difficulty still affects the word's **rarity** and **hint style**, but not the size
- Value `"DEFAULT"` = dynamic behavior by difficulty

### Statistics

Dedicated screen with:

```
┌──────────────────────────────┐
│ ← Voltar      Estatísticas   │
├──────────────────────────────┤
│                              │
│  42          87%             │
│  Jogos     Vitórias          │
│                              │
│  8           12              │
│  Sequência  Melhor           │
│  atual      sequência        │
│                              │
│  Distribuição de tentativas  │
│  1 ██                   5    │
│  2 ████████            12    │
│  3 ████████████████    18    │
│  4 ████████             8    │
│  5 ███                  3    │
│  6 █                    1    │
│                              │
│  [📤 Compartilhar stats]     │
│                              │
└──────────────────────────────┘
```

- Data from `PlayerStatsEntity`
- Attempt distribution histogram
- "Share stats" button generates text to clipboard:

```
Palabrita 📊
42 jogos · 87% vitórias
🔥 Sequência: 8
```

### Switch Model

Screen/dialog with available options:

```
┌──────────────────────────────┐
│ Trocar modelo de IA          │
├──────────────────────────────┤
│                              │
│ Modelo atual: Gemma 4 E2B    │
│ Tamanho: 2,6 GB              │
│                              │
│ Trocar para:                 │
│                              │
│ ┌────────────────────────┐   │
│ │ Gemma 3 1B             │   │
│ │ ~529 MB · Requer 4 GB  │   │
│ │ Mais leve, um pouco    │   │
│ │ menos preciso           │   │
│ └────────────────────────┘   │
│                              │
│ ┌────────────────────────┐   │
│ │ Modo Light (sem IA)    │   │
│ │ 0 MB · Sem requisitos  │   │
│ │ Banco de palavras       │   │
│ │ curado, sem chat        │   │
│ └────────────────────────┘   │
│                              │
│ ⚠️ Trocar de modelo irá:     │
│ • Baixar o novo modelo       │
│ • Excluir o modelo atual     │
│ • Regenerar puzzles          │
│                              │
│ [Cancelar]  [Trocar modelo]  │
│                              │
└──────────────────────────────┘
```

**Switch flow:**

```
1. Usuário seleciona novo modelo
2. Confirma na dialog de aviso
3. Se novo modelo precisa download:
   a. Mostrar tela de download (reutilizar componente do onboarding)
   b. Download novo modelo
   c. Verificar integridade (inicializar Engine)
   d. Se sucesso: deletar modelo antigo
   e. Se falha: manter modelo antigo, mostrar erro
4. Se trocando para Light mode:
   a. Confirmar: "Tem certeza? Isso excluirá o modelo e desabilitará o chat."
   b. Deletar modelo
   c. Carregar dataset estático
5. Atualizar ModelConfigEntity
6. Regenerar puzzles:
   a. Modo AI: gerar 7 novos puzzles com novo modelo
   b. Modo Light: carregar do dataset estático
7. Limpar cache de puzzles não jogados do modelo anterior (puzzles jogados permanecem no histórico)
```

**Rules:**
- Do not allow switching if there is a game in progress (or finish the current game first)
- New model download happens BEFORE deleting the old one (safety)
- If the device does not have space for both models simultaneously: warn and offer to delete first
- Warning if switching to a model above the device's tier

### Storage Used

Static information (no action):
- Model size on disk
- Database size (puzzles + stats + chat)
- Total storage used by the app

### Delete Model

- Confirmation dialog: "This will delete the AI model ({size}). The app will switch to Light mode with a curated word bank. You can download a model again at any time."
- Buttons: "Cancel" / "Delete"
- On confirm:
  1. Destroy Engine
  2. Delete model file
  3. Update ModelConfig to `none`
  4. Load static dataset
  5. Clear unplayed AI puzzles

### Reset Progress

- Confirmation dialog: "This will erase all your statistics, played puzzles, and chat history. This action cannot be undone."
- Buttons: "Cancel" / "Reset"
- On confirm:
  1. Clear `PlayerStatsEntity` (reset to initial values)
  2. Clear `GameSessionEntity` (all)
  3. Clear `ChatMessageEntity` (all)
  4. Mark all puzzles as unplayed
  5. Does NOT delete the model (keeps AI)

## SettingsViewModel

### State

```kotlin
data class SettingsState(
    val currentLanguage: String,
    val currentModel: ModelConfig,
    val deviceTier: DeviceTier,
    val stats: PlayerStats,
    val storageInfo: StorageInfo,
    val isModelSwitching: Boolean,
    val downloadProgress: Float?,
    val error: String?
)

data class StorageInfo(
    val modelSizeBytes: Long,
    val databaseSizeBytes: Long,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long
)
```

### Actions

```kotlin
sealed class SettingsAction {
    data class ChangeLanguage(val language: String) : SettingsAction()
    data class SwitchModel(val newModelId: ModelId) : SettingsAction()
    data object DeleteModel : SettingsAction()
    data object ResetProgress : SettingsAction()
    data object ShareStats : SettingsAction()
}
```

## Edge Cases

| Scenario | Behavior |
|---|---|
| Model switch with game in progress | Block switch; show "Finish the current game before switching" |
| Insufficient space for two models simultaneously | Offer: "Delete current model first?" |
| New model download fails | Keep old model intact, show error + retry |
| Switch to model requiring more RAM than device | Warning, allow with confirmation |
| Reset progress | Does not affect model or language preferences |

## Acceptance Criteria

- [ ] Word language can be changed and persists
- [ ] Statistics display correct data from PlayerStats
- [ ] Attempt histogram renders proportionally
- [ ] Share stats generates correct text to clipboard
- [ ] Model switch: download → verification → deletion of old model works end-to-end
- [ ] Switch to Light mode: deletes model, loads static dataset
- [ ] Switch does not lose already-played puzzles (history preserved)
- [ ] Delete model correctly frees storage
- [ ] Reset progress clears stats without affecting model
- [ ] RAM warning appears when selecting a model above the tier
- [ ] Switch blocked during active game
