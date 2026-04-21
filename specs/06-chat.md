# Spec 06 — Chat Pós-Acerto

## Resumo

Após o jogador descobrir (ou não) a palavra, ele pode explorar a palavra via chat com o LLM local. O chat é educacional: etimologia, curiosidades, uso em frases, sinônimos, traduções. Funciona apenas em modo AI. No modo Light, um card estático substitui o chat.

## Fluxo

```
Tela de resultado (vitória ou derrota)
    │
    ├── Modo AI ──→ Botão "Explorar a palavra" ──→ Chat screen
    │
    └── Modo Light ──→ Card estático de curiosidade (inline na tela de resultado)
```

## Chat Screen (Modo AI)

### Layout

```
┌──────────────────────────────┐
│ ← Voltar    Explorar: GATOS  │
├──────────────────────────────┤
│                              │
│  🤖 "Gatos" é uma palavra    │
│  fascinante! Vem do latim    │
│  tardio "cattus"...          │
│                              │
│  👤 De onde vem essa palavra? │
│                              │
│  🤖 A palavra "gatos" tem    │
│  origem no latim tardio...   │
│  ████████▎ (typing)          │
│                              │
│  ─── 2 de 10 mensagens ───  │
│                              │
├──────────────────────────────┤
│  [    Digite sua pergunta  ] │
│  [         Enviar ➤        ] │
└──────────────────────────────┘
```

### Comportamento

**Mensagem inicial automática:**
- Ao abrir o chat, o LLM envia automaticamente uma primeira mensagem sobre a palavra
- Prompt interno (não visível ao usuário): "Conte uma curiosidade interessante sobre a palavra '{word}'"
- Streaming: tokens aparecem um a um com typing indicator

**Mensagens do usuário:**
- Input de texto livre
- Botão enviar (ou Enter no teclado)
- Input desabilitado durante resposta do modelo (streaming)
- Input desabilitado ao atingir limite

**Streaming de resposta:**
- Tokens aparecem progressivamente (via `Flow<String>`)
- Typing indicator (3 dots animados) antes do primeiro token
- Scroll automático para baixo conforme tokens chegam

**Limite de mensagens:**
- Máximo 10 mensagens do USUÁRIO por sessão (respostas do modelo não contam)
- Indicador visual: "X de 10 mensagens"
- Ao atingir limite:
  - Input desabilitado
  - Mensagem: "Você atingiu o limite de perguntas para esta palavra. Jogue novamente amanhã para explorar outra!"
  - Botão "Voltar ao jogo"

**Persistência:**
- Todas as mensagens salvas em `ChatMessageEntity`
- Se o usuário voltar para a tela de resultado e reabrir o chat: conversa anterior restaurada
- Histórico só existe para o puzzle atual (não carrega conversas de puzzles anteriores)

### System Prompt

**Gemma 4 (system role nativo):**

```
System: You are an educational assistant for the word game Palabrita.
The player just discovered the word "{word}" (category: {category}).

Your role:
- Answer questions about the word in an educational and engaging way
- Cover: origin/etymology, fun facts, everyday usage in sentences, synonyms, translations to other languages
- Keep responses short (3 paragraphs max)
- Use accessible, enthusiastic language
- Always respond in {language}
- Do not make up facts — if unsure, say so

You must NOT:
- Generate offensive content
- Go off-topic from the word
- Give overly long responses
```

**Gemma 3 (prepend ao primeiro user message):**

```
[Context: You are an educational assistant. The player discovered the word "{word}" (category: {category}). Answer about: origin, etymology, fun facts, usage in sentences, synonyms, translations. Keep responses short (3 paragraphs max). Always respond in {language}.]

{user_message}
```

### Sugestões de Perguntas

Abaixo do input, mostrar chips sugeridos (desaparecem após primeira mensagem manual):

- "De onde vem essa palavra?"
- "Me dá sinônimos"
- "Como usar em uma frase?"
- "Como se diz em inglês?"
- "Alguma curiosidade?"

Ao clicar em um chip: preenche o input e envia automaticamente.

## Card Estático (Modo Light)

Quando o usuário está em modo Light, o chat não está disponível. Em vez disso, a tela de resultado mostra um card inline:

```
┌──────────────────────────────┐
│ 💡 Curiosidade               │
│                              │
│ Categoria: Animal            │
│                              │
│ A palavra "gatos" vem do     │
│ latim tardio "cattus".       │
│ Presente em 90% dos lares    │
│ brasileiros como animal de   │
│ estimação.                   │
│                              │
│ [Ative a IA para explorar    │
│  mais sobre as palavras →]   │
└──────────────────────────────┘
```

- Conteúdo vem do campo `curiosity` do dataset estático
- Se `curiosity` estiver vazio: mostrar apenas categoria
- Link "Ative a IA" → navega para Settings (troca de modelo)

## ChatViewModel

### State

```kotlin
data class ChatState(
    val word: String,
    val category: String,
    val language: String,
    val messages: List<ChatMessage>,
    val currentInput: String,
    val isModelResponding: Boolean,
    val userMessageCount: Int,
    val maxMessages: Int = 10,
    val isAtLimit: Boolean,
    val error: String?,
    val suggestionsVisible: Boolean
)

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false  // true enquanto tokens estão chegando
)

enum class MessageRole { USER, MODEL }
```

### Actions

```kotlin
sealed class ChatAction {
    data class UpdateInput(val text: String) : ChatAction()
    data object SendMessage : ChatAction()
    data class SelectSuggestion(val suggestion: String) : ChatAction()
    data object GoBack : ChatAction()
}
```

## Edge Cases

| Cenário | Comportamento |
|---|---|
| LLM demora muito (>30s) para responder | Mostrar timeout message + botão retry |
| LLM gera resposta ofensiva | V1: não filtramos (modelos Gemma já têm safety filters). V2: pós-filtro |
| LLM gera resposta em idioma errado | V1: aceitar (prompt instrui idioma, mas não é garantido) |
| App fechado durante streaming | Salvar tokens recebidos até o momento como mensagem parcial |
| Usuário envia mensagem vazia | Ignorar (botão desabilitado se input vazio) |
| Engine não inicializado | Mostrar loading + inicializar engine, depois continuar |
| Conversa muito longa (muitos tokens) | Limite de 10 mensagens controla isso; contexto do Gemma 4 é 128K |

## Critérios de Aceite

- [ ] Chat abre com mensagem inicial automática do modelo
- [ ] Streaming de tokens funciona (tokens aparecem progressivamente)
- [ ] Typing indicator aparece antes do primeiro token
- [ ] Scroll automático segue a resposta
- [ ] Limite de 10 mensagens é respeitado
- [ ] Input desabilitado durante resposta do modelo
- [ ] Input desabilitado ao atingir limite
- [ ] Sugestões de perguntas aparecem e funcionam
- [ ] Mensagens persistem se usuário sair e voltar ao chat
- [ ] Modo Light mostra card estático em vez de chat
- [ ] Link "Ative a IA" no card estático navega para Settings
- [ ] System prompt inclui palavra e categoria corretas
