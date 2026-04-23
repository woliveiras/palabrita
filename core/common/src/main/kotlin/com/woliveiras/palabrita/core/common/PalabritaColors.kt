package com.woliveiras.palabrita.core.common

import androidx.compose.ui.graphics.Color

/**
 * Primitive color tokens extracted from the Palabrita design system.
 *
 * Rules:
 * - These are raw values only — no semantics, no usage context.
 * - Never reference these directly in UI. Use MaterialTheme.colorScheme or
 *   LocalGameColors instead.
 * - App theme (app/ui/theme/Color.kt) maps these to Material color roles.
 */
object PalabritaColors {

    // -------------------------------------------------------------------------
    // Splash screen gradient
    // from-[#6366f1] via-[#8b5cf6] to-[#a855f7], direction: top → bottom
    // -------------------------------------------------------------------------

    val SplashGradientStart = Color(0xFF6366F1) // indigo top
    val SplashGradientMid = Color(0xFF8B5CF6)   // violet center
    val SplashGradientEnd = Color(0xFFA855F7)   // purple bottom

    // -------------------------------------------------------------------------
    // Brand — purple/indigo gradient identity
    // -------------------------------------------------------------------------

    /** Gradient start — blue-indigo side (splash, buttons, daily challenge card). */
    val BrandIndigo = Color(0xFF6C63FF)

    /** Gradient end — violet-purple side. */
    val BrandViolet = Color(0xFFB44BEC)

    /**
     * Single-color reference for brand when a gradient is not possible
     * (e.g. Material primary role, icon tint).
     */
    val BrandPurple = Color(0xFF7B5CFA)

    /** Light tonal variant used for selected-state backgrounds and primaryContainer. */
    val BrandPurpleContainer = Color(0xFFEDE9FE)

    /** On-brand — white text/icons on top of any brand color or gradient. */
    val BrandOnPurple = Color(0xFFFFFFFF)

    /** On-brand container — dark text on light tonal container. */
    val BrandOnPurpleContainer = Color(0xFF3D008A)

    // -------------------------------------------------------------------------
    // Neutral — surfaces and text
    // -------------------------------------------------------------------------

    val BackgroundLight = Color(0xFFF8F8FA)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF0F4FF)

    val BackgroundDark = Color(0xFF121213)
    val SurfaceDark = Color(0xFF1E1E20)
    val SurfaceVariantDark = Color(0xFF2A2A2E)

    /** Primary text — dark navy, used for headings and body on light backgrounds. */
    val ContentPrimary = Color(0xFF1A1A2E)

    /** Secondary text — medium gray for subtitles and captions. */
    val ContentSecondary = Color(0xFF6B7280)

    /** Light-mode dark-surface equivalents. */
    val ContentPrimaryDark = Color(0xFFF5F5F5)
    val ContentSecondaryDark = Color(0xFF9CA3AF)

    // -------------------------------------------------------------------------
    // Outline
    // -------------------------------------------------------------------------

    val OutlineDefault = Color(0xFFE5E7EB)
    val OutlineDark = Color(0xFF3A3A3C)

    // -------------------------------------------------------------------------
    // Icon container tints
    // Soft, pastel backgrounds for rounded icon containers in cards and list items.
    // -------------------------------------------------------------------------

    val ContainerPurple = Color(0xFFEDE9FE)   // AI model, settings icons
    val ContainerGreen = Color(0xFFDCFCE7)    // Privacy, security icons
    val ContainerBlue = Color(0xFFDBEAFE)     // Battery, info icons
    val ContainerAmber = Color(0xFFFFF7ED)    // Tips, warnings

    val OnContainerPurple = Color(0xFF6C63FF)
    val OnContainerGreen = Color(0xFF15803D)
    val OnContainerBlue = Color(0xFF1D4ED8)
    val OnContainerAmber = Color(0xFFB45309)

    // -------------------------------------------------------------------------
    // Game tile feedback
    // Standard Wordle-style colors, same in light and dark.
    // -------------------------------------------------------------------------

    /** Correct letter in the correct position. */
    val TileCorrect = Color(0xFF22C55E)

    /** Correct letter in the wrong position. */
    val TilePresent = Color(0xFFF59E0B)

    /** Letter not in the word. */
    val TileAbsent = Color(0xFF787C7E)

    /** Tile not yet interacted with. */
    val TileUnused = Color(0xFFE5E7EB)

    val TileUnusedDark = Color(0xFF3A3A3C)

    /** White text/icons rendered on top of any colored tile. */
    val OnTile = Color(0xFFFFFFFF)

    // -------------------------------------------------------------------------
    // Status banners
    // -------------------------------------------------------------------------

    val StatusSuccessContainer = Color(0xFFDCFCE7)
    val StatusOnSuccessContainer = Color(0xFF15803D)

    // -------------------------------------------------------------------------
    // Error
    // -------------------------------------------------------------------------

    val ErrorRed = Color(0xFFFF6B6B)
    val ErrorRedDark = Color(0xFFFF6B6B)
    val OnError = Color(0xFFFFFFFF)
    val OnErrorDark = Color(0xFF1A1A2E)

    // -------------------------------------------------------------------------
    // Player tier badge colors
    // -------------------------------------------------------------------------

    val TierNovato = Color(0xFF9CA3AF)
    val TierCurioso = Color(0xFF4ECDC4)
    val TierAstuto = Color(0xFFFFB347)
    val TierSabio = Color(0xFFA78BFA)
    val TierEpico = Color(0xFFF472B6)
    val TierLendario = Color(0xFFFFD700)
}
