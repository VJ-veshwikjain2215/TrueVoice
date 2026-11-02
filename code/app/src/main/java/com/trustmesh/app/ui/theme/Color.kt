package com.trustmesh.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Stitch Design Tokens ───────────────────────────────────────────────────
// Core Backgrounds & Surfaces (Cyber Dark Slate Aesthetic)
val StitchBgDark             = Color(0xFF0F1715)   // Deep cyan-slate dark mode background
val StitchSurfaceDark        = Color(0xFF15221E)   // Surface container
val StitchCardDark           = Color(0xFF1B2B26)   // Glassmorphism card container
val StitchCardElevatedDark   = Color(0xFF22352F)   // Elevated card container
val StitchBorderGlow         = Color(0x3388D5C0)   // Subtle mint border glow (20% alpha)
val StitchGlassBorder        = Color(0x2288D5C0)   // Frosted glass border / subtle divider (13% alpha mint)

// Brand Primary Colors (Emerald Teal & Mint Glow)
val StitchPrimaryTeal        = Color(0xFF006B58)   // Primary brand emerald teal
val StitchPrimaryTealDark    = Color(0xFF005142)   // Darker teal shade
val StitchMintGlow           = Color(0xFF88D5C0)   // Mint accent glow
val StitchTealLight          = Color(0xFFA4F2DB)   // Soft teal container highlight

// Secondary & Tertiary Accent Colors
val StitchWarningAmber       = Color(0xFFFEB64E)   // Warm gold/amber for caution
val StitchSecondaryAmber     = Color(0xFFE8A33D)   // Secondary amber
val StitchAlertRed           = Color(0xFFBA1A1A)   // Critical threat red
val StitchTertiaryRed        = Color(0xFFD9622B)   // Coral red accent

// Neutral Typography & Icon Colors
val StitchTextPrimary        = Color(0xFFE5E2E1)   // Crisp light headline text
val StitchTextSecondary      = Color(0xFF9DA3A0)   // Muted slate secondary copy
val StitchTextMuted          = Color(0xFF6C7572)   // Caption / subtle divider text
val StitchDividerDark        = Color(0xFF263631)   // Separator line color

// Light Mode & Legacy Palette (Maintained for total backward compatibility)
val OnboardingBackground     = Color(0xFF0F1715)   // Aligned to dark slate theme
val OnboardingText           = Color(0xFFE5E2E1)
val OnboardingTextSecondary  = Color(0xFF9DA3A0)
val OnboardingDivider        = Color(0xFF263631)

val OnboardingAccentBlue     = Color(0xFF006B58)
val OnboardingAccentPurple   = Color(0xFF88D5C0)
val OnboardingAccentGreen    = Color(0xFF88D5C0)
val OnboardingAccentAmber    = Color(0xFFFEB64E)
val OnboardingAccentRed      = Color(0xFFBA1A1A)

val OnboardingButtonBg       = Color(0xFF006B58)
val OnboardingButtonText     = Color(0xFFFFFFFF)

val OnboardingProgressActive = Color(0xFF88D5C0)
val OnboardingProgressInactive = Color(0xFF263631)

// Legacy Theme Colors
val TrustMeshBackground      = StitchBgDark
val TrustMeshSurface         = StitchSurfaceDark
val TrustMeshSurfaceElevated = StitchCardDark

val TextPrimary              = StitchTextPrimary
val TextSecondary            = StitchTextSecondary

val SecurityAccent           = StitchMintGlow
val RiskLow                  = StitchMintGlow
val RiskElevated             = StitchWarningAmber
val RiskHigh                 = StitchTertiaryRed
val RiskCritical             = StitchAlertRed
