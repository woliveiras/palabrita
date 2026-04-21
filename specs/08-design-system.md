# Spec 08 — Design System

## Resumo

Paleta de cores, tipografia, espaçamento e componentes visuais do Palabrita. Todas as cores são definidas como `Color` no Compose Theme e referenciadas por tokens semânticos, nunca hardcoded.

## Paleta de Cores

### Cores de Feedback do Jogo

| Token | Nome | Hex | Uso |
|---|---|---|---|
| `correct` | Mint/Teal | `#4ECDC4` | Letra correta na posição certa (grid + teclado) |
| `present` | Amber/Gold | `#FFB347` | Letra existe mas posição errada (grid + teclado) |
| `absent` | Coral | `#FF6B6B` | Letra não existe na palavra (grid + teclado) |
| `unused` | Cinza Neutro | `#787C7E` | Tecla não usada no teclado |

### Cores de UI — Light Theme

| Token | Hex | Uso |
|---|---|---|
| `surface` | `#FAFAFA` | Background principal |
| `surfaceVariant` | `#F0F0F0` | Cards, inputs, cells vazias |
| `onSurface` | `#1A1A2E` | Texto primário |
| `onSurfaceVariant` | `#6B7280` | Texto secundário, labels |
| `primary` | `#4ECDC4` | Botões primários, accent, links |
| `onPrimary` | `#FFFFFF` | Texto sobre botão primário |
| `primaryContainer` | `#D4F5F2` | Background de badges, chips |
| `error` | `#FF6B6B` | Erros, estados destrutivos |
| `outline` | `#D1D5DB` | Bordas de cells, dividers |
| `cellBorder` | `#878A8C` | Borda de cell com letra digitada (antes do submit) |

### Cores de UI — Dark Theme

| Token | Hex | Uso |
|---|---|---|
| `surface` | `#121213` | Background principal |
| `surfaceVariant` | `#1E1E20` | Cards, inputs, cells vazias |
| `onSurface` | `#F5F5F5` | Texto primário |
| `onSurfaceVariant` | `#9CA3AF` | Texto secundário |
| `primary` | `#4ECDC4` | Accent (mesmo em ambos os themes) |
| `onPrimary` | `#1A1A2E` | Texto sobre botão primário |
| `primaryContainer` | `#1A3A38` | Background de badges, chips |
| `error` | `#FF6B6B` | Erros (mesmo em ambos os themes) |
| `outline` | `#3A3A3C` | Bordas |
| `cellBorder` | `#565758` | Borda de cell com letra |

### Cores de Feedback — ajuste Dark Theme

As cores de feedback (correct, present, absent) **não mudam** entre light e dark. São vibrantes o suficiente para ambos os fundos. Isso garante que prints e compartilhamentos sejam consistentes.

### Cores de Tier

| Tier | Cor | Hex | Uso |
|---|---|---|---|
| Novato | Cinza | `#9CA3AF` | Badge, borda de perfil |
| Curioso | Teal | `#4ECDC4` | Badge, borda de perfil |
| Astuto | Amber | `#FFB347` | Badge, borda de perfil |
| Sábio | Roxo | `#A78BFA` | Badge, borda de perfil |
| Épico | Rosa | `#F472B6` | Badge, borda de perfil |
| Lendário | Dourado gradiente | `#FFD700` → `#FFA500` | Badge com brilho, borda animada |

## Tipografia

Material 3 type scale com Compose `Typography`. Font padrão: sistema (Roboto no Android).

