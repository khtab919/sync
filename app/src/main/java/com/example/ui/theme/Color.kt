package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Layali — restrained petrol, slate and muted amethyst tokens
// (Zero Yellow / Gold — Pure Luxury Nightlife & Creator Club Palette)
// ============================================================================

// 1. Core Backgrounds & Surfaces
val RoyalNavyBackground = Color(0xFF0B121C) // خلفية ليلية داكنة فاخرة
val RoyalNavyCard = Color(0xFF15202C)       // بطاقات كحلي/أوبسيديان أنيقة
val ElevatedNavySurface = Color(0xFF1D2A38) // سطح فرعي مرفوع

val CharcoalNight = RoyalNavyBackground
val DarkCardSurface = RoyalNavyCard
val ElevatedDarkSurface = ElevatedNavySurface

// Light Mode Soft Surfaces (when toggled)
val PureWhite = Color(0xFFF8FAFC)           // النص الأساسي - أبيض ناعم
val SnowGraySurface = Color(0xFF15202C)
val LightCardSurface = Color(0xFF15202C)

// 2. Primary Interactive Accent (أزرق ياقوتي / سيان للأزرار والتفاعل)
val DeepTealPrimary = Color(0xFF287C88)     // اللون الأساسي للأزرار والتفاعل
val DeepTealBright = Color(0xFF6FADB5)      // تدرج سيان مضيء
val DeepTealDark = Color(0xFF185D68)        // أزرق ياقوتي عميق

// 3. Royal Amethyst & Platinum Rose (بديل اللون الأصفر بالكامل للـ VIP والـ LEVEL)
val RoyalAmethystVip = Color(0xFF8F82A4)    // بنفسجي ملكي فاخر للـ VIP والـ LEVEL
val LuminousLavender = Color(0xFFB8A9C6)    // لافندر بلاتيني مضيء
val ImperialRosePink = Color(0xFFB9849C)    // وردي ياقوتي فاخر

// Semantic aliases (replacing yellow/gold tokens with Royal Amethyst & Lavender)
val MutedLuxuryGold = RoyalAmethystVip
val SoftChampagneGold = LuminousLavender

val CoralPinkPrimary = DeepTealPrimary
val CoralSoftAccent = DeepTealBright
val VioletBlueVip = RoyalAmethystVip
val CharcoalButton = RoyalNavyCard

val SovereignGold = RoyalAmethystVip
val DeepAmberGold = Color(0xFF78648F)
val ShamCashTeal = Color(0xFF287C88)
val ShamCashIndigo = Color(0xFF5A7391)
val UsdtTetherGreen = Color(0xFF10B981)
val MidnightSapphire = RoyalNavyBackground

// 4. Typography & Hierarchy Colors
val SoftWhiteText = Color(0xFFF8FAFC)
val SlateSecondaryText = Color(0xFFA6B3BE)

val TitleDarkText = SoftWhiteText
val SubtitleGrayText = SlateSecondaryText
val MutedLightText = SlateSecondaryText

// 5. Subtle Borders & Dividers
val DarkBorderGray = Color(0xFF2A3846)
val LightBorderGray = Color(0xFF2A3846)

// 6. Status & Semantic Accents
val EmeraldVerified = Color(0xFF10B981)
val TelegramBlue = Color(0xFF287C88)
val ErrorCrimson = Color(0xFFD45E70)

// ============================================================================
// Compatibility Aliases
// ============================================================================
val ObsidianNight = RoyalNavyBackground
val VelvetPlum = RoyalNavyCard
val ElevatedPlumCard = ElevatedNavySurface
val DeepRoyalViolet = ElevatedNavySurface

val DustyRose = DeepTealPrimary
val SoftRosePink = DeepTealBright
val WarmCoral = Color(0xFFD45E70)

val RoyalGold = RoyalAmethystVip
val SoftGold = LuminousLavender

val IvoryCream = SoftWhiteText
val SoftBlushBackground = RoyalNavyBackground
val SoftRoseSurface = RoyalNavyCard
val SoftRoseCard = RoyalNavyCard
val DeepPlumText = SoftWhiteText
val MutedPlumText = SlateSecondaryText

val MutedLavender = SlateSecondaryText
val DeepWineOutline = DarkBorderGray
val SoftRoseOutline = DarkBorderGray

val BrandChampagneGold = RoyalAmethystVip
val BrandDeepViolet = RoyalNavyCard
val BrandDustyRose = DeepTealPrimary
val BrandRoyalPlum = RoyalNavyCard
val BrandSoftPearl = SoftWhiteText
