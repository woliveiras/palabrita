# Spec 08 — Design System

## Summary

Color palette, typography, spacing, and visual components of Palabrita. All colors are defined as `Color` in the Compose Theme and referenced by semantic tokens, never hardcoded.

## Color Palette

### Game Feedback Colors

| Token | Name | Hex | Usage |
|---|---|---|---|
| `correct` | Mint/Teal | `#4ECDC4` | Correct letter in the right position (grid + keyboard) |
| `present` | Amber/Gold | `#FFB347` | Letter exists but wrong position (grid + keyboard) |
| `absent` | Coral | `#FF6B6B` | Letter does not exist in the word (grid + keyboard) |
| `unused` | Neutral Gray | `#787C7E` | Unused key on keyboard |

### UI Colors — Light Theme

| Token | Hex | Usage |
|---|---|---|
| `surface` | `#FAFAFA` | Main background |
| `surfaceVariant` | `#F0F0F0` | Cards, inputs, empty cells |
| `onSurface` | `#1A1A2E` | Primary text |
| `onSurfaceVariant` | `#6B7280` | Secondary text, labels |
| `primary` | `#4ECDC4` | Primary buttons, accent, links |
| `onPrimary` | `#FFFFFF` | Text on primary button |
| `primaryContainer` | `#D4F5F2` | Badge and chip backgrounds |
| `error` | `#FF6B6B` | Errors, destructive states |
| `outline` | `#D1D5DB` | Cell borders, dividers |
| `cellBorder` | `#878A8C` | Cell border with typed letter (before submit) |

### UI Colors — Dark Theme

| Token | Hex | Usage |
|---|---|---|
| `surface` | `#121213` | Main background |
| `surfaceVariant` | `#1E1E20` | Cards, inputs, empty cells |
| `onSurface` | `#F5F5F5` | Primary text |
| `onSurfaceVariant` | `#9CA3AF` | Secondary text |
| `primary` | `#4ECDC4` | Accent (same in both themes) |
| `onPrimary` | `#1A1A2E` | Text on primary button |
| `primaryContainer` | `#1A3A38` | Badge and chip backgrounds |
| `error` | `#FF6B6B` | Errors (same in both themes) |
| `outline` | `#3A3A3C` | Borders |
| `cellBorder` | `#565758` | Cell border with letter |

### Feedback Colors — Dark Theme Adjustment

The feedback colors (correct, present, absent) **do not change** between light and dark. They are vibrant enough for both backgrounds. This ensures that screenshots and shares are consistent.

### Tier Colors

| Tier | Color | Hex | Usage |
|---|---|---|---|
| Novice | Gray | `#9CA3AF` | Badge, profile border |
| Curious | Teal | `#4ECDC4` | Badge, profile border |
| Cunning | Amber | `#FFB347` | Badge, profile border |
| Wise | Purple | `#A78BFA` | Badge, profile border |
| Epic | Pink | `#F472B6` | Badge, profile border |
| Legendary | Golden gradient | `#FFD700` → `#FFA500` | Badge with shimmer, animated border |

## Typography

Material 3 type scale with Compose `Typography`. Default font: system (Roboto on Android).

| Token | Size | Weight | Usage |
|---|---|---|---|
| `displayLarge` | 32sp | Bold | Result screen ("Congratulations!") |
| `headlineMedium` | 24sp | SemiBold | Section titles (onboarding, settings) |
| `titleLarge` | 20sp | SemiBold | Game header ("Palabrita") |
| `titleMedium` | 16sp | Medium | Subtitles, card labels |
| `bodyLarge` | 16sp | Regular | Main text, hints |
| `bodyMedium` | 14sp | Regular | Secondary text |
| `labelLarge` | 14sp | Medium | Buttons |
| `labelMedium` | 12sp | Medium | Badges, chips, counters |
| `gridLetter` | 24sp | Bold | Letter inside grid cell |
| `keyboardLetter` | 14sp | SemiBold | Letter on keyboard key |

## Spacing

Based on multiples of 4dp (Material 3):

| Token | Value | Usage |
|---|---|---|
| `spacingXs` | 4dp | Between inline icon and text |
| `spacingSm` | 8dp | Internal padding for chips/badges |
| `spacingMd` | 16dp | Card padding, gap between rows |
| `spacingLg` | 24dp | Screen side margin |
| `spacingXl` | 32dp | Gap between sections |
| `spacingXxl` | 48dp | Top/bottom of onboarding screens |

## Corners (Shape)

| Component | Radius |
|---|---|
| Primary button | 12dp (rounded) |
| Card | 16dp |
| Grid cell | 4dp |
| Keyboard key | 6dp |
| Tier badge | 8dp |
| Dialog | 28dp (Material 3 default) |
| Bottom sheet | 28dp top corners |