| Token | Size | Weight | Uso |
|---|---|---|---|
| `displayLarge` | 32sp | Bold | Tela de resultado ("Parabéns!") |
| `headlineMedium` | 24sp | SemiBold | Títulos de seção (onboarding, settings) |
| `titleLarge` | 20sp | SemiBold | Header do jogo ("Palabrita") |
| `titleMedium` | 16sp | Medium | Subtítulos, labels de card |
| `bodyLarge` | 16sp | Regular | Texto principal, dicas |
| `bodyMedium` | 14sp | Regular | Texto secundário |
| `labelLarge` | 14sp | Medium | Botões |
| `labelMedium` | 12sp | Medium | Badges, chips, contadores |
| `gridLetter` | 24sp | Bold | Letra dentro da célula do grid |
| `keyboardLetter` | 14sp | SemiBold | Letra na tecla do teclado |

## Espaçamento

Baseado em múltiplos de 4dp (Material 3):

| Token | Valor | Uso |
|---|---|---|
| `spacingXs` | 4dp | Entre ícone e texto inline |
| `spacingSm` | 8dp | Padding interno de chips/badges |
| `spacingMd` | 16dp | Padding de cards, gap entre rows |
| `spacingLg` | 24dp | Margem lateral da tela |
| `spacingXl` | 32dp | Gap entre seções |
| `spacingXxl` | 48dp | Topo/fundo de telas de onboarding |

## Cantos (Shape)

| Componente | Radius |
|---|---|
| Botão primário | 12dp (rounded) |
| Card | 16dp |
| Grid cell | 4dp |
| Tecla do teclado | 6dp |
| Badge de tier | 8dp |
| Dialog | 28dp (Material 3 default) |
| Bottom sheet | 28dp top corners |

## Elevação

| Componente | Elevation |
|---|---|
| Surface principal | 0dp |
| Cards | 1dp |
| Teclado | 2dp |
| Hint card (revelada) | 3dp |
| Dialog/Bottom sheet | 6dp |

## Animações

| Animação | Duração | Easing | Onde |
|---|---|---|---|
| Flip da célula | 300ms por célula, 100ms stagger | FastOutSlowIn | Após submeter tentativa |
| Shake da row | 300ms | Linear (oscilação) | Tentativa inválida |
| Bounce/pop | 400ms | OvershootInterpolator | Vitória (todas corretas) |
| Fade in | 200ms | FastOutSlowIn | Transições de tela, dica revelada |
| Slide up | 250ms | FastOutSlowIn | Hint card aparecendo |
| Progress bar | Contínuo | Linear | Download, geração |
| Tier badge brilho | 2s loop | EaseInOut | Lendário badge (shimmer) |

## Componentes Reutilizáveis

### PalabritaButton

```kotlin
// Variantes:
// - Primary: background mint, texto branco
// - Secondary: outlined, borda mint, texto mint
// - Destructive: background coral, texto branco
// - Ghost: sem background, texto mint (para ações terciárias)
```

### PalabritaCard

```kotlin
// Background: surfaceVariant
// Border: outline (1dp)
// Padding: spacingMd (16dp)
// Corner: 16dp
// Selecionável: borda muda para primary (mint) quando selecionado
```

### TierBadge

```kotlin
// Chip com cor do tier + ícone
// Corner: 8dp
// Texto: labelMedium
// Lendário: efeito shimmer no background
```

## Novos Componentes (Spec 10-12)

### BottomNavigation

```kotlin
// 3 tabs: Home (🏠), Stats (📊), Mais (⚙️)
// Background: surface
// Indicador ativo: primary (mint) pill shape
// Ícones: Material Icons (Home, BarChart, MoreHoriz)
// Labels: labelMedium
// Elevation: 2dp
// Height: 80dp (include bottom inset)
```

### HomeCards

Todos os cards do HomeScreen seguem o padrão `PalabritaCard` com variações:

**StreakCard**
```kotlin
// Background: primaryContainer (#D4F5F2 light / #1A3A38 dark)
// Ícone: 🔥 (emoji inline)
// Barra de progresso: primary (#4ECDC4) sobre outline (#D1D5DB)
// Texto do streak: titleMedium, bold
// Texto do marco: bodyMedium, onSurfaceVariant
// Padding: spacingMd (16dp)
// Corner: 16dp
```

