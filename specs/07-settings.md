# Spec 07 — Settings

## Resumo

A tela de configurações permite ao jogador: trocar idioma do jogo, trocar de modelo de IA, ver estatísticas, gerenciar armazenamento e ver informações do app. A troca de modelo é a feature mais complexa desta tela.

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

## Seções

### Idioma das Palavras

- Abre seletor com as mesmas opções do onboarding (PT/EN/ES)
- Ao mudar: atualiza `PlayerStatsEntity.preferredLanguage`
- Puzzles futuros serão gerados no novo idioma
- Puzzles já gerados (cache) no idioma antigo NÃO são deletados (podem ser usados se voltar)
- Não afeta o idioma da UI (segue locale do device, alterável nas configurações do Android)

### Tamanho das Palavras

**Bloqueado** até o tier **Astuto** (150 XP). Quando bloqueado:
- Mostra ícone de cadeado (🔒)
- Texto: "Desbloqueado no Astuto"
- Tap mostra toast: "Continue jogando para desbloquear!"

**Desbloqueado** (tier ≥ Astuto):
- Abre seletor com as opções:

| Opção | Range | Descrição |
|---|---|---|
| Padrão | Dinâmico por dificuldade (5-8) | Comportamento normal |
| Palavras curtas | 5-6 letras (fixo) | Para quem prefere rapidez |
| Palavras longas | 7-9 letras (fixo) | Para quem quer mais desafio |
| Palavras épicas | 8-10 letras (fixo) | Máximo desafio, tier Épico+ |

- "Palavras épicas" só aparece para tier ≥ Épico (1000 XP)
- Salva em `PlayerStatsEntity.wordSizePreference`
- Quando o jogador escolhe um range fixo, a dificuldade ainda afeta a **raridade** da palavra e o **estilo das dicas**, mas não o tamanho
- Valor `"DEFAULT"` = comportamento dinâmico por dificuldade

### Estatísticas

Tela dedicada com:

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

- Dados de `PlayerStatsEntity`
- Histograma de distribuição de tentativas
- Botão "Compartilhar stats" gera texto para clipboard:

```
Palabrita 📊
42 jogos · 87% vitórias
🔥 Sequência: 8
```

### Trocar Modelo

Tela/dialog com opções disponíveis:

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

**Fluxo de troca:**

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

**Regras:**
- Não permitir troca se há jogo em andamento (ou finalizar o jogo atual primeiro)
- Download do novo modelo acontece ANTES de deletar o antigo (segurança)
- Se o device não tem espaço para os dois modelos simultaneamente: avisar e oferecer deletar primeiro
- Warning se trocar para modelo acima do tier do device

### Espaço Usado

Informação estática (sem ação):
- Tamanho do modelo em disco
- Tamanho do banco de dados (puzzles + stats + chat)
- Espaço total usado pelo app

### Excluir Modelo

- Dialog de confirmação: "Isso excluirá o modelo de IA ({size}). O app mudará para o modo Light com banco de palavras curado. Você pode baixar um modelo novamente a qualquer momento."
- Botões: "Cancelar" / "Excluir"
- Ao confirmar:
  1. Destruir Engine
  2. Deletar arquivo do modelo
  3. Atualizar ModelConfig para `none`
  4. Carregar dataset estático
  5. Limpar puzzles AI não jogados

### Resetar Progresso

- Dialog de confirmação: "Isso apagará todas as suas estatísticas, puzzles jogados e histórico de chat. Esta ação não pode ser desfeita."
- Botões: "Cancelar" / "Resetar"
- Ao confirmar:
  1. Limpar `PlayerStatsEntity` (resetar para valores iniciais)
  2. Limpar `GameSessionEntity` (todas)
  3. Limpar `ChatMessageEntity` (todas)
  4. Marcar todos os puzzles como não jogados
  5. NÃO deleta o modelo (mantém a IA)

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

| Cenário | Comportamento |
|---|---|
| Troca de modelo com jogo em andamento | Bloquear troca; mostrar "Termine o jogo atual antes de trocar" |
| Espaço insuficiente para dois modelos simultâneos | Oferecer: "Excluir modelo atual primeiro?" |
| Download do novo modelo falha | Manter modelo antigo intacto, mostrar erro + retry |
| Troca para modelo que exige mais RAM que o device | Warning, permitir com confirmação |
| Resetar progresso | Não afeta modelo nem preferências de idioma |

## Critérios de Aceite

- [ ] Idioma das palavras pode ser alterado e persiste
- [ ] Estatísticas exibem dados corretos de PlayerStats
- [ ] Histograma de tentativas renderiza proporcionalmente
- [ ] Compartilhar stats gera texto correto no clipboard
- [ ] Troca de modelo: download → verificação → exclusão do antigo funciona end-to-end
- [ ] Troca para Light mode: deleta modelo, carrega dataset estático
- [ ] Troca não perde puzzles já jogados (histórico mantido)
- [ ] Excluir modelo libera espaço corretamente
- [ ] Resetar progresso limpa stats sem afetar modelo
- [ ] Warning de RAM aparece ao selecionar modelo acima do tier
- [ ] Troca bloqueada durante jogo em andamento