## Elevation

| Component | Elevation |
|---|---|
| Main surface | 0dp |
| Cards | 1dp |
| Keyboard | 2dp |
| Hint card (revealed) | 3dp |
| Dialog/Bottom sheet | 6dp |

## Animations

| Animation | Duration | Easing | Where |
|---|---|---|---|
| Cell flip | 300ms per cell, 100ms stagger | FastOutSlowIn | After submitting attempt |
| Row shake | 300ms | Linear (oscillation) | Invalid attempt |
| Bounce/pop | 400ms | OvershootInterpolator | Victory (all correct) |
| Fade in | 200ms | FastOutSlowIn | Screen transitions, hint revealed |
| Slide up | 250ms | FastOutSlowIn | Hint card appearing |
| Progress bar | Continuous | Linear | Download, generation |
| Tier badge shimmer | 2s loop | EaseInOut | Legendary badge (shimmer) |

## Reusable Components

### PalabritaButton

```kotlin
// Variants:
// - Primary: mint background, white text
// - Secondary: outlined, mint border, mint text
// - Destructive: coral background, white text
// - Ghost: no background, mint text (for tertiary actions)
```

### PalabritaCard

```kotlin
// Background: surfaceVariant
// Border: outline (1dp)
// Padding: spacingMd (16dp)
// Corner: 16dp
// Selectable: border changes to primary (mint) when selected
```

### TierBadge

```kotlin
// Chip with tier color + icon
// Corner: 8dp
// Text: labelMedium
// Legendary: shimmer effect on background
```

## New Components (Spec 10-12)

### BottomNavigation

```kotlin
// 3 tabs: Home (🏠), Stats (📊), More (⚙️)
// Background: surface
// Active indicator: primary (mint) pill shape
// Icons: Material Icons (Home, BarChart, MoreHoriz)
// Labels: labelMedium
// Elevation: 2dp
// Height: 80dp (include bottom inset)
```

### HomeCards

All HomeScreen cards follow the `PalabritaCard` pattern with variations:

**StreakCard**
```kotlin
// Background: primaryContainer (#D4F5F2 light / #1A3A38 dark)
// Icon: 🔥 (inline emoji)
// Progress bar: primary (#4ECDC4) over outline (#D1D5DB)
// Streak text: titleMedium, bold
// Milestone text: bodyMedium, onSurfaceVariant
// Padding: spacingMd (16dp)
// Corner: 16dp
```

**DailyChallengesCard**
```kotlin
// Background: surfaceVariant
// Header: "⭐ DAILY CHALLENGES (N/3)" — titleMedium
// Each challenge: Row with number + state icon + category + difficulty
// States:
//   ✅ COMPLETED: onSurface text, correct icon (#4ECDC4)
//   🔓 AVAILABLE: onSurface text, primary icon
//   🔒 LOCKED: onSurfaceVariant text (dimmed), outline icon
// CTA "PLAY #N": PalabritaButton Primary
// Corner: 16dp
// Elevation: 1dp
```

**FreePlayCard**
```kotlin
// Background: surfaceVariant
// Icon: 🎲 (inline emoji)
// Title: "FREE MODE" — titleMedium
// Description: bodyMedium, onSurfaceVariant
// CTA: PalabritaButton Secondary (outlined)
// Corner: 16dp
```

**QuickStatsRow**
```kotlin
// 2×2 grid of mini-cards
// Each mini-card: surfaceVariant, corner 12dp, padding spacingSm
// Value: titleMedium, bold, onSurface
// Label: labelMedium, onSurfaceVariant
// Gap: spacingSm (8dp)
```

### GenerationIndicator

```kotlin
// Background: surfaceVariant with outline border
// Icon: loading animation (CircularProgressIndicator, size 16dp)
// Text: bodyMedium, onSurfaceVariant
// "Ready" state: ✓ icon (correct color), auto-dismiss 3s
// Corner: 12dp
// Padding: spacingSm vertical, spacingMd horizontal
```

### ChatCard (ResultScreen)

```kotlin
// Background: primaryContainer (#D4F5F2 light / #1A3A38 dark)
// Border: 2dp, primary (#4ECDC4)
// Corner: 16dp
// Elevation: 2dp
// Header: "💬 Explore '{word}'" — titleMedium
// Suggestions: horizontally scrollable chips
//   Each chip: surfaceVariant bg, corner 8dp, labelMedium
//   Icon: category emoji + text
// Bonus: "+1 XP bonus ✨" — labelMedium, primary color
// CTA: PalabritaButton Primary "EXPLORE NOW"
// Light Mode: replaced by StaticCuriosityCard
```

### StaticCuriosityCard (Light Mode)