**DailyChallengesCard**
```kotlin
// Background: surfaceVariant
// Header: "⭐ DESAFIOS DO DIA (N/3)" — titleMedium
// Cada desafio: Row com número + ícone estado + categoria + dificuldade
// Estados:
//   ✅ COMPLETED: texto onSurface, ícone correct (#4ECDC4)
//   🔓 AVAILABLE: texto onSurface, ícone primary
//   🔒 LOCKED: texto onSurfaceVariant (dimmed), ícone outline
// CTA "JOGAR #N": PalabritaButton Primary
// Corner: 16dp
// Elevation: 1dp
```

**FreePlayCard**
```kotlin
// Background: surfaceVariant
// Ícone: 🎲 (emoji inline)
// Título: "MODO LIVRE" — titleMedium
// Descrição: bodyMedium, onSurfaceVariant
// CTA: PalabritaButton Secondary (outlined)
// Corner: 16dp
```

**QuickStatsRow**
```kotlin
// 2×2 grid de mini-cards
// Cada mini-card: surfaceVariant, corner 12dp, padding spacingSm
// Valor: titleMedium, bold, onSurface
// Label: labelMedium, onSurfaceVariant
// Gap: spacingSm (8dp)
```

### GenerationIndicator

```kotlin
// Background: surfaceVariant com borda outline
// Ícone: animação de loading (CircularProgressIndicator, size 16dp)
// Texto: bodyMedium, onSurfaceVariant
// Estado "pronto": ícone ✓ (correct color), auto-dismiss 3s
// Corner: 12dp
// Padding: spacingSm vertical, spacingMd horizontal
```

### ChatCard (ResultScreen)

```kotlin
// Background: primaryContainer (#D4F5F2 light / #1A3A38 dark)
// Borda: 2dp, primary (#4ECDC4)
// Corner: 16dp
// Elevation: 2dp
// Header: "💬 Explore '{palavra}'" — titleMedium
// Sugestões: chips horizontais scrolláveis
//   Cada chip: surfaceVariant bg, corner 8dp, labelMedium
//   Ícone: emoji da categoria + texto
// Bônus: "+1 XP bônus ✨" — labelMedium, primary color
// CTA: PalabritaButton Primary "EXPLORAR AGORA"
// Modo Light: substitui por StaticCuriosityCard
```

### StaticCuriosityCard (Modo Light)

```kotlin
// Background: surfaceVariant
// Borda: 1dp, outline
// Corner: 16dp
// Ícone: 📖
// Título: "Sobre '{palavra}'" — titleMedium
// Texto: bodyMedium, onSurface — curiosidade estática do puzzle
// Rodapé: "— Curiosidade do dia" — labelMedium, onSurfaceVariant, italic
```

### ChatNudge

```kotlin
// Background: primaryContainer
// Corner: 12dp
// Texto: "💬 Quer saber mais sobre '{palavra}'?" — bodyMedium
// CTA: "Explorar agora" — labelLarge, primary
// Dismiss: IconButton "✕" no canto superior direito
// Animação: slide-in de baixo, 250ms
```

### ConfirmAbandonDialog

```kotlin
// Dialog padrão Material 3 (AlertDialog)
// Título: "Abandonar partida?" — headlineMedium
// Body: "Seu progresso neste jogo será perdido." — bodyLarge
// Botão primário: "Continuar jogando" — PalabritaButton Primary
// Botão destrutivo: "Abandonar" — PalabritaButton Destructive
// Corner: 28dp (Material 3 default)
```

### SharingCard (compartilhamento visual — futuro)

```kotlin
// Background: surface
// Header: "Palabrita 🔥N · Tier · XP" — titleLarge
// Contexto: "Desafio N/3 ⭐⭐" ou "Livre ⭐⭐⭐" — bodyMedium
// Grid de emojis: monospaced, bodyLarge
// Rodapé: dicas + XP — labelMedium, onSurfaceVariant
// Corner: 16dp
// Borda: 1dp, outline
```