```kotlin
// Background: surfaceVariant
// Border: 1dp, outline
// Corner: 16dp
// Icon: 📖
// Title: "About '{word}'" — titleMedium
// Text: bodyMedium, onSurface — static curiosity from puzzle
// Footer: "— Curiosity of the day" — labelMedium, onSurfaceVariant, italic
```

### ChatNudge

```kotlin
// Background: primaryContainer
// Corner: 12dp
// Text: "💬 Want to know more about '{word}'?" — bodyMedium
// CTA: "Explore now" — labelLarge, primary
// Dismiss: IconButton "✕" in top-right corner
// Animation: slide-in from bottom, 250ms
```

### ConfirmAbandonDialog

```kotlin
// Standard Material 3 Dialog (AlertDialog)
// Title: "Abandon game?" — headlineMedium
// Body: "Your progress in this game will be lost." — bodyLarge
// Primary button: "Keep playing" — PalabritaButton Primary
// Destructive button: "Abandon" — PalabritaButton Destructive
// Corner: 28dp (Material 3 default)
```

### SharingCard (visual sharing — future)

```kotlin
// Background: surface
// Header: "Palabrita 🔥N · Tier · XP" — titleLarge
// Context: "Challenge N/3 ⭐⭐" or "Free ⭐⭐⭐" — bodyMedium
// Emoji grid: monospaced, bodyLarge
// Footer: hints + XP — labelMedium, onSurfaceVariant
// Corner: 16dp
// Border: 1dp, outline
```

### Confetti Animation

```kotlin
// Trigger: game victory (all cells correct)
// Particles: 50-80 colored rectangles/squares
// Colors: correct (#4ECDC4), present (#FFB347), absent (#FF6B6B), primary
// Duration: 2s
// Origin: top of screen, gravity downward
// Easing: EaseOut for gravity
// Suggested library: nl.dionsegijn:konfetti-compose (lightweight, Compose-native)
// Fallback: if performance < 60fps, reduce particles or disable
```

### Badge Component

```kotlin
// Sizes: Small (24dp), Medium (32dp), Large (48dp)
// Earned: colored icon + tier color background
// Not earned: gray icon (unused #787C7E) + surfaceVariant background
// Progress: "7/10" — labelMedium below icon
// Corner: 50% (circular)
// Legendary: shimmer effect (same as TierBadge)
```

## Accessibility

- **Minimum contrast**: WCAG AA (4.5:1 for text, 3:1 for UI)
  - Mint `#4ECDC4` on `#1A1A2E` (dark text) → 4.6:1 ✅
  - Coral `#FF6B6B` on `#FFFFFF` → 3.9:1 (use dark text `#1A1A2E` on coral)
  - Amber `#FFB347` on `#FFFFFF` → 2.1:1 (use dark text `#1A1A2E` on amber)
- **Text on feedback colors**: always `#1A1A2E` (dark) to ensure contrast
- **High contrast mode**: respect `isHighContrastEnabled` from system, increase cell borders
- **Content descriptions**: all grid cells and keys with TalkBack labels
- **Don't rely on color alone**: add borders/icons to differentiate states (accessible to colorblind users)

## Implementation — Compose Theme

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

// Feedback colors as CompositionLocal (do not change with theme)
val LocalGameColors = staticCompositionLocalOf { GameColors() }

data class GameColors(
    val correct: Color = Color(0xFF4ECDC4),
    val present: Color = Color(0xFFFFB347),
    val absent: Color = Color(0xFFFF6B6B),
    val unused: Color = Color(0xFF787C7E),
    val onFeedback: Color = Color(0xFF1A1A2E)  // text on any feedback color
)
```

## Acceptance Criteria

- [ ] All feedback colors visible in both light and dark theme
- [ ] WCAG AA contrast for text on feedback colors
- [ ] Tier colors correctly applied to badges
- [ ] Animations run at 60fps on reference devices
- [ ] TalkBack describes cell states without relying on color
- [ ] Theme switching (light/dark) causes no flash or jank
- [ ] Share emojis use 🟦🟧🟥 (not 🟩🟨⬜)
- [ ] BottomNavigation with 3 tabs and active indicator in mint
- [ ] HomeCards (Streak, Daily, FreePlay, QuickStats) follow PalabritaCard pattern
- [ ] ChatCard on ResultScreen uses primaryContainer with primary border
- [ ] StaticCuriosityCard appears in Light mode in place of ChatCard
- [ ] ChatNudge with slide-in animation
- [ ] ConfirmAbandonDialog follows Material 3 standard
- [ ] Confetti animation on victory at 60fps
- [ ] GenerationIndicator with loading spinner and auto-dismiss
- [ ] Badge component in 3 sizes with shimmer for Legendary