### Confetti Animation

```kotlin
// Trigger: vitória no jogo (todas as células correct)
// Partículas: 50-80 retângulos/quadrados coloridos
// Cores: correct (#4ECDC4), present (#FFB347), absent (#FF6B6B), primary
// Duração: 2s
// Origem: topo da tela, gravidade para baixo
// Easing: EaseOut para gravidade
// Biblioteca sugerida: nl.dionsegijn:konfetti-compose (leve, Compose-native)
// Fallback: se performance < 60fps, reduzir partículas ou desabilitar
```

### Badge Component

```kotlin
// Tamanhos: Small (24dp), Medium (32dp), Large (48dp)
// Earned: ícone colorido + background tier color
// Not earned: ícone cinza (unused #787C7E) + background surfaceVariant
// Progresso: "7/10" — labelMedium abaixo do ícone
// Corner: 50% (circular)
// Lendário: efeito shimmer (mesmo do TierBadge)
```

## Acessibilidade

- **Contraste mínimo**: WCAG AA (4.5:1 para texto, 3:1 para UI)
  - Mint `#4ECDC4` sobre `#1A1A2E` (dark text) → 4.6:1 ✅
  - Coral `#FF6B6B` sobre `#FFFFFF` → 3.9:1 (usar texto dark `#1A1A2E` sobre coral)
  - Amber `#FFB347` sobre `#FFFFFF` → 2.1:1 (usar texto dark `#1A1A2E` sobre amber)
- **Texto sobre cores de feedback**: sempre `#1A1A2E` (dark) para garantir contraste
- **Modo alto contraste**: respeitar `isHighContrastEnabled` do sistema, aumentar borda das cells
- **Content descriptions**: todas as células do grid e teclas com TalkBack labels
- **Não depender só de cor**: adicionar bordas/ícones para diferenciar estados (acessível a daltônicos)

## Implementação — Theme Compose

```kotlin
// PalabritaTheme.kt
@Composable
fun PalabritaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PalabritaTypography,
        shapes = PalabritaShapes,
        content = content
    )
}

// Cores de feedback como CompositionLocal (não mudam com theme)
val LocalGameColors = staticCompositionLocalOf { GameColors() }

data class GameColors(
    val correct: Color = Color(0xFF4ECDC4),
    val present: Color = Color(0xFFFFB347),
    val absent: Color = Color(0xFFFF6B6B),
    val unused: Color = Color(0xFF787C7E),
    val onFeedback: Color = Color(0xFF1A1A2E)  // texto sobre qualquer cor de feedback
)
```

## Critérios de Aceite

- [ ] Todas as cores de feedback visíveis em light e dark theme
- [ ] Contraste WCAG AA para texto sobre cores de feedback
- [ ] Cores de tier aplicadas corretamente nos badges
- [ ] Animações rodam a 60fps em dispositivos de referência
- [ ] TalkBack descreve estados das células sem depender de cor
- [ ] Theme switching (light/dark) não causa flash ou jank
- [ ] Emojis de compartilhamento usam 🟦🟧🟥 (não 🟩🟨⬜)
- [ ] BottomNavigation com 3 tabs e indicador ativo em mint
- [ ] HomeCards (Streak, Daily, FreePlay, QuickStats) seguem padrão PalabritaCard
- [ ] ChatCard no ResultScreen usa primaryContainer com borda primary
- [ ] StaticCuriosityCard aparece em modo Light no lugar do ChatCard
- [ ] ChatNudge com slide-in animation
- [ ] ConfirmAbandonDialog segue padrão Material 3
- [ ] Confetti animation na vitória a 60fps
- [ ] GenerationIndicator com loading spinner e auto-dismiss
- [ ] Badge component em 3 tamanhos com shimmer para Lendário
